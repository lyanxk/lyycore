package org.lyy.lyycore.content.blockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.lyy.lyycore.registry.LyyBlockEntities;

import java.util.ArrayList;
import java.util.List;

public class BaseImaginaryGeneratorBlockEntity extends BlockEntity {
    private final ArrayList<BlockPos> savedPositions = new ArrayList<>();
    private static final int DISTANCE = 5;

    public BaseImaginaryGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(LyyBlockEntities.IGB.get(), pos, state);
    }

    public static void getPositions(BaseImaginaryGeneratorBlockEntity be) {
        if (be.level == null) return;
        be.savedPositions.clear();
        BlockPos origin = be.getBlockPos();
        for (int dx = -DISTANCE; dx <= DISTANCE; dx++) {
            for (int dy = -DISTANCE; dy <= DISTANCE; dy++) {
                for (int dz = -DISTANCE; dz <= DISTANCE; dz++) {
                    BlockPos cur = origin.offset(dx, dy, dz);
                    if (cur.equals(origin)) continue;
                    if (be.level.getCapability(Capabilities.EnergyStorage.BLOCK, cur, null) != null) {
                        be.savedPositions.add(cur.immutable());
                    }
                }
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BaseImaginaryGeneratorBlockEntity be) {
        if (level.isClientSide || be.savedPositions.isEmpty()) return;
        provideEnergy(level, be);
    }

    private static void provideEnergy(Level level, BaseImaginaryGeneratorBlockEntity be) {
        List<BlockPos> toRemove = new ArrayList<>();
        for (BlockPos targetPos : be.savedPositions) {
            boolean delivered = false;
            for (Direction d : Direction.values()) {
                IEnergyStorage energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, targetPos, d);
                if (energy != null && energy.canReceive()) {
                    energy.receiveEnergy(512, false);
                    delivered = true;
                    break;
                }
            }
            if (!delivered) {
                // Check if block still has energy capability at all
                if (level.getCapability(Capabilities.EnergyStorage.BLOCK, targetPos, null) == null) {
                    toRemove.add(targetPos);
                }
            }
        }
        be.savedPositions.removeAll(toRemove);
    }
}
