package org.lyy.lyycore.content.blocks;

import net.minecraft.world.level.block.BaseEntityBlock;

/** Shared blast resistance for machines, including every part of a multiblock. */
public abstract class MachineBlock extends BaseEntityBlock {
    public static final float BLAST_RESISTANCE = 3_600_000F;

    protected MachineBlock(Properties properties) {
        super(properties.explosionResistance(BLAST_RESISTANCE));
    }
}
