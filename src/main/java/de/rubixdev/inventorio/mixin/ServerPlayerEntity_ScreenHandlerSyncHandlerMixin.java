package de.rubixdev.inventorio.mixin;

import de.rubixdev.inventorio.player.inventory.PlayerInventoryExtension;
import de.rubixdev.inventorio.util.MixinHelpers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.server.level.ServerPlayer;
import java.util.List;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.server.level.ServerPlayer$1")
public class ServerPlayerEntity_ScreenHandlerSyncHandlerMixin {
    @Shadow
    @Final
    ServerPlayer this$0;

    @Inject(method = "sendInitialData", at = @At("HEAD"))
    private void updateAddonState(
        AbstractContainerMenu handler,
        List<ItemStack> stacks,
        ItemStack cursorStack,
        int[] properties,
        CallbackInfo ci
    ) {
        MixinHelpers.withInventoryAddon(this.this$0, PlayerInventoryExtension::updateState);
    }
}
