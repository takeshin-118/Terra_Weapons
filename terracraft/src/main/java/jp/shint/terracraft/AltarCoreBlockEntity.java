package jp.shint.terracraft;

import java.util.UUID;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;

/** Persistent server-authoritative lock for a ritual running at an altar core. */
public final class AltarCoreBlockEntity extends BlockEntity {
    /** Persisted offering storage. It is intentionally not exposed to hoppers. */
    private final SimpleInventory offerings = new SimpleInventory(27) {
        @Override public void markDirty() {
            super.markDirty();
            AltarCoreBlockEntity.this.markDirty();
        }
    };
    private UUID ritualPlayer;

    public AltarCoreBlockEntity(BlockPos pos, BlockState state) {
        super(TerracraftMod.ALTAR_CORE_ENTITY, pos, state);
    }

    public boolean begin(UUID player) {
        if (ritualPlayer != null && !ritualPlayer.equals(player)) return false;
        ritualPlayer = player;
        markDirty();
        return true;
    }

    public boolean belongsTo(UUID player) { return player.equals(ritualPlayer); }

    public SimpleInventory offerings() { return offerings; }

    public void end(UUID player) {
        if (belongsTo(player)) {
            ritualPlayer = null;
            markDirty();
        }
    }

    @Override protected void readData(ReadView view) {
        super.readData(view);
        view.getOptionalTypedListView("offerings", ItemStack.CODEC).ifPresent(offerings::readDataList);
    }

    @Override protected void writeData(WriteView view) {
        super.writeData(view);
        offerings.toDataList(view.getListAppender("offerings", ItemStack.CODEC));
    }
}
