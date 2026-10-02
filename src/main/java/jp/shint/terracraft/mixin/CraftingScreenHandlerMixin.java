package jp.shint.terracraft.mixin;

import jp.shint.terracraft.TerracraftMod;
import jp.shint.terracraft.WeaponPermissions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A recipe-book lock alone is cosmetic; remove unlicensed manual-crafting output too. */
@Mixin(CraftingScreenHandler.class)
abstract class CraftingScreenHandlerMixin {
    @Shadow protected abstract PlayerEntity getPlayer();

    @Inject(method = "onContentChanged", at = @At("TAIL"))
    private void terracraft$removeUnlicensedWeaponOutput(Inventory inventory, CallbackInfo ci) {
        ItemStack result = ((CraftingScreenHandler) (Object) this).getOutputSlot().getStack();
        if (result.isOf(TerracraftMod.TERRA_AXE) && getPlayer() instanceof ServerPlayerEntity player
                && !WeaponPermissions.hasTerraAxe(player)) {
            ((CraftingScreenHandler) (Object) this).getOutputSlot().setStack(ItemStack.EMPTY);
        }
    }
}
