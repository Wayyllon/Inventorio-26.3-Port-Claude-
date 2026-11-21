package de.rubixdev.inventorio.mixin.optional.bowfix;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import de.rubixdev.inventorio.util.BowTester;
import de.rubixdev.inventorio.util.RandomStuff;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Restriction(require = @Condition(type = Condition.Type.TESTER, tester = BowTester.class))
@Mixin(Player.class)
public abstract class PlayerEntityMixin extends LivingEntity {
    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    /**
     * This fixes a bug that an Infinity Bow requires an arrow to shoot.
     * (26.3: the creative check is now {@code hasInfiniteMaterials()} instead of
     * {@code abilities.instabuild})
     */
    @ModifyExpressionValue(
        method = "getProjectile",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;hasInfiniteMaterials()Z"
        )
    )
    private boolean inventorioFixInfinityBow(boolean original, ItemStack bow) {
        var infinity = RandomStuff.getEnchantment(this, Enchantments.INFINITY);
        return original || RandomStuff.getLevelOn(infinity, bow) > 0;
    }
}
