package de.rubixdev.inventorio.slot

import com.mojang.datafixers.util.Pair
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
    override fun setByPlayer(stack: ItemStack?, previousStack: ItemStack?) {
        entity.onEquipItem(equipmentSlot, previousStack, stack)
        super.setByPlayer(stack, previousStack)
        // TODO: update deep pockets?
    }

    override fun getMaxStackSize(): Int = 1

    override fun mayPlace(stack: ItemStack): Boolean =
        equipmentSlot == entity.getEquipmentSlotForItem(stack)

    override fun mayPickup(playerEntity: Player): Boolean {
        val itemStack = this.item
        return !(
            !itemStack.isEmpty
                && !playerEntity.isCreative
                && EnchantmentHelper.has(itemStack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)
            ) && super.mayPickup(playerEntity)
    }

    override fun getNoItemIcon(): Pair<Identifier, Identifier>? {
        return Pair.of(InventoryMenu.BLOCK_ATLAS, EMPTY_ARMOR_SLOT_TEXTURES[equipmentSlot])
    }

    companion object {
        private val EMPTY_ARMOR_SLOT_TEXTURES = mapOf(
            EquipmentSlot.FEET to InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS,
            EquipmentSlot.LEGS to InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS,
            EquipmentSlot.CHEST to InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
            EquipmentSlot.HEAD to InventoryMenu.EMPTY_ARMOR_SLOT_HELMET,
        )
    }
}
