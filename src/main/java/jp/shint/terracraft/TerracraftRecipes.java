package jp.shint.terracraft;

import java.util.List;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;

/** Keeps recipe-book mutations next to the permission workflow, not inside ItemStacks. */
public final class TerracraftRecipes {
    private static final RegistryKey<Recipe<?>> NETHER_AXE_RECIPE = RegistryKey.of(RegistryKeys.RECIPE, TerracraftMod.id("nether_axe"));
    private TerracraftRecipes() { }

    public static void unlockNetherAxe(ServerPlayerEntity player) {
        player.getEntityWorld().getServer().getRecipeManager().get(NETHER_AXE_RECIPE).ifPresent(recipe -> player.unlockRecipes(List.of(recipe)));
    }

    public static void lockNetherAxe(ServerPlayerEntity player) {
        player.getEntityWorld().getServer().getRecipeManager().get(NETHER_AXE_RECIPE).ifPresent(recipe -> player.lockRecipes(List.of(recipe)));
    }
}
