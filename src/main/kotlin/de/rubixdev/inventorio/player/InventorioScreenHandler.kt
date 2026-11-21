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
import net.minecraft.world.inventory.TransientCraftingContainer
import net.minecraft.world.inventory.ResultContainer
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeHolder
import net.minecraft.world.entity.player.StackedContents
import net.minecraft.world.inventory.RecipeBookType
import net.minecraft.world.inventory.RecipeBookMenu
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.inventory.ResultSlot
import net.minecraft.world.inventory.Slot
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

import net.minecraft.world.item.crafting.CraftingRecipe
import net.minecraft.world.item.crafting.CraftingInput

class InventorioScreenHandler(syncId: Int, val inventory: Inventory) :
    RecipeBookMenu<CraftingInput, CraftingRecipe>
    (ScreenTypeProvider.INSTANCE.getScreenHandlerType(), syncId) {
    val inventoryAddon = inventory.player.inventoryAddon!!

    private val craftingInput = TransientCraftingContainer(this, 2, 2)
    private val craftingResult = ResultContainer()

    // ===================================================
    // Modified methods
    // ===================================================
    init {
        // Crafting Grid
        if (GlobalSettings.allow2x2CraftingGrid.boolValue) {
            addSlot(ResultSlot(inventory.player, craftingInput, craftingResult, 0, 174, 28))
            for (i in 0..3)
                addSlot(Slot(craftingInput, i, 118 + i % 2 * 18, 18 + i / 2 * 18))
        } else {
            addSlot(BlockedSlot(inventory, 0, 174, 28))
            for (i in 0..3)
                addSlot(BlockedSlot(craftingInput, i, 118 + i % 2 * 18, 18 + i / 2 * 18))
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
                sourceSlot.item = ItemStack.EMPTY
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
        if (sourceSlot.id !in beltRange) {
            if (insertItem(itemStackDynamic, beltRange.first, beltRange.last + 1, false)) {
                if (inventoryAddon.player.level().isClientSide) {
                    InventorioNetworking.INSTANCE.c2sMoveItemToUtilityBelt(sourceSlot.id)
                }
                return true
            }
            return false
        }
        // If we're here, we're sending an item FROM the utility belt to the rest of the inventory
        val deepPocketsRange = getAvailableDeepPocketsRange()
        if (insertItem(itemStackDynamic, mainInventoryRange.first, mainInventoryRange.last + 1, false)
            || insertItem(itemStackDynamic, deepPocketsRange.first, deepPocketsRange.last + 1, false)
        ) {
            if (inventoryAddon.player.level().isClientSide) {
                InventorioNetworking.INSTANCE.c2sMoveItemToUtilityBelt(sourceSlot.id)
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
            player.drop(slot.item, false, true)
            slot.set(ItemStack.EMPTY)
            slot.canTakeItems = false
        }
        for (i in getUnavailableUtilityBeltRange()) {
            val slot = getSlot(i) as DeepPocketsSlot
            player.drop(slot.item, false, true)
            slot.set(ItemStack.EMPTY)
            slot.canTakeItems = false
            if (inventoryAddon.selectedUtility >= UTILITY_BELT_SMALL_SIZE) {
                inventoryAddon.selectedUtility -= UTILITY_BELT_SMALL_SIZE
            }
        }
        if (inventoryAddon.player.level().isClientSide) {
            refreshSlotPositions()
        }
    }

    @Environment(EnvType.CLIENT)
    private fun refreshSlotPositions() {
        (Minecraft.getInstance().screen as? InventorioScreen)?.onRefresh()
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
    override fun shouldMoveToInventory(index: Int): Boolean {
        return index != this.resultSlotIndex
    }

    override fun fillCraftSlotsStackedContents(finder: StackedContents) {
        craftingInput.fillStackedContents(finder)
    }

    override fun clearCraftingContent() {
        craftingResult.clear()
        craftingInput.clear()
    }

    override fun recipeMatches(recipe: RecipeHolder<CraftingRecipe>?): Boolean =
        recipe?.value?.matches(craftingInput.asCraftInput(), inventory.player.level()) ?: false

    override fun slotsChanged(inventory: Container) {
        CraftingScreenHandlerAccessor.updateTheResult(
            this,
            this.inventory.player.level(),
            this.inventory.player,
            this.craftingInput,
            this.craftingResult,
            null,
        )
    }

    override fun removed(player: Player) {
        super.removed(player)
        craftingResult.clear()
        if (!player.level().isClientSide) {
            clearContainer(player, craftingInput)
        }
    }

    override fun stillValid(player: Player): Boolean {
        return true
    }

    override fun shouldMoveToInventory(stack: ItemStack, slot: Slot): Boolean {
        return slot.inventory !== craftingResult && super.shouldMoveToInventory(stack, slot)
    }

    override fun getResultSlotIndex(): Int {
        return 0
    }

    override fun getGridWidth(): Int {
        return craftingInput.width
    }

    override fun getGridHeight(): Int {
        return craftingInput.height
    }

    override fun getSize(): Int {
        return 5
    }

    override fun getRecipeBookType(): RecipeBookType {
        return RecipeBookType.CRAFTING
    }

    // required for curios integration because curios does the same
    override fun getSlot(index: Int): Slot {
        return super.getSlot(index.coerceIn(slots.indices))
    }

    @Suppress("RedundantOverride") // this makes it easier to add functionality for mod compat via mixin
    override fun setItem(slot: Int, revision: Int, stack: ItemStack?) {
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
            get() = currentScreenHandler as? InventorioScreenHandler

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
