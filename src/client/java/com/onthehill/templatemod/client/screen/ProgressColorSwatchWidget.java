package com.onthehill.templatemod.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.IntSupplier;

/**
 * A small filled rectangle showing the currently-configured color for a
 * color-valued field, per {@code minecraft-gui-standards.md}'s "Color
 * Configuration Fields" rule. The color is read fresh from
 * {@code colorSupplier} every frame (never cached), so it updates live on
 * every change anywhere in {@link ProgressConfigScreen}'s pending state or
 * {@link ProgressColorPickerScreen}.
 *
 * <p>Optionally clickable: when {@code onClick} is non-{@code null}, clicking
 * this swatch invokes it — used on {@link ProgressConfigScreen}'s inline
 * field row so the swatch itself is one of the clickable surfaces that opens
 * {@link ProgressColorPickerScreen}. When {@code onClick} is {@code null}
 * (its use inside the picker screen itself, as the large live preview),
 * this widget is purely decorative.
 */
final class ProgressColorSwatchWidget extends AbstractWidget
{
    private static final int BORDER_RGB = 0xFF1E1E1E;

    private final IntSupplier colorSupplier;
    private final Runnable onClick;

    ProgressColorSwatchWidget(int x, int y, int width, int height, IntSupplier colorSupplier, Runnable onClick)
    {
        super(x, y, width, height, Component.empty());
        this.colorSupplier = colorSupplier;
        this.onClick = onClick;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        int argb = 0xFF000000 | (colorSupplier.getAsInt() & 0xFFFFFF);
        guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), BORDER_RGB);
        guiGraphics.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + getHeight() - 1, argb);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick)
    {
        // AbstractWidget#mouseClicked already verified isMouseOver(...) before dispatching here —
        // no need to re-check it ourselves.
        if (onClick != null && event.button() == 0)
        {
            onClick.run();
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output)
    {
        // Purely visual — no interaction to narrate beyond what the adjacent field control already
        // announces.
    }
}
