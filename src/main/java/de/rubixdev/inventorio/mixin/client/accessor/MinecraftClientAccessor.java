package de.rubixdev.inventorio.mixin.client.accessor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
@Environment(EnvType.CLIENT)
public interface MinecraftClientAccessor {
    @Invoker("startUseItem")
    void invokeDoItemUse();

    @Accessor("rightClickDelay")
    int getItemUseCooldown();

    @Accessor("rightClickDelay")
    void setItemUseCooldown(int value);
}
