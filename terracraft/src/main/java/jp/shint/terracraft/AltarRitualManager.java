package jp.shint.terracraft;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** Server-side 16 tick altar transaction. Revalidates structure and items immediately before consuming. */
public final class AltarRitualManager {
    private static final Map<UUID, Ritual> ACTIVE = new HashMap<>();
    private AltarRitualManager() { }
    public static void initialize() { ServerTickEvents.END_SERVER_TICK.register(AltarRitualManager::tick); }
    /** Offerings are still in player inventory before completion, so cancellation cannot lose items. */
    public static void cancelAll() {
        for (Map.Entry<UUID, Ritual> entry : ACTIVE.entrySet()) endLock(entry.getValue(), entry.getKey());
        ACTIVE.clear();
    }
    /** Used when a core is broken; no ritual may finish after its offerings are returned. */
    public static void cancelAt(net.minecraft.world.World world, BlockPos core) {
        Iterator<Map.Entry<UUID, Ritual>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Ritual> entry = it.next();
            Ritual ritual = entry.getValue();
            if (ritual.world == world && ritual.core.equals(core)) {
                endLock(ritual, entry.getKey());
                it.remove();
            }
        }
    }
    public static boolean begin(ServerPlayerEntity player, BlockPos core) {
        if (ACTIVE.containsKey(player.getUuid())) return false;
        if (!(player.getEntityWorld().getBlockEntity(core) instanceof AltarCoreBlockEntity altar)) return false;
        if (!hasOfferings(player, altar)) { player.sendMessage(Text.translatable("message.terracraft.offerings_missing"), true); return false; }
        if (!altar.begin(player.getUuid())) {
            player.sendMessage(Text.translatable("message.terracraft.altar_busy"), true);
            return false;
        }
        ACTIVE.put(player.getUuid(), new Ritual(player.getEntityWorld(), core, 16));
        player.sendMessage(Text.translatable("message.terracraft.ritual_started"), true);
        player.getEntityWorld().playSound(null, core, SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.BLOCKS, 0.9F, 0.9F);
        particles(player.getEntityWorld(), core, ParticleTypes.ENCHANT, 24, 0.8D, 0.8D, 0.8D, 0.15D);
        return true;
    }
    /** Used by the altar UI to decide whether a normal click should begin the ritual. */
    public static boolean hasRequiredOfferings(ServerPlayerEntity player, BlockPos core) {
        return player.getEntityWorld().getBlockEntity(core) instanceof AltarCoreBlockEntity altar
                && hasOfferings(player, altar);
    }
    private static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Ritual>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Ritual> entry = it.next(); ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey()); Ritual ritual = entry.getValue();
            if (player == null || !player.isAlive() || player.getEntityWorld() != ritual.world || player.getEntityPos().squaredDistanceTo(Vec3d.ofCenter(ritual.core)) > 64 || !AltarStructure.matchesNetherAxe(ritual.world, ritual.core)) { endLock(ritual, entry.getKey()); ritual.world.playSound(null, ritual.core, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 0.8F, 0.8F); particles(ritual.world, ritual.core, ParticleTypes.SMOKE, 18, 0.6D, 0.45D, 0.6D, 0.03D); if (player != null) player.sendMessage(Text.translatable("message.terracraft.ritual_cancelled"), true); it.remove(); continue; }
            particles(ritual.world, ritual.core, ParticleTypes.PORTAL, 4, 0.55D, 0.6D, 0.55D, 0.02D);
            if (--ritual.remaining > 0) continue;
            if (WeaponPermissions.get(server).has(player.getUuid(), "nether_axe")) player.sendMessage(Text.translatable("message.terracraft.already_permitted"), true);
            else if (!(ritual.world.getBlockEntity(ritual.core) instanceof AltarCoreBlockEntity altar) || !hasOfferings(player, altar)) player.sendMessage(Text.translatable("message.terracraft.offerings_missing"), true);
            else { for (TerracraftConfig.Offering offering : TerracraftConfig.altar().offerings()) consume(player, altar, offering, offering.count()); WeaponPermissions.get(server).grant(player.getUuid(), "nether_axe"); TerracraftRecipes.unlockNetherAxe(player); PermissionHud.granted(player); ritual.world.playSound(null, ritual.core, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.0F, 1.1F); particles(ritual.world, ritual.core, ParticleTypes.HAPPY_VILLAGER, 36, 0.85D, 0.95D, 0.85D, 0.18D); }
            endLock(ritual, entry.getKey());
            it.remove();
        }
    }
    private static void endLock(Ritual ritual, UUID playerId) {
        if (ritual.world.getBlockEntity(ritual.core) instanceof AltarCoreBlockEntity altar) altar.end(playerId);
    }
    private static void particles(ServerWorld world, BlockPos pos, net.minecraft.particle.ParticleEffect effect, int count, double dx, double dy, double dz, double speed) {
        world.spawnParticles(effect, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, count, dx, dy, dz, speed);
    }
    private static boolean hasOfferings(ServerPlayerEntity p, AltarCoreBlockEntity altar) { for (TerracraftConfig.Offering offering : TerracraftConfig.altar().offerings()) if (count(p, offering) + count(altar, offering) < offering.count()) return false; return true; }
    private static int count(ServerPlayerEntity p, TerracraftConfig.Offering offering) { int n=0; for(int i=0;i<p.getInventory().size();i++) { ItemStack s=p.getInventory().getStack(i); if(offering.matches(s)) n+=s.getCount(); } return n; }
    private static int count(AltarCoreBlockEntity altar, TerracraftConfig.Offering offering) { int n=0; for(int i=0;i<altar.offerings().size();i++) { ItemStack s=altar.offerings().getStack(i); if(offering.matches(s)) n+=s.getCount(); } return n; }
    private static void consume(ServerPlayerEntity p, AltarCoreBlockEntity altar, TerracraftConfig.Offering offering, int amount) { for(int i=0;i<altar.offerings().size() && amount>0;i++) { ItemStack s=altar.offerings().getStack(i); if(offering.matches(s)) { int take=Math.min(amount,s.getCount()); s.decrement(take); amount-=take; } } for(int i=0;i<p.getInventory().size() && amount>0;i++) { ItemStack s=p.getInventory().getStack(i); if(offering.matches(s)) { int take=Math.min(amount,s.getCount()); s.decrement(take); amount-=take; } } }
    private static final class Ritual { final ServerWorld world; final BlockPos core; int remaining; Ritual(ServerWorld w, BlockPos p, int t){world=w;core=p.toImmutable();remaining=t;} }
}
