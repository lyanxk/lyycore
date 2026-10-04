package org.lyy.lyycore.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.content.blockEntities.*;
import org.lyy.lyycore.content.blockEntities.BaseImaginaryGeneratorBlockEntity;
import org.lyy.lyycore.content.blockEntities.CrystalCondensingFrameBlockEntity;
import org.lyy.lyycore.content.blockEntities.EnergyCellBlockEntity;
import org.lyy.lyycore.content.blockEntities.ImaginaryAlloyForgeBlockEntity;
import org.lyy.lyycore.content.blockEntities.ImaginaryCraftingTableBlockEntity;
import org.lyy.lyycore.content.blockEntities.ImaginaryGateBlockEntity;
import org.lyy.lyycore.content.blockEntities.AdvancedImaginaryGateBlockEntity;
import org.lyy.lyycore.content.blockEntities.ItemCollectorBlockEntity;
import org.lyy.lyycore.content.blockEntities.ResourceGatheringFrameBlockEntity;
import org.lyy.lyycore.content.blockEntities.ResearchTableBlockEntity;
import org.lyy.lyycore.content.blockEntities.ProductionLabBlockEntity;

public class LyyBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, LyyCore.MODID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ImaginaryCondensingBeaconBlockEntity>> IMAGINARY_CONDENSING_BEACON =
            BLOCK_ENTITIES.register("imaginary_condensing_beacon", () -> BlockEntityType.Builder.of(ImaginaryCondensingBeaconBlockEntity::new, LyyBlocks.IMAGINARY_CONDENSING_BEACON.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpatialTransmissionTowerBlockEntity>> SPATIAL_TRANSMISSION_TOWER =
            BLOCK_ENTITIES.register("spatial_transmission_tower", () -> BlockEntityType.Builder.of(SpatialTransmissionTowerBlockEntity::new, LyyBlocks.SPATIAL_TRANSMISSION_TOWER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AdvancedImaginaryGateBlockEntity>> ADVANCED_IMAGINARY_GATE =
            BLOCK_ENTITIES.register("advanced_imaginary_gate", () -> BlockEntityType.Builder.of(AdvancedImaginaryGateBlockEntity::new,
                    LyyBlocks.ADVANCED_IMAGINARY_GATE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProductionLabBlockEntity>> PRODUCTION_LAB =
            BLOCK_ENTITIES.register("production_lab", () -> BlockEntityType.Builder.of(ProductionLabBlockEntity::new, LyyBlocks.PRODUCTION_LAB.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResearchTableBlockEntity>> RESEARCH_TABLE =
            BLOCK_ENTITIES.register("imaginary_research_table", () -> BlockEntityType.Builder.of(
                    ResearchTableBlockEntity::new, LyyBlocks.IMAGINARY_RESEARCH_TABLE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ImaginaryGateBlockEntity>> IMAGINARY_GATE =
            BLOCK_ENTITIES.register("imaginary_gate", () -> BlockEntityType.Builder.of(ImaginaryGateBlockEntity::new,
                    LyyBlocks.IMAGINARY_GATE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ImaginaryCraftingTableBlockEntity>> IMAGINARY_CRAFTING_TABLE =
            BLOCK_ENTITIES.register("imaginary_crafting_table", () -> BlockEntityType.Builder.of(ImaginaryCraftingTableBlockEntity::new,
                    LyyBlocks.IMAGINARY_CRAFTING_TABLE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResourceGatheringFrameBlockEntity>> RESOURCE_GATHERING_FRAME =
            BLOCK_ENTITIES.register("resource_gathering_frame", () -> BlockEntityType.Builder.of(
                    ResourceGatheringFrameBlockEntity::new,
                    java.util.stream.Stream.concat(LyyBlocks.RESOURCE_FRAMES.values().stream(), LyyBlocks.RESOURCE_FACTORIES.values().stream()).map(holder -> holder.get()).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrystalCondensingFrameBlockEntity>> CRYSTAL_CONDENSING_FRAME =
            BLOCK_ENTITIES.register("crystal_condensing_frame", () -> BlockEntityType.Builder.of(
                    CrystalCondensingFrameBlockEntity::new, LyyBlocks.CRYSTAL_CONDENSING_FRAME.get(), LyyBlocks.MINIATURE_CRYSTAL_FACTORY.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyCellBlockEntity>> IMAGINARY_ENERGY_CELL =
            BLOCK_ENTITIES.register("imaginary_energy_cell",
                    () -> BlockEntityType.Builder.of(EnergyCellBlockEntity::new,
                            LyyBlocks.IMAGINARY_ENERGY_CELL.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ItemCollectorBlockEntity>> ITEM_COLLECTOR =
            BLOCK_ENTITIES.register("item_collector",
                    () -> BlockEntityType.Builder.of(ItemCollectorBlockEntity::new,
                            LyyBlocks.ITEM_COLLECTOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ImaginaryAlloyForgeBlockEntity>> IAF =
            BLOCK_ENTITIES.register("imaginary_alloy_forge",
                    () -> BlockEntityType.Builder.of(ImaginaryAlloyForgeBlockEntity::new,
                            LyyBlocks.IAF.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BaseImaginaryGeneratorBlockEntity>> IGB =
            BLOCK_ENTITIES.register("im_generator",
                    () -> BlockEntityType.Builder.of(BaseImaginaryGeneratorBlockEntity::new,
                            LyyBlocks.IGB.get()).build(null));
}
