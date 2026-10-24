package org.lyy.lyycore.content.wings;

import net.minecraft.world.phys.Vec3;

public record WingsTier(int level, double damageReduction, int featherCount, float featherDamage,
                        double acceleration, double maxSpeed) {
    public static final WingsTier FIRST = new WingsTier(1, .5, 4, 10, 0, 0);
    public static final WingsTier SECOND = new WingsTier(2, .75, 16, 30, .014, 100.0 / 72);

    public Vec3 accelerate(Vec3 velocity, Vec3 direction, int percent, int upgrade) {
        double thrust = acceleration * percent / 100.0;
        // Base boost never brakes a dive or firework flight already above its cap.
        if (thrust == 0 || upgrade == 0 && velocity.lengthSqr() >= maxSpeed * maxSpeed) return velocity;
        var boosted = velocity.add(direction.normalize().scale(thrust * (1 << upgrade)));
        return upgrade == 0 && boosted.lengthSqr() > maxSpeed * maxSpeed
                ? boosted.normalize().scale(maxSpeed) : boosted;
    }
}
