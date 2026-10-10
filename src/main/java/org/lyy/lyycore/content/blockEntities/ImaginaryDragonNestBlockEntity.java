package org.lyy.lyycore.content.blockEntities;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.lyy.lyycore.content.*;
import org.lyy.lyycore.content.entity.ImaginaryDragon;
import org.lyy.lyycore.content.menu.DragonNestMenu;
import org.lyy.lyycore.registry.*;

/** The nest owns production and decisions, including while its dragon is absent. */
public final class ImaginaryDragonNestBlockEntity extends BlockEntity implements MenuProvider {
    public enum Mode { PATROL, BREATH, STOWED }
    public static final int EGG_TICKS = 6000, DECISION_TICKS = 100;
    private UUID owner, dragon;
    private Mode mode = Mode.PATROL;
    private int eggTicks, decisionTicks;
    private boolean tickets;
    public final ItemStackHandler output = new ItemStackHandler(1) {
        @Override public boolean isItemValid(int slot, ItemStack stack) { return false; }
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return stack; }
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    public final ContainerData data = new ContainerData() {
        public int get(int index) { return index == 0 ? eggTicks : mode.ordinal(); }
        public void set(int index, int value) { }
        public int getCount() { return 2; }
    };
    public ImaginaryDragonNestBlockEntity(BlockPos pos, BlockState state) { super(LyyBlockEntities.IMAGINARY_DRAGON_NEST.get(), pos, state); }
    public void bind(ServerPlayer player) { owner = player.getUUID(); EnderCompanions.bindNest(player, worldPosition); setChanged(); }
    public boolean owns(ImaginaryDragon entity) { return entity.getUUID().equals(dragon) && mode != Mode.STOWED; }
    public boolean canUse(Player player) { return owner != null && owner.equals(player.getUUID()); }
    public Mode mode() { return mode; }
    public void select(ServerPlayer player, Mode selected) {
        if (!canUse(player) || mode == selected) return;
        mode = selected; recall(); decisionTicks = 0; setChanged();
    }
    public void cycle(ServerPlayer player) {
        if (!canUse(player)) return;
        select(player, Mode.values()[(mode.ordinal()+1)%Mode.values().length]);
        player.displayClientMessage(Component.translatable("message.lyycore.dragon_mode", Component.translatable("gui.lyycore.dragon."+mode.name().toLowerCase(Locale.ROOT))), true);
    }
    private ImaginaryDragon active() { return dragon != null && level instanceof ServerLevel server && server.getEntity(dragon) instanceof ImaginaryDragon d && !d.isRemoved() ? d : null; }
    private void recall() { var entity = active(); if (entity != null) entity.dismiss(); else dragon = null; }
    private void chunks(boolean force) {
        if (!(level instanceof ServerLevel server)) return;
        for (int x = (worldPosition.getX()-1)>>4; x <= (worldPosition.getX()+1)>>4; x++)
            for (int z = (worldPosition.getZ()-1)>>4; z <= (worldPosition.getZ()+1)>>4; z++)
                DragonNests.TICKETS.forceChunk(server, worldPosition, x, z, force, true);
        tickets = force;
    }
    public void release() {
        recall(); chunks(false);
        Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), output.getStackInSlot(0));
    }
    public static AABB area(ServerPlayer player) { return new AABB(player.position(), player.position()).inflate(10); }
    public static boolean hostile(LivingEntity entity, ServerPlayer player) {
        return entity instanceof Enemy && entity.isAlive() && !entity.isInvulnerable() && !entity.isAlliedTo(player);
    }
    public static void serverTick(Level level, BlockPos pos, BlockState state, ImaginaryDragonNestBlockEntity nest) {
        if (!nest.tickets) nest.chunks(true);
        var server = (ServerLevel) level;
        var player = nest.owner == null ? null : server.getServer().getPlayerList().getPlayer(nest.owner);
        // Production remains active without the player; the unique owner binding is established on placement.
        if (nest.owner != null && nest.output.getStackInSlot(0).getCount() < 64) {
            if (++nest.eggTicks >= EGG_TICKS) {
                nest.output.setStackInSlot(0, new ItemStack(Items.DRAGON_EGG, nest.output.getStackInSlot(0).getCount()+1)); nest.eggTicks = 0;
            }
            nest.setChanged();
        }
        if (++nest.decisionTicks < DECISION_TICKS) return;
        nest.decisionTicks = 0;
        if (nest.mode == Mode.STOWED || player == null || !player.isAlive() || player.isSpectator() || player.level() != level
                || !EnderCompanions.evolved(player) || !EnderCompanions.isNest(player, level.dimension(), pos)) { nest.recall(); return; }
        var current = nest.active();
        if (current != null && current.busy()) return;
        var targets = level.getEntitiesOfClass(LivingEntity.class, area(player), e -> hostile(e, player));
        if (targets.isEmpty()) return;
        var entity = current != null ? current : LyyEntities.IMAGINARY_DRAGON.get().create(level);
        if (entity == null) return;
        boolean breath = nest.mode == Mode.BREATH || targets.size() >= 10;
        var target = targets.stream().min(Comparator.comparingDouble(player::distanceToSqr)).orElseThrow();
        entity.begin(player, pos, target, breath);
        if (current == null && server.addFreshEntity(entity)) { nest.dragon = entity.getUUID(); nest.setChanged(); }
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider r) {
        super.saveAdditional(tag, r); if (owner != null) tag.putUUID("Owner", owner); if (dragon != null) tag.putUUID("Dragon", dragon);
        tag.putInt("Mode", mode.ordinal()); tag.putInt("EggTicks", eggTicks); tag.put("Output", output.serializeNBT(r));
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider r) {
        super.loadAdditional(tag, r); owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null; dragon = tag.hasUUID("Dragon") ? tag.getUUID("Dragon") : null;
        mode = Mode.values()[Math.clamp(tag.getInt("Mode"), 0, 2)]; eggTicks = Math.clamp(tag.getInt("EggTicks"), 0, EGG_TICKS-1); output.deserializeNBT(r, tag.getCompound("Output"));
    }
    @Override public Component getDisplayName() { return Component.translatable("block.lyycore.imaginary_dragon_nest"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) { return new DragonNestMenu(id, inv, this, data); }
}
