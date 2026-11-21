package de.rubixdev.inventorio.mixin;

import de.rubixdev.inventorio.util.MixinHelpers;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ExperienceOrb.class)
public class ExperienceOrbEntityMixin {
    /**
     * This allows items in the ToolBelt to be Mended
     * <p>
     * 26.3: the orb value is no longer a plain field (it is synced entity data
     * and orbs may be merged with a count), so the value read in
     * {@code playerTouch} is modified instead: the tool belt is mended first
     * and only the remaining XP goes on to vanilla mending/the player.
     */
    @ModifyExpressionValue(
        method = "playerTouch",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;getValue()I"),
        require = 0
    )
    private int inventorioMendToolBeltItems(int value, Player player) {
        Integer left = MixinHelpers.withInventoryAddonReturning(player, addon -> addon.mendToolBeltItems(value));
        return left != null ? left : value;
    }
}
