package jp.shint.terracraft.mixin;

import jp.shint.terracraft.WeaponWorkstation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
abstract class WorkstationScreenHandlerMixin {
    @Inject(method = "sendContentUpdates", at = @At("HEAD"))
    private void terracraft$refresh(CallbackInfo ci) {
        if ((Object) this instanceof WeaponWorkstation workstation) workstation.terracraft$refreshPermission();
    }

    @Inject(method = "onSlotClick", at = @At("HEAD"))
    private void terracraft$validateBeforeClick(int slot, int button, SlotActionType action, PlayerEntity player, CallbackInfo ci) {
        if ((Object) this instanceof WeaponWorkstation workstation) {
            workstation.terracraft$refreshPermission();
            if (workstation.terracraft$problem() != WeaponWorkstation.NONE
                    && !((Object) this instanceof EnchantmentScreenHandler)) {
                ((ScreenHandler) (Object) this).getSlot(2).setStack(ItemStack.EMPTY);
            }
        }
    }
}
