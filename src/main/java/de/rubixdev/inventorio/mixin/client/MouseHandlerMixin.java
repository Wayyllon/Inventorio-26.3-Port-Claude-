package de.rubixdev.inventorio.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.rubixdev.inventorio.client.control.InventorioKeyHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 26.3: {@code Inventory.swapPaint} (previously hooked in PlayerInventoryMixin)
 * no longer exists; the hotbar scrolling is done directly in
 * {@code MouseHandler.onScroll}, so it is hooked here instead.
 */
@Environment(EnvType.CLIENT)
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    /**
     * If player has a "scroll utility belt with a mouse wheel" setting enabled,
     * hijack the vanilla hotbar scrolling and scroll the utility belt instead
     * (the selected hotbar slot is left unchanged). Otherwise, set the selected
     * hotbar segment value to -1 in case a player scrolls a mouse wheel while
     * using Segmented Hotbar
     */
    @WrapOperation(
        method = "onScroll",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/ScrollWheelHandler;getNextScrollWheelSelection(DII)I"
        )
    )
    private int inventorioScrollInHotbar(double wheel, int currentSelected, int limit, Operation<Integer> original) {
        if (minecraft.player != null && InventorioKeyHandler.INSTANCE.scrollInHotbar(minecraft.player, wheel)) {
            return currentSelected;
        }
        return original.call(wheel, currentSelected, limit);
    }
}
