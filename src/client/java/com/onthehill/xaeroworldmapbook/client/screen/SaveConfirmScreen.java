package com.onthehill.xaeroworldmapbook.client.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Confirm-on-close prompt shown when {@link ProgressConfigScreen} is closed
 * while dirty. Offers exactly the three actions required by the studio
 * config standard — "Save and Close," "Close without Saving," and a
 * cancel that returns to the still-open config screen with every pending
 * edit intact — no more and no fewer.
 *
 * <p>Layout mirrors {@code com.onthehill.climbing}'s
 * {@code ClimbingConfigConfirmCloseScreen}: a fixed-width button column,
 * each child centered on the layout's cross-axis via
 * {@code defaultCellSetting().alignHorizontallyCenter()} so the (potentially
 * wrapped, per-locale-length-varying) message and the buttons all line up
 * regardless of each one's individual width, arranged before being
 * positioned/read.
 */
public final class SaveConfirmScreen extends Screen
{
    private final Runnable onSaveAndClose;
    private final Runnable onCloseWithoutSaving;
    private final Runnable onCancel;

    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;

    /**
     * Wider than {@link #BUTTON_WIDTH} so the message can wrap without
     * clipping — other locales' translations commonly run longer than
     * English, per {@code minecraft-gui-standards.md}'s Scaling &amp;
     * Readability rule: widen the container, never shrink/truncate text.
     */
    private static final int MESSAGE_MAX_WIDTH = 240;

    /**
     * Gap between the message and the button column, and between buttons,
     * per {@code minecraft-gui-standards.md}'s Margins &amp; Spacing section.
     */
    private static final int GROUP_SPACING = 8;

    public SaveConfirmScreen(Runnable onSaveAndClose, Runnable onCloseWithoutSaving, Runnable onCancel)
    {
        super(Component.translatable("gui.xaero-world-map-book.confirm_save.title"));
        this.onSaveAndClose = onSaveAndClose;
        this.onCloseWithoutSaving = onCloseWithoutSaving;
        this.onCancel = onCancel;
    }

    @Override
    protected void init()
    {
        LinearLayout content = LinearLayout.vertical().spacing(GROUP_SPACING);
        content.defaultCellSetting().alignHorizontallyCenter();

        MultiLineTextWidget messageWidget = new MultiLineTextWidget(
            Component.translatable("gui.xaero-world-map-book.confirm_save.message"), this.font);
        messageWidget.setMaxWidth(MESSAGE_MAX_WIDTH);
        messageWidget.setCentered(true);
        content.addChild(messageWidget);

        content.addChild(Button.builder(
            Component.translatable("gui.xaero-world-map-book.confirm_save.save_and_close"),
            button -> this.onSaveAndClose.run()
        ).bounds(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT)
            .tooltip(Tooltip.create(Component.translatable("gui.xaero-world-map-book.confirm_save.save_and_close_tooltip")))
            .build());

        content.addChild(Button.builder(
            Component.translatable("gui.xaero-world-map-book.confirm_save.close_without_saving"),
            button -> this.onCloseWithoutSaving.run()
        ).bounds(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT)
            .tooltip(Tooltip.create(Component.translatable("gui.xaero-world-map-book.confirm_save.close_without_saving_tooltip")))
            .build());

        content.addChild(Button.builder(
            Component.translatable("gui.xaero-world-map-book.confirm_save.cancel"),
            button -> this.onCancel.run()
        ).bounds(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT)
            .tooltip(Tooltip.create(Component.translatable("gui.xaero-world-map-book.confirm_save.cancel_tooltip")))
            .build());

        // arrangeElements() must run before the tree is read from or
        // positioned — it's what actually computes each child's size and
        // position from the addChild() calls above.
        content.arrangeElements();
        // Center against content's own measured width, not the BUTTON_WIDTH
        // constant — the wrapped MultiLineTextWidget can measure wider than
        // BUTTON_WIDTH (it's allowed up to MESSAGE_MAX_WIDTH), and
        // alignHorizontallyCenter() centers every child within the layout's
        // own actual width, not an assumed one. Centering the layout itself
        // against a narrower assumed width than its real content shifted the
        // whole block visibly off-center.
        content.setX(this.width / 2 - content.getWidth() / 2);
        content.setY(this.height / 2 - content.getHeight() / 2);
        content.visitWidgets(this::addRenderableWidget);
    }

    @Override
    public void onClose()
    {
        // The "X"/escape path on this prompt itself is equivalent to Cancel —
        // it must return to the still-open config screen, never close it.
        this.onCancel.run();
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
