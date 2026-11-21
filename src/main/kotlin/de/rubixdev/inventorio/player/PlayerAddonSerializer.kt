package de.rubixdev.inventorio.player

import com.mojang.serialization.Codec
import com.mojang.serialization.Dynamic
import de.rubixdev.inventorio.util.logger
import net.minecraft.SharedConstants
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.Tag
import net.minecraft.util.Prediction
import net.minecraft.util.datafix.DataFixers
import net.minecraft.util.datafix.fixes.References
import net.minecraft.world.ItemStackWithSlot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput

/**
 * 26.3: player data is saved through [ValueOutput]/[ValueInput] instead of raw NBT.
 * The on-disk layout is unchanged: an `Inventorio` compound with `SelectedUtilitySlot` and the
 * `DeepPockets`, `UtilityBelt` and `ToolBelt` lists of `{Slot, id, count, components}` entries.
 *
 * Vanilla's data fixers do not know about the `Inventorio` compound, so its item stacks are not upgraded when an
 * older world is loaded (e.g. the 1.21.5 enchantment component format change would make enchanted tools fail to
 * decode and disappear). The compound therefore now also stores a `DataVersion`; data without one is treated as
 * coming from Minecraft 1.21.1 and each stack is run through the vanilla `item_stack` data fixer before decoding.
 */
object PlayerAddonSerializer {
    /** Data version of Minecraft 1.21.1, the version upstream Inventorio data without a `DataVersion` comes from */
    const val LEGACY_DATA_VERSION = 3955

    private val currentDataVersion get() = SharedConstants.getCurrentVersion().dataVersion().version()

    fun serialize(
        inventoryAddon: PlayerInventoryAddon,
        inventorioTag: ValueOutput,
    ) {
        inventorioTag.putInt("DataVersion", currentDataVersion)
        inventorioTag.putInt("SelectedUtilitySlot", inventoryAddon.selectedUtility)
        serializeSection(inventorioTag.list("DeepPockets", ItemStackWithSlot.CODEC), inventoryAddon.deepPockets)
        serializeSection(inventorioTag.list("UtilityBelt", ItemStackWithSlot.CODEC), inventoryAddon.utilityBelt)
        serializeSection(inventorioTag.list("ToolBelt", ItemStackWithSlot.CODEC), inventoryAddon.toolBelt)
    }

    private fun serializeSection(output: ValueOutput.TypedOutputList<ItemStackWithSlot>, section: List<ItemStack>) {
        for ((slotIndex, itemStack) in section.withIndex()) {
            if (itemStack.isEmpty) {
                continue
            }
            output.add(ItemStackWithSlot(slotIndex, itemStack))
        }
    }

    fun deserialize(
        inventoryAddon: PlayerInventoryAddon,
        inventorioTag: ValueInput,
    ) {
        inventoryAddon.selectedUtility = inventorioTag.getIntOr("SelectedUtilitySlot", 0)
        val dataVersion = inventorioTag.getIntOr("DataVersion", LEGACY_DATA_VERSION)

        deserializeSection(inventoryAddon, inventoryAddon.utilityBelt, readSection(inventorioTag, "UtilityBelt", dataVersion))
        deserializeSection(inventoryAddon, inventoryAddon.toolBelt, readSection(inventorioTag, "ToolBelt", dataVersion))
        deserializeSection(inventoryAddon, inventoryAddon.deepPockets, readSection(inventorioTag, "DeepPockets", dataVersion))
    }

    private fun readSection(input: ValueInput, name: String, dataVersion: Int): Iterable<ItemStackWithSlot> {
        val current = currentDataVersion
        if (dataVersion >= current) {
            return input.listOrEmpty(name, ItemStackWithSlot.CODEC)
        }
        // Older data: upgrade every stack with the vanilla data fixer first
        val rawEntries = input.read(name, Codec.list(CompoundTag.CODEC)).orElse(emptyList())
        val ops = input.lookup().createSerializationContext(NbtOps.INSTANCE)
        return rawEntries.mapNotNull { raw ->
            val fixed: Tag = try {
                DataFixers.getDataFixer()
                    .update(References.ITEM_STACK, Dynamic<Tag>(NbtOps.INSTANCE, raw), dataVersion, current)
                    .value
            } catch (e: Exception) {
                logger.error("Failed to upgrade Inventorio item stack $raw from data version $dataVersion", e)
                raw
            }
            ItemStackWithSlot.CODEC.parse(ops, fixed)
                .resultOrPartial { logger.error("Failed to load Inventorio item stack $fixed: $it") }
                .orElse(null)
        }
    }

    private fun deserializeSection(
        inventoryAddon: PlayerInventoryAddon,
        inventorySection: MutableList<ItemStack>,
        sectionTag: Iterable<ItemStackWithSlot>,
    ) {
        for (i in inventorySection.indices)
            inventorySection[i] = ItemStack.EMPTY

        for (entry in sectionTag) {
            val itemStack = entry.stack()
            val slotIndex = entry.slot()
            if (slotIndex in inventorySection.indices) {
                inventorySection[slotIndex] = itemStack
            } else {
                inventoryAddon.player.drop(itemStack, false, Prediction.SERVER_ONLY)
            }
        }
    }
}
