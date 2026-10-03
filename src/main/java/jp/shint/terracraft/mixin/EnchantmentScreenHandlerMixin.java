package jp.shint.terracraft.mixin;

import jp.shint.terracraft.TerracraftMod;
import jp.shint.terracraft.WeaponWorkstation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.EnchantmentScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only players who earned the weapon may enchant it at an enchanting table. */
@Mixin(EnchantmentScreenHandler.class)
abstract class EnchantmentScreenHandlerMixin {
    @Shadow private Inventory inventory;
    @Shadow public int[] enchantmentPower;
    @Shadow public int[] enchantmentId;
    @Shadow public int[] enchantmentLevel;

    @Inject(method = "onContentChanged", at = @At("HEAD"), cancellable = true)
    private void terracraft$hideUnavailableOffers(Inventory changed, CallbackInfo ci) {
        if (((WeaponWorkstation) this).terracraft$problem() == WeaponWorkstation.PERMISSION_REQUIRED) {
            java.util.Arrays.fill(enchantmentPower, 0);
            java.util.Arrays.fill(enchantmentId, -1);
            java.util.Arrays.fill(enchantmentLevel, -1);
            ci.cancel();
        }
    }

    @Inject(method = "onButtonClick", at = @At("HEAD"), cancellable = true)
    private void terracraft$rejectEnchantingTable(PlayerEntity player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (inventory.getStack(0).isOf(TerracraftMod.TERRA_AXE)
                && !((WeaponWorkstation) this).terracraft$allowed()) {
            cir.setReturnValue(false);
        }
    }
}
