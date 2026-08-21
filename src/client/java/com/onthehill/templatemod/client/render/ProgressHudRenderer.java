package com.onthehill.templatemod.client.render;

import com.onthehill.templatemod.client.config.ClientVisualizationConfig;
import com.onthehill.templatemod.client.config.ProgressVisualizationMode;
import com.onthehill.templatemod.client.network.ClientNetworkHandler;
import com.onthehill.templatemod.progress.ProgressMath;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Draws the example progress value on the player's HUD using whichever
 * visualization the player has selected in {@link ClientVisualizationConfig}.
 * Registered as a {@link HudElement} via {@code HudElementRegistry} from
 * {@code TemplateModClient} — this MC version replaced the old single
 * {@code HudRenderCallback} event with a per-element registry, but the
 * per-frame drawing API itself ({@code GuiGraphicsExtractor}, the direct
 * successor to the old {@code DrawContext}) is otherwise unchanged.
 *
 * <p>Both draw methods cache their {@code Identifier}/color constants as
 * static fields rather than resolving anything inside the per-frame draw
 * calls, per the GUI performance standard.
 */
public final class ProgressHudRenderer implements HudElement
{
    private static final int BAR_WIDTH = 100;
    private static final int BAR_HEIGHT = 6;
    private static final int BAR_MARGIN_BOTTOM = 60;
    private static final int BAR_BACKGROUND_COLOR = 0x80000000;

    private static final int RADIAL_RADIUS = 10;
    private static final int RADIAL_MARGIN_BOTTOM = 70;
    private static final int RADIAL_SEGMENTS = 32;
    private static final int RADIAL_BACKGROUND_COLOR = 0x80000000;

    private final ClientVisualizationConfig config;

    public ProgressHudRenderer(ClientVisualizationConfig config)
    {
        this.config = config;
    }

    /**
     * Draws the current frame's progress visualization.
     *
     * @param context Active draw context for this frame.
     * @param tickCounter Delta tracker for this frame, unused by this renderer
     *     since its extrapolation is driven by {@code ClientNetworkHandler}'s
     *     own tick counter instead.
     */
    @Override
    public void extractRenderState(GuiGraphicsExtractor context, DeltaTracker tickCounter)
    {
        int screenWidth = context.guiWidth();
        int screenHeight = context.guiHeight();

        float progress = ProgressMath.extrapolate(
            ClientNetworkHandler.getLastSyncedProgress(),
            ClientNetworkHandler.getLastSyncedRatePerTick(),
            ClientNetworkHandler.getTicksSinceLastSync()
        );

        // Read fresh every frame (never cached across frames) so a color
        // change from the config screen takes effect immediately, per
        // minecraft-gui-standards.md's Color Configuration Fields rule —
        // this is a cheap int parse/read, not the kind of per-frame
        // allocation or expensive recomputation that rule's Performance
        // section warns against.
        int fillColor = 0xFF000000 | config.getVisualizationColorRgb();

        ProgressVisualizationMode mode = config.getVisualizationMode();
        if (mode == ProgressVisualizationMode.RADIAL)
        {
            drawRadial(context, screenWidth, screenHeight, progress, fillColor);
        }
        else
        {
            drawBar(context, screenWidth, screenHeight, progress, fillColor);
        }
    }

    private static void drawBar(GuiGraphicsExtractor context, int screenWidth, int screenHeight, float progress, int fillColor)
    {
        int left = (screenWidth - BAR_WIDTH) / 2;
        int top = screenHeight - BAR_MARGIN_BOTTOM;

        context.fill(left, top, left + BAR_WIDTH, top + BAR_HEIGHT, BAR_BACKGROUND_COLOR);

        int filledWidth = Math.round(BAR_WIDTH * progress);
        if (filledWidth > 0)
        {
            context.fill(left, top, left + filledWidth, top + BAR_HEIGHT, fillColor);
        }
    }

    /**
     * Draws a filled radial wedge sweeping clockwise from the top, using a
     * fan of small filled triangles-as-rectangles approximation via repeated
     * {@code GuiGraphicsExtractor.fill} calls along {@code RADIAL_SEGMENTS}
     * angular steps. This keeps the renderer to vanilla drawing primitives
     * rather than requiring a custom shader/texture asset.
     *
     * @implNote For each segment whose angle is within the completed
     *     fraction of progress * 360 degrees, a short filled line is drawn
     *     from the center outward at that angle, approximating a solid pie
     *     wedge as the segment count grows. This has not been visually
     *     confirmed on-screen — see this class's outstanding verification
     *     note in Spec 000's Post-Implementation Notes.
     */
    private static void drawRadial(GuiGraphicsExtractor context, int screenWidth, int screenHeight, float progress, int fillColor)
    {
        int centerX = screenWidth / 2;
        int centerY = screenHeight - RADIAL_MARGIN_BOTTOM;

        drawRing(context, centerX, centerY, RADIAL_RADIUS, RADIAL_SEGMENTS, RADIAL_BACKGROUND_COLOR, 1.0f);
        if (progress > 0.0f)
        {
            drawRing(context, centerX, centerY, RADIAL_RADIUS, RADIAL_SEGMENTS, fillColor, progress);
        }
    }

    private static void drawRing(GuiGraphicsExtractor context, int centerX, int centerY, int radius, int segments, int color, float fraction)
    {
        int segmentsToDraw = Math.max(1, Math.round(segments * fraction));
        for (int i = 0; i < segmentsToDraw; i++)
        {
            double angle = (Math.PI * 2.0) * ((double) i / segments) - (Math.PI / 2.0);
            int x = centerX + (int) Math.round(Math.cos(angle) * radius);
            int y = centerY + (int) Math.round(Math.sin(angle) * radius);

            // The center corner must stay exactly at (centerX, centerY) —
            // padding it too (the previous centerX - 1 / centerY - 1) biased
            // every spoke asymmetrically toward the top-left, shifting the
            // whole rendered wedge's apparent center off the true one. Only
            // the outer corner is nudged, and only away from center, so
            // spokes lying exactly on an axis (x == centerX or y == centerY)
            // still get at least 1px of thickness instead of collapsing to a
            // zero-width fill.
            int outerX = x + (x >= centerX ? 1 : -1);
            int outerY = y + (y >= centerY ? 1 : -1);
            context.fill(centerX, centerY, outerX, outerY, color);
        }
    }
}
