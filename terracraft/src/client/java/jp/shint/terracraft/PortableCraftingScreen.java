package jp.shint.terracraft;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.Click;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

/** Dedicated material list and craft button for the portable altar-core recipe. */
public final class PortableCraftingScreen extends HandledScreen<PortableCraftingScreenHandler> {
    private int buttonX, buttonY;
    public PortableCraftingScreen(PortableCraftingScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title); backgroundWidth = 194; backgroundHeight = 176; playerInventoryTitleY = 82;
    }
    @Override protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.fill(x, y, x + backgroundWidth, y + backgroundHeight, 0xED151021);
        context.fill(x + 8, y + 20, x + 186, y + 75, 0xDD302044);
        buttonX = x + 112; buttonY = y + 46;
        context.fill(buttonX, buttonY, buttonX + 65, buttonY + 18, 0xFF5F3B8A);
    }
    @Override protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(textRenderer, title, 8, 7, 0xFFF5E9FF, false);
        context.drawText(textRenderer, Text.translatable("screen.terracraft.portable.selected"), 12, 27, 0xFFB9E5FF, false);
        context.drawText(textRenderer, Text.translatable("screen.terracraft.portable.materials"), 12, 42, 0xFFE2CCE9, false);
        context.drawText(textRenderer, Text.translatable("screen.terracraft.portable.recipe"), 12, 56, 0xFFE2CCE9, false);
        context.drawText(textRenderer, Text.translatable("screen.terracraft.portable.craft"), 119, 51, 0xFFFFFFFF, false);
    }
    @Override public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0 && click.x() >= buttonX && click.x() < buttonX + 65 && click.y() >= buttonY && click.y() < buttonY + 18) {
            client.interactionManager.clickButton(handler.syncId, 0);
            return true;
        }
        return super.mouseClicked(click, doubled);
    }
}
