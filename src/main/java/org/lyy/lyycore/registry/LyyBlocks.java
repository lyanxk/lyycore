package org.lyy.lyycore.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.content.blocks.*;
import org.lyy.lyycore.content.ResourceFrameKind;
import org.lyy.lyycore.content.FrameProduction;
import org.lyy.lyycore.content.blocks.CrystalCondensingFrameBlock;
import org.lyy.lyycore.content.blocks.BaseImaginaryGenerator;
import org.lyy.lyycore.content.blocks.EnergyCellBlock;
import org.lyy.lyycore.content.blocks.ImaginaryAlloyForge;
import org.lyy.lyycore.content.blocks.ImaginaryCraftingTableBlock;
import org.lyy.lyycore.content.blocks.ImaginaryResearchTableBlock;
import org.lyy.lyycore.content.blocks.ProductionLabBlock;
import org.lyy.lyycore.content.blocks.ImaginaryGateBlock;
import org.lyy.lyycore.content.blocks.AdvancedImaginaryGateBlock;
import org.lyy.lyycore.content.blocks.EnderSentryBlock;
import org.lyy.lyycore.content.blocks.ItemCollectorBlock;
import org.lyy.lyycore.content.blocks.ResourceGatheringFrameBlock;
import java.util.EnumMap;
import java.util.Map;

public class LyyBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(LyyCore.MODID);
    public static final DeferredBlock<AlloyCauldronBlock> ALLOY_CAULDRON = BLOCKS.register("alloy_cauldron", AlloyCauldronBlock::new);
    public static final DeferredBlock<MindControlBeaconBlock> MIND_CONTROL_BEACON = BLOCKS.register("mind_control_beacon", MindControlBeaconBlock::new);
    public static final DeferredBlock<ImaginaryCondensingBeaconBlock> IMAGINARY_CONDENSING_BEACON = BLOCKS.register("imaginary_condensing_beacon", ImaginaryCondensingBeaconBlock::new);
    public static final DeferredBlock<SpatialTransmissionTowerBlock> SPATIAL_TRANSMISSION_TOWER = BLOCKS.register("spatial_transmission_tower", SpatialTransmissionTowerBlock::new);
    public static final DeferredBlock<ProductionLabBlock> PRODUCTION_LAB = BLOCKS.register("production_lab", ProductionLabBlock::new);
    public static final DeferredBlock<ImaginaryResearchTableBlock> IMAGINARY_RESEARCH_TABLE =
            BLOCKS.register("imaginary_research_table", ImaginaryResearchTableBlock::new);
    public static final DeferredBlock<Block> CRYSTAL_CONDENSING_FRAME =
            BLOCKS.register("crystal_condensing_frame", () -> new CrystalCondensingFrameBlock(FrameProduction.STANDARD));
    public static final DeferredBlock<Block> MINIATURE_CRYSTAL_FACTORY =
            BLOCKS.register("miniature_crystal_factory", () -> new CrystalCondensingFrameBlock(FrameProduction.MINIATURE_FACTORY));
    public static final Map<ResourceFrameKind, DeferredBlock<Block>> RESOURCE_FACTORIES = new EnumMap<>(ResourceFrameKind.class);
    public static final Map<ResourceFrameKind, DeferredBlock<Block>> RESOURCE_FRAMES = new EnumMap<>(ResourceFrameKind.class);
    static {
        for (var kind : ResourceFrameKind.values()) RESOURCE_FRAMES.put(kind,
                BLOCKS.register(kind.blockId(), () -> ResourceGatheringFrameBlock.create(kind)));
        for (var kind : ResourceFrameKind.FACTORY_KINDS) RESOURCE_FACTORIES.put(kind,
                BLOCKS.register(kind.factoryId(), () -> ResourceGatheringFrameBlock.create(kind, FrameProduction.MINIATURE_FACTORY)));
    }

    public static final DeferredBlock<Block> IMAGINARY_ENERGY_CELL =
            BLOCKS.register("imaginary_energy_cell", EnergyCellBlock::new);
    public static final DeferredBlock<Block> ITEM_COLLECTOR =
            BLOCKS.register("item_collector", ItemCollectorBlock::new);
    public static final DeferredBlock<Block> IAF =
            BLOCKS.register("imaginary_alloy_forge", ImaginaryAlloyForge::new);
    public static final DeferredBlock<Block> IGB =
            BLOCKS.register("im_generator", BaseImaginaryGenerator::new);

    public static final DeferredBlock<Block> CRYSTAL_BLOCK = BLOCKS.register("crystal_block",
            () -> new TransparentBlock(BlockBehaviour.Properties.of().strength(3).sound(SoundType.AMETHYST)
                    .noOcclusion()
                    .isViewBlocking((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isRedstoneConductor((state, level, pos) -> false)));
    public static final DeferredBlock<Block> ALLOY_BLOCK = BLOCKS.register("alloy_block",
            () -> new Block(BlockBehaviour.Properties.of().strength(5).sound(SoundType.METAL)));
    public static final DeferredBlock<ImaginaryGateBlock> IMAGINARY_GATE =
            BLOCKS.register("imaginary_gate", ImaginaryGateBlock::new);
    public static final DeferredBlock<AdvancedImaginaryGateBlock> ADVANCED_IMAGINARY_GATE =
            BLOCKS.register("advanced_imaginary_gate", AdvancedImaginaryGateBlock::new);
    public static final DeferredBlock<EnderSentryBlock> ENDER_SENTRY = BLOCKS.register("ender_sentry", EnderSentryBlock::new);
    public static final DeferredBlock<ImaginaryCraftingTableBlock> IMAGINARY_CRAFTING_TABLE =
            BLOCKS.register("imaginary_crafting_table", ImaginaryCraftingTableBlock::new);

    // Simple ore blocks — no custom class needed
    public static final DeferredBlock<Block> IMAGINIUM_ORE =
            BLOCKS.register("imaginium_ore", () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE).strength(3.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> DEEPSLATE_IMAGINIUM_ORE =
            BLOCKS.register("deepslate_imaginium_ore", () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE).strength(3.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()));
}
