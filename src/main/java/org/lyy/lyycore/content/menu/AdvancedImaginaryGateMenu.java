package org.lyy.lyycore.content.menu;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import org.lyy.lyycore.content.blockEntities.AdvancedImaginaryGateBlockEntity;
import org.lyy.lyycore.registry.LyyMenus;
import java.util.Comparator;
import java.util.List;

public final class AdvancedImaginaryGateMenu extends ImaginaryGateMenu {
    public static final int START = -1, PAUSE = -2;
    private final AdvancedImaginaryGateBlockEntity gate;
    private final List<ResourceLocation> biomes;
    private final ContainerData terrain;

    public AdvancedImaginaryGateMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, (AdvancedImaginaryGateBlockEntity) inventory.player.level().getBlockEntity(buffer.readBlockPos()), new SimpleContainerData(1));
    }
    public AdvancedImaginaryGateMenu(int id, Inventory inventory, AdvancedImaginaryGateBlockEntity gate, ContainerData status) {
        super(LyyMenus.ADVANCED_IMAGINARY_GATE.get(), id, inventory, gate, status);
        this.gate = gate;
        biomes = inventory.player.registryAccess().registryOrThrow(Registries.BIOME).keySet().stream()
                .sorted(Comparator.comparing(ResourceLocation::toString)).toList();
        terrain = inventory.player.level().isClientSide ? new SimpleContainerData(4) : new ContainerData() {
            @Override public int get(int index) {
                return switch (index) {
                    case 0 -> biomes.indexOf(gate.selectedBiome());
                    case 1 -> gate.completedColumns();
                    case 2 -> gate.running() ? 1 : 0;
                    default -> gate.waitingForChunk() ? 1 : 0;
                };
            }
            @Override public void set(int index, int value) { }
            @Override public int getCount() { return 4; }
        };
        addDataSlots(terrain);
    }
    public List<ResourceLocation> biomes() { return biomes; }
    public int selected() { return terrain.get(0); }
    public int completedColumns() { return terrain.get(1); }
    public boolean running() { return terrain.get(2) != 0; }
    public boolean waitingForChunk() { return terrain.get(3) != 0; }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (player.level().isClientSide || !stillValid(player) || player.containerMenu != this) return false;
        if (button == START) gate.setRunning(true);
        else if (button == PAUSE) gate.setRunning(false);
        else if (button >= 0 && button < biomes.size() && !gate.running()) gate.selectBiome(biomes.get(button));
        else return false;
        broadcastChanges();
        return true;
    }
}
