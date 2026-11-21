package de.rubixdev.inventorio

import de.rubixdev.inventorio.client.ui.InventorioScreen
import de.rubixdev.inventorio.player.InventorioScreenHandler
import de.rubixdev.inventorio.util.id
import net.minecraft.client.gui.screens.MenuScreens
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Registry
import net.minecraft.world.flag.FeatureFlags
import net.minecraft.world.inventory.MenuType

object ScreenTypeProviderFabric : ScreenTypeProvider {
    private val handlerProvider = Registry.register(
        BuiltInRegistries.MENU,
        "player_screen".id,
        MenuType({ syncId, inv -> InventorioScreenHandler(syncId, inv) }, FeatureFlags.VANILLA_SET),
    )

    override fun getScreenHandlerType(): MenuType<InventorioScreenHandler> {
        return handlerProvider
    }

    fun registerScreen() {
        MenuScreens.register(getScreenHandlerType()) { handler, inventory, _ -> InventorioScreen(handler, inventory) }
    }
}
