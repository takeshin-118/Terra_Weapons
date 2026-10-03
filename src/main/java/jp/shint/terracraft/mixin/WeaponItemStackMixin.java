package jp.shint.terracraft.mixin;

import java.lang.ref.WeakReference;
import jp.shint.terracraft.EnchantmentUser;
import jp.shint.terracraft.TerracraftMod;
import jp.shint.terracraft.TreeFellingManager;
import jp.shint.terracraft.WeaponEnchantments;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
abstract class WeaponItemStackMixin implements EnchantmentUser {
    @Unique private WeakReference<LivingEntity> terracraft$user;
    @Override public LivingEntity terracraft$getEnchantmentUser() { return terracraft$user == null ? null : terracraft$user.get(); }
    @Override public void terracraft$setEnchantmentUser(LivingEntity user) {
        if (terracraft$getEnchantmentUser() != user) terracraft$user = user == null ? null : new WeakReference<>(user);
    }

    @Inject(method = "copy", at = @At("RETURN"))
    private void terracraft$copyContext(CallbackInfoReturnable<ItemStack> cir) {
        WeaponEnchantments.bind(cir.getReturnValue(), terracraft$getEnchantmentUser());
    }

    @Inject(method = "calculateDamage", at = @At("HEAD"))
    private void terracraft$damageUser(int amount, ServerWorld world, ServerPlayerEntity player, CallbackInfoReturnable<Integer> cir) {
        WeaponEnchantments.bind((ItemStack) (Object) this, player);
    }

    @Inject(method = "postMine", at = @At("HEAD"), cancellable = true)
    private void terracraft$freeQueuedFoliage(World world, BlockState state, BlockPos pos, PlayerEntity player, CallbackInfo ci) {
        if (((ItemStack) (Object) this).isOf(TerracraftMod.TERRA_AXE) && TreeFellingManager.skipsQueuedDurability()) ci.cancel();
    }
}
