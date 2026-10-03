package org.lyy.lyycore.content.blockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.lyy.lyycore.Config;
import org.lyy.lyycore.registry.LyyBlockEntities;
import org.lyy.lyycore.registry.LyyCapabilities;
import org.lyy.lyycore.energy.ImaginaryEnergy;
import org.lyy.lyycore.energy.IEnergyConversion;

import java.util.ArrayList;
import java.util.Iterator;

public class BaseImaginaryGeneratorBlockEntity extends BlockEntity {
    private static final int SUPPLY_RADIUS = 5;
    private final ArrayList<Target> savedTargets = new ArrayList<>();
    private boolean needsScan = true;
    private long nextScanGameTime;
    private int generationRemainderFE;

    private record Target(BlockPos pos, Direction side, boolean nativeIE) { }

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

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("GenerationRemainderFE", generationRemainderFE);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        generationRemainderFE = Math.clamp(tag.getInt("GenerationRemainderFE"), 0, IEnergyConversion.FE_PER_IMAGINARY - 1);
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

                    Target nativeTarget = findImaginaryTarget(cur);
                    if (nativeTarget != null) {
                        savedTargets.add(nativeTarget);
                        continue;
                    }

                    IEnergyStorage unsided = level.getCapability(Capabilities.EnergyStorage.BLOCK, cur, null);
                    if (unsided != null && unsided.canReceive()) {
                        savedTargets.add(new Target(cur.immutable(), null, false));
                        continue;
                    }

                    for (Direction side : Direction.values()) {
                        IEnergyStorage sided = level.getCapability(Capabilities.EnergyStorage.BLOCK, cur, side);
                        if (sided != null && sided.canReceive()) {
                            savedTargets.add(new Target(cur.immutable(), side, false));
                            break;
                        }
                    }
                }
            }
        }
        needsScan = false;
        nextScanGameTime = level.getGameTime() + Config.GENERATOR_RESCAN_INTERVAL.get();
    }

    private Target findImaginaryTarget(BlockPos pos) {
        ImaginaryEnergy unsided = level.getCapability(LyyCapabilities.IMAGINARY_ENERGY, pos, null);
        if (unsided != null && unsided.canReceiveImaginaryEnergy()) return new Target(pos.immutable(), null, true);
        for (Direction side : Direction.values()) {
            ImaginaryEnergy sided = level.getCapability(LyyCapabilities.IMAGINARY_ENERGY, pos, side);
            if (sided != null && sided.canReceiveImaginaryEnergy()) return new Target(pos.immutable(), side, true);
        }
        return null;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BaseImaginaryGeneratorBlockEntity be) {
        if (level.isClientSide) return;
        if (be.needsScan || level.getGameTime() >= be.nextScanGameTime) be.scanTargets();
        if (be.savedTargets.isEmpty()) return;
        provideEnergy(level, be);
    }

    private static void provideEnergy(Level level, BaseImaginaryGeneratorBlockEntity be) {
        int amountFE = Config.GENERATOR_FE_PER_TARGET_TICK.get();
        if (amountFE <= 0) return;
        // Native IE storage is integral. Carry the fraction across ticks and saves:
        // 512 FE/t supplies exactly 128 IE over 25 ticks, rather than truncating to 125.
        int fraction = be.generationRemainderFE + amountFE % IEnergyConversion.FE_PER_IMAGINARY;
        int amountIE = amountFE / IEnergyConversion.FE_PER_IMAGINARY + fraction / IEnergyConversion.FE_PER_IMAGINARY;
        be.generationRemainderFE = fraction % IEnergyConversion.FE_PER_IMAGINARY;
        be.setChanged();

        Iterator<Target> iterator = be.savedTargets.iterator();
        while (iterator.hasNext()) {
            Target target = iterator.next();
            if (!level.hasChunkAt(target.pos())) {
                iterator.remove();
                continue;
            }
            if (target.nativeIE()) {
                ImaginaryEnergy energy = level.getCapability(LyyCapabilities.IMAGINARY_ENERGY, target.pos(), target.side());
                if (energy == null || !energy.canReceiveImaginaryEnergy()) iterator.remove();
                else energy.receiveImaginaryEnergy(amountIE, false);
            } else {
                IEnergyStorage energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, target.pos(), target.side());
                if (energy == null || !energy.canReceive()) iterator.remove();
                else energy.receiveEnergy(amountFE, false);
            }
        }
    }
}
