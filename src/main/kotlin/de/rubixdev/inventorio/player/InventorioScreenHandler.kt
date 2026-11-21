package de.rubixdev.inventorio.player

import de.rubixdev.inventorio.ScreenTypeProvider
import de.rubixdev.inventorio.client.ui.InventorioScreen
import de.rubixdev.inventorio.config.GlobalSettings
import de.rubixdev.inventorio.mixin.accessor.CraftingScreenHandlerAccessor
import de.rubixdev.inventorio.mixin.accessor.SlotAccessor
import de.rubixdev.inventorio.packet.InventorioNetworking
import de.rubixdev.inventorio.player.PlayerInventoryAddon.Companion.inventoryAddon
import de.rubixdev.inventorio.player.PlayerInventoryAddon.Companion.toolBeltTemplates
import de.rubixdev.inventorio.slot.ArmorSlot
import de.rubixdev.inventorio.slot.BlockedSlot
import de.rubixdev.inventorio.slot.DeepPocketsSlot
import de.rubixdev.inventorio.slot.ToolBeltSlot
import de.rubixdev.inventorio.util.*
import java.util.function.Consumer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Prediction
import net.minecraft.world.Container
import net.minecraft.world.inventory.AbstractCraftingMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.inventory.RecipeBookType
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.inventory.ResultSlot
import net.minecraft.world.inventory.Slot
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

class InventorioScreenHandler(syncId: Int, val inventory: Inventory) :
    AbstractCraftingMenu(ScreenTypeProvider.INSTANCE.getScreenHandlerType(), syncId, 2, 2) {
    val inventoryAddon = inventory.player.inventoryAddon!!

    // 26.3: the crafting grid and result containers are owned by AbstractCraftingMenu
    private val craftingInput get() = craftSlots
    private val craftingResult get() = resultSlots

    // ===================================================
    // Modified methods
    // ===================================================
    init {
        // Crafting Grid
        if (GlobalSettings.allow2x2CraftingGrid.boolValue) {
            addSlot(ResultSlot(inventory.player, craftSlots, resultSlots, 0, 174, 28))
            for (i in 0..3)
                addSlot(Slot(craftSlots, i, 118 + i % 2 * 18, 18 + i / 2 * 18))
        } else {
            addSlot(BlockedSlot(inventory, 0, 174, 28))
            for (i in 0..3)
                addSlot(BlockedSlot(craftSlots, i, 118 + i % 2 * 18, 18 + i / 2 * 18))
        }

        // Armor
        for ((_, relativeIndex) in armorSlotsRange.withRelativeIndex())
            addSlot(ArmorSlot(inventory, inventory.player, armorSlots[relativeIndex], 39 - relativeIndex, 8, 8 + relativeIndex * 18))

        // Main Inventory
        for ((_, relativeIndex) in mainInventoryWithoutHotbarRange.withRelativeIndex())
            addSlot(Slot(inventory, relativeIndex + 9, 8 + (relativeIndex % 9) * 18, 84 + (relativeIndex / 9) * 18))

        // Hotbar
        for ((_, relativeIndex) in hotbarRange.withRelativeIndex())
            addSlot(Slot(inventory, relativeIndex, 8 + relativeIndex * 18, 142))

        // Extended Inventory Section (Deep Pockets Enchantment)
        for ((absoluteIndex, relativeIndex) in INVENTORY_ADDON_DEEP_POCKETS_RANGE.withRelativeIndex())
            addSlot(
                DeepPocketsSlot(
                    inventoryAddon,
                    absoluteIndex,
                    SLOT_INVENTORY_DEEP_POCKETS.x + (relativeIndex % VANILLA_ROW_LENGTH) * SLOT_UI_SIZE,
                    SLOT_INVENTORY_DEEP_POCKETS.y + (relativeIndex / VANILLA_ROW_LENGTH) * SLOT_UI_SIZE,
                ),
            )

        // Utility Belt
        for ((absoluteIndex, relativeIndex) in INVENTORY_ADDON_UTILITY_BELT_RANGE.withRelativeIndex())
            addSlot(
                DeepPocketsSlot(
                    inventoryAddon,
                    absoluteIndex,
                    SLOT_UTILITY_BELT_COLUMN_1.x + SLOT_UI_SIZE * (relativeIndex / UTILITY_BELT_SMALL_SIZE),
                    SLOT_UTILITY_BELT_COLUMN_1.y + SLOT_UI_SIZE * (relativeIndex % UTILITY_BELT_SMALL_SIZE),
                ),
            )

        // Tool Belt
        val deepPocketsRowCount = inventoryAddon.getDeepPocketsRowCount()
        for ((relativeIndex, toolBeltTemplate) in toolBeltTemplates.withIndex()) {
            addSlot(
                ToolBeltSlot(
                    toolBeltTemplate,
                    inventoryAddon,
                    relativeIndex + INVENTORY_ADDON_TOOL_BELT_INDEX_OFFSET,
                    ToolBeltSlot.getSlotPosition(deepPocketsRowCount, relativeIndex, getToolBeltSlotCount()).x,
                    ToolBeltSlot.getSlotPosition(deepPocketsRowCount, relativeIndex, getToolBeltSlotCount()).y,
                ),
            )
        }

        updateDeepPocketsCapacity()

        openConsumers.forEach {
            try {
                it.value.accept(this)
            } catch (e: Throwable) {
                logger.error("Inventory Screen Handler Open Consumer '${it.key}' has failed: ", e)
            }
        }
    }

    override fun quickMoveStack(player: Player, sourceIndex: Int): ItemStack {
        val sourceSlot = slots[sourceIndex]
        val stackDynamic = sourceSlot.item
        // fix for #191, issue where "Origins: Classes" copies the crafting result stack each time it is accessed,
        // so we have to pass the same `stackDynamic` to the inner function instead of re-getting it
        val stackStatic = quickMoveInner(player, sourceIndex, stackDynamic)
        if (stackStatic.isNotEmpty) {
            if (stackDynamic.isEmpty) {
                sourceSlot.setByPlayer(ItemStack.EMPTY)
            } else {
                sourceSlot.setChanged()
            }

            if (stackDynamic.count == stackStatic.count) {
                return ItemStack.EMPTY
            }

            sourceSlot.onTake(player, stackDynamic)
        }
        return stackStatic
    }

    private fun quickMoveInner(player: Player, sourceIndex: Int, stackDynamic: ItemStack): ItemStack {
        val stackStatic = stackDynamic.copy()
        val availableDeepPocketsRange = getAvailableDeepPocketsRange()

        // First, we want to transfer armor or tools into their respective slots from any other section
        if (sourceIndex in mainInventoryRange || sourceIndex in availableDeepPocketsRange) {
            // Try to send an item into the armor slots
            if (
                player.getEquipmentSlotForItem(stackStatic).type == EquipmentSlot.Type.HUMANOID_ARMOR
                && insertItem(stackDynamic, armorSlotsRange)
            ) {
                updateDeepPocketsCapacity()
                return stackStatic
            }
            // Try to send an item into the Tool Belt
            if (insertItem(stackDynamic, toolBeltRange)) {
                return stackStatic
            }
        }
        // If we're here, an item can't be moved to neither tool belt nor armor slots

        when (sourceIndex) {
            // when we shift-click an item that's in the hotbar, we try to move it to main section and then into deep pockets
            in hotbarRange -> if (
                insertItem(stackDynamic, mainInventoryWithoutHotbarRange)
                || (!availableDeepPocketsRange.isEmpty() && insertItem(stackDynamic, availableDeepPocketsRange))
            ) {
                return stackStatic
            }
            // when we shift-click an item that's in the main inventory, we try to move it into deep pockets and then
            // into hotbar (that's what vanilla does)
            // TODO: some players would want this reversed
            in mainInventoryWithoutHotbarRange -> if (
                (!availableDeepPocketsRange.isEmpty() && insertItem(stackDynamic, availableDeepPocketsRange))
                || insertItem(stackDynamic, hotbarRange)
            ) {
                return stackStatic
            }
            // when we shift-click an item that's in the deep pockets, we try to move it into the main inventory
            in availableDeepPocketsRange -> if (insertItem(stackDynamic, mainInventoryRange)) {
                return stackStatic
            }
            // when we shift-click an item from anywhere else (armor slots, tool belt, utility belt, crafting grid/result),
            // try to move it into the main inventory or deep pockets
            else -> if (
                insertItem(stackDynamic, mainInventoryRange)
                || (!availableDeepPocketsRange.isEmpty() && insertItem(stackDynamic, availableDeepPocketsRange))
            ) {
                if (sourceIndex in craftingGridRange) {
                    slotsChanged(craftingInput)
                    slotsChanged(craftingResult)
                }
                return stackStatic
            }
        }

        return ItemStack.EMPTY
    }

    override fun clicked(slotIndex: Int, clickData: Int, actionType: ContainerInput, playerEntity: Player) {
        super.clicked(slotIndex, clickData, actionType, playerEntity)
        if (slotIndex in armorSlotsRange) {
            updateDeepPocketsCapacity()
        } else if (slotIndex in utilityBeltRange && inventoryAddon.getSelectedUtilityStack().isEmpty) {
            inventoryAddon.selectedUtility = slotIndex - utilityBeltRange.first
        }
    }

    // ==============================
    // Additional functionality
    // ==============================
    /**
     * This is called when the player presses "Swap Item With Offhand" (F by default) in the player's inventory screen
     */
    fun tryTransferToUtilityBeltSlot(sourceSlot: Slot?): Boolean {
        if (sourceSlot == null) {
            return false
        }
        val itemStackDynamic = sourceSlot.item
        val beltRange = getAvailableUtilityBeltRange()
        // If this is true, we send an item to the utility belt
        if (sourceSlot.index !in beltRange) {
            if (moveItemStackTo(itemStackDynamic, beltRange.first, beltRange.last + 1, false)) {
                if (inventoryAddon.player.level().isClientSide()) {
                    InventorioNetworking.INSTANCE.c2sMoveItemToUtilityBelt(sourceSlot.index)
                }
                return true
            }
            return false
        }
        // If we're here, we're sending an item FROM the utility belt to the rest of the inventory
        val deepPocketsRange = getAvailableDeepPocketsRange()
        if (moveItemStackTo(itemStackDynamic, mainInventoryRange.first, mainInventoryRange.last + 1, false)
            || moveItemStackTo(itemStackDynamic, deepPocketsRange.first, deepPocketsRange.last + 1, false)
        ) {
            if (inventoryAddon.player.level().isClientSide()) {
                InventorioNetworking.INSTANCE.c2sMoveItemToUtilityBelt(sourceSlot.index)
            }
            return true
        }
        return false
    }

    /**
     * Updates slots position and availability depending on the current level of Deep Pockets Enchantment,
     * and drops items from newly locked slots
     */
    fun updateDeepPocketsCapacity() {
        val player = inventoryAddon.player

        for (i in getAvailableDeepPocketsRange())
            (getSlot(i) as DeepPocketsSlot).canTakeItems = true
        for (i in getAvailableUtilityBeltRange())
            (getSlot(i) as DeepPocketsSlot).canTakeItems = true

        for (i in getUnavailableDeepPocketsRange()) {
            val slot = getSlot(i) as DeepPocketsSlot
            player.drop(slot.item, true, Prediction.PREDICTED)
            slot.setByPlayer(ItemStack.EMPTY)
            slot.canTakeItems = false
        }
        for (i in getUnavailableUtilityBeltRange()) {
            val slot = getSlot(i) as DeepPocketsSlot
            player.drop(slot.item, true, Prediction.PREDICTED)
            slot.setByPlayer(ItemStack.EMPTY)
            slot.canTakeItems = false
            if (inventoryAddon.selectedUtility >= UTILITY_BELT_SMALL_SIZE) {
                inventoryAddon.selectedUtility -= UTILITY_BELT_SMALL_SIZE
            }
        }
        if (inventoryAddon.player.level().isClientSide()) {
            refreshSlotPositions()
        }
    }

    @Environment(EnvType.CLIENT)
    private fun refreshSlotPositions() {
        (Minecraft.getInstance().gui.screen() as? InventorioScreen)?.onRefresh()
        val deepPocketsRowCount = inventoryAddon.getDeepPocketsRowCount()

        for ((absoluteIndex, relativeIndex) in mainInventoryWithoutHotbarRange.withRelativeIndex()) {
            val slot = getSlot(absoluteIndex) as SlotAccessor
            slot.x = SLOTS_INVENTORY_MAIN(deepPocketsRowCount).x + SLOT_UI_SIZE * (relativeIndex % VANILLA_ROW_LENGTH)
            slot.y = SLOTS_INVENTORY_MAIN(deepPocketsRowCount).y + SLOT_UI_SIZE * (relativeIndex / VANILLA_ROW_LENGTH)
        }
        for ((absoluteIndex, relativeIndex) in hotbarRange.withRelativeIndex()) {
            val slot = getSlot(absoluteIndex) as SlotAccessor
            slot.x = SLOTS_INVENTORY_HOTBAR(deepPocketsRowCount).x + SLOT_UI_SIZE * relativeIndex
            slot.y = SLOTS_INVENTORY_HOTBAR(deepPocketsRowCount).y
        }
        for ((absoluteIndex, relativeIndex) in toolBeltRange.withRelativeIndex()) {
            val slot = getSlot(absoluteIndex) as SlotAccessor
            slot.x = ToolBeltSlot.getSlotPosition(deepPocketsRowCount, relativeIndex, getToolBeltSlotCount()).x
            slot.y = ToolBeltSlot.getSlotPosition(deepPocketsRowCount, relativeIndex, getToolBeltSlotCount()).y
        }
    }

    // Note: this class returns the range within the SCREEN HANDLER, which is different from the range within the inventory
    private fun getAvailableUtilityBeltRange(): IntRange {
        return utilityBeltRange.first expandBy inventoryAddon.getAvailableUtilityBeltSize()
    }

    // Note: this class returns the range within the SCREEN HANDLER, which is different from the range within the inventory
    private fun getUnavailableUtilityBeltRange(): IntRange {
        return getAvailableUtilityBeltRange().last + 1..utilityBeltRange.last
    }

    // Note: this class returns the range within the SCREEN HANDLER, which is different from the range within the inventory
    @Suppress("MemberVisibilityCanBePrivate") // used in non-common package
    fun getAvailableDeepPocketsRange(): IntRange {
        return deepPocketsRange.first expandBy inventoryAddon.getDeepPocketsRowCount() * VANILLA_ROW_LENGTH
    }

    // Note: this class returns the range within the SCREEN HANDLER, which is different from the range within the inventory
    private fun getUnavailableDeepPocketsRange(): IntRange {
        return getAvailableDeepPocketsRange().last + 1..deepPocketsRange.last
    }

    fun getToolBeltSlotCount(): Int = toolBeltTemplates.size

    // ===================================================
    // Unmodified methods lifted from InventoryScreen
    // ===================================================
    // 26.3: recipe book placement (fillCraftSlotsStackedContents, clearCraftingContent, recipeMatches,
    // grid width/height) is handled by AbstractCraftingMenu
    override fun slotsChanged(inventory: Container) {
        val level = this.inventory.player.level()
        if (level is ServerLevel) {
            CraftingScreenHandlerAccessor.updateTheResult(
                this,
                level,
                this.inventory.player,
                this.craftingInput,
                this.craftingResult,
                null,
            )
        }
    }

    override fun removed(player: Player) {
        super.removed(player)
        craftingResult.clearContent()
        if (!player.level().isClientSide()) {
            clearContainer(player, craftingInput)
        }
    }

    override fun stillValid(player: Player): Boolean {
        return true
    }

    override fun canTakeItemForPickAll(carried: ItemStack, target: Slot): Boolean {
        return target.container !== craftingResult && super.canTakeItemForPickAll(carried, target)
    }

    override fun getResultSlot(): Slot = slots[0]

    override fun getInputGridSlots(): List<Slot> = slots.subList(1, 5)

    override fun owner(): Player = inventory.player

    override fun getRecipeBookType(): RecipeBookType {
        return RecipeBookType.CRAFTING
    }

    // required for curios integration because curios does the same
    override fun getSlot(index: Int): Slot {
        return super.getSlot(index.coerceIn(slots.indices))
    }

    @Suppress("RedundantOverride") // this makes it easier to add functionality for mod compat via mixin
    override fun setItem(slot: Int, revision: Int, stack: ItemStack) {
        super.setItem(slot, revision, stack)
    }

    // ===================================================
    // Companion Object
    // ===================================================
    companion object {
        @JvmField val craftingGridRange: IntRange = 0 expandBy CRAFTING_GRID_SIZE
        @JvmField val armorSlotsRange: IntRange = craftingGridRange.last + 1 expandBy ARMOR_SIZE
        @JvmField val mainInventoryRange: IntRange = armorSlotsRange.last + 1 expandBy MAIN_INVENTORY_SIZE
        @JvmField val deepPocketsRange: IntRange = mainInventoryRange.last + 1 expandBy DEEP_POCKETS_MAX_SIZE
        @JvmField val utilityBeltRange: IntRange = deepPocketsRange.last + 1 expandBy UTILITY_BELT_FULL_SIZE
        @JvmField val toolBeltRange: IntRange = utilityBeltRange.last + 1 expandBy toolBeltTemplates.size

        @JvmField val mainInventoryWithoutHotbarRange: IntRange = mainInventoryRange.first..mainInventoryRange.last - 9
        @JvmField val hotbarRange: IntRange = mainInventoryWithoutHotbarRange.last + 1..mainInventoryRange.last

        @JvmStatic
        val Player.inventorioScreenHandler: InventorioScreenHandler?
            get() = containerMenu as? InventorioScreenHandler

        private val openConsumers = mutableMapOf<Identifier, Consumer<InventorioScreenHandler>>()
        private val armorSlots = arrayOf(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)

        @JvmStatic
        fun registerOpenConsumer(customIdentifier: Identifier, screenHandlerConsumer: Consumer<InventorioScreenHandler>) {
            if (openConsumers.containsKey(customIdentifier)) {
                throw IllegalStateException("The Identifier '$customIdentifier' has already been taken")
            }
            openConsumers[customIdentifier] = screenHandlerConsumer
        }

        @Suppress("unused") // used in non-common package
        @JvmStatic
        fun open(player: Player) {
            player.openMenu(
                SimpleMenuProvider(
                    { syncId, playerInventory, _ -> InventorioScreenHandler(syncId, playerInventory!!) },
                    Component.translatable("container.crafting"),
                ),
            )
        }
    }
}
