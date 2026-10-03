package jp.shint.terracraft.mixin;

import jp.shint.terracraft.WeaponWorkstation;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GrindstoneScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GrindstoneScreenHandler.class)
abstract class GrindstoneScreenHandlerMixin {
    @Inject(method = "getOutputStack", at = @At("HEAD"), cancellable = true)
    private void terracraft$validate(ItemStack first, ItemStack second, CallbackInfoReturnable<ItemStack> cir) {
        if (((WeaponWorkstation) this).terracraft$problem() != WeaponWorkstation.NONE) cir.setReturnValue(ItemStack.EMPTY);
    }
}
