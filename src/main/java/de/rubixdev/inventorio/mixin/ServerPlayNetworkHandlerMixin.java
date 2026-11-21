package de.rubixdev.inventorio.mixin;

import de.rubixdev.inventorio.util.MixinHelpers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerPlayNetworkHandlerMixin {
    @Shadow
    public ServerPlayer player;

    /**
     * These two injections remove the display tool (server-side) when a player
     * tries to use (right-click) something from the main hand, so that the item
     * that's ACTUALLY in the main hand can be used immediately
     */
    @Inject(method = "handleUseItemOn", at = @At(value = "HEAD"))
    private void inventorioRemoveDisplayHand(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
        if (packet.hand() == InteractionHand.MAIN_HAND)
            MixinHelpers.withInventoryAddon(player, addon -> addon.setDisplayTool(ItemStack.EMPTY));
    }

    @Inject(method = "handleUseItem", at = @At(value = "HEAD"))
    private void inventorioRemoveDisplayHand(ServerboundUseItemPacket packet, CallbackInfo ci) {
        if (packet.hand() == InteractionHand.MAIN_HAND)
            MixinHelpers.withInventoryAddon(player, addon -> addon.setDisplayTool(ItemStack.EMPTY));
    }
}
