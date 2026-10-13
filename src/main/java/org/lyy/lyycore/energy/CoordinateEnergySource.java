package org.lyy.lyycore.energy;

import net.minecraft.core.GlobalPos;

/** Implemented by energy sources whose destinations can be bound with a coordinate device. */
public interface CoordinateEnergySource {
    enum BindingResult { ADDED, ALREADY_BOUND, LIMIT_REACHED }

    BindingResult bindTarget(GlobalPos target);
    int getTargetCount();
    int maxTargets();
}
