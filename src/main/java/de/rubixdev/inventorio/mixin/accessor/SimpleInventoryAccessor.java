package de.rubixdev.inventorio.mixin.accessor;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SimpleContainer.class)
public interface SimpleInventoryAccessor {
    @Accessor("size")
    @Mutable
    void setSize(int size);

    @Accessor("items")
    @Mutable
    void setHeldStacks(NonNullList<ItemStack> stacks);

    @Accessor("items")
    NonNullList<ItemStack> getItems();
}
