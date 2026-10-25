package org.lyy.lyycore.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.lyy.lyycore.LyyCore;

public final class LyySounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, LyyCore.MODID);
    public static final DeferredHolder<SoundEvent, SoundEvent> SPACETIME_MATRIX_HUM = SOUNDS.register("spacetime_matrix_hum",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(LyyCore.MODID, "spacetime_matrix_hum")));
    public static final DeferredHolder<SoundEvent, SoundEvent> LIGHTNING_ZAP = SOUNDS.register("lightning_zap",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(LyyCore.MODID, "lightning_zap")));
    public static final DeferredHolder<SoundEvent, SoundEvent> LIUXI_THROW = SOUNDS.register("liuxi_throw",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(LyyCore.MODID, "liuxi_throw")));
    private LyySounds() { }
}
