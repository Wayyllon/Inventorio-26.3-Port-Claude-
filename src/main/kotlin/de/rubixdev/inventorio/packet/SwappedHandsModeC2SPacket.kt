package de.rubixdev.inventorio.packet

import de.rubixdev.inventorio.player.PlayerInventoryAddon.Companion.inventoryAddon
import de.rubixdev.inventorio.util.id
import io.netty.buffer.ByteBuf
import java.util.concurrent.Executor
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer

data class SwappedHandsModeC2SPacket(val swappedHands: Boolean = false) : CustomPacketPayload {
    companion object {
        val ID = CustomPacketPayload.Type<SwappedHandsModeC2SPacket>("swapped_hands".id)
        val CODEC: StreamCodec<ByteBuf, SwappedHandsModeC2SPacket> = ByteBufCodecs.BOOL.map(::SwappedHandsModeC2SPacket) { it.swappedHands }
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = ID

    fun consume(executor: Executor, player: ServerPlayer) {
        executor.execute { player.inventoryAddon?.swappedHands = swappedHands }
    }
}
