package org.lyy.lyycore.content.finality;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.*;

import org.lyy.lyycore.content.equipment.Accessory;
import org.lyy.lyycore.registry.LyyCapabilities;

@EventBusSubscriber(modid = "lyycore")
public final class FinalityContent {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("lyycore");
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("lyycore");
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, "lyycore");
    private static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, "lyycore");
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, "lyycore");
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "lyycore");
    private static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, "lyycore");

    static {
        CHUNK_GENERATORS.register("finality_arena", () -> FinalityArenaGenerator.CODEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("lyycore", path);
    }

    public static final DeferredItem<Item> ECHO =
            ITEMS.registerSimpleItem(
                    "echo_of_two_realms", new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<FinalityArmorItem> HELMET =
            armor("finality_helmet", ArmorItem.Type.HELMET);
    public static final DeferredItem<FinalityArmorItem> CHEST =
            armor("finality_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<FinalityArmorItem> LEGS =
            armor("finality_leggings", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<FinalityArmorItem> BOOTS =
            armor("finality_boots", ArmorItem.Type.BOOTS);
    public static final DeferredItem<FinalityAccessoryItem> NECKLACE =
            ITEMS.registerItem(
                    "initial_promise", p -> new FinalityAccessoryItem(p, Accessory.Kind.NECKLACE));
    public static final DeferredItem<FinalityAccessoryItem> NOTEBOOK =
            ITEMS.registerItem(
                    "unfinished_chapter",
                    p -> new FinalityAccessoryItem(p, Accessory.Kind.NOTEBOOK));
    public static final DeferredItem<DestinyWeaverItem> WEAVER =
            ITEMS.registerItem("destiny_weaver", DestinyWeaverItem::new);
    public static final DeferredItem<PortableEnergyItem> PORTABLE =
            ITEMS.registerItem("portable_spacetime_energy", PortableEnergyItem::new);
    public static final DeferredBlock<GenesisBlock> GENESIS_BLOCK =
            BLOCKS.register("genesis", GenesisBlock::new);
    public static final DeferredItem<BlockItem> GENESIS =
            ITEMS.registerSimpleBlockItem(GENESIS_BLOCK);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GenesisBlockEntity>>
            GENESIS_ENTITY =
                    BLOCK_ENTITIES.register(
                            "genesis",
                            () ->
                                    BlockEntityType.Builder.of(
                                                    GenesisBlockEntity::new, GENESIS_BLOCK.get())
                                            .build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<FinalitySettingsMenu>> SETTINGS =
            MENUS.register(
                    "finality_settings",
                    () -> IMenuTypeExtension.create(FinalitySettingsMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<GenesisMenu>> GENESIS_MENU =
            MENUS.register("genesis", () -> IMenuTypeExtension.create(GenesisMenu::new));
    public static final DeferredHolder<MobEffect, MobEffect> MARK =
            EFFECTS.register(
                    "finality_mark", () -> new MobEffect(MobEffectCategory.HARMFUL, 0xDAB3ED) {});
    public static final DeferredHolder<EntityType<?>, EntityType<FinalitySummon>> SUMMON =
            ENTITIES.register(
                    "finality_summon",
                    () ->
                            EntityType.Builder.of(FinalitySummon::new, MobCategory.MONSTER)
                                    .sized(1.2F, 1.5F)
                                    .clientTrackingRange(12)
                                    .updateInterval(20)
                                    .noSave()
                                    .build("lyycore:finality_summon"));
    public static final DeferredHolder<EntityType<?>, EntityType<FinalityProjectile>> PROJECTILE =
            ENTITIES.register(
                    "finality_projectile",
                    () ->
                            EntityType.Builder.of(FinalityProjectile::new, MobCategory.MISC)
                                    .sized(.4F, .4F)
                                    .clientTrackingRange(12)
                                    .updateInterval(65536)
                                    .noSave()
                                    .build("lyycore:finality_projectile"));
    public static final DeferredHolder<EntityType<?>, EntityType<FinalityWither>> WITHER =
            ENTITIES.register(
                    "finality_wither",
                    () ->
                            EntityType.Builder.of(FinalityWither::new, MobCategory.MONSTER)
                                    .sized(.9F, 3.5F)
                                    .fireImmune()
                                    .clientTrackingRange(12)
                                    .noSave()
                                    .build("lyycore:finality_wither"));

    private static DeferredItem<FinalityArmorItem> armor(String name, ArmorItem.Type type) {
        return ITEMS.registerItem(name, p -> new FinalityArmorItem(type, p));
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        BLOCKS.register(bus);
        ENTITIES.register(bus);
        EFFECTS.register(bus);
        MENUS.register(bus);
        BLOCK_ENTITIES.register(bus);
        CHUNK_GENERATORS.register(bus);
    }

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent e) {
        e.put(SUMMON.get(), FinalitySummon.attributes().build());
        e.put(
                WITHER.get(),
                net.minecraft.world.entity.boss.wither.WitherBoss.createAttributes()
                        .add(net.minecraft.world.entity.ai.attributes.Attributes.SCALE, 2)
                        .build());
    }

    @SubscribeEvent
    public static void creative(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey().location().getNamespace().equals("lyycore"))
            ITEMS.getEntries().forEach(v -> e.accept(v.get()));
    }

    @SubscribeEvent
    public static void capabilities(RegisterCapabilitiesEvent e) {
        e.registerBlockEntity(
                LyyCapabilities.IMAGINARY_ENERGY, GENESIS_ENTITY.get(), (b, s) -> b.energy);
        e.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK, GENESIS_ENTITY.get(), (b, s) -> b.fe);
    }
}
