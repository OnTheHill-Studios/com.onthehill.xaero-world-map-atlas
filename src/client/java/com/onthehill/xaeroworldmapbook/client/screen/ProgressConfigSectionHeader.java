package com.onthehill.xaeroworldmapbook.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * A section heading, its text centered horizontally within the row — per the project owner's explicit request that
 * a section's heading read as obviously a heading, not just another left-aligned line of text. Vanilla's
 * {@link net.minecraft.client.gui.components.StringWidget} has no built-in centering, so this draws directly via
 * {@link GuiGraphicsExtractor#centeredText}, the same helper vanilla's own centered titles use, rather than
 * hand-computing an x offset against a left-aligned draw call.
 * <p>
 * Non-interactive, matching {@link ProgressConfigSectionDivider}'s own rationale: overrides no mouse/keyboard
 * handling, and narrates nothing of its own since the heading text is already read by narration when the fields
 * beneath it are focused.
 */
final class ProgressConfigSectionHeader extends AbstractWidget
{
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    private final Font font;

    ProgressConfigSectionHeader(int width, int height, Component message, Font font)
    {
        super(0, 0, width, height, message);
        this.font = font;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        guiGraphics.centeredText(this.font, getMessage(), getX() + getWidth() / 2, getY() + (getHeight() - 9) / 2, TEXT_COLOR);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output)
    {
        // Intentionally empty — see class javadoc.
    }
}
