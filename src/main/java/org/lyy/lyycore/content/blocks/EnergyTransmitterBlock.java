package org.lyy.lyycore.content.blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.lyy.lyycore.registry.LyyItems;

/** Shared device dispensing for the tower and matrix; binding lives in CoordinateDeviceItem. */
public abstract class EnergyTransmitterBlock extends SquareMachineBlock {
    protected EnergyTransmitterBlock(Properties properties, String model, int height, int width) { super(properties, model, height, width); }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide) {
            ItemStack device = new ItemStack(LyyItems.COORDINATE_DEVICE.get());
            if (!player.getInventory().add(device)) player.drop(device, false);
            player.inventoryMenu.broadcastChanges();
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
