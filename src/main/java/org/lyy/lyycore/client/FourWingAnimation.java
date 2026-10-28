package org.lyy.lyycore.client;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import org.lyy.lyycore.content.AegisWings;
import org.lyy.lyycore.content.wings.FeatherAttack;

/** A single pose per player, with short snapshot blends when an action interrupts a loop. */
final class FourWingAnimation {
    private static final Map<Player, FourWingAnimation> PLAYERS = new WeakHashMap<>();
    private static AnimatedMeshModel model;
    // fold_hold is a 3.2-second loop with linear keys every 0.2 seconds, shared by all players.
    private static final float FOLD_FRAME_SECONDS = .2F;
    private static float[][] foldedFrames;
    private final float[] pose, from;
    private String clip = "idle", destination = "idle";
    private double started, blendStarted;
    private long lastHit = Long.MIN_VALUE;
    private boolean pendingHit, initialized;
    private FourWingAnimation() { pose = model().newPose(); from = model().newPose(); model().sample("idle", 0, pose); }
    static AnimatedMeshModel model() { if (model == null) model = new AnimatedMeshModel("white_aegis_four_wings"); return model; }
    static FourWingAnimation get(Player player) { return PLAYERS.computeIfAbsent(player, ignored -> new FourWingAnimation()); }
    static void reset() { PLAYERS.clear(); model = null; foldedFrames = null; }
    float[] pose() { return pose; }
    boolean folded() { return clip.equals("fold_hold"); }
    void update(AbstractClientPlayer player, float partial) {
        double now = ((double) player.tickCount + partial) / 20;
        long hit = AegisWings.shieldStarted(player);
        float hitAge = (player.level().getGameTime() - hit + partial) / 20;
        if (hit != lastHit) { pendingHit = hitAge >= 0 && hitAge < .86F; lastHit = hit; }
        var attack = FeatherAttackRenderer.current(player);
        float attackTime = FeatherAttackRenderer.age(player, partial);
        boolean attacking = attack != null && attackTime >= 0 && attackTime < FeatherAttack.duration(attack.featherCount());
        boolean guard = player.getPersistentData().getBoolean("lyycore:wings_guarding") || hitAge >= 0 && hitAge < .86F;
        String wanted = !AegisWings.visible(player) ? "fold" : guard && !attacking ? "shield"
                : player.getAbilities().flying || player.isFallFlying() && player.getPersistentData().getBoolean("lyycore:wings_boosting") ? "flight"
                : player.isFallFlying() ? "glide" : player.onGround() && !attacking ? "fold" : "idle";
        if (!initialized) {
            initialized = true;
            destination = wanted;
            if (wanted.equals("fold")) begin("fold_hold", now);
            else enter(now);
            model().sample(clip, 0, pose);
            System.arraycopy(pose, 0, from, 0, pose.length);
        } else if (!wanted.equals(destination)) {
            String previous = destination;
            destination = wanted;
            if (wanted.equals("fold")) begin("fold", now);
            else if (previous.equals("fold") && !attacking && !guard) begin("unfold", now);
            else if (wanted.equals("shield")) enter(now);
            else if (previous.equals("shield")) begin("shield_recall", now);
            else if (previous.equals("flight") || previous.equals("glide")) begin(previous + "_exit", now);
            else enter(now);
        }
        double elapsed = now - started;
        if (clip.equals("fold") && elapsed >= 1.2F) begin("fold_hold", now);
        else if (clip.equals("unfold") && elapsed >= 1.2F || clip.endsWith("_exit") && elapsed >= .5F
                || clip.equals("shield_recall") && elapsed >= .65F) enter(now);
        else if ((clip.equals("flight_enter") || clip.equals("glide_enter")) && elapsed >= .5F) begin(destination, now);
        else if (clip.equals("shield_deploy") && elapsed >= .58F) begin(pendingHit ? "shield_hit" : "shield_hold", now);
        else if (clip.equals("shield_hold") && pendingHit) begin("shield_hit", now);
        else if (clip.equals("shield_hit") && elapsed >= .28F) begin("shield_hold", now);
        if (folded()) sampleFolded(now - started, pose);
        else model().sample(clip, now - started, pose);
        float blend = (float) Mth.clamp((now - blendStarted) / .12, 0, 1);
        if (blend >= 1) return;
        // Cubic easing avoids abrupt velocity changes when leaving a flapping loop mid-cycle.
        blend = blend * blend * (3 - 2 * blend);
        for (int i = 0; i < pose.length; i++) pose[i] = Mth.lerp(blend, from[i], pose[i]);
    }
    private static void sampleFolded(double seconds, float[] result) {
        if (foldedFrames == null) {
            foldedFrames = new float[17][];
            for (int frame = 0; frame < foldedFrames.length; frame++) {
                foldedFrames[frame] = model().newPose();
                model().sample("fold_hold", frame * FOLD_FRAME_SECONDS, foldedFrames[frame]);
            }
        }
        float frame = (float) (seconds % (16 * FOLD_FRAME_SECONDS)) / FOLD_FRAME_SECONDS;
        int index = Math.min((int) frame, foldedFrames.length - 2);
        float[] a = foldedFrames[index], b = foldedFrames[index + 1];
        for (int i = 0; i < result.length; i++) result[i] = Mth.lerp(frame - index, a[i], b[i]);
    }
    private void enter(double now) {
        begin(switch (destination) { case "flight", "glide" -> destination + "_enter"; case "shield" -> "shield_deploy"; default -> destination; }, now);
    }
    private void begin(String next, double now) {
        System.arraycopy(pose, 0, from, 0, pose.length);
        clip = next; started = blendStarted = now;
        if (next.equals("shield_hit")) pendingHit = false;
    }
}
