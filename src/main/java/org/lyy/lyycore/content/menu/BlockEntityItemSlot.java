package org.lyy.lyycore.content.menu;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** Also persists menu operations that mutate an existing stack without invoking the handler. */
public class BlockEntityItemSlot extends SlotItemHandler {
    private final BlockEntity owner;

    public BlockEntityItemSlot(BlockEntity owner, IItemHandler handler, int index, int x, int y) {
        super(handler, index, x, y);
        this.owner = owner;
    }

    @Override public void setChanged() {
        super.setChanged();
        owner.setChanged();
    }
}
