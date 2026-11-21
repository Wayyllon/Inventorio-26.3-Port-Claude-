package de.rubixdev.inventorio.client.control

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.KeyMapping
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.network.chat.Component

/**
 * The usecase: when you bind "Use Item" and "Use Utility" to the same key, the intended behavior is them merging together.
 * It works on NeoForge, but FABRIC allows only one bindings per key.
 * This is the fix: the co-bound binding will unbind itself if binded to the same key.
 */
@Environment(EnvType.CLIENT)
open class KeyCoBinding(protected val coBindTranslationKey: String, protected val vanillaBinging: () -> KeyMapping?, translationKey: String, type: InputConstants.Type, code: Int, category: String) : KeyMapping(translationKey, type, code, category) {
    open val isThisOrVanillaPressed: Boolean
        get() = (isUnbound && vanillaBinging()?.isDown == true) || super.isDown()

    override fun setKey(boundKey: InputConstants.Key) {
        super.setKey(boundKey)
        if (!this.isUnbound && vanillaBinging() != null && this.same(vanillaBinging())) {
            super.setKey(InputConstants.UNKNOWN)
        }
    }

    override fun getTranslatedKeyMessage(): Component {
        return if (isUnbound) {
            Component.translatable(coBindTranslationKey)
        } else {
            super.getTranslatedKeyMessage()
        }
    }
}
