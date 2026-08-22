package com.onthehill.xaeroworldmapbook.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

/**
 * A small, square, icon-only reset-to-default button — this template's
 * worked example of pulling an external icon (rather than vanilla text) onto
 * a config screen control, per the project owner's request that the
 * generated template demonstrate this pattern for new mods bootstrapped from
 * it. No suitable built-in vanilla sprite exists for a "reset"/refresh
 * concept (checked the mapped client jar's
 * {@code assets/minecraft/textures/gui/sprites/} tree), so this mod ships
 * its own small texture at
 * {@code assets/xaero-world-map-book/textures/gui/widgets/reset.png}, drawn directly
 * via {@link GuiGraphicsExtractor#blit}, per
 * {@code minecraft-gui-standards.md}'s Texture &amp; Identifier Naming
 * section's {@code assets/<modid>/textures/gui/widgets/<widget_name>.png}
 * convention — the plain (non-atlas-stitched) texture path, not
 * {@code textures/gui/sprites/...}, since this one-off icon needs no
 * atlas/sprite-source registration.
 * <p>
 * <strong>Icon source / attribution:</strong> the glyph is Material Design
 * Icons' {@code mdi-restore} (a circular-arrow "restore/undo" icon), by
 * <a href="https://pictogrammers.com/">Pictogrammers</a>
 * (<a href="https://github.com/Templarian/MaterialDesign">github.com/Templarian/MaterialDesign</a>),
 * licensed under the
 * <a href="https://www.apache.org/licenses/LICENSE-2.0">Apache License 2.0</a>.
 * Rasterized from the upstream SVG path data (fetched via Iconify's public
 * SVG API, which re-serves the same MDI glyph set) to a 64x64 RGBA PNG —
 * this mod's own asset build, not a redistributed upstream binary. Recolored
 * to solid white (preserving the original alpha shape exactly) so it reads
 * correctly against this screen's grey buttons and can be multiplicatively
 * tinted for the drop shadow below without the tint being unable to lighten
 * a darker source color. See {@code README.md}'s Third-Party Assets section
 * for the same attribution in this project's own credits location.
 * <p>
 * Extends vanilla's own {@link Button} (rather than being built from
 * scratch) specifically so hover/disabled/focus background chrome, click
 * handling, sound, and tooltip behavior are all inherited unmodified — only
 * the icon draw and the accessible narration text are customized. The
 * button's visible {@code message} is intentionally {@link Component#empty()}
 * (so no vanilla button label text renders on top of the icon);
 * {@code narrationMessage} supplies the text the built-in narrator announces
 * instead, via {@link Button.Builder#createNarration}, per
 * {@code minecraft-gui-standards.md}'s Tooltips &amp; Narration section's
 * requirement that an icon-only interactive element still narrate its
 * purpose. A {@link Tooltip} carrying the same text is attached so hovering
 * also visually communicates "this is a reset button" (also required by that
 * same section, for any icon-only control without self-evident behavior).
 */
final class ProgressConfigResetButton extends Button
{
    private static final Identifier ICON =
        Identifier.fromNamespaceAndPath("xaero-world-map-book", "textures/gui/widgets/reset.png");

    // Native dimensions of assets/xaero-world-map-book/textures/gui/widgets/reset.png
    // — must match the actual file so the blit's texture-size parameters are
    // correct. 64x64 gives headroom above this button's actual on-screen
    // size so the glyph stays crisp when scaled down rather than being baked
    // at exactly the button's own pixel size.
    private static final int ICON_TEXTURE_SIZE = 64;
    private static final int ICON_MARGIN = 3;

    private ProgressConfigResetButton(int x, int y, int size, MutableComponent narrationMessage, OnPress onPress)
    {
        super(x, y, size, size, Component.empty(), onPress, ignored -> narrationMessage);
        setTooltip(Tooltip.create(narrationMessage));
    }

    /**
     * Builds a reset button at the given position/size.
     *
     * @param x The button's left screen coordinate.
     * @param y The button's top screen coordinate.
     * @param size The button's width and height (square).
     * @param narrationMessage The text announced by the narrator and shown
     *     in the hover tooltip — e.g. "Reset" for a per-field button,
     *     "Reset All" for a section-wide one.
     * @param onPress Invoked when the button is clicked.
     * @return The constructed button. Caller is responsible for setting
     *     {@code active} if it should start disabled (e.g. a non-interactive
     *     admin-tab control).
     */
    static ProgressConfigResetButton create(int x, int y, int size, MutableComponent narrationMessage, OnPress onPress)
    {
        return new ProgressConfigResetButton(x, y, size, narrationMessage, onPress);
    }

    /**
     * Shadow tint applied to the offset copy drawn behind the main icon, matching vanilla's own text-shadow
     * darkening convention (each RGB channel divided by 4 — {@code 255/4 ≈ 64 = 0x40}). The icon texture itself is
     * plain white with alpha (see {@link #ICON}'s own file), so multiplicative tinting can recolor it freely —
     * unlike the original black-on-transparent icon, which could only ever be darkened further by a tint, never
     * lightened to white.
     */
    private static final int SHADOW_COLOR = 0xFF404040;

    /** Full white, i.e. no recoloring — the icon texture is already the exact color this button wants on top. */
    private static final int MAIN_COLOR = 0xFFFFFFFF;

    /** How far down-and-right the shadow copy is offset from the main icon, matching vanilla's own text-shadow offset. */
    private static final int SHADOW_OFFSET = 1;

    // AbstractButton's own extractWidgetRenderState (background chrome/hover
    // state) is final — extractContents is the actual per-button-subclass
    // content-drawing hook (what Button itself overrides to draw centered
    // message text). Button leaves it abstract when not built via
    // Button.builder(...).build(), so there is no super implementation to
    // defer to here; this button's message is always Component.empty()
    // anyway, so the only content this method draws is the icon below.
    @Override
    protected void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        // The standard button background bezel is NOT drawn automatically —
        // extractWidgetRenderState (final, on AbstractButton) handles only
        // the outer hover/focus state; the actual sprite background is a
        // separate, explicitly-callable hook (extractDefaultSprite, also
        // final on AbstractButton) that every content-drawing override is
        // expected to call itself. Skipping it — as this class originally
        // did — draws only the icon with no button chrome behind it at all.
        extractDefaultSprite(guiGraphics);

        int iconSize = getWidth() - ICON_MARGIN * 2;

        if (iconSize <= 0)
        {
            return;
        }

        int iconX = getX() + (getWidth() - iconSize) / 2;
        int iconY = getY() + (getHeight() - iconSize) / 2;

        // Parameter order: (x, y, u, v, width, height, srcWidth, srcHeight,
        // textureWidth, textureHeight, color) — "width"/"height" is the
        // on-screen draw size, "srcWidth"/"srcHeight" is the region sampled
        // from the texture (in texture pixels), "textureWidth"/"textureHeight"
        // is the full backing texture's own dimensions (used to normalize the
        // UV coordinates), and "color" is an ARGB multiplicative tint. Drawn
        // twice: once offset down-right and tinted dark (the drop shadow),
        // then once at the true position with no recoloring — the same
        // shadow-then-main draw order vanilla's own text-shadow rendering
        // uses, so this icon reads consistently with the white, shadowed
        // button labels around it.
        blitIcon(guiGraphics, iconX + SHADOW_OFFSET, iconY + SHADOW_OFFSET, iconSize, SHADOW_COLOR);
        blitIcon(guiGraphics, iconX, iconY, iconSize, MAIN_COLOR);
    }

    private void blitIcon(GuiGraphicsExtractor guiGraphics, int x, int y, int iconSize, int color)
    {
        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED,
            ICON,
            x, y,
            0.0f, 0.0f,
            iconSize, iconSize,
            ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE,
            ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE,
            color);
    }
}
