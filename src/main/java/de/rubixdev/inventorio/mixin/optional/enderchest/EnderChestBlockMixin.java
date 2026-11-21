package de.rubixdev.inventorio.mixin.optional.enderchest;

import de.rubixdev.inventorio.util.EnderChestTester;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ChestMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * This mixin enlarges the Ender Chest's Inventory Screen to 6 rows. To enlarge
 * the actual storage, {@link PlayerEntityMixin#inventorioResizeEnderChest} is
 * used
 */
@Restriction(require = @Condition(type = Condition.Type.TESTER, tester = EnderChestTester.class))
@Mixin(value = EnderChestBlock.class)
public class EnderChestBlockMixin {
    @Redirect(
        method = "lambda$useWithoutItem$0",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/ChestMenu;threeRows(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;)Lnet/minecraft/world/inventory/ChestMenu;"
        )
    )
    private static ChestMenu inventorioOnEnderChestOpen(
        int syncId,
        Inventory playerInventory,
        Container inventory
    ) {
        return ChestMenu.sixRows(syncId, playerInventory, inventory);
    }
}
