package de.rubixdev.inventorio.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import de.rubixdev.inventorio.player.InventorioScreenHandler;
import de.rubixdev.inventorio.player.PlayerAddonSerializer;
import de.rubixdev.inventorio.player.PlayerInventoryAddon;
import de.rubixdev.inventorio.util.MixinHelpers;
import de.rubixdev.inventorio.util.PlayerDuck;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerEntityMixin extends LivingEntity implements PlayerDuck {
    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Shadow
    public abstract Inventory getInventory();

    @Unique public PlayerInventoryAddon inventorioAddon;

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void inventorioCreateAddon(Level world, GameProfile gameProfile, CallbackInfo ci) {
        Player thisPlayer = (Player) (Object) this;
        inventorioAddon = new PlayerInventoryAddon(thisPlayer);
    }

    /**
     * This override causes the selected UtilityBelt item to be displayed in
     * the offhand.
     * <p>
     * 26.3: {@code Player} no longer overrides {@code getItemBySlot}/{@code setItemSlot}
     * (they live in {@link LivingEntity} and delegate to the entity equipment), so instead
     * of injecting into them, this mixin adds overrides to {@code Player}. The equipment
     * itself is left untouched so that inventory slot 40 keeps referring to the (unused)
     * vanilla offhand slot, like in previous versions.
     */
    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        if (slot == EquipmentSlot.OFFHAND && inventorioAddon != null) return inventorioAddon.getDisplayedOffHandStack();
        return super.getItemBySlot(slot);
    }

    /**
     * This override governs the custom behavior of displaying items in both
     * hands. First, the offhand is attached to the utility belt, rather than a
     * vanilla slot. Second, a player can swap the main hand and the offhand.
     * It also refreshes the available slots when we equip armor through
     * right-clicking or a dispenser.
     */
    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        if (inventorioAddon == null) {
            super.setItemSlot(slot, stack);
            return;
        }
        if (slot == EquipmentSlot.MAINHAND && inventorioAddon.getSwappedHands()) {
            inventorioAddon.setSelectedUtilityStack(stack);
            // TODO: should this pass something else as the previous stack?
            this.onEquipItem(slot, ItemStack.EMPTY, stack);
        } else if (slot == EquipmentSlot.OFFHAND) {
            if (inventorioAddon.getSwappedHands()) inventorioAddon.setSelectedHotbarStack(stack);
            else inventorioAddon.setSelectedUtilityStack(stack);
            // TODO: should this pass something else as the previous stack?
            this.onEquipItem(slot, ItemStack.EMPTY, stack);
        } else {
            super.setItemSlot(slot, stack);
        }

        if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) MixinHelpers
            .withScreenHandler((Player) (Object) this, InventorioScreenHandler::updateDeepPocketsCapacity);
    }

    /**
     * This makes block breaking speed use the most preferred tool from the
     * tool belt.
     * <p>
     * 26.3: {@code Inventory.getDestroySpeed} was removed and inlined into
     * {@code Player.getDestroySpeed}, so this replaces the selected item's
     * destroy speed there (moved here from {@link PlayerInventoryMixin}).
     */
    @WrapOperation(
        method = "getDestroySpeed",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;getDestroySpeed(Lnet/minecraft/world/level/block/state/BlockState;)F"
        )
    )
    private float inventorioGetBlockBreakingSpeed(ItemStack instance, BlockState state, Operation<Float> original) {
        if (inventorioAddon != null) return inventorioAddon.getMiningSpeedMultiplier(state);
        return original.call(instance, state);
    }

    /**
     * This mixin allows arrows stored in the addon slots to be used by a bow
     */
    @ModifyReturnValue(method = "getProjectile", at = @At("RETURN"))
    private ItemStack inventorioGetArrowType(ItemStack original, ItemStack bowStack) {
        if (!original.isEmpty()) return original;
        ItemStack arrowStack = inventorioAddon.getActiveArrowType(bowStack);
        return arrowStack != null ? arrowStack : original;
    }

    /**
     * These 2 injections cause a correct weapon to be automatically selected
     * and withdrawn upon attack
     */
    @Inject(method = "attack", at = @At(value = "HEAD"))
    private void inventorioPreAttack(Entity target, CallbackInfo ci) {
        if (target.isAttackable()) inventorioAddon.prePlayerAttack();
    }

    @Inject(method = "attack", at = @At(value = "RETURN"))
    private void inventorioPostAttack(Entity target, CallbackInfo ci) {
        if (target.isAttackable()) inventorioAddon.postPlayerAttack();
    }

    /**
     * These 2 injections read and write additional data into Player's NBT
     */
    @Inject(method = "readAdditionalSaveData", at = @At(value = "RETURN"))
    private void inventorioDeserializePlayerAddon(ValueInput input, CallbackInfo ci) {
        PlayerAddonSerializer.INSTANCE.deserialize(inventorioAddon, input.childOrEmpty("Inventorio"));
    }

    @Inject(method = "addAdditionalSaveData", at = @At(value = "RETURN"))
    private void inventorioSerializePlayerAddon(ValueOutput output, CallbackInfo ci) {
        PlayerAddonSerializer.INSTANCE.serialize(inventorioAddon, output.child("Inventorio"));
    }

    @Inject(method = "aiStep", at = @At(value = "RETURN"))
    private void inventorioTick(CallbackInfo ci) {
        inventorioAddon.tick();
    }

    @Nullable @Override
    public PlayerInventoryAddon inventorio$getInventorioAddon() {
        return inventorioAddon;
    }
}
