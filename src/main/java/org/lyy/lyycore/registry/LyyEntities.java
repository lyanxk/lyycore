package org.lyy.lyycore.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.content.entity.SonnetArrow;
import org.lyy.lyycore.content.entity.SonnetDome;
import org.lyy.lyycore.content.entity.SonnetVolley;

public class LyyEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, LyyCore.MODID);

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
