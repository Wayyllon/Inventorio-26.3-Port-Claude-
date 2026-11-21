package de.rubixdev.inventorio.packet

import de.rubixdev.inventorio.player.PlayerInventoryAddon
import de.rubixdev.inventorio.util.id
import java.util.concurrent.Executor
import net.minecraft.world.item.ItemStack
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

data class UpdateAddonStacksS2CPacket(val updatedStacks: Map<Int, ItemStack>) : CustomPacketPayload {
    companion object {
        val ID = CustomPacketPayload.Type<UpdateAddonStacksS2CPacket>("update_addon_stacks".id)
        private val MAP_CODEC: StreamCodec<RegistryFriendlyByteBuf, Map<Int, ItemStack>> =
            ByteBufCodecs.map(::LinkedHashMap, ByteBufCodecs.VAR_INT, ItemStack.OPTIONAL_STREAM_CODEC)
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, UpdateAddonStacksS2CPacket> =
            MAP_CODEC.xmap(::UpdateAddonStacksS2CPacket) { it.updatedStacks }
    }

    fun consume(executor: Executor) {
        executor.execute { PlayerInventoryAddon.Client.local?.receiveStacksUpdateS2C(updatedStacks) }
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = ID
}
