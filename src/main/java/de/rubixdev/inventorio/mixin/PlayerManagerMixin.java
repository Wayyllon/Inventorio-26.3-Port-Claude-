package de.rubixdev.inventorio.mixin;

import de.rubixdev.inventorio.packet.InventorioNetworking;
import de.rubixdev.inventorio.util.MixinHelpers;
import net.minecraft.network.Connection;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.Entity;

@Mixin(PlayerList.class)
public class PlayerManagerMixin {
    /**
     * This injection sends the last utility slot, saved by the server, from the
     * server to the client
     */
    @Inject(method = "placeNewPlayer", at = @At(value = "RETURN"), require = 0)
    private void inventorioSetPlayerSettings(
        Connection connection,
        ServerPlayer player,
        CommonListenerCookie clientData,
        CallbackInfo ci
    ) {
        InventorioNetworking.getInstance().s2cSelectUtilitySlot(player);
        InventorioNetworking.getInstance().s2cGlobalSettings(player);
    }

    /**
     * This inject sends the last utility slot, saved by the server, from the
     * server to the client
     */
    @Inject(method = "respawn", at = @At(value = "RETURN"), require = 0)
    private void inventorioSetPlayerSettings(/* #if <- hack around formatter */
        ServerPlayer oldPlayer,
        boolean alive,
        Entity.RemovalReason removalReason,
        CallbackInfoReturnable<ServerPlayer> cir
    ) {
        ServerPlayer newPlayer = cir.getReturnValue();
        MixinHelpers.withInventoryAddon(newPlayer, newAddon -> MixinHelpers.withInventoryAddon(oldPlayer, oldAddon -> {
            newAddon.setSwappedHands(oldAddon.getSwappedHands());
            newAddon.setSelectedUtility(oldAddon.getSelectedUtility());
        }));
        InventorioNetworking.getInstance().s2cSelectUtilitySlot(newPlayer);
    }

    @Inject(method = "sendAllPlayerInfo", at = @At("RETURN"))
    private void sendSelectedUtility(ServerPlayer player, CallbackInfo ci) {
        InventorioNetworking.getInstance().s2cSelectUtilitySlot(player);
    }
}
