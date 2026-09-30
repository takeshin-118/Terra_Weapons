package jp.shint.terracraft.mixin;

import java.util.Optional;
import jp.shint.terracraft.TerracraftMod;
import net.minecraft.block.CrafterBlock;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** A redstone crafter must never consume materials for a permission-gated weapon. */
@Mixin(CrafterBlock.class)
abstract class CrafterBlockMixin {
    @Redirect(method = "craft", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/CrafterBlock;getCraftingRecipe(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/recipe/input/CraftingRecipeInput;)Ljava/util/Optional;"))
    private Optional<RecipeEntry<CraftingRecipe>> terracraft$rejectAutomaticWeaponCrafting(ServerWorld world, CraftingRecipeInput input) {
        Optional<RecipeEntry<CraftingRecipe>> recipe = CrafterBlock.getCraftingRecipe(world, input);
        if (recipe.isPresent() && recipe.get().value().craft(input, world.getRegistryManager()).isOf(TerracraftMod.NETHER_AXE)) {
            return Optional.empty();
        }
        return recipe;
    }
}
