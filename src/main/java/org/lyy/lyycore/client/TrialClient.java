package org.lyy.lyycore.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.lyy.lyycore.registry.LyyEffects;

@EventBusSubscriber(modid = "lyycore", value = Dist.CLIENT)
public final class TrialClient {
    private static final ResourceLocation LOCK = ResourceLocation.parse("lyycore:textures/gui/health_binding_threads.png");
    private static int healthY;
    static AnimatedMeshModel binding;

    @SubscribeEvent public static void beforeHealth(RenderGuiLayerEvent.Pre event) {
        if (event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH))
            healthY = event.getGuiGraphics().guiHeight() - Minecraft.getInstance().gui.leftHeight;
    }
    @SubscribeEvent public static void afterHealth(RenderGuiLayerEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (!event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH) || mc.options.hideGui || mc.player == null
                || mc.gameMode == null || !mc.gameMode.canHurtPlayer()) return;
        var lock = mc.player.getEffect(LyyEffects.SPACETIME_LOCK);
        if (lock == null) return;
        int locked = Math.min(10, (lock.getAmplifier()+1)/2);
        int hearts = Mth.ceil(mc.player.getMaxHealth()/2), absorption = Mth.ceil(mc.player.getAbsorptionAmount()/2);
        var random = RandomSource.create((long)mc.gui.getGuiTicks()*312871);
        int regen = mc.player.hasEffect(MobEffects.REGENERATION) ? mc.gui.getGuiTicks()%Mth.ceil(mc.player.getMaxHealth()+5) : -1;
        RenderSystem.enableBlend();
        // Traverse in vanilla's order so low-health shaking stays aligned with the underlying icons.
        for (int index=hearts+absorption-1; index>=0; index--) {
            int y = healthY;
            if (Mth.ceil(mc.player.getHealth())+Mth.ceil(mc.player.getAbsorptionAmount())<=4) y+=random.nextInt(2);
            if (index==regen) y-=2;
            if (index>=locked || index>=hearts) continue;
            int x=event.getGuiGraphics().guiWidth()/2-91+index*8;
            event.getGuiGraphics().blit(LOCK, x-1,y-1,11,11,0,0,48,48,48,48);
        }
        RenderSystem.disableBlend();
    }
    @SubscribeEvent public static void binding(RenderPlayerEvent.Post event) {
        if (binding == null || !event.getEntity().hasEffect(LyyEffects.TRIAL_BINDING)) return;
        var pose = event.getPoseStack();
        pose.pushPose();
        pose.scale(.8F,1.4F,.9F);
        binding.render(pose, event.getMultiBufferSource(), LightTexture.FULL_BRIGHT, "idle",
                ((double) event.getEntity().tickCount+event.getPartialTick())/20);
        pose.popPose();
    }
}
