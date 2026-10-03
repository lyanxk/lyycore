package org.lyy.lyycore.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.content.blocks.BaseImaginaryGenerator;
import org.lyy.lyycore.content.blocks.EnergyCellBlock;
import org.lyy.lyycore.content.blocks.ImaginaryAlloyForge;
import org.lyy.lyycore.content.blocks.ItemCollectorBlock;
import org.lyy.lyycore.content.ResourceFrameKind;
import org.lyy.lyycore.content.blocks.ResourceGatheringFrameBlock;
import java.util.EnumMap;
import java.util.Map;

public class LyyBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(LyyCore.MODID);
    public static final DeferredBlock<Block> CRYSTAL_CONDENSING_FRAME =
            BLOCKS.register("crystal_condensing_frame", org.lyy.lyycore.content.blocks.CrystalCondensingFrameBlock::new);
    public static final Map<ResourceFrameKind, DeferredBlock<Block>> RESOURCE_FRAMES = new EnumMap<>(ResourceFrameKind.class);
    static {
        for (var kind : ResourceFrameKind.values()) RESOURCE_FRAMES.put(kind,
                BLOCKS.register(kind.blockId(), () -> ResourceGatheringFrameBlock.create(kind)));
    }

    public static final DeferredBlock<Block> IMAGINARY_ENERGY_CELL =
            BLOCKS.register("imaginary_energy_cell", EnergyCellBlock::new);
    public static final DeferredBlock<Block> ITEM_COLLECTOR =
            BLOCKS.register("item_collector", ItemCollectorBlock::new);
    public static final DeferredBlock<Block> IAF =
            BLOCKS.register("imaginary_alloy_forge", ImaginaryAlloyForge::new);
    public static final DeferredBlock<Block> IGB =
            BLOCKS.register("im_generator", BaseImaginaryGenerator::new);

    // Simple ore blocks — no custom class needed
    public static final DeferredBlock<Block> IMAGINIUM_ORE =
            BLOCKS.register("imaginium_ore", () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE).strength(3.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> DEEPSLATE_IMAGINIUM_ORE =
            BLOCKS.register("deepslate_imaginium_ore", () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE).strength(3.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()));
}
