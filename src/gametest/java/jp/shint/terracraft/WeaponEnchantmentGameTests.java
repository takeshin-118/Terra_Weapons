package jp.shint.terracraft;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.recipe.RepairItemRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.screen.GrindstoneScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;

public class WeaponEnchantmentGameTests {
    private static ServerPlayerEntity player(TestContext context, boolean licensed) {
        var world = context.getWorld();
        var server = world.getServer();
        var id = UUID.randomUUID();
        var data = ConnectedClientData.createDefault(new GameProfile(id, "test_" + id.toString().substring(0, 8)), false);
        var player = new ServerPlayerEntity(server, world, data.gameProfile(), data.syncedOptions());
        var connection = new ClientConnection(NetworkSide.SERVERBOUND);
        new EmbeddedChannel(connection);
        server.getPlayerManager().onPlayerConnect(connection, player, data);
        player.changeGameMode(GameMode.SURVIVAL);
        player.refreshPositionAndAngles(context.getAbsolutePos(new BlockPos(1, 2, 1)), 0, 0);
        if (licensed) WeaponPermissions.get(server).grant(player.getUuid(), "terra_axe");
        return player;
    }

    private static RegistryEntry<Enchantment> enchantment(TestContext context, RegistryKey<Enchantment> key) {
        return context.getWorld().getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(key);
    }

    private static ItemStack axe() { return new ItemStack(TerracraftMod.TERRA_AXE); }

    private static void invoke(Class<?> type, String name, Object object) {
        try {
            Method method = type.getDeclaredMethod(name);
            method.setAccessible(true);
            method.invoke(object);
        } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
    }

    @GameTest public void enchantmentCompatibilityAndFireOffers(TestContext context) {
        var registry = context.getWorld().getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        ItemStack axe = axe();
        ItemStack vanilla = new ItemStack(Items.NETHERITE_AXE);
        registry.streamEntries().forEach(entry -> context.assertEquals(entry.value().isAcceptableItem(axe),
                entry.value().isAcceptableItem(vanilla) || entry.matchesKey(Enchantments.FIRE_ASPECT), entry.getIdAsString()));
        var candidates = registry.getOrThrow(EnchantmentTags.IN_ENCHANTING_TABLE);
        var options = EnchantmentHelper.getPossibleEntries(30, axe, candidates.stream());
        context.assertTrue(options.stream().anyMatch(entry -> entry.enchantment().matchesKey(Enchantments.FIRE_ASPECT) && entry.level() == 2), "Fire Aspect II must be offered");
        context.assertFalse(options.stream().anyMatch(entry -> entry.enchantment().matchesKey(Enchantments.LOOTING)), "Do not add sword-only enchants");
        context.complete();
    }

    @GameTest public void anvilCostAndPermission(TestContext context) {
        var player = player(context, true);
        var handler = new AnvilScreenHandler(1, player.getInventory());
        ItemStack axe = axe();
        axe.setDamage(1000);
        axe.set(DataComponentTypes.REPAIR_COST, Integer.MAX_VALUE);
        handler.getSlot(0).setStack(axe);
        handler.getSlot(1).setStack(new ItemStack(Items.NETHERITE_INGOT));
        context.assertEquals(handler.getLevelCost(), 64, "Cap saturated repair penalties before vanilla rejects the result");
        context.assertTrue(handler.getSlot(2).hasStack(), "High-cost repair should remain possible");
        player.experienceLevel = 63;
        context.assertFalse(handler.getSlot(2).canTakeItems(player), "63 levels are insufficient");
        player.experienceLevel = 64;
        context.assertTrue(handler.getSlot(2).canTakeItems(player), "64 levels suffice");
        WeaponPermissions.get(context.getWorld().getServer()).revoke(player.getUuid(), "terra_axe");
        handler.onSlotClick(2, 0, SlotActionType.QUICK_MOVE, player);
        context.assertTrue(handler.getCursorStack().isEmpty() && handler.getSlot(0).hasStack() && handler.getSlot(1).hasStack(), "Revocation cannot consume inputs or yield output");
        context.assertEquals(player.experienceLevel, 64, "Denied processing must not consume XP");
        WeaponPermissions.get(context.getWorld().getServer()).grant(player.getUuid(), "terra_axe");
        handler.sendContentUpdates();
        handler.onSlotClick(2, 0, SlotActionType.PICKUP, player);
        context.assertTrue(handler.getCursorStack().isOf(TerracraftMod.TERRA_AXE), "Grant restores output without reopening");
        context.assertEquals(player.experienceLevel, 0, "Consume exactly 64 levels");
        var vanilla = new AnvilScreenHandler(2, player.getInventory());
        ItemStack normal = new ItemStack(Items.NETHERITE_AXE);
        normal.setDamage(100);
        normal.set(DataComponentTypes.REPAIR_COST, 40);
        vanilla.getSlot(0).setStack(normal);
        vanilla.getSlot(1).setStack(new ItemStack(Items.NETHERITE_INGOT));
        context.assertFalse(vanilla.getSlot(2).hasStack(), "Vanilla too-expensive rule must stay unchanged");
        context.complete();
    }

    @GameTest public void fireBookRenameAndCombination(TestContext context) {
        var player = player(context, true);
        var handler = new AnvilScreenHandler(1, player.getInventory());
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        book.addEnchantment(enchantment(context, Enchantments.FIRE_ASPECT), 2);
        handler.getSlot(0).setStack(axe());
        handler.getSlot(1).setStack(book);
        context.assertEquals(EnchantmentHelper.getLevel(enchantment(context, Enchantments.FIRE_ASPECT), handler.getSlot(2).getStack()), 2, "Fire book works");
        context.assertEquals(handler.getLevelCost(), 4, "No 1.3x surcharge");
        handler.getSlot(1).setStack(ItemStack.EMPTY);
        handler.setNewItemName("Renamed axe");
        context.assertEquals(handler.getSlot(2).getStack().getName().getString(), "Renamed axe", "Renaming works");
        handler.getSlot(1).setStack(axe());
        context.assertEquals(((WeaponWorkstation) handler).terracraft$problem(), WeaponWorkstation.TOO_HARD, "Two axes report fusion error");
        context.assertFalse(handler.getSlot(2).hasStack(), "No axe combination");
        player.changeGameMode(GameMode.CREATIVE);
        context.assertFalse(handler.getSlot(2).hasStack(), "Creative bypasses permission, not fusion rules");
        context.complete();
    }

    @GameTest public void grindstonePreservesCurses(TestContext context) {
        var player = player(context, false);
        var handler = new GrindstoneScreenHandler(1, player.getInventory());
        ItemStack axe = axe();
        var efficiency = enchantment(context, Enchantments.EFFICIENCY);
        var curse = enchantment(context, Enchantments.VANISHING_CURSE);
        axe.addEnchantment(efficiency, 5);
        axe.addEnchantment(curse, 1);
        handler.getSlot(1).setStack(axe);
        context.assertFalse(handler.getSlot(2).hasStack(), "Second input cannot bypass permission");
        context.assertEquals(((WeaponWorkstation) handler).terracraft$problem(), WeaponWorkstation.PERMISSION_REQUIRED, "Permission message");
        WeaponPermissions.get(context.getWorld().getServer()).grant(player.getUuid(), "terra_axe");
        handler.sendContentUpdates();
        ItemStack output = handler.getSlot(2).getStack();
        context.assertTrue(output.isOf(TerracraftMod.TERRA_AXE), "Single axe can be ground");
        context.assertEquals(output.getEnchantments().getLevel(efficiency), 0, "Remove ordinary enchantments");
        context.assertEquals(output.getEnchantments().getLevel(curse), 1, "Keep curse data");
        handler.getSlot(0).setStack(axe());
        context.assertFalse(handler.getSlot(2).hasStack(), "Do not combine in grindstone");
        context.complete();
    }

    @GameTest public void enchantingTablePermission(TestContext context) {
        var player = player(context, false);
        BlockPos pos = context.getAbsolutePos(new BlockPos(3, 2, 3));
        context.getWorld().setBlockState(pos, Blocks.ENCHANTING_TABLE.getDefaultState());
        var handler = new EnchantmentScreenHandler(1, player.getInventory(), ScreenHandlerContext.create(context.getWorld(), pos));
        handler.getSlot(0).setStack(axe());
        handler.getSlot(1).setStack(new ItemStack(Items.LAPIS_LAZULI, 64));
        player.experienceLevel = 100;
        context.assertFalse(handler.onButtonClick(player, 0), "Denied table cannot enchant");
        context.assertEquals(handler.enchantmentPower[0], 0, "Hide disabled offers");
        context.assertEquals(handler.getLapisCount(), 64, "No lapis consumed");
        player.changeGameMode(GameMode.CREATIVE);
        handler.sendContentUpdates();
        context.assertTrue(handler.enchantmentPower[0] > 0, "Creative restores offers without a right");
        context.assertTrue(handler.onButtonClick(player, 0), "Creative table bypass");
        context.assertTrue(handler.getSlot(0).getStack().hasEnchantments(), "Table actually adds enchantments");
        context.complete();
    }

    @GameTest public void clientSideTableClickUsesSynchronizedPermission(TestContext context) {
        // A non-ServerPlayerEntity exercises the same permission branch as the client GUI.
        var player = context.createMockPlayer(GameMode.SURVIVAL);
        player.experienceLevel = 100;
        var handler = new EnchantmentScreenHandler(1, player.getInventory());
        handler.getSlot(0).setStack(axe());
        handler.getSlot(1).setStack(new ItemStack(Items.LAPIS_LAZULI, 64));
        handler.setProperty(10, 1); // Ten vanilla properties precede the permission property.
        handler.enchantmentPower[0] = 1;
        context.assertTrue(handler.onButtonClick(player, 0), "Licensed client must be able to send click packet");
        handler.setProperty(10, 0);
        context.assertFalse(handler.onButtonClick(player, 0), "Unlicensed client must not send click packet");
        context.complete();
    }

    @GameTest public void craftingRepairCannotBypass(TestContext context) {
        var recipe = new RepairItemRecipe(CraftingRecipeCategory.MISC);
        for (int width : new int[] {2, 3}) {
            var stacks = new java.util.ArrayList<ItemStack>(java.util.Collections.nCopies(width * width, ItemStack.EMPTY));
            stacks.set(0, axe()); stacks.set(1, axe());
            var input = CraftingRecipeInput.create(width, width, stacks);
            context.assertFalse(recipe.matches(input, context.getWorld()), "Repair cannot match");
            context.assertTrue(recipe.craft(input, context.getWorld().getRegistryManager()).isEmpty(), "Direct crafting cannot bypass");
        }
        var vanilla = CraftingRecipeInput.create(2, 1, List.of(new ItemStack(Items.NETHERITE_AXE), new ItemStack(Items.NETHERITE_AXE)));
        context.assertTrue(recipe.matches(vanilla, context.getWorld()), "Vanilla repair stays available");
        context.complete();
    }

    @GameTest public void effectsFollowCurrentUserWithoutDeletingData(TestContext context) {
        var unlicensed = player(context, false);
        var licensed = player(context, true);
        ItemStack axe = axe();
        for (var key : List.of(Enchantments.SILK_TOUCH, Enchantments.MENDING, Enchantments.VANISHING_CURSE)) axe.addEnchantment(enchantment(context, key), 1);
        axe.addEnchantment(enchantment(context, Enchantments.UNBREAKING), 3);
        axe.addEnchantment(enchantment(context, Enchantments.FIRE_ASPECT), 2);
        axe.setDamage(500);
        unlicensed.equipStack(EquipmentSlot.MAINHAND, axe);
        ItemStack held = unlicensed.getMainHandStack();
        context.assertEquals(EnchantmentHelper.getLevel(enchantment(context, Enchantments.SILK_TOUCH), held.copy()), 0, "Copied loot tool must remain restricted");
        context.assertEquals(held.getEnchantments().getSize(), 5, "Stored enchantments are intact");
        for (int i = 0; i < 30; i++) context.assertEquals(EnchantmentHelper.getItemDamage(context.getWorld(), held, 1), 1, "Unbreaking must not reduce wear");
        context.assertTrue(EnchantmentHelper.chooseEquipmentWith(EnchantmentEffectComponentTypes.REPAIR_WITH_XP, unlicensed, ItemStack::isDamaged).isEmpty(), "No mending selection");
        var leaves = Block.getDroppedStacks(Blocks.OAK_LEAVES.getDefaultState(), context.getWorld(), context.getAbsolutePos(new BlockPos(2, 2, 2)), null, unlicensed, held.copy());
        context.assertFalse(leaves.stream().anyMatch(s -> s.isOf(Items.OAK_LEAVES)), "Silk touch loot predicate must be inactive");
        invoke(PlayerEntity.class, "vanishCursedItems", unlicensed);
        context.assertTrue(unlicensed.getMainHandStack().isOf(TerracraftMod.TERRA_AXE), "Vanishing must be inactive");
        var target = context.spawnEntity(EntityType.ZOMBIE, new BlockPos(4, 2, 4));
        EnchantmentHelper.onTargetDamaged(context.getWorld(), target, unlicensed.getDamageSources().playerAttack(unlicensed), held);
        context.assertFalse(target.isOnFire(), "No fire for unlicensed attacker");
        unlicensed.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        licensed.equipStack(EquipmentSlot.MAINHAND, held);
        held = licensed.getMainHandStack();
        context.assertEquals(EnchantmentHelper.getLevel(enchantment(context, Enchantments.SILK_TOUCH), held), 1, "Effects return immediately on transfer");
        context.assertTrue(EnchantmentHelper.chooseEquipmentWith(EnchantmentEffectComponentTypes.REPAIR_WITH_XP, licensed, ItemStack::isDamaged).isPresent(), "Mending restored");
        EnchantmentHelper.onTargetDamaged(context.getWorld(), target, licensed.getDamageSources().playerAttack(licensed), held);
        context.assertTrue(target.isOnFire(), "Fire restored");
        invoke(PlayerEntity.class, "vanishCursedItems", licensed);
        context.assertTrue(licensed.getMainHandStack().isEmpty(), "Curse restored for licensed owner");
        context.complete();
    }

    @GameTest public void efficiencyRefreshesOnPermissionChange(TestContext context) {
        var player = player(context, true);
        ItemStack axe = axe();
        axe.addEnchantment(enchantment(context, Enchantments.EFFICIENCY), 5);
        player.equipStack(EquipmentSlot.MAINHAND, axe);
        invoke(LivingEntity.class, "sendEquipmentChanges", player);
        context.assertEquals(player.getAttributeValue(EntityAttributes.MINING_EFFICIENCY), 26.0, "Efficiency V applied");
        WeaponPermissions.get(context.getWorld().getServer()).revoke(player.getUuid(), "terra_axe");
        invoke(LivingEntity.class, "sendEquipmentChanges", player);
        context.assertEquals(player.getAttributeValue(EntityAttributes.MINING_EFFICIENCY), 0.0, "Revoke removes old attribute");
        WeaponPermissions.get(context.getWorld().getServer()).grant(player.getUuid(), "terra_axe");
        invoke(LivingEntity.class, "sendEquipmentChanges", player);
        context.assertEquals(player.getAttributeValue(EntityAttributes.MINING_EFFICIENCY), 26.0, "Grant reapplies attribute");
        player.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        invoke(LivingEntity.class, "sendEquipmentChanges", player);
        context.assertEquals(player.getAttributeValue(EntityAttributes.MINING_EFFICIENCY), 0.0, "Unequip removes all modifiers");
        context.complete();
    }

    @GameTest(maxTicks = 40) public void fellingOnlyConsumesLogsAndKeepsSilkTouch(TestContext context) {
        var player = player(context, true);
        ItemStack axe = axe();
        axe.addEnchantment(enchantment(context, Enchantments.SILK_TOUCH), 1);
        player.equipStack(EquipmentSlot.MAINHAND, axe);
        for (int y = 2; y <= 4; y++) context.setBlockState(3, y, 3, Blocks.OAK_LOG);
        context.setBlockState(3, 5, 3, Blocks.OAK_LEAVES);
        context.assertTrue(player.interactionManager.tryBreakBlock(context.getAbsolutePos(new BlockPos(3, 2, 3))), "Start felling");
        context.waitAndRun(3, () -> {
            context.expectBlock(Blocks.AIR, 3, 4, 3);
            context.expectBlock(Blocks.AIR, 3, 5, 3);
            context.assertEquals(axe.getDamage(), 3, "Three logs cost three durability; leaves cost none");
            context.expectItem(Items.OAK_LEAVES);
            context.complete();
        });
    }

    @GameTest(maxTicks = 40) public void fellingStopsAtOneButDirectBreakCanDestroy(TestContext context) {
        var player = player(context, true);
        ItemStack axe = axe();
        axe.setDamage(axe.getMaxDamage() - 2);
        player.equipStack(EquipmentSlot.MAINHAND, axe);
        context.setBlockState(3, 2, 3, Blocks.OAK_LOG);
        context.setBlockState(3, 3, 3, Blocks.OAK_LOG);
        player.interactionManager.tryBreakBlock(context.getAbsolutePos(new BlockPos(3, 2, 3)));
        context.waitAndRun(3, () -> {
            context.expectBlock(Blocks.OAK_LOG, 3, 3, 3);
            context.assertEquals(axe.getMaxDamage() - axe.getDamage(), 1, "Queue protects last durability");
            player.setSneaking(true);
            player.interactionManager.tryBreakBlock(context.getAbsolutePos(new BlockPos(3, 3, 3)));
            context.assertTrue(player.getMainHandStack().isEmpty(), "Direct mining can still break the axe");
            context.complete();
        });
    }
}
