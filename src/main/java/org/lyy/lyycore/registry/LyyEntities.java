package org.lyy.lyycore.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.content.entity.GuardianCrystal;
import org.lyy.lyycore.content.entity.GrappleHook;
import org.lyy.lyycore.content.entity.GuardianSpikes;
import org.lyy.lyycore.content.entity.ImaginaryGuardian;
import org.lyy.lyycore.content.entity.EnderCompanion;
import org.lyy.lyycore.content.entity.LifeRevel;
import org.lyy.lyycore.content.entity.RevelDancer;
import org.lyy.lyycore.content.entity.RevelBlind;
import org.lyy.lyycore.content.entity.SonnetArrow;
import org.lyy.lyycore.content.entity.SonnetDome;
import org.lyy.lyycore.content.entity.SonnetVolley;

@EventBusSubscriber(modid = LyyCore.MODID)
public class LyyEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, LyyCore.MODID);
    @SubscribeEvent public static void attributes(EntityAttributeCreationEvent event) {
        event.put(LIFE_REVEL.get(), LifeRevel.attributes().build());
        event.put(REVEL_DANCER.get(), RevelDancer.attributes().build());
        event.put(REVEL_BLIND.get(), RevelBlind.attributes().build());
    }
    public static final DeferredHolder<EntityType<?>, EntityType<LifeRevel>> LIFE_REVEL =
            ENTITIES.register("life_revel", () -> EntityType.Builder.of(LifeRevel::new, MobCategory.MONSTER)
                    .sized(2, 3.5F).clientTrackingRange(12).updateInterval(1).build("lyycore:life_revel"));
    public static final DeferredHolder<EntityType<?>, EntityType<RevelDancer>> REVEL_DANCER =
            ENTITIES.register("revel_dancer", () -> EntityType.Builder.of(RevelDancer::new, MobCategory.MONSTER)
                    .sized(0.7F, 0.6F).clientTrackingRange(12).updateInterval(1).build("lyycore:revel_dancer"));
    public static final DeferredHolder<EntityType<?>, EntityType<RevelBlind>> REVEL_BLIND =
            ENTITIES.register("revel_blind", () -> EntityType.Builder.of(RevelBlind::new, MobCategory.MONSTER)
                    .sized(0.7F, 1.4F).clientTrackingRange(12).updateInterval(2).build("lyycore:revel_blind"));
    public static final DeferredHolder<EntityType<?>, EntityType<EnderCompanion>> ENDER_COMPANION =
            ENTITIES.register("ender_companion", () -> EntityType.Builder.<EnderCompanion>of(EnderCompanion::new, MobCategory.CREATURE)
                    .sized(0.7F, 0.8F).clientTrackingRange(10).updateInterval(2).fireImmune().build("lyycore:ender_companion"));

    public static final DeferredHolder<EntityType<?>, EntityType<GrappleHook>> GRAPPLE_HOOK =
            ENTITIES.register("grapple_hook", () -> EntityType.Builder.<GrappleHook>of(
                    GrappleHook::new, MobCategory.MISC).sized(0.25F, 0.25F)
                    .clientTrackingRange(16).updateInterval(1).noSave().build("lyycore:grapple_hook"));

    public static final DeferredHolder<EntityType<?>, EntityType<ImaginaryGuardian>> IMAGINARY_GUARDIAN =
            ENTITIES.register("imaginary_guardian", () -> EntityType.Builder.<ImaginaryGuardian>of(
                    ImaginaryGuardian::new, MobCategory.MONSTER).sized(2, 3.5F)
                    .clientTrackingRange(12).updateInterval(1).fireImmune().build("lyycore:imaginary_guardian"));
    public static final DeferredHolder<EntityType<?>, EntityType<GuardianCrystal>> GUARDIAN_CRYSTAL =
            ENTITIES.register("guardian_crystal", () -> EntityType.Builder.<GuardianCrystal>of(
                    GuardianCrystal::new, MobCategory.MISC).sized(0.5F, 0.5F)
                    .clientTrackingRange(12).updateInterval(1).fireImmune().noSave().build("lyycore:guardian_crystal"));
    public static final DeferredHolder<EntityType<?>, EntityType<GuardianSpikes>> GUARDIAN_SPIKES =
            ENTITIES.register("guardian_spikes", () -> EntityType.Builder.<GuardianSpikes>of(
                    GuardianSpikes::new, MobCategory.MISC).sized(1.2F, 1.5F)
                    .clientTrackingRange(12).updateInterval(1).fireImmune().noSave().build("lyycore:guardian_spikes"));
    public static final DeferredHolder<EntityType<?>, EntityType<SonnetDome>> SONNET_DOME =
            ENTITIES.register("sonnet_dome", () -> EntityType.Builder.<SonnetDome>of(SonnetDome::new, MobCategory.MISC)
                    .sized((float) (SonnetDome.RADIUS * 2), (float) SonnetDome.HEIGHT).clientTrackingRange(16).updateInterval(20).fireImmune().build("lyycore:sonnet_dome"));
    public static final DeferredHolder<EntityType<?>, EntityType<SonnetVolley>> SONNET_VOLLEY =
            ENTITIES.register("sonnet_volley", () -> EntityType.Builder.<SonnetVolley>of(SonnetVolley::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(16).updateInterval(1).noSave().fireImmune().build("lyycore:sonnet_volley"));

    public static final DeferredHolder<EntityType<?>, EntityType<SonnetArrow>> CRYSTAL_ARROW =
            ENTITIES.register("sonnet_crystal_arrow", () -> EntityType.Builder
                    .<SonnetArrow>of(SonnetArrow::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(16).updateInterval(1)
                    .build("lyycore:sonnet_crystal_arrow"));

    public static final DeferredHolder<EntityType<?>, EntityType<SonnetArrow.Spectral>> CRYSTAL_SPECTRAL_ARROW =
            ENTITIES.register("sonnet_crystal_spectral_arrow", () -> EntityType.Builder
                    .<SonnetArrow.Spectral>of(SonnetArrow.Spectral::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(16).updateInterval(1)
                    .build("lyycore:sonnet_crystal_spectral_arrow"));
}
