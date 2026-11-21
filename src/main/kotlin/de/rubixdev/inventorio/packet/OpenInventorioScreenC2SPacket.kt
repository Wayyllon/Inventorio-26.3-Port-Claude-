package de.rubixdev.inventorio.packet

import de.rubixdev.inventorio.player.InventorioScreenHandler
import de.rubixdev.inventorio.util.id
import io.netty.buffer.ByteBuf
import java.util.concurrent.Executor
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer

object OpenInventorioScreenC2SPacket : CustomPacketPayload {
    val ID = CustomPacketPayload.Type<OpenInventorioScreenC2SPacket>("open_screen".id)
    val CODEC: StreamCodec<ByteBuf, OpenInventorioScreenC2SPacket> = StreamCodec.unit(OpenInventorioScreenC2SPacket)

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = ID

    fun consume(payload: OpenInventorioScreenC2SPacket, executor: Executor, player: ServerPlayer) {
        executor.execute { InventorioScreenHandler.open(player) }
    }
}
