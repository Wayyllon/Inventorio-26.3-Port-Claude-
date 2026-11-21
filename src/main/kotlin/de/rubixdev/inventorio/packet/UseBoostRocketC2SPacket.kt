package de.rubixdev.inventorio.packet

import de.rubixdev.inventorio.player.PlayerInventoryAddon.Companion.inventoryAddon
import de.rubixdev.inventorio.util.id
import io.netty.buffer.ByteBuf
import java.util.concurrent.Executor
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer

object UseBoostRocketC2SPacket : CustomPacketPayload {
    val ID = CustomPacketPayload.Type<UseBoostRocketC2SPacket>("fire_boost_rocket_c2s".id)
    val CODEC: StreamCodec<ByteBuf, UseBoostRocketC2SPacket> = StreamCodec.unit(UseBoostRocketC2SPacket)

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = ID

    fun consume(payload: UseBoostRocketC2SPacket, executor: Executor, player: ServerPlayer) {
        executor.execute { player.inventoryAddon?.fireRocketFromInventory() }
    }
}
