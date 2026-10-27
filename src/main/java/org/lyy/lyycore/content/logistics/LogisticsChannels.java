package org.lyy.lyycore.content.logistics;

import java.util.*;
import javax.annotation.Nullable;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.lyy.lyycore.content.blockEntities.MatrixPortBlockEntity;

/** Persistent channel identities; only loaded ports participate in routing. */
public final class LogisticsChannels extends SavedData {
    public static final int CHANNEL_LIMIT = 5, NAME_LIMIT = 32;
    /** Inclusive block corners in the first corner's dimension. Both corners belong to the same dimension. */
    public record CollectionArea(GlobalPos first, BlockPos second) {
        public CollectionArea { first = GlobalPos.of(first.dimension(), first.pos().immutable()); second = second.immutable(); }
        public AABB bounds() { return new AABB(first.pos()).minmax(new AABB(second)); }
    }
    public record BlockTarget(GlobalPos position, @Nullable Direction side) {
        public BlockTarget { position = GlobalPos.of(position.dimension(), position.pos().immutable()); }
        @Nullable public IItemHandler inventory(ServerLevel context) {
            var level = context.getServer().getLevel(position.dimension());
            return level == null || !level.hasChunkAt(position.pos()) ? null
                    : level.getCapability(Capabilities.ItemHandler.BLOCK, position.pos(), side);
        }
    }
    public static final class Channel {
        public final UUID id;
        public final GlobalPos source;
        private String name;
        private UUID owner;
        private boolean publicChannel;
        private final Set<MatrixPortBlockEntity> inputs = new LinkedHashSet<>();
        private final Set<MatrixPortBlockEntity> outputs = new LinkedHashSet<>();
        private List<CollectionArea> collectionAreas = List.of();
        private List<BlockTarget> inputTargets = List.of(), outputTargets = List.of();

        private Channel(UUID id, GlobalPos source, String name, UUID owner, boolean publicChannel) {
            this.id = id; this.source = source; this.name = name;
            this.owner = owner; this.publicChannel = publicChannel;
        }
        public String name() { return name; }
        public boolean isPublic() { return publicChannel; }
        public boolean visibleTo(UUID player) { return publicChannel || player.equals(owner); }
        public boolean editableBy(UUID player) { return owner == null || player.equals(owner); }

        public List<CollectionArea> collectionAreas() { return collectionAreas; }
        public List<BlockTarget> inputTargets() { return inputTargets; }
        public List<BlockTarget> outputTargets() { return outputTargets; }

        /** Input and collector ticks push their own inventory to the channel's loaded outputs. */
        public void pushFrom(MatrixPortBlockEntity input) {
            if (outputs.isEmpty() || !LogisticsTransfer.hasItems(input.items)) return;
            for (var output : outputs) {
                if (output.isRemoved() || !LogisticsTransfer.hasRoom(output.items)) continue;
                LogisticsTransfer.move(input.items, output.items);
                if (!LogisticsTransfer.hasItems(input.items)) break;
            }
        }

        public boolean hasOutputPorts() { return !outputs.isEmpty(); }

        /** Matrix-selected sources feed only its designated blocks, without channel storage. */
        public void collectAndOutput(ServerLevel context) {
            if (outputTargets.isEmpty() || (inputTargets.isEmpty() && collectionAreas.isEmpty())) return;
            var destinations = new ArrayList<IItemHandler>();
            for (var target : outputTargets) {
                var inventory = target.inventory(context);
                if (inventory != null && LogisticsTransfer.hasRoom(inventory)) destinations.add(inventory);
            }
            if (destinations.isEmpty()) return;
            for (var target : inputTargets) {
                var source = target.inventory(context);
                if (source == null) continue;
                for (var destination : destinations) LogisticsTransfer.move(source, destination);
            }
            for (var area : collectionAreas) {
                var level = context.getServer().getLevel(area.first.dimension());
                if (level != null) LogisticsTransfer.collect(level, area.bounds(), destinations);
            }
        }
    }

    private static final Factory<LogisticsChannels> FACTORY = new Factory<>(LogisticsChannels::new, LogisticsChannels::load);
    private final Map<UUID, Channel> channels = new LinkedHashMap<>();
    private long revision;

    public static LogisticsChannels get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, "lyycore_logistics");
    }
    public long revision() { return revision; }
    public Channel channel(UUID id) { return channels.get(id); }
    public List<Channel> all() { return List.copyOf(channels.values()); }
    public List<Channel> atSource(GlobalPos source) {
        return channels.values().stream().filter(c -> c.source.equals(source)).toList();
    }
    public List<Channel> visibleTo(UUID player) {
        return channels.values().stream().filter(c -> c.visibleTo(player)).toList();
    }
    /** Locator bindings persist with the channel and share its visibility rules. */
    public void setCollectionAreas(UUID id, List<CollectionArea> areas) {
        var channel = channels.get(id);
        if (channel == null || channel.collectionAreas.equals(areas)) return;
        channel.collectionAreas = List.copyOf(new LinkedHashSet<>(areas));
        changed();
    }
    public void setBlockTargets(UUID id, boolean output, List<BlockTarget> targets) {
        var channel = channels.get(id);
        if (channel == null || (output ? channel.outputTargets : channel.inputTargets).equals(targets)) return;
        var bindings = List.copyOf(new LinkedHashSet<>(targets));
        if (output) channel.outputTargets = bindings; else channel.inputTargets = bindings;
        changed();
    }
    public void create(GlobalPos source, UUID player, String name, boolean publicChannel) {
        name = cleanName(name);
        if (name.isEmpty() || atSource(source).size() >= CHANNEL_LIMIT) return;
        var channel = new Channel(UUID.randomUUID(), source, name, player, publicChannel);
        channels.put(channel.id, channel); changed();
    }
    public void update(GlobalPos source, UUID player, UUID id, String name, boolean publicChannel) {
        var channel = channels.get(id);
        name = cleanName(name);
        if (channel == null || !channel.source.equals(source) || !channel.editableBy(player) || name.isEmpty()) return;
        if (name.equals(channel.name) && publicChannel == channel.publicChannel && channel.owner != null) return;
        channel.name = name; channel.publicChannel = publicChannel; channel.owner = player;
        changed();
    }
    public void delete(GlobalPos source, UUID player, UUID id) {
        var channel = channels.get(id);
        if (channel != null && channel.source.equals(source) && channel.editableBy(player)) remove(channel);
    }
    private void remove(Channel channel) {
        channels.remove(channel.id); changed();
        // Inactive ports retain their ID until loading, then discard the missing binding.
        for (var port : channel.inputs) port.clearBinding();
        for (var port : channel.outputs) port.clearBinding();
    }
    public void removeMatrix(GlobalPos source) {
        for (var channel : atSource(source)) remove(channel);
    }
    public boolean attach(UUID id, MatrixPortBlockEntity port) {
        var channel = channels.get(id);
        if (channel == null) return false;
        (port.isOutput() ? channel.outputs : channel.inputs).add(port);
        return true;
    }
    public void detach(UUID id, MatrixPortBlockEntity port) {
        var channel = channels.get(id);
        if (channel != null) {
            channel.inputs.remove(port); channel.outputs.remove(port);
        }
    }
    private void changed() { revision++; setDirty(); }
    private static String cleanName(String name) {
        var clean = net.minecraft.util.StringUtil.filterText(name).strip();
        return clean.substring(0, Math.min(NAME_LIMIT, clean.length()));
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var entries = new ListTag();
        for (var channel : channels.values()) {
            var entry = new CompoundTag();
            entry.putUUID("Id", channel.id); entry.putString("Name", channel.name);
            if (channel.owner != null) entry.putUUID("Owner", channel.owner);
            entry.putBoolean("Public", channel.publicChannel);
            entry.putString("Dimension", channel.source.dimension().location().toString());
            entry.putLong("Pos", channel.source.pos().asLong());
            var areas = new ListTag();
            for (var area : channel.collectionAreas) {
                var areaTag = savePosition(area.first);
                areaTag.putLong("Second", area.second.asLong()); areas.add(areaTag);
            }
            entry.put("CollectionAreas", areas);
            entry.put("InputTargets", saveTargets(channel.inputTargets));
            entry.put("OutputTargets", saveTargets(channel.outputTargets));
            entries.add(entry);
        }
        tag.put("Channels", entries); return tag;
    }
    private static LogisticsChannels load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new LogisticsChannels();
        for (var raw : tag.getList("Channels", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag) raw;
            var dimension = ResourceLocation.tryParse(entry.getString("Dimension"));
            if (dimension == null || !entry.hasUUID("Id")) continue;
            var source = GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dimension), BlockPos.of(entry.getLong("Pos")));
            if (data.atSource(source).size() >= CHANNEL_LIMIT) continue;
            var channel = new Channel(entry.getUUID("Id"), source, cleanName(entry.getString("Name")),
                    entry.hasUUID("Owner") ? entry.getUUID("Owner") : null,
                    !entry.contains("Public") || entry.getBoolean("Public"));
            var areas = new ArrayList<CollectionArea>();
            for (var areaRaw : entry.getList("CollectionAreas", Tag.TAG_COMPOUND)) {
                var areaTag = (CompoundTag) areaRaw;
                areas.add(new CollectionArea(loadPosition(areaTag), BlockPos.of(areaTag.getLong("Second"))));
            }
            channel.collectionAreas = List.copyOf(areas);
            channel.inputTargets = loadTargets(entry.getList("InputTargets", Tag.TAG_COMPOUND));
            channel.outputTargets = loadTargets(entry.getList("OutputTargets", Tag.TAG_COMPOUND));
            data.channels.put(channel.id, channel);
        }
        return data;
    }
    private static ListTag saveTargets(List<BlockTarget> targets) {
        var result = new ListTag();
        for (var target : targets) {
            var tag = savePosition(target.position);
            if (target.side != null) tag.putString("Side", target.side.getName());
            result.add(tag);
        }
        return result;
    }
    private static List<BlockTarget> loadTargets(ListTag entries) {
        var result = new ArrayList<BlockTarget>();
        for (var raw : entries) {
            var tag = (CompoundTag) raw;
            result.add(new BlockTarget(loadPosition(tag), Direction.byName(tag.getString("Side"))));
        }
        return List.copyOf(result);
    }
    private static CompoundTag savePosition(GlobalPos position) {
        var tag = new CompoundTag();
        tag.putString("Dimension", position.dimension().location().toString());
        tag.putLong("Pos", position.pos().asLong());
        return tag;
    }
    private static GlobalPos loadPosition(CompoundTag tag) {
        return GlobalPos.of(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(tag.getString("Dimension"))),
                BlockPos.of(tag.getLong("Pos")));
    }
}
