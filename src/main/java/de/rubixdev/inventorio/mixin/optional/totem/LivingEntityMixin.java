package de.rubixdev.inventorio.mixin.optional.totem;

import de.rubixdev.inventorio.util.MixinHelpers;
import de.rubixdev.inventorio.util.TotemTester;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;

@Restriction(require = @Condition(type = Condition.Type.TESTER, tester = TotemTester.class))
@Mixin(value = LivingEntity.class, priority = 500)
public abstract class LivingEntityMixin extends Entity {
    public LivingEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    /**
     * This optional mixin allows a Totem of Undying to be used automatically
     * from any Utility Slot.
     */
    @SuppressWarnings("InvalidInjectorMethodSignature")
//    @ModifyVariable(
//            method = "checkTotemDeathProtection",
//            at = @At(value = "JUMP", ordinal = 2, shift = At.Shift.BEFORE),
//            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V")),
//            ordinal = 0
//    )
    @ModifyVariable(
        method = "checkTotemDeathProtection",
        at = @At(value = "JUMP", opcode = Opcodes.IFNULL, shift = At.Shift.BEFORE),
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V")),
        ordinal = 0
    )
    private ItemStack inventorioGetTotemFromUtilityBar(ItemStack original) {
        // noinspection ConstantValue
        if (original != null || !((Entity) this instanceof Player player)) return original;
        return MixinHelpers.withInventoryAddonReturning(
            player,
            addon -> addon.utilityBelt.stream()
                .filter(stack -> stack.is(Items.TOTEM_OF_UNDYING))
                .findFirst()
                .map(totem -> {
                    ItemStack copy = totem.copy();
                    totem.shrink(1);
                    return copy;
                })
                .orElse(original)
        );
    }
}
