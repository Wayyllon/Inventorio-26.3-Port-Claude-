package de.rubixdev.inventorio.enchantment

import de.rubixdev.inventorio.config.GlobalSettings
import net.minecraft.world.item.enchantment.EnchantmentInstance
import net.minecraft.world.item.EnchantedBookItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.CustomRecipe
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.world.level.Level

import de.rubixdev.inventorio.InventorioResources
import de.rubixdev.inventorio.util.getEnchantment
import net.minecraft.world.item.crafting.CraftingInput

class DeepPocketsBookRecipe(category: CraftingBookCategory) : CustomRecipe(category) {
    override fun matches(input: CraftingInput, world: Level): Boolean {
        if (!GlobalSettings.deepPocketsBookCraft.boolValue) {
            return false
        }
        var shells = 0
        var books = 0

        for (i in 0..<input.size) {
            val itemStack = input.getItem(i)
            if (SHULKER_SHELL.test(itemStack)) {
                shells++
            }
            if (BOOKS.test(itemStack)) {
                books++
            }
        }
        return shells == 2 && books == 1
    }

    override fun assemble(input: CraftingInput, lookup: HolderLookup.Provider): ItemStack =
        EnchantedBookItem.createForEnchantment(EnchantmentInstance(lookup.getEnchantment(InventorioResources.DEEP_POCKETS), 1))

    override fun canCraftInDimensions(width: Int, height: Int): Boolean {
        return width >= 2 && height >= 2
    }

    override fun getSerializer(): RecipeSerializer<*> {
        return SERIALIZER
    }

    override fun isSpecial(): Boolean {
        return !GlobalSettings.deepPocketsBookCraft.boolValue
    }

    override fun getIngredients(): NonNullList<Ingredient> {
        return NonNullList.of(SHULKER_SHELL, SHULKER_SHELL, BOOKS, SHULKER_SHELL)
    }

    override fun getResultItem(lookup: HolderLookup.Provider): ItemStack = when (GlobalSettings.deepPocketsBookCraft.boolValue) {
        true -> EnchantedBookItem.createForEnchantment(EnchantmentInstance(lookup.getEnchantment(InventorioResources.DEEP_POCKETS), 1))
        false -> ItemStack.EMPTY
    }

    companion object {
        private val SHULKER_SHELL = Ingredient.of(Items.SHULKER_SHELL)
        private val BOOKS = Ingredient.of(Items.BOOK, Items.WRITABLE_BOOK)

        @Suppress("ktlint:standard:property-naming")
        lateinit var SERIALIZER: SimpleCraftingRecipeSerializer<DeepPocketsBookRecipe>
    }
}
