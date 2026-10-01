package jp.shint.terracraft;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** A Nether-themed offering screen with a fixed, player-filled material row. */
public final class AltarScreen extends HandledScreen<AltarScreenHandler> {
    private static final Identifier NETHER_VISTA_TEXTURE = Identifier.of("terracraft", "textures/gui/altar_nether_vista.png");
    private int ritualX, ritualY;
    public AltarScreen(AltarScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = 300;
        backgroundHeight = 260;
    }
    @Override protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int left = x, top = y;
        context.fill(0, 0, width, height, 0x55000000);
        context.fill(left, top, left + backgroundWidth, top + backgroundHeight, 0xFF19090E);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, NETHER_VISTA_TEXTURE, left + 3, top + 3,
                0, 0, backgroundWidth - 6, backgroundHeight - 6, 1448, 1086, 1448, 1086);
        context.fill(left + 3, top + 3, left + backgroundWidth - 3, top + backgroundHeight - 3, 0x3309000D);
        drawPanelFrame(context, left, top, backgroundWidth, backgroundHeight);
        context.fill(left + 10, top + 39, left + 290, top + 147, 0xA82A1119);
        context.fill(left + 12, top + 41, left + 288, top + 43, 0xFFC5481A);
        context.fill(left + 12, top + 144, left + 288, top + 146, 0xFF6E2118);
        drawInventoryPanel(context, left, top);
        // The five fixed offering slots are backed by matching, item-restricted server slots.
        ItemStack[] guides = {new ItemStack(Items.NETHERITE_INGOT), new ItemStack(Items.BLAZE_ROD),
                new ItemStack(Items.MAGMA_CREAM), new ItemStack(Items.CRYING_OBSIDIAN), new ItemStack(Items.NETHER_STAR)};
        int[] guideCounts = {2, 16, 16, 8, 1};
        int[][] guidePositions = {{62,64},{101,64},{140,64},{179,64},{218,64}};
        for (int i = 0; i < guides.length; i++) {
            ItemStack supplied = handler.getSlot(i).getStack();
            boolean fulfilled = supplied.isOf(guides[i].getItem()) && supplied.getCount() >= guideCounts[i];
            int frameX = left + guidePositions[i][0] - 12;
            int frameY = top + guidePositions[i][1] - 12;
            drawHotbarFrame(context, frameX, frameY, 40, fulfilled ? 0xFF9C4F20 : 0xFF49262A);
            context.drawItem(guides[i], left + guidePositions[i][0], top + guidePositions[i][1]);
            context.fill(left + guidePositions[i][0], top + guidePositions[i][1], left + guidePositions[i][0] + 16, top + guidePositions[i][1] + 16, 0x66000000);
            String count = supplied.isOf(guides[i].getItem()) ? supplied.getCount() + "\u002F" + guideCounts[i] : "0\u002F" + guideCounts[i];
            int countX = left + guidePositions[i][0] + 28 - textRenderer.getWidth(count);
            context.drawText(textRenderer, count, countX, top + guidePositions[i][1] + 20, fulfilled ? 0xFFFFC36A : 0xFFE6D7CA, true);
        }
        ritualX = left + 92; ritualY = top + 119;
        boolean ready = hasAllOfferings();
        context.fill(ritualX, ritualY, ritualX + 116, ritualY + 24, ready ? 0xFF9B4A19 : 0xFF3D3032);
        context.fill(ritualX + 2, ritualY + 2, ritualX + 114, ritualY + 22, ready ? 0xFFC46725 : 0xFF5A4A4B);
    }
    private static void drawHotbarFrame(DrawContext context, int frameX, int frameY, int size, int fillColor) {
        context.fill(frameX, frameY, frameX + size, frameY + size, 0xFF1A1118);
        context.fill(frameX + 3, frameY + 3, frameX + size - 3, frameY + size - 3, 0xFF6D565E);
        context.fill(frameX + 6, frameY + 6, frameX + size - 6, frameY + size - 6, fillColor);
    }
    private static void drawInventoryPanel(DrawContext context, int left, int top) {
        // The player inventory shares the altar's stone-and-ember styling instead of using the vanilla sheet.
        context.fill(left + 62, top + 157, left + 238, top + 221, 0xA01A080D);
        context.fill(left + 64, top + 159, left + 236, top + 219, 0x804B1921);
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) {
            drawInventorySlot(context, left + 70 + column * 18, top + 167 + row * 18);
        }
        context.fill(left + 62, top + 223, left + 238, top + 244, 0xA01A080D);
        context.fill(left + 64, top + 225, left + 236, top + 242, 0x805A1E20);
        for (int column = 0; column < 9; column++) drawInventorySlot(context, left + 70 + column * 18, top + 225);
    }
    private static void drawInventorySlot(DrawContext context, int slotX, int slotY) {
        context.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0xFF160E15);
        context.fill(slotX + 1, slotY + 1, slotX + 16, slotY + 16, 0xFF5F4A55);
        context.fill(slotX + 3, slotY + 3, slotX + 15, slotY + 15, 0xFF2B1E28);
    }
    private static void drawPanelFrame(DrawContext context, int frameX, int frameY, int frameWidth, int frameHeight) {
        int border = 0xFF6D565E;
        context.fill(frameX, frameY, frameX + frameWidth, frameY + 2, border);
        context.fill(frameX, frameY + frameHeight - 2, frameX + frameWidth, frameY + frameHeight, border);
        context.fill(frameX, frameY, frameX + 2, frameY + frameHeight, border);
        context.fill(frameX + frameWidth - 2, frameY, frameX + frameWidth, frameY + frameHeight, border);
    }
    @Override protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        int titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;
        context.drawText(textRenderer, title, titleX, 16, 0xFFF4D9C1, true);
        context.drawText(textRenderer, Text.translatable("screen.terracraft.altar.offerings"), 12, 46, 0xFFE6A651, false);
        context.drawText(textRenderer, Text.translatable("container.inventory"), 62, 150, 0xFFE6A651, false);
        Text begin = Text.translatable("screen.terracraft.altar.begin");
        int buttonTextX = ritualX - x + (116 - textRenderer.getWidth(begin)) / 2;
        context.drawText(textRenderer, begin, buttonTextX, 127, hasAllOfferings() ? 0xFFFFFFFF : 0xFF968889, true);
    }
    @Override public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        if (click.button() == 1 && client.options.sneakKey.isPressed() && client.player != null) {
            int localX = (int) click.x() - x;
            int localY = (int) click.y() - y;
            for (int index = 5; index < handler.slots.size(); index++) {
                net.minecraft.screen.slot.Slot slot = handler.getSlot(index);
                if (localX >= slot.x && localX < slot.x + 16 && localY >= slot.y && localY < slot.y + 16) {
                    client.interactionManager.clickSlot(handler.syncId, index, 0,
                            net.minecraft.screen.slot.SlotActionType.QUICK_MOVE, client.player);
                    return true;
                }
            }
        }
        if (click.button() == 0 && hasAllOfferings() && click.x() >= ritualX && click.x() < ritualX + 116 && click.y() >= ritualY && click.y() < ritualY + 24) {
            client.interactionManager.clickButton(handler.syncId, 0);
            client.player.closeHandledScreen();
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    private boolean hasAllOfferings() {
        Item[] items = {Items.NETHERITE_INGOT, Items.BLAZE_ROD, Items.MAGMA_CREAM, Items.CRYING_OBSIDIAN, Items.NETHER_STAR};
        int[] counts = {2, 16, 16, 8, 1};
        for (int i = 0; i < items.length; i++) {
            ItemStack stack = handler.getSlot(i).getStack();
            if (!stack.isOf(items[i]) || stack.getCount() < counts[i]) return false;
        }
        return true;
    }
}
