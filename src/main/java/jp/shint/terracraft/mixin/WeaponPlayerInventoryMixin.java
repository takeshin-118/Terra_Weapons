package jp.shint.terracraft.mixin;

import jp.shint.terracraft.WeaponEnchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInventory.class)
abstract class WeaponPlayerInventoryMixin {
    @Shadow @Final public PlayerEntity player;
    @Inject(method = {"getStack", "getSelectedStack"}, at = @At("RETURN"))
    private void terracraft$bindUser(CallbackInfoReturnable<ItemStack> cir) {
        WeaponEnchantments.bind(cir.getReturnValue(), player);
    }
}
