package de.rubixdev.inventorio

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import de.rubixdev.inventorio.config.GlobalSettings
import de.rubixdev.inventorio.config.SettingsEntryBoolean
import de.rubixdev.inventorio.util.MOD_ID
import de.rubixdev.inventorio.util.id
import de.rubixdev.inventorio.util.logger
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions
import net.fabricmc.fabric.api.resource.v1.ResourceLoader
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.RegistryOps
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.enchantment.Enchantment

/**
 * Upstream generated the Deep Pockets data at runtime with YARRP, which has no 26.x build. The data is now
 * static and the [GlobalSettings] toggles are honoured through a Fabric resource condition
 * (`inventorio:global_setting`):
 *
 * - `data/inventorio/enchantment/deep_pockets.json`: the enchantment (always loaded, as upstream).
 * - `data/inventorio/recipe/deep_pockets_book.json` and
 *   `data/inventorio/advancement/recipes/misc/deep_pockets_book.json`: book recipe and its unlock advancement,
 *   gated by `fabric:load_conditions` on `DeepPocketsBookCraft`.
 * - built-in, always enabled data pack `inventorio:deep_pockets_tags` (`resourcepacks/deep_pockets_tags`):
 *   adds the enchantment to the vanilla enchantment tags via `fabric:overlays`, one overlay per setting
 *   (`DeepPocketsInTrades`, `DeepPocketsInRandomSelection`, `DeepPocketsInEnchantingTable`), or to
 *   `#minecraft:non_treasure` when all three are on. Tag files cannot carry `fabric:load_conditions`,
 *   hence the overlays (evaluated whenever the pack list is reloaded, i.e. on world load and `/reload`).
 */
object InventorioResources {
    @JvmField
    val DEEP_POCKETS: ResourceKey<Enchantment> = ResourceKey.create(Registries.ENCHANTMENT, "deep_pockets".id)

    fun register() {
        ResourceConditions.register(GlobalSettingCondition.TYPE)

        val container = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow()
        val registered = ResourceLoader.registerBuiltinPack(
            "deep_pockets_tags".id,
            container,
            Component.literal("Inventorio Deep Pockets Tags"),
            PackActivationType.ALWAYS_ENABLED,
        )
        if (!registered) logger.error("Failed to register built-in data pack inventorio:deep_pockets_tags")
    }

    /** True iff the boolean [GlobalSettings] entry with the given config key is enabled. */
    data class GlobalSettingCondition(val setting: String) : ResourceCondition {
        override fun getType(): ResourceConditionType<*> = TYPE

        override fun test(registryInfo: RegistryOps.RegistryInfoLookup?): Boolean {
            val entry = GlobalSettings.entries.firstOrNull { it.configKey == setting }
            if (entry !is SettingsEntryBoolean) {
                logger.warn("Unknown boolean global setting '$setting' in resource condition")
                return false
            }
            return entry.boolValue
        }

        companion object {
            val CODEC: MapCodec<GlobalSettingCondition> = RecordCodecBuilder.mapCodec { instance ->
                instance.group(
                    Codec.STRING.fieldOf("setting").forGetter(GlobalSettingCondition::setting),
                ).apply(instance, ::GlobalSettingCondition)
            }

            val TYPE: ResourceConditionType<GlobalSettingCondition> =
                ResourceConditionType.create("global_setting".id, CODEC)
        }
    }
}
