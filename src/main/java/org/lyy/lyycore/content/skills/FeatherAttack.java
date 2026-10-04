package org.lyy.lyycore.content.skills;

/** Seconds shared by server hit scheduling and the supplied four-feather animations. */
public final class FeatherAttack {
    public static final float PREPARE = 0.1375F, RELEASE = 0.125F;
    public static final float FLIGHT = 0.845F, HIT = 0.315F, RETURN = 0.1375F;
    public static final float RETURN_START = release(3) + FLIGHT;
    public static final float DURATION = RETURN_START + RETURN;
    private FeatherAttack() { }
    public static float release(int feather) { return PREPARE + 0.025F + feather * 0.03F; }
    public static int hitTick(int feather) { return (int) Math.ceil((release(feather) + HIT) * 20); }
}
