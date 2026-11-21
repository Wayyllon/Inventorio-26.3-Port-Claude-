package de.rubixdev.inventorio.packet

import com.google.gson.Gson
import com.google.gson.JsonObject
import de.rubixdev.inventorio.config.GlobalSettings
import de.rubixdev.inventorio.util.id
import io.netty.buffer.ByteBuf
import java.util.concurrent.Executor
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

data class GlobalSettingsS2CPacket(val settings: JsonObject = GlobalSettings.asJson()) : CustomPacketPayload {
    companion object {
        val ID = CustomPacketPayload.Type<GlobalSettingsS2CPacket>("global_settings".id)
        val CODEC: StreamCodec<ByteBuf, GlobalSettingsS2CPacket> = ByteBufCodecs.STRING_UTF8.map(
            { jsonString: String -> GlobalSettingsS2CPacket(Gson().fromJson(jsonString, JsonObject::class.java)) },
            { packet: GlobalSettingsS2CPacket -> packet.settings.toString() },
        )
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = ID

    fun consume(executor: Executor) {
        executor.execute { GlobalSettings.syncFromServer(settings) }
    }
}
