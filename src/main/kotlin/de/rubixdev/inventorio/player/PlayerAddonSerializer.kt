package de.rubixdev.inventorio.player

import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.core.HolderLookup

object PlayerAddonSerializer {
    fun serialize(
        registries: HolderLookup.Provider,
        inventoryAddon: PlayerInventoryAddon,
        inventorioTag: CompoundTag,
    ) {
        inventorioTag.putInt("SelectedUtilitySlot", inventoryAddon.selectedUtility)
        inventorioTag.put("DeepPockets", serializeSection(registries, inventoryAddon.deepPockets))
        inventorioTag.put("UtilityBelt", serializeSection(registries, inventoryAddon.utilityBelt))
        inventorioTag.put("ToolBelt", serializeSection(registries, inventoryAddon.toolBelt))
    }

    private fun serializeSection(registries: HolderLookup.Provider, section: List<ItemStack>): ListTag {
        val resultTag = ListTag()
        for ((slotIndex, itemStack) in section.withIndex()) {
            if (itemStack.isEmpty) {
                continue
            }
            var itemTag = CompoundTag()
            itemTag.putInt("Slot", slotIndex)
            itemTag = itemStack.save(registries, itemTag) as CompoundTag
            resultTag.add(itemTag)
        }
        return resultTag
    }

    fun deserialize(
        registries: HolderLookup.Provider,
        inventoryAddon: PlayerInventoryAddon,
        inventorioTag: CompoundTag,
    ) {
        inventoryAddon.selectedUtility = inventorioTag.getInt("SelectedUtilitySlot")

        deserializeSection(registries, inventoryAddon, inventoryAddon.utilityBelt, inventorioTag.getList("UtilityBelt", 10))
        deserializeSection(registries, inventoryAddon, inventoryAddon.toolBelt, inventorioTag.getList("ToolBelt", 10))
        deserializeSection(registries, inventoryAddon, inventoryAddon.deepPockets, inventorioTag.getList("DeepPockets", 10))
    }

    private fun deserializeSection(
        registries: HolderLookup.Provider,
        inventoryAddon: PlayerInventoryAddon,
        inventorySection: MutableList<ItemStack>,
        sectionTag: ListTag,
    ) {
        for (i in inventorySection.indices)
            inventorySection[i] = ItemStack.EMPTY

        for (itemTag in sectionTag) {
            val compoundTag = itemTag as CompoundTag
            val itemStack = ItemStack.parseOptional(registries, compoundTag)
            val slotIndex = compoundTag.getInt("Slot")
            if (slotIndex in inventorySection.indices) {
                inventorySection[slotIndex] = itemStack
            } else {
                inventoryAddon.player.dropItem(itemStack, false)
            }
        }
    }
}
