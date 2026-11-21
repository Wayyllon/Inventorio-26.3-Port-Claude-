package de.rubixdev.inventorio.packet

import java.util.concurrent.Executor
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer

fun <T : CustomPacketPayload> consumeClient(consumer: T.(Executor) -> Unit) =
    { payload: T, ctx: ClientPlayNetworking.Context ->
        payload.consumer(ctx.client())
    }

fun <T : CustomPacketPayload> consumeServer(consumer: T.(Executor, ServerPlayer) -> Unit) =
    { payload: T, ctx: ServerPlayNetworking.Context ->
        payload.consumer(ctx.server(), ctx.player())
    }

object InventorioNetworkingFabric : InventorioNetworking {
    init {
        PayloadTypeRegistry.clientboundPlay().register(SelectUtilitySlotPacket.ID, SelectUtilitySlotPacket.CODEC)
        PayloadTypeRegistry.clientboundPlay().register(GlobalSettingsS2CPacket.ID, GlobalSettingsS2CPacket.CODEC)
        PayloadTypeRegistry.clientboundPlay().register(UpdateAddonStacksS2CPacket.ID, UpdateAddonStacksS2CPacket.CODEC)

        PayloadTypeRegistry.serverboundPlay().register(SelectUtilitySlotPacket.ID, SelectUtilitySlotPacket.CODEC)
        PayloadTypeRegistry.serverboundPlay().register(UseBoostRocketC2SPacket.ID, UseBoostRocketC2SPacket.CODEC)
        PayloadTypeRegistry.serverboundPlay().register(SwapItemsInHandsKeyC2SPacket.ID, SwapItemsInHandsKeyC2SPacket.CODEC)
        PayloadTypeRegistry.serverboundPlay().register(SwappedHandsModeC2SPacket.ID, SwappedHandsModeC2SPacket.CODEC)
        PayloadTypeRegistry.serverboundPlay().register(MoveItemToUtilityBeltC2SPacket.ID, MoveItemToUtilityBeltC2SPacket.CODEC)
        PayloadTypeRegistry.serverboundPlay().register(OpenInventorioScreenC2SPacket.ID, OpenInventorioScreenC2SPacket.CODEC)

        if (FabricLoader.getInstance().environmentType == EnvType.CLIENT) {
            ClientPlayNetworking.registerGlobalReceiver(
                SelectUtilitySlotPacket.ID,
                consumeClient(SelectUtilitySlotPacket::consume),
            )
            ClientPlayNetworking.registerGlobalReceiver(
                GlobalSettingsS2CPacket.ID,
                consumeClient(GlobalSettingsS2CPacket::consume),
            )
            ClientPlayNetworking.registerGlobalReceiver(
                UpdateAddonStacksS2CPacket.ID,
                consumeClient(UpdateAddonStacksS2CPacket::consume),
            )
        }

        ServerPlayNetworking.registerGlobalReceiver(
            SelectUtilitySlotPacket.ID,
            consumeServer(SelectUtilitySlotPacket::consume),
        )
        ServerPlayNetworking.registerGlobalReceiver(
            UseBoostRocketC2SPacket.ID,
            consumeServer(UseBoostRocketC2SPacket::consume),
        )
        ServerPlayNetworking.registerGlobalReceiver(
            SwapItemsInHandsKeyC2SPacket.ID,
            consumeServer(SwapItemsInHandsKeyC2SPacket::consume),
        )
        ServerPlayNetworking.registerGlobalReceiver(
            SwappedHandsModeC2SPacket.ID,
            consumeServer(SwappedHandsModeC2SPacket::consume),
        )
        ServerPlayNetworking.registerGlobalReceiver(
            MoveItemToUtilityBeltC2SPacket.ID,
            consumeServer(MoveItemToUtilityBeltC2SPacket::consume),
        )
        ServerPlayNetworking.registerGlobalReceiver(
            OpenInventorioScreenC2SPacket.ID,
            consumeServer(OpenInventorioScreenC2SPacket::consume),
        )
    }

    override fun sendToPlayer(
        player: ServerPlayer,
        packet: CustomPacketPayload,
    ) {
        ServerPlayNetworking.send(player, packet)
    }

    @Environment(EnvType.CLIENT)
    override fun sendToServer(packet: CustomPacketPayload) {
        ClientPlayNetworking.send(packet)
    }
}
