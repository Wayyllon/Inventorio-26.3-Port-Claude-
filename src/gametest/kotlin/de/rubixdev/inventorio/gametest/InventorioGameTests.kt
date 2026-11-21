package de.rubixdev.inventorio.gametest

import com.mojang.serialization.JsonOps
import de.rubixdev.inventorio.InventorioResources
import de.rubixdev.inventorio.config.GlobalSettings
import de.rubixdev.inventorio.player.InventorioScreenHandler
import de.rubixdev.inventorio.player.PlayerAddonSerializer
import de.rubixdev.inventorio.player.PlayerInventoryAddon
import de.rubixdev.inventorio.player.PlayerInventoryAddon.Companion.inventoryAddon
import de.rubixdev.inventorio.slot.DeepPocketsSlot
import de.rubixdev.inventorio.slot.ToolBeltSlot
import net.fabricmc.fabric.api.gametest.v1.GameTest
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.TagParser
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.EnchantmentTags
import net.minecraft.util.ProblemReporter
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.GameType
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.storage.TagValueInput
import net.minecraft.world.level.storage.TagValueOutput
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

/**
 * Server game tests for the 26.3 port. Run with `./gradlew runGameTest`.
 *
 * Inventory slot layout of [InventorioScreenHandler]: 0-4 crafting, 5-8 armor, 9-35 main, 36-44 hotbar,
 * 45-71 Deep Pockets, 72-79 Utility Belt, 80+ Tool Belt.
 */
class InventorioGameTests {
    private fun GameTestHelper.player(): ServerPlayer {
        val player = makeMockServerPlayer(GameType.SURVIVAL) as ServerPlayer
        val pos = absoluteVec(Vec3(2.5, 1.0, 2.5))
        player.snapTo(pos.x, pos.y, pos.z, 0f, 0f)
        return player
    }

    private val ServerPlayer.addon: PlayerInventoryAddon
        get() = inventoryAddon ?: throw AssertionError("player has no Inventorio addon")

    private fun GameTestHelper.check(condition: Boolean, message: String) = assertTrue(condition, message)

    private fun GameTestHelper.deepPockets(): Holder.Reference<Enchantment> =
        level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(InventorioResources.DEEP_POCKETS)

    private fun GameTestHelper.leggings(level: Int): ItemStack =
        ItemStack(Items.IRON_LEGGINGS).also { it.enchant(deepPockets(), level) }

    private fun toolBeltIndex(addon: PlayerInventoryAddon, stack: ItemStack): Int {
        val index = addon.findFittingToolBeltIndex(stack)
        if (index == -1) throw AssertionError("${stack.item} does not fit into any tool belt slot")
        return index
    }

    // ===================== Deep Pockets =====================

    @GameTest
    fun deepPocketsEnchantmentExistsAndAddsSlots(helper: GameTestHelper) {
        val enchantment = helper.deepPockets()
        helper.check(enchantment.value().maxLevel == 3, "Deep Pockets max level should be 3")
        helper.check(enchantment.value().canEnchant(ItemStack(Items.DIAMOND_LEGGINGS)), "Deep Pockets should apply to leggings")
        helper.check(!enchantment.value().canEnchant(ItemStack(Items.DIAMOND_HELMET)), "Deep Pockets should not apply to helmets")
        // default config: in trades, random loot and the enchanting table -> #minecraft:non_treasure (built-in pack overlay)
        helper.check(enchantment.`is`(EnchantmentTags.NON_TREASURE), "Deep Pockets should be in #minecraft:non_treasure")
        helper.check(enchantment.`is`(EnchantmentTags.IN_ENCHANTING_TABLE), "Deep Pockets should be available in the enchanting table")

        val player = helper.player()
        val addon = player.addon
        helper.check(addon.getDeepPocketsRowCount() == 0, "no Deep Pockets rows without the enchantment")
        helper.check(addon.getAvailableUtilityBeltSize() == 4, "small utility belt without Deep Pockets")

        player.setItemSlot(EquipmentSlot.LEGS, helper.leggings(2))
        helper.check(EnchantmentHelper.getEnchantmentLevel(enchantment, player) == 2, "enchantment level on player")
        helper.check(addon.getDeepPocketsRowCount() == 2, "Deep Pockets II should give 2 rows, got ${addon.getDeepPocketsRowCount()}")
        helper.check(addon.getAvailableDeepPocketsRange().count() == 18, "18 Deep Pockets slots")
        helper.check(addon.getAvailableUtilityBeltSize() == 8, "full utility belt with Deep Pockets")

        val handler = InventorioScreenHandler(1, player.inventory)
        helper.check(handler.getAvailableDeepPocketsRange() == 45..62, "screen handler Deep Pockets range, got ${handler.getAvailableDeepPocketsRange()}")
        helper.check((handler.getSlot(45) as DeepPocketsSlot).canTakeItems, "first Deep Pockets slot usable")
        helper.check(!(handler.getSlot(63) as DeepPocketsSlot).canTakeItems, "third Deep Pockets row locked")
        helper.check(handler.getSlot(80) is ToolBeltSlot, "slot 80 should be the first tool belt slot")

        // items picked up go into Deep Pockets once the main inventory is full
        for (i in 0 until 36) player.inventory.setItem(i, ItemStack(Items.STONE, 64))
        helper.check(player.inventory.add(ItemStack(Items.DIRT, 10)), "pick up into Deep Pockets")
        helper.check(addon.deepPockets[0].`is`(Items.DIRT) && addon.deepPockets[0].count == 10, "dirt should be in Deep Pockets")
        helper.check(player.inventory.contains(ItemStack(Items.DIRT)), "Inventory.contains(ItemStack) sees the addon")
        helper.check(player.inventory.contains { it.`is`(Items.DIRT) }, "Inventory.contains(Predicate) sees the addon")
        helper.succeed()
    }

    @GameTest
    fun deepPocketsBookRecipeAndCondition(helper: GameTestHelper) {
        val recipeKey = ResourceKey.create<Recipe<*>>(Registries.RECIPE, Identifier.fromNamespaceAndPath("inventorio", "deep_pockets_book"))
        helper.check(helper.level.server.recipeManager.byKey(recipeKey).isPresent, "Deep Pockets book recipe should be loaded with the default config")

        val json = com.google.gson.JsonParser.parseString("""{"condition":"inventorio:global_setting","setting":"DeepPocketsBookCraft"}""")
        val condition = ResourceCondition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow()
        val old = GlobalSettings.deepPocketsBookCraft.value
        try {
            GlobalSettings.deepPocketsBookCraft.value = true
            helper.check(condition.test(null), "condition should pass when the setting is enabled")
            GlobalSettings.deepPocketsBookCraft.value = false
            helper.check(!condition.test(null), "condition should fail when the setting is disabled")
        } finally {
            GlobalSettings.deepPocketsBookCraft.value = old
        }
        helper.succeed()
    }

    // ===================== Tool Belt =====================

    @GameTest
    fun toolBeltSelectsBestTool(helper: GameTestHelper) {
        val player = helper.player()
        val addon = player.addon
        val pickaxe = ItemStack(Items.DIAMOND_PICKAXE)
        val axe = ItemStack(Items.IRON_AXE)
        val shovel = ItemStack(Items.STONE_SHOVEL)
        val sword = ItemStack(Items.DIAMOND_SWORD)

        helper.check(toolBeltIndex(addon, pickaxe) == 0, "pickaxe slot")
        helper.check(toolBeltIndex(addon, sword) == 1, "sword slot")
        helper.check(toolBeltIndex(addon, axe) == 2, "axe slot")
        helper.check(toolBeltIndex(addon, shovel) == 3, "shovel slot")
        helper.check(toolBeltIndex(addon, ItemStack(Items.GOLDEN_HOE)) == 4, "hoe slot")
        helper.check(toolBeltIndex(addon, ItemStack(Items.SHEARS)) == 4, "shears go into the hoe slot")
        helper.check(toolBeltIndex(addon, ItemStack(Items.TRIDENT)) == 1, "trident goes into the sword slot")
        helper.check(addon.findFittingToolBeltIndex(ItemStack(Items.STONE)) == -1, "blocks don't fit the tool belt")

        // insert through the screen handler (shift-click from the main inventory)
        val handler = InventorioScreenHandler(1, player.inventory)
        for ((i, tool) in listOf(pickaxe, axe, shovel, sword).withIndex()) player.inventory.setItem(9 + i, tool)
        for (i in 0 until 4) handler.quickMoveStack(player, 9 + i)
        helper.check(addon.toolBelt[0].`is`(Items.DIAMOND_PICKAXE), "pickaxe moved into the tool belt")
        helper.check(addon.toolBelt[2].`is`(Items.IRON_AXE), "axe moved into the tool belt")
        helper.check(addon.toolBelt[3].`is`(Items.STONE_SHOVEL), "shovel moved into the tool belt")
        helper.check(addon.toolBelt[1].`is`(Items.DIAMOND_SWORD), "sword moved into the tool belt")
        for (i in 9 until 13) helper.check(player.inventory.getItem(i).isEmpty, "main inventory slot $i emptied")
        val beltPickaxe = addon.toolBelt[0]
        val beltSword = addon.toolBelt[1]
        val beltAxe = addon.toolBelt[2]
        val beltShovel = addon.toolBelt[3]

        player.setOnGround(true)
        // empty main hand: the fastest tool from the belt is used and displayed
        val cases = listOf(
            Blocks.STONE.defaultBlockState() to beltPickaxe,
            Blocks.DIRT.defaultBlockState() to beltShovel,
            Blocks.OAK_LOG.defaultBlockState() to beltAxe,
        )
        for ((state, expected) in cases) {
            addon.displayTool = ItemStack.EMPTY
            val speed = addon.getMiningSpeedMultiplier(state)
            helper.check(addon.displayTool === expected, "${state.block} should use ${expected.item}, got ${addon.displayTool.item}")
            helper.check(speed == expected.getDestroySpeed(state), "${state.block} speed $speed")
            helper.check(player.mainHandItem === expected, "displayed main hand item should be ${expected.item}")
            // the vanilla Player.getDestroySpeed hook uses the tool belt as well
            addon.displayTool = ItemStack.EMPTY
            val playerSpeed = player.getDestroySpeed(state)
            helper.check(playerSpeed == expected.getDestroySpeed(state), "${state.block}: Player.getDestroySpeed $playerSpeed should use the tool belt")
            helper.check(player.hasCorrectToolForDrops(state), "${state.block}: correct tool for drops")
        }

        // a tool held in the main hand is respected
        addon.displayTool = ItemStack.EMPTY
        val heldPickaxe = ItemStack(Items.WOODEN_PICKAXE)
        player.inventory.setItem(player.inventory.selectedSlot, heldPickaxe)
        addon.getMiningSpeedMultiplier(Blocks.DIRT.defaultBlockState())
        helper.check(addon.displayTool.isEmpty, "held tool must not be replaced by the tool belt")

        // attacking with an empty hand displays the sword
        player.inventory.setItem(player.inventory.selectedSlot, ItemStack.EMPTY)
        addon.prePlayerAttack()
        helper.check(addon.displayTool === beltSword, "attack should use the tool belt sword")
        addon.postPlayerAttack()
        helper.succeed()
    }

    // ===================== Shift-click =====================

    @GameTest
    fun shiftClickRouting(helper: GameTestHelper) {
        val player = helper.player()
        val addon = player.addon
        player.setItemSlot(EquipmentSlot.LEGS, helper.leggings(1))
        val handler = InventorioScreenHandler(1, player.inventory)

        // hotbar -> main inventory
        player.inventory.setItem(0, ItemStack(Items.COBBLESTONE, 32))
        handler.quickMoveStack(player, 36)
        helper.check(player.inventory.getItem(0).isEmpty, "hotbar slot emptied")
        helper.check(player.inventory.getItem(9).`is`(Items.COBBLESTONE), "hotbar item moved to the main inventory")

        // main inventory -> Deep Pockets
        handler.quickMoveStack(player, 9)
        helper.check(addon.deepPockets[0].`is`(Items.COBBLESTONE) && addon.deepPockets[0].count == 32, "main inventory item moved to Deep Pockets")

        // Deep Pockets -> main inventory
        handler.quickMoveStack(player, 45)
        helper.check(addon.deepPockets[0].isEmpty, "Deep Pockets slot emptied")
        helper.check((0 until 36).any { player.inventory.getItem(it).`is`(Items.COBBLESTONE) }, "Deep Pockets item moved back to the main inventory")

        // armor from the main inventory -> armor slot
        player.inventory.setItem(20, ItemStack(Items.DIAMOND_HELMET))
        handler.quickMoveStack(player, 20)
        helper.check(player.getItemBySlot(EquipmentSlot.HEAD).`is`(Items.DIAMOND_HELMET), "helmet equipped by shift-click")

        // utility belt -> main inventory
        addon.utilityBelt[1] = ItemStack(Items.TORCH, 16)
        handler.quickMoveStack(player, 73)
        helper.check(addon.utilityBelt[1].isEmpty, "utility belt slot emptied")

        // tool belt -> main inventory
        addon.toolBelt[0] = ItemStack(Items.IRON_PICKAXE)
        handler.quickMoveStack(player, 80)
        helper.check(addon.toolBelt[0].isEmpty, "tool belt slot emptied")
        helper.check((0 until 36).any { player.inventory.getItem(it).`is`(Items.IRON_PICKAXE) }, "tool moved to the main inventory")
        helper.succeed()
    }

    // ===================== Save / load =====================

    private fun fillAddon(addon: PlayerInventoryAddon) {
        addon.deepPockets[3] = ItemStack(Items.DIAMOND, 12)
        addon.utilityBelt[2] = ItemStack(Items.TORCH, 40)
        addon.toolBelt[0] = ItemStack(Items.NETHERITE_PICKAXE).also { it.damageValue = 17 }
        addon.selectedUtility = 2
    }

    @GameTest
    fun saveLoadRoundTrip(helper: GameTestHelper) {
        val registries = helper.level.registryAccess()
        val player = helper.player()
        player.setItemSlot(EquipmentSlot.LEGS, helper.leggings(1))
        fillAddon(player.addon)
        player.addon.toolBelt[0].enchant(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY), 4)

        val output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries)
        player.saveWithoutId(output)
        val tag = output.buildResult()
        val inventorio = tag.getCompoundOrEmpty("Inventorio")
        helper.check(inventorio.getIntOr("SelectedUtilitySlot", -1) == 2, "SelectedUtilitySlot saved")
        for (key in listOf("DeepPockets", "UtilityBelt", "ToolBelt"))
            helper.check(inventorio.getListOrEmpty(key).size == 1, "$key saved with one entry")
        val deepPocketsEntry = inventorio.getListOrEmpty("DeepPockets").getCompoundOrEmpty(0)
        helper.check(deepPocketsEntry.getIntOr("Slot", -1) == 3, "entry keeps its Slot")
        helper.check(deepPocketsEntry.getStringOr("id", "") == "minecraft:diamond", "entry keeps its id")
        helper.check(deepPocketsEntry.getIntOr("count", -1) == 12, "entry keeps its count")

        val loaded = helper.player()
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag))
        val addon = loaded.addon
        helper.check(addon.selectedUtility == 2, "selected utility restored")
        helper.check(addon.deepPockets[3].`is`(Items.DIAMOND) && addon.deepPockets[3].count == 12, "Deep Pockets restored")
        helper.check(addon.utilityBelt[2].`is`(Items.TORCH) && addon.utilityBelt[2].count == 40, "utility belt restored")
        val tool = addon.toolBelt[0]
        helper.check(tool.`is`(Items.NETHERITE_PICKAXE) && tool.damageValue == 17, "tool belt restored")
        helper.check(EnchantmentHelper.getItemEnchantmentLevel(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY), tool) == 4, "tool enchantment restored")
        helper.succeed()
    }

    @GameTest
    fun loadLegacy1211Data(helper: GameTestHelper) {
        // Inventorio compound as written by Inventorio 1.11.0 on Minecraft 1.21.1 (no DataVersion, old enchantment format)
        val legacy = TagParser.parseCompoundFully(
            """
            {SelectedUtilitySlot: 1,
             DeepPockets: [{Slot: 0, id: "minecraft:emerald", count: 5}],
             UtilityBelt: [{Slot: 1, id: "minecraft:totem_of_undying", count: 1}],
             ToolBelt: [{Slot: 0, id: "minecraft:diamond_pickaxe", count: 1,
                         components: {"minecraft:enchantments": {levels: {"minecraft:efficiency": 5}, show_in_tooltip: 0b}}}]}
            """.trimIndent(),
        )
        val tag = CompoundTag()
        tag.put("Inventorio", legacy)
        val player = helper.player()
        val input = TagValueInput.create(ProblemReporter.DISCARDING, helper.level.registryAccess(), tag)
        PlayerAddonSerializer.deserialize(player.addon, input.childOrEmpty("Inventorio"))
        val addon = player.addon
        helper.check(addon.selectedUtility == 1, "legacy selected utility")
        helper.check(addon.deepPockets[0].`is`(Items.EMERALD) && addon.deepPockets[0].count == 5, "legacy Deep Pockets item")
        helper.check(addon.utilityBelt[1].`is`(Items.TOTEM_OF_UNDYING), "legacy utility belt item")
        val tool = addon.toolBelt[0]
        helper.check(tool.`is`(Items.DIAMOND_PICKAXE), "legacy tool belt item")
        val efficiency = helper.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY)
        helper.check(EnchantmentHelper.getItemEnchantmentLevel(efficiency, tool) == 5, "legacy enchantment upgraded by the data fixer")
        helper.succeed()
    }

    // ===================== Death / respawn =====================

    @GameTest
    fun deathDropsAndKeepInventoryCopy(helper: GameTestHelper) {
        val player = helper.player()
        fillAddon(player.addon)
        val curse = helper.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.VANISHING_CURSE)
        player.addon.deepPockets[5] = ItemStack(Items.GOLDEN_SWORD).also { it.enchant(curse, 1) }

        // keepInventory / dimension change: Inventory.replaceWith copies the addon
        val copy = helper.player()
        copy.inventory.replaceWith(player.inventory)
        helper.check(copy.addon.deepPockets[3].`is`(Items.DIAMOND), "addon copied with the inventory")
        helper.check(copy.addon.selectedUtility == 2, "selected utility copied")

        // death without keepInventory: everything in the addon is dropped
        player.inventory.dropAll()
        helper.check(player.addon.stacks.all { it.isEmpty }, "addon empty after dropAll")
        val area = AABB.ofSize(player.position(), 8.0, 8.0, 8.0)
        val dropped = helper.level.getEntitiesOfClass(ItemEntity::class.java, area).map { it.item }
        helper.check(dropped.any { it.`is`(Items.DIAMOND) && it.count == 12 }, "Deep Pockets item dropped")
        helper.check(dropped.any { it.`is`(Items.TORCH) && it.count == 40 }, "utility belt item dropped")
        helper.check(dropped.any { it.`is`(Items.NETHERITE_PICKAXE) }, "tool belt item dropped")
        helper.check(dropped.none { it.`is`(Items.GOLDEN_SWORD) }, "Curse of Vanishing item not dropped")
        helper.succeed()
    }

    // ===================== Utility belt =====================

    @GameTest
    fun totemFromUtilityBeltSavesPlayer(helper: GameTestHelper) {
        // plain mock player: the mock ServerPlayer has no connection to send the totem's effects to
        val player = helper.makeMockPlayer(GameType.SURVIVAL)
        val addon = player.inventoryAddon ?: throw AssertionError("player has no Inventorio addon")
        addon.selectedUtility = 0
        addon.utilityBelt[0] = ItemStack(Items.TORCH, 3)
        addon.utilityBelt[2] = ItemStack(Items.TOTEM_OF_UNDYING)
        helper.check(player.offhandItem.`is`(Items.TORCH), "selected utility shown in the offhand")

        val method = LivingEntity::class.java.getDeclaredMethod("checkTotemDeathProtection", DamageSource::class.java)
        method.isAccessible = true
        val saved = method.invoke(player, helper.level.damageSources().generic()) as Boolean
        helper.check(saved, "totem in a non-selected utility slot should save the player")
        helper.check(addon.utilityBelt[2].isEmpty, "totem consumed")
        helper.check(player.health == 1.0f, "health set to 1")
        helper.check(addon.utilityBelt[0].count == 3, "other utility untouched")

        val savedAgain = method.invoke(player, helper.level.damageSources().generic()) as Boolean
        helper.check(!savedAgain, "no second totem")
        helper.succeed()
    }

    @GameTest
    fun utilityBeltOffhandAndArrows(helper: GameTestHelper) {
        val player = helper.player()
        val addon = player.addon
        addon.utilityBelt[0] = ItemStack(Items.SHIELD)
        helper.check(player.offhandItem.`is`(Items.SHIELD), "offhand shows the selected utility")
        player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack(Items.TORCH, 5))
        helper.check(addon.utilityBelt[0].`is`(Items.TORCH), "setting the offhand writes into the utility belt")
        // (switchToNextUtility is client-only; the server just receives the index)
        addon.selectedUtility = 1
        helper.check(player.offhandItem.isEmpty, "offhand follows the selected utility")
        addon.selectedUtility = 0

        // arrows anywhere in the addon are used by a bow
        player.inventory.setItem(player.inventory.selectedSlot, ItemStack(Items.BOW))
        addon.deepPockets[0] = ItemStack(Items.SPECTRAL_ARROW, 7)
        val projectile = player.getProjectile(player.mainHandItem)
        helper.check(projectile.`is`(Items.SPECTRAL_ARROW), "bow finds arrows in the addon, got ${projectile.item}")
        helper.succeed()
    }
}
