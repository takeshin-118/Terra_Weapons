package jp.shint.terracraft.mixin;

import jp.shint.terracraft.TerracraftMod;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(AnvilScreen.class)
abstract class WeaponAnvilScreenMixin {
    @ModifyConstant(method = "drawForeground", constant = @Constant(intValue = 40))
    private int terracraft$showActualCost(int original) {
        return ((AnvilScreen) (Object) this).getScreenHandler().getSlot(0).getStack().isOf(TerracraftMod.TERRA_AXE) ? 65 : original;
    }
}
