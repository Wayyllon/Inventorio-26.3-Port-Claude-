package de.rubixdev.inventorio.slot

import net.minecraft.world.entity.player.Player
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.inventory.Slot

open class BlockedSlot(inventory: Container, index: Int, x: Int, y: Int) : Slot(inventory, index, x, y) {
    override fun mayPickup(playerEntity: Player): Boolean {
        return false
    }

    override fun mayPlace(stack: ItemStack): Boolean {
        return false
    }

    override fun isActive(): Boolean {
        return false
    }
}
