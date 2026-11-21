package de.rubixdev.inventorio.client.control

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.client.KeyMapping
import com.mojang.blaze3d.platform.InputConstants

@Environment(EnvType.CLIENT)
object InventorioControls {
    @JvmField val keyUseUtility = KeyCoBinding(
        "inventorio.keys.combined.with_use_item",
        { Minecraft.getInstance()?.options?.useKey },
        "inventorio.keys.use_utility",
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.value,
        "inventorio.keys.category",
    )

    @JvmField val keyNextUtility = KeyMapping(
        "inventorio.keys.next_utility",
        InputConstants.Type.MOUSE,
        InputConstants.MOUSE_BUTTON_4,
        "inventorio.keys.category",
    )

    @JvmField val keyPrevUtility = KeyMapping(
        "inventorio.keys.prev_utility",
        InputConstants.Type.MOUSE,
        InputConstants.MOUSE_BUTTON_5,
        "inventorio.keys.category",
    )

    @JvmField val keyEmptyUtility = KeyMapping(
        "inventorio.keys.empty_utility",
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.value,
        "inventorio.keys.category",
    )

    @JvmField val keyFireBoostRocket = KeyCoBinding(
        "inventorio.keys.combined.with_jump",
        { Minecraft.getInstance()?.options?.jumpKey },
        "inventorio.keys.rocket_boost",
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.value,
        "inventorio.keys.category",
    )

    @JvmField val keyOpenPlayerSettingsMenu = KeyMapping(
        "inventorio.keys.open_player_settings",
        InputConstants.Type.KEYSYM,
        InputConstants.KEY_I,
        "inventorio.keys.category",
    )

    @JvmField val keyOpenGlobalSettingsMenu = KeyMapping(
        "inventorio.keys.open_global_settings",
        InputConstants.Type.KEYSYM,
        InputConstants.KEY_O,
        "inventorio.keys.category",
    )

    @JvmField val keys = arrayOf(
        keyUseUtility,
        keyNextUtility,
        keyPrevUtility,
        keyEmptyUtility,
        keyFireBoostRocket,

        keyOpenPlayerSettingsMenu,
        keyOpenGlobalSettingsMenu,
    )
}
