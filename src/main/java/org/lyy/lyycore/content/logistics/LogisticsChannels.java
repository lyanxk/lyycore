package org.lyy.lyycore.content.logistics;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.lyy.lyycore.content.blockEntities.MatrixPortBlockEntity;

/** Persistent channel identities; only loaded ports participate in routing. */
public final class LogisticsChannels extends SavedData {
    public static final int CHANNEL_LIMIT = 5, NAME_LIMIT = 32;
    public static final class Channel {
        public final UUID id;
        public final GlobalPos source;
        private String name;
        private UUID owner;
        private boolean publicChannel;
        private final Set<MatrixPortBlockEntity> inputs = new LinkedHashSet<>();
        private final Set<MatrixPortBlockEntity> outputs = new LinkedHashSet<>();

        private Channel(UUID id, GlobalPos source, String name, UUID owner, boolean publicChannel) {
            this.id = id; this.source = source; this.name = name;
            this.owner = owner; this.publicChannel = publicChannel;
        }
        public String name() { return name; }
        public boolean isPublic() { return publicChannel; }
        public boolean visibleTo(UUID player) { return publicChannel || player.equals(owner); }
        public boolean editableBy(UUID player) { return owner == null || player.equals(owner); }

        public void transfer() {
            if (inputs.isEmpty() || outputs.isEmpty()) return;
            for (var input : inputs) {
                if (input.isRemoved()) continue;
                for (int slot = 0; slot < input.items.getSlots(); slot++) {
                    var stack = input.items.getStackInSlot(slot);
                    if (stack.isEmpty()) continue;
                    var remaining = stack.copy();
                    for (var output : outputs) {
                        if (output.isRemoved()) continue;
                        remaining = ItemHandlerHelper.insertItemStacked(output.items, remaining, false);
                        if (remaining.isEmpty()) break;
                    }
                    // Both ends are our own inventories; charge exactly the accepted count.
                    input.items.extractItem(slot, stack.getCount() - remaining.getCount(), false);
                }
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
        if (channel != null) { channel.inputs.remove(port); channel.outputs.remove(port); }
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
            entry.putLong("Pos", channel.source.pos().asLong()); entries.add(entry);
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
            data.channels.put(channel.id, channel);
        }
        return data;
    }
}
