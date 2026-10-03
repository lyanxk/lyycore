package org.lyy.lyycore.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.content.blockEntities.BaseImaginaryGeneratorBlockEntity;
import org.lyy.lyycore.content.blockEntities.EnergyCellBlockEntity;
import org.lyy.lyycore.content.blockEntities.ImaginaryAlloyForgeBlockEntity;
import org.lyy.lyycore.content.blockEntities.ItemCollectorBlockEntity;
import org.lyy.lyycore.content.blockEntities.CrystalCondensingFrameBlockEntity;
import org.lyy.lyycore.content.blockEntities.ResourceGatheringFrameBlockEntity;

public class LyyBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, LyyCore.MODID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResourceGatheringFrameBlockEntity>> RESOURCE_GATHERING_FRAME =
            BLOCK_ENTITIES.register("resource_gathering_frame", () -> BlockEntityType.Builder.of(
                    ResourceGatheringFrameBlockEntity::new,
                    LyyBlocks.RESOURCE_FRAMES.values().stream().map(holder -> holder.get()).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrystalCondensingFrameBlockEntity>> CRYSTAL_CONDENSING_FRAME =
            BLOCK_ENTITIES.register("crystal_condensing_frame", () -> BlockEntityType.Builder.of(
                    CrystalCondensingFrameBlockEntity::new, LyyBlocks.CRYSTAL_CONDENSING_FRAME.get()).build(null));

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
