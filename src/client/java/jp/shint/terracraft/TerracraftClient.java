package jp.shint.terracraft;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public final class TerracraftClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        HandledScreens.register(TerracraftMod.ALTAR_SCREEN_HANDLER, AltarScreen::new);
        new jp.shint.terracraft.portablerecipe.PortableRecipeClient().onInitializeClient();
        HandledScreens.register(TerracraftMod.PORTABLE_CRAFTING_SCREEN_HANDLER, PortableCraftingScreen::new);
    }
}
