package jp.shint.terracraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import jp.shint.terracraft.WeaponEnchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.MatchToolLootCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Silk-touch loot predicates read the component directly instead of EnchantmentHelper.getLevel. */
@Mixin(MatchToolLootCondition.class)
abstract class WeaponLootConditionMixin {
    @ModifyExpressionValue(method = "test(Lnet/minecraft/loot/context/LootContext;)Z", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/loot/context/LootContext;get(Lnet/minecraft/util/context/ContextParameter;)Ljava/lang/Object;"))
    private Object terracraft$filterLootTool(Object value) {
        return value instanceof ItemStack stack ? WeaponEnchantments.lootTool(stack) : value;
    }
}
