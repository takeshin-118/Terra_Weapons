package jp.shint.terracraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.function.Predicate;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Validated external altar definition. An invalid reload never replaces the last good definition. */
public final class TerracraftConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("terracraft-config");
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("terracraft").resolve("terra_axe_altar.json");
    private static final Path TREE_FILE = FabricLoader.getInstance().getConfigDir().resolve("terracraft").resolve("tree_felling.json");
    private static volatile AltarDefinition definition = AltarDefinition.defaults();
    private static volatile TreeFellingDefinition treeDefinition = TreeFellingDefinition.defaults();

    private TerracraftConfig() { }
    public static AltarDefinition altar() { return definition; }
    public static TreeFellingDefinition tree() { return treeDefinition; }

    public static boolean load() {
        try {
            ensureTemplate();
            ensureTreeTemplate();
            AltarDefinition loaded = parse(Files.readString(FILE, StandardCharsets.UTF_8));
            TreeFellingDefinition treeLoaded = parseTree(Files.readString(TREE_FILE, StandardCharsets.UTF_8));
            definition = loaded;
            treeDefinition = treeLoaded;
            LOGGER.info("Loaded Terracraft altar definition from {}", FILE);
            return true;
        } catch (Exception exception) {
            LOGGER.error("Terracraft altar configuration is invalid; retaining the previous valid definition: {}", exception.getMessage());
            return false;
        }
    }

    private static void ensureTreeTemplate() throws IOException {
        if (Files.exists(TREE_FILE)) return;
        Files.createDirectories(TREE_FILE.getParent());
        try (InputStream input = TerracraftConfig.class.getResourceAsStream("/data/terracraft/defaults/tree_felling.json")) {
            if (input == null) throw new IOException("Bundled tree-felling template is missing");
            Files.copy(input, TREE_FILE);
        }
    }

    private static TreeFellingDefinition parseTree(String source) {
        JsonObject root = JsonParser.parseString(source).getAsJsonObject();
        return new TreeFellingDefinition(number(root, "max_logs", 128, 1, 512), number(root, "max_foliage", 512, 0, 2048),
                number(root, "per_task_per_tick", 32, 1, 128), number(root, "global_per_tick", 64, 1, 256),
                number(root, "max_tasks", 16, 1, 64), number(root, "max_tasks_per_player", 4, 1, 16),
                bool(root, "include_vines", true), bool(root, "include_cocoa", true), bool(root, "include_nether_foliage", true));
    }

    private static int number(JsonObject root, String key, int fallback, int min, int max) {
        if (!root.has(key)) return fallback;
        int value = root.get(key).getAsInt();
        if (value < min || value > max) throw new IllegalArgumentException(key + " must be between " + min + " and " + max);
        return value;
    }

    private static boolean bool(JsonObject root, String key, boolean fallback) {
        return root.has(key) ? root.get(key).getAsBoolean() : fallback;
    }

    private static void ensureTemplate() throws IOException {
        if (Files.exists(FILE)) return;
        Files.createDirectories(FILE.getParent());
        try (InputStream input = TerracraftConfig.class.getResourceAsStream("/data/terracraft/defaults/terra_axe_altar.json")) {
            if (input == null) throw new IOException("Bundled altar template is missing");
            Files.copy(input, FILE);
        }
    }

    private static AltarDefinition parse(String source) {
        JsonObject root = JsonParser.parseString(source).getAsJsonObject();
        if (!"terra_axe".equals(root.get("weapon_id").getAsString())) throw new IllegalArgumentException("weapon_id must be terra_axe");
        JsonArray rawLayers = root.getAsJsonArray("layers");
        if (rawLayers == null || rawLayers.size() != 2) throw new IllegalArgumentException("layers must contain exactly two layers");
        String[][] layers = new String[2][5];
        for (int y = 0; y < 2; y++) {
            JsonArray layer = rawLayers.get(y).getAsJsonArray();
            if (layer.size() != 5) throw new IllegalArgumentException("each layer must be 5 rows");
            for (int row = 0; row < 5; row++) {
                layers[y][row] = layer.get(row).getAsString();
                if (layers[y][row].length() != 5) throw new IllegalArgumentException("each altar row must be 5 symbols");
            }
        }
        JsonObject symbols = root.getAsJsonObject("symbols");
        Map<Character, Predicate<net.minecraft.block.BlockState>> symbolsByState = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : symbols.entrySet()) {
            if (entry.getKey().length() != 1) throw new IllegalArgumentException("symbol keys must be one character");
            JsonObject value = entry.getValue().getAsJsonObject();
            if (value.has("air") && value.get("air").getAsBoolean()) continue;
            Map<String, String> requiredProperties = new LinkedHashMap<>();
            if (value.has("properties")) for (Map.Entry<String, JsonElement> property : value.getAsJsonObject("properties").entrySet()) {
                requiredProperties.put(property.getKey(), property.getValue().getAsString());
            }
            if (value.has("block")) {
                Identifier id = Identifier.of(value.get("block").getAsString());
                if (!Registries.BLOCK.containsId(id)) throw new IllegalArgumentException("unknown block: " + id);
                Block block = Registries.BLOCK.get(id);
                symbolsByState.put(entry.getKey().charAt(0), state -> state.isOf(block) && propertiesMatch(state, requiredProperties));
            } else if (value.has("tag")) {
                TagKey<Block> tag = TagKey.of(RegistryKeys.BLOCK, Identifier.of(value.get("tag").getAsString()));
                symbolsByState.put(entry.getKey().charAt(0), state -> state.isIn(tag) && propertiesMatch(state, requiredProperties));
            } else throw new IllegalArgumentException("symbol needs block, tag, or air: " + entry.getKey());
        }
        for (String[] layer : layers) for (String row : layer) for (int i = 0; i < row.length(); i++) {
            char symbol = row.charAt(i);
            if (symbol != '.' && !symbolsByState.containsKey(symbol)) throw new IllegalArgumentException("missing symbol mapping: " + symbol);
        }
        if (!"..A..".equals(layers[1][2]) || !layers[1][0].equals(".....") || !layers[1][1].equals(".....") || !layers[1][3].equals(".....") || !layers[1][4].equals(".....")) throw new IllegalArgumentException("upper layer must contain only the central A");
        JsonArray rawOfferings = root.getAsJsonArray("offerings");
        if (rawOfferings == null || rawOfferings.size() < 1 || rawOfferings.size() > 9) throw new IllegalArgumentException("offerings must contain 1 to 9 slots");
        java.util.List<Offering> offerings = new java.util.ArrayList<>();
        for (JsonElement element : rawOfferings) {
            JsonObject offering = element.getAsJsonObject();
            boolean hasItem = offering.has("item");
            boolean hasTag = offering.has("tag");
            if (hasItem == hasTag) throw new IllegalArgumentException("offering needs exactly one of item or tag");
            Item item = null;
            TagKey<Item> tag = null;
            String sourceId;
            if (hasItem) {
                Identifier itemId = Identifier.of(offering.get("item").getAsString());
                if (!Registries.ITEM.containsId(itemId)) throw new IllegalArgumentException("unknown offering item: " + itemId);
                item = Registries.ITEM.get(itemId);
                sourceId = itemId.toString();
            } else {
                Identifier tagId = Identifier.of(offering.get("tag").getAsString());
                tag = TagKey.of(RegistryKeys.ITEM, tagId);
                sourceId = "#" + tagId;
            }
            int count = offering.get("count").getAsInt();
            int maximum = item == null ? 64 : item.getMaxCount();
            if (count < 1 || count > maximum) throw new IllegalArgumentException("invalid offering count for " + sourceId);
            offerings.add(new Offering(item, tag, count));
        }
        return new AltarDefinition(layers, Map.copyOf(symbolsByState), java.util.List.copyOf(offerings));
    }

    private static boolean propertiesMatch(net.minecraft.block.BlockState state, Map<String, String> required) {
        for (Map.Entry<String, String> expected : required.entrySet()) {
            boolean matches = false;
            for (Map.Entry<net.minecraft.state.property.Property<?>, Comparable<?>> actual : state.getEntries().entrySet()) {
                if (actual.getKey().getName().equals(expected.getKey()) && String.valueOf(actual.getValue()).equals(expected.getValue())) {
                    matches = true;
                    break;
                }
            }
            if (!matches) return false;
        }
        return true;
    }

    /** Exactly one selector is set: a concrete item or an item-registry tag. */
    public record Offering(Item item, TagKey<Item> tag, int count) {
        public boolean matches(net.minecraft.item.ItemStack stack) {
            return !stack.isEmpty() && (item != null ? stack.isOf(item) : stack.isIn(tag));
        }
    }
    public record AltarDefinition(String[][] layers, Map<Character, Predicate<net.minecraft.block.BlockState>> symbols, java.util.List<Offering> offerings) {
        static AltarDefinition defaults() {
            return new AltarDefinition(new String[][] {{"GCRBG", "CBBRM", "RRCMM", "MRBRC", "GMBCG"}, {".....", ".....", "..A..", ".....", "....."}}, Map.of(
                    'G', state -> state.isOf(net.minecraft.block.Blocks.GILDED_BLACKSTONE), 'C', state -> state.isOf(net.minecraft.block.Blocks.CRYING_OBSIDIAN),
                    'B', state -> state.isOf(net.minecraft.block.Blocks.POLISHED_BLACKSTONE_BRICKS), 'R', state -> state.isOf(net.minecraft.block.Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS),
                    'M', state -> state.isOf(net.minecraft.block.Blocks.MAGMA_BLOCK), 'A', state -> state.isOf(TerracraftMod.ALTAR_CORE)),
                    java.util.List.of(new Offering(Items.NETHERITE_INGOT, null, 2), new Offering(Items.BLAZE_ROD, null, 16), new Offering(Items.MAGMA_CREAM, null, 16), new Offering(Items.CRYING_OBSIDIAN, null, 8), new Offering(Items.NETHER_STAR, null, 1)));
        }
    }

    public record TreeFellingDefinition(int maxLogs, int maxFoliage, int perTaskPerTick, int globalPerTick,
                                        int maxTasks, int maxTasksPerPlayer, boolean includeVines,
                                        boolean includeCocoa, boolean includeNetherFoliage) {
        static TreeFellingDefinition defaults() { return new TreeFellingDefinition(128, 512, 32, 64, 16, 4, true, true, true); }
    }
}
