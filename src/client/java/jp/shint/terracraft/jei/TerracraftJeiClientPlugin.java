package jp.shint.terracraft.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.util.Identifier;
import java.util.List;

public final class TerracraftJeiClientPlugin implements IModPlugin {
    private static IJeiRuntime runtime;
    static IJeiRuntime runtime() { return runtime; }
    /** Optional entry point for the portable recipe book, without a hard JEI dependency there. */
    public static boolean openTerraAxeRitual() {
        if (runtime == null) return false;
        runtime.getRecipesGui().showTypes(List.of(RitualCategory.TYPE));
        return true;
    }
    @Override public void onRuntimeAvailable(IJeiRuntime value) { runtime = value; }
    @Override public void onRuntimeUnavailable() { runtime = null; }
    @Override public Identifier getPluginUid() { return TerracraftJeiPlugin.CATEGORY_ID; }
    @Override public void registerVanillaCategoryExtensions(mezz.jei.api.registration.IVanillaCategoryExtensionRegistration registration) {
        var category = registration.getCraftingCategory();
        var extension = new TerraAxeCraftingExtension();
        category.addExtension(net.minecraft.recipe.ShapedRecipe.class, extension);
        // JEI reconstructs server-synchronized crafting displays using its own recipe class.
        try {
            var syncedRecipe = Class.forName("mezz.jei.library.plugins.vanilla.crafting.JeiShapedRecipe")
                    .asSubclass(net.minecraft.recipe.CraftingRecipe.class);
            category.addExtension(syncedRecipe, extension);
        } catch (ClassNotFoundException ignored) {
            // Versions using vanilla ShapedRecipe are already covered above.
        }
    }
    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new RitualCategory(registration.getJeiHelpers().getGuiHelper()));
    }
    @Override public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(RitualCategory.TYPE, List.of(new TerracraftJeiPlugin.RitualGuide()));
    }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(jp.shint.terracraft.TerracraftMod.ALTAR_CORE_ITEM, RitualCategory.TYPE);
    }
}
