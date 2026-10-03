package org.lyy.lyycore.content;

/** Production rules shared by the machine, its menu and JEI. */
public enum FrameProduction {
    STANDARD(1, 1),
    MINIATURE_FACTORY(4, 0);

    private final int batchSize;
    private final int passiveEnergy;

    FrameProduction(int batchSize, int passiveEnergy) {
        this.batchSize = batchSize;
        this.passiveEnergy = passiveEnergy;
    }

    public int batchSize() { return batchSize; }
    public int passiveEnergyPerSecond() { return passiveEnergy; }
    public int energyCost(int recipeCost) { return this == MINIATURE_FACTORY ? 100 : recipeCost; }
}
