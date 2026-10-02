package jp.shint.terracraft;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.block.Blocks;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

/** Central registration point. Change provisional IDs here before a production world is created. */
public final class TerracraftMod implements ModInitializer {
    public static final String MOD_ID = "terracraft";
    public static final int TERRA_AXE_MAX_DAMAGE = 4062;
    public static final Item TERRA_AXE = registerItem("terra_axe", new AxeItem(ToolMaterial.NETHERITE, 11.0F, -3.3333333F,
            settings("terra_axe").maxCount(1).maxDamage(TERRA_AXE_MAX_DAMAGE).repairable(Items.NETHERITE_INGOT)));
    public static final Block ALTAR_CORE = registerBlock("altar_core", new AltarCoreBlock(AbstractBlock.Settings.copy(Blocks.OBSIDIAN)
            .registryKey(RegistryKey.of(RegistryKeys.BLOCK, id("altar_core"))).luminance(state -> 10).requiresTool()
            .pistonBehavior(PistonBehavior.BLOCK)));
    public static final BlockEntityType<AltarCoreBlockEntity> ALTAR_CORE_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, id("altar_core"),
            FabricBlockEntityTypeBuilder.create(AltarCoreBlockEntity::new, ALTAR_CORE).build());
    public static final ScreenHandlerType<AltarScreenHandler> ALTAR_SCREEN_HANDLER = Registry.register(Registries.SCREEN_HANDLER,
            id("altar"), new ScreenHandlerType<>(AltarScreenHandler::new, FeatureSet.empty()));
    public static final ScreenHandlerType<PortableCraftingScreenHandler> PORTABLE_CRAFTING_SCREEN_HANDLER = Registry.register(Registries.SCREEN_HANDLER,
            id("portable_crafting"), new ScreenHandlerType<>(PortableCraftingScreenHandler::new, FeatureSet.empty()));
    public static final Item ALTAR_CORE_ITEM = registerItem("altar_core", new BlockItem(ALTAR_CORE, settings("altar_core").fireproof()));
    public TerracraftMod() { }
    public static Identifier id(String path) { return Identifier.of(MOD_ID, path); }
    private static Item.Settings settings(String path) { return new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, id(path))).fireproof(); }
    private static Item registerItem(String path, Item item) { return Registry.register(Registries.ITEM, id(path), item); }
    private static Block registerBlock(String path, Block block) { return Registry.register(Registries.BLOCK, id(path), block); }
    @Override public void onInitialize() {
        TerracraftConfig.load();
        TreeFellingManager.initialize();
        PermissionAttributeManager.initialize();
        PortableCrafting.initialize();
        jp.shint.terracraft.portablerecipe.PortableRecipeNetworking.initializeServer();
        AltarSessionManager.initialize();
        AltarRitualManager.initialize();
        PermissionNotifications.initialize();
        TerracraftCommands.initialize();
    }
}
