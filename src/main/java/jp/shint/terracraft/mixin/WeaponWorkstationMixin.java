package jp.shint.terracraft.mixin;

import jp.shint.terracraft.TerracraftMod;
import jp.shint.terracraft.WeaponPermissions;
import jp.shint.terracraft.WeaponWorkstation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.*;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({AnvilScreenHandler.class, GrindstoneScreenHandler.class, EnchantmentScreenHandler.class})
abstract class WeaponWorkstationMixin extends ScreenHandler implements WeaponWorkstation {
    @Unique private PlayerEntity terracraft$player;
    @Unique private Property terracraft$permission;

    protected WeaponWorkstationMixin(ScreenHandlerType<?> type, int syncId) { super(type, syncId); }

    @Inject(method = "<init>(ILnet/minecraft/entity/player/PlayerInventory;Lnet/minecraft/screen/ScreenHandlerContext;)V", at = @At("RETURN"))
    private void terracraft$initialize(int syncId, PlayerInventory inventory, ScreenHandlerContext context, CallbackInfo ci) {
        terracraft$player = inventory.player;
        terracraft$permission = addProperty(Property.create());
        if (terracraft$player instanceof ServerPlayerEntity player) {
            terracraft$permission.set(WeaponPermissions.hasTerraAxe(player) ? 1 : 0);
        }
    }

    @Override public boolean terracraft$allowed() {
        if (terracraft$player instanceof ServerPlayerEntity player) return WeaponPermissions.hasTerraAxe(player);
        return terracraft$player != null && (terracraft$player.isCreative()
                || terracraft$permission != null && terracraft$permission.get() != 0);
    }

    @Override public int terracraft$problem() {
        if (slots.size() < 2) return NONE;
        boolean first = getSlot(0).getStack().isOf(TerracraftMod.TERRA_AXE);
        boolean second = !((Object) this instanceof EnchantmentScreenHandler)
                && getSlot(1).getStack().isOf(TerracraftMod.TERRA_AXE);
        if (!(first || second)) return NONE;
        if (!terracraft$allowed()) return PERMISSION_REQUIRED;
        return first && second ? TOO_HARD : NONE;
    }

    @Override public void terracraft$refreshPermission() {
        if (terracraft$permission == null || !(terracraft$player instanceof ServerPlayerEntity player)) return;
        int allowed = WeaponPermissions.hasTerraAxe(player) ? 1 : 0;
        if (terracraft$permission.get() != allowed) {
            // Set first: recalculation may call sendContentUpdates recursively.
            terracraft$permission.set(allowed);
            onContentChanged(getSlot(0).inventory);
        }
    }
}
