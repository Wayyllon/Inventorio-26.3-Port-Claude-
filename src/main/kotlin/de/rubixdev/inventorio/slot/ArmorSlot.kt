package de.rubixdev.inventorio.slot

import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.resources.Identifier

import net.minecraft.world.item.enchantment.EnchantmentEffectComponents

class ArmorSlot(
    inventory: Container,
    private val entity: LivingEntity,
    private val equipmentSlot: EquipmentSlot,
    index: Int,
    x: Int,
    y: Int,
) : Slot(inventory, index, x, y) {
    override fun setByPlayer(stack: ItemStack, previousStack: ItemStack) {
        entity.onEquipItem(equipmentSlot, previousStack, stack)
        super.setByPlayer(stack, previousStack)
        // TODO: update deep pockets?
    }

    override fun getMaxStackSize(): Int = 1

    // 26.3: equipability is decided by the `equippable` component, like the vanilla ArmorSlot
    override fun mayPlace(stack: ItemStack): Boolean =
        entity.isEquippableInSlot(stack, equipmentSlot)

    override fun mayPickup(playerEntity: Player): Boolean {
        val itemStack = this.item
        return !(
            !itemStack.isEmpty
                && !playerEntity.isCreative
                && EnchantmentHelper.has(itemStack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)
            ) && super.mayPickup(playerEntity)
    }

    // 26.3: slot icons are GUI sprites, no atlas needed
    override fun getNoItemIcon(): Identifier? = EMPTY_ARMOR_SLOT_TEXTURES[equipmentSlot]

    companion object {
        private val EMPTY_ARMOR_SLOT_TEXTURES = mapOf(
            EquipmentSlot.FEET to InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS,
            EquipmentSlot.LEGS to InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS,
            EquipmentSlot.CHEST to InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
            EquipmentSlot.HEAD to InventoryMenu.EMPTY_ARMOR_SLOT_HELMET,
        )
    }
}
