package com.dubz.raid.registry;

import com.dubz.raid.RaidMod;
import com.dubz.raid.block.DataCableBlock;
import com.dubz.raid.block.RaidChassisBlock;
import com.dubz.raid.block.RaidChestBlock;
import com.dubz.raid.block.CollectorBlock;
import com.dubz.raid.block.LookerBlock;
import com.dubz.raid.block.RaidCrafterBlock;
import com.dubz.raid.block.TreeFarmBlock;
import com.dubz.raid.item.MachineItem;
import com.dubz.raid.block.RaidHopperBlock;
import com.dubz.raid.item.RaidChassisItem;
import com.dubz.raid.item.RaidChestItem;
import com.dubz.raid.item.RaidCrafterItem;
import com.dubz.raid.item.RaidHopperItem;
import com.dubz.raid.storage.ChestTier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RaidMod.MOD_ID);

    public static final DeferredBlock<RaidChassisBlock> RAID_CHASSIS = register("raid_chassis",
            props -> new RaidChassisBlock(props.mapColor(MapColor.STONE).strength(1.0f, 2400.0f).sound(SoundType.STONE).pushReaction(PushReaction.IMMOVEABLE)));

    public static final DeferredBlock<DataCableBlock> DATA_CABLE = register("data_cable",
            props -> new DataCableBlock(props.mapColor(MapColor.COLOR_ORANGE).strength(1.0f).sound(ModSounds.cable())
                    .noOcclusion()
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos, box) -> false)
                    .isRedstoneConductor((state, level, pos) -> false)));

    public static final DeferredBlock<RaidHopperBlock> HOPPER = BLOCKS.registerBlock("hopper",
            props -> new RaidHopperBlock(props.mapColor(MapColor.STONE).strength(1.0f).sound(SoundType.METAL).pushReaction(PushReaction.IMMOVEABLE)));

    public static final DeferredBlock<RaidCrafterBlock> AUTO_CRAFTER = BLOCKS.registerBlock("auto_crafter",
            props -> new RaidCrafterBlock(props.mapColor(MapColor.COLOR_PURPLE).strength(1.0f).sound(SoundType.AMETHYST).pushReaction(PushReaction.IMMOVEABLE)));

    public static final DeferredBlock<TreeFarmBlock> TREE_FARM = BLOCKS.registerBlock("tree_farm",
            props -> new TreeFarmBlock(props.mapColor(MapColor.WOOD).strength(1.0f).sound(SoundType.WOOD).pushReaction(PushReaction.IMMOVEABLE)));

    public static final DeferredBlock<TreeFarmBlock> NETHER_TREE_FARM = BLOCKS.registerBlock("nether_tree_farm",
            props -> new TreeFarmBlock(props.mapColor(MapColor.NETHER).strength(1.0f).sound(SoundType.STEM).pushReaction(PushReaction.IMMOVEABLE), true));

    public static final DeferredBlock<CollectorBlock> COLLECTOR = BLOCKS.registerBlock("collector",
            props -> new CollectorBlock(props.mapColor(MapColor.METAL).strength(1.0f).sound(SoundType.METAL).pushReaction(PushReaction.IMMOVEABLE)));

    public static final DeferredBlock<LookerBlock> LOOKER = BLOCKS.registerBlock("looker",
            props -> new LookerBlock(props.mapColor(MapColor.COLOR_GREEN).strength(1.0f).sound(SoundType.WOOD).pushReaction(PushReaction.IMMOVEABLE)));

    public static final DeferredBlock<RaidChestBlock> STONE_CHEST = chest("stone_chest", ChestTier.STONE, MapColor.STONE, SoundType.STONE, 12.0f);
    public static final DeferredBlock<RaidChestBlock> COPPER_CHEST = chest("copper_chest", ChestTier.COPPER, MapColor.COLOR_ORANGE, SoundType.COPPER, 12.0f);
    public static final DeferredBlock<RaidChestBlock> IRON_CHEST = chest("iron_chest", ChestTier.IRON, MapColor.METAL, SoundType.IRON, 12.0f);
    public static final DeferredBlock<RaidChestBlock> GOLD_CHEST = chest("gold_chest", ChestTier.GOLD, MapColor.GOLD, SoundType.METAL, 12.0f);
    public static final DeferredBlock<RaidChestBlock> QUARTZ_CHEST = chest("quartz_chest", ChestTier.QUARTZ, MapColor.QUARTZ, SoundType.STONE, 12.0f);
    public static final DeferredBlock<RaidChestBlock> AMETHYST_CHEST = chest("amethyst_chest", ChestTier.AMETHYST, MapColor.COLOR_PURPLE, SoundType.AMETHYST, 12.0f);
    public static final DeferredBlock<RaidChestBlock> DIAMOND_CHEST = chest("diamond_chest", ChestTier.DIAMOND, MapColor.DIAMOND, SoundType.METAL, 12.0f);
    public static final DeferredBlock<RaidChestBlock> NETHERITE_CHEST = chest("netherite_chest", ChestTier.NETHERITE, MapColor.COLOR_BLACK, SoundType.NETHERITE_BLOCK, 2400.0f);
    public static final DeferredBlock<RaidChestBlock> ADMIN_CHEST = chest("admin_chest", ChestTier.ADMIN, MapColor.COLOR_BLACK, SoundType.NETHERITE_BLOCK, 2400.0f);

    private static DeferredBlock<RaidChestBlock> chest(String name, ChestTier tier, MapColor color, SoundType sound, float blastResistance) {
        DeferredBlock<RaidChestBlock> block = BLOCKS.registerBlock(name, props -> new RaidChestBlock(
                props.mapColor(color).strength(1.0f, blastResistance).sound(sound).pushReaction(PushReaction.IMMOVEABLE),
                tier
        ));
        ModItems.ITEMS.registerItem(name, props -> new RaidChestItem(block.get(), props.useBlockDescriptionPrefix()));
        return block;
    }

    private static <T extends net.minecraft.world.level.block.Block> DeferredBlock<T> register(String name, java.util.function.Function<BlockBehaviour.Properties, T> factory) {
        DeferredBlock<T> block = BLOCKS.registerBlock(name, factory);
        if (name.equals("raid_chassis")) {
            ModItems.ITEMS.registerItem(name, props -> new RaidChassisItem(block.get(), props.useBlockDescriptionPrefix()));
        } else {
            ModItems.ITEMS.registerItem(name, props -> new BlockItem(block.get(), props.useBlockDescriptionPrefix()));
        }
        return block;
    }

    public static void register(IEventBus bus) {
        ModItems.ITEMS.registerItem("hopper", props -> new RaidHopperItem(HOPPER.get(), props.useBlockDescriptionPrefix()));
        ModItems.ITEMS.registerItem("auto_crafter", props -> new RaidCrafterItem(AUTO_CRAFTER.get(), props.useBlockDescriptionPrefix()));
        ModItems.ITEMS.registerItem("tree_farm", props -> new MachineItem(TREE_FARM.get(), props.useBlockDescriptionPrefix(),
                "block.raid.tree_farm.subtitle", "block.raid.tree_farm.tooltip"));
        ModItems.ITEMS.registerItem("nether_tree_farm", props -> new MachineItem(NETHER_TREE_FARM.get(), props.useBlockDescriptionPrefix(),
                "block.raid.nether_tree_farm.subtitle", "block.raid.nether_tree_farm.tooltip"));
        ModItems.ITEMS.registerItem("collector", props -> new MachineItem(COLLECTOR.get(), props.useBlockDescriptionPrefix(),
                "block.raid.collector.subtitle", "block.raid.collector.tooltip"));
        ModItems.ITEMS.registerItem("looker", props -> new MachineItem(LOOKER.get(), props.useBlockDescriptionPrefix(),
                "block.raid.looker.subtitle", "block.raid.looker.tooltip"));
        BLOCKS.register(bus);
    }
}
