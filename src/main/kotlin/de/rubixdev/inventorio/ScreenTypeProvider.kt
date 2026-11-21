package de.rubixdev.inventorio

import de.rubixdev.inventorio.player.InventorioScreenHandler
import net.minecraft.world.inventory.MenuType

interface ScreenTypeProvider {
    fun getScreenHandlerType(): MenuType<InventorioScreenHandler>

    companion object {
        // TODO: custom ktlint rule to allow lateinit var in caps
        @Suppress("ktlint:standard:property-naming")
        lateinit var INSTANCE: ScreenTypeProvider
    }
}
