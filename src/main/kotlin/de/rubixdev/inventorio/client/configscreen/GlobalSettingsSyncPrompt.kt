package de.rubixdev.inventorio.client.configscreen

import com.google.gson.JsonObject
import de.rubixdev.inventorio.config.GlobalSettings
import me.shedaniel.clothconfig2.api.ConfigBuilder
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting

@Environment(EnvType.CLIENT)
object GlobalSettingsSyncPrompt {
    private var exitFlag = false

    fun get(newSettingsJson: JsonObject): Screen {
        exitFlag = false
        val builder = ConfigBuilder.create()
            .setParentScreen(getLoopbackScreen(newSettingsJson))
            .setTitle(Component.translatable("inventorio.settings.global.title"))
        val entryBuilder = builder.entryBuilder()
        val category = builder.getOrCreateCategory(Component.translatable("inventorio.settings.global.title"))

        val appended = Component.literal("")
        for (option in GlobalSettings.entries) {
            val newValue = option.tryElementAsValue(newSettingsJson.get(option.configKey))
            if (option.value == newValue) {
                continue
            }
            val crossedValueText = Component.translatable(option.value.toString())
            crossedValueText.style = Style.EMPTY.withFormatting(ChatFormatting.STRIKETHROUGH)
            appended.append(option.displayText).append(": ").append(crossedValueText).append(" -> $newValue\n")
        }
        category.addEntry(entryBuilder.startTextDescription(Component.translatable("inventorio.settings.global.sync_restart_prompt", appended)).build())

        category.addEntry(
            entryBuilder
                .startEnumSelector(
                    Component.translatable("inventorio.settings.global.sync_restart_toggle"),
                    ExitMode::class.java,
                    ExitMode.PLACEHOLDER,
                )
                .setEnumNameProvider { Component.translatable("inventorio.settings.global.sync_restart_toggle." + it.name) }
                .setSaveConsumer {
                    exitFlag = true
                    if (it != ExitMode.DO_NOTHING) {
                        GlobalSettings.fromJson(newSettingsJson)
                        GlobalSettings.save()
                    }
                    if (it == ExitMode.SYNC_AND_EXIT) {
                        Minecraft.getInstance().destroy()
                    } else {
                        Minecraft.getInstance().setScreen(null)
                    }
                }
                .build(),
        )
        category.addEntry(entryBuilder.startTextDescription(Component.translatable("inventorio.settings.global.sync_restart_prompt.hint")).build())

        return builder.build()
    }

    private fun getLoopbackScreen(newSettingsJson: JsonObject): Screen {
        val builder = ConfigBuilder.create()
            .setParentScreen(null)
            .setAfterInitConsumer { Minecraft.getInstance().setScreen(if (!exitFlag) get(newSettingsJson) else null) }
            .setTitle(Component.translatable("inventorio.settings.global.title"))
        builder.getOrCreateCategory(Component.translatable("inventorio.settings.global.title"))
        return builder.build()
    }

    private enum class ExitMode {
        SYNC_AND_EXIT,
        SYNC_WITHOUT_EXIT,
        DO_NOTHING,
        PLACEHOLDER,
    }
}
