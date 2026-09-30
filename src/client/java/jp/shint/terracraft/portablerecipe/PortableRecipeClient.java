package jp.shint.terracraft.portablerecipe;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class PortableRecipeClient implements ClientModInitializer {
    private static KeyBinding openBook;
    private static volatile RecipeViewState state = RecipeViewState.loading();
    private static volatile int successVersion;

    @Override
    public void onInitializeClient() {
        openBook = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.terracraft_portable_recipe_ui.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                KeyBinding.Category.create(Identifier.of(PortableRecipeMod.MOD_ID, "recipe_book"))
        ));
        ClientPlayNetworking.registerGlobalReceiver(PortableRecipeNetworking.RECIPE_STATE,
                (payload, context) -> context.client().execute(() -> {
                    state = RecipeViewState.from(payload);
                    if ("作成しました".equals(payload.message())) {
                        successVersion++;
                    }
                }));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openBook.wasPressed()) {
                if (client.currentScreen == null && client.player != null) {
                    state = RecipeViewState.loading();
                    client.setScreen(new PortableRecipeScreen());
                    ClientPlayNetworking.send(new PortableRecipeNetworking.RequestStatePayload());
                }
            }
        });
    }

    static RecipeViewState state() {
        return state;
    }

    static int successVersion() {
        return successVersion;
    }

    record RecipeViewState(boolean loaded, boolean unlocked, int netheriteCount, int blazeRodCount, String message) {
        static RecipeViewState loading() {
            return new RecipeViewState(false, false, 0, 0, "確認中…");
        }

        static RecipeViewState from(PortableRecipeNetworking.RecipeStatePayload payload) {
            return new RecipeViewState(true, payload.unlocked(), payload.netheriteCount(), payload.blazeRodCount(), payload.message());
        }
    }
}
