package jp.shint.terracraft;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.state.property.Properties;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Bounded, server-side tree felling queue. The initiating block remains a normal player break. */
public final class TreeFellingManager {
    private static final List<Task> TASKS = new ArrayList<>();
    /** Positions claimed by active queues, so the same tree cannot be queued twice. */
    private static final Set<BlockPos> RESERVED_POSITIONS = new HashSet<>();
    private static boolean breakingQueuedBlock;

    private TreeFellingManager() { }

    public static void initialize() {
        PlayerBlockBreakEvents.AFTER.register(TreeFellingManager::afterBreak);
        ServerTickEvents.END_SERVER_TICK.register(TreeFellingManager::tick);
    }

    /** Called before a configuration swap so no queued work uses stale targeting rules. */
    public static void cancelAll() {
        for (Task task : TASKS) release(task);
        TASKS.clear();
        RESERVED_POSITIONS.clear();
    }

    private static void afterBreak(net.minecraft.world.World rawWorld, net.minecraft.entity.player.PlayerEntity rawPlayer, BlockPos origin, BlockState state, net.minecraft.block.entity.BlockEntity blockEntity) {
        if (breakingQueuedBlock || !(rawWorld instanceof ServerWorld world) || !(rawPlayer instanceof ServerPlayerEntity player) || !eligible(player, state)) return;
        TerracraftConfig.TreeFellingDefinition settings = TerracraftConfig.tree();
        if (TASKS.size() >= settings.maxTasks()) { player.sendMessage(net.minecraft.text.Text.translatable("message.terracraft.tree_limit"), true); return; }
        if (taskCount(player.getUuid()) >= settings.maxTasksPerPlayer()) { player.sendMessage(net.minecraft.text.Text.translatable("message.terracraft.tree_player_limit"), true); return; }
        ArrayDeque<BlockPos> queue = findConnectedLogs(world, origin, state);
        if (queue.isEmpty()) return;
        for (BlockPos position : queue) if (RESERVED_POSITIONS.contains(position)) {
            player.sendMessage(net.minecraft.text.Text.translatable("message.terracraft.tree_busy"), true);
            return;
        }
        RESERVED_POSITIONS.addAll(queue);
        TASKS.add(new Task(player.getUuid(), world, queue, axeId(player.getMainHandStack())));
    }

    private static boolean eligible(ServerPlayerEntity player, BlockState state) {
        if (!player.interactionManager.getGameMode().isSurvivalLike() || !WeaponPermissions.hasNetherAxe(player) || player.isSneaking() || !player.getMainHandStack().isOf(TerracraftMod.NETHER_AXE)) return false;
        return isTargetLog(state);
    }

    private static boolean isTargetLog(BlockState state) {
        if (!state.isIn(BlockTags.LOGS)) return false;
        String id = String.valueOf(net.minecraft.registry.Registries.BLOCK.getId(state.getBlock()));
        return !id.contains("stripped_") && !id.contains("wood");
    }

    private static int taskCount(UUID playerId) {
        int count = 0;
        for (Task task : TASKS) if (task.playerId.equals(playerId)) count++;
        return count;
    }

    /** Lazily assigns a persisted identity; copied/dropped stacks keep the same identity. */
    private static String axeId(ItemStack stack) {
        NbtComponent data = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        String id = data.copyNbt().getString("terracraft_axe_id", "");
        if (!id.isEmpty()) return id;
        id = UUID.randomUUID().toString();
        String generated = id;
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.putString("terracraft_axe_id", generated));
        return id;
    }

    private static ArrayDeque<BlockPos> findConnectedLogs(ServerWorld world, BlockPos origin, BlockState originState) {
        ArrayDeque<BlockPos> result = new ArrayDeque<>();
        ArrayDeque<BlockPos> search = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        search.add(origin); visited.add(origin);
        while (!search.isEmpty()) {
            BlockPos current = search.removeFirst();
            for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
                if (x == 0 && y == 0 && z == 0) continue;
                BlockPos next = current.add(x, y, z);
                if (!visited.add(next)) continue;
                if (!world.isChunkLoaded(next)) return new ArrayDeque<>();
                BlockState nextState = world.getBlockState(next);
                if (nextState.getBlock() == originState.getBlock()) {
                    // The original block was already broken, but still counts toward the 128-log limit.
                    if (result.size() + 2 > TerracraftConfig.tree().maxLogs()) return new ArrayDeque<>();
                    result.addLast(next);
                    search.addLast(next);
                }
            }
        }
        ArrayDeque<BlockPos> foliage = findNaturalLeaves(world, origin, result);
        if (foliage == null) return new ArrayDeque<>();
        result.addAll(foliage);
        return result;
    }

    /** Finds only leaf blocks attached to this log cluster; hand-placed persistent leaves are preserved. */
    private static ArrayDeque<BlockPos> findNaturalLeaves(ServerWorld world, BlockPos origin, ArrayDeque<BlockPos> logs) {
        ArrayDeque<BlockPos> foliage = new ArrayDeque<>();
        ArrayDeque<BlockPos> search = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        search.add(origin);
        for (BlockPos log : logs) search.add(log);
        while (!search.isEmpty()) {
            BlockPos current = search.removeFirst();
            for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
                if (x == 0 && y == 0 && z == 0) continue;
                BlockPos next = current.add(x, y, z);
                if (!visited.add(next)) continue;
                if (!world.isChunkLoaded(next)) return null;
                if (!isFellingFoliage(world.getBlockState(next)) || !nearLog(next, origin, logs)) continue;
                if (foliage.size() >= TerracraftConfig.tree().maxFoliage()) return null;
                foliage.addLast(next);
                search.addLast(next);
            }
        }
        return foliage;
    }

    private static boolean isNaturalLeaf(BlockState state) {
        return state.isIn(BlockTags.LEAVES) && (!state.contains(Properties.PERSISTENT) || !state.get(Properties.PERSISTENT));
    }

    /** Leaves plus vegetation which is physically attached to a natural tree or nether fungus. */
    private static boolean isFellingFoliage(BlockState state) {
        if (isNaturalLeaf(state)) return true;
        TerracraftConfig.TreeFellingDefinition settings = TerracraftConfig.tree();
        if ((settings.includeVines() && state.isOf(Blocks.VINE)) || (settings.includeCocoa() && state.isOf(Blocks.COCOA))) return true;
        return settings.includeNetherFoliage() && (state.isOf(Blocks.NETHER_WART_BLOCK) || state.isOf(Blocks.WARPED_WART_BLOCK)
                || state.isOf(Blocks.SHROOMLIGHT) || state.isOf(Blocks.WEEPING_VINES)
                || state.isOf(Blocks.WEEPING_VINES_PLANT) || state.isOf(Blocks.TWISTING_VINES)
                || state.isOf(Blocks.TWISTING_VINES_PLANT));
    }

    private static boolean nearLog(BlockPos position, BlockPos origin, ArrayDeque<BlockPos> logs) {
        if (position.getSquaredDistance(origin) <= 100) return true;
        for (BlockPos log : logs) if (position.getSquaredDistance(log) <= 100) return true;
        return false;
    }

    private static void tick(MinecraftServer server) {
        int budget = TerracraftConfig.tree().globalPerTick();
        Iterator<Task> iterator = TASKS.iterator();
        while (iterator.hasNext() && budget > 0) {
            Task task = iterator.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(task.playerId);
            if (player == null || player.getEntityWorld() != task.world || !player.isAlive()
                    || !player.getMainHandStack().isOf(TerracraftMod.NETHER_AXE)
                    || !task.axeId.equals(axeId(player.getMainHandStack()))) { release(task); iterator.remove(); continue; }
            int used = 0;
            while (used < TerracraftConfig.tree().perTaskPerTick() && budget > 0 && !task.positions.isEmpty()) {
                BlockPos pos = task.positions.removeFirst();
                RESERVED_POSITIONS.remove(pos);
                if (isTargetLog(task.world.getBlockState(pos)) || isFellingFoliage(task.world.getBlockState(pos))) {
                    breakingQueuedBlock = true;
                    try { player.interactionManager.tryBreakBlock(pos); } finally { breakingQueuedBlock = false; }
                    mergeNearbyDrops(task.world, pos);
                }
                used++; budget--;
            }
            if (task.positions.isEmpty()) { release(task); iterator.remove(); }
        }
    }

    private static void release(Task task) { RESERVED_POSITIONS.removeAll(task.positions); }

    /** Merge normal block drops from the same felling operation before they spread out. */
    private static void mergeNearbyDrops(ServerWorld world, BlockPos pos) {
        List<ItemEntity> drops = world.getEntitiesByClass(ItemEntity.class,
                Box.of(Vec3d.ofCenter(pos), 4.0, 3.0, 4.0), entity -> !entity.isRemoved());
        for (int left = 0; left < drops.size(); left++) {
            ItemEntity target = drops.get(left);
            ItemStack targetStack = target.getStack();
            if (targetStack.isEmpty() || targetStack.isOf(TerracraftMod.NETHER_AXE)) continue;
            for (int right = left + 1; right < drops.size() && !targetStack.isEmpty(); right++) {
                ItemEntity source = drops.get(right);
                ItemStack sourceStack = source.getStack();
                if (source.isRemoved() || sourceStack.isEmpty() || !ItemStack.areItemsAndComponentsEqual(targetStack, sourceStack)) continue;
                int moved = Math.min(targetStack.getMaxCount() - targetStack.getCount(), sourceStack.getCount());
                if (moved <= 0) continue;
                targetStack.increment(moved);
                sourceStack.decrement(moved);
                if (sourceStack.isEmpty()) source.discard();
            }
        }
    }

    private record Task(UUID playerId, ServerWorld world, ArrayDeque<BlockPos> positions, String axeId) { }
}
