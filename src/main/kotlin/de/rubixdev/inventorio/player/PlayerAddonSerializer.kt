package de.rubixdev.inventorio.player

import net.minecraft.util.Prediction
import net.minecraft.world.ItemStackWithSlot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput

/**
 * 26.3: player data is saved through [ValueOutput]/[ValueInput] instead of raw NBT.
 * The on-disk layout is unchanged: an `Inventorio` compound with `SelectedUtilitySlot` and the
 * `DeepPockets`, `UtilityBelt` and `ToolBelt` lists of `{Slot, id, count, components}` entries.
 */
object PlayerAddonSerializer {
    fun serialize(
        inventoryAddon: PlayerInventoryAddon,
        inventorioTag: ValueOutput,
    ) {
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

        deserializeSection(inventoryAddon, inventoryAddon.utilityBelt, inventorioTag.listOrEmpty("UtilityBelt", ItemStackWithSlot.CODEC))
        deserializeSection(inventoryAddon, inventoryAddon.toolBelt, inventorioTag.listOrEmpty("ToolBelt", ItemStackWithSlot.CODEC))
        deserializeSection(inventoryAddon, inventoryAddon.deepPockets, inventorioTag.listOrEmpty("DeepPockets", ItemStackWithSlot.CODEC))
    }

    private fun deserializeSection(
        inventoryAddon: PlayerInventoryAddon,
        inventorySection: MutableList<ItemStack>,
        sectionTag: ValueInput.TypedInputList<ItemStackWithSlot>,
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
