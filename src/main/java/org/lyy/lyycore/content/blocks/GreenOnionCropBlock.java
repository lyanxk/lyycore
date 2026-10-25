package org.lyy.lyycore.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.lyy.lyycore.registry.LyyItems;

public final class GreenOnionCropBlock extends CropBlock {
    public static final MapCodec<GreenOnionCropBlock> CODEC = simpleCodec(GreenOnionCropBlock::new);
    public GreenOnionCropBlock(BlockBehaviour.Properties properties) { super(properties); }
    @Override public MapCodec<GreenOnionCropBlock> codec() { return CODEC; }
    @Override protected ItemLike getBaseSeedId() { return LyyItems.GREEN_ONION.get(); }
}
