package jp.shint.terracraft.portablerecipe;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class PortableRecipeNetworking {
    public static final CustomPayload.Id<RequestStatePayload> REQUEST_STATE = id("request_state");
    public static final CustomPayload.Id<CraftTerraAxePayload> CRAFT_TERRA_AXE = id("craft_terra_axe");
    public static final CustomPayload.Id<RecipeStatePayload> RECIPE_STATE = id("recipe_state");

    private PortableRecipeNetworking() {
    }

    public static void initializeServer() {
        PayloadTypeRegistry.playC2S().register(REQUEST_STATE, RequestStatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CRAFT_TERRA_AXE, CraftTerraAxePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RECIPE_STATE, RecipeStatePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(REQUEST_STATE, (payload, context) ->
                context.server().execute(() -> sendState(context.player(), "")));
        ServerPlayNetworking.registerGlobalReceiver(CRAFT_TERRA_AXE, (payload, context) ->
                context.server().execute(() -> craftTerraAxe(context.player())));
    }

    private static void craftTerraAxe(ServerPlayerEntity player) {
        if (!TerracraftAccess.hasTerraAxePermission(player)) {
            sendState(player, "儀式により解放してください");
            return;
        }
        if (!hasIngredients(player, Items.NETHERITE_INGOT, 3)) {
            sendState(player, "ネザライトインゴットが不足しています");
            return;
        }
        if (!hasIngredients(player, Items.BLAZE_ROD, 2)) {
            sendState(player, "ブレイズロッドが不足しています");
            return;
        }
        if (!PortableRecipeCrafting.tryCraft(player.getInventory(), TerracraftAccess.terraAxe())) {
            sendState(player, "インベントリに空きがありません");
            return;
        }

        player.currentScreenHandler.sendContentUpdates();
        sendState(player, "作成しました");
    }

    private static boolean hasIngredients(ServerPlayerEntity player, Item item, int amount) {
        return count(player, item) >= amount;
    }

    private static int count(ServerPlayerEntity player, Item item) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isOf(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void sendState(ServerPlayerEntity player, String message) {
        ServerPlayNetworking.send(player, new RecipeStatePayload(
                TerracraftAccess.hasTerraAxePermission(player),
                count(player, Items.NETHERITE_INGOT),
                count(player, Items.BLAZE_ROD),
                message
        ));
    }

    private static <T extends CustomPayload> CustomPayload.Id<T> id(String path) {
        return new CustomPayload.Id<>(Identifier.of(PortableRecipeMod.MOD_ID, path));
    }

    public record RequestStatePayload() implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, RequestStatePayload> CODEC = PacketCodec.unit(new RequestStatePayload());

        @Override
        public Id<? extends CustomPayload> getId() {
            return REQUEST_STATE;
        }
    }

    public record CraftTerraAxePayload() implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, CraftTerraAxePayload> CODEC = PacketCodec.unit(new CraftTerraAxePayload());

        @Override
        public Id<? extends CustomPayload> getId() {
            return CRAFT_TERRA_AXE;
        }
    }

    public record RecipeStatePayload(boolean unlocked, int netheriteCount, int blazeRodCount, String message) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, RecipeStatePayload> CODEC = PacketCodec.tuple(
                PacketCodecs.BOOLEAN, RecipeStatePayload::unlocked,
                PacketCodecs.VAR_INT, RecipeStatePayload::netheriteCount,
                PacketCodecs.VAR_INT, RecipeStatePayload::blazeRodCount,
                PacketCodecs.STRING, RecipeStatePayload::message,
                RecipeStatePayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return RECIPE_STATE;
        }
    }
}
