package de.rubixdev.inventorio.client.control

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.client.KeyMapping
import com.mojang.blaze3d.platform.InputConstants
import de.rubixdev.inventorio.util.id

@Environment(EnvType.CLIENT)
object InventorioControls {
    /** 26.3: key categories are registered objects; label key is `key.category.inventorio.inventorio` */
    @JvmField val CATEGORY: KeyMapping.Category = KeyMapping.Category.register("inventorio".id)

    @JvmField val keyUseUtility = KeyCoBinding(
        "inventorio.keys.combined.with_use_item",
        { Minecraft.getInstance()?.options?.keyUse },
        "inventorio.keys.use_utility",
        InputConstants.Type.KEYBOARD,
        InputConstants.UNKNOWN.value,
        CATEGORY,
    )

    @JvmField val keyNextUtility = KeyMapping(
        "inventorio.keys.next_utility",
        InputConstants.Type.MOUSE,
        InputConstants.MOUSE_BUTTON_4,
        CATEGORY,
    )

    @JvmField val keyPrevUtility = KeyMapping(
        "inventorio.keys.prev_utility",
        InputConstants.Type.MOUSE,
        InputConstants.MOUSE_BUTTON_5,
        CATEGORY,
    )

    @JvmField val keyEmptyUtility = KeyMapping(
        "inventorio.keys.empty_utility",
        InputConstants.Type.KEYBOARD,
        InputConstants.UNKNOWN.value,
        CATEGORY,
    )

    @JvmField val keyFireBoostRocket = KeyCoBinding(
        "inventorio.keys.combined.with_jump",
        { Minecraft.getInstance()?.options?.keyJump },
        "inventorio.keys.rocket_boost",
        InputConstants.Type.KEYBOARD,
        InputConstants.UNKNOWN.value,
        CATEGORY,
    )

    @JvmField val keyOpenPlayerSettingsMenu = KeyMapping(
        "inventorio.keys.open_player_settings",
        InputConstants.Type.KEYBOARD,
        InputConstants.KEY_I,
        CATEGORY,
    )

    @JvmField val keyOpenGlobalSettingsMenu = KeyMapping(
        "inventorio.keys.open_global_settings",
        InputConstants.Type.KEYBOARD,
        InputConstants.KEY_O,
        CATEGORY,
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
