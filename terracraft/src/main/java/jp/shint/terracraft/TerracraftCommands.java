package jp.shint.terracraft;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.command.permission.Permission;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TerracraftCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger("terracraft-commands");
    private TerracraftCommands() { }
    public static void initialize() { CommandRegistrationCallback.EVENT.register(TerracraftCommands::register); }
    private static void register(com.mojang.brigadier.CommandDispatcher<ServerCommandSource> d, CommandRegistryAccess a, CommandManager.RegistrationEnvironment e) {
        var permission = CommandManager.literal("permission");
        permission.then(CommandManager.literal("grant").then(CommandManager.argument("player", EntityArgumentType.player()).then(CommandManager.argument("weaponId", StringArgumentType.word()).executes(c -> change(c.getSource(), EntityArgumentType.getPlayer(c, "player").getUuid(), EntityArgumentType.getPlayer(c, "player").getName().getString(), StringArgumentType.getString(c, "weaponId"), true)))).then(CommandManager.argument("uuid", UuidArgumentType.uuid()).then(CommandManager.argument("weaponId", StringArgumentType.word()).executes(c -> change(c.getSource(), UuidArgumentType.getUuid(c, "uuid"), UuidArgumentType.getUuid(c, "uuid").toString(), StringArgumentType.getString(c, "weaponId"), true)))));
        permission.then(CommandManager.literal("revoke").then(CommandManager.argument("player", EntityArgumentType.player()).then(CommandManager.argument("weaponId", StringArgumentType.word()).executes(c -> change(c.getSource(), EntityArgumentType.getPlayer(c, "player").getUuid(), EntityArgumentType.getPlayer(c, "player").getName().getString(), StringArgumentType.getString(c, "weaponId"), false)))).then(CommandManager.argument("uuid", UuidArgumentType.uuid()).then(CommandManager.argument("weaponId", StringArgumentType.word()).executes(c -> change(c.getSource(), UuidArgumentType.getUuid(c, "uuid"), UuidArgumentType.getUuid(c, "uuid").toString(), StringArgumentType.getString(c, "weaponId"), false)))));
        permission.then(CommandManager.literal("check").then(CommandManager.argument("player", EntityArgumentType.player()).then(CommandManager.argument("weaponId", StringArgumentType.word()).executes(c -> check(c.getSource(), EntityArgumentType.getPlayer(c, "player").getUuid(), EntityArgumentType.getPlayer(c, "player").getName().getString(), StringArgumentType.getString(c, "weaponId"))))));
        permission.then(CommandManager.literal("list").then(CommandManager.argument("player", EntityArgumentType.player()).executes(c -> list(c.getSource(), EntityArgumentType.getPlayer(c, "player").getUuid(), EntityArgumentType.getPlayer(c, "player").getName().getString()))));
        d.register(CommandManager.literal("terracraft").requires(source -> source.getPermissions().hasPermission(new Permission.Level(PermissionLevel.ADMINS)))
                .then(CommandManager.literal("weapon").then(permission))
                .then(CommandManager.literal("config").then(CommandManager.literal("reload").executes(c -> reload(c.getSource())))));
    }
    private static int change(ServerCommandSource source, UUID player, String name, String id, boolean grant) {
        if (!"nether_axe".equals(id)) { source.sendError(Text.literal("Unknown Terracraft weapon: " + id)); return 0; }
        if (grant) {
            WeaponPermissions.get(source.getServer()).grant(player, id);
            ServerPlayerEntity online = source.getServer().getPlayerManager().getPlayer(player);
            if (online != null) {
                TerracraftRecipes.unlockNetherAxe(online);
                PermissionHud.granted(online);
            } else WeaponPermissions.get(source.getServer()).queueNotification(player, id, true);
        } else {
            WeaponPermissions.get(source.getServer()).revoke(player, id);
            ServerPlayerEntity online = source.getServer().getPlayerManager().getPlayer(player);
            if (online != null) {
                TerracraftRecipes.lockNetherAxe(online);
                PermissionHud.revoked(online);
            } else WeaponPermissions.get(source.getServer()).queueNotification(player, id, false);
        }
        LOGGER.info("Terracraft permission {}: actor={}, target={}, weapon={}, time={}", grant ? "grant" : "revoke", source.getName(), player, id, java.time.Instant.now());
        source.sendFeedback(() -> Text.literal((grant ? "Granted " : "Revoked ") + id + " for " + name), true);
        return 1;
    }
    private static int check(ServerCommandSource source, UUID player, String name, String id) {
        boolean result = "nether_axe".equals(id) && WeaponPermissions.get(source.getServer()).has(player, id);
        source.sendFeedback(() -> Text.literal(name + ": " + result), false); return result ? 1 : 0;
    }
    private static int list(ServerCommandSource source, UUID player, String name) {
        var rights = WeaponPermissions.get(source.getServer()).list(player);
        source.sendFeedback(() -> Text.literal(name + ": " + (rights.isEmpty() ? "(none)" : String.join(", ", rights))), false);
        return rights.size();
    }
    private static int reload(ServerCommandSource source) {
        boolean ok = TerracraftConfig.load();
        if (ok) {
            TreeFellingManager.cancelAll();
            AltarRitualManager.cancelAll();
            source.sendFeedback(() -> Text.translatable("command.terracraft.reload"), true);
        }
        else source.sendError(Text.translatable("command.terracraft.reload_failed"));
        return ok ? 1 : 0;
    }
}
