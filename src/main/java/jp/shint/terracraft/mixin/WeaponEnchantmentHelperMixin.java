package jp.shint.terracraft.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import jp.shint.terracraft.TerracraftMod;
import jp.shint.terracraft.WeaponEnchantments;
import net.minecraft.component.ComponentType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentEffectContext;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Mixin(EnchantmentHelper.class)
abstract class WeaponEnchantmentHelperMixin {
    @Inject(method = "forEachEnchantment(Lnet/minecraft/item/ItemStack;Lnet/minecraft/enchantment/EnchantmentHelper$Consumer;)V", at = @At("HEAD"), cancellable = true)
    private static void terracraft$filterEffects(ItemStack stack, @Coerce Object consumer, CallbackInfo ci) {
        if (WeaponEnchantments.restricted(stack)) ci.cancel();
    }

    @Inject(method = "forEachEnchantment(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EquipmentSlot;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/enchantment/EnchantmentHelper$ContextAwareConsumer;)V", at = @At("HEAD"), cancellable = true)
    private static void terracraft$filterContextEffects(ItemStack stack, EquipmentSlot slot, LivingEntity user, @Coerce Object consumer, CallbackInfo ci) {
        if (WeaponEnchantments.restricted(stack, user)) ci.cancel();
    }

    @Inject(method = "getLevel", at = @At("HEAD"), cancellable = true)
    private static void terracraft$filterLevel(RegistryEntry<Enchantment> enchantment, ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (WeaponEnchantments.restricted(stack)) cir.setReturnValue(0);
    }

    @Inject(method = "hasAnyEnchantmentsIn", at = @At("HEAD"), cancellable = true)
    private static void terracraft$filterTags(ItemStack stack, TagKey<Enchantment> tag, CallbackInfoReturnable<Boolean> cir) {
        if (WeaponEnchantments.restricted(stack)) cir.setReturnValue(false);
    }

    @WrapMethod(method = "chooseEquipmentWith")
    private static Optional<EnchantmentEffectContext> terracraft$filterRepair(ComponentType<?> type, LivingEntity user,
            Predicate<ItemStack> predicate, Operation<Optional<EnchantmentEffectContext>> original) {
        return original.call(type, user, predicate.and(stack -> !WeaponEnchantments.restricted(stack, user)));
    }

    @WrapMethod(method = "getPossibleEntries")
    private static List<EnchantmentLevelEntry> terracraft$fireAspectOffers(int power, ItemStack stack,
            Stream<RegistryEntry<Enchantment>> candidates, Operation<List<EnchantmentLevelEntry>> original) {
        if (!stack.isOf(TerracraftMod.TERRA_AXE)) return original.call(power, stack, candidates);
        List<EnchantmentLevelEntry> result = new ArrayList<>();
        candidates.filter(entry -> entry.value().isPrimaryItem(stack)
                || entry.matchesKey(Enchantments.FIRE_ASPECT) && entry.value().isAcceptableItem(stack)).forEach(entry -> {
            Enchantment enchantment = entry.value();
            for (int level = enchantment.getMaxLevel(); level >= enchantment.getMinLevel(); level--) {
                if (power >= enchantment.getMinPower(level) && power <= enchantment.getMaxPower(level)) {
                    result.add(new EnchantmentLevelEntry(entry, level));
                    break;
                }
            }
        });
        return result;
    }
}
