package de.rubixdev.inventorio.mixin.client;

import de.rubixdev.inventorio.packet.InventorioNetworking;
import de.rubixdev.inventorio.player.PlayerInventoryAddon;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkHandlerMixin {
    /**
     * When a player joins, this mixin sends a packet to the server, telling
     * whether the player uses Swapped Hands or not
     */
    @Inject(method = "handleLogin", at = @At(value = "RETURN"))
    private void inventorioSendSwappedHandsStatus(ClientboundLoginPacket packet, CallbackInfo ci) {
        PlayerInventoryAddon addon = PlayerInventoryAddon.Client.INSTANCE.getLocal();
        if (addon != null) InventorioNetworking.getInstance().c2sSetSwappedHandsMode(addon.getSwappedHands());
    }
}
