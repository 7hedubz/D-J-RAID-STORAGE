package com.dubz.raid.storage;

import com.dubz.raid.registry.ModDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class RaidMath {
    public static final TagKey<Item> TOOLS_AND_WEAPONS = TagKey.create(
            Registries.ITEM, Identifier.fromNamespaceAndPath("raid", "tools_and_weapons"));
    public static final TagKey<Item> NESTED_STORAGE = TagKey.create(
            Registries.ITEM, Identifier.fromNamespaceAndPath("raid", "nested_storage"));

    private static final long UNIT_THRESHOLD = 2048;
    private static final String[] UNITS = {"B", "KB", "MB", "GB", "TB", "PB"};
    public static final String[] COMMAND_UNITS = {"B", "KB", "MB", "GB"};

    private RaidMath() {
    }

    /**
     * First item of a digitized stack is 16 bytes; every extra item is 1 byte.
     * Anything that only stacks to 1 (tools, armor, filled buckets, etc.) costs 16 bytes each.
     */
    public static long bytesForCount(long count) {
        return bytesFor(ItemStack.EMPTY, count);
    }

    public static long bytesFor(ItemStack stack, long count) {
        if (count <= 0) {
            return 0;
        }
        if (!stack.isEmpty() && (stack.getMaxStackSize() <= 1 || stack.typeHolder().is(TOOLS_AND_WEAPONS))) {
            return count * 16;
        }
        return count + 15;
    }

    /**
     * Largest CHEST contributes its full tier. Every extra CHEST contributes half of its own tier.
     */
    public static long capacityFor(List<Long> chestBytes) {
        if (chestBytes == null || chestBytes.isEmpty()) {
            return 0;
        }
        List<Long> sorted = new ArrayList<>(chestBytes);
        sorted.sort(Comparator.reverseOrder());
        long total = sorted.getFirst();
        for (int i = 1; i < sorted.size(); i++) {
            total += sorted.get(i) / 2;
        }
        return total;
    }

    public static long itemsThatFit(ItemStack stack, long currentCount, long additionalWanted, long usedBytes, long capacity) {
        if (additionalWanted <= 0) {
            return 0;
        }
        long low = 0;
        long high = additionalWanted;
        long best = 0;
        while (low <= high) {
            long mid = (low + high) / 2;
            long newUsed = usedBytes - bytesFor(stack, currentCount) + bytesFor(stack, currentCount + mid);
            if (newUsed <= capacity) {
                best = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return best;
    }

    public static long itemsThatFit(long currentCount, long additionalWanted, long usedBytes, long capacity) {
        return itemsThatFit(ItemStack.EMPTY, currentCount, additionalWanted, usedBytes, capacity);
    }

    /**
     * Nested inventories (filled shulkers, bundles, RAID chassis) cannot be digitized.
     * Empty containers of those types are allowed. Traveler's Backpacks never digitize.
     */
    public static boolean canDigitize(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.typeHolder().is(NESTED_STORAGE)) {
            return false;
        }
        BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundle != null && !bundle.isEmpty()) {
            return false;
        }
        ItemContainerContents container = stack.get(DataComponents.CONTAINER);
        if (container != null && container.nonEmptyItems().iterator().hasNext()) {
            return false;
        }
        if (stack.has(DataComponents.CONTAINER_LOOT)) {
            return false;
        }
        DigitizedStorage stored = stack.get(ModDataComponents.STORAGE.get());
        return stored == null || stored.isEmpty();
    }

    public static String formatBytes(long bytes) {
        int unit = 0;
        double value = bytes;
        while (unit < UNITS.length - 1 && value >= UNIT_THRESHOLD) {
            value /= 1024.0;
            unit++;
        }
        return formatNumber(value, unit == 0) + " " + UNITS[unit];
    }

    public static String formatUsedCapacity(long used, long capacity) {
        long magnitude = Math.max(Math.abs(used), Math.abs(capacity));
        int unit = 0;
        double divisor = 1;
        while (unit < UNITS.length - 1 && magnitude / divisor >= UNIT_THRESHOLD) {
            divisor *= 1024;
            unit++;
        }
        return formatNumber(used / divisor, unit == 0) + " / " + formatNumber(capacity / divisor, unit == 0) + " " + UNITS[unit];
    }

    private static String formatNumber(double value, boolean integer) {
        if (integer || Math.abs(value - Math.rint(value)) < 1e-6) {
            return Long.toString(Math.round(value));
        }
        String text = String.format(Locale.ROOT, "%.2f", value);
        int end = text.length();
        while (end > 0 && text.charAt(end - 1) == '0') {
            end--;
        }
        if (end > 0 && text.charAt(end - 1) == '.') {
            end--;
        }
        return text.substring(0, end);
    }

    public static long bytesFromAmount(double amount, String unit) {
        if (amount <= 0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
            throw new IllegalArgumentException("amount");
        }
        int shift = switch (unit.toUpperCase(Locale.ROOT)) {
            case "B", "BYTE", "BYTES" -> 0;
            case "K", "KB", "KIB" -> 1;
            case "M", "MB", "MIB" -> 2;
            case "G", "GB", "GIB" -> 3;
            default -> throw new IllegalArgumentException(unit);
        };
        double bytes = amount * Math.pow(1024, shift);
        if (bytes < 1 || bytes > Long.MAX_VALUE) {
            throw new IllegalArgumentException("amount");
        }
        return Math.max(1, Math.round(bytes));
    }

    public static String formatAdminCommand(long bytes) {
        long value = Math.max(1, bytes);
        int unit = 0;
        while (unit < COMMAND_UNITS.length - 1 && value % 1024 == 0) {
            value /= 1024;
            unit++;
        }
        return "/raidadmin " + value + " " + COMMAND_UNITS[unit];
    }
}
