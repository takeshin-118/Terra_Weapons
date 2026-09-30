package jp.shint.terracraft.mixin;

import jp.shint.terracraft.AltarScreen;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hides JEI's ingredient panel only while Terracraft's altar UI is active. */
@Pseudo
@Mixin(targets = "mezz.jei.gui.overlay.IngredientListOverlay", remap = false)
public abstract class JeiIngredientListOverlayMixin {
    @Inject(method = "isListDisplayed", at = @At("HEAD"), cancellable = true, remap = false)
    private void terracraft$hideForAltar(CallbackInfoReturnable<Boolean> callback) {
        if (MinecraftClient.getInstance().currentScreen instanceof AltarScreen) callback.setReturnValue(false);
    }
}
