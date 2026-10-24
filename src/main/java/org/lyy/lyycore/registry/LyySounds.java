package org.lyy.lyycore.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.lyy.lyycore.LyyCore;

public final class LyySounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, LyyCore.MODID);
    public static final DeferredHolder<SoundEvent, SoundEvent> LIGHTNING_ZAP = SOUNDS.register("lightning_zap",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(LyyCore.MODID, "lightning_zap")));
    private LyySounds() { }
}
