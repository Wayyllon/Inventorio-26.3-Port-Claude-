package de.rubixdev.inventorio.mixin.client;

import de.rubixdev.inventorio.client.control.InventorioControls;
import de.rubixdev.inventorio.player.PlayerInventoryAddon;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(MultiPlayerGameMode.class)
public class ClientPlayerInteractionManagerMixin {
    /**
     * The default implementation stops an item from being used if the vanilla
     * "use" key isn't pressed. This mod adds an optional secondary use key, and
     * this prevents the secondary item usage from being cancelled
     */
    @Inject(method = "releaseUsingItem", at = @At(value = "HEAD"), cancellable = true)
    private void inventorioStopUsingItem(Player player, CallbackInfo ci) {
        if (PlayerInventoryAddon.Client.isUsingUtility) {
            if (InventorioControls.keyUseUtility.isDown()) ci.cancel();
            else PlayerInventoryAddon.Client.isUsingUtility = false;
        }
    }
}
