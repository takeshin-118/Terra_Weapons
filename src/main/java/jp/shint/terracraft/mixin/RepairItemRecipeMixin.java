package jp.shint.terracraft.mixin;

import jp.shint.terracraft.TerracraftMod;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RepairItemRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Covers both crafting grids and automated crafters, including direct recipe.craft calls. */
@Mixin(RepairItemRecipe.class)
abstract class RepairItemRecipeMixin {
    @Inject(method = "canCombineStacks", at = @At("HEAD"), cancellable = true)
    private static void terracraft$noAxeRepair(ItemStack first, ItemStack second, CallbackInfoReturnable<Boolean> cir) {
        if (first.isOf(TerracraftMod.TERRA_AXE) || second.isOf(TerracraftMod.TERRA_AXE)) cir.setReturnValue(false);
    }
}
