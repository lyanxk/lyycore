package org.lyy.lyycore.content.wings;

/** Seconds shared by wing hit scheduling and feather animations at every tier. */
public final class FeatherAttack {
    public static final float PREPARE = 0.1375F, RELEASE = 0.125F;
    public static final float FLIGHT = 0.845F, HIT = 0.315F, RETURN = 0.1375F;
    private FeatherAttack() { }
    public static float release(int feather) { return PREPARE + 0.025F + feather * 0.03F; }
    public static float release(int feather, int count) { return release(count == 16 ? feather / 4 : feather); }
    public static int hitTick(int feather) { return (int) Math.ceil((release(feather) + HIT) * 20); }
    public static int hitTick(int feather, int count) { return (int)Math.ceil((release(feather, count) + HIT) * 20); }
    public static float duration(int count) { return release(count - 1, count) + FLIGHT + RETURN; }
    public static float returnedAt(int modelFeather, int count) {
        if (count == 16) return release(modelFeather, count) + FLIGHT;
        int last = modelFeather + (count - 1 - modelFeather) / 4 * 4;
        return release(last) + FLIGHT;
    }
}
