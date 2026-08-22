package com.onthehill.xaeroworldmapbook.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.network.chat.Component;

/**
 * A field-row label that only scrolls its overflowing text while the mouse is hovering over it — per the project
 * owner's explicit request — and otherwise clips with an ellipsis, the same as any other non-interactive label.
 * <p>
 * Vanilla's own {@link StringWidget#setMaxWidth(int, StringWidget.TextOverflow)} scrolling mode has no hover
 * condition built in — a {@code StringWidget} configured with {@link StringWidget.TextOverflow#SCROLLING} animates
 * continuously regardless of mouse position. {@link AbstractWidget#isHovered} (inherited, set by the framework's
 * own {@code render(...)} immediately before {@link #extractWidgetRenderState} runs each frame) is accurate at
 * render time, so this subclass just flips which {@code TextOverflow} mode is active for that one frame before
 * delegating to the real vanilla rendering logic — no custom scrolling/animation code of its own.
 */
final class ProgressConfigScrollingLabel extends StringWidget
{
    private final int maxWidth;

    ProgressConfigScrollingLabel(int width, int height, Component message, Font font)
    {
        super(width, height, message, font);
        this.maxWidth = width;
        setMaxWidth(width, TextOverflow.CLAMPED);
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        setMaxWidth(this.maxWidth, isHovered() ? TextOverflow.SCROLLING : TextOverflow.CLAMPED);
        super.extractWidgetRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }
}
