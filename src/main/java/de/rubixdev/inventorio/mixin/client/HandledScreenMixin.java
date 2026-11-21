package de.rubixdev.inventorio.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.rubixdev.inventorio.util.MixinHelpers;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerInput;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(value = AbstractContainerScreen.class)
@Environment(EnvType.CLIENT)
public class HandledScreenMixin {
    @Shadow
    @Nullable protected Slot hoveredSlot;

    /**
     * This injection allows to use the vanilla Swap Offhand key (F by default)
     * to move items to the Utility Belt in the inventory (the injected method
     * is an edge-case handler when you have "swap offhand" on mouse buttons)
     */
    @WrapOperation(
        method = "checkHotbarMouseClicked(I)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V",
            ordinal = 0
        ),
        slice = @Slice(
            from = @At(
                value = "FIELD",
                target = "Lnet/minecraft/client/Options;keySwapOffhand:Lnet/minecraft/client/KeyMapping;"
            )
        )
    )
    private void inventorioOffhandSwapWithMouse(
        AbstractContainerScreen<?> instance,
        Slot slot,
        int slotId,
        int button,
        ContainerInput actionType,
        Operation<Void> original
    ) {
        MixinHelpers.withScreenHandler(
            Minecraft.getInstance().player,
            screenHandler -> screenHandler.tryTransferToUtilityBeltSlot(hoveredSlot),
            player -> original.call(instance, slot, slotId, button, actionType, original)
        );
    }

    /**
     * This injection allows to use the vanilla Swap Offhand key (F by default)
     * to move items to the Utility Belt in the inventory
     */
    @WrapOperation(
        method = "checkHotbarKeyPressed",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V",
            ordinal = 0
        ),
        slice = @Slice(
            from = @At(
                value = "FIELD",
                target = "Lnet/minecraft/client/Options;keySwapOffhand:Lnet/minecraft/client/KeyMapping;"
            )
        )
    )
    private void inventorioOffhandSwapWithKeyboard(
        AbstractContainerScreen<?> instance,
        Slot slot,
        int slotId,
        int button,
        ContainerInput actionType,
        Operation<Void> original
    ) {
        MixinHelpers.withScreenHandler(
            Minecraft.getInstance().player,
            screenHandler -> screenHandler.tryTransferToUtilityBeltSlot(hoveredSlot),
            player -> original.call(instance, slot, slotId, button, actionType)
        );
    }
}
