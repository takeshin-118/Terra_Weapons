package jp.shint.terracraft.mixin;

import jp.shint.terracraft.TerracraftMod;
import jp.shint.terracraft.WeaponPermissions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.EnchantmentScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Only players who earned the weapon may enchant it at an enchanting table. */
@Mixin(EnchantmentScreenHandler.class)
abstract class EnchantmentScreenHandlerMixin {
    @Shadow private Inventory inventory;

    @Inject(method = "onButtonClick", at = @At("HEAD"), cancellable = true)
    private void terracraft$rejectEnchantingTable(PlayerEntity player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (inventory.getStack(0).isOf(TerracraftMod.NETHER_AXE)
                && !(player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer
                && WeaponPermissions.hasNetherAxe(serverPlayer))) {
            cir.setReturnValue(false);
        }
    }
}
