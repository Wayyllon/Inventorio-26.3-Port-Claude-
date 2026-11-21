package de.rubixdev.inventorio.util

import de.rubixdev.inventorio.player.InventorioScreenHandler
import de.rubixdev.inventorio.player.InventorioScreenHandler.Companion.inventorioScreenHandler
import de.rubixdev.inventorio.player.PlayerInventoryAddon
import de.rubixdev.inventorio.player.PlayerInventoryAddon.Companion.inventoryAddon
import java.util.function.Consumer
import net.minecraft.world.entity.player.Player

object MixinHelpers {
    @JvmStatic
    @JvmOverloads
    fun <T> withInventoryAddonReturning(
        playerEntity: Player?,
        consumer: (PlayerInventoryAddon) -> T?,
        ifNotPresent: (Player?) -> T? = { null },
    ): T? {
        val addon = playerEntity?.inventoryAddon
        return if (addon != null) {
            consumer(addon)
        } else {
            ifNotPresent(playerEntity)
        }
    }

    @JvmStatic
    @JvmOverloads
    fun withInventoryAddon(
        playerEntity: Player?,
        consumer: Consumer<PlayerInventoryAddon>,
        ifNotPresent: Consumer<Player?> = Consumer {},
    ) {
        withInventoryAddonReturning(playerEntity, { addon -> consumer.accept(addon) }, { player -> ifNotPresent.accept(player) })
    }

    @JvmStatic
    @JvmOverloads
    fun withScreenHandler(
        playerEntity: Player?,
        consumer: Consumer<InventorioScreenHandler>,
        ifNotPresent: Consumer<Player?> = Consumer {},
    ) {
        val addon = playerEntity?.inventorioScreenHandler
        if (addon != null) {
            consumer.accept(addon)
        } else {
            ifNotPresent.accept(playerEntity)
        }
    }
}
