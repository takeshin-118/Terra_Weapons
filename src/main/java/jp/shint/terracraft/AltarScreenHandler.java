package jp.shint.terracraft;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/** A ritual-only handler: no vanilla player-inventory grid is exposed in this UI. */
public final class AltarScreenHandler extends ScreenHandler {
    private static final int OFFERING_SLOTS = 5;
    private static final Item[] REQUIRED_ITEMS = {
            Items.NETHERITE_INGOT, Items.BLAZE_ROD, Items.MAGMA_CREAM, Items.CRYING_OBSIDIAN, Items.NETHER_STAR
    };
    private final Inventory offerings;
    private final PropertyDelegate state;
    private final BlockPos core;

    public AltarScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(27), new ArrayPropertyDelegate(2), null);
    }

    private AltarScreenHandler(int syncId, PlayerInventory playerInventory, Inventory offerings, PropertyDelegate state, BlockPos core) {
        super(TerracraftMod.ALTAR_SCREEN_HANDLER, syncId);
        this.offerings = offerings;
        this.state = state;
        this.core = core;
        checkSize(offerings, 27);
        checkDataCount(state, 2);
        addProperties(state);
        // One horizontal offering row keeps every required material and its target count easy to read.
        for (int slot = 0; slot < OFFERING_SLOTS; slot++) {
            final Item requiredItem = REQUIRED_ITEMS[slot];
            addSlot(new Slot(offerings, slot, 62 + slot * 39, 64) {
                @Override public boolean canInsert(ItemStack stack) { return stack.isOf(requiredItem); }
            });
        }
        // The player's inventory remains available below the ritual area for moving offerings.
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column + row * 9 + 9, 70 + column * 18, 167 + row * 18));
        }
        for (int column = 0; column < 9; column++) addSlot(new Slot(playerInventory, column, 70 + column * 18, 225));
    }

    public static AltarScreenHandler createServer(int syncId, PlayerInventory playerInventory, AltarCoreBlockEntity altar, ServerWorld world, BlockPos core) {
        ArrayPropertyDelegate state = new ArrayPropertyDelegate(2);
        state.set(0, AltarStructure.missingMask(world, core));
        state.set(1, 0);
        return new AltarScreenHandler(syncId, playerInventory, altar.offerings(), state, core.toImmutable());
    }

    public int missingMask() { return state.get(0); }
    @Override public boolean onButtonClick(PlayerEntity player, int id) {
        return id == 0 && core != null && player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer
                && AltarRitualManager.begin(serverPlayer, core);
    }

    @Override public boolean canUse(PlayerEntity player) { return !offerings.isEmpty() || true; }

    @Override public ItemStack quickMove(PlayerEntity player, int index) {
        if (index < OFFERING_SLOTS || index >= slots.size()) return ItemStack.EMPTY;

        Slot source = slots.get(index);
        if (!source.hasStack()) return ItemStack.EMPTY;
        ItemStack sourceStack = source.getStack();
        for (int offering = 0; offering < OFFERING_SLOTS; offering++) {
            if (!sourceStack.isOf(REQUIRED_ITEMS[offering])) continue;

            ItemStack original = sourceStack.copy();
            if (!insertItem(sourceStack, offering, offering + 1, false)) return ItemStack.EMPTY;
            if (sourceStack.isEmpty()) source.setStack(ItemStack.EMPTY);
            else source.markDirty();
            return original;
        }
        return ItemStack.EMPTY;
    }
}
