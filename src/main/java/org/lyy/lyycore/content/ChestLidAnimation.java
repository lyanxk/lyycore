package org.lyy.lyycore.content;

import net.minecraft.util.Mth;

/** Local tick progress avoids loss of interpolation precision in long-running worlds. */
public final class ChestLidAnimation {
    private final int openTicks, closeTicks, idleTicks;
    private boolean open;
    private int tick, previousTick, idleTick;

    public ChestLidAnimation(int openTicks, int closeTicks, int idleTicks, boolean open) {
        this.openTicks = openTicks;
        this.closeTicks = closeTicks;
        this.idleTicks = idleTicks;
        this.open = open;
        tick = previousTick = open ? openTicks : closeTicks;
    }

    public void tick(boolean open) {
        if (this.open != open) {
            this.open = open;
            tick = idleTick = 0;
        }
        previousTick = tick;
        if (tick < (open ? openTicks : closeTicks)) tick++;
        else idleTick = idleTick % idleTicks + 1;
    }

    public boolean open() { return open; }
    public float progress(float partial) {
        return Mth.lerp(partial, previousTick, tick) / (open ? openTicks : closeTicks);
    }
    public float idleTime(float partial) { return Math.max(0, idleTick - 1 + partial) / 20F; }
}
