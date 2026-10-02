package jp.shint.terracraft;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WeaponPermissionsTest {
    @Test void oldAndNewWeaponNamesShareTheSamePermission() {
        var permissions = new WeaponPermissions();
        var player = UUID.randomUUID();
        permissions.grant(player, "nether_axe");
        assertTrue(permissions.has(player, "terra_axe"));
        assertEquals(java.util.Set.of("terra_axe"), permissions.list(player));
        permissions.revoke(player, "terra_axe");
        assertFalse(permissions.has(player, "nether_axe"));
        permissions.grant(player, "terra_axe");
        permissions.revoke(player, "nether_axe");
        assertTrue(permissions.list(player).isEmpty());
    }
    @Test void offlineNotificationAliasesCollapseToTheLastChange() {
        var permissions = new WeaponPermissions();
        var player = UUID.randomUUID();
        permissions.queueNotification(player, "nether_axe", true);
        permissions.queueNotification(player, "terra_axe", false);
        assertEquals(java.util.Map.of("terra_axe", false), permissions.consumeNotifications(player));
    }
}
