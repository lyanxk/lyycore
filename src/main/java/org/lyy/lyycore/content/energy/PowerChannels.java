package org.lyy.lyycore.content.energy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringUtil;
import net.minecraft.world.level.saveddata.SavedData;
import org.lyy.lyycore.energy.EnergyReceiver;

/** One persistent, independently owned power channel for each spacetime matrix. */
public final class PowerChannels extends SavedData {
    public static final int TARGET_LIMIT = 30, NAME_LIMIT = 32;

    public static final class Channel {
        public final UUID id;
        public final GlobalPos source;
        private final UUID owner;
        private String name;
        private boolean publicChannel;
        private final List<GlobalPos> targets = new ArrayList<>();

        private Channel(UUID id, GlobalPos source, UUID owner, String name, boolean publicChannel) {
            this.id = id;
            this.source = source;
            this.owner = owner;
            this.name = name;
            this.publicChannel = publicChannel;
        }

        public UUID owner() { return owner; }
        public String name() { return name; }
        public boolean isPublic() { return publicChannel; }
        public boolean visibleTo(UUID player) { return publicChannel || editableBy(player); }
        public boolean editableBy(UUID player) { return owner.equals(player); }
        public List<GlobalPos> targets() { return List.copyOf(targets); }
    }

    private static final Factory<PowerChannels> FACTORY = new Factory<>(PowerChannels::new, PowerChannels::load);
    private final Map<UUID, Channel> channels = new LinkedHashMap<>();
    private long revision;
    private ServerLevel server;

    public static PowerChannels get(ServerLevel level) {
        var data = level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, "lyycore_power_channels");
        data.server = level.getServer().overworld();
        return data;
    }

    public long revision() { return revision; }
    public Channel channel(UUID id) { return channels.get(id); }
    public List<Channel> visibleTo(UUID player) {
        return channels.values().stream().filter(channel -> channel.visibleTo(player)).toList();
    }

    public Channel create(GlobalPos source, UUID owner) {
        for (var channel : channels.values()) if (channel.source.equals(source)) return channel;
        var player = server.getServer().getPlayerList().getPlayer(owner);
        String name = player == null ? "时空矩阵频道" : player.getDisplayName().getString() + "的频道";
        var channel = new Channel(UUID.randomUUID(), source, owner, cleanName(name), false);
        channels.put(channel.id, channel);
        changed();
        return channel;
    }

    /** Metadata is owned by the creator even when bindings are public. */
    public boolean update(UUID id, UUID actor, String name, boolean publicChannel) {
        var channel = channel(id);
        name = cleanName(name);
        if (channel == null || !channel.editableBy(actor) || name.isEmpty()) return false;
        if (name.equals(channel.name) && publicChannel == channel.publicChannel) return false;
        channel.name = name;
        channel.publicChannel = publicChannel;
        changed();
        return true;
    }

    public boolean addTarget(UUID id, UUID actor, GlobalPos target) {
        var channel = channel(id);
        target = receiver(server, target);
        if (channel == null || !channel.visibleTo(actor) || channel.targets.size() >= TARGET_LIMIT
                || containsReceiver(channel, target)) return false;
        channel.targets.add(target);
        changed();
        return true;
    }

    public boolean removeTarget(UUID id, UUID actor, GlobalPos target) {
        var channel = channel(id);
        if (channel == null || !channel.visibleTo(actor)) return false;
        var receiver = receiver(server, target);
        for (var connection : channel.targets)
            if (receiver(server, connection).equals(receiver)) return removeInvalidTarget(id, connection);
        return false;
    }

    /** Merges bindings until the destination is full; existing receivers are skipped. */
    public boolean copyTargets(UUID from, UUID to, UUID actor) {
        var source = channel(from);
        var target = channel(to);
        if (source == null || target == null || source == target || !source.visibleTo(actor) || !target.visibleTo(actor)) return false;
        boolean added = false;
        for (var connection : source.targets) {
            if (target.targets.size() >= TARGET_LIMIT) break;
            var receiver = receiver(server, connection);
            if (containsReceiver(target, receiver)) continue;
            target.targets.add(receiver);
            added = true;
        }
        if (!added) return false;
        changed();
        return true;
    }

    /** Server ticking may discard a capability that disappeared from a loaded destination. */
    public boolean removeInvalidTarget(UUID id, GlobalPos target) {
        var channel = channel(id);
        if (channel == null || !channel.targets.remove(target)) return false;
        changed();
        return true;
    }

    public void removeMatrix(GlobalPos source) {
        if (channels.values().removeIf(channel -> channel.source.equals(source))) changed();
    }

    public static GlobalPos receiver(ServerLevel source, GlobalPos connection) {
        var destination = source.getServer().getLevel(connection.dimension());
        return destination != null && destination.hasChunkAt(connection.pos())
                ? GlobalPos.of(connection.dimension(), EnergyReceiver.controller(destination, connection.pos())) : connection;
    }

    private boolean containsReceiver(Channel channel, GlobalPos target) {
        return channel.targets.stream().anyMatch(connection -> receiver(server, connection).equals(target));
    }

    private void changed() { revision++; setDirty(); }
    private static String cleanName(String value) {
        var name = StringUtil.filterText(value).strip();
        return name.substring(0, Math.min(name.length(), NAME_LIMIT));
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var entries = new ListTag();
        for (var channel : channels.values()) {
            var entry = new CompoundTag();
            entry.putUUID("Id", channel.id);
            entry.putUUID("Owner", channel.owner);
            entry.putString("Name", channel.name);
            entry.putBoolean("Public", channel.publicChannel);
            entry.put("Source", GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, channel.source).getOrThrow());
            var targets = new ListTag();
            for (var target : channel.targets) targets.add(GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, target).getOrThrow());
            entry.put("Targets", targets);
            entries.add(entry);
        }
        tag.put("Channels", entries);
        return tag;
    }

    private static PowerChannels load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new PowerChannels();
        for (var raw : tag.getList("Channels", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag) raw;
            if (!entry.hasUUID("Id") || !entry.hasUUID("Owner")) continue;
            var source = GlobalPos.CODEC.parse(NbtOps.INSTANCE, entry.get("Source")).result();
            if (source.isEmpty()) continue;
            var channel = new Channel(entry.getUUID("Id"), source.get(), entry.getUUID("Owner"),
                    cleanName(entry.getString("Name")), entry.getBoolean("Public"));
            for (var value : entry.getList("Targets", Tag.TAG_COMPOUND)) {
                if (channel.targets.size() >= TARGET_LIMIT) break;
                GlobalPos.CODEC.parse(NbtOps.INSTANCE, value).result().ifPresent(target -> {
                    if (!channel.targets.contains(target)) channel.targets.add(target);
                });
            }
            data.channels.put(channel.id, channel);
        }
        return data;
    }
}
