package jp.shint.terracraft;

import net.minecraft.entity.LivingEntity;

/** Transient evaluation context only; never serialized into or transferred as item ownership. */
public interface EnchantmentUser {
    LivingEntity terracraft$getEnchantmentUser();
    void terracraft$setEnchantmentUser(LivingEntity user);
}
