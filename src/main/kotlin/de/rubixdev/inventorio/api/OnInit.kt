package de.rubixdev.inventorio.api

import net.minecraft.tags.ItemTags
import net.minecraft.world.item.ShearsItem
import net.minecraft.world.item.TridentItem
import de.rubixdev.inventorio.config.GlobalSettings
import de.rubixdev.inventorio.util.ToolBeltMode
import de.rubixdev.inventorio.util.id

// 26.3: PickaxeItem/SwordItem/AxeItem/ShovelItem/HoeItem no longer exist (tools are plain items with
// components), so the vanilla item tags are used instead
fun onApiInit() {
    if (GlobalSettings.toolBeltMode.value != ToolBeltMode.ENABLED) return

    InventorioAPI
        .registerToolBeltSlotIfNotExists(
            InventorioAPI.SLOT_PICKAXE,
            "textures/gui/empty/pickaxe.png".id,
        )!!
        .addAllowingCondition { stack, _ -> stack.`is`(ItemTags.PICKAXES) }
        .addAllowingTag("pickaxes".id)
        .addDenyingTag("pickaxes_blacklist".id)

    InventorioAPI
        .registerToolBeltSlotIfNotExists(
            InventorioAPI.SLOT_SWORD,
            "textures/gui/empty/sword.png".id,
        )!!
        .addAllowingCondition { stack, _ -> stack.`is`(ItemTags.SWORDS) || stack.item is TridentItem }
        .addAllowingTag("swords".id)
        .addDenyingTag("swords_blacklist".id)

    InventorioAPI
        .registerToolBeltSlotIfNotExists(
            InventorioAPI.SLOT_AXE,
            "textures/gui/empty/axe.png".id,
        )!!
        .addAllowingCondition { stack, _ -> stack.`is`(ItemTags.AXES) }
        .addAllowingTag("axes".id)
        .addDenyingTag("axes_blacklist".id)

    InventorioAPI
        .registerToolBeltSlotIfNotExists(
            InventorioAPI.SLOT_SHOVEL,
            "textures/gui/empty/shovel.png".id,
        )!!
        .addAllowingCondition { stack, _ -> stack.`is`(ItemTags.SHOVELS) }
        .addAllowingTag("shovels".id)
        .addDenyingTag("shovels_blacklist".id)

    InventorioAPI
        .registerToolBeltSlotIfNotExists(
            InventorioAPI.SLOT_HOE,
            "textures/gui/empty/hoe.png".id,
        )!!
        .addAllowingCondition { stack, _ -> stack.`is`(ItemTags.HOES) || stack.item is ShearsItem }
        .addAllowingTag("hoes".id)
        .addDenyingTag("hoes_blacklist".id)
}
