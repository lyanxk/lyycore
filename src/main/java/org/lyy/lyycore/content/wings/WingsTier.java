package org.lyy.lyycore.content.wings;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.phys.Vec3;

/** Register tier definitions during mod initialization; players save only the stable level number. */
public record WingsTier(int level, double damageReduction, int featherCount, float featherDamage, FlightBoost flightBoost) {
    private static final Map<Integer, WingsTier> TIERS = new HashMap<>();
    public static final WingsTier FIRST = register(new WingsTier(1, .5, 4, 10, FlightBoost.NONE));
    public static final WingsTier SECOND = register(new WingsTier(2, .75, 16, 30, new FlightBoost(.014, 100.0 / 72)));

    public WingsTier {
        if (level < 1 || !Double.isFinite(damageReduction) || damageReduction < 0 || damageReduction > 1
                || featherCount < 1 || featherCount > 64 || !Float.isFinite(featherDamage) || featherDamage <= 0 || flightBoost == null)
            throw new IllegalArgumentException("Invalid wings tier: " + level);
    }

    public static WingsTier register(WingsTier tier) {
        if (TIERS.putIfAbsent(tier.level, tier) != null) throw new IllegalArgumentException("Duplicate wings level: " + tier.level);
        return tier;
    }

    /** Missing higher tiers fall back to level one without discarding the saved upgrade. */
    public static WingsTier forLevel(int level) { return TIERS.getOrDefault(level, FIRST); }

    /** Per-tick acceleration and maximum boosted speed, in blocks/tick; zero disables boost. */
    public record FlightBoost(double acceleration, double maxSpeed) {
        public static final FlightBoost NONE = new FlightBoost(0, 0);
        public FlightBoost {
            if (!Double.isFinite(acceleration) || !Double.isFinite(maxSpeed) || acceleration < 0 || maxSpeed < 0
                    || (acceleration == 0) != (maxSpeed == 0)) throw new IllegalArgumentException("Invalid wings flight boost");
        }
        public Vec3 accelerate(Vec3 velocity, Vec3 direction) {
            // Never brake a dive or firework flight that is already above the boost cap.
            if (acceleration == 0 || velocity.lengthSqr() >= maxSpeed * maxSpeed) return velocity;
            var boosted = velocity.add(direction.normalize().scale(acceleration));
            return boosted.lengthSqr() > maxSpeed * maxSpeed ? boosted.normalize().scale(maxSpeed) : boosted;
        }
    }
}
