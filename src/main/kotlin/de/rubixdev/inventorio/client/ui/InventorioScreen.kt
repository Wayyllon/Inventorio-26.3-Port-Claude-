package de.rubixdev.inventorio.client.ui

import com.mojang.blaze3d.systems.RenderSystem
import de.rubixdev.inventorio.config.GlobalSettings
import de.rubixdev.inventorio.config.PlayerSettings
import de.rubixdev.inventorio.duck.RecipeBookLeftOffsetOverride
import de.rubixdev.inventorio.mixin.client.accessor.HandledScreenAccessor
import de.rubixdev.inventorio.packet.InventorioNetworking
import de.rubixdev.inventorio.player.InventorioScreenHandler
import de.rubixdev.inventorio.player.InventorioScreenHandler.Companion.inventorioScreenHandler
import de.rubixdev.inventorio.player.PlayerInventoryAddon
import de.rubixdev.inventorio.player.PlayerInventoryAddon.Companion.inventoryAddon
import de.rubixdev.inventorio.slot.ToolBeltSlot
import de.rubixdev.inventorio.util.*
import java.util.function.Consumer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.WidgetSprites
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent
import net.minecraft.client.gui.components.ImageButton
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.Slot
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

@Environment(EnvType.CLIENT)
class InventorioScreen(handler: InventorioScreenHandler, internal val inventory: Inventory) :
    EffectRenderingInventoryScreen<InventorioScreenHandler>(
        handler,
        inventory,
        Component.translatable("container.crafting"),
    ),
    RecipeUpdateListener {
    private var mouseX = 0f
    private var mouseY = 0f
    private val recipeBook = RecipeBookComponent()
    private var recipeButton: ImageButton? = null
    private var toggleButton: ImageButton? = null
    private var lockedCraftButton: ImageButton? = null
    private var open = false
    private var narrow = false
    private var mouseDown = false

    private val inventoryAddon = inventory.player.inventoryAddon!!

    init {
        titleLabelX = INVENTORY_TITLE_X + CRAFTING_GRID_OFFSET_X
    }

    // ===================================================
    // Heavily modified or new methods
    // ===================================================
    override fun init() {
        val client = this.client!!
        if (client.gameMode!!.hasInfiniteItems()) {
            client.setScreen(CreativeModeInventoryScreen(client.player, client.player!!.connection.enabledFeatures, client.options.operatorItemsTab.value))
            return
        }
        onRefresh()
        super.init()
        narrow = width < 379
        recipeBook.init(width, height, client, narrow, handler)
        setRecipeLeftOffset()
        open = true
        toggleButton = addToggleButton(this)
        lockedCraftButton = addLockedCraftButton(this)
        recipeButton = addRenderableWidget(
            ImageButton(
                x + GUI_RECIPE_WIDGET_BUTTON.x,
                y + GUI_RECIPE_WIDGET_BUTTON.y,
                GUI_RECIPE_WIDGET_BUTTON.width,
                GUI_RECIPE_WIDGET_BUTTON.height,
                RecipeBookComponent.RECIPE_BUTTON_SPRITES,
            ) {
                setRecipeLeftOffset()
                recipeBook.toggleVisibility()
                updateScreenPosition()
            },
        )
        addWidget(recipeBook)
        setInitialFocus(recipeBook)
        updateScreenPosition()
        client.player?.inventorioScreenHandler?.updateDeepPocketsCapacity()

        initConsumers.forEach {
            try {
                it.value.accept(this)
            } catch (e: Throwable) {
                logger.error("Inventory Screen Init Consumer '${it.key}' has failed: ", e)
            }
        }
    }

    fun onRefresh() {
        imageWidth = GUI_INVENTORY_TOP.width + ((handler.getToolBeltSlotCount() - 1) / ToolBeltSlot.getColumnCapacity(inventoryAddon.getDeepPocketsRowCount())) * SLOT_UI_SIZE
        imageHeight = INVENTORY_HEIGHT + inventoryAddon.getDeepPocketsRowCount() * SLOT_UI_SIZE
        updateScreenPosition()
    }

    @Suppress("MemberVisibilityCanBePrivate") // used from non-common package
    fun updateScreenPosition() {
        x = findLeftEdge()
        y = (height - imageHeight) / 2
        recipeButton?.x = x + GUI_RECIPE_WIDGET_BUTTON.x
        recipeButton?.y = y + GUI_RECIPE_WIDGET_BUTTON.y
        mouseDown = true
    }

    private fun setRecipeLeftOffset() {
        (recipeBook as RecipeBookLeftOffsetOverride).`inventorio$setBackgroundWidth`(imageWidth)
    }

    private fun findLeftEdge(): Int = when (PlayerSettings.centeredScreen.boolValue) {
        true -> when (recipeBook.isVisible && !narrow) {
            true -> (width - imageWidth + RecipeBookComponent.IMAGE_WIDTH + 2) / 2
            false -> (width - imageWidth) / 2
        }.also { setRecipeLeftOffset() }
        // TODO: fix recipe book offset for non-centered screen
        false -> recipeBook.findLeftEdge(width, GUI_INVENTORY_TOP.width - 22)
    }

    override fun renderBg(drawContext: GuiGraphicsExtractor, delta: Float, mouseX: Int, mouseY: Int) {
        if (PlayerSettings.aggressiveButtonRemoval.boolValue) {
            for (child in children().filter {
                it is Renderable
                    && it != recipeButton
                    && it != toggleButton
                    && it != lockedCraftButton
            }) {
                removeWidget(child)
            }
        }
        toggleButton?.x = x + imageWidth + GUI_TOGGLE_BUTTON_OFFSET.x
        toggleButton?.y = y + GUI_TOGGLE_BUTTON_OFFSET.y
        lockedCraftButton?.x = x + GUI_LOCKED_CRAFTING_POS.x
        lockedCraftButton?.y = y + GUI_LOCKED_CRAFTING_POS.y

        RenderSystem.setShader { GameRenderer.getPositionTexShader() }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
        val texture = if (PlayerSettings.darkTheme.boolValue) {
            BACKGROUND_TEXTURE_DARK
        } else {
            BACKGROUND_TEXTURE
        }

        val deepPocketsRowCount = inventoryAddon.getDeepPocketsRowCount()

        // Top Part
        drawContext.blit(
            texture,
            x + GUI_INVENTORY_TOP.x, y + GUI_INVENTORY_TOP.y,
            CANVAS_INVENTORY_TOP.x, CANVAS_INVENTORY_TOP.y,
            GUI_INVENTORY_TOP.width, GUI_INVENTORY_TOP.height,
            CANVAS_INVENTORY_TEXTURE_SIZE.x, CANVAS_INVENTORY_TEXTURE_SIZE.y,
        )

        // Main Rows
        val guiMainRect = GUI_INVENTORY_MAIN(deepPocketsRowCount)
        drawContext.blit(
            texture,
            x + guiMainRect.x, y + guiMainRect.y,
            CANVAS_INVENTORY_MAIN.x, CANVAS_INVENTORY_MAIN.y,
            guiMainRect.width, guiMainRect.height,
            CANVAS_INVENTORY_TEXTURE_SIZE.x, CANVAS_INVENTORY_TEXTURE_SIZE.y,
        )

        // Deep Pockets Rows
        if (inventoryAddon.getAvailableUtilityBeltSize() == UTILITY_BELT_FULL_SIZE) {
            val guiDeepPocketsRect = GUI_INVENTORY_DEEP_POCKETS(deepPocketsRowCount)
            drawContext.blit(
                texture,
                x + guiDeepPocketsRect.x, y + guiDeepPocketsRect.y,
                CANVAS_INVENTORY_DEEP_POCKETS.x, CANVAS_INVENTORY_DEEP_POCKETS.y,
                guiDeepPocketsRect.width, guiDeepPocketsRect.height,
                CANVAS_INVENTORY_TEXTURE_SIZE.x, CANVAS_INVENTORY_TEXTURE_SIZE.y,
            )

            drawContext.blit(
                texture,
                x + GUI_UTILITY_BELT_COLUMN_2.x, y + GUI_UTILITY_BELT_COLUMN_2.y,
                CANVAS_UTILITY_BELT_COLUMN_2.x, CANVAS_UTILITY_BELT_COLUMN_2.y,
                GUI_UTILITY_BELT_COLUMN_2.width, GUI_UTILITY_BELT_COLUMN_2.height,
                CANVAS_INVENTORY_TEXTURE_SIZE.x, CANVAS_INVENTORY_TEXTURE_SIZE.y,
            )
        }

        // Utility Belt Selection Frame
        drawContext.blit(
            texture,
            x + GUI_UTILITY_BELT_FRAME_ORIGIN.x + (inventoryAddon.selectedUtility / UTILITY_BELT_SMALL_SIZE) * SLOT_UI_SIZE,
            y + GUI_UTILITY_BELT_FRAME_ORIGIN.y + (inventoryAddon.selectedUtility % UTILITY_BELT_SMALL_SIZE) * SLOT_UI_SIZE,
            CANVAS_UTILITY_BELT_FRAME.x.toFloat(), CANVAS_UTILITY_BELT_FRAME.y.toFloat(),
            CANVAS_UTILITY_BELT_FRAME.width, CANVAS_UTILITY_BELT_FRAME.height,
            CANVAS_INVENTORY_TEXTURE_SIZE.x, CANVAS_INVENTORY_TEXTURE_SIZE.y,
        )

        // Tool Belt

        // If Tool Belt is 2+ columns wide, draw extra background pieces
        val size = handler.getToolBeltSlotCount()
        for (column in 0..<(size - 1) / ToolBeltSlot.getColumnCapacity(deepPocketsRowCount)) {
            drawContext.blit(
                texture,
                x + GUI_TOOL_BELT_UI_EXTENSION.x + column * SLOT_UI_SIZE, y + GUI_TOOL_BELT_UI_EXTENSION.y,
                CANVAS_TOOL_BELT_UI_EXTENSION.x.toFloat(), CANVAS_TOOL_BELT_UI_EXTENSION.y.toFloat(),
                CANVAS_TOOL_BELT_UI_EXTENSION.width, CANVAS_TOOL_BELT_UI_EXTENSION.height,
                CANVAS_INVENTORY_TEXTURE_SIZE.x, CANVAS_INVENTORY_TEXTURE_SIZE.y,
            )
            if (deepPocketsRowCount > 0) {
                drawContext.blit(
                    texture,
                    x + GUI_TOOL_BELT_UI_EXTENSION.x + column * SLOT_UI_SIZE,
                    y + GUI_TOOL_BELT_UI_EXTENSION.y + CANVAS_TOOL_BELT_UI_EXTENSION.height / 2 + DEEP_POCKETS_EXTRA_HEIGHT(deepPocketsRowCount),
                    CANVAS_TOOL_BELT_UI_EXTENSION.x.toFloat(),
                    CANVAS_TOOL_BELT_UI_EXTENSION.y.toFloat() + CANVAS_TOOL_BELT_UI_EXTENSION.height / 2,
                    CANVAS_TOOL_BELT_UI_EXTENSION.width, CANVAS_TOOL_BELT_UI_EXTENSION.height / 2,
                    CANVAS_INVENTORY_TEXTURE_SIZE.x, CANVAS_INVENTORY_TEXTURE_SIZE.y,
                )
            }
        }

        // Draw a slot background per each tool belt slot
        for (index in inventoryAddon.toolBelt.indices)
            drawContext.blit(
                texture,
                x + ToolBeltSlot.getGuiPosition(deepPocketsRowCount, index, size).x,
                y + ToolBeltSlot.getGuiPosition(deepPocketsRowCount, index, size).y,
                CANVAS_TOOL_BELT.x, CANVAS_TOOL_BELT.y,
                SLOT_UI_SIZE, SLOT_UI_SIZE,
                CANVAS_INVENTORY_TEXTURE_SIZE.x, CANVAS_INVENTORY_TEXTURE_SIZE.y,
            )

        // Draw empty slot icons
        for ((index, stack) in inventoryAddon.toolBelt.withIndex())
            if (stack.isEmpty) {
                drawContext.blit(
                    PlayerInventoryAddon.toolBeltTemplates[index].emptyIcon,
                    x + ToolBeltSlot.getSlotPosition(deepPocketsRowCount, index, size).x,
                    y + ToolBeltSlot.getSlotPosition(deepPocketsRowCount, index, size).y,
                    0f, 0f, 16, 16, 16, 16,
                )
            }

        InventoryScreen.drawEntity(drawContext, x + 26, y + 8, x + 75, y + 78, 30, 0.0625f, this.mouseX, this.mouseY, client!!.player)
    }

    // ===================================================
    // Unmodified methods lifted from InventoryScreen
    // ===================================================
    override fun renderLabels(drawContext: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        drawContext.drawString(font, title, titleLabelX, titleLabelY, 4210752, false)
    }

    override fun render(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        if (recipeBook.isVisible && narrow) {
            this.renderBackground(context, mouseX, mouseY, delta)
            recipeBook.render(context, mouseX, mouseY, delta)
        } else {
            super.render(context, mouseX, mouseY, delta)
            recipeBook.render(context, mouseX, mouseY, delta)
            recipeBook.renderGhostRecipe(context, x, y, false, delta)
        }

        this.renderTooltip(context, mouseX, mouseY)
        recipeBook.renderTooltip(context, x, y, mouseX, mouseY)
        this.mouseX = mouseX.toFloat()
        this.mouseY = mouseY.toFloat()
    }

    override fun isHovering(xPosition: Int, yPosition: Int, width: Int, height: Int, pointX: Double, pointY: Double): Boolean {
        return (!narrow || !recipeBook.isVisible) && super.isHovering(xPosition, yPosition, width, height, pointX, pointY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!recipeBook.mouseClicked(mouseX, mouseY, button)) {
            return if (narrow && recipeBook.isVisible) false else super.mouseClicked(mouseX, mouseY, button)
        }
        focused = recipeBook
        return true
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!mouseDown) {
            return super.mouseReleased(mouseX, mouseY, button)
        }
        mouseDown = false
        return true
    }

    override fun hasClickedOutside(mouseX: Double, mouseY: Double, left: Int, top: Int, button: Int): Boolean {
        val bl = mouseX < left.toDouble() || mouseY < top.toDouble() || mouseX >= (left + imageWidth).toDouble() || mouseY >= (top + imageHeight).toDouble()
        return recipeBook.hasClickedOutside(mouseX, mouseY, x, y, imageWidth, imageHeight, button) && bl
    }

    override fun onMouseClick(slot: Slot?, invSlot: Int, clickData: Int, actionType: ContainerInput) {
        super.onMouseClick(slot, invSlot, clickData, actionType)
        recipeBook.slotClicked(slot)
    }

    override fun refreshRecipeBook() {
        recipeBook.recipesUpdated()
    }

    override fun getRecipeBookWidget(): RecipeBookComponent {
        return recipeBook
    }

    override fun containerTick() {
        val client = client!!
        if (client.gameMode!!.hasInfiniteItems() && client.player != null) {
            client.player?.closeContainer()
            client.setScreen(CreativeModeInventoryScreen(client.player, client.player!!.connection.enabledFeatures, client.options.operatorItemsTab.value))
        } else {
            recipeBook.tick()
        }
    }

    override fun renderTooltip(context: GuiGraphicsExtractor, x: Int, y: Int) {
        super.renderTooltip(context, x, y)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        horizontalAmount: Double,
        verticalAmount: Double,
    ): Boolean {
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    // ===================================================
    // Companion Object
    // ===================================================
    companion object {
        private val TOGGLE_BUTTON_ON_TEXTURES = WidgetSprites("toggle_button_on".id, "toggle_button_active_on".id)
        private val TOGGLE_BUTTON_OFF_TEXTURES = WidgetSprites("toggle_button_off".id, "toggle_button_active_off".id)
        private val LOCK_BUTTON_TEXTURES = WidgetSprites("lock_button".id, "lock_button_active".id)
        private val TOGGLE_BUTTON_ON_TEXTURES_DARK = WidgetSprites("toggle_button_on_dark".id, "toggle_button_active_on_dark".id)
        private val TOGGLE_BUTTON_OFF_TEXTURES_DARK = WidgetSprites("toggle_button_off_dark".id, "toggle_button_active_off_dark".id)
        private val LOCK_BUTTON_TEXTURES_DARK = WidgetSprites("lock_button_dark".id, "lock_button_active_dark".id)
        private val BACKGROUND_TEXTURE = "textures/gui/player_inventory.png".id
        private val BACKGROUND_TEXTURE_DARK = "textures/gui/player_inventory_dark.png".id

        private val initConsumers = mutableMapOf<Identifier, Consumer<InventorioScreen>>()

        @JvmField
        var shouldOpenVanillaInventory = false
        @JvmField
        var isSwappingInvScreens = false

        @JvmStatic
        fun registerInitConsumer(customIdentifier: Identifier, uiConsumer: Consumer<InventorioScreen>) {
            if (initConsumers.containsKey(customIdentifier)) {
                throw IllegalStateException("The Identifier '$customIdentifier' has already been taken")
            }
            initConsumers[customIdentifier] = uiConsumer
        }

        @JvmStatic
        fun addToggleButton(screen: Screen): ImageButton? {
            if (!PlayerSettings.toggleButton.boolValue) {
                return null
            }
            val textures = if (screen is InventorioScreen) TOGGLE_BUTTON_ON_TEXTURES else TOGGLE_BUTTON_OFF_TEXTURES
            val texturesDark = if (screen is InventorioScreen) TOGGLE_BUTTON_ON_TEXTURES_DARK else TOGGLE_BUTTON_OFF_TEXTURES_DARK
            val screenAccessor = screen as HandledScreenAccessor<*>
            val button = ImageButton(
                screenAccessor.x + screen.imageWidth + GUI_TOGGLE_BUTTON_OFFSET.x,
                screenAccessor.y + GUI_TOGGLE_BUTTON_OFFSET.y,
                GUI_TOGGLE_BUTTON_OFFSET.width,
                GUI_TOGGLE_BUTTON_OFFSET.height,
                if (PlayerSettings.darkTheme.boolValue) texturesDark else textures,
            ) {
                val client = Minecraft.getInstance() ?: return@ImageButton
                shouldOpenVanillaInventory = client.screen is InventorioScreen
                isSwappingInvScreens = true
                client.screen?.close()
                if (shouldOpenVanillaInventory) {
                    client.setScreen(InventoryScreen(client.player))
                } else {
                    InventorioNetworking.INSTANCE.c2sOpenInventorioScreen()
                }
                isSwappingInvScreens = false
            }
            screenAccessor.narratables.add(button)
            screenAccessor.renderables.add(button)
            screenAccessor.children.add(button)
            return button
        }

        @JvmStatic
        fun addLockedCraftButton(screen: Screen): ImageButton? {
            if (GlobalSettings.allow2x2CraftingGrid.boolValue) {
                return null
            }
            val screenAccessor = screen as HandledScreenAccessor<*>
            val button = ImageButton(
                screenAccessor.x + GUI_LOCKED_CRAFTING_POS.x,
                screenAccessor.y + GUI_LOCKED_CRAFTING_POS.y,
                GUI_LOCKED_CRAFTING_POS.width,
                GUI_LOCKED_CRAFTING_POS.height,
                if (PlayerSettings.darkTheme.boolValue) LOCK_BUTTON_TEXTURES_DARK else LOCK_BUTTON_TEXTURES,
            ) {
                val client = Minecraft.getInstance() ?: return@ImageButton
                isSwappingInvScreens = true
                client.screen?.close()
                client.setScreen(InventoryScreen(client.player))
                isSwappingInvScreens = false
            }
            screenAccessor.narratables.add(button)
            screenAccessor.renderables.add(button)
            screenAccessor.children.add(button)
            return button
        }
    }
}
