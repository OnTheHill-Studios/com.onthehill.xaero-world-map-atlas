package com.onthehill.xaeroworldmapbook.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * An invisible, non-interactive filler occupying a fixed amount of space in a {@code LinearLayout} row or column.
 * Used two ways in {@link ProgressConfigScreen}:
 * <ul>
 *     <li>Horizontally, inside a field row, between the label and the value control — its width is
 *     {@code rowWidth} minus every other fixed-width segment in that row, so it absorbs 100% of whatever extra
 *     width a wider window provides. That is what makes the label anchor to the row's left edge (a fixed width, no
 *     reason to move) while the value control and reset button anchor to the row's right edge (their combined
 *     width is also fixed, but their position tracks however wide this spacer currently is) — per the project
 *     owner's explicit request that the label flex left and the controls flex right, rather than both growing
 *     proportionally with the window (which was the previous, incorrect behavior: a proportionally-wide label
 *     column left a large dead gap between the short label text and the value control, and didn't actually pin the
 *     controls to the true right margin the way a flexible gap does).</li>
 *     <li>Vertically, between a section's last field row and the next section's heading — see
 *     {@link ProgressConfigScreen#buildFields} — so the extra breathing room the project owner asked for sits
 *     <em>after</em> a section's own content, not baked into the heading widget itself (which put the extra gap on
 *     the wrong side: between the heading text and its own divider, rather than between the previous section and
 *     the heading), and so the very first section — which has no previous section — never gets a spacer before it.</li>
 * </ul>
 */
final class ProgressConfigSpacer extends AbstractWidget
{
    ProgressConfigSpacer(int width, int height)
    {
        super(0, 0, Math.max(0, width), Math.max(0, height), Component.empty());
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        // Intentionally empty — purely a layout filler, nothing to draw.
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output)
    {
        // Intentionally empty — purely a layout filler, nothing to narrate.
    }
}
