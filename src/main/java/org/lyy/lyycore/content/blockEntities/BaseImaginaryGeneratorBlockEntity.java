package org.lyy.lyycore.content.blockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.lyy.lyycore.Config;
import org.lyy.lyycore.registry.LyyBlockEntities;

import java.util.ArrayList;
import java.util.Iterator;

public class BaseImaginaryGeneratorBlockEntity extends BlockEntity {
    private static final int SUPPLY_RADIUS = 5;
    private final ArrayList<Target> savedTargets = new ArrayList<>();
    private boolean needsScan = true;
    private long nextScanGameTime;

    private record Target(BlockPos pos, Direction side) { }

    public BaseImaginaryGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(LyyBlockEntities.IGB.get(), pos, state);
    }

    public static void getPositions(BaseImaginaryGeneratorBlockEntity be) {
        be.scanTargets();
    }

    public int getTargetCount() { return savedTargets.size(); }

    public void requestScan() {
        needsScan = true;
        nextScanGameTime = 0;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        requestScan();
    }

    private void scanTargets() {
        if (level == null || level.isClientSide) return;
        savedTargets.clear();
        BlockPos origin = getBlockPos();
        int distance = SUPPLY_RADIUS;
        for (int dx = -distance; dx <= distance; dx++) {
            for (int dy = -distance; dy <= distance; dy++) {
                for (int dz = -distance; dz <= distance; dz++) {
                    BlockPos cur = origin.offset(dx, dy, dz);
                    if (cur.equals(origin)) continue;
                    if (!level.hasChunkAt(cur)) continue;

                    IEnergyStorage unsided = level.getCapability(Capabilities.EnergyStorage.BLOCK, cur, null);
                    if (unsided != null && unsided.canReceive()) {
                        savedTargets.add(new Target(cur.immutable(), null));
                        continue;
                    }

                    for (Direction side : Direction.values()) {
                        IEnergyStorage sided = level.getCapability(Capabilities.EnergyStorage.BLOCK, cur, side);
                        if (sided != null && sided.canReceive()) {
                            savedTargets.add(new Target(cur.immutable(), side));
                            break;
                        }
                    }
                }
            }
        }
        needsScan = false;
        nextScanGameTime = level.getGameTime() + Config.GENERATOR_RESCAN_INTERVAL.get();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BaseImaginaryGeneratorBlockEntity be) {
        if (level.isClientSide) return;
        if (be.needsScan || level.getGameTime() >= be.nextScanGameTime) be.scanTargets();
        if (be.savedTargets.isEmpty()) return;
        provideEnergy(level, be);
    }

    private static void provideEnergy(Level level, BaseImaginaryGeneratorBlockEntity be) {
        int amount = Config.GENERATOR_FE_PER_TARGET_TICK.get();
        if (amount <= 0) return;

        Iterator<Target> iterator = be.savedTargets.iterator();
        while (iterator.hasNext()) {
            Target target = iterator.next();
            if (!level.hasChunkAt(target.pos())) {
                iterator.remove();
                continue;
            }
            IEnergyStorage energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, target.pos(), target.side());
            if (energy == null || !energy.canReceive()) iterator.remove();
            else energy.receiveEnergy(amount, false);
        }
    }
}
