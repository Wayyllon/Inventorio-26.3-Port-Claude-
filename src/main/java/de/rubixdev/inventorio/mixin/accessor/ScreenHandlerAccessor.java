package de.rubixdev.inventorio.mixin.accessor;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.core.NonNullList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@SuppressWarnings("unused")
@Mixin(AbstractContainerMenu.class)
public interface ScreenHandlerAccessor {
    @Accessor("lastSlots")
    NonNullList<ItemStack> getTrackedStacks();

    @Accessor("remoteSlots")
    NonNullList<ItemStack> getPreviousTrackedStacks();

    @Invoker("addSlot")
    Slot callAddSlot(Slot slot);

    @Invoker("moveItemStackTo")
    boolean callInsertItem(ItemStack stack, int firstIndex, int lastIndex, boolean fromLast);
}
