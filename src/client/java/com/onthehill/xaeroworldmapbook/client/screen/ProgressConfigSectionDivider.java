package com.onthehill.xaeroworldmapbook.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * A purely decorative horizontal rule, dropped into {@link ProgressConfigScreen}'s content immediately after each
 * section heading, so the boundary between the World Map section, the Minimap section, and the shared settings
 * below them reads as a real visual break rather than just a gap.
 * <p>
 * Deliberately a real {@code LinearLayout} child (via {@code AbstractWidget}/{@code LayoutElement}) rather than a
 * rectangle drawn separately in {@link ProgressConfigScreen}'s own render pass — per
 * {@code minecraft-gui-standards.md}'s Layout Container Semantics section, anything positioned outside the actual
 * widget tree that this screen's {@code ScrollableLayout} manages would not scroll in sync with the fields it's
 * meant to visually bound. Being a genuine tree member means it is measured, positioned, and scrolled by exactly
 * the same {@code arrangeElements()}/{@code ScrollableLayout} machinery as every surrounding
 * {@code StringWidget}/{@code Button}/{@code EditBox} row, with no separate coordinate bookkeeping to keep in sync.
 * Pattern matches {@code com.onthehill.climbing}'s {@code ClimbingConfigSectionDivider}.
 * <p>
 * Non-interactive: overrides no mouse/keyboard handling, and {@link #updateWidgetNarration} adds nothing to the
 * narration builder, per {@code minecraft-gui-standards.md}'s Tooltips &amp; Narration section's "purely
 * decorative background elements" tooltip exemption — the same applies to narration for a non-interactive element
 * with no player-facing purpose beyond visual grouping.
 */
final class ProgressConfigSectionDivider extends AbstractWidget
{
    /**
     * Total row height this widget occupies in its parent {@code LinearLayout} (including blank space above/below
     * the drawn line itself, so adjacent rows don't feel cramped against it).
     */
    static final int HEIGHT = 10;

    private static final int LINE_RGB = 0xFFB8AF9F;

    ProgressConfigSectionDivider(int width)
    {
        super(0, 0, width, HEIGHT, Component.empty());
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        int lineY = getY() + HEIGHT / 2;
        guiGraphics.fill(getX(), lineY, getX() + getWidth(), lineY + 1, LINE_RGB);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output)
    {
        // Intentionally empty — see class javadoc.
    }
}
