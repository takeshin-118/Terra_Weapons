package jp.shint.terracraft.portablerecipe;

import jp.shint.terracraft.TerracraftMod;
import jp.shint.terracraft.WeaponPermissions;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;

/** Access to the weapon and permissions in the same mod. */
public final class TerracraftAccess {
    private TerracraftAccess() {
    }

    public static Item terraAxe() {
        return TerracraftMod.TERRA_AXE;
    }

    public static boolean hasTerraAxePermission(ServerPlayerEntity player) {
        return WeaponPermissions.hasTerraAxeCraftPermission(player);
    }
}
