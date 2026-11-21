package de.rubixdev.inventorio.client.configscreen

import de.rubixdev.inventorio.config.GlobalSettings
import de.rubixdev.inventorio.config.PlayerSettings
import de.rubixdev.inventorio.config.SettingsEntry
import de.rubixdev.inventorio.config.SettingsEntryBoolean
import de.rubixdev.inventorio.util.PlatformApi
import de.rubixdev.inventorio.util.ScrollWheelUtilityBeltMode
import de.rubixdev.inventorio.util.SegmentedHotbar
import me.shedaniel.clothconfig2.api.ConfigBuilder
import me.shedaniel.clothconfig2.api.ConfigCategory
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import me.shedaniel.clothconfig2.api.Requirement
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

@Environment(EnvType.CLIENT)
object PlayerSettingsScreen {
    fun get(parent: Screen?): Screen {
        val builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setSavingRunnable(PlayerSettings::save)
            .setTitle(Component.translatable("inventorio.settings.player.title"))
        val entryBuilder = builder.entryBuilder()
        val category = builder.getOrCreateCategory(Component.translatable("inventorio.settings.player.title"))

        if (GlobalSettings.allowSwappedHands.boolValue) {
            addBoolEntry(category, entryBuilder, PlayerSettings.swappedHands, requireRestart = false, blocked = false)
        }

        addEnumEntry(category, entryBuilder, PlayerSettings.segmentedHotbar, requireRestart = false, blocked = false, SegmentedHotbar::class.java, SegmentedHotbar.OFF)
        addEnumEntry(category, entryBuilder, PlayerSettings.scrollWheelUtilityBelt, requireRestart = false, blocked = false, ScrollWheelUtilityBeltMode::class.java, ScrollWheelUtilityBeltMode.OFF)
        addBoolEntry(category, entryBuilder, PlayerSettings.skipEmptyUtilitySlots, requireRestart = false, blocked = false)
        addBoolEntry(category, entryBuilder, PlayerSettings.useItemAppliesToOffhand, requireRestart = false, blocked = false)
        addBoolEntry(category, entryBuilder, PlayerSettings.canThrowUnloyalTrident, requireRestart = false, blocked = false)
        addBoolEntry(category, entryBuilder, PlayerSettings.darkTheme, requireRestart = false, blocked = false)
        addBoolEntry(category, entryBuilder, PlayerSettings.aggressiveButtonRemoval, requireRestart = false, blocked = false)
        addBoolEntry(category, entryBuilder, PlayerSettings.toggleButton, requireRestart = false, blocked = false)
        addBoolEntry(category, entryBuilder, PlayerSettings.centeredScreen, requireRestart = false, blocked = false)

        if (!GlobalSettings.allowSwappedHands.boolValue) {
            category.addEntry(
                entryBuilder
                    .startTextDescription(Component.translatable("inventorio.settings.player.swapped_hands.disabled"))
                    .setTooltipForNextRenderPass(Component.translatable("inventorio.settings.player.swapped_hands.disabled.tooltip"))
                    .build(),
            )
        }
        return builder.build()
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Enum<*>> addEnumEntry(
        category: ConfigCategory,
        entryBuilder: ConfigEntryBuilder,
        settingsEntry: SettingsEntry,
        requireRestart: Boolean,
        blocked: Boolean,
        enumClass: Class<T>,
        defaultValue: T,
    ) {
        if (blocked) {
            category.addEntry(
                entryBuilder
                    .startTextDescription(
                        Component.translatable(settingsEntry.displayText)
                            .append(" = ")
                            .append(Component.translatable("${settingsEntry.displayText}.${settingsEntry.value}")),
                    )
                    .build(),
            )
            return
        }

        val builder = entryBuilder
            .startEnumSelector(
                Component.translatable(settingsEntry.displayText),
                enumClass,
                settingsEntry.value as T,
            )
            .setEnumNameProvider { Component.translatable("${settingsEntry.displayText}.${it.name}") }
            .setDefaultValue(defaultValue)
            .setSaveConsumer { settingsEntry.value = it }
        if (requireRestart) {
            builder.requireRestart()
        }
        if (settingsEntry.tooltipText != null) {
            builder.setTooltipForNextRenderPass(Component.translatable(settingsEntry.tooltipText))
        }
        category.addEntry(builder.build())
    }

    @Suppress("UnstableApiUsage")
    fun addBoolEntry(
        category: ConfigCategory,
        entryBuilder: ConfigEntryBuilder,
        settingsEntry: SettingsEntryBoolean,
        requireRestart: Boolean,
        blocked: Boolean,
        requirement: Requirement? = null,
    ) {
        if (blocked) {
            category.addEntry(
                entryBuilder
                    .startTextDescription(
                        Component.translatable(settingsEntry.displayText)
                            .append(" = ")
                            .append(Component.translatable("text.cloth-config.boolean.value.${settingsEntry.boolValue}")),
                    )
                    .build(),
            )
            return
        }

        val builder = entryBuilder
            .startBooleanToggle(
                Component.translatable(settingsEntry.displayText),
                settingsEntry.boolValue,
            )
            .setDefaultValue(settingsEntry.defaultValue == true)
            .setSaveConsumer { settingsEntry.value = it }
            .apply {
                requirement?.let { setRequirement(it) }
            }
        if (requireRestart) {
            builder.requireRestart()
        }
        if (settingsEntry.tooltipText != null) {
            builder.setTooltipForNextRenderPass(Component.translatable(settingsEntry.tooltipText))
        }
        category.addEntry(builder.build())
    }
}
