package org.lyy.lyycore.client;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import org.joml.Matrix4f;
import org.lyy.lyycore.LyyCore;

@EventBusSubscriber(modid = LyyCore.MODID, value = Dist.CLIENT)
public final class FinalityArenaEffects extends DimensionSpecialEffects {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(LyyCore.MODID, "finality_arena");
    public static final float FLOOR_HEIGHT = 0.0F;

    private FinalityArenaEffects() {
        super(Float.NaN, false, SkyType.NONE, true, true);
    }

    @SubscribeEvent
    public static void register(RegisterDimensionSpecialEffectsEvent event) {
        event.register(ID, new FinalityArenaEffects());
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float daylight) {
        return new Vec3(0.65, 0.55, 0.75);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f view,
                             Camera camera, Matrix4f projection, boolean foggy, Runnable setupFog) {
        return true;
    }
}
