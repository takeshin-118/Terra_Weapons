package jp.shint.terracraft.mixin;

import jp.shint.terracraft.TerracraftMod;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.GrindstoneScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents a grindstone from stripping the weapon's controlled enchantments. */
@Mixin(GrindstoneScreenHandler.class)
abstract class GrindstoneScreenHandlerMixin {
    @Shadow private Inventory result;
    @Shadow private Inventory input;

    @Inject(method = "onContentChanged", at = @At("TAIL"))
    private void terracraft$blockGrindstoneOutput(Inventory changed, CallbackInfo ci) {
        if (input.getStack(0).isOf(TerracraftMod.TERRA_AXE) || input.getStack(1).isOf(TerracraftMod.TERRA_AXE)) {
            result.clear();
        }
    }
}
