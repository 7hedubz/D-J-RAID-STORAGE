package com.dubz.raid.storage;

import com.dubz.raid.block.RaidChassisBlock;
import com.dubz.raid.block.RaidChestBlock;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Persistent per-dimension cable graph. Devices only connect on their back face.
 */
public class NetworkGraph extends SavedData {
    public static final SavedDataType<NetworkGraph> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("raid", "network_graph"),
            NetworkGraph::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.list(Node.CODEC).fieldOf("nodes").forGetter(NetworkGraph::nodeList)
            ).apply(instance, NetworkGraph::fromNodes))
    );

    private final Map<BlockPos, Node> nodes = new HashMap<>();

    public NetworkGraph() {
    }

    private NetworkGraph(List<Node> loaded) {
        for (Node node : loaded) {
            nodes.put(node.pos, node);
        }
    }

    public static NetworkGraph fromNodes(List<Node> loaded) {
        return new NetworkGraph(loaded);
    }

    public static NetworkGraph get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    private List<Node> nodeList() {
        return new ArrayList<>(nodes.values());
    }

    public void addCable(BlockPos pos) {
        put(new Node(pos.immutable(), NodeType.CABLE, ChestTier.STONE, Direction.NORTH));
    }

    public void addChassis(BlockPos pos, Direction facing) {
        put(new Node(pos.immutable(), NodeType.CHASSIS, ChestTier.STONE, facing));
    }

    public void addChest(BlockPos pos, ChestTier tier, Direction facing) {
        put(new Node(pos.immutable(), NodeType.CHEST, tier, facing));
    }

    public void addHopper(BlockPos pos, Direction facing) {
        put(new Node(pos.immutable(), NodeType.HOPPER, ChestTier.STONE, facing));
    }

    public void addCrafter(BlockPos pos, Direction facing) {
        put(new Node(pos.immutable(), NodeType.CRAFTER, ChestTier.STONE, facing));
    }

    public void remove(BlockPos pos) {
        if (nodes.remove(pos.immutable()) != null) {
            setDirty();
        }
    }

    private void put(Node node) {
        Node old = nodes.put(node.pos, node);
        if (old == null || old.type != node.type || old.tier != node.tier || old.facing != node.facing) {
            setDirty();
        }
    }

    private static Direction port(Node node) {
        if (node.type == NodeType.HOPPER || node.type == NodeType.CRAFTER) {
            return node.facing.getClockWise();
        }
        return node.facing.getOpposite();
    }

    public Lookup lookupChest(BlockPos chestPos) {
        return lookupDevice(chestPos.immutable());
    }

    public Lookup lookupDevice(BlockPos pos) {
        Node node = nodes.get(pos.immutable());
        if (node == null || node.type == NodeType.CABLE) {
            return Lookup.disconnected();
        }
        BlockPos cablePos = node.pos.relative(port(node));
        Node cable = nodes.get(cablePos);
        if (cable == null || cable.type != NodeType.CABLE) {
            return Lookup.disconnected();
        }
        return analyzeFromCable(cablePos);
    }

    public Lookup lookupCable(BlockPos cablePos) {
        Node node = nodes.get(cablePos.immutable());
        if (node == null || node.type != NodeType.CABLE) {
            return Lookup.disconnected();
        }
        return analyzeFromCable(cablePos.immutable());
    }

    private Lookup analyzeFromCable(BlockPos startCable) {
        Set<BlockPos> cables = floodCables(startCable);
        Set<BlockPos> chassis = new HashSet<>();
        List<Long> capacities = new ArrayList<>();
        for (BlockPos cable : cables) {
            for (Direction dir : Direction.values()) {
                Node node = nodes.get(cable.relative(dir));
                if (node == null || node.type == NodeType.CABLE) {
                    continue;
                }
                if (port(node) != dir.getOpposite()) {
                    continue;
                }
                if (node.type == NodeType.CHASSIS) {
                    chassis.add(node.pos);
                } else if (node.type == NodeType.CHEST) {
                    capacities.add(node.tier.bytes());
                }
            }
        }
        if (chassis.size() > 1) {
            return Lookup.error();
        }
        if (chassis.isEmpty()) {
            return Lookup.disconnected();
        }
        long capacity = RaidMath.capacityFor(capacities);
        BlockPos chassisPos = chassis.iterator().next();
        return new Lookup(NetworkStatus.OK, chassisPos, capacity);
    }

    private Set<BlockPos> floodCables(BlockPos start) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        if (visited.add(start)) {
            queue.add(start);
        }
        while (!queue.isEmpty()) {
            BlockPos cur = queue.removeFirst();
            for (Direction dir : Direction.values()) {
                BlockPos near = cur.relative(dir);
                Node node = nodes.get(near);
                if (node != null && node.type == NodeType.CABLE && visited.add(near)) {
                    queue.add(near);
                }
            }
        }
        return visited;
    }

    public void refreshVisuals(ServerLevel level) {
        for (Node node : List.copyOf(nodes.values())) {
            if (!level.isLoaded(node.pos)) {
                continue;
            }
            BlockState state = level.getBlockState(node.pos);
            DisplayStatus display = DisplayStatus.from(lookupDevice(node.pos).status());
            if (node.type == NodeType.CRAFTER
                    && level.getBlockEntity(node.pos) instanceof com.dubz.raid.blockentity.RaidCrafterBlockEntity crafter
                    && crafter.recipeInvalid()
                    && display == DisplayStatus.OK) {
                display = DisplayStatus.ERROR;
            }
            if (node.type == NodeType.CHEST && state.getBlock() instanceof RaidChestBlock
                    && state.hasProperty(DisplayStatus.PROPERTY)
                    && state.getValue(DisplayStatus.PROPERTY) != display) {
                level.setBlock(node.pos, state.setValue(DisplayStatus.PROPERTY, display),
                        Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_ON_PLACE);
            } else if ((node.type == NodeType.CHASSIS && state.getBlock() instanceof RaidChassisBlock
                    || node.type == NodeType.HOPPER && state.getBlock() instanceof com.dubz.raid.block.RaidHopperBlock
                    || node.type == NodeType.CRAFTER && state.getBlock() instanceof com.dubz.raid.block.RaidCrafterBlock)
                    && state.hasProperty(DisplayStatus.PROPERTY)
                    && state.getValue(DisplayStatus.PROPERTY) != display) {
                level.setBlock(node.pos, state.setValue(DisplayStatus.PROPERTY, display),
                        Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_ON_PLACE);
            }
        }
    }

    public record Lookup(NetworkStatus status, BlockPos chassisPos, long capacity) {
        static Lookup disconnected() {
            return new Lookup(NetworkStatus.DISCONNECTED, null, 0);
        }

        static Lookup error() {
            return new Lookup(NetworkStatus.ERROR, null, 0);
        }
    }

    public enum NodeType implements StringRepresentable {
        CABLE, CHEST, CHASSIS, HOPPER, CRAFTER;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public record Node(BlockPos pos, NodeType type, ChestTier tier, Direction facing) {
        public static final Codec<Node> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Node::pos),
                StringRepresentable.fromEnum(NodeType::values).fieldOf("type").forGetter(Node::type),
                StringRepresentable.fromEnum(ChestTier::values).fieldOf("tier").forGetter(Node::tier),
                Direction.CODEC.optionalFieldOf("facing", Direction.NORTH).forGetter(Node::facing)
        ).apply(instance, Node::new));
    }
}
