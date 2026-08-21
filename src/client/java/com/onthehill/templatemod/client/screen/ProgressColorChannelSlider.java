package com.onthehill.templatemod.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;
import java.util.function.IntUnaryOperator;

/**
 * One of {@link ProgressColorPickerScreen}'s three R/G/B sliders (0-255
 * each). A hand-rolled {@code AbstractWidget} (drawn with plain
 * {@code fill(...)} calls) rather than a subclass of vanilla's
 * {@code AbstractSliderButton} — matches the plain {@code onClick}/
 * {@code onDrag} contract every other custom widget in this package already
 * uses.
 *
 * <p>The track renders a left-to-right gradient of what the final color
 * would become at every possible value of <em>this</em> channel, holding
 * the other two channels at their current values (e.g. the R slider sweeps
 * from the current color with R=0 on the left to the current color with
 * R=255 on the right). {@link #gradientCompose} is supplied by
 * {@link ProgressColorPickerScreen} at construction — given a candidate 0-255
 * value for *this* channel, it returns the full composed {@code 0xRRGGBB}
 * color using the other two channels' live current values (read from the
 * sibling slider instances at call time, not captured/cached). Per
 * {@code minecraft-gui-standards.md}'s Performance section, the gradient is
 * precomputed once per pixel column into {@link #gradientArgb} by
 * {@link #refreshGradient()} — called once after construction and again
 * only when some channel's value actually changes — never recomputed inside
 * {@link #extractWidgetRenderState}, which only replays the precomputed
 * array.
 */
final class ProgressColorChannelSlider extends AbstractWidget
{
    private static final int TRACK_HEIGHT = 4;
    private static final int HANDLE_WIDTH = 6;
    private static final int HANDLE_RGB = 0xFFE0E0E0;

    private final IntConsumer onChange;
    private final IntUnaryOperator gradientCompose;
    private int value;
    private int[] gradientArgb = new int[0];

    /**
     * @param gradientCompose Given a candidate {@code [0, 255]} value for
     *     this slider's own channel, returns the fully composed
     *     {@code 0xRRGGBB} color that value would produce against the
     *     other two channels' current values. Invoked only from
     *     {@link #refreshGradient()}, never per frame.
     */
    ProgressColorChannelSlider(int x, int y, int width, int height, Component message, int initialValue,
        IntConsumer onChange, IntUnaryOperator gradientCompose)
    {
        super(x, y, width, height, message);
        this.value = clamp(initialValue);
        this.onChange = onChange;
        this.gradientCompose = gradientCompose;
    }

    /**
     * Sets this slider's displayed value without invoking {@link #onChange} — the path used when
     * an external control (hex field, wheel) is the source of truth for this change.
     *
     * @param value The new channel value, {@code [0, 255]}.
     */
    void setValueExternally(int value)
    {
        this.value = clamp(value);
    }

    int value()
    {
        return this.value;
    }

    /**
     * Recomputes this slider's per-pixel track gradient from
     * {@link #gradientCompose} — one call per pixel column of the track's
     * width, not per frame. Must be called once after construction (before
     * this widget is first rendered) and again whenever any channel's
     * value changes, including this slider's own.
     */
    void refreshGradient()
    {
        int usableWidth = Math.max(1, getWidth());
        int[] gradient = new int[usableWidth];

        for (int px = 0; px < usableWidth; px++)
        {
            float fraction = usableWidth <= 1 ? 0.0f : px / (float) (usableWidth - 1);
            int channelValue = Math.round(fraction * 255.0f);
            gradient[px] = 0xFF000000 | (gradientCompose.applyAsInt(channelValue) & 0xFFFFFF);
        }

        this.gradientArgb = gradient;
    }

    private static int clamp(int value)
    {
        return Math.max(0, Math.min(255, value));
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        int trackY = getY() + (getHeight() - TRACK_HEIGHT) / 2;

        for (int px = 0; px < gradientArgb.length; px++)
        {
            int columnX = getX() + px;
            guiGraphics.fill(columnX, trackY, columnX + 1, trackY + TRACK_HEIGHT, gradientArgb[px]);
        }

        int usableWidth = getWidth() - HANDLE_WIDTH;
        int handleX = getX() + Math.round(usableWidth * (value / 255.0f));
        guiGraphics.fill(handleX, getY(), handleX + HANDLE_WIDTH, getY() + getHeight(), HANDLE_RGB);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick)
    {
        if (event.button() == 0)
        {
            handlePointer(event.x());
        }
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dragX, double dragY)
    {
        handlePointer(event.x());
    }

    private void handlePointer(double mouseX)
    {
        int usableWidth = getWidth() - HANDLE_WIDTH;
        double localX = mouseX - getX() - HANDLE_WIDTH / 2.0;
        float fraction = usableWidth <= 0 ? 0.0f : (float) (localX / usableWidth);
        this.value = clamp(Math.round(fraction * 255.0f));
        onChange.accept(this.value);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output)
    {
        output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, getMessage());
    }
}
