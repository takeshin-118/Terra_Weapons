package jp.shint.terracraft;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

/** Server-authoritative portable recipe selector. Button 0 crafts an altar core. */
public final class PortableCraftingScreenHandler extends ScreenHandler {
    public PortableCraftingScreenHandler(int syncId, PlayerInventory inventory) {
        super(TerracraftMod.PORTABLE_CRAFTING_SCREEN_HANDLER, syncId);
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column + row * 9 + 9, 17 + column * 18, 94 + row * 18));
        }
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 17 + column * 18, 152));
    }
    @Override public boolean canUse(PlayerEntity player) { return true; }
    @Override public ItemStack quickMove(PlayerEntity player, int index) { return ItemStack.EMPTY; }
    @Override public boolean onButtonClick(PlayerEntity player, int id) {
        if (id != 0 || !player.getMainHandStack().isOf(TerracraftMod.TERRA_AXE)) return false;
        Item[] required = {Items.DIAMOND_BLOCK, Items.CRYING_OBSIDIAN, Items.DIAMOND_BLOCK, Items.CRYING_OBSIDIAN,
                Items.NETHER_STAR, Items.CRYING_OBSIDIAN, Items.DIAMOND_BLOCK, Items.CRYING_OBSIDIAN, Items.DIAMOND_BLOCK};
        for (Item item : required) if (count(player.getInventory(), item) < occurrences(required, item)) return false;
        for (Item item : required) removeOne(player.getInventory(), item);
        ItemStack result = new ItemStack(TerracraftMod.ALTAR_CORE_ITEM);
        if (!player.getInventory().insertStack(result)) player.dropItem(result, false);
        sendContentUpdates();
        return true;
    }
    private static int count(PlayerInventory inventory, Item item) {
        int total = 0; for (int i = 0; i < inventory.size(); i++) if (inventory.getStack(i).isOf(item)) total += inventory.getStack(i).getCount(); return total;
    }
    private static int occurrences(Item[] items, Item target) { int total = 0; for (Item item : items) if (item == target) total++; return total; }
    private static void removeOne(PlayerInventory inventory, Item item) {
        for (int i = 0; i < inventory.size(); i++) if (inventory.getStack(i).isOf(item)) { inventory.removeStack(i, 1); return; }
    }
}
