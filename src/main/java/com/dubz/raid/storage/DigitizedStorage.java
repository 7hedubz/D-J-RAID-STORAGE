package com.dubz.raid.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class DigitizedStorage {
    public static final Codec<DigitizedStorage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.list(Entry.CODEC).fieldOf("entries").forGetter(s -> s.entries)
    ).apply(instance, DigitizedStorage::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DigitizedStorage> STREAM_CODEC = StreamCodec.of(
            DigitizedStorage::encode,
            DigitizedStorage::decode
    );

    private final List<Entry> entries;

    public DigitizedStorage() {
        this.entries = new ArrayList<>();
    }

    public DigitizedStorage(List<Entry> entries) {
        this.entries = new ArrayList<>(entries);
    }

    public List<Entry> entries() {
        return entries;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public DigitizedStorage copy() {
        List<Entry> copied = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            copied.add(new Entry(entry.stack, entry.count));
        }
        return new DigitizedStorage(copied);
    }

    private static void encode(RegistryFriendlyByteBuf buf, DigitizedStorage storage) {
        buf.writeVarInt(storage.entries.size());
        for (Entry entry : storage.entries) {
            ItemStack.STREAM_CODEC.encode(buf, entry.stack);
            buf.writeVarLong(entry.count);
        }
    }

    private static DigitizedStorage decode(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<Entry> loaded = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            loaded.add(new Entry(ItemStack.STREAM_CODEC.decode(buf), buf.readVarLong()));
        }
        return new DigitizedStorage(loaded);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DigitizedStorage other) || entries.size() != other.entries.size()) {
            return false;
        }
        for (int i = 0; i < entries.size(); i++) {
            Entry a = entries.get(i);
            Entry b = other.entries.get(i);
            if (a.count != b.count || !ItemStack.isSameItemSameComponents(a.stack, b.stack)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 1;
        for (Entry entry : entries) {
            hash = 31 * hash + Objects.hash(ItemStack.hashItemAndComponents(entry.stack), entry.count);
        }
        return hash;
    }

    public long usedBytes() {
        long used = 0;
        for (Entry entry : entries) {
            used += RaidMath.bytesFor(entry.stack, entry.count);
        }
        return used;
    }

    public long insert(ItemStack stack, long capacity) {
        if (stack.isEmpty() || !RaidMath.canDigitize(stack)) {
            return 0;
        }
        long used = usedBytes();
        if (used > capacity) {
            return 0;
        }
        int match = indexOf(stack);
        long current = match >= 0 ? entries.get(match).count : 0;
        long fit = RaidMath.itemsThatFit(stack, current, stack.getCount(), used, capacity);
        if (fit <= 0) {
            return 0;
        }
        if (match >= 0) {
            entries.get(match).count += fit;
        } else {
            ItemStack template = stack.copy();
            template.setCount(1);
            entries.add(new Entry(template, fit));
        }
        return fit;
    }

    public ItemStack extract(ItemStack template, long amount) {
        if (template.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        int match = indexOf(template);
        if (match < 0) {
            return ItemStack.EMPTY;
        }
        Entry entry = entries.get(match);
        long take = Math.min(amount, entry.count);
        take = Math.min(take, template.getMaxStackSize());
        if (take <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack out = entry.stack.copy();
        out.setCount((int) take);
        entry.count -= take;
        if (entry.count <= 0) {
            entries.remove(match);
        }
        return out;
    }

    public ItemStack extractAllPossible(ItemStack template, int maxCount) {
        return extract(template, maxCount);
    }

    private int indexOf(ItemStack stack) {
        for (int i = 0; i < entries.size(); i++) {
            if (ItemStack.isSameItemSameComponents(entries.get(i).stack, stack)) {
                return i;
            }
        }
        return -1;
    }

    public void removeEmpty() {
        Iterator<Entry> it = entries.iterator();
        while (it.hasNext()) {
            if (it.next().count <= 0) {
                it.remove();
            }
        }
    }

    public static class Entry {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemStack.CODEC.fieldOf("item").forGetter(e -> e.stack),
                Codec.LONG.fieldOf("count").forGetter(e -> e.count)
        ).apply(instance, Entry::new));

        public final ItemStack stack;
        public long count;

        public Entry(ItemStack stack, long count) {
            this.stack = stack.copy();
            this.stack.setCount(1);
            this.count = count;
        }
    }
}
