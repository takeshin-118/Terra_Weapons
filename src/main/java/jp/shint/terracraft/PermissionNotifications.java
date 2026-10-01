package jp.shint.terracraft;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.text.Text;

/** Delivers only the final offline permission state once the player has joined. */
public final class PermissionNotifications {
    private PermissionNotifications() { }
    public static void initialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var player = handler.player;
            WeaponPermissions.get(server).consumeNotifications(player.getUuid()).forEach((weapon, granted) -> {
                if (!"nether_axe".equals(weapon)) return;
                if (granted) {
                    TerracraftRecipes.unlockNetherAxe(player);
                    PermissionHud.granted(player);
                } else {
                    TerracraftRecipes.lockNetherAxe(player);
                    PermissionHud.revoked(player);
                }
            });
        });
    }
}
