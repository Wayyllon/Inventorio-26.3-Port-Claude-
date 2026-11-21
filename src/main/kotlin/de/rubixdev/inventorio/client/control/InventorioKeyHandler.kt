package de.rubixdev.inventorio.client.control

import de.rubixdev.inventorio.client.configscreen.GlobalSettingsScreen
import de.rubixdev.inventorio.client.configscreen.PlayerSettingsScreen
import de.rubixdev.inventorio.config.PlayerSettings
import de.rubixdev.inventorio.mixin.client.accessor.MinecraftClientAccessor
import de.rubixdev.inventorio.player.PlayerInventoryAddon
import de.rubixdev.inventorio.player.PlayerInventoryAddon.Companion.inventoryAddon
import de.rubixdev.inventorio.util.ScrollWheelUtilityBeltMode
import de.rubixdev.inventorio.util.SegmentedHotbar
import de.rubixdev.inventorio.util.canRMBItem
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import net.minecraft.world.InteractionHand

@Environment(EnvType.CLIENT)
object InventorioKeyHandler {
    fun handleItemUsage(player: Player): Array<InteractionHand> {
        // Some items which can be used as a tool (e.g. trident) would REPLACE a selected hotbar stack upon use.
        // But something like VANILLA axes and shovels work fine.
        // Some modded stuff breaks it tho.
        var useMainHand = canRMBItem(player.getItemInHand(InteractionHand.MAIN_HAND), player.registryAccess())
        var useOffHand = canRMBItem(player.getItemInHand(InteractionHand.OFF_HAND), player.registryAccess())

        if (PlayerInventoryAddon.Client.triesToUseUtility) {
            useMainHand = false
        } else if (!PlayerSettings.useItemAppliesToOffhand.boolValue && !InventorioControls.keyUseUtility.isUnbound) {
            useOffHand = false
        }

        if (useMainHand) {
            player.inventoryAddon?.displayTool = ItemStack.EMPTY
        }

        return if (useMainHand && useOffHand) {
            arrayOf(InteractionHand.MAIN_HAND, InteractionHand.OFF_HAND)
        } else if (useMainHand) {
            arrayOf(InteractionHand.MAIN_HAND)
        } else if (useOffHand) {
            arrayOf(InteractionHand.OFF_HAND)
        } else {
            arrayOf()
        }
    }

    /**
     * Returns true if we shall cancel the vanilla hotbar slot selection
     */
    fun handleSegmentedHotbarSlotSelection(inventory: Inventory, slotToSelect: Int): Boolean {
        if (PlayerSettings.segmentedHotbar.value == SegmentedHotbar.OFF
            || PlayerSettings.segmentedHotbar.value == SegmentedHotbar.ONLY_VISUAL
        ) {
            return false
        }
        if (slotToSelect > 2) {
            return true
        }
        val addon = PlayerInventoryAddon.Client
        if (addon.selectedHotbarSection == -1) {
            addon.selectedHotbarSection = slotToSelect
        } else {
            inventory.selected = slotToSelect + 3 * addon.selectedHotbarSection
            addon.selectedHotbarSection = -1
        }
        return true
    }

    /**
     * Returns `true` if vanilla hotbar scrolling has to be cancelled.
     */
    fun scrollInHotbar(player: Player, scrollAmount: Double): Boolean {
        val scrollMode = PlayerSettings.scrollWheelUtilityBelt.value as ScrollWheelUtilityBeltMode
        if (scrollMode != ScrollWheelUtilityBeltMode.OFF) {
            val realScrollAmount = if (scrollMode == ScrollWheelUtilityBeltMode.REGULAR) -scrollAmount.toInt() else scrollAmount.toInt()
            player.inventoryAddon?.switchToNextUtility(realScrollAmount, PlayerSettings.skipEmptyUtilitySlots.boolValue)
            return true
        } else {
            PlayerInventoryAddon.Client.selectedHotbarSection = -1
        }
        return false
    }

    fun tick() {
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        val inventoryAddon = player.inventoryAddon ?: return

        if ((client as MinecraftClientAccessor).rightClickDelay <= 0) {
            if (InventorioControls.keyUseUtility.isPressed && !player.isUsingItem) {
                PlayerInventoryAddon.Client.activateSelectedUtility()
            }
            if (player.isFallFlying && InventorioControls.keyFireBoostRocket.isThisOrVanillaPressed) {
                inventoryAddon.fireRocketFromInventory()
            }
        }
        // This code handles finishing the usage of the utility belt item if you have a dedicated "use utility" key
        if (!InventorioControls.keyUseUtility.isPressed && PlayerInventoryAddon.Client.isUsingUtility) {
            client.gameMode?.releaseUsingItem(player)
        }

        // Scroll through the Utility Belt
        if (InventorioControls.keyNextUtility.consumeClick()) {
            inventoryAddon.switchToNextUtility(1, PlayerSettings.skipEmptyUtilitySlots.boolValue)
        }
        if (InventorioControls.keyPrevUtility.consumeClick()) {
            inventoryAddon.switchToNextUtility(-1, PlayerSettings.skipEmptyUtilitySlots.boolValue)
        }
        if (InventorioControls.keyEmptyUtility.consumeClick()) {
            inventoryAddon.switchToEmptyUtility(1)
        }

        if (InventorioControls.keyOpenPlayerSettingsMenu.consumeClick()) {
            client.setScreen(PlayerSettingsScreen.get(client.screen))
        }
        if (InventorioControls.keyOpenGlobalSettingsMenu.consumeClick()) {
            client.setScreen(GlobalSettingsScreen.get(client.screen))
        }
    }
}
