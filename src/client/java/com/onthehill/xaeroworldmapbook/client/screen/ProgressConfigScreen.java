package com.onthehill.xaeroworldmapbook.client.screen;

import com.onthehill.xaeroworldmapbook.client.network.MapAccessClientNetworkHandler;
import com.onthehill.xaeroworldmapbook.config.KeybindItemRequirement;
import com.onthehill.xaeroworldmapbook.config.MapAccessConfig;
import com.onthehill.xaeroworldmapbook.config.MinimapAccessRequirement;
import com.onthehill.xaeroworldmapbook.config.MinimapAdvancementGate;
import com.onthehill.xaeroworldmapbook.network.MapAccessConfigUpdatePayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The mod's single, consolidated config screen — every server-authoritative field {@link MapAccessConfig} defines,
 * since that is this mod's only remaining config object (the earlier example tick-progress feature and its
 * separate client tab were removed as leftover template scaffolding). One command opens it
 * ({@code /xaero-world-map-book gui} for any player, {@code /xaero-world-map-book-admin gui} via
 * {@code OpenAdminScreenPayload} for the same screen through the op-gated route), both routing through the same
 * {@link #create(Screen)} factory — there is deliberately no second, separately-maintained construction path.
 *
 * <p>Per the studio config standard's mandatory read-only-view rule, a non-operator viewer sees every field with
 * its real synced value but every control disabled, or — if the server's {@code allowNonOpReadOnlyView} setting is
 * off — sees no settings at all.
 *
 * <p>Layout follows the same fixed-column, independently-centered-groups pattern as the studio's other Fabric
 * mods' config screens (see the {@code com.onthehill.climbing} project's {@code ClimbingConfigScreen} for the
 * reference implementation this was aligned to), sized against the screen's own current dimensions rather than a
 * fixed pixel column — see {@link #computeLayout()} — so every row's label/control/reset segments always line up
 * into visual columns, using the same widths for every row: the label always leftmost, the control always the
 * same width immediately after it, and every row's reset button landing at the exact same X coordinate so every
 * reset button in the screen lines up into one vertical column on the right edge. A row's label is the portion of
 * text that never changes for that field (e.g. "Keybind Item Requirement"); the control next to it shows only the
 * current value (e.g. "Hotbar"), via {@link CycleButton.Builder#displayOnlyValue()}, rather than baking both into
 * one combined "Label: Value" button — the label itself is a {@link ProgressConfigScrollingLabel}, which scrolls a
 * label that doesn't fit its column only while the mouse hovers over it, clipping with an ellipsis otherwise.
 */
public final class ProgressConfigScreen extends Screen
{
    private static final int ROW_HEIGHT = 20;
    private static final int RESET_WIDTH = ROW_HEIGHT;

    /**
     * Minimum gap between adjacent controls in the same row/column, per
     * {@code minecraft-gui-standards.md}'s Margins &amp; Spacing section.
     */
    private static final int INTRA_ROW_SPACING = 4;

    /**
     * Gap between logically distinct groups (e.g. a section heading and the row above it), per the same standard.
     */
    private static final int GROUP_SPACING = 8;

    /**
     * Fixed width every field row's label column occupies — the static, never-changing portion of a row's text
     * (e.g. "Keybind Item Requirement"), left-aligned as the row's first child. Deliberately narrower than the
     * longest label actually needs; {@link ProgressConfigScrollingLabel}'s hover-scrolling handles the overflow
     * instead of growing this column to fit the longest string, which would waste horizontal space on every
     * shorter-labeled row. Fixed rather than proportional to the screen's width — see {@link #computeLayout()}'s
     * own Javadoc for why a proportional label column was the wrong fix for "use more of the screen."
     */
    private static final int LABEL_WIDTH = 140;

    /** Fixed width every field row's value control occupies, after the flexible gap and before the reset button. */
    private static final int CONTROL_WIDTH = 160;

    /**
     * Fraction of the screen's own width/height, respectively, left empty as margin on every side — per the
     * project owner's explicit request that the screen use "the whole screen available except for a 10% margin."
     */
    private static final float SCREEN_MARGIN_FRACTION = 0.10f;

    /**
     * Extra vertical gap inserted after a section's own fields, before the next section's heading — per the
     * project owner's explicit correction: the extra breathing room between sections belongs at the end of the
     * section that just finished, not baked into the next heading's own padding (which put the visible extra gap
     * on the wrong side — between a heading and its own divider, rather than between the previous section and that
     * heading) and would otherwise give the very first section a gap above it that no earlier section produced.
     */
    private static final int SECTION_END_GAP = 12;

    private static final int BOTTOM_BUTTON_WIDTH = 110;
    private static final int BOTTOM_BUTTON_SPACING = 8;

    // The following are recomputed by computeLayout() every rebuild() — not
    // static finals — since they scale with the screen's current width/height
    // rather than being fixed pixel counts, so a window resize (which
    // triggers Screen#init() again) produces a freshly-fitted layout instead
    // of a stale one anchored to whatever size the screen last opened at.
    private int rowWidth;
    private int fieldGapWidth;
    private int contentTop;
    private int contentBottom;

    private final Screen parent;

    private boolean pendingKeybindOpenEnabled;
    private KeybindItemRequirement pendingKeybindItemRequirement;
    private MinimapAccessRequirement pendingMinimapAccessRequirement;
    private int pendingChunksRequiredForWellTraveled;
    private MinimapAdvancementGate pendingMinimapAdvancementGate;
    private boolean pendingCreativeBypassEnabled;
    private boolean pendingAllowNonOpReadOnlyView;
    private boolean lastSyncedKeybindOpenEnabled;
    private KeybindItemRequirement lastSyncedKeybindItemRequirement;
    private MinimapAccessRequirement lastSyncedMinimapAccessRequirement;
    private int lastSyncedChunksRequiredForWellTraveled;
    private MinimapAdvancementGate lastSyncedMinimapAdvancementGate;
    private boolean lastSyncedCreativeBypassEnabled;
    private boolean lastSyncedAllowNonOpReadOnlyView;

    /** Tracks the chunk-count field's raw text so a mid-typing invalid/empty value doesn't clobber the pending int. */
    private String chunkCountFieldText;

    /**
     * The scrollable content area's own scroll widget from the most recent {@link #placeContent} call, and the
     * scroll offset to restore into the next one. Editing a field calls {@link #rebuild()}, which tears down and
     * recreates every widget including a brand-new {@link ScrollableLayout} — without explicitly saving and
     * restoring the offset across that recreation, every single field edit silently snapped the scroll position
     * back to the top, since a freshly-constructed scroll widget always starts at zero.
     */
    private AbstractScrollArea scrollContainer;
    private double savedScrollAmount = 0;

    private ProgressConfigScreen(Screen parent)
    {
        super(Component.translatable("gui.xaero-world-map-book.config.title"));
        this.parent = parent;

        MapAccessConfig lastSyncedMapAccess = MapAccessClientNetworkHandler.getLastSyncedConfigOrDefaults();
        this.lastSyncedKeybindOpenEnabled = lastSyncedMapAccess.isKeybindOpenEnabled();
        this.lastSyncedKeybindItemRequirement = lastSyncedMapAccess.getKeybindItemRequirement();
        this.lastSyncedMinimapAccessRequirement = lastSyncedMapAccess.getMinimapAccessRequirement();
        this.lastSyncedChunksRequiredForWellTraveled = lastSyncedMapAccess.getChunksRequiredForWellTraveled();
        this.lastSyncedMinimapAdvancementGate = lastSyncedMapAccess.getMinimapAdvancementGate();
        this.lastSyncedCreativeBypassEnabled = lastSyncedMapAccess.isCreativeBypassEnabled();
        this.lastSyncedAllowNonOpReadOnlyView = lastSyncedMapAccess.isAllowNonOpReadOnlyView();
        this.pendingKeybindOpenEnabled = this.lastSyncedKeybindOpenEnabled;
        this.pendingKeybindItemRequirement = this.lastSyncedKeybindItemRequirement;
        this.pendingMinimapAccessRequirement = this.lastSyncedMinimapAccessRequirement;
        this.pendingChunksRequiredForWellTraveled = this.lastSyncedChunksRequiredForWellTraveled;
        this.pendingMinimapAdvancementGate = this.lastSyncedMinimapAdvancementGate;
        this.pendingCreativeBypassEnabled = this.lastSyncedCreativeBypassEnabled;
        this.pendingAllowNonOpReadOnlyView = this.lastSyncedAllowNonOpReadOnlyView;
        this.chunkCountFieldText = String.valueOf(this.pendingChunksRequiredForWellTraveled);
    }

    /**
     * Opens the screen. Used by both {@code /xaero-world-map-book gui} and the client-side handler for
     * {@code OpenAdminScreenPayload} (sent in response to {@code /xaero-world-map-book-admin gui}) — there is only
     * ever this one construction path.
     *
     * @param parent Screen to return to on close.
     * @return A new screen instance.
     */
    public static ProgressConfigScreen create(Screen parent)
    {
        return new ProgressConfigScreen(parent);
    }

    @Override
    protected void init()
    {
        rebuild();
    }

    private void rebuild()
    {
        if (this.scrollContainer != null)
        {
            this.savedScrollAmount = this.scrollContainer.scrollAmount();
        }
        computeLayout();
        clearWidgets();
        buildContent();
        buildBottomRow();
    }

    /**
     * Recomputes every size/position that scales with the screen's current dimensions. Per the project owner's
     * explicit request, the screen uses the full available space except for a 10% margin on every side: a usable
     * band running from 10%-of-width to 90%-of-width horizontally, and 10%-of-height to 90%-of-height vertically —
     * flexing with the window's actual current size/aspect ratio rather than a fixed pixel column, so a wide window
     * gets a wide screen and a narrow one still gets its full proportional share.
     * <p>
     * A wider {@link #rowWidth} does <em>not</em> widen {@link #LABEL_WIDTH} or {@link #CONTROL_WIDTH} — an earlier
     * version of this screen made the label column a fixed <em>fraction</em> of the row width, which grew the
     * label's empty box far past its actual text on a wide window (the short label text stayed left-aligned inside
     * that box, visually "stuck" near the left edge) while the value control and reset button, whose start
     * position is computed from the label column's width, did not reliably end up flush against the row's true
     * right edge either. Fixing both columns' widths and putting 100% of any extra row width into
     * {@link #fieldGapWidth} — a spacer between the label and the value control — makes the label anchor to the
     * row's left edge and the value control + reset button anchor to the row's right edge, with only the gap
     * between them growing or shrinking as the window resizes.
     */
    private void computeLayout()
    {
        int marginX = Math.round(this.width * SCREEN_MARGIN_FRACTION);
        int marginY = Math.round(this.height * SCREEN_MARGIN_FRACTION);

        this.rowWidth = this.width - marginX * 2;
        this.fieldGapWidth = Math.max(0,
            this.rowWidth - LABEL_WIDTH - CONTROL_WIDTH - RESET_WIDTH - INTRA_ROW_SPACING * 3);
        this.contentTop = marginY;
        this.contentBottom = this.height - marginY;
    }

    private void buildBottomRow()
    {
        int totalWidth = BOTTOM_BUTTON_WIDTH * 2 + BOTTOM_BUTTON_SPACING;
        int left = this.width / 2 - totalWidth / 2;
        int y = this.contentBottom - ROW_HEIGHT;

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
        buildFields(content);
        placeContent(content);
    }

    /**
     * Arranges {@code content} (must happen before wrapping — see
     * {@code minecraft-gui-standards.md}'s Layout Container Semantics rule:
     * a scrollable wrapper snapshots child positions at wrap time, and
     * wrapping still-unarranged content produces a permanently broken scroll
     * region no later arrangement pass elsewhere can fix), wraps it in a
     * {@link ScrollableLayout} sized to whatever vertical space is actually
     * left below the top margin at the screen's *current* height, and
     * centers the fixed-width column against {@code this.width / 2}.
     */
    private void placeContent(LinearLayout content)
    {
        content.arrangeElements();

        int bottomReserved = this.height - this.contentBottom + GROUP_SPACING + ROW_HEIGHT;
        int availableHeight = Math.max(ROW_HEIGHT, this.height - this.contentTop - bottomReserved);
        ScrollableLayout scrollable = new ScrollableLayout(this.minecraft, content, availableHeight);
        scrollable.setX(this.width / 2 - this.rowWidth / 2);
        scrollable.setY(this.contentTop);
        scrollable.arrangeElements();
        scrollable.visitWidgets(widget ->
        {
            addRenderableWidget(widget);
            if (widget instanceof AbstractScrollArea area)
            {
                area.setScrollAmount(this.savedScrollAmount);
                this.scrollContainer = area;
            }
        });
    }

    private void centeredMessage(LinearLayout content, Component message)
    {
        StringWidget widget = scrollingStringWidget(this.rowWidth, message);
        widget.setTooltip(Tooltip.create(message));
        content.addChild(widget);
    }

    /**
     * A section heading: centered text (so it visually reads as a heading rather than another left-aligned field
     * label), followed by the divider line. The extra breathing room that visually separates one section from the
     * next is added by {@link #sectionEndGap} after the <em>previous</em> section's fields, not here — see that
     * method's Javadoc.
     */
    private void sectionHeading(LinearLayout content, Component heading)
    {
        content.addChild(new ProgressConfigSectionHeader(this.rowWidth, ROW_HEIGHT, heading, this.font));
        content.addChild(new ProgressConfigSectionDivider(this.rowWidth));
    }

    /**
     * The extra vertical gap after a section's fields, before whatever comes next (the next section's heading, or
     * the shared settings below the last section) — see {@link #SECTION_END_GAP}'s own Javadoc for why it belongs
     * here rather than baked into {@link #sectionHeading}.
     */
    private void sectionEndGap(LinearLayout content)
    {
        content.addChild(new ProgressConfigSpacer(this.rowWidth, SECTION_END_GAP));
    }

    private ProgressConfigScrollingLabel scrollingStringWidget(int width, Component message)
    {
        return new ProgressConfigScrollingLabel(width, ROW_HEIGHT, message, this.font);
    }

    private void buildFields(LinearLayout content)
    {
        boolean viewerIsOperator = MapAccessClientNetworkHandler.isOperator();
        boolean permitted = MapAccessClientNetworkHandler.isPermitted();

        if (!MapAccessClientNetworkHandler.isSyncReceived())
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

        boolean interactive = viewerIsOperator;

        sectionHeading(content, Component.translatable("gui.xaero-world-map-book.mapaccess.section_world_map"));
        buildWorldMapFields(content, interactive);
        sectionEndGap(content);

        sectionHeading(content, Component.translatable("gui.xaero-world-map-book.mapaccess.section_minimap"));
        buildMinimapFields(content, interactive);
        sectionEndGap(content);

        content.addChild(new ProgressConfigSectionDivider(this.rowWidth));

        CycleButton<Boolean> readOnlyToggle = CycleButton.onOffBuilder(this.pendingAllowNonOpReadOnlyView)
            .displayOnlyValue()
            .create(0, 0, CONTROL_WIDTH, ROW_HEIGHT,
                Component.translatable("gui.xaero-world-map-book.config.allow_read_only_label"),
                (button, newValue) -> { this.pendingAllowNonOpReadOnlyView = newValue; rebuild(); });
        readOnlyToggle.active = interactive;
        readOnlyToggle.setTooltip(Tooltip.create(
            Component.translatable("gui.xaero-world-map-book.config.allow_read_only_tooltip")));
        content.addChild(fieldRow(
            Component.translatable("gui.xaero-world-map-book.config.allow_read_only_label"), readOnlyToggle,
            () -> this.pendingAllowNonOpReadOnlyView = MapAccessConfig.DEFAULT_ALLOW_NON_OP_READ_ONLY_VIEW, interactive));

        content.addChild(resetAllRow(() ->
        {
            this.pendingKeybindOpenEnabled = MapAccessConfig.DEFAULT_KEYBIND_OPEN_ENABLED;
            this.pendingKeybindItemRequirement = MapAccessConfig.DEFAULT_KEYBIND_ITEM_REQUIREMENT;
            this.pendingCreativeBypassEnabled = MapAccessConfig.DEFAULT_CREATIVE_BYPASS_ENABLED;
            this.pendingMinimapAccessRequirement = MapAccessConfig.DEFAULT_MINIMAP_ACCESS_REQUIREMENT;
            this.pendingChunksRequiredForWellTraveled = MapAccessConfig.DEFAULT_CHUNKS_REQUIRED_FOR_WELL_TRAVELED;
            this.chunkCountFieldText = String.valueOf(this.pendingChunksRequiredForWellTraveled);
            this.pendingMinimapAdvancementGate = MapAccessConfig.DEFAULT_MINIMAP_ADVANCEMENT_GATE;
            this.pendingAllowNonOpReadOnlyView = MapAccessConfig.DEFAULT_ALLOW_NON_OP_READ_ONLY_VIEW;
            rebuild();
        }, interactive));
    }

    /**
     * "World Map" section: the gated keybind's own settings, plus the Creative-mode bypass (governs both this
     * section and the Minimap section below, since it is a single mod-wide setting — not duplicated per section).
     */
    private void buildWorldMapFields(LinearLayout content, boolean interactive)
    {
        CycleButton<Boolean> keybindEnabledControl = CycleButton.onOffBuilder(this.pendingKeybindOpenEnabled)
            .displayOnlyValue()
            .create(0, 0, CONTROL_WIDTH, ROW_HEIGHT,
                Component.translatable("gui.xaero-world-map-book.mapaccess.keybind_enabled_label"),
                (button, newValue) -> { this.pendingKeybindOpenEnabled = newValue; rebuild(); });
        keybindEnabledControl.active = interactive;
        keybindEnabledControl.setTooltip(Tooltip.create(
            Component.translatable("gui.xaero-world-map-book.mapaccess.keybind_enabled_tooltip")));
        content.addChild(fieldRow(
            Component.translatable("gui.xaero-world-map-book.mapaccess.keybind_enabled_label"), keybindEnabledControl,
            () -> this.pendingKeybindOpenEnabled = MapAccessConfig.DEFAULT_KEYBIND_OPEN_ENABLED, interactive));

        CycleButton<KeybindItemRequirement> keybindRequirementControl = CycleButton.builder(
                ProgressConfigScreen::keybindRequirementLabel, this.pendingKeybindItemRequirement)
            .withValues(KeybindItemRequirement.values())
            .displayOnlyValue()
            .create(0, 0, CONTROL_WIDTH, ROW_HEIGHT,
                Component.translatable("gui.xaero-world-map-book.mapaccess.keybind_item_requirement_label"),
                (button, newValue) -> { this.pendingKeybindItemRequirement = newValue; rebuild(); });
        keybindRequirementControl.active = interactive;
        keybindRequirementControl.setTooltip(Tooltip.create(
            Component.translatable("gui.xaero-world-map-book.mapaccess.keybind_item_requirement_tooltip")));
        content.addChild(fieldRow(
            Component.translatable("gui.xaero-world-map-book.mapaccess.keybind_item_requirement_label"), keybindRequirementControl,
            () -> this.pendingKeybindItemRequirement = MapAccessConfig.DEFAULT_KEYBIND_ITEM_REQUIREMENT, interactive));

        CycleButton<Boolean> creativeBypassControl = CycleButton.onOffBuilder(this.pendingCreativeBypassEnabled)
            .displayOnlyValue()
            .create(0, 0, CONTROL_WIDTH, ROW_HEIGHT,
                Component.translatable("gui.xaero-world-map-book.mapaccess.creative_bypass_enabled_label"),
                (button, newValue) -> { this.pendingCreativeBypassEnabled = newValue; rebuild(); });
        creativeBypassControl.active = interactive;
        creativeBypassControl.setTooltip(Tooltip.create(
            Component.translatable("gui.xaero-world-map-book.mapaccess.creative_bypass_enabled_tooltip")));
        content.addChild(fieldRow(
            Component.translatable("gui.xaero-world-map-book.mapaccess.creative_bypass_enabled_label"), creativeBypassControl,
            () -> this.pendingCreativeBypassEnabled = MapAccessConfig.DEFAULT_CREATIVE_BYPASS_ENABLED, interactive));
    }

    /** "Minimap" section: the Minimap's own holding requirement, the chunk-travel milestone, and its advancement source. */
    private void buildMinimapFields(LinearLayout content, boolean interactive)
    {
        CycleButton<MinimapAccessRequirement> minimapRequirementControl = CycleButton.builder(
                ProgressConfigScreen::minimapRequirementLabel, this.pendingMinimapAccessRequirement)
            .withValues(MinimapAccessRequirement.values())
            .displayOnlyValue()
            .create(0, 0, CONTROL_WIDTH, ROW_HEIGHT,
                Component.translatable("gui.xaero-world-map-book.mapaccess.minimap_access_requirement_label"),
                (button, newValue) -> { this.pendingMinimapAccessRequirement = newValue; rebuild(); });
        minimapRequirementControl.active = interactive;
        minimapRequirementControl.setTooltip(Tooltip.create(
            Component.translatable("gui.xaero-world-map-book.mapaccess.minimap_access_requirement_tooltip")));
        content.addChild(fieldRow(
            Component.translatable("gui.xaero-world-map-book.mapaccess.minimap_access_requirement_label"), minimapRequirementControl,
            () -> this.pendingMinimapAccessRequirement = MapAccessConfig.DEFAULT_MINIMAP_ACCESS_REQUIREMENT, interactive));

        content.addChild(buildChunkCountRow(interactive));

        CycleButton<MinimapAdvancementGate> minimapAdvancementGateControl = CycleButton.builder(
                ProgressConfigScreen::minimapAdvancementGateLabel, this.pendingMinimapAdvancementGate)
            .withValues(MinimapAdvancementGate.values())
            .displayOnlyValue()
            .create(0, 0, CONTROL_WIDTH, ROW_HEIGHT,
                Component.translatable("gui.xaero-world-map-book.mapaccess.minimap_advancement_gate_label"),
                (button, newValue) -> { this.pendingMinimapAdvancementGate = newValue; rebuild(); });
        minimapAdvancementGateControl.active = interactive;
        minimapAdvancementGateControl.setTooltip(Tooltip.create(
            Component.translatable("gui.xaero-world-map-book.mapaccess.minimap_advancement_gate_tooltip")));
        content.addChild(fieldRow(
            Component.translatable("gui.xaero-world-map-book.mapaccess.minimap_advancement_gate_label"), minimapAdvancementGateControl,
            () -> this.pendingMinimapAdvancementGate = MapAccessConfig.DEFAULT_MINIMAP_ADVANCEMENT_GATE, interactive));
    }

    private static Component minimapAdvancementGateLabel(MinimapAdvancementGate gate)
    {
        return Component.translatable("gui.xaero-world-map-book.mapaccess.minimap_advancement_gate."
            + gate.name().toLowerCase(java.util.Locale.ROOT));
    }

    /**
     * A real numeric text-input field for the chunk-travel milestone — per
     * the project owner's explicit request, not a +/- stepper. Digit-only:
     * the field's own responder strips any non-digit character back out
     * immediately, and only a value that parses as a valid non-negative
     * integer updates {@link #pendingChunksRequiredForWellTraveled}; an
     * empty or otherwise unparseable in-progress edit leaves the last valid
     * pending value alone rather than clobbering it mid-keystroke.
     */
    private LinearLayout buildChunkCountRow(boolean interactive)
    {
        LinearLayout row = LinearLayout.horizontal().spacing(INTRA_ROW_SPACING);

        StringWidget label = scrollingStringWidget(LABEL_WIDTH,
            Component.translatable("gui.xaero-world-map-book.mapaccess.chunks_required_label"));
        label.setTooltip(Tooltip.create(
            Component.translatable("gui.xaero-world-map-book.mapaccess.chunks_required_label_tooltip")));
        row.addChild(label);
        row.addChild(new ProgressConfigSpacer(this.fieldGapWidth, ROW_HEIGHT));

        EditBox chunkCountField = new EditBox(this.font, 0, 0, CONTROL_WIDTH, ROW_HEIGHT,
            Component.translatable("gui.xaero-world-map-book.mapaccess.chunks_required_label"));
        chunkCountField.setValue(this.chunkCountFieldText);
        chunkCountField.setMaxLength(7);
        chunkCountField.setEditable(interactive);
        chunkCountField.setTooltip(Tooltip.create(
            Component.translatable("gui.xaero-world-map-book.mapaccess.chunks_required_value_tooltip")));
        chunkCountField.setResponder(text ->
        {
            String digitsOnly = text.replaceAll("[^0-9]", "");
            if (!digitsOnly.equals(text))
            {
                chunkCountField.setValue(digitsOnly);
                return;
            }
            this.chunkCountFieldText = digitsOnly;
            if (!digitsOnly.isEmpty())
            {
                try
                {
                    this.pendingChunksRequiredForWellTraveled = Integer.parseInt(digitsOnly);
                }
                catch (NumberFormatException exception)
                {
                    // Too many digits for an int (rare given setMaxLength(7));
                    // leave the last successfully-parsed pending value as-is.
                }
            }
        });
        row.addChild(chunkCountField);

        row.addChild(resetButton(() ->
        {
            this.pendingChunksRequiredForWellTraveled = MapAccessConfig.DEFAULT_CHUNKS_REQUIRED_FOR_WELL_TRAVELED;
            this.chunkCountFieldText = String.valueOf(this.pendingChunksRequiredForWellTraveled);
            rebuild();
        }, interactive));

        return row;
    }

    /**
     * A single field row: a static label (leftmost, fixed {@link #labelWidth}, hover-scrolling if it overflows —
     * see {@link ProgressConfigScrollingLabel}), the control showing only the current value (fixed
     * {@link #controlWidth}), and this field's own reset button (fixed {@link #RESET_WIDTH}, always the row's last
     * child) — together filling {@link #rowWidth}, so every row's reset button lands at the same X coordinate
     * regardless of that row's label/value text.
     */
    private LinearLayout fieldRow(Component label, AbstractWidget control, Runnable resetAction, boolean interactive)
    {
        LinearLayout row = LinearLayout.horizontal().spacing(INTRA_ROW_SPACING);
        row.addChild(scrollingStringWidget(LABEL_WIDTH, label));
        row.addChild(new ProgressConfigSpacer(this.fieldGapWidth, ROW_HEIGHT));
        row.addChild(control);
        row.addChild(resetButton(() ->
        {
            resetAction.run();
            rebuild();
        }, interactive));
        return row;
    }

    /**
     * The one "Reset All" control for the whole screen — a full-width text button (per the project owner's
     * explicit request: words, not the icon per-field resets use), placed as the last row so it reads as
     * "reset everything above."
     */
    private LinearLayout resetAllRow(Runnable resetAction, boolean interactive)
    {
        LinearLayout row = LinearLayout.horizontal();
        Button reset = Button.builder(
            Component.translatable("gui.xaero-world-map-book.config.reset_section"),
            button -> resetAction.run()
        ).bounds(0, 0, this.rowWidth, ROW_HEIGHT)
            .tooltip(Tooltip.create(Component.translatable("gui.xaero-world-map-book.config.reset_section_tooltip")))
            .build();
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

    private static Component keybindRequirementLabel(KeybindItemRequirement requirement)
    {
        return Component.translatable("gui.xaero-world-map-book.mapaccess.keybind_item_requirement."
            + requirement.name().toLowerCase(java.util.Locale.ROOT));
    }

    private static Component minimapRequirementLabel(MinimapAccessRequirement requirement)
    {
        return Component.translatable("gui.xaero-world-map-book.mapaccess.minimap_access_requirement."
            + requirement.name().toLowerCase(java.util.Locale.ROOT));
    }

    private boolean isDirty()
    {
        return MapAccessClientNetworkHandler.isOperator()
            && (this.pendingKeybindOpenEnabled != this.lastSyncedKeybindOpenEnabled
                || this.pendingKeybindItemRequirement != this.lastSyncedKeybindItemRequirement
                || this.pendingMinimapAccessRequirement != this.lastSyncedMinimapAccessRequirement
                || this.pendingChunksRequiredForWellTraveled != this.lastSyncedChunksRequiredForWellTraveled
                || this.pendingMinimapAdvancementGate != this.lastSyncedMinimapAdvancementGate
                || this.pendingCreativeBypassEnabled != this.lastSyncedCreativeBypassEnabled
                || this.pendingAllowNonOpReadOnlyView != this.lastSyncedAllowNonOpReadOnlyView);
    }

    private void save()
    {
        if (!isDirty())
        {
            return;
        }

        ClientPlayNetworking.send(new MapAccessConfigUpdatePayload(
            this.pendingKeybindOpenEnabled, this.pendingKeybindItemRequirement,
            this.pendingMinimapAccessRequirement, this.pendingChunksRequiredForWellTraveled,
            this.pendingMinimapAdvancementGate, this.pendingCreativeBypassEnabled, this.pendingAllowNonOpReadOnlyView));
        // The authoritative baseline updates once the server's
        // MapAccessConfigSyncPayload confirmation round-trips back; treat the
        // send as optimistically applied here so Save clears the dirty
        // flag immediately rather than waiting on network latency.
        this.lastSyncedKeybindOpenEnabled = this.pendingKeybindOpenEnabled;
        this.lastSyncedKeybindItemRequirement = this.pendingKeybindItemRequirement;
        this.lastSyncedMinimapAccessRequirement = this.pendingMinimapAccessRequirement;
        this.lastSyncedChunksRequiredForWellTraveled = this.pendingChunksRequiredForWellTraveled;
        this.lastSyncedMinimapAdvancementGate = this.pendingMinimapAdvancementGate;
        this.lastSyncedCreativeBypassEnabled = this.pendingCreativeBypassEnabled;
        this.lastSyncedAllowNonOpReadOnlyView = this.pendingAllowNonOpReadOnlyView;
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
