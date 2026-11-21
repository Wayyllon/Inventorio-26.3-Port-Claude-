package de.rubixdev.inventorio.player.inventory

import de.rubixdev.inventorio.mixin.client.accessor.MinecraftClientAccessor
import de.rubixdev.inventorio.packet.InventorioNetworking
import de.rubixdev.inventorio.util.getLevelOn
import kotlin.math.max
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.StainedGlassBlock
import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.FireworkRocketEntity
import net.minecraft.world.item.FireworkRocketItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.TieredItem

import de.rubixdev.inventorio.util.getEnchantment

abstract class PlayerInventoryExtraStuff protected constructor(player: Player) : PlayerInventoryHandFeatures(player) {
    /**
     * Returns the block breaking speed based on the return of [getMostPreferredTool]
     */
    fun getMiningSpeedMultiplier(block: BlockState): Float {
        val tool = getMostPreferredTool(block)
        if (tool != getActualMainHandItem()
            // TODO: find a better way around this that's not exclusive to Jade
            && !Thread.currentThread().stackTrace.any { it.className.startsWith("snownee.jade.addon") }
        ) {
            displayTool = tool
        }
        return max(1f, tool.getMiningSpeedMultiplier(block))
    }

    private fun getMostPreferredTool(block: BlockState): ItemStack {
        // TODO: some mods will disagree with justifying the most preferred tool by just speed
        // If a player tries to dig with a selected tool, respect that tool and don't change anything
        if (getActualMainHandItem().item is TieredItem || findFittingToolBeltIndex(getActualMainHandItem()) != -1) {
            return getActualMainHandItem()
        }
        // Try to find the fastest tool on the tool belt to mine this block
        val result = toolBelt.maxByOrNull { it.getMiningSpeedMultiplier(block) } ?: ItemStack.EMPTY
        if (result.getMiningSpeedMultiplier(block) > 1.0f) {
            return result
        }
        val isGlass = block.block is StainedGlassBlock || block.`is`(Blocks.GLASS)
        if (isGlass) {
            val silkTouch = player.getEnchantment(Enchantments.SILK_TOUCH)
            return toolBelt.firstOrNull { silkTouch.getLevelOn(it) > 0 } ?: ItemStack.EMPTY
        }
        return ItemStack.EMPTY
    }

    private fun playerAttackConditions(): Boolean {
        // if a player tries to attack with a tool, respect that tool and don't change anything
        if (getActualMainHandItem().item is TieredItem || findFittingToolBeltIndex(getActualMainHandItem()) != -1) {
            return false
        }
        // if the attack is done through a Pickarang (from Quark), do nothing
        if (Thread.currentThread().stackTrace.any { it.className.startsWith("org.violetmoon.quark.content.tools.entity.rang") }) {
            return false
        }
        return true
    }

    fun prePlayerAttack() {
        if (!playerAttackConditions()) return
        // Or else set a sword/trident as a weapon of choice, or an axe if a sword slot is empty
        player.swinging = true
        val swordStack = findFittingToolBeltStack(ItemStack(Items.DIAMOND_SWORD))
        displayTool = if (!swordStack.isEmpty) {
            swordStack
        } else {
            findFittingToolBeltStack(ItemStack(Items.DIAMOND_AXE))
        }
        // For some reason we need to manually add weapon's attack modifiers - the game doesn't do that for us
        displayTool.forEachModifier(EquipmentSlot.MAINHAND) { attribute, modifier ->
            val instance = player.attributes.getInstance(attribute)
            instance?.removeModifier(modifier)
            instance?.addTransientModifier(modifier)
        }
    }

    fun postPlayerAttack() {
        if (!playerAttackConditions()) return
        displayTool.forEachModifier(EquipmentSlot.MAINHAND) { attribute, modifier ->
            val instance = player.attributes.getInstance(attribute)
            instance?.removeModifier(modifier)
        }
    }

    fun fireRocketFromInventory() {
        if (!player.isFallFlying) {
            return
        }
        for (itemStack in player.inventory.main)
            if (tryFireRocket(itemStack)) {
                return
            }
        for (itemStack in stacks)
            if (tryFireRocket(itemStack)) {
                return
            }
    }

    private fun tryFireRocket(itemStack: ItemStack): Boolean {
        if (itemStack.item is FireworkRocketItem && itemStack.components.get(DataComponents.FIREWORKS)?.explosions?.isEmpty() != false) {
            val copyStack = itemStack.copy()
            if (!player.abilities.creativeMode) {
                itemStack.shrink(1)
            }
            // If this is the client side, request a server to actually fire a rocket.
            if (player.level().isClientSide) {
                displayTool = itemStack
                InventorioNetworking.INSTANCE.c2sUseBoostRocket()
                (Minecraft.getInstance() as MinecraftClientAccessor).rightClickDelay = 4
            } else {
                // If this is a server, spawn a firework entity
                player.level().spawnEntity(FireworkRocketEntity(player.level(), copyStack, player))
            }
            return true
        }
        return false
    }
}
