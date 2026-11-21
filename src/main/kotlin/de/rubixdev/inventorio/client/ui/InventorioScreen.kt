package de.rubixdev.inventorio.client.ui

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
import net.minecraft.client.gui.components.ImageButton
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.WidgetSprites
import net.minecraft.client.gui.navigation.ScreenPosition
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen
import net.minecraft.client.gui.screens.inventory.EffectsInInventory
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.gui.screens.recipebook.CraftingRecipeBookComponent
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.sounds.SoundManager
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.Inventory

@Environment(EnvType.CLIENT)
class InventorioScreen private constructor(
    handler: InventorioScreenHandler,
    internal val inventory: Inventory,
    private val recipeBook: CraftingRecipeBookComponent,
) : AbstractRecipeBookScreen<InventorioScreenHandler>(
    handler,
    recipeBook,
    inventory,
    Component.translatable("container.crafting"),
) {
    constructor(handler: InventorioScreenHandler, inventory: Inventory) :
        this(handler, inventory, CraftingRecipeBookComponent(handler))

    private var xMouse = 0f
    private var yMouse = 0f
    private var recipeButton: ImageButton? = null
    private var toggleButton: ImageButton? = null
    private var lockedCraftButton: ImageButton? = null
    private var narrow = false
    private var mouseDown = false
    private val effects = EffectsInInventory(this)

    private val inventoryAddon = inventory.player.inventoryAddon!!

    // imageWidth/imageHeight are final in 26.x; they are made mutable through the accessor
    @Suppress("CAST_NEVER_SUCCEEDS")
    private val accessor get() = (this as Any) as HandledScreenAccessor<*>

    init {
        titleLabelX = INVENTORY_TITLE_X + CRAFTING_GRID_OFFSET_X
    }

    // ===================================================
    // Heavily modified or new methods
    // ===================================================
    override fun init() {
        val player = minecraft.player!!
        if (player.hasInfiniteMaterials()) {
            minecraft.gui.setScreen(CreativeModeInventoryScreen(player, player.connection.enabledFeatures(), minecraft.options.operatorItemsTab().get()))
            return
        }
        narrow = width < 379
        onRefresh()
        // sets leftPos/topPos, inits the recipe book and adds the recipe book button
        // (positioned through [getRecipeBookButtonPosition])
        super.init()
        val vanillaRecipeButton = children().firstOrNull { it is ImageButton } as ImageButton?
        recipeButton = vanillaRecipeButton?.let { old ->
            // replace the vanilla recipe book button with an equivalent one that doesn't play the click sound
            removeWidget(old)
            val pos = getRecipeBookButtonPosition()
            addRenderableWidget(
                SilentImageButton(pos.x(), pos.y(), 20, 18, RecipeBookComponent.RECIPE_BUTTON_SPRITES) { button ->
                    recipeBook.toggleVisibility()
                    leftPos = recipeBook.updateScreenPosition(width, imageWidth)
                    val newPos = getRecipeBookButtonPosition()
                    button.setPosition(newPos.x(), newPos.y())
                    onRecipeBookButtonClick()
                },
            )
        }
        toggleButton = addToggleButton(this)
        lockedCraftButton = addLockedCraftButton(this)
        updateScreenPosition()
        player.inventorioScreenHandler?.updateDeepPocketsCapacity()

        initConsumers.forEach {
            try {
                it.value.accept(this)
            } catch (e: Throwable) {
                logger.error("Inventory Screen Init Consumer '${it.key}' has failed: ", e)
            }
        }
    }

    fun onRefresh() {
        accessor.setBackgroundWidth(GUI_INVENTORY_TOP.width + ((menu.getToolBeltSlotCount() - 1) / ToolBeltSlot.getColumnCapacity(inventoryAddon.getDeepPocketsRowCount())) * SLOT_UI_SIZE)
        accessor.setBackgroundHeight(INVENTORY_HEIGHT + inventoryAddon.getDeepPocketsRowCount() * SLOT_UI_SIZE)
        setRecipeLeftOffset()
        updateScreenPosition()
    }

    @Suppress("MemberVisibilityCanBePrivate") // used from non-common package
    fun updateScreenPosition() {
        leftPos = findLeftEdge()
        topPos = (height - imageHeight) / 2
        recipeButton?.x = leftPos + GUI_RECIPE_WIDGET_BUTTON.x
        recipeButton?.y = topPos + GUI_RECIPE_WIDGET_BUTTON.y
        mouseDown = true
    }

    override fun getRecipeBookButtonPosition(): ScreenPosition {
        // Called by vanilla right after it has (re)computed leftPos; override it with our own logic
        leftPos = findLeftEdge()
        return ScreenPosition(leftPos + GUI_RECIPE_WIDGET_BUTTON.x, topPos + GUI_RECIPE_WIDGET_BUTTON.y)
    }

    override fun onRecipeBookButtonClick() {
        updateScreenPosition()
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
        false -> recipeBook.updateScreenPosition(width, GUI_INVENTORY_TOP.width - 22)
    }

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        super.extractBackground(graphics, mouseX, mouseY, a)
        if (PlayerSettings.aggressiveButtonRemoval.boolValue) {
            for (child in children().filter {
                it is Renderable &&
                    it != recipeButton &&
                    it != toggleButton &&
                    it != lockedCraftButton &&
                    it != recipeBook
            }) {
                removeWidget(child)
            }
        }
        val x = leftPos
        val y = topPos
        toggleButton?.x = x + imageWidth + GUI_TOGGLE_BUTTON_OFFSET.x
        toggleButton?.y = y + GUI_TOGGLE_BUTTON_OFFSET.y
        lockedCraftButton?.x = x + GUI_LOCKED_CRAFTING_POS.x
        lockedCraftButton?.y = y + GUI_LOCKED_CRAFTING_POS.y

        val texture = if (PlayerSettings.darkTheme.boolValue) {
            BACKGROUND_TEXTURE_DARK
        } else {
            BACKGROUND_TEXTURE
        }
        val texW = CANVAS_INVENTORY_TEXTURE_SIZE.x
        val texH = CANVAS_INVENTORY_TEXTURE_SIZE.y
        val pipeline = RenderPipelines.GUI_TEXTURED

        val deepPocketsRowCount = inventoryAddon.getDeepPocketsRowCount()

        // Top Part
        graphics.blit(
            pipeline, texture,
            x + GUI_INVENTORY_TOP.x, y + GUI_INVENTORY_TOP.y,
            CANVAS_INVENTORY_TOP.x, CANVAS_INVENTORY_TOP.y,
            GUI_INVENTORY_TOP.width, GUI_INVENTORY_TOP.height,
            texW, texH,
        )

        // Main Rows
        val guiMainRect = GUI_INVENTORY_MAIN(deepPocketsRowCount)
        graphics.blit(
            pipeline, texture,
            x + guiMainRect.x, y + guiMainRect.y,
            CANVAS_INVENTORY_MAIN.x, CANVAS_INVENTORY_MAIN.y,
            guiMainRect.width, guiMainRect.height,
            texW, texH,
        )

        // Deep Pockets Rows
        if (inventoryAddon.getAvailableUtilityBeltSize() == UTILITY_BELT_FULL_SIZE) {
            val guiDeepPocketsRect = GUI_INVENTORY_DEEP_POCKETS(deepPocketsRowCount)
            graphics.blit(
                pipeline, texture,
                x + guiDeepPocketsRect.x, y + guiDeepPocketsRect.y,
                CANVAS_INVENTORY_DEEP_POCKETS.x, CANVAS_INVENTORY_DEEP_POCKETS.y,
                guiDeepPocketsRect.width, guiDeepPocketsRect.height,
                texW, texH,
            )

            graphics.blit(
                pipeline, texture,
                x + GUI_UTILITY_BELT_COLUMN_2.x, y + GUI_UTILITY_BELT_COLUMN_2.y,
                CANVAS_UTILITY_BELT_COLUMN_2.x, CANVAS_UTILITY_BELT_COLUMN_2.y,
                GUI_UTILITY_BELT_COLUMN_2.width, GUI_UTILITY_BELT_COLUMN_2.height,
                texW, texH,
            )
        }

        // Utility Belt Selection Frame
        graphics.blit(
            pipeline, texture,
            x + GUI_UTILITY_BELT_FRAME_ORIGIN.x + (inventoryAddon.selectedUtility / UTILITY_BELT_SMALL_SIZE) * SLOT_UI_SIZE,
            y + GUI_UTILITY_BELT_FRAME_ORIGIN.y + (inventoryAddon.selectedUtility % UTILITY_BELT_SMALL_SIZE) * SLOT_UI_SIZE,
            CANVAS_UTILITY_BELT_FRAME.x.toFloat(), CANVAS_UTILITY_BELT_FRAME.y.toFloat(),
            CANVAS_UTILITY_BELT_FRAME.width, CANVAS_UTILITY_BELT_FRAME.height,
            texW, texH,
        )

        // Tool Belt

        // If Tool Belt is 2+ columns wide, draw extra background pieces
        val size = menu.getToolBeltSlotCount()
        for (column in 0..<(size - 1) / ToolBeltSlot.getColumnCapacity(deepPocketsRowCount)) {
            graphics.blit(
                pipeline, texture,
                x + GUI_TOOL_BELT_UI_EXTENSION.x + column * SLOT_UI_SIZE, y + GUI_TOOL_BELT_UI_EXTENSION.y,
                CANVAS_TOOL_BELT_UI_EXTENSION.x.toFloat(), CANVAS_TOOL_BELT_UI_EXTENSION.y.toFloat(),
                CANVAS_TOOL_BELT_UI_EXTENSION.width, CANVAS_TOOL_BELT_UI_EXTENSION.height,
                texW, texH,
            )
            if (deepPocketsRowCount > 0) {
                graphics.blit(
                    pipeline, texture,
                    x + GUI_TOOL_BELT_UI_EXTENSION.x + column * SLOT_UI_SIZE,
                    y + GUI_TOOL_BELT_UI_EXTENSION.y + CANVAS_TOOL_BELT_UI_EXTENSION.height / 2 + DEEP_POCKETS_EXTRA_HEIGHT(deepPocketsRowCount),
                    CANVAS_TOOL_BELT_UI_EXTENSION.x.toFloat(),
                    CANVAS_TOOL_BELT_UI_EXTENSION.y.toFloat() + CANVAS_TOOL_BELT_UI_EXTENSION.height / 2,
                    CANVAS_TOOL_BELT_UI_EXTENSION.width, CANVAS_TOOL_BELT_UI_EXTENSION.height / 2,
                    texW, texH,
                )
            }
        }

        // Draw a slot background per each tool belt slot
        for (index in inventoryAddon.toolBelt.indices)
            graphics.blit(
                pipeline, texture,
                x + ToolBeltSlot.getGuiPosition(deepPocketsRowCount, index, size).x,
                y + ToolBeltSlot.getGuiPosition(deepPocketsRowCount, index, size).y,
                CANVAS_TOOL_BELT.x, CANVAS_TOOL_BELT.y,
                SLOT_UI_SIZE, SLOT_UI_SIZE,
                texW, texH,
            )

        // Draw empty slot icons
        for ((index, stack) in inventoryAddon.toolBelt.withIndex())
            if (stack.isEmpty) {
                graphics.blit(
                    pipeline,
                    PlayerInventoryAddon.toolBeltTemplates[index].emptyIcon,
                    x + ToolBeltSlot.getSlotPosition(deepPocketsRowCount, index, size).x,
                    y + ToolBeltSlot.getSlotPosition(deepPocketsRowCount, index, size).y,
                    0f, 0f, 16, 16, 16, 16,
                )
            }

        InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, x + 26, y + 8, x + 75, y + 78, 30, 0.0625f, xMouse, yMouse, minecraft.player!!)
    }

    // ===================================================
    // Unmodified methods lifted from InventoryScreen
    // ===================================================
    override fun extractLabels(graphics: GuiGraphicsExtractor, xm: Int, ym: Int) {
        graphics.text(font, title, titleLabelX, titleLabelY, -12566464, false)
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        effects.extractRenderState(graphics, mouseX, mouseY)
        super.extractRenderState(graphics, mouseX, mouseY, a)
        xMouse = mouseX.toFloat()
        yMouse = mouseY.toFloat()
    }

    override fun showsActiveEffects(): Boolean = effects.canSeeEffects()

    override fun isBiggerResultSlot(): Boolean = false

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (!mouseDown) {
            return super.mouseReleased(event)
        }
        mouseDown = false
        return true
    }

    override fun containerTick() {
        val player = minecraft.player
        if (player != null && player.hasInfiniteMaterials()) {
            player.closeContainer()
            minecraft.gui.setScreen(CreativeModeInventoryScreen(player, player.connection.enabledFeatures(), minecraft.options.operatorItemsTab().get()))
        } else {
            super.containerTick()
        }
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
            val button = SilentImageButton(
                screenAccessor.x + screenAccessor.backgroundWidth + GUI_TOGGLE_BUTTON_OFFSET.x,
                screenAccessor.y + GUI_TOGGLE_BUTTON_OFFSET.y,
                GUI_TOGGLE_BUTTON_OFFSET.width,
                GUI_TOGGLE_BUTTON_OFFSET.height,
                if (PlayerSettings.darkTheme.boolValue) texturesDark else textures,
            ) {
                val client = Minecraft.getInstance() ?: return@SilentImageButton
                shouldOpenVanillaInventory = client.gui.screen() is InventorioScreen
                isSwappingInvScreens = true
                client.gui.screen()?.onClose()
                if (shouldOpenVanillaInventory) {
                    client.gui.setScreen(InventoryScreen(client.player!!))
                } else {
                    InventorioNetworking.INSTANCE.c2sOpenInventorioScreen()
                }
                isSwappingInvScreens = false
            }
            screenAccessor.selectables.add(button)
            screenAccessor.drawables.add(button)
            screenAccessor.children.add(button)
            return button
        }

        @JvmStatic
        fun addLockedCraftButton(screen: Screen): ImageButton? {
            if (GlobalSettings.allow2x2CraftingGrid.boolValue) {
                return null
            }
            val screenAccessor = screen as HandledScreenAccessor<*>
            val button = SilentImageButton(
                screenAccessor.x + GUI_LOCKED_CRAFTING_POS.x,
                screenAccessor.y + GUI_LOCKED_CRAFTING_POS.y,
                GUI_LOCKED_CRAFTING_POS.width,
                GUI_LOCKED_CRAFTING_POS.height,
                if (PlayerSettings.darkTheme.boolValue) LOCK_BUTTON_TEXTURES_DARK else LOCK_BUTTON_TEXTURES,
            ) {
                val client = Minecraft.getInstance() ?: return@SilentImageButton
                isSwappingInvScreens = true
                client.gui.screen()?.onClose()
                client.gui.setScreen(InventoryScreen(client.player!!))
                isSwappingInvScreens = false
            }
            screenAccessor.selectables.add(button)
            screenAccessor.drawables.add(button)
            screenAccessor.children.add(button)
            return button
        }
    }
}

/** An [ImageButton] that doesn't play the UI click sound when pressed. */
@Environment(EnvType.CLIENT)
class SilentImageButton(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    sprites: WidgetSprites,
    onPress: OnPress,
) : ImageButton(x, y, width, height, sprites, onPress) {
    override fun playDownSound(soundManager: SoundManager) {}
}
