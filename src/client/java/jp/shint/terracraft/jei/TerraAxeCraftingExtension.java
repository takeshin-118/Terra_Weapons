package jp.shint.terracraft.jei;

import java.util.List;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.display.ShapedCraftingRecipeDisplay;
import net.minecraft.recipe.display.SlotDisplay;

/** Adds a permission link to the existing recipe, without adding an ingredient to it. */
final class TerraAxeCraftingExtension implements ICraftingCategoryExtension<CraftingRecipe> {
    @Override public boolean isHandled(RecipeEntry<CraftingRecipe> recipe) {
        var id = recipe.id().getValue();
        return id.getNamespace().equals("terracraft")
                && (id.getPath().equals("nether_axe") || id.getPath().equals("terra_axe"))
                && !recipe.value().getDisplays().isEmpty()
                && recipe.value().getDisplays().getFirst() instanceof ShapedCraftingRecipeDisplay;
    }
    private ShapedCraftingRecipeDisplay display(RecipeEntry<CraftingRecipe> recipe) {
        return (ShapedCraftingRecipeDisplay) recipe.value().getDisplays().getFirst();
    }
    @Override public List<SlotDisplay> getIngredients(RecipeEntry<CraftingRecipe> recipe) { return display(recipe).ingredients(); }
    @Override public int getWidth(RecipeEntry<CraftingRecipe> recipe) { return display(recipe).width(); }
    @Override public int getHeight(RecipeEntry<CraftingRecipe> recipe) { return display(recipe).height(); }
    @Override public void createRecipeExtras(RecipeEntry<CraftingRecipe> recipe, IRecipeExtrasBuilder builder,
            ICraftingGridHelper grid, IFocusGroup focuses) {
        var widget = new CraftPermissionWidget();
        builder.addWidget(widget);
        builder.addInputHandler(widget);
    }
}
