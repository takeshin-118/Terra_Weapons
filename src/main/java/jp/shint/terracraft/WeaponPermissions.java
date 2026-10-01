package jp.shint.terracraft;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

/** Overworld persistent, UUID-keyed rights. Rights never live on an ItemStack. */
public final class WeaponPermissions extends PersistentState {
    private static final Codec<Map<String, List<String>>> MAP_CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf());
    private static final Codec<WeaponPermissions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MAP_CODEC.optionalFieldOf("rights", Map.of()).forGetter(WeaponPermissions::encodeRights),
            MAP_CODEC.optionalFieldOf("pending_notifications", Map.of()).forGetter(WeaponPermissions::encodeNotifications)
    ).apply(instance, WeaponPermissions::decode));
    private static final PersistentStateType<WeaponPermissions> TYPE = new PersistentStateType<>("terracraft_permissions", WeaponPermissions::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
    private final Map<UUID, Set<String>> rights = new HashMap<>();
    /** Last offline change per weapon. A later change replaces an earlier one. */
    private final Map<UUID, Map<String, Boolean>> pendingNotifications = new HashMap<>();
    public static WeaponPermissions get(MinecraftServer server) { return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE); }
    public static boolean hasNetherAxe(ServerPlayerEntity player) { return player.isCreative() || get(player.getEntityWorld().getServer()).has(player.getUuid(), "nether_axe"); }
    public boolean has(UUID player, String weapon) { return rights.getOrDefault(player, Set.of()).contains(weapon); }
    public Set<String> list(UUID player) { return Set.copyOf(rights.getOrDefault(player, Set.of())); }
    public void grant(UUID player, String weapon) { rights.computeIfAbsent(player, ignored -> new HashSet<>()).add(weapon); markDirty(); }
    public void revoke(UUID player, String weapon) { Set<String> set = rights.get(player); if (set != null) { set.remove(weapon); if (set.isEmpty()) rights.remove(player); markDirty(); } }
    public void queueNotification(UUID player, String weapon, boolean granted) { pendingNotifications.computeIfAbsent(player, ignored -> new HashMap<>()).put(weapon, granted); markDirty(); }
    public Map<String, Boolean> consumeNotifications(UUID player) { Map<String, Boolean> result = pendingNotifications.remove(player); if (result == null) return Map.of(); markDirty(); return Map.copyOf(result); }
    private static WeaponPermissions decode(Map<String, List<String>> encodedRights, Map<String, List<String>> encodedPending) {
        WeaponPermissions state = new WeaponPermissions();
        encodedRights.forEach((uuid, weapons) -> { try { state.rights.put(UUID.fromString(uuid), new HashSet<>(weapons)); } catch (IllegalArgumentException ignored) { } });
        encodedPending.forEach((uuid, changes) -> { try { Map<String, Boolean> values = new HashMap<>(); for (String value : changes) if (value.length() > 1 && (value.charAt(0) == '+' || value.charAt(0) == '-')) values.put(value.substring(1), value.charAt(0) == '+'); if (!values.isEmpty()) state.pendingNotifications.put(UUID.fromString(uuid), values); } catch (IllegalArgumentException ignored) { } });
        return state;
    }
    private Map<String, List<String>> encodeRights() {
        Map<String, List<String>> encodedRights = new HashMap<>(); rights.forEach((uuid, weapons) -> encodedRights.put(uuid.toString(), List.copyOf(weapons)));
        return encodedRights;
    }
    private Map<String, List<String>> encodeNotifications() {
        Map<String, List<String>> encodedNotifications = new HashMap<>(); pendingNotifications.forEach((uuid, changes) -> encodedNotifications.put(uuid.toString(), changes.entrySet().stream().map(entry -> (entry.getValue() ? "+" : "-") + entry.getKey()).toList()));
        return encodedNotifications;
    }
}
