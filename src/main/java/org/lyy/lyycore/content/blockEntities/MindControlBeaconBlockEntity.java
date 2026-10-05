package org.lyy.lyycore.content.blockEntities;

import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.lyy.lyycore.content.control.*;
import org.lyy.lyycore.content.menu.MindControlMenu;
import org.lyy.lyycore.registry.LyyBlockEntities;

public final class MindControlBeaconBlockEntity extends BlockEntity implements MenuProvider {
    private static final int PASSIVE_SCAN_INTERVAL = 60;
    private UUID owner;
    private int passiveScanDelay = PASSIVE_SCAN_INTERVAL;
    private final ContainerData data = new ContainerData() {
        public int get(int index) {
            var binding = binding();
            return switch (index) {
                case 0 -> binding == null ? 0 : binding.mobs.size();
                case 1 -> binding == null ? 0 : binding.mode.ordinal();
                default -> 0;
            };
        }
        public void set(int index, int value) { }
        public int getCount() { return 2; }
    };
    public MindControlBeaconBlockEntity(BlockPos pos, BlockState state) { super(LyyBlockEntities.MIND_CONTROL_BEACON.get(), pos, state); }
    public MindControlData.Binding binding() {
        if (owner == null || !(level instanceof ServerLevel server)) return null;
        var binding = MindControlData.get(server).active(owner);
        return binding != null && binding.beacon.equals(GlobalPos.of(level.dimension(), worldPosition)) ? binding : null;
    }
    public boolean activate(ServerPlayer player) {
        if (owner != null && !owner.equals(player.getUUID())) {
            player.displayClientMessage(Component.translatable("message.lyycore.beacon_owned"), true);
            return false;
        }
        if (binding() == null) passiveScanDelay = PASSIVE_SCAN_INTERVAL;
        owner = player.getUUID();
        MindControlData.get(player.serverLevel()).activate(owner, GlobalPos.of(level.dimension(), worldPosition));
        setChanged();
        return true;
    }
    public boolean canUse(Player player) { return player.getUUID().equals(owner) && binding() != null; }
    public void release() {
        if (owner != null && level instanceof ServerLevel server)
            MindControlData.get(server).release(owner, GlobalPos.of(level.dimension(), worldPosition));
    }
    public boolean command(Player player, int command) {
        var binding = binding();
        if (!canUse(player) || binding == null || command < 0 || command > 4) return false;
        var server = (ServerLevel)level;
        var saved = MindControlData.get(server);
        if (command == 0) {
            var victims = java.util.List.copyOf(binding.mobs);
            saved.execute(binding);
            for (var dimension : server.getServer().getAllLevels()) for (var id : victims)
                if (dimension.getEntity(id) instanceof Mob mob && saved.takeExecution(id)) MindControl.execute(mob, player);
        } else if (command == 2) {
            // Active control scans the entire volume immediately, independently of the passive timer.
            scan(binding, new AABB(worldPosition.getX() - 30, worldPosition.getY() - 10, worldPosition.getZ() - 30,
                    worldPosition.getX() + 30, worldPosition.getY() + 70, worldPosition.getZ() + 30));
        } else {
            binding.mode = switch (command) {
                case 1 -> MindControlData.Mode.GATHER;
                case 4 -> MindControlData.Mode.ATTACK;
                default -> MindControlData.Mode.WANDER;
            };
            for (var dimension : server.getServer().getAllLevels()) for (var id : binding.mobs)
                if (dimension.getEntity(id) instanceof Mob mob) { mob.setTarget(null); mob.getNavigation().stop(); }
            saved.setDirty();
        }
        setChanged();
        return true;
    }
    public static void serverTick(Level level, BlockPos pos, BlockState state, MindControlBeaconBlockEntity beacon) {
        var binding = beacon.binding();
        if (binding == null || --beacon.passiveScanDelay > 0) return;
        beacon.scan(binding, new AABB(pos).inflate(5));
        beacon.passiveScanDelay = PASSIVE_SCAN_INTERVAL;
        beacon.setChanged();
    }
    private void scan(MindControlData.Binding binding, AABB area) {
        // Binding stores a set of UUIDs: scans accumulate every eligible monster without replacing earlier ones.
        for (var mob : level.getEntitiesOfClass(Mob.class, area, MindControl::eligible)) MindControl.bind(mob, binding);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putInt("PassiveScanDelay", passiveScanDelay);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        passiveScanDelay = tag.contains("PassiveScanDelay")
                ? Math.clamp(tag.getInt("PassiveScanDelay"), 1, PASSIVE_SCAN_INTERVAL) : PASSIVE_SCAN_INTERVAL;
    }
    @Override public Component getDisplayName() { return Component.translatable("block.lyycore.mind_control_beacon"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new MindControlMenu(id, inventory, this, data); }
}
