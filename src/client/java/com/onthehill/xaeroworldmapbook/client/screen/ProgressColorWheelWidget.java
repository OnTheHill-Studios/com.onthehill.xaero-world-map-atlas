package com.onthehill.xaeroworldmapbook.client.screen;

import com.onthehill.xaeroworldmapbook.client.config.ProgressColorUtil;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The "ring + inner area" color-wheel input mode for
 * {@link ProgressColorPickerScreen}: a hue ring around the perimeter, with a
 * circular saturation/value area in the center.
 *
 * <p><b>Circular inner area.</b> The inner saturation/value area is a
 * genuine disc, not an inscribed square with visible gaps in the corners.
 * Implemented via <b>elliptical grid mapping</b> (Fong, "Analytical Methods
 * for Squaring the Disc"), a well-established square&harr;disc coordinate
 * transform:
 * <pre>
 *   forward (square [-1,1]&sup2; -&gt; disc):  u = x * sqrt(1 - y&sup2;/2),  v = y * sqrt(1 - x&sup2;/2)
 *   inverse (disc -&gt; square [-1,1]&sup2;):
 *     x = 0.5*sqrt(2 + u&sup2; - v&sup2; + 2u&radic;2) - 0.5*sqrt(2 + u&sup2; - v&sup2; - 2u&radic;2)
 *     y = 0.5*sqrt(2 - u&sup2; + v&sup2; + 2v&radic;2) - 0.5*sqrt(2 - u&sup2; + v&sup2; - 2v&radic;2)
 * </pre>
 * The original saturation/value square (x = saturation, y = 1 - value, both
 * in {@code [-1, 1]}) is warped through the forward mapping to build the
 * disc's precomputed color grid ({@link #buildSvCells()}), and a click/drag
 * point inside the disc is warped back through the inverse mapping
 * ({@link #discPointToSaturationValue(double, double)}) to recover the
 * {@code (saturation, value)} pair that point represents. This mapping is
 * deliberately <b>not</b> bijective at the boundary: the original square's
 * bottom edge (y = 1, uniformly black regardless of saturation) sweeps out
 * to an <em>arc</em> of the disc's boundary under the forward mapping, not a
 * single point — multiple boundary points all represent pure black. What
 * matters, and is genuinely true of this mapping, is that pure white,
 * pure black, and the fully-saturated pure hue each land at one specific,
 * individually reachable point on the disc.
 *
 * <p><b>Rendering approach.</b> {@code GuiGraphicsExtractor} has no
 * arbitrary per-pixel draw primitive suited to a smooth radial/angular
 * gradient, so this widget precomputes a coarse grid of small filled cells
 * at construction time ({@link #buildRingCells()}, called once in the
 * constructor; {@link #buildSvCells()}, called once in the constructor and
 * again only when the selected hue actually changes — never per frame) and
 * its {@code extractWidgetRenderState} simply replays those precomputed
 * cells with {@code fill(...)} calls every frame. No trigonometry, HSV
 * conversion, or square&harr;disc mapping happens in the render path itself,
 * per {@code minecraft-gui-standards.md}'s Performance section.
 *
 * <p><b>Drag capture.</b> The ring and inner area are two zones of one
 * widget; {@link #activeDragRegion} records which region the pointer went
 * down on ({@link #onClick}) and every subsequent {@link #onDrag} for that
 * same press routes through that captured region only, regardless of where
 * the cursor has physically wandered to since — the region is only ever
 * re-derived from the cursor's position at the start of a new press. This
 * prevents a drag that starts in the inner area from being silently
 * reinterpreted as a ring drag if it wanders out past the ring's inner
 * radius mid-gesture.
 */
final class ProgressColorWheelWidget extends AbstractWidget
{
    /**
     * Notified whenever the player's own click/drag on this widget selects
     * a new color — never called by this widget's own {@link #setColor(int)}
     * (that path is for an external caller, e.g. the hex field or sliders,
     * pushing a value into this widget, and must not re-trigger a
     * synchronization loop back out).
     */
    interface Listener
    {
        void onColorChanged(int rgb);
    }

    /**
     * Which of this widget's two hit-testable regions a drag gesture
     * started on — captured on {@link #onClick} and reused for every
     * subsequent {@link #onDrag} of the same press.
     */
    private enum DragRegion
    {
        NONE,
        HUE_RING,
        SV_DISC
    }

    private static final int CELL = 3;
    private static final int SIZE = 150;
    private static final float OUTER_RADIUS = 73.0f;
    private static final float RING_INNER_RADIUS = 58.0f;

    // Elliptical grid mapping constant (Fong's inverse formula's "2u*sqrt(2)"/"2v*sqrt(2)" terms).
    private static final double SQRT2 = Math.sqrt(2.0);

    private record Cell(int localX, int localY, int argb)
    {
    }

    private final Listener listener;
    private final List<Cell> ringCells;
    private List<Cell> svCells;
    private DragRegion activeDragRegion = DragRegion.NONE;

    private float hue;
    private float saturation = 1.0f;
    private float value = 1.0f;

    ProgressColorWheelWidget(int x, int y, Listener listener)
    {
        super(x, y, SIZE, SIZE, Component.translatable("gui.xaero-world-map-book.color_picker.wheel"));
        this.listener = listener;
        this.ringCells = buildRingCells();
        this.svCells = buildSvCells();
    }

    /**
     * Sets this widget's displayed color without notifying {@link #listener} — the path used when
     * an external control (hex field, sliders) is the source of truth for this change, so this
     * widget only needs to redraw, not re-broadcast the same change back out.
     *
     * @param rgb The packed {@code 0xRRGGBB} color to display.
     */
    void setColor(int rgb)
    {
        float[] hsv = ProgressColorUtil.rgbToHsv(rgb);
        boolean hueChanged = Float.compare(hsv[0], this.hue) != 0;
        this.hue = hsv[0];
        this.saturation = hsv[1];
        this.value = hsv[2];

        if (hueChanged)
        {
            this.svCells = buildSvCells();
        }
    }

    private List<Cell> buildRingCells()
    {
        List<Cell> cells = new ArrayList<>();
        float center = SIZE / 2.0f;

        for (int gy = 0; gy < SIZE; gy += CELL)
        {
            for (int gx = 0; gx < SIZE; gx += CELL)
            {
                float dx = (gx + CELL / 2.0f) - center;
                float dy = (gy + CELL / 2.0f) - center;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);

                if (distance < RING_INNER_RADIUS || distance > OUTER_RADIUS)
                {
                    continue;
                }

                double angleDegrees = Math.toDegrees(Math.atan2(dy, dx));

                if (angleDegrees < 0)
                {
                    angleDegrees += 360.0;
                }

                int rgb = ProgressColorUtil.hsvToRgb((float) angleDegrees, 1.0f, 1.0f);
                cells.add(new Cell(gx, gy, 0xFF000000 | rgb));
            }
        }

        return cells;
    }

    /**
     * Builds the inner disc's precomputed color grid by warping the saturation/value square through
     * the forward elliptical-grid mapping (square -&gt; disc), then filling every grid cell whose
     * center lands inside the disc (radius {@link #RING_INNER_RADIUS}) with the color the
     * <em>inverse</em> mapping recovers for that cell — i.e. cell colors are computed the same way
     * a click there would be interpreted, so the rendered grid and the hit-test agree exactly.
     */
    private List<Cell> buildSvCells()
    {
        List<Cell> cells = new ArrayList<>();
        float center = SIZE / 2.0f;

        for (int gy = 0; gy < SIZE; gy += CELL)
        {
            for (int gx = 0; gx < SIZE; gx += CELL)
            {
                float px = gx + CELL / 2.0f;
                float py = gy + CELL / 2.0f;
                float dx = px - center;
                float dy = py - center;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);

                if (distance > RING_INNER_RADIUS)
                {
                    continue;
                }

                double[] sv = discPointToSaturationValue(dx / RING_INNER_RADIUS, dy / RING_INNER_RADIUS);
                int rgb = ProgressColorUtil.hsvToRgb(this.hue, (float) sv[0], (float) sv[1]);
                cells.add(new Cell(gx, gy, 0xFF000000 | rgb));
            }
        }

        return cells;
    }

    /**
     * Inverse elliptical-grid mapping: given a point {@code (u, v)} inside the unit disc (both in
     * {@code [-1, 1]}, {@code u&sup2;+v&sup2; &le; 1}), recovers the {@code (saturation, value)}
     * pair the original square would have had at the corresponding square coordinate. Radicands are
     * clamped to {@code >= 0} defensively against floating-point round-off exactly at the disc's
     * boundary, where the mathematically-exact result is {@code 0} but the computed value can drift
     * fractionally negative.
     *
     * @param u Disc x-coordinate, normalized to {@code [-1, 1]} (divide the pixel offset from
     *     center by {@link #RING_INNER_RADIUS} first).
     * @param v Disc y-coordinate, normalized the same way.
     * @return {@code [saturation, value]}, both clamped to {@code [0, 1]}.
     */
    private static double[] discPointToSaturationValue(double u, double v)
    {
        double u2 = u * u;
        double v2 = v * v;
        double termX = 2.0 + u2 - v2;
        double termY = 2.0 - u2 + v2;

        double x = 0.5 * Math.sqrt(Math.max(0.0, termX + 2.0 * u * SQRT2))
            - 0.5 * Math.sqrt(Math.max(0.0, termX - 2.0 * u * SQRT2));
        double y = 0.5 * Math.sqrt(Math.max(0.0, termY + 2.0 * v * SQRT2))
            - 0.5 * Math.sqrt(Math.max(0.0, termY - 2.0 * v * SQRT2));

        // x = -1..1 -> saturation = 0..1 ; y = -1(top, full value)..1(bottom, black) -> value = 1..0
        double saturation = clamp01((x + 1.0) / 2.0);
        double value = clamp01((1.0 - y) / 2.0);
        return new double[] { saturation, value };
    }

    /**
     * Forward elliptical-grid mapping: given the current {@code (saturation, value)}, returns the
     * disc-normalized {@code (u, v)} point (both in {@code [-1, 1]}) used to place the SV
     * indicator, the exact inverse operation of {@link #discPointToSaturationValue(double, double)}.
     */
    private double[] saturationValueToDiscPoint()
    {
        double x = 2.0 * this.saturation - 1.0;
        double y = 1.0 - 2.0 * this.value;

        double u = x * Math.sqrt(Math.max(0.0, 1.0 - y * y / 2.0));
        double v = y * Math.sqrt(Math.max(0.0, 1.0 - x * x / 2.0));
        return new double[] { u, v };
    }

    private static double clamp01(double value)
    {
        return Math.max(0.0, Math.min(1.0, value));
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        int baseX = getX();
        int baseY = getY();

        for (Cell cell : ringCells)
        {
            guiGraphics.fill(baseX + cell.localX(), baseY + cell.localY(),
                baseX + cell.localX() + CELL, baseY + cell.localY() + CELL, cell.argb());
        }

        for (Cell cell : svCells)
        {
            guiGraphics.fill(baseX + cell.localX(), baseY + cell.localY(),
                baseX + cell.localX() + CELL, baseY + cell.localY() + CELL, cell.argb());
        }

        drawIndicator(guiGraphics, ringIndicatorX(), ringIndicatorY());
        drawIndicator(guiGraphics, svIndicatorX(), svIndicatorY());
    }

    private void drawIndicator(GuiGraphicsExtractor guiGraphics, float localX, float localY)
    {
        int cx = getX() + Math.round(localX);
        int cy = getY() + Math.round(localY);
        guiGraphics.fill(cx - 3, cy - 3, cx + 3, cy + 3, 0xFF000000);
        guiGraphics.fill(cx - 2, cy - 2, cx + 2, cy + 2, 0xFFFFFFFF);
    }

    private float ringIndicatorX()
    {
        float center = SIZE / 2.0f;
        float radius = (RING_INNER_RADIUS + OUTER_RADIUS) / 2.0f;
        return center + radius * (float) Math.cos(Math.toRadians(this.hue));
    }

    private float ringIndicatorY()
    {
        float center = SIZE / 2.0f;
        float radius = (RING_INNER_RADIUS + OUTER_RADIUS) / 2.0f;
        return center + radius * (float) Math.sin(Math.toRadians(this.hue));
    }

    private float svIndicatorX()
    {
        float center = SIZE / 2.0f;
        double[] disc = saturationValueToDiscPoint();
        return center + (float) (disc[0] * RING_INNER_RADIUS);
    }

    private float svIndicatorY()
    {
        float center = SIZE / 2.0f;
        double[] disc = saturationValueToDiscPoint();
        return center + (float) (disc[1] * RING_INNER_RADIUS);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick)
    {
        if (event.button() == 0)
        {
            this.activeDragRegion = regionAt(event.x(), event.y());
            handlePointer(event.x(), event.y());
        }
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dragX, double dragY)
    {
        // Never re-derive the region from the cursor's current position here — always reuse
        // whichever region onClick captured for this press, per the class-level documentation
        // above. If a press somehow never went through onClick, activeDragRegion is NONE and this
        // is a no-op.
        handlePointer(event.x(), event.y());
    }

    private DragRegion regionAt(double mouseX, double mouseY)
    {
        float localX = (float) (mouseX - getX());
        float localY = (float) (mouseY - getY());
        float center = SIZE / 2.0f;
        float dx = localX - center;
        float dy = localY - center;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        return distance >= RING_INNER_RADIUS ? DragRegion.HUE_RING : DragRegion.SV_DISC;
    }

    private void handlePointer(double mouseX, double mouseY)
    {
        if (this.activeDragRegion == DragRegion.NONE)
        {
            return;
        }

        float localX = (float) (mouseX - getX());
        float localY = (float) (mouseY - getY());
        float center = SIZE / 2.0f;
        float dx = localX - center;
        float dy = localY - center;

        boolean hueChanged = false;

        if (this.activeDragRegion == DragRegion.HUE_RING)
        {
            double angleDegrees = Math.toDegrees(Math.atan2(dy, dx));

            if (angleDegrees < 0)
            {
                angleDegrees += 360.0;
            }

            hueChanged = Float.compare((float) angleDegrees, this.hue) != 0;
            this.hue = (float) angleDegrees;
        }
        else
        {
            double u = clamp(dx / RING_INNER_RADIUS, -1.0f, 1.0f);
            double v = clamp(dy / RING_INNER_RADIUS, -1.0f, 1.0f);
            double[] sv = discPointToSaturationValue(u, v);
            this.saturation = (float) sv[0];
            this.value = (float) sv[1];
        }

        if (hueChanged)
        {
            this.svCells = buildSvCells();
        }

        this.listener.onColorChanged(ProgressColorUtil.hsvToRgb(this.hue, this.saturation, this.value));
    }

    private static double clamp(double value, double min, double max)
    {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output)
    {
        output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, getMessage());
    }
}
