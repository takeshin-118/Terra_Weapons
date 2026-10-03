package jp.shint.terracraft;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/** Filter effect evaluation, never the stored enchantments used by saves, tooltips or workstations. */
public final class WeaponEnchantments {
    private static final ThreadLocal<Boolean> UNFILTERED = ThreadLocal.withInitial(() -> false);
    private WeaponEnchantments() { }

    public static void bind(ItemStack stack, LivingEntity user) {
        if (stack.isOf(TerracraftMod.TERRA_AXE)) ((EnchantmentUser) (Object) stack).terracraft$setEnchantmentUser(user);
    }

    public static boolean restricted(ItemStack stack) {
        return stack.isOf(TerracraftMod.TERRA_AXE)
                && restricted(stack, ((EnchantmentUser) (Object) stack).terracraft$getEnchantmentUser());
    }

    public static boolean restricted(ItemStack stack, LivingEntity user) {
        return !UNFILTERED.get() && stack.isOf(TerracraftMod.TERRA_AXE)
                && user instanceof ServerPlayerEntity player && !WeaponPermissions.hasTerraAxe(player);
    }

    public static ItemStack lootTool(ItemStack stack) {
        if (!restricted(stack)) return stack;
        ItemStack copy = stack.copy();
        copy.set(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        return copy;
    }

    /** Removal must enumerate old modifiers even after their owner's permission was revoked. */
    public static void withoutFiltering(Runnable action) {
        boolean previous = UNFILTERED.get();
        UNFILTERED.set(true);
        try { action.run(); } finally { UNFILTERED.set(previous); }
    }
}
