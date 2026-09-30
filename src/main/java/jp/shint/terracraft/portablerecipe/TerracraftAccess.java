package jp.shint.terracraft.portablerecipe;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.lang.reflect.Method;

/**
 * Keeps this add-on binary-compatible with the distributed Terracraft JAR,
 * which is compiled with intermediary mappings rather than development mappings.
 */
public final class TerracraftAccess {
    private static final Identifier NETHER_AXE_ID = Identifier.of("terracraft", "nether_axe");

    private TerracraftAccess() {
    }

    public static Item netherAxe() {
        return Registries.ITEM.get(NETHER_AXE_ID);
    }

    public static boolean hasNetherAxePermission(ServerPlayerEntity player) {
        try {
            Class<?> permissions = Class.forName("jp.shint.terracraft.WeaponPermissions");
            Method checker = permissions.getMethod("hasNetherAxe", ServerPlayerEntity.class);
            return (boolean) checker.invoke(null, player);
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }
}
