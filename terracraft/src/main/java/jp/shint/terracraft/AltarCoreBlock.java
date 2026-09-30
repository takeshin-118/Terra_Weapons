package jp.shint.terracraft;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Altar core with a player-operated, persistent offering inventory. */
public final class AltarCoreBlock extends BlockWithEntity {
    public AltarCoreBlock(Settings settings) { super(settings); }
    @Override protected MapCodec<? extends BlockWithEntity> getCodec() { return MapCodec.unit(() -> this); }
    @Override public BlockEntity createBlockEntity(BlockPos pos, BlockState state) { return new AltarCoreBlockEntity(pos, state); }
    @Override public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient()) {
            if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
                boolean structureComplete = AltarStructure.matchesNetherAxe(world, pos);
                // An incomplete altar never opens the ritual UI: construction feedback stays in-world.
                if (!structureComplete) {
                    player.sendMessage(Text.translatable("message.terracraft.altar_incomplete_detail"), true);
                } else if (world.getBlockEntity(pos) instanceof AltarCoreBlockEntity altar
                        && AltarSessionManager.open(serverPlayer, pos)) {
                    player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                            (syncId, inventory, ignored) -> AltarScreenHandler.createServer(syncId, inventory, altar,
                                    (net.minecraft.server.world.ServerWorld) world, pos),
                            Text.translatable("container.terracraft.altar")));
                }
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override protected void onStateReplaced(BlockState state, net.minecraft.server.world.ServerWorld world, BlockPos pos, boolean moved) {
        if (world.getBlockEntity(pos) instanceof AltarCoreBlockEntity altar) {
            AltarRitualManager.cancelAt(world, pos);
            AltarSessionManager.closeAt(world, pos);
            ItemScatterer.spawn(world, pos, altar.offerings());
        }
        super.onStateReplaced(state, world, pos, moved);
    }
}
