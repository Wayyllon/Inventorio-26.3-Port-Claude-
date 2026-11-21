package de.rubixdev.inventorio.mixin;

import de.rubixdev.inventorio.util.MixinHelpers;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrb.class)
public class ExperienceOrbEntityMixin {
    @Shadow
    private int value;

    /**
     * This inject allows items in the ToolBelt to be Mended
     */
    @Inject(
        method = "playerTouch",
        at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/ExperienceOrb;value:I"),
        require = 0
    )
    private void inventorioMendToolBeltItems(Player player, CallbackInfo ci) {
        MixinHelpers.withInventoryAddon(player, addon -> this.value = addon.mendToolBeltItems(this.value));
    }
}
