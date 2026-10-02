package jp.shint.terracraft.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import java.util.ArrayList;
import java.util.List;

final class RitualCategory implements IRecipeCategory<TerracraftJeiPlugin.RitualGuide> {
    static final RecipeType<TerracraftJeiPlugin.RitualGuide> TYPE = RecipeType.create("terracraft", "terra_axe_rights", TerracraftJeiPlugin.RitualGuide.class);
    private static final String[] FLOOR = {"GCRBG", "CBBRM", "RRCMM", "MRBRC", "GMBCG"};
    private static final Block[] FLOOR_BLOCKS = {Blocks.GILDED_BLACKSTONE, Blocks.CRYING_OBSIDIAN,
            Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS, Blocks.MAGMA_BLOCK};
    private static final Item[] OFFERINGS = {Registries.ITEM.get(Identifier.of("minecraft", "netherite_ingot")),
            Registries.ITEM.get(Identifier.of("minecraft", "blaze_rod")), Registries.ITEM.get(Identifier.of("minecraft", "magma_cream")),
            Blocks.CRYING_OBSIDIAN.asItem(), Registries.ITEM.get(Identifier.of("minecraft", "nether_star"))};
    private static final int[] OFFERING_COUNTS = {2, 16, 16, 8, 1};
    private final IDrawable icon;

    RitualCategory(IGuiHelper gui) { icon = gui.createDrawableItemStack(new ItemStack(jp.shint.terracraft.TerracraftMod.ALTAR_CORE_ITEM)); }
    @Override public RecipeType<TerracraftJeiPlugin.RitualGuide> getRecipeType() { return TYPE; }
    @Override public Text getTitle() { return Text.translatable("jei.terracraft.ritual.title"); }
    @Override public int getWidth() { return 214; }
    @Override public int getHeight() { return 184; }
    @Override public IDrawable getIcon() { return icon; }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, TerracraftJeiPlugin.RitualGuide recipe, IFocusGroup focuses) {
        int[] counts = new int[FLOOR_BLOCKS.length];
        for (String row : FLOOR) for (char symbol : row.toCharArray()) counts[blockIndex(symbol)]++;
        for (int i = 0; i < FLOOR_BLOCKS.length; i++) {
            builder.addInputSlot(8 + i * 27, 129).setStandardSlotBackground()
                    .add(new ItemStack(FLOOR_BLOCKS[i], counts[i]));
        }
        builder.addInputSlot(143, 129).setStandardSlotBackground().add(jp.shint.terracraft.TerracraftMod.ALTAR_CORE_ITEM);
        // Informational JEI result only; this does not create a crafting recipe or grant an item.
        builder.addOutputSlot(188, 164).setStandardSlotBackground()
                .add(jp.shint.terracraft.TerracraftMod.TERRA_AXE_PERMISSION)
                .addRichTooltipCallback((slot, tooltip) -> tooltip.add(Text.translatable("jei.terracraft.ritual.steps1")));
        for (int i = 0; i < OFFERINGS.length; i++) {
            builder.addInputSlot(8 + i * 26, 164).setStandardSlotBackground().add(new ItemStack(OFFERINGS[i], OFFERING_COUNTS[i]));
        }
    }

    private static int blockIndex(char symbol) {
        return switch (symbol) { case 'G' -> 0; case 'C' -> 1; case 'B' -> 2; case 'R' -> 3; default -> 4; };
    }

    @Override public void createRecipeExtras(IRecipeExtrasBuilder builder, TerracraftJeiPlugin.RitualGuide recipe, IFocusGroup focuses) {
        List<RitualSceneWidget.Cell> cells = new ArrayList<>();
        for (int z = 0; z < 5; z++) for (int x = 0; x < 5; x++) {
            cells.add(new RitualSceneWidget.Cell(new BlockPos(x, 0, z), FLOOR_BLOCKS[blockIndex(FLOOR[z].charAt(x))].getDefaultState()));
        }
        cells.add(new RitualSceneWidget.Cell(new BlockPos(2, 1, 2), jp.shint.terracraft.TerracraftMod.ALTAR_CORE.getDefaultState()));
        RitualSceneWidget scene = new RitualSceneWidget(cells);
        builder.addWidget(scene);
        builder.addInputHandler(scene);
    }

    @Override public void draw(TerracraftJeiPlugin.RitualGuide recipe, IRecipeSlotsView slots, DrawContext graphics, double mouseX, double mouseY) {
        var font = MinecraftClient.getInstance().textRenderer;
        graphics.drawText(font, Text.translatable("jei.terracraft.scene.materials"), 7, 117, 0xFFFFFFFF, false);
        graphics.drawText(font, Text.translatable("jei.terracraft.ritual.offerings"), 7, 152, 0xFFFFFFFF, false);
        graphics.drawText(font, Text.translatable("jei.terracraft.scene.rights"), 180, 152, 0xFFFFCC66, false);
        graphics.drawText(font, ">", 162, 168, 0xFFFFCC66, false);
    }
}
