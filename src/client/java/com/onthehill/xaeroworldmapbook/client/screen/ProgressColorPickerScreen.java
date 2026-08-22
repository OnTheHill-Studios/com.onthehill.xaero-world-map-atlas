package com.onthehill.xaeroworldmapbook.client.screen;

import com.onthehill.xaeroworldmapbook.client.config.ProgressColorUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * The full color-picker popup: a player can choose a color by typing a hex
 * value, dragging three R/G/B sliders, or using a hue-ring + saturation/
 * value area, all three live and kept in sync simultaneously.
 *
 * <p>Opened from the visualization color field's control on
 * {@link ProgressConfigScreen}'s Client tab (the swatch or the hex-display
 * button), never from a raw always-editable text field directly. All three
 * input modes are live simultaneously (not a mode switcher) and kept in
 * sync via a single authoritative {@code 0xRRGGBB} int ({@link #currentRgb}):
 * whichever widget's own callback fires calls {@link #applyRgb(int, Object)},
 * which pushes the new value into every <em>other</em> widget (never the one
 * that originated the change, to avoid feedback loops/caret fighting) and
 * refreshes the live preview swatch.
 *
 * <p>"Done" hands the selected color back to {@code onDone} and returns to
 * the owning {@link ProgressConfigScreen}; the owning screen's own handler
 * is responsible for writing it into the pending value and rebuilding,
 * which is what makes a picker selection count as a real edit against that
 * screen's existing dirty-tracking/reset-button system, exactly like typing
 * into a field's own text box would. "Cancel"/Escape returns without
 * invoking {@code onDone} at all.
 *
 * <p>Layout: built as a real {@link LinearLayout} tree — the color wheel in
 * a left column, the hex field, R/G/B sliders, and preview swatch stacked in
 * a right column, and the Done/Cancel row beneath both — arranged once to
 * measure its own total size, then centered on the screen and re-arranged at
 * that position, per {@code minecraft-gui-standards.md}'s "arrange before
 * wrapping/positioning" rule, so the button row's position is always derived
 * from the content's actual measured size rather than an assumed fixed
 * offset.
 */
public final class ProgressColorPickerScreen extends Screen
{
    private static final int SLIDER_WIDTH = 180;
    private static final int SLIDER_HEIGHT = 16;
    private static final int ROW_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 90;
    private static final int HEX_FIELD_WIDTH = 120;
    private static final int COLUMN_SPACING = 16;
    private static final int ROW_SPACING = 6;

    private final Screen owner;
    private final Consumer<String> onDone;
    private int currentRgb;

    private EditBox hexField;
    private ProgressColorSwatchWidget previewSwatch;
    private ProgressColorChannelSlider redSlider;
    private ProgressColorChannelSlider greenSlider;
    private ProgressColorChannelSlider blueSlider;
    private ProgressColorWheelWidget wheel;

    private ProgressColorPickerScreen(Screen owner, String initialHex, Consumer<String> onDone)
    {
        super(Component.translatable("gui.xaero-world-map-book.color_picker.title"));
        this.owner = owner;
        this.onDone = onDone;
        this.currentRgb = ProgressColorUtil.parseHexRgb(initialHex, 0x55FF55);
    }

    /**
     * Opens the color picker over {@code owner}, seeded with {@code initialHex}.
     *
     * @param owner The screen to return to (the {@link ProgressConfigScreen} this was opened from).
     * @param initialHex The color field's current pending value, with or without a leading
     *     {@code #}.
     * @param onDone Invoked with the selected color (always as an uppercase {@code "#RRGGBB"}
     *     string) if the player presses "Done" — never invoked on "Cancel"/Escape.
     */
    public static void open(Screen owner, String initialHex, Consumer<String> onDone)
    {
        Minecraft.getInstance().setScreenAndShow(new ProgressColorPickerScreen(owner, initialHex, onDone));
    }

    @Override
    protected void init()
    {
        LinearLayout root = LinearLayout.vertical().spacing(10);
        root.defaultCellSetting().alignHorizontallyCenter();

        root.addChild(new StringWidget(200, ROW_HEIGHT, Component.translatable("gui.xaero-world-map-book.color_picker.title"), this.font));
        root.addChild(buildColumnsRow());
        root.addChild(buildButtonRow());

        // Per minecraft-gui-standards.md's Layout Container Semantics section: arrange the content
        // once to measure its real size before positioning it, then set its position and arrange
        // again — never trust a pre-arrangement (0,0) snapshot, and never assume a fixed offset fits
        // the current window without measuring the actual tree.
        root.arrangeElements();
        root.setX(this.width / 2 - root.getWidth() / 2);
        root.setY(Math.max(8, this.height / 2 - root.getHeight() / 2));
        root.arrangeElements();
        root.visitWidgets(this::addRenderableWidget);

        syncSliderTrackColors();
    }

    private LinearLayout buildColumnsRow()
    {
        LinearLayout columns = LinearLayout.horizontal().spacing(COLUMN_SPACING);
        columns.defaultCellSetting().alignVerticallyTop();

        this.wheel = new ProgressColorWheelWidget(0, 0, rgb -> applyRgb(rgb, this.wheel));
        this.wheel.setColor(this.currentRgb);
        columns.addChild(this.wheel);

        columns.addChild(buildRightColumn());

        return columns;
    }

    private LinearLayout buildRightColumn()
    {
        LinearLayout column = LinearLayout.vertical().spacing(ROW_SPACING);
        column.defaultCellSetting().alignHorizontallyLeft();

        // Hex mode — accepts with or without a leading '#'. Parsed on every keystroke; a
        // partially-typed/invalid string simply does not push an update yet, rather than fighting
        // the player's cursor mid-edit.
        this.hexField = new EditBox(this.font, 0, 0, HEX_FIELD_WIDTH, ROW_HEIGHT,
            Component.translatable("gui.xaero-world-map-book.color_picker.hex"));
        this.hexField.setMaxLength(7);
        this.hexField.setValue(ProgressColorUtil.toHex(this.currentRgb));
        this.hexField.setResponder(text ->
        {
            if (ProgressColorUtil.isValidHex(text))
            {
                applyRgb(ProgressColorUtil.parseHexRgb(text, this.currentRgb), this.hexField);
            }
        });
        column.addChild(this.hexField);

        this.redSlider = new ProgressColorChannelSlider(0, 0, SLIDER_WIDTH, SLIDER_HEIGHT,
            Component.translatable("gui.xaero-world-map-book.color_picker.red"), (this.currentRgb >> 16) & 0xFF,
            value -> applyRgb(composeRgb(value, this.greenSlider.value(), this.blueSlider.value()), this.redSlider),
            value -> composeRgb(value, this.greenSlider.value(), this.blueSlider.value()));
        column.addChild(this.redSlider);

        this.greenSlider = new ProgressColorChannelSlider(0, 0, SLIDER_WIDTH, SLIDER_HEIGHT,
            Component.translatable("gui.xaero-world-map-book.color_picker.green"), (this.currentRgb >> 8) & 0xFF,
            value -> applyRgb(composeRgb(this.redSlider.value(), value, this.blueSlider.value()), this.greenSlider),
            value -> composeRgb(this.redSlider.value(), value, this.blueSlider.value()));
        column.addChild(this.greenSlider);

        this.blueSlider = new ProgressColorChannelSlider(0, 0, SLIDER_WIDTH, SLIDER_HEIGHT,
            Component.translatable("gui.xaero-world-map-book.color_picker.blue"), this.currentRgb & 0xFF,
            value -> applyRgb(composeRgb(this.redSlider.value(), this.greenSlider.value(), value), this.blueSlider),
            value -> composeRgb(this.redSlider.value(), this.greenSlider.value(), value));
        column.addChild(this.blueSlider);

        // Sized to the hex field's own actual height (read from the real widget, not a duplicated
        // constant) so it lines up with the rest of the right column's row height.
        int swatchSize = this.hexField.getHeight();
        this.previewSwatch = new ProgressColorSwatchWidget(0, 0, swatchSize, swatchSize, () -> this.currentRgb, null);
        column.addChild(this.previewSwatch);

        return column;
    }

    private LinearLayout buildButtonRow()
    {
        LinearLayout buttons = LinearLayout.horizontal().spacing(8);

        buttons.addChild(Button.builder(Component.translatable("gui.xaero-world-map-book.color_picker.done"), button ->
            {
                this.onDone.accept(ProgressColorUtil.toHex(this.currentRgb));
                Minecraft.getInstance().setScreenAndShow(this.owner);
            })
            .bounds(0, 0, BUTTON_WIDTH, ROW_HEIGHT)
            .build());

        buttons.addChild(Button.builder(Component.translatable("gui.xaero-world-map-book.color_picker.cancel"), button ->
                Minecraft.getInstance().setScreenAndShow(this.owner))
            .bounds(0, 0, BUTTON_WIDTH, ROW_HEIGHT)
            .build());

        return buttons;
    }

    private static int composeRgb(int r, int g, int b)
    {
        return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    /**
     * The single choke point every input mode's callback routes through. Updates
     * {@link #currentRgb} and pushes the new value into every widget except {@code source} (the
     * widget whose own callback triggered this call), then refreshes the sliders' own gradient
     * tracks. Never touches {@link #previewSwatch} directly — that widget reads {@link #currentRgb}
     * live via its own supplier every frame, per {@code minecraft-gui-standards.md}'s Color
     * Configuration Fields rule.
     *
     * @param rgb The newly selected packed {@code 0xRRGGBB} color.
     * @param source The widget instance that originated this change, or {@code null} if none.
     */
    private void applyRgb(int rgb, Object source)
    {
        this.currentRgb = rgb & 0xFFFFFF;

        if (source != this.hexField)
        {
            this.hexField.setValue(ProgressColorUtil.toHex(this.currentRgb));
        }

        if (source != this.redSlider)
        {
            this.redSlider.setValueExternally((this.currentRgb >> 16) & 0xFF);
        }

        if (source != this.greenSlider)
        {
            this.greenSlider.setValueExternally((this.currentRgb >> 8) & 0xFF);
        }

        if (source != this.blueSlider)
        {
            this.blueSlider.setValueExternally(this.currentRgb & 0xFF);
        }

        if (source != this.wheel)
        {
            this.wheel.setColor(this.currentRgb);
        }

        syncSliderTrackColors();
    }

    /**
     * Refreshes each slider's own per-channel gradient track — recomputed on every change (not per
     * frame; called only from {@link #applyRgb(int, Object)} and once at {@link #init()}'s end).
     */
    private void syncSliderTrackColors()
    {
        this.redSlider.refreshGradient();
        this.greenSlider.refreshGradient();
        this.blueSlider.refreshGradient();
    }

    @Override
    protected void updateNarrationState(NarrationElementOutput builder)
    {
        builder.add(NarratedElementType.TITLE, this.title);
    }

    @Override
    public void onClose()
    {
        // Escape acts as Cancel — never silently applies a color the player didn't explicitly
        // confirm with "Done".
        Minecraft.getInstance().setScreenAndShow(this.owner);
    }
}
