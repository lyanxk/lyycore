package org.lyy.lyycore.content.blockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.lyy.lyycore.content.menu.AdvancedImaginaryGateMenu;
import org.lyy.lyycore.registry.LyyBlockEntities;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AdvancedImaginaryGateBlockEntity extends ImaginaryGateBlockEntity {
    public static final int RADIUS = 8;
    private static final List<BlockPos> COLUMNS = columns();
    private ResourceLocation selectedBiome = ResourceLocation.withDefaultNamespace("plains");
    private int completedColumns, elapsedTicks;
    private boolean running, waitingForChunk;

    public AdvancedImaginaryGateBlockEntity(BlockPos pos, BlockState state) {
        super(LyyBlockEntities.ADVANCED_IMAGINARY_GATE.get(), pos, state);
    }
    private static List<BlockPos> columns() {
        List<BlockPos> result = new ArrayList<>();
        for (int x = -RADIUS; x <= RADIUS; x++) for (int z = -RADIUS; z <= RADIUS; z++)
            if (x * x + z * z <= RADIUS * RADIUS) result.add(new BlockPos(x, 0, z));
        result.sort(Comparator.comparingDouble(pos -> pos.distSqr(BlockPos.ZERO)));
        return List.copyOf(result);
    }
    public ResourceLocation selectedBiome() { return selectedBiome; }
    public int completedColumns() { return completedColumns; }
    public static int totalColumns() { return COLUMNS.size(); }
    public boolean running() { return running; }
    public boolean waitingForChunk() { return waitingForChunk; }
    public void selectBiome(ResourceLocation id) {
        if (running || level == null || !level.registryAccess().registryOrThrow(Registries.BIOME).containsKey(id)) return;
        if (!id.equals(selectedBiome)) { selectedBiome = id; completedColumns = 0; elapsedTicks = 0; }
        setChanged();
    }
    public void setRunning(boolean value) {
        running = value;
        waitingForChunk = false;
        if (value && completedColumns == COLUMNS.size()) completedColumns = 0;
        setChanged();
    }
    public static void serverTick(Level level, BlockPos pos, BlockState state, AdvancedImaginaryGateBlockEntity gate) {
        ImaginaryGateBlockEntity.serverTick(level, pos, state, gate);
        if (!gate.running || ++gate.elapsedTicks < 20) return;
        gate.elapsedTicks = 0;
        gate.convertNextColumn((ServerLevel) level);
    }
    private void convertNextColumn(ServerLevel server) {
        var biome = server.registryAccess().registryOrThrow(Registries.BIOME).getHolder(selectedBiome).orElse(null);
        if (biome == null) { setRunning(false); return; }
        BlockPos column = worldPosition.offset(COLUMNS.get(completedColumns));
        var chunk = server.getChunk(column.getX() >> 4, column.getZ() >> 4, ChunkStatus.FULL, false);
        waitingForChunk = chunk == null;
        if (chunk == null) return; // Never generate or force-load a neighbouring chunk.
        int quartX = QuartPos.fromBlock(column.getX()), quartZ = QuartPos.fromBlock(column.getZ());
        // Vanilla stores biomes in 4x4x4 cells. Preserve every other column at every height.
        chunk.fillBiomesFromNoise((x, y, z, sampler) -> x == quartX && z == quartZ
                ? biome : chunk.getNoiseBiome(x, y, z), server.getChunkSource().randomState().sampler());
        chunk.setUnsaved(true);
        server.getChunkSource().chunkMap.resendBiomesForChunks(List.of(chunk));
        if (++completedColumns == COLUMNS.size()) running = false;
        setChanged();
    }
    @Override public Component getDisplayName() { return Component.translatable("block.lyycore.advanced_imaginary_gate"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AdvancedImaginaryGateMenu(id, inventory, this, data);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Biome", selectedBiome.toString());
        tag.putInt("Columns", completedColumns);
        tag.putInt("Elapsed", elapsedTicks);
        tag.putBoolean("Running", running);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ResourceLocation biome = ResourceLocation.tryParse(tag.getString("Biome"));
        selectedBiome = biome == null ? ResourceLocation.withDefaultNamespace("plains") : biome;
        completedColumns = Math.clamp(tag.getInt("Columns"), 0, COLUMNS.size());
        elapsedTicks = Math.clamp(tag.getInt("Elapsed"), 0, 19);
        running = tag.getBoolean("Running") && completedColumns < COLUMNS.size();
    }
}
