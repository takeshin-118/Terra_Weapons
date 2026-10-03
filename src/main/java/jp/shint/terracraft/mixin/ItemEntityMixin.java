package jp.shint.terracraft.mixin;

import jp.shint.terracraft.TerracraftMod;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Preserves dropped Terracraft weapons for ten real minutes. */
@Mixin(ItemEntity.class)
abstract class ItemEntityMixin {
    private static final int TERRACRAFT_MAX_DROP_AGE = 12_000;
    @Shadow private int itemAge;
    @Shadow private int pickupDelay;
    @Unique private int terracraft$dropAge;

    @Unique
    private boolean terracraft$isProtectedWeapon() {
        return ((ItemEntity) (Object) this).getStack().isOf(TerracraftMod.TERRA_AXE);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void terracraft$keepDroppedWeapon(CallbackInfo ci) {
        if (!terracraft$isProtectedWeapon()) return;
        // /give creates an unpickable visual copy with age 5999. Let vanilla
        // remove it instead of resetting its age and keeping a ghost axe alive.
        if (pickupDelay == 32_767) return;
        if (++terracraft$dropAge >= TERRACRAFT_MAX_DROP_AGE) {
            ((ItemEntity) (Object) this).discard();
            return;
        }
        if (itemAge >= 5_999) itemAge = 0;
    }

    @Inject(method = "isFireImmune", at = @At("HEAD"), cancellable = true)
    private void terracraft$makeWeaponFireproof(CallbackInfoReturnable<Boolean> cir) {
        if (terracraft$isProtectedWeapon()) cir.setReturnValue(true);
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void terracraft$rejectFireAndLava(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (terracraft$isProtectedWeapon() && source.isIn(DamageTypeTags.IS_FIRE)) cir.setReturnValue(false);
    }

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float terracraft$increaseWeaponDurability(float amount) {
        return terracraft$isProtectedWeapon() ? amount / 10.0F : amount;
    }

    @Inject(method = "writeCustomData", at = @At("TAIL"))
    private void terracraft$writeDropAge(WriteView view, CallbackInfo ci) {
        if (terracraft$isProtectedWeapon()) view.putInt("terracraft_drop_age", terracraft$dropAge);
    }

    @Inject(method = "readCustomData", at = @At("TAIL"))
    private void terracraft$readDropAge(ReadView view, CallbackInfo ci) {
        terracraft$dropAge = view.getInt("terracraft_drop_age", 0);
    }
}
