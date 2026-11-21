package de.rubixdev.inventorio.packet

import de.rubixdev.inventorio.player.PlayerInventoryAddon.Companion.inventoryAddon
import de.rubixdev.inventorio.util.id
import io.netty.buffer.ByteBuf
import java.util.concurrent.Executor
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer

object SwapItemsInHandsKeyC2SPacket : CustomPacketPayload {
    val ID = CustomPacketPayload.Type<SwapItemsInHandsKeyC2SPacket>("swap_items_in_hands".id)
    val CODEC: StreamCodec<ByteBuf, SwapItemsInHandsKeyC2SPacket> = StreamCodec.unit(SwapItemsInHandsKeyC2SPacket)

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = ID

    fun consume(payload: SwapItemsInHandsKeyC2SPacket, executor: Executor, player: ServerPlayer) {
        executor.execute { player.inventoryAddon?.swapItemsInHands() }
    }
}
