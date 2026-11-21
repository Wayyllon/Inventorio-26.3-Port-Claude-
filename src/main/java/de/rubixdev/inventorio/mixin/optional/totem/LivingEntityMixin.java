package de.rubixdev.inventorio.mixin.optional.totem;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.rubixdev.inventorio.util.MixinHelpers;
import de.rubixdev.inventorio.util.TotemTester;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Restriction(require = @Condition(type = Condition.Type.TESTER, tester = TotemTester.class))
@Mixin(value = LivingEntity.class, priority = 500)
public abstract class LivingEntityMixin extends Entity {
    public LivingEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    /**
     * This optional mixin allows a Totem of Undying to be used automatically
     * from any Utility Slot.
     * <p>
     * 26.3: totems are identified by the {@code death_protection} component and
     * vanilla reads both the stack and the component from
     * {@code getItemInHand} inside its loop. So instead of replacing the found
     * stack afterwards, the stack returned for the offhand is replaced with the
     * first death-protecting utility belt stack when the hand itself has none.
     * Vanilla then copies, shrinks and applies it as usual.
     */
    @WrapOperation(
        method = "checkTotemDeathProtection",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;"
        )
    )
    private ItemStack inventorioGetTotemFromUtilityBar(LivingEntity instance, InteractionHand hand, Operation<ItemStack> original) {
        ItemStack inHand = original.call(instance, hand);
        if (hand != InteractionHand.OFF_HAND
            || inHand.has(DataComponents.DEATH_PROTECTION)
            || !((Entity) this instanceof Player player)) return inHand;
        ItemStack totem = MixinHelpers.withInventoryAddonReturning(
            player,
            addon -> addon.utilityBelt.stream()
                .filter(stack -> stack.has(DataComponents.DEATH_PROTECTION))
                .findFirst()
                .orElse(null)
        );
        return totem != null ? totem : inHand;
    }
}
