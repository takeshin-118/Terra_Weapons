package jp.shint.terracraft.jei;

import java.lang.reflect.Method;
import jp.shint.terracraft.TerracraftMod;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.inputs.IJeiInputHandler;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenPos;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

final class CraftPermissionWidget implements IRecipeWidget, IJeiInputHandler {
    private static final Method PERMISSION = findPermissionMethod();
    private static Method findPermissionMethod() {
        try {
            return Class.forName("jp.shint.terracraft.portablerecipe.PortableRecipeClient")
                    .getMethod("hasCraftPermissionForRecipeViewer");
        } catch (ReflectiveOperationException | LinkageError exception) { return null; }
    }
    private boolean unlocked() {
        if (PERMISSION == null) return false;
        try { return (boolean) PERMISSION.invoke(null); }
        catch (ReflectiveOperationException exception) { return false; }
    }
    @Override public ScreenPos getPosition() { return new ScreenPos(64, 0); }
    @Override public ScreenRect getArea() { return new ScreenRect(64, 0, 22, 18); }
    @Override public ScreenRect getScreenRectangle() { return getArea(); }
    @Override public void drawWidget(DrawContext g, double x, double y) {
        boolean allowed = unlocked();
        g.fill(0, 0, 22, 18, 0xFF322C28);
        g.drawStrokedRectangle(0, 0, 22, 18, allowed ? 0xFF75B86C : 0xFFE4A947);
        g.drawItem(new ItemStack(TerracraftMod.TERRA_AXE_PERMISSION), 1, 1);
        if (!allowed) {
            // Pixel padlock, visible on top of the permission star.
            g.fill(13, 4, 19, 11, 0xFF231B15);
            g.fill(14, 5, 18, 10, 0xFFAAAAAA);
            g.fill(15, 7, 17, 10, 0xFF231B15);
            g.fill(11, 9, 21, 17, 0xFF231B15);
            g.fill(12, 10, 20, 16, 0xFFAAAAAA);
            g.fill(15, 11, 17, 14, 0xFF231B15);
        }
    }
    @Override public void getTooltip(ITooltipBuilder tooltip, double x, double y) {
        tooltip.add(TerracraftMod.TERRA_AXE_PERMISSION.getName());
        tooltip.add(Text.literal(unlocked() ? "クラフト権利：取得済み" : "クラフト権利：未取得（クラフト不可）"));
        tooltip.add(Text.literal("クリックで権利取得の儀式を表示"));
    }
    @Override public boolean handleInput(double x, double y, IJeiUserInput input) {
        if (x < 0 || x >= 22 || y < 0 || y >= 18
                || input.getKey().getCategory() != InputUtil.Type.MOUSE || input.getKey().getCode() != 0) return false;
        if (!input.isSimulate()) return TerracraftJeiClientPlugin.openTerraAxeRitual();
        return true;
    }
}
