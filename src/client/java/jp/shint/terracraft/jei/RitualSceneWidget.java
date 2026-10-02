package jp.shint.terracraft.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.inputs.IJeiInputHandler;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenPos;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import org.joml.Matrix3x2f;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** A rotatable orthographic scene, using baked block geometry and the live block atlas. */
final class RitualSceneWidget implements IRecipeWidget, IJeiInputHandler {
    record Cell(BlockPos pos, BlockState state) { }
    private record Face(Cell cell, BakedQuad quad, RitualProjection.Point[] points, float depth) { }
    private final List<Cell> cells;
    private List<Face> faces = List.of();
    private double yaw = Math.PI / 4;
    private boolean autoRotate = true, floorOnly;
    private long lastFrame = System.nanoTime();
    private static final int[] BUTTON_X = {4, 106};
    private static final int[] BUTTON_W = {96, 96};

    RitualSceneWidget(List<Cell> cells) { this.cells = List.copyOf(cells); }
    @Override public ScreenPos getPosition() { return new ScreenPos(4, 3); }
    @Override public ScreenRect getArea() { return new ScreenRect(4, 3, 206, 110); }
    @Override public ScreenRect getScreenRectangle() { return getArea(); }

    @Override public void drawWidget(DrawContext graphics, double mouseX, double mouseY) {
        long now = System.nanoTime();
        double dt = Math.min((now - lastFrame) / 1_000_000_000.0, 0.05);
        lastFrame = now;
        // Freeze the view under the pointer so the same face stays selectable on mouse-up.
        if (autoRotate && !(mouseX >= 0 && mouseX < 206 && mouseY >= 0 && mouseY < 92)) yaw += dt * 0.18;
        graphics.fill(0, 0, 206, 110, 0xFF171D26);
        graphics.drawStrokedRectangle(0, 0, 206, 110, 0xFF596273);
        graphics.enableScissor(1, 1, 205, 91);
        try {
            faces = projectFaces();
            Cell hovered = pick(mouseX, mouseY);
            for (Face face : faces) drawFace(graphics, face, face.cell.equals(hovered));
        } finally { graphics.disableScissor(); }
        var font = MinecraftClient.getInstance().textRenderer;
        graphics.drawText(font, "5 x 5", 5, 5, 0xFFB7C4D6, false);
        for (int i = 0; i < BUTTON_X.length; i++) {
            int x = BUTTON_X[i], width = BUTTON_W[i];
            boolean hover = mouseY >= 93 && mouseY < 108 && mouseX >= x && mouseX < x + width;
            graphics.fill(x, 93, x + width, 108, hover ? 0xFF53677E : 0xFF303E50);
            Text label = Text.translatable("jei.terracraft.scene." + (i == 0
                    ? (floorOnly ? "all" : "floor") : (autoRotate ? "pause" : "play")));
            graphics.drawText(font, label, x + (width - font.getWidth(label)) / 2, 97, 0xFFF0D5A7, false);
        }
    }

    private List<Face> projectFaces() {
        var camera = new RitualProjection(yaw);
        var renderer = MinecraftClient.getInstance().getBlockRenderManager();
        List<Face> result = new ArrayList<>();
        for (Cell cell : cells) {
            if (floorOnly && cell.pos.getY() > 0) continue;
            var parts = renderer.getModel(cell.state).getParts(Random.create(cell.pos.asLong()));
            for (var part : parts) {
                for (Direction direction : Direction.values()) {
                    if (cells.stream().anyMatch(other -> (!floorOnly || other.pos.getY() == 0)
                            && other.pos.equals(cell.pos.offset(direction)))) continue;
                    for (BakedQuad quad : part.getQuads(direction)) addFace(result, camera, cell, quad);
                }
                for (BakedQuad quad : part.getQuads(null)) addFace(result, camera, cell, quad);
            }
        }
        result.sort(Comparator.comparingDouble(Face::depth));
        return result;
    }

    private static void addFace(List<Face> faces, RitualProjection camera, Cell cell, BakedQuad quad) {
        var normal = quad.face().getFloatVector();
        if (camera.facing(normal.x(), normal.y(), normal.z()) <= 0.001) return;
        var points = new RitualProjection.Point[4];
        float depth = 0;
        for (int i = 0; i < 4; i++) {
            var vertex = quad.getPosition(i);
            points[i] = camera.project(vertex.x() + cell.pos.getX() - 2.5F,
                    vertex.y() + cell.pos.getY(), vertex.z() + cell.pos.getZ() - 2.5F);
            depth += points[i].depth();
        }
        faces.add(new Face(cell, quad, points, depth / 4));
    }

    private static void drawFace(DrawContext graphics, Face face, boolean hovered) {
        // Project the actual face UV basis to the GUI affine matrix. This preserves the
        // model's UV rotations and resource-pack textures instead of drawing item icons.
        float[] u = new float[4], v = new float[4];
        float minU = Float.POSITIVE_INFINITY, minV = minU, maxU = Float.NEGATIVE_INFINITY, maxV = maxU;
        for (int i = 0; i < 4; i++) {
            long uv = face.quad.getTexcoords(i);
            u[i] = net.minecraft.client.util.math.Vector2f.getX(uv);
            v[i] = net.minecraft.client.util.math.Vector2f.getY(uv);
            minU = Math.min(minU, u[i]); maxU = Math.max(maxU, u[i]);
            minV = Math.min(minV, v[i]); maxV = Math.max(maxV, v[i]);
        }
        if (maxU - minU < 1e-7F || maxV - minV < 1e-7F) return;
        for (int i = 0; i < 4; i++) { u[i] = (u[i] - minU) / (maxU - minU); v[i] = (v[i] - minV) / (maxV - minV); }
        float[] transform = RitualProjection.faceTransform(face.points, u, v);
        if (transform == null) return;
        graphics.getMatrices().pushMatrix();
        try {
            graphics.getMatrices().mul(new Matrix3x2f(transform[0], transform[1], transform[2],
                    transform[3], transform[4], transform[5]));
            graphics.drawTexturedQuad(face.quad.sprite().getAtlasId(), 0, 0, 1, 1, minU, maxU, minV, maxV);
            int shade = switch (face.quad.face()) { case UP -> 0; case EAST, WEST -> 50; default -> 80; };
            if (shade > 0) graphics.fill(0, 0, 1, 1, shade << 24);
            if (hovered) graphics.fill(0, 0, 1, 1, 0x55FFCB65);
        } finally { graphics.getMatrices().popMatrix(); }
    }

    private Cell pick(double x, double y) {
        if (x < 1 || x >= 205 || y < 1 || y >= 91) return null;
        // Reverse draw order picks the visible surface, never a block behind the core.
        for (int i = faces.size() - 1; i >= 0; i--) if (RitualProjection.contains(faces.get(i).points, x, y)) return faces.get(i).cell;
        return null;
    }

    @Override public void getTooltip(ITooltipBuilder tooltip, double mouseX, double mouseY) {
        Cell cell = pick(mouseX, mouseY);
        if (cell != null) {
            tooltip.add(cell.state.getBlock().getName());
            tooltip.add(Text.translatable("jei.terracraft.scene.position", cell.pos.getX() - 2, cell.pos.getY() - 1, cell.pos.getZ() - 2));
            tooltip.add(Text.translatable("jei.terracraft.scene.click"));
        } else if (mouseY < 92) tooltip.add(Text.translatable("jei.terracraft.scene.controls"));
    }

    @Override public boolean handleInput(double x, double y, IJeiUserInput input) {
        boolean left = input.getKey().getCategory() == InputUtil.Type.MOUSE && input.getKey().getCode() == 0;
        if (left && y >= 93 && y < 108) {
            for (int i = 0; i < BUTTON_X.length; i++) if (x >= BUTTON_X[i] && x < BUTTON_X[i] + BUTTON_W[i]) {
                if (!input.isSimulate()) {
                    if (i == 0) floorOnly = !floorOnly;
                    else autoRotate = !autoRotate;
                }
                return true;
            }
        }
        var runtime = TerracraftJeiClientPlugin.runtime();
        Cell cell = pick(x, y);
        if (runtime == null || cell == null) return false;
        boolean recipes = input.is(runtime.getKeyMappings().getShowRecipe());
        boolean uses = input.is(runtime.getKeyMappings().getShowUses());
        if (!recipes && !uses) return false;
        if (!input.isSimulate()) {
            var focus = runtime.getJeiHelpers().getFocusFactory().createFocus(
                    recipes ? RecipeIngredientRole.OUTPUT : RecipeIngredientRole.INPUT,
                    VanillaTypes.ITEM_STACK, new ItemStack(cell.state.getBlock()));
            runtime.getRecipesGui().show(focus);
        }
        return true;
    }

    @Override public boolean handleMouseScrolled(double x, double y, double dx, double dy) {
        if (y >= 92 || dy == 0) return false;
        yaw += dy * Math.PI / 12; autoRotate = false;
        return true;
    }
}
