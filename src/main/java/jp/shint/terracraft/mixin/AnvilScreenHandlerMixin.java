package jp.shint.terracraft.mixin;

import jp.shint.terracraft.TerracraftMod;
import jp.shint.terracraft.WeaponWorkstation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilScreenHandler.class)
abstract class AnvilScreenHandlerMixin {
    @Shadow private Property levelCost;
    @Unique private AnvilScreenHandler terracraft$handler() { return (AnvilScreenHandler) (Object) this; }
    @Unique private boolean terracraft$isAxe() { return terracraft$handler().getSlot(0).getStack().isOf(TerracraftMod.TERRA_AXE); }

    @Inject(method = "updateResult", at = @At("HEAD"), cancellable = true)
    private void terracraft$validate(CallbackInfo ci) {
        if (((WeaponWorkstation) this).terracraft$problem() != WeaponWorkstation.NONE) {
            terracraft$handler().getSlot(2).setStack(ItemStack.EMPTY);
            levelCost.set(0);
            ci.cancel();
        }
    }

    // Clamp before vanilla tests the cost, including costs saturated at Integer.MAX_VALUE.
    @ModifyArg(method = "updateResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/Property;set(I)V"), index = 0)
    private int terracraft$capCost(int cost) { return terracraft$isAxe() ? Math.min(64, cost) : cost; }

    @ModifyConstant(method = "updateResult", constant = @Constant(intValue = 40))
    private int terracraft$costLimit(int original) { return terracraft$isAxe() ? 65 : original; }

    @Inject(method = "canTakeOutput", at = @At("HEAD"), cancellable = true)
    private void terracraft$validateTake(PlayerEntity player, boolean present, CallbackInfoReturnable<Boolean> cir) {
        if (((WeaponWorkstation) this).terracraft$problem() != WeaponWorkstation.NONE) cir.setReturnValue(false);
    }
}
