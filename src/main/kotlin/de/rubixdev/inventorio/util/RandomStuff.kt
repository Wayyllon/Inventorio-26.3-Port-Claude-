@file:JvmName("RandomStuff")

package de.rubixdev.inventorio.util

import de.rubixdev.inventorio.config.PlayerSettings
import de.rubixdev.inventorio.mixin.accessor.ScreenHandlerAccessor
import de.rubixdev.inventorio.player.PlayerInventoryAddon
import java.util.function.Consumer
import java.util.function.Supplier
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TridentItem
import net.minecraft.core.RegistryAccess
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.resources.Identifier
import org.apache.logging.log4j.LogManager

import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.entity.Entity
import net.minecraft.resources.ResourceKey
import net.minecraft.core.registries.Registries
import net.minecraft.core.HolderLookup
import net.minecraft.core.Holder

data class Point2I(@JvmField val x: Int, @JvmField val y: Int)
data class Point2F(@JvmField val x: Float, @JvmField val y: Float)
data class Rectangle(@JvmField val x: Int, @JvmField val y: Int, @JvmField val width: Int, @JvmField val height: Int)

enum class SegmentedHotbar {
    OFF,
    ONLY_VISUAL,
    ON,
    ONLY_FUNCTION,
}

enum class ScrollWheelUtilityBeltMode {
    OFF,
    REGULAR,
    REVERSE,
}

enum class ToolBeltMode {
    ENABLED,
    NO_VANILLA_SLOTS_ONLY,
    DISABLED,
}

interface PlayerDuck {
    @Suppress("INAPPLICABLE_JVM_NAME") // see https://stackoverflow.com/questions/47504279/java-interop-apply-jvmname-to-getters-of-properties-in-interface-or-abstract-c
    @get:JvmName("inventorio${'$'}getInventorioAddon")
    val inventorioAddon: PlayerInventoryAddon?
}

val logger = LogManager.getLogger("Inventorio")!!

val ItemStack.isNotEmpty
    get() = !this.isEmpty

fun canRMBItem(itemStack: ItemStack, registries: RegistryAccess): Boolean {
    return PlayerSettings.canThrowUnloyalTrident.boolValue
        || itemStack.item !is TridentItem
        || EnchantmentHelper.getItemEnchantmentLevel(registries.getEnchantment(Enchantments.LOYALTY), itemStack) > 0
        || EnchantmentHelper.getItemEnchantmentLevel(registries.getEnchantment(Enchantments.RIPTIDE), itemStack) > 0
}

fun Holder<Enchantment>.getLevelOn(stack: ItemStack): Int =
    EnchantmentHelper.getItemEnchantmentLevel(this, stack)

fun <E> MutableList<E>.subList(indices: IntRange) = subList(indices.first, indices.last + 1)

fun AbstractContainerMenu.insertItem(stack: ItemStack, indices: IntRange, fromLast: Boolean = false): Boolean =
    (this as ScreenHandlerAccessor).callInsertItem(stack, indices.first, indices.last + 1, fromLast)

class MixinDelegate<T>(
    private val getter: Supplier<T>,
    private val setter: Consumer<T>,
) : ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = getter.get()
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = setter.accept(value)
}

/** Turn this String into an [Identifier] in this mod's namespace */
val String.id get() = Identifier.fromNamespaceAndPath(MOD_ID, this)

/** Turn this String into an [Identifier] in the vanilla namespace */
fun String.id() = Identifier.withDefaultNamespace(this)

/** Turn this String into an [Identifier] in the given namespace */
fun String.id(namespace: String) = Identifier.fromNamespaceAndPath(namespace, this)

fun RegistryAccess.getEnchantment(key: ResourceKey<Enchantment>): Holder.Reference<Enchantment> =
    lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key)

fun HolderLookup.Provider.getEnchantment(key: ResourceKey<Enchantment>): Holder.Reference<Enchantment> =
    lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key)

fun Entity.getEnchantment(key: ResourceKey<Enchantment>): Holder.Reference<Enchantment> =
    registryAccess().getEnchantment(key)
