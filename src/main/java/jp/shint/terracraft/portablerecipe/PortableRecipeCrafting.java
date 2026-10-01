package jp.shint.terracraft.portablerecipe;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/** Plans the whole transaction before changing the player's inventory. */
final class PortableRecipeCrafting {
    private PortableRecipeCrafting() { }

    static boolean tryCraft(Inventory inventory, Item result) {
        ItemStack[] planned = new ItemStack[inventory.size()];
        for (int slot = 0; slot < planned.length; slot++) {
            planned[slot] = inventory.getStack(slot).copy();
        }
        if (!consume(planned, Items.NETHERITE_INGOT, 3)
                || !consume(planned, Items.BLAZE_ROD, 2)) {
            return false;
        }
        // Armor and offhand slots cannot receive the crafted axe. A slot freed
        // by consuming ingredients can receive it, even in a full inventory.
        for (int slot = 0; slot < Math.min(PlayerInventory.MAIN_SIZE, planned.length); slot++) {
            if (planned[slot].isEmpty()) {
                planned[slot] = new ItemStack(result);
                for (int index = 0; index < planned.length; index++) {
                    inventory.setStack(index, planned[index]);
                }
                inventory.markDirty();
                return true;
            }
        }
        return false;
    }

    private static boolean consume(ItemStack[] stacks, Item ingredient, int amount) {
        for (ItemStack stack : stacks) {
            if (stack.isOf(ingredient)) {
                int taken = Math.min(amount, stack.getCount());
                stack.decrement(taken);
                amount -= taken;
                if (amount == 0) return true;
            }
        }
        return false;
    }
}
