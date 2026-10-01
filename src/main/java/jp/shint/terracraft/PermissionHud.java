package jp.shint.terracraft;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Server-authoritative permission feedback shown in the in-game HUD. */
public final class PermissionHud {
    private PermissionHud() { }

    public static void granted(ServerPlayerEntity player) {
        player.sendMessage(Text.translatable("hud.terracraft.granted").formatted(Formatting.AQUA), true);
        player.playSound(SoundEvents.BLOCK_BEACON_ACTIVATE, 0.75F, 1.25F);
    }

    public static void revoked(ServerPlayerEntity player) {
        player.sendMessage(Text.translatable("hud.terracraft.revoked").formatted(Formatting.RED), true);
        player.playSound(SoundEvents.BLOCK_BEACON_DEACTIVATE, 0.75F, 0.8F);
    }
}
