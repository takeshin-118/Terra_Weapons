package jp.shint.terracraft.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import jp.shint.terracraft.TerracraftMod;
import jp.shint.terracraft.WeaponEnchantments;
import jp.shint.terracraft.WeaponPermissions;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Map;

@Mixin(LivingEntity.class)
abstract class WeaponLivingEntityMixin {
    @Unique private boolean terracraft$previousPermission;
    @Unique private boolean terracraft$permissionChanged;

    @Inject(method = "getEquippedStack", at = @At("RETURN"))
    private void terracraft$bindUser(EquipmentSlot slot, CallbackInfoReturnable<ItemStack> cir) {
        WeaponEnchantments.bind(cir.getReturnValue(), (LivingEntity) (Object) this);
    }

    @Inject(method = "getEquipmentChanges", at = @At("HEAD"))
    private void terracraft$checkPermission(CallbackInfoReturnable<Map<EquipmentSlot, ItemStack>> cir) {
        if ((Object) this instanceof ServerPlayerEntity player) {
            boolean allowed = WeaponPermissions.hasTerraAxe(player);
            terracraft$permissionChanged = allowed != terracraft$previousPermission;
            terracraft$previousPermission = allowed;
        }
    }

    @Inject(method = "areItemsDifferent", at = @At("HEAD"), cancellable = true)
    private void terracraft$refreshAttributes(ItemStack oldStack, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (terracraft$permissionChanged && (oldStack.isOf(TerracraftMod.TERRA_AXE) || stack.isOf(TerracraftMod.TERRA_AXE))) cir.setReturnValue(true);
    }

    @WrapMethod(method = "onEquipmentRemoved")
    private void terracraft$removeAllOldAttributes(ItemStack stack, EquipmentSlot slot, AttributeContainer attributes, Operation<Void> original) {
        WeaponEnchantments.withoutFiltering(() -> original.call(stack, slot, attributes));
    }
}
