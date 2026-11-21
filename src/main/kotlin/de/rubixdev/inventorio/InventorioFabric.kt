package de.rubixdev.inventorio

import de.rubixdev.inventorio.api.InventorioAPI
import de.rubixdev.inventorio.client.control.InventorioControls
import de.rubixdev.inventorio.client.control.InventorioKeyHandler
import de.rubixdev.inventorio.config.PlayerSettings
import de.rubixdev.inventorio.integration.InventorioModIntegration
import de.rubixdev.inventorio.integration.ModIntegration
import de.rubixdev.inventorio.packet.InventorioNetworking
import de.rubixdev.inventorio.packet.InventorioNetworkingFabric
import de.rubixdev.inventorio.util.id
import net.fabricmc.api.EnvType
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.fabricmc.loader.api.FabricLoader


open class InventorioFabric : ModInitializer {
    private val fabricModIntegrations = listOf<ModIntegration>(
        // TODO(26.3): ClumpsIntegration disabled (see disabled/README.md)
    )

    override fun onInitialize() {
        ScreenTypeProvider.INSTANCE = ScreenTypeProviderFabric
        InventorioNetworking.INSTANCE = InventorioNetworkingFabric
        // Deep Pockets book recipe is now a data-driven shapeless recipe (see InventorioResources)
        InventorioResources.register()

        initToolBelt()

        if (FabricLoader.getInstance().environmentType == EnvType.CLIENT) {
            ClientTickEvents.START_CLIENT_TICK.register(ClientTickEvents.StartTick { InventorioKeyHandler.tick() })
            InventorioControls.keys.forEach { KeyMappingHelper.registerKeyMapping(it) }
            PlayerSettings.load(FabricLoader.getInstance().configDir.resolve("inventorio.json").toFile())
            ScreenTypeProviderFabric.registerScreen()
        }

        InventorioModIntegration.applyModIntegrations(fabricModIntegrations)
    }

    private fun initToolBelt() {
        // What this actually does is loads the [InventorioAPI] which creates the ToolBelt
        // The reason why we do it this way is that we can't guarantee that other mods
        // won't call [InventorioAPI] BEFORE [InventorioFabric#onInitialize] has been invoked
        InventorioAPI.getToolBeltSlotTemplate(InventorioAPI.SLOT_PICKAXE)
            ?.addAllowingTag("pickaxes".id("fabric"))
            ?.addAllowingTag("hammers".id("fabric"))

        InventorioAPI.getToolBeltSlotTemplate(InventorioAPI.SLOT_SWORD)
            ?.addAllowingTag("swords".id("fabric"))
            ?.addAllowingTag("tridents".id("fabric"))
            ?.addAllowingTag("battleaxes".id("fabric"))

        InventorioAPI.getToolBeltSlotTemplate(InventorioAPI.SLOT_AXE)
            ?.addAllowingTag("axes".id("fabric"))
            ?.addAllowingTag("battleaxes".id("fabric"))

        InventorioAPI.getToolBeltSlotTemplate(InventorioAPI.SLOT_SHOVEL)
            ?.addAllowingTag("shovels".id("fabric"))
            ?.addAllowingTag("mattocks".id("fabric"))

        InventorioAPI.getToolBeltSlotTemplate(InventorioAPI.SLOT_HOE)
            ?.addAllowingTag("hoes".id("fabric"))
            ?.addAllowingTag("shears".id("fabric"))
    }
}
