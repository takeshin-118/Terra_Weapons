package jp.shint.terracraft;

import java.util.List;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;

/** Keeps recipe-book mutations next to the permission workflow, not inside ItemStacks. */
public final class TerracraftRecipes {
    private static final RegistryKey<Recipe<?>> TERRA_AXE_RECIPE = RegistryKey.of(RegistryKeys.RECIPE, TerracraftMod.id("terra_axe"));
    private TerracraftRecipes() { }

    public static void unlockTerraAxe(ServerPlayerEntity player) {
        player.getEntityWorld().getServer().getRecipeManager().get(TERRA_AXE_RECIPE).ifPresent(recipe -> player.unlockRecipes(List.of(recipe)));
    }

    public static void lockTerraAxe(ServerPlayerEntity player) {
        player.getEntityWorld().getServer().getRecipeManager().get(TERRA_AXE_RECIPE).ifPresent(recipe -> player.lockRecipes(List.of(recipe)));
    }
}
