package jp.shint.terracraft.mixin;

import jp.shint.terracraft.WeaponWorkstation;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
abstract class WeaponWorkstationScreenMixin<T extends ScreenHandler> extends Screen {
    @Shadow protected T handler;
    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow protected int backgroundWidth;

    protected WeaponWorkstationScreenMixin(Text title) { super(title); }

    @Inject(method = "renderMain", at = @At("TAIL"))
    private void terracraft$explainBlockedOperation(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (!(handler instanceof WeaponWorkstation workstation)) return;
        int problem = workstation.terracraft$problem();
        if (problem == WeaponWorkstation.NONE) return;
        Text text = Text.translatable(problem == WeaponWorkstation.PERMISSION_REQUIRED
                ? "message.terracraft.enchant_permission_required" : "message.terracraft.too_hard_to_combine");
        int width = textRenderer.getWidth(text);
        int left = x + (backgroundWidth - width) / 2;
        int top = Math.max(4, y - 18);
        context.fill(left - 5, top - 3, left + width + 5, top + 12, 0xE0201515);
        context.drawTextWithShadow(textRenderer, text, left, top, 0xFFFF8080);
    }
}
