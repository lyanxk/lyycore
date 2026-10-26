package org.lyy.lyycore.content.equipment;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.util.RandomSource;
import org.lyy.lyycore.content.PlayerAttributes.Stat;

/** Equipment rules supplied by each accessory, independent of its item implementation. */
public interface Accessory {
    enum Kind { NECKLACE, NOTEBOOK }
    record Bonus(Stat stat, double amount) { }

    Kind kind();
    int rollTraitCount(RandomSource random);
    int attributePoints(int traitCount);
    List<Bonus> bonuses();
    ResourceLocation setId();
    default double setBonusMultiplier() { return 1; }
    Item reforgeMaterial();
}
