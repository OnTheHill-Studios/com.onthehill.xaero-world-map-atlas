package com.onthehill.xaeroworldmapbook.client.screen;

import com.onthehill.xaeroworldmapbook.client.ClientModCommands;
import com.onthehill.xaeroworldmapbook.client.XaeroWorldMapBookClient;
import com.onthehill.xaeroworldmapbook.client.config.ClientVisualizationConfig;
import com.onthehill.xaeroworldmapbook.client.config.ProgressColorUtil;
import com.onthehill.xaeroworldmapbook.client.config.ProgressVisualizationMode;
import com.onthehill.xaeroworldmapbook.client.network.ClientNetworkHandler;
import com.onthehill.xaeroworldmapbook.config.ServerProgressConfig;
import com.onthehill.xaeroworldmapbook.network.AdminConfigUpdatePayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The mod's single, consolidated config screen, split into a Client tab and
 * an Admin tab per the studio config standard's authority-line rule. One
 * command opens it on each tab ({@code /xaero-world-map-book gui} → client,
 * {@code /xaero-world-map-book-admin gui} → admin via {@code OpenAdminScreenPayload}),
 * and both construct through the same {@link #createOnClientTab(Screen)} /
 * {@link #createOnAdminTab(Screen)} factory pair — there is deliberately no
 * second, separately-maintained construction path.
 *
 * <p>Layout follows the same fixed-column, independently-centered-groups
 * pattern as the studio's other Fabric mods' config screens (see the
 * {@code com.onthehill.climbing} project's {@code ClimbingConfigScreen} for
 * the reference implementation this was aligned to): every row within the
 * scrollable content column uses fixed pixel widths for its label/control/
 * reset segments so rows always line up into visual columns regardless of
 * label text length, and the tab row / content column / bottom button row
 * are each centered independently against {@code this.width / 2} rather than
 * as one combined tree — the earlier combined-tree approach caused the tab
 * and bottom buttons to visibly shift left/right between tabs whenever the
 * active tab's content happened to be a different width than the other tab's.
 */
public final class ProgressConfigScreen extends Screen
{
    private enum Tab
    {
        CLIENT,
        ADMIN
    }

    private static final int ROW_WIDTH = 280;
    private static final int ROW_HEIGHT = 20;
    private static final int RESET_WIDTH = ROW_HEIGHT;

    /**
     * Minimum gap between adjacent controls in the same row/column, per
     * {@code minecraft-gui-standards.md}'s Margins &amp; Spacing section.
     */
    private static final int INTRA_ROW_SPACING = 4;

    /**
     * Gap between logically distinct groups (e.g. the tab row, the tab
     * content, and the Save/Close row), per the same standard.
     */
    private static final int GROUP_SPACING = 8;

    /** Full-row control width for a single-control row (a toggle/cycle field). */
    private static final int CYCLE_WIDTH = ROW_WIDTH - RESET_WIDTH - INTRA_ROW_SPACING;

    // Color field: a swatch (square, matching ROW_HEIGHT) plus a hex-display
    // button, together filling CYCLE_WIDTH — same proportions as
    // com.onthehill.climbing's addClientColorField.
    private static final int COLOR_SWATCH_SIZE = ROW_HEIGHT;
    private static final int COLOR_BUTTON_WIDTH = CYCLE_WIDTH - COLOR_SWATCH_SIZE - INTRA_ROW_SPACING;

    // The rate row's label and current-value readout are two separate,
    // narrow, fixed-width widgets — not one wide StringWidget combining
    // "Progress rate per tick: 0.020" — specifically because StringWidget
    // never wraps; a long combined label+value string got truncated at a
    // fixed width no matter how that width was tuned. Two short widgets
    // both fit comfortably and stay readable regardless of the value's
    // current digit count.
    private static final int RATE_LABEL_WIDTH = 50;
    private static final int RATE_VALUE_WIDTH = 60;
    private static final int RATE_STEP_BUTTON_WIDTH = 40;

    private static final int TAB_BUTTON_WIDTH = 100;
    private static final int BOTTOM_BUTTON_WIDTH = 110;
    private static final int BOTTOM_BUTTON_SPACING = 8;

    /** Where the scrollable content column starts, vertically: below the tab row and its margin. */
    private static final int CONTENT_TOP = 8 + ROW_HEIGHT + GROUP_SPACING;

    /** Reserved vertical space below the content column for the bottom button row and its margin. */
    private static final int BOTTOM_RESERVED = GROUP_SPACING + ROW_HEIGHT + 8;

    private final Screen parent;
    private Tab activeTab;

    private final ClientVisualizationConfig clientConfig;
    private ProgressVisualizationMode pendingVisualizationMode;
    private ProgressVisualizationMode lastSavedVisualizationMode;
    private String pendingVisualizationColor;
    private String lastSavedVisualizationColor;

    private float pendingProgressRate;
    private boolean pendingAllowNonOpReadOnlyView;
    private float lastSyncedProgressRate;
    private boolean lastSyncedAllowNonOpReadOnlyView;

    private ProgressConfigScreen(Screen parent, Tab initialTab)
    {
        super(Component.translatable("gui.xaero-world-map-book.config.title"));
        this.parent = parent;
        this.activeTab = initialTab;

        // The shared instance XaeroWorldMapBookClient/ProgressHudRenderer read
        // from — not a freshly loaded separate copy. A separately-loaded
        // copy can still be saved to disk correctly, but the already-running
        // HUD renderer holds its own distinct reference and would never see
        // a change made only to a separate object.
        this.clientConfig = XaeroWorldMapBookClient.getClientConfig();
        this.pendingVisualizationMode = clientConfig.getVisualizationMode();
        this.lastSavedVisualizationMode = clientConfig.getVisualizationMode();
        this.pendingVisualizationColor = clientConfig.getVisualizationColorHex();
        this.lastSavedVisualizationColor = clientConfig.getVisualizationColorHex();

        this.lastSyncedProgressRate = ClientNetworkHandler.getAdminProgressRate();
        this.lastSyncedAllowNonOpReadOnlyView = ClientNetworkHandler.isAdminAllowNonOpReadOnlyView();
        this.pendingProgressRate = this.lastSyncedProgressRate;
        this.pendingAllowNonOpReadOnlyView = this.lastSyncedAllowNonOpReadOnlyView;
    }

    /**
     * Opens the screen on the client tab. Used by {@code /xaero-world-map-book gui}
     * and as the base construction path for a future Mod Menu integration.
     *
     * @param parent Screen to return to on close.
     * @return A new screen instance positioned on the client tab.
     */
    public static ProgressConfigScreen createOnClientTab(Screen parent)
    {
        return new ProgressConfigScreen(parent, Tab.CLIENT);
    }

    /**
     * Opens the screen on the admin tab. Used by the client-side handler for
     * {@code OpenAdminScreenPayload}, sent in response to
     * {@code /xaero-world-map-book-admin gui}.
     *
     * @param parent Screen to return to on close.
     * @return A new screen instance positioned on the admin tab.
     */
    public static ProgressConfigScreen createOnAdminTab(Screen parent)
    {
        return new ProgressConfigScreen(parent, Tab.ADMIN);
    }

    @Override
    protected void init()
    {
        rebuild();
    }

    private void rebuild()
    {
        clearWidgets();
        buildTabRow();
        buildContent();
        buildBottomRow();
    }

    private void switchTab(Tab tab)
    {
        this.activeTab = tab;
        rebuild();
    }

    /**
     * Two fixed-width buttons, centered as their own two-button-wide group
     * against {@code this.width / 2} — independent of the content column's
     * width, so this row's screen position never shifts when the active tab
     * (and therefore the content column's height/contents) changes.
     */
    private void buildTabRow()
    {
        int left = this.width / 2 - TAB_BUTTON_WIDTH;

        addRenderableWidget(Button.builder(
            Component.translatable("gui.xaero-world-map-book.config.tab_client"),
            button -> switchTab(Tab.CLIENT)
        ).bounds(left, 8, TAB_BUTTON_WIDTH, ROW_HEIGHT)
            .tooltip(Tooltip.create(Component.translatable("gui.xaero-world-map-book.config.tab_client_tooltip")))
            .build());

        addRenderableWidget(Button.builder(
            Component.translatable("gui.xaero-world-map-book.config.tab_admin"),
            button -> switchTab(Tab.ADMIN)
        ).bounds(left + TAB_BUTTON_WIDTH, 8, TAB_BUTTON_WIDTH, ROW_HEIGHT)
            .tooltip(Tooltip.create(Component.translatable("gui.xaero-world-map-book.config.tab_admin_tooltip")))
            .build());
    }

    /**
     * Save (left) immediately next to Close (right), as one fixed-width
     * two-button group centered against {@code this.width / 2} — same
     * independent-centering rationale as {@link #buildTabRow()}.
     */
    private void buildBottomRow()
    {
        int totalWidth = BOTTOM_BUTTON_WIDTH * 2 + BOTTOM_BUTTON_SPACING;
        int left = this.width / 2 - totalWidth / 2;
        int y = this.height - 8 - ROW_HEIGHT;

        addRenderableWidget(Button.builder(Component.translatable("gui.xaero-world-map-book.config.save"), button -> save())
            .bounds(left, y, BOTTOM_BUTTON_WIDTH, ROW_HEIGHT)
            .tooltip(Tooltip.create(Component.translatable("gui.xaero-world-map-book.config.save_tooltip")))
            .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.xaero-world-map-book.config.close"), button -> onClose())
            .bounds(left + BOTTOM_BUTTON_WIDTH + BOTTOM_BUTTON_SPACING, y, BOTTOM_BUTTON_WIDTH, ROW_HEIGHT)
            .tooltip(Tooltip.create(Component.translatable("gui.xaero-world-map-book.config.close_tooltip")))
            .build());
    }

    private void buildContent()
    {
        LinearLayout content = LinearLayout.vertical().spacing(GROUP_SPACING);

        if (this.activeTab == Tab.CLIENT)
        {
            buildClientTab(content);
        }
        else
        {
            buildAdminTab(content);
        }

        placeContent(content);
    }

    /**
     * Arranges {@code content} (must happen before wrapping — see
     * {@code minecraft-gui-standards.md}'s Layout Container Semantics rule:
     * a scrollable wrapper snapshots child positions at wrap time, and
     * wrapping still-unarranged content produces a permanently broken scroll
     * region no later arrangement pass elsewhere can fix), wraps it in a
     * {@link ScrollableLayout} sized to whatever vertical space is actually
     * left between the tab row and the bottom row at the screen's *current*
     * height, and centers the fixed-width column against
     * {@code this.width / 2} — independent of the tab row/bottom row's own
     * centering math, per this class's javadoc.
     */
    private void placeContent(LinearLayout content)
    {
        content.arrangeElements();

        int availableHeight = Math.max(ROW_HEIGHT, this.height - CONTENT_TOP - BOTTOM_RESERVED);
        ScrollableLayout scrollable = new ScrollableLayout(this.minecraft, content, availableHeight);
        scrollable.setX(this.width / 2 - ROW_WIDTH / 2);
        scrollable.setY(CONTENT_TOP);
        scrollable.arrangeElements();
        scrollable.visitWidgets(this::addRenderableWidget);
    }

    private void centeredMessage(LinearLayout content, Component message)
    {
        StringWidget widget = new StringWidget(ROW_WIDTH, ROW_HEIGHT, message, this.font);
        widget.setTooltip(Tooltip.create(message));
        content.addChild(widget);
    }

    private void buildClientTab(LinearLayout content)
    {
        CycleButton<ProgressVisualizationMode> visualizationControl = CycleButton.builder(
                ProgressConfigScreen::visualizationLabel, this.pendingVisualizationMode)
            .withValues(ProgressVisualizationMode.values())
            .create(0, 0, CYCLE_WIDTH, ROW_HEIGHT,
                Component.translatable("gui.xaero-world-map-book.config.visualization_label"),
                (button, newValue) ->
                {
                    this.pendingVisualizationMode = newValue;
                    rebuild();
                });
        visualizationControl.setTooltip(Tooltip.create(
            Component.translatable("gui.xaero-world-map-book.config.visualization_tooltip")));

        Runnable resetVisualization = () -> this.pendingVisualizationMode = ClientVisualizationConfig.DEFAULT_VISUALIZATION_MODE;
        content.addChild(fieldRow(visualizationControl, resetVisualization));

        content.addChild(buildColorRow());

        content.addChild(sectionResetRow(() ->
        {
            resetVisualization.run();
            this.pendingVisualizationColor = ClientVisualizationConfig.DEFAULT_VISUALIZATION_COLOR;
            rebuild();
        }));
    }

    /**
     * The visualization fill color field: a live swatch plus a hex-display
     * button, both of which open {@link ProgressColorPickerScreen} on
     * click — per {@code minecraft-gui-standards.md}'s Color Configuration
     * Fields rule (a live visual swatch, not just a raw hex value) and its
     * general tooltip rule (every clickable surface in the row routes to
     * the same picker, not just one of them). Selecting a color in the
     * picker and pressing "Done" writes the result straight into
     * {@link #pendingVisualizationColor} and rebuilds, so it participates
     * in this screen's existing dirty-tracking/reset-button system exactly
     * like any other field edit.
     */
    private LinearLayout buildColorRow()
    {
        LinearLayout row = LinearLayout.horizontal().spacing(INTRA_ROW_SPACING);

        Runnable openPicker = () -> ProgressColorPickerScreen.open(this, this.pendingVisualizationColor, hex ->
        {
            this.pendingVisualizationColor = hex;
            rebuild();
        });

        ProgressColorSwatchWidget swatch = new ProgressColorSwatchWidget(0, 0, COLOR_SWATCH_SIZE, COLOR_SWATCH_SIZE,
            () -> ProgressColorUtil.parseHexRgb(this.pendingVisualizationColor,
                ProgressColorUtil.parseHexRgb(ClientVisualizationConfig.DEFAULT_VISUALIZATION_COLOR, 0x55FF55)),
            openPicker);
        swatch.setTooltip(Tooltip.create(Component.translatable("gui.xaero-world-map-book.config.visualization_color_tooltip")));
        row.addChild(swatch);

        Button colorButton = Button.builder(
            Component.translatable("gui.xaero-world-map-book.config.visualization_color_label"),
            button -> openPicker.run()
        ).bounds(0, 0, COLOR_BUTTON_WIDTH, ROW_HEIGHT)
            .tooltip(Tooltip.create(Component.translatable("gui.xaero-world-map-book.config.visualization_color_tooltip")))
            .build();
        row.addChild(colorButton);

        Runnable resetColor = () -> this.pendingVisualizationColor = ClientVisualizationConfig.DEFAULT_VISUALIZATION_COLOR;
        row.addChild(resetButton(() ->
        {
            resetColor.run();
            rebuild();
        }, true));

        return row;
    }

    private void buildAdminTab(LinearLayout content)
    {
        boolean viewerIsOperator = ClientNetworkHandler.isAdminOperator();
        boolean permitted = ClientNetworkHandler.isAdminPermitted();

        if (!ClientNetworkHandler.isAdminSyncReceived())
        {
            centeredMessage(content, Component.translatable("gui.xaero-world-map-book.config.admin_unavailable"));
            return;
        }

        if (!viewerIsOperator && !permitted)
        {
            centeredMessage(content, Component.translatable("gui.xaero-world-map-book.config.admin_no_permission"));
            return;
        }

        if (!viewerIsOperator)
        {
            centeredMessage(content, Component.translatable("gui.xaero-world-map-book.config.admin_read_only_note"));
        }

        content.addChild(buildRateRow(viewerIsOperator));

        CycleButton<Boolean> readOnlyToggle = CycleButton.onOffBuilder(this.pendingAllowNonOpReadOnlyView)
            .create(0, 0, CYCLE_WIDTH, ROW_HEIGHT,
                Component.translatable("gui.xaero-world-map-book.config.allow_read_only_label"),
                (button, newValue) ->
                {
                    this.pendingAllowNonOpReadOnlyView = newValue;
                    rebuild();
                });
        readOnlyToggle.active = viewerIsOperator;
        readOnlyToggle.setTooltip(Tooltip.create(
            Component.translatable("gui.xaero-world-map-book.config.allow_read_only_tooltip")));

        Runnable resetReadOnly = () -> this.pendingAllowNonOpReadOnlyView = ServerProgressConfig.DEFAULT_ALLOW_NON_OP_READ_ONLY_VIEW;
        content.addChild(fieldRow(readOnlyToggle, resetReadOnly, viewerIsOperator));

        boolean interactive = viewerIsOperator;
        content.addChild(sectionResetRow(() ->
        {
            this.pendingProgressRate = ServerProgressConfig.DEFAULT_PROGRESS_RATE;
            resetReadOnly.run();
            rebuild();
        }, interactive));
    }

    private LinearLayout buildRateRow(boolean interactive)
    {
        LinearLayout row = LinearLayout.horizontal().spacing(INTRA_ROW_SPACING);

        // Per minecraft-gui-standards.md's Config Field Tooltips section: the
        // label's tooltip describes the field and names its JSON key; the
        // value/control's tooltip states the unit. Two separate tooltips on
        // two separate widgets, not one combined tooltip on the row.
        StringWidget label = new StringWidget(RATE_LABEL_WIDTH, ROW_HEIGHT,
            Component.translatable("gui.xaero-world-map-book.config.progress_rate_label"), this.font);
        label.setTooltip(Tooltip.create(
            Component.translatable("gui.xaero-world-map-book.config.progress_rate_label_tooltip")));
        row.addChild(label);

        StringWidget value = new StringWidget(RATE_VALUE_WIDTH, ROW_HEIGHT,
            Component.literal(String.valueOf(this.pendingProgressRate)), this.font);
        value.setTooltip(Tooltip.create(
            Component.translatable("gui.xaero-world-map-book.config.progress_rate_value_tooltip")));
        row.addChild(value);

        Button decreaseButton = Button.builder(
            Component.translatable("gui.xaero-world-map-book.config.decrease"),
            button -> { this.pendingProgressRate = Math.max(ServerProgressConfig.MIN_PROGRESS_RATE, this.pendingProgressRate - 0.005f); rebuild(); }
        ).bounds(0, 0, RATE_STEP_BUTTON_WIDTH, ROW_HEIGHT)
            .tooltip(Tooltip.create(Component.translatable("gui.xaero-world-map-book.config.decrease_tooltip")))
            .build();
        Button increaseButton = Button.builder(
            Component.translatable("gui.xaero-world-map-book.config.increase"),
            button -> { this.pendingProgressRate = Math.min(ServerProgressConfig.MAX_PROGRESS_RATE, this.pendingProgressRate + 0.005f); rebuild(); }
        ).bounds(0, 0, RATE_STEP_BUTTON_WIDTH, ROW_HEIGHT)
            .tooltip(Tooltip.create(Component.translatable("gui.xaero-world-map-book.config.increase_tooltip")))
            .build();
        decreaseButton.active = interactive;
        increaseButton.active = interactive;
        row.addChild(decreaseButton);
        row.addChild(increaseButton);

        Runnable resetRate = () -> this.pendingProgressRate = ServerProgressConfig.DEFAULT_PROGRESS_RATE;
        // Unlike fieldRow() (which wraps every reset action with rebuild()
        // itself), this row is built by hand, so the wrap has to happen
        // here explicitly — omitting it was the actual bug: the field
        // reset correctly updated pendingProgressRate, but nothing told the
        // screen to redraw, so the old value stayed on screen until some
        // other action (e.g. +/-) triggered the next rebuild().
        row.addChild(resetButton(() ->
        {
            resetRate.run();
            rebuild();
        }, interactive));

        return row;
    }

    /**
     * A single-control row: the control (already sized to {@link #CYCLE_WIDTH}) plus its
     * per-field reset button, together filling {@link #ROW_WIDTH}.
     */
    private LinearLayout fieldRow(AbstractWidget control, Runnable resetAction)
    {
        return fieldRow(control, resetAction, true);
    }

    private LinearLayout fieldRow(AbstractWidget control, Runnable resetAction, boolean interactive)
    {
        LinearLayout row = LinearLayout.horizontal().spacing(INTRA_ROW_SPACING);
        row.addChild(control);
        row.addChild(resetButton(() ->
        {
            resetAction.run();
            rebuild();
        }, interactive));
        return row;
    }

    private LinearLayout sectionResetRow(Runnable resetAction)
    {
        return sectionResetRow(resetAction, true);
    }

    private LinearLayout sectionResetRow(Runnable resetAction, boolean interactive)
    {
        LinearLayout row = LinearLayout.horizontal();
        // Icon reset button (see ProgressConfigResetButton's javadoc), right-aligned to match the
        // per-field reset buttons' column, per fieldRow()'s layout.
        LinearLayout spacer = LinearLayout.horizontal();
        spacer.addChild(new StringWidget(CYCLE_WIDTH, ROW_HEIGHT, Component.empty(), this.font));
        row.addChild(spacer);
        ProgressConfigResetButton reset = ProgressConfigResetButton.create(0, 0, RESET_WIDTH,
            Component.translatable("gui.xaero-world-map-book.config.reset_section"), button -> resetAction.run());
        reset.active = interactive;
        row.addChild(reset);
        return row;
    }

    private ProgressConfigResetButton resetButton(Runnable onPress, boolean interactive)
    {
        ProgressConfigResetButton reset = ProgressConfigResetButton.create(0, 0, RESET_WIDTH,
            Component.translatable("gui.xaero-world-map-book.config.reset_field"), button -> onPress.run());
        reset.active = interactive;
        return reset;
    }

    private static Component visualizationLabel(ProgressVisualizationMode mode)
    {
        return mode == ProgressVisualizationMode.RADIAL
            ? Component.translatable("gui.xaero-world-map-book.config.visualization_radial")
            : Component.translatable("gui.xaero-world-map-book.config.visualization_bar");
    }

    private boolean isDirty()
    {
        boolean clientDirty = this.pendingVisualizationMode != this.lastSavedVisualizationMode
            || !this.pendingVisualizationColor.equals(this.lastSavedVisualizationColor);
        boolean adminDirty = ClientNetworkHandler.isAdminOperator()
            && (this.pendingProgressRate != this.lastSyncedProgressRate
                || this.pendingAllowNonOpReadOnlyView != this.lastSyncedAllowNonOpReadOnlyView);
        return clientDirty || adminDirty;
    }

    private void save()
    {
        if (this.pendingVisualizationMode != this.lastSavedVisualizationMode
            || !this.pendingVisualizationColor.equals(this.lastSavedVisualizationColor))
        {
            this.clientConfig.setVisualizationMode(this.pendingVisualizationMode);
            this.clientConfig.setVisualizationColorHex(this.pendingVisualizationColor);
            this.clientConfig.save(ClientModCommands.clientConfigPath());
            this.lastSavedVisualizationMode = this.pendingVisualizationMode;
            this.lastSavedVisualizationColor = this.pendingVisualizationColor;
        }

        if (ClientNetworkHandler.isAdminOperator()
            && (this.pendingProgressRate != this.lastSyncedProgressRate
                || this.pendingAllowNonOpReadOnlyView != this.lastSyncedAllowNonOpReadOnlyView))
        {
            ClientPlayNetworking.send(new AdminConfigUpdatePayload(this.pendingProgressRate, this.pendingAllowNonOpReadOnlyView));
            // The authoritative baseline updates once the server's
            // AdminConfigSyncPayload confirmation round-trips back; treat the
            // send as optimistically applied here so Save clears the dirty
            // flag immediately rather than waiting on network latency.
            this.lastSyncedProgressRate = this.pendingProgressRate;
            this.lastSyncedAllowNonOpReadOnlyView = this.pendingAllowNonOpReadOnlyView;
        }
    }

    @Override
    public void onClose()
    {
        if (isDirty())
        {
            assert this.minecraft != null;
            this.minecraft.setScreenAndShow(new SaveConfirmScreen(
                () -> { save(); this.minecraft.setScreenAndShow(this.parent); },
                () -> this.minecraft.setScreenAndShow(this.parent),
                () -> this.minecraft.setScreenAndShow(this)
            ));
            return;
        }
        assert this.minecraft != null;
        this.minecraft.setScreenAndShow(this.parent);
    }

    @Override
    protected void updateNarrationState(NarrationElementOutput output)
    {
        output.add(NarratedElementType.TITLE, this.title);
    }
}
