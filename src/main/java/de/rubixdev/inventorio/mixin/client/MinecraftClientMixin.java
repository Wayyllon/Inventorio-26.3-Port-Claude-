package de.rubixdev.inventorio.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.rubixdev.inventorio.client.control.InventorioKeyHandler;
import de.rubixdev.inventorio.client.ui.InventorioScreen;
import de.rubixdev.inventorio.packet.InventorioNetworking;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Minecraft.class, priority = 9999)
@Environment(EnvType.CLIENT)
public class MinecraftClientMixin {
    @Shadow
    public LocalPlayer player;

    /**
     * This injection opens the Inventorio Screen instead of the Vanilla Player
     * Screen
     */
    @WrapOperation(
        method = "handleKeybinds",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V",
            ordinal = 0
        ),
        slice = @Slice(
            from = @At(value = "INVOKE", target = "Lnet/minecraft/client/tutorial/Tutorial;onOpenInventory()V")
        )
    )
    private void inventorioOpenReplacingScreen(Gui instance, Screen old, Operation<Void> original) {
        if (
            InventorioScreen.shouldOpenVanillaInventory
                || (player != null && player.hasInfiniteMaterials())
        ) {
            original.call(instance, old);
        } else {
            InventorioNetworking.getInstance().c2sOpenInventorioScreen();
        }
    }

    /**
     * This injection replaces vanilla Hotbar slot selection with ours
     * (Segmented Hotbar)
     */
    @WrapOperation(
        method = "handleKeybinds",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;setSelectedSlot(I)V")
    )
    private void inventorioHandleHotbarSlotSelection(
        Inventory instance,
        int value,
        Operation<Void> original
    ) {
        // 26.3: vanilla calls setSelectedSlot(i) with the hotbar key index i
        if (!InventorioKeyHandler.INSTANCE.handleSegmentedHotbarSlotSelection(instance, value)) {
            original.call(instance, value);
        }
    }

    /**
     * This injection prevents swapping the items between hands while there is a
     * display tool active, because that causes a non-duping glitch when a tool
     * in-use gets pulled off from a tool belt
     */
    @Inject(
        method = "handleKeybinds",
        at = @At(
            value = "NEW",
            target = "(Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket$Action;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket;"
        ),
        cancellable = true
    )
    private void inventorioPreventOffhandSwapForDisplayTool(CallbackInfo ci) {
        ci.cancel();
        InventorioNetworking.getInstance().c2sSwapItemsInHands();
    }

    /**
     * This redirect enables the ability to bind the Offhand/Utility to a
     * separate button.
     * <p>
     * If the option is enabled, a regular right click won't attempt to use an
     * Offhand item, but a dedicated key will attempt to use ONLY an Offhand
     * item.
     */
    @Redirect(
        method = "startUseItem",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/InteractionHand;values()[Lnet/minecraft/world/InteractionHand;")
    )
    private InteractionHand[] inventorioDoItemUse() {
        if (player == null) return new InteractionHand[] {};
        return InventorioKeyHandler.INSTANCE.handleItemUsage(player);
    }

    // 26.3: upstream's `seamlessScreenTransition` (@Inject into Minecraft#setScreen at the PUTFIELD of
    // `screen`) was dropped. Screen handling moved to Gui#setScreen and the upstream condition
    // (`screen == null && screen instanceof ...`) could never be true, so the injection was a no-op.
}
