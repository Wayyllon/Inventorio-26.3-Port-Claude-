package de.rubixdev.inventorio.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import de.rubixdev.inventorio.player.PlayerInventoryAddon;
import de.rubixdev.inventorio.util.MixinHelpers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.TagKey;
import java.util.function.Predicate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Inventory.class, priority = -5000)
public abstract class PlayerInventoryMixin {
    @Shadow
    @Final
    public Player player;

    /**
     * This mixin also copies inventory addon when the main inventory is copied
     * (e.g. when playing with `/gamerule keepInventory true` or going from the
     * End to the Overworld)
     */
    @Inject(method = "replaceWith", at = @At(value = "RETURN"))
    private void inventorioClonePlayerInventory(Inventory sourceInventory, CallbackInfo ci) {
        MixinHelpers.withInventoryAddon(
            player,
            thisAddon -> MixinHelpers.withInventoryAddon(sourceInventory.player, thisAddon::cloneFrom)
        );
    }

    /**
     * 26.3: {@code getSelected} was renamed to {@code getSelectedItem}. The
     * block breaking speed hook (formerly on {@code Inventory.getDestroySpeed},
     * which no longer exists) moved to {@link PlayerEntityMixin}.
     */
    @Inject(method = "getSelectedItem", at = @At(value = "RETURN"), cancellable = true)
    private void inventorioGetMainHandStack(CallbackInfoReturnable<ItemStack> cir) {
        MixinHelpers.withInventoryAddon(player, inventorioAddon -> {
            ItemStack mainHandStack = inventorioAddon.getDisplayedMainHandStack();
            if (mainHandStack != null) cir.setReturnValue(mainHandStack);
        });
    }

    /**
     * Here's how it works: when an item is getting inserted into a player's
     * inventory (e.g. by picking up an item from the ground), it will find a
     * similar incomplete stack in the Deep Pockets and Utility Belt first. If
     * none is present, it will go through vanilla logic, and if the Main
     * Inventory is full, it will find a free slot in the Deep Pockets.
     */
    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At(value = "HEAD"), cancellable = true)
    private void inventorioInsertSimilarStackIntoAddon(
        int slot,
        ItemStack originalStack,
        CallbackInfoReturnable<Boolean> cir
    ) {
        MixinHelpers.withInventoryAddon(player, inventorioAddon -> {
            if (slot == -1 && inventorioAddon != null && inventorioAddon.insertOnlySimilarStack(originalStack))
                cir.setReturnValue(true);
        });
    }

    @ModifyReturnValue(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"))
    private boolean inventorioInsertStackIntoAddon(boolean original, int slot, ItemStack originalStack) {
        return original
            || Boolean.TRUE.equals(
                MixinHelpers.withInventoryAddonReturning(
                    player,
                    addon -> (slot == -1 && addon.insertStackIntoEmptySlot(originalStack))
                )
            );
    }

    @Inject(method = "removeItem(Lnet/minecraft/world/item/ItemStack;)V", at = @At(value = "HEAD"), cancellable = true)
    private void inventorioRemoveOneFromAddon(ItemStack stack, CallbackInfo ci) {
        MixinHelpers.withInventoryAddon(player, inventorioAddon -> {
            if (inventorioAddon != null && inventorioAddon.removeOne(stack)) ci.cancel();
        });
    }

    @Inject(method = "dropAll", at = @At(value = "RETURN"))
    private void inventorioDropAllFromAddon(CallbackInfo ci) {
        MixinHelpers.withInventoryAddon(player, PlayerInventoryAddon::dropAll);
    }

    @Inject(method = "clearContent", at = @At(value = "RETURN"))
    private void inventorioClearAddon(CallbackInfo ci) {
        MixinHelpers.withInventoryAddon(player, SimpleContainer::clearContent);
    }

    // 26.3: Inventory.swapPaint was removed; the hotbar scrolling hook moved to
    // client.MouseHandlerMixin

    @ModifyReturnValue(method = "contains(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"))
    private boolean searchAddon(boolean original, ItemStack stack) {
        return original
            || Boolean.TRUE.equals(MixinHelpers.withInventoryAddonReturning(player, addon -> addon.contains(stack)));
    }

    @ModifyReturnValue(method = "contains(Lnet/minecraft/tags/TagKey;)Z", at = @At("RETURN"))
    private boolean searchAddon(boolean original, TagKey<Item> tag) {
        return original
            || Boolean.TRUE.equals(MixinHelpers.withInventoryAddonReturning(player, addon -> addon.contains(tag)));
    }

    /**
     * 26.3: vanilla now also searches the inventory with a predicate (e.g.
     * {@code MapItemSavedData} to keep the player marker of held maps), so the
     * addon is searched there as well, like the other {@code contains} overloads.
     */
    @ModifyReturnValue(method = "contains(Ljava/util/function/Predicate;)Z", at = @At("RETURN"))
    private boolean searchAddon(boolean original, Predicate<ItemStack> predicate) {
        return original
            || Boolean.TRUE.equals(MixinHelpers.withInventoryAddonReturning(player, addon -> addon.contains(predicate)));
    }
}
