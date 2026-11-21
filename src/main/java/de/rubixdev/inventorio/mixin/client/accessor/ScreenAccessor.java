package de.rubixdev.inventorio.mixin.client.accessor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(Screen.class)
@Environment(EnvType.CLIENT)
public interface ScreenAccessor {
    @Accessor("children")
    List<GuiEventListener> getChildren();

    @Accessor("renderables")
    List<Renderable> getDrawables();

    @Accessor("narratables")
    List<NarratableEntry> getSelectables();

    @Accessor("minecraft")
    @Nullable Minecraft getClient();

    @Accessor("font")
    Font getTextRenderer();

    @Invoker("addRenderableWidget")
    <T extends GuiEventListener & Renderable> T callAddDrawableChild(T drawableElement);
}
