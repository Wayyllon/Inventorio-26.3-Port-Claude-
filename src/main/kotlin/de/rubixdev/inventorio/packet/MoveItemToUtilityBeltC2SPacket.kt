package de.rubixdev.inventorio.packet

import de.rubixdev.inventorio.player.InventorioScreenHandler.Companion.inventorioScreenHandler
import de.rubixdev.inventorio.util.id
import io.netty.buffer.ByteBuf
import java.util.concurrent.Executor
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer

data class MoveItemToUtilityBeltC2SPacket(val sourceSlot: Int = 0) : CustomPacketPayload {
    companion object {
        val ID = CustomPacketPayload.Type<MoveItemToUtilityBeltC2SPacket>("move_to_utility_c2s".id)
        val CODEC: StreamCodec<ByteBuf, MoveItemToUtilityBeltC2SPacket> = ByteBufCodecs.VAR_INT.xmap(::MoveItemToUtilityBeltC2SPacket) { it.sourceSlot }
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = ID

    fun consume(executor: Executor, player: ServerPlayer) {
        executor.execute {
            val screenHandler = player.inventorioScreenHandler ?: return@execute
            screenHandler.tryTransferToUtilityBeltSlot(screenHandler.getSlot(sourceSlot))
        }
    }
}
