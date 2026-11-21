package de.rubixdev.inventorio

import de.rubixdev.inventorio.util.id
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.enchantment.Enchantment

/**
 * TODO(26.3): The original implementation generated the Deep Pockets enchantment, its tags, the
 *  Deep Pockets book recipe and its unlock advancement at runtime via YARRP
 *  (https://github.com/RubixDev/YARRP). YARRP has no build for Minecraft 26.x (latest is v0.2.0 for
 *  1.21.1), so the runtime pack is disabled for now. The original (remapped to Mojang names) code is in
 *  `disabled/src/main/kotlin/de/rubixdev/inventorio/InventorioResources.kt.yarrp`.
 *  Options: port YARRP, use Fabric's datagen / a static datapack, or Fabric API dynamic registries.
 */
object InventorioResources {
    fun register() {
        // TODO(26.3): register runtime data pack (see class doc)
    }

    @JvmField
    val DEEP_POCKETS: ResourceKey<Enchantment> = ResourceKey.create(Registries.ENCHANTMENT, "deep_pockets".id)
}
