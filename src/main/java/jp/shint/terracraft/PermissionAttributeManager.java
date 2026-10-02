package jp.shint.terracraft;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Applies the restricted form of a Terracraft weapon to its current holder.
 * These modifiers are attached to the player, never to an ItemStack, so a
 * traded axe immediately uses the recipient's permission state.
 */
public final class PermissionAttributeManager {
    private static final Identifier DAMAGE_ID = TerracraftMod.id("restricted_terra_axe_damage");
    private static final Identifier SPEED_ID = TerracraftMod.id("restricted_terra_axe_speed");
    private static final EntityAttributeModifier DAMAGE_PENALTY = new EntityAttributeModifier(
            DAMAGE_ID, -3.0, EntityAttributeModifier.Operation.ADD_VALUE);
    // The registered axe is 12 damage / 0.6667 speed. This restores stone-axe
    // combat values (9 damage / 0.8 speed) only while an unlicensed holder uses it.
    private static final EntityAttributeModifier SPEED_COMPENSATION = new EntityAttributeModifier(
            SPEED_ID, 0.1333333, EntityAttributeModifier.Operation.ADD_VALUE);

    private PermissionAttributeManager() { }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(PermissionAttributeManager::refresh);
    }

    private static void refresh(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            boolean restricted = player.getMainHandStack().isOf(TerracraftMod.TERRA_AXE)
                    && !WeaponPermissions.hasTerraAxe(player);
            set(player.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE), DAMAGE_ID, DAMAGE_PENALTY, restricted);
            set(player.getAttributeInstance(EntityAttributes.ATTACK_SPEED), SPEED_ID, SPEED_COMPENSATION, restricted);
        }
    }

    private static void set(EntityAttributeInstance attribute, Identifier id, EntityAttributeModifier modifier, boolean enabled) {
        if (attribute == null) return;
        if (enabled && !attribute.hasModifier(id)) attribute.addTemporaryModifier(modifier);
        if (!enabled && attribute.hasModifier(id)) attribute.removeModifier(id);
    }
}
