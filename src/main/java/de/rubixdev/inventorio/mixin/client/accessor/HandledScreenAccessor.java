package de.rubixdev.inventorio.mixin.client.accessor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@SuppressWarnings("unused")
@Mixin(AbstractContainerScreen.class)
@Environment(EnvType.CLIENT)
public interface HandledScreenAccessor<T extends AbstractContainerMenu>extends ScreenAccessor {
    @Accessor("imageWidth")
    int getBackgroundWidth();

    @Accessor("imageHeight")
    int getBackgroundHeight();

    @Accessor("leftPos")
    int getX();

    @Accessor("leftPos")
    void setX(int x);

    @Accessor("topPos")
    int getY();

    @Accessor("topPos")
    void setY(int y);

    @Accessor("menu")
    T getHandler();
}
