package org.lyy.lyycore.content.blockEntities;

import java.util.List;
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
    // Execution, active scanning and mode changes each share one server-tick budget per beacon.
    private final long[] commandTicks = {Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE};
    private final ContainerData data = new ContainerData() {
        public int get(int index) {
            var binding = binding();
            return switch (index) {
                case 0 -> binding == null ? 0 : binding.mobs.size();
                case 1 -> binding == null ? 0 : binding.mode().ordinal();
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
        if (!(player instanceof ServerPlayer) || !(level instanceof ServerLevel server)
                || player.level() != level || !player.isAlive() || player.isSpectator()
                || !player.getUUID().equals(owner) || command < 0 || command > 4) return false;
        var binding = binding();
        if (binding == null) return false;
        var mode = switch (command) {
            case 1 -> MindControlData.Mode.GATHER;
            case 4 -> MindControlData.Mode.ATTACK;
            default -> MindControlData.Mode.WANDER;
        };
        if (command != 0 && command != 2 && binding.mode() == mode) return true;
        int category = command == 0 ? 0 : command == 2 ? 1 : 2;
        long tick = server.getServer().getTickCount();
        if (commandTicks[category] == tick) return false;
        // Consume before dispatch: entity and damage callbacks may reenter this method.
        commandTicks[category] = tick;
        var saved = MindControlData.get(server);
        if (command == 0) {
            var victims = List.copyOf(binding.mobs);
            saved.execute(binding);
            for (var id : victims) for (var dimension : server.getServer().getAllLevels()) {
                if (dimension.getEntity(id) instanceof Mob mob) { MindControl.processExecution(mob); break; }
            }
        } else if (command == 2) {
            scan(binding, new AABB(worldPosition.getX() - 30, worldPosition.getY() - 10, worldPosition.getZ() - 30,
                    worldPosition.getX() + 30, worldPosition.getY() + 70, worldPosition.getZ() + 30));
        } else if (saved.setMode(binding, mode)) {
            for (var id : List.copyOf(binding.mobs)) for (var dimension : server.getServer().getAllLevels()) {
                if (dimension.getEntity(id) instanceof Mob mob) { mob.setTarget(null); mob.getNavigation().stop(); break; }
            }
        }
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
