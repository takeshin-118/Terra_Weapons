package jp.shint.terracraft.portablerecipe;

import net.fabricmc.api.ModInitializer;

public final class PortableRecipeMod implements ModInitializer {
    public static final String MOD_ID = "terracraft_portable_recipe_ui";

    @Override
    public void onInitialize() {
        PortableRecipeNetworking.initializeServer();
    }
}
