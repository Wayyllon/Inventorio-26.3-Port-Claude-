package de.rubixdev.inventorio.mixin.accessor;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

@Mixin(CraftingMenu.class)
public interface CraftingScreenHandlerAccessor {
    @Invoker("slotChangedCraftingGrid")
    static void updateTheResult(
        AbstractContainerMenu handler,
        ServerLevel world,
        Player player,
        CraftingContainer craftingInventory,
        ResultContainer resultInventory
        , @Nullable RecipeHolder<CraftingRecipe> recipe
    ) {}
}
