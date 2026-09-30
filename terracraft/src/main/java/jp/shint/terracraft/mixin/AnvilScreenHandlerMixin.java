package jp.shint.terracraft.mixin;

import jp.shint.terracraft.TerracraftMod;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Allows a Terracraft weapon to use the anvil up to level 51 at 1.3x cost. */
@Mixin(AnvilScreenHandler.class)
abstract class AnvilScreenHandlerMixin {
    @Shadow private Property levelCost;

    @ModifyConstant(method = "updateResult", constant = @Constant(intValue = 40))
    private int terracraft$raiseVanillaTooExpensiveLimit(int original) {
        return 52;
    }

    @Inject(method = "updateResult", at = @At("TAIL"))
    private void terracraft$applyTerracraftAnvilCost(CallbackInfo ci) {
        if (!((AnvilScreenHandler) (Object) this).getSlot(0).getStack().isOf(TerracraftMod.NETHER_AXE)) return;
        int cost = levelCost.get();
        if (cost > 0) levelCost.set(Math.min(51, (int) Math.ceil(cost * 1.3D)));
    }

    @Inject(method = "canTakeOutput", at = @At("HEAD"), cancellable = true)
    private void terracraft$permitLevelFiftyOne(PlayerEntity player, boolean present, CallbackInfoReturnable<Boolean> cir) {
        if (!((AnvilScreenHandler) (Object) this).getSlot(0).getStack().isOf(TerracraftMod.NETHER_AXE)) return;
        int cost = levelCost.get();
        cir.setReturnValue(present && (player.getAbilities().creativeMode || (cost > 0 && cost <= 51 && player.experienceLevel >= cost)));
    }
}
