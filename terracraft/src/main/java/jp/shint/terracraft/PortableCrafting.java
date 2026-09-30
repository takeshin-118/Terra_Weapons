package jp.shint.terracraft;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

/** Opens vanilla's safe 3x3 crafting handler from the licensed weapon. */
public final class PortableCrafting {
    private PortableCrafting() { }

    public static void initialize() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient() || hand != Hand.MAIN_HAND || !player.isSneaking()
                    || !player.getStackInHand(hand).isOf(TerracraftMod.NETHER_AXE)) return ActionResult.PASS;
            player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inventory, ignored) ->
                    new PortableCraftingScreenHandler(syncId, inventory),
                    Text.translatable("container.terracraft.portable_crafting")));
            return ActionResult.SUCCESS_SERVER;
        });
    }

}
