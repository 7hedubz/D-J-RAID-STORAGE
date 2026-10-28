package com.dubz.raid.menu;

import com.dubz.raid.storage.DigitizedStorage;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RaidItemGroups {
    private static final TagKey<Item> FARMING = tag("raid", "farming");
    private static final TagKey<Item> CRAFTING_STATIONS = tag("raid", "crafting_stations");
    private static final TagKey<Item> DECORATION = tag("raid", "decoration");
    private static final TagKey<Item> ORES = tag("c", "ores");
    private static final TagKey<Item> INGOTS = tag("c", "ingots");
    private static final TagKey<Item> NUGGETS = tag("c", "nuggets");
    private static final TagKey<Item> RAW_MATERIALS = tag("c", "raw_materials");
    private static final TagKey<Item> GEMS = tag("c", "gems");
    private static final TagKey<Item> VANILLA_ORES = tag("minecraft", "ores");
    private static final TagKey<Item> DOORS = tag("minecraft", "doors");

    private record Category(String id, Component title, int order) {
    }

    private static final Category BUILDING = vanilla("building", 0);
    private static final Category DECORATION_CAT = vanilla("decoration", 1);
    private static final Category NATURAL = vanilla("natural", 2);
    private static final Category CRAFTING = vanilla("crafting", 3);
    private static final Category FUNCTIONAL = vanilla("functional", 4);
    private static final Category REDSTONE = vanilla("redstone", 5);
    private static final Category ORES_CAT = vanilla("ores", 6);
    private static final Category INGOTS_CAT = vanilla("ingots", 7);
    private static final Category GEMS_CAT = vanilla("gems", 8);
    private static final Category TOOLS = vanilla("tools", 9);
    private static final Category COMBAT = vanilla("combat", 10);
    private static final Category FARMING_AND_FOOD = vanilla("farming_and_food", 11);
    private static final Category INGREDIENTS = vanilla("ingredients", 12);
    private static final Category ENCHANTED_BOOKS = vanilla("enchanted_books", 13);
    private static final Category SPAWN_EGGS = vanilla("spawn_eggs", 14);
    private static final Category OTHER = vanilla("other", 1000);

    private RaidItemGroups() {
    }

    public record Row(Component header, List<DigitizedStorage.Entry> items) {
        public boolean isHeader() {
            return header != null;
        }

        public int height() {
            return isHeader() ? ChestGuiLayout.HEADER_H : ChestGuiLayout.CELL_H;
        }
    }

    public static int rowHeight(Row row) {
        return row.height();
    }

    public static List<Row> rows(List<DigitizedStorage.Entry> entries) {
        Map<Item, Category> cache = new HashMap<>();
        Map<String, List<DigitizedStorage.Entry>> grouped = new LinkedHashMap<>();
        Map<String, Category> categories = new LinkedHashMap<>();
        for (DigitizedStorage.Entry entry : entries) {
            Item item = entry.stack.getItem();
            Category category = cache.get(item);
            if (category == null) {
                category = classify(entry.stack);
                cache.put(item, category);
            }
            categories.putIfAbsent(category.id, category);
            grouped.computeIfAbsent(category.id, ignored -> new ArrayList<>()).add(entry);
        }
        Comparator<DigitizedStorage.Entry> byName = Comparator.comparing(
                entry -> entry.stack.getHoverName().getString(), String.CASE_INSENSITIVE_ORDER);
        List<Category> ordered = new ArrayList<>(categories.values());
        ordered.sort(Comparator.comparingInt(Category::order)
                .thenComparing(category -> category.title.getString(), String.CASE_INSENSITIVE_ORDER));
        List<Row> rows = new ArrayList<>();
        for (Category category : ordered) {
            appendGroup(rows, category.title, grouped.get(category.id), byName);
        }
        return rows;
    }

    private static void appendGroup(List<Row> rows, Component title, List<DigitizedStorage.Entry> items,
                                    Comparator<DigitizedStorage.Entry> byName) {
        if (items == null || items.isEmpty()) {
            return;
        }
        items.sort(byName);
        rows.add(new Row(title, List.of()));
        for (int i = 0; i < items.size(); i += ChestGuiLayout.COLS) {
            int end = Math.min(i + ChestGuiLayout.COLS, items.size());
            rows.add(new Row(null, List.copyOf(items.subList(i, end))));
        }
    }

    private static Category classify(ItemStack stack) {
        String namespace = namespace(stack);
        if (!"minecraft".equals(namespace)) {
            return modCategory(namespace);
        }
        Item item = stack.getItem();
        if (item == Items.ENCHANTED_BOOK) {
            return ENCHANTED_BOOKS;
        }
        if (item == Items.STRING) {
            return INGREDIENTS;
        }
        if (tagged(stack, DOORS) || tagged(stack, ItemTags.WOODEN_DOORS)
                || tagged(stack, ItemTags.BOATS) || tagged(stack, ItemTags.CHEST_BOATS)) {
            return FUNCTIONAL;
        }
        if (isFarmingOrFood(stack)) {
            return FARMING_AND_FOOD;
        }
        if (tagged(stack, GEMS)) {
            return GEMS_CAT;
        }
        if (tagged(stack, ORES) || tagged(stack, VANILLA_ORES) || tagged(stack, RAW_MATERIALS)) {
            return ORES_CAT;
        }
        if (tagged(stack, INGOTS) || tagged(stack, NUGGETS) || isIngotStorageBlock(stack)) {
            return INGOTS_CAT;
        }
        if (isPot(stack) || tagged(stack, DECORATION) || inTab(item, CreativeModeTabs.COLORED_BLOCKS)) {
            return DECORATION_CAT;
        }
        if (tagged(stack, CRAFTING_STATIONS)) {
            return CRAFTING;
        }
        if (inTab(item, CreativeModeTabs.REDSTONE_BLOCKS)) {
            return REDSTONE;
        }
        if (inTab(item, CreativeModeTabs.COMBAT)) {
            return COMBAT;
        }
        if (inTab(item, CreativeModeTabs.TOOLS_AND_UTILITIES)) {
            return TOOLS;
        }
        if (inTab(item, CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
            return FUNCTIONAL;
        }
        if (inTab(item, CreativeModeTabs.NATURAL_BLOCKS)) {
            return NATURAL;
        }
        if (inTab(item, CreativeModeTabs.BUILDING_BLOCKS)) {
            return BUILDING;
        }
        if (inTab(item, CreativeModeTabs.INGREDIENTS)) {
            return INGREDIENTS;
        }
        if (inTab(item, CreativeModeTabs.SPAWN_EGGS)) {
            return SPAWN_EGGS;
        }
        if (item instanceof BlockItem) {
            return BUILDING;
        }
        return OTHER;
    }

    private static Category modCategory(String namespace) {
        String name = ModList.get().getModContainerById(namespace)
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(namespace);
        return new Category("mod:" + namespace, Component.literal(name), 500);
    }

    private static String namespace(ItemStack stack) {
        return stack.typeHolder().unwrapKey()
                .map(key -> key.identifier().getNamespace())
                .orElse("minecraft");
    }

    private static boolean isPot(ItemStack stack) {
        String path = itemPath(stack);
        return path.equals("flower_pot") || path.equals("decorated_pot") || path.startsWith("potted_");
    }

    private static boolean isIngotStorageBlock(ItemStack stack) {
        return switch (itemPath(stack)) {
            case "iron_block", "gold_block", "copper_block", "netherite_block" -> true;
            default -> false;
        };
    }

    private static String itemPath(ItemStack stack) {
        return stack.typeHolder().unwrapKey()
                .map(key -> key.identifier().getPath())
                .orElse("");
    }

    private static boolean isFarmingOrFood(ItemStack stack) {
        if (stack.has(DataComponents.FOOD) || tagged(stack, FARMING)) {
            return true;
        }
        return inTab(stack.getItem(), CreativeModeTabs.FOOD_AND_DRINKS);
    }

    private static boolean tagged(ItemStack stack, TagKey<Item> tag) {
        return stack.typeHolder().is(tag);
    }

    private static boolean inTab(Item item, ResourceKey<CreativeModeTab> key) {
        return tabContains(BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(key), item);
    }

    private static boolean tabContains(CreativeModeTab tab, Item item) {
        for (ItemStack displayed : tab.getDisplayItems()) {
            if (displayed.getItem() == item) {
                return true;
            }
        }
        for (ItemStack displayed : tab.getSearchTabDisplayItems()) {
            if (displayed.getItem() == item) {
                return true;
            }
        }
        return false;
    }

    private static Category vanilla(String path, int order) {
        return new Category(path, Component.translatable("gui.raid.group." + path), order);
    }

    private static TagKey<Item> tag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(namespace, path));
    }
}
