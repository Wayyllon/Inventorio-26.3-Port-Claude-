package de.rubixdev.inventorio.player.inventory

import de.rubixdev.inventorio.util.INVENTORY_HOTBAR_RANGE
import de.rubixdev.inventorio.util.getLevelOn
import de.rubixdev.inventorio.util.isNotEmpty
import kotlin.math.min
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ProjectileWeaponItem

import de.rubixdev.inventorio.util.getEnchantment

abstract class PlayerInventoryInjects protected constructor(player: Player) : PlayerInventoryExtension(player) {
    fun mendToolBeltItems(xpAmount: Int): Int {
        var xpLeft = xpAmount
        for (itemStack in toolBelt) {
            val mending = player.getEnchantment(Enchantments.MENDING)
            if (itemStack.isNotEmpty && itemStack.isDamaged && mending.getLevelOn(itemStack) > 0) {
                val damageRestored = min(xpAmount * 2, itemStack.damageValue)
                itemStack.damageValue -= damageRestored
                xpLeft = xpAmount - damageRestored / 2
                return if (xpLeft > 0) mendToolBeltItems(xpLeft) else 0
            }
        }
        return xpLeft
    }

    /**
     * Returns null if we want to proceed with vanilla behaviour.
     */
    fun getActiveArrowType(bowStack: ItemStack): ItemStack? {
        if (bowStack.item !is ProjectileWeaponItem) {
            return null
        }
        val predicate = (bowStack.item as ProjectileWeaponItem).supportedHeldProjectiles
        return stacks.firstOrNull { predicate.test(it) }
    }

    /**
     * Returns false if we want to proceed with vanilla behavior
     * Returns true if we did our own logic instead
     */
    fun insertOnlySimilarStack(sourceStack: ItemStack): Boolean {
        // Skip unstackable items
        if (!sourceStack.isStackable) {
            return false
        }
        // Skip items which can go into hotbar (and allow vanilla to handle it)
        for (i in INVENTORY_HOTBAR_RANGE) {
            val hotbarStack = player.inventory.main[i]
            if (areItemsSimilar(sourceStack, hotbarStack) && hotbarStack.count < hotbarStack.maxStackSize) {
                return false
            }
        }
        for (utilityStack in utilityBelt) {
            if (areItemsSimilar(sourceStack, utilityStack)) {
                transfer(sourceStack, utilityStack)
                if (sourceStack.isEmpty) {
                    return true
                }
            }
        }
        for (index in getAvailableDeepPocketsRange()) {
            val targetStack = deepPockets[index]
            if (areItemsSimilar(sourceStack, targetStack)) {
                transfer(sourceStack, targetStack)
                if (sourceStack.isEmpty) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Returns false if we want to proceed with vanilla behavior
     * Returns true if we did our own logic instead
     */
    fun insertStackIntoEmptySlot(sourceStack: ItemStack): Boolean {
        for (index in getAvailableDeepPocketsRange()) {
            if (deepPockets[index].isEmpty) {
                deepPockets[index] = sourceStack.copy()
                sourceStack.count = 0
                setChanged()
                return true
            }
        }
        return false
    }

    private fun transfer(sourceStack: ItemStack, targetStack: ItemStack) {
        val j = min(sourceStack.count, targetStack.maxStackSize - targetStack.count)
        if (j > 0) {
            targetStack.grow(j)
            sourceStack.shrink(j)
            setChanged()
        }
    }

    /**
     * Returns false if we want to proceed with vanilla behavior
     * Returns true if we did our own logic instead
     */
    fun removeOne(sourceStack: ItemStack): Boolean {
        for ((index, item) in stacks.withIndex()) {
            if (item === sourceStack) {
                stacks[index] = ItemStack.EMPTY
                return true
            }
        }
        return false
    }
}
