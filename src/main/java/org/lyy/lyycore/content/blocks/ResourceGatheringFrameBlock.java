package org.lyy.lyycore.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.lyy.lyycore.content.ResourceFrameKind;
import org.lyy.lyycore.content.blockEntities.ResourceGatheringFrameBlockEntity;
import org.lyy.lyycore.registry.LyyBlockEntities;

public class ResourceGatheringFrameBlock extends CondensingFrameBlock {
    private final ResourceFrameKind kind;

    private ResourceGatheringFrameBlock(ResourceFrameKind kind) { this.kind = kind; }

    public static ResourceGatheringFrameBlock create(ResourceFrameKind kind) {
        return kind.supportsWater() ? new Waterlogged(kind) : new ResourceGatheringFrameBlock(kind);
    }
    public ResourceFrameKind kind() { return kind; }
    @Override protected MapCodec<ResourceGatheringFrameBlock> codec() {
        return simpleCodec(properties -> create(kind));
    }
    @Override public BlockEntityType<ResourceGatheringFrameBlockEntity> frameBlockEntityType() {
        return LyyBlockEntities.RESOURCE_GATHERING_FRAME.get();
    }

    // Waterlogging needs both the vanilla interface and a state property. The
    // ordinary variant (nether) has neither and keeps vanilla item interactions.
    private static final class Waterlogged extends ResourceGatheringFrameBlock implements SimpleWaterloggedBlock {
        private Waterlogged(ResourceFrameKind kind) { super(kind); }

        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(WATERLOGGED);
        }
    }
}
