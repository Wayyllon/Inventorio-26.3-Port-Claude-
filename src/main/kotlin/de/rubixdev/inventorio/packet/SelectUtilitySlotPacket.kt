package de.rubixdev.inventorio.packet

import com.mojang.datafixers.util.Pair
import de.rubixdev.inventorio.player.PlayerInventoryAddon
import de.rubixdev.inventorio.player.PlayerInventoryAddon.Companion.inventoryAddon
import de.rubixdev.inventorio.util.id
import io.netty.buffer.ByteBuf
import java.util.concurrent.Executor
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.level.ServerLevel

data class SelectUtilitySlotPacket(val slot: Byte = 0) : CustomPacketPayload {
    companion object {
        val ID = CustomPacketPayload.Type<SelectUtilitySlotPacket>("select_utility".id)
        val CODEC: StreamCodec<ByteBuf, SelectUtilitySlotPacket> =
            ByteBufCodecs.BYTE.xmap(::SelectUtilitySlotPacket) { it.slot }
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = ID

    // Server's receiving consumer
    fun consume(executor: Executor, player: ServerPlayer) {
        executor.execute {
            player.inventoryAddon?.selectedUtility = slot.toInt()

            // Resending the current offhand item (aka a selected utility belt item) of this player to other players
            val broadcastPacket =
                ClientboundSetEquipmentPacket(
                    player.id,
                    listOf(Pair(EquipmentSlot.OFFHAND, player.offhandItem)),
                )
            (player.level() as ServerLevel).chunkSource.broadcast(player, broadcastPacket)
        }
    }

    // Client's receiving consumer
    fun consume(executor: Executor) {
        executor.execute { PlayerInventoryAddon.Client.local?.selectedUtility = slot.toInt() }
    }
}
