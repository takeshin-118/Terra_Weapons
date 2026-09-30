package jp.shint.terracraft;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ItemScatterer;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** Server-side ownership and expiry for a core's offering screen. */
public final class AltarSessionManager {
    private static final long IDLE_LIMIT_TICKS = 20L * 60L;
    private static final Map<UUID, Session> BY_PLAYER = new HashMap<>();
    private AltarSessionManager() { }

    public static void initialize() { ServerTickEvents.END_SERVER_TICK.register(AltarSessionManager::tick); }

    public static boolean open(ServerPlayerEntity player, BlockPos core) {
        for (Map.Entry<UUID, Session> entry : BY_PLAYER.entrySet()) {
            Session current = entry.getValue();
            if (current.world == player.getEntityWorld() && current.core.equals(core) && !entry.getKey().equals(player.getUuid())) {
                player.sendMessage(Text.translatable("message.terracraft.altar_in_use"), true);
                return false;
            }
        }
        ServerWorld world = (ServerWorld) player.getEntityWorld();
        BY_PLAYER.put(player.getUuid(), new Session(world, core.toImmutable(), world.getTime(), inventoryFingerprint(world, core)));
        return true;
    }

    public static void closeAt(net.minecraft.world.World world, BlockPos core) {
        Iterator<Map.Entry<UUID, Session>> it = BY_PLAYER.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Session> entry = it.next();
            if (entry.getValue().world != world || !entry.getValue().core.equals(core)) continue;
            ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(entry.getKey());
            if (player != null) returnOfferings(player, entry.getValue());
            else scatterOfferings(entry.getValue());
            it.remove();
        }
    }

    public static void cancelAll() { BY_PLAYER.clear(); }

    private static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Session>> it = BY_PLAYER.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Session> entry = it.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            Session session = entry.getValue();
            int fingerprint = inventoryFingerprint(session.world, session.core);
            if (fingerprint != session.inventoryFingerprint) {
                session.inventoryFingerprint = fingerprint;
                session.lastActivityAt = session.world.getTime();
            }
            boolean invalid = player == null || !player.isAlive() || player.getEntityWorld() != session.world
                    || player.getEntityPos().squaredDistanceTo(Vec3d.ofCenter(session.core)) > 64
                    || !session.world.getBlockState(session.core).isOf(TerracraftMod.ALTAR_CORE)
                    || !(player.currentScreenHandler instanceof AltarScreenHandler)
                    || session.world.getTime() - session.lastActivityAt >= IDLE_LIMIT_TICKS;
            if (invalid) {
                if (player != null) {
                    player.closeHandledScreen();
                    returnOfferings(player, session);
                } else scatterOfferings(session);
                it.remove();
            }
        }
    }

    /** Return the session owner's unconsumed offerings before releasing a UI lock. */
    private static void returnOfferings(ServerPlayerEntity player, Session session) {
        if (!(session.world.getBlockEntity(session.core) instanceof AltarCoreBlockEntity altar)) return;
        for (int slot = 0; slot < altar.offerings().size(); slot++) {
            ItemStack stack = altar.offerings().removeStack(slot);
            if (stack.isEmpty()) continue;
            player.getInventory().insertStack(stack);
            if (!stack.isEmpty()) player.dropItem(stack, false);
        }
    }

    private static void scatterOfferings(Session session) {
        if (session.world.getBlockEntity(session.core) instanceof AltarCoreBlockEntity altar) {
            ItemScatterer.spawn(session.world, session.core, altar.offerings());
        }
    }

    private static int inventoryFingerprint(ServerWorld world, BlockPos core) {
        if (!(world.getBlockEntity(core) instanceof AltarCoreBlockEntity altar)) return 0;
        int result = 1;
        for (int slot = 0; slot < altar.offerings().size(); slot++) {
            ItemStack stack = altar.offerings().getStack(slot);
            result = 31 * result + ItemStack.hashCode(stack);
        }
        return result;
    }

    private static final class Session {
        private final ServerWorld world;
        private final BlockPos core;
        private long lastActivityAt;
        private int inventoryFingerprint;

        private Session(ServerWorld world, BlockPos core, long lastActivityAt, int inventoryFingerprint) {
            this.world = world;
            this.core = core;
            this.lastActivityAt = lastActivityAt;
            this.inventoryFingerprint = inventoryFingerprint;
        }
    }
}
