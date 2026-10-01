package jp.shint.terracraft.portablerecipe;

import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PortableRecipeCraftingTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
    }

    @Test
    void fullMainInventoryWithEmptyEquipmentDoesNotConsumeMaterials() {
        SimpleInventory inventory = fullInventory();
        inventory.setStack(0, new ItemStack(Items.NETHERITE_INGOT, 4));
        inventory.setStack(1, new ItemStack(Items.BLAZE_ROD, 3));

        assertFalse(PortableRecipeCrafting.tryCraft(inventory, Items.NETHERITE_AXE));
        assertEquals(4, count(inventory, Items.NETHERITE_INGOT));
        assertEquals(3, count(inventory, Items.BLAZE_ROD));
        assertEquals(0, count(inventory, Items.NETHERITE_AXE));
    }

    @Test
    void consumingExactIngredientsFreesAnOutputSlot() {
        SimpleInventory inventory = fullInventory();
        inventory.setStack(0, new ItemStack(Items.NETHERITE_INGOT, 3));
        inventory.setStack(1, new ItemStack(Items.BLAZE_ROD, 2));

        assertTrue(PortableRecipeCrafting.tryCraft(inventory, Items.NETHERITE_AXE));
        assertTrue(inventory.getStack(0).isOf(Items.NETHERITE_AXE));
        assertEquals(0, count(inventory, Items.NETHERITE_INGOT));
        assertEquals(0, count(inventory, Items.BLAZE_ROD));
        assertEquals(1, count(inventory, Items.NETHERITE_AXE));
        assertEquals(34 * 64, count(inventory, Items.STONE));
    }

    @Test
    void missingSecondIngredientLeavesFirstIngredientUntouched() {
        SimpleInventory inventory = new SimpleInventory(41);
        inventory.setStack(0, new ItemStack(Items.NETHERITE_INGOT, 3));
        inventory.setStack(1, new ItemStack(Items.BLAZE_ROD, 1));

        assertFalse(PortableRecipeCrafting.tryCraft(inventory, Items.NETHERITE_AXE));
        assertEquals(3, count(inventory, Items.NETHERITE_INGOT));
        assertEquals(1, count(inventory, Items.BLAZE_ROD));
    }

    @Test
    void splitStacksAndOffhandIngredientsProduceExactlyOneAxe() {
        SimpleInventory inventory = fullInventory();
        inventory.setStack(0, new ItemStack(Items.NETHERITE_INGOT, 1));
        inventory.setStack(1, new ItemStack(Items.NETHERITE_INGOT, 2));
        inventory.setStack(40, new ItemStack(Items.BLAZE_ROD, 2));

        assertTrue(PortableRecipeCrafting.tryCraft(inventory, Items.NETHERITE_AXE));
        assertEquals(1, count(inventory, Items.NETHERITE_AXE));
        assertEquals(0, count(inventory, Items.NETHERITE_INGOT));
        assertEquals(0, count(inventory, Items.BLAZE_ROD));
        assertTrue(inventory.getStack(40).isEmpty());
        assertFalse(PortableRecipeCrafting.tryCraft(inventory, Items.NETHERITE_AXE));
        assertEquals(1, count(inventory, Items.NETHERITE_AXE));
    }

    private static SimpleInventory fullInventory() {
        SimpleInventory inventory = new SimpleInventory(41);
        for (int slot = 0; slot < PlayerInventory.MAIN_SIZE; slot++) {
            inventory.setStack(slot, new ItemStack(Items.STONE, 64));
        }
        return inventory;
    }

    private static int count(SimpleInventory inventory, Item item) {
        int count = 0;
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (inventory.getStack(slot).isOf(item)) count += inventory.getStack(slot).getCount();
        }
        return count;
    }
}
