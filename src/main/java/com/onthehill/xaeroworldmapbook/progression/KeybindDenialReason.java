package com.onthehill.xaeroworldmapbook.progression;

/**
 * Why a gated {@code M} keypress was blocked, as determined by
 * {@link MapAccessEvaluator#describeKeybindDenialReason}. The Minecraft-facing caller
 * ({@code client.XaeroWorldMapBookClient}, {@code client.mixin.XaeroWorldMapOpenGateMixin})
 * maps each non-{@link #NONE} value to its own translatable action-bar message, so a
 * blocked attempt always explains specifically what is missing — not just that
 * something is missing.
 */
public enum KeybindDenialReason
{
    /** Not denied — the keybind press should be allowed through. */
    NONE,

    /** The keybind is disabled entirely by server config, regardless of what the player holds or has earned. */
    KEYBIND_DISABLED,

    /** {@code keybindItemRequirement} is {@code HOTBAR} and the Atlas isn't in the hotbar. */
    NEEDS_ITEM_IN_HOTBAR,

    /** {@code keybindItemRequirement} is {@code INVENTORY} and the Atlas isn't anywhere in the main inventory. */
    NEEDS_ITEM_IN_INVENTORY,

    /** {@code keybindItemRequirement} is {@code ADVANCEMENT} and {@code craft_atlas} hasn't been earned. */
    NEEDS_ADVANCEMENT
}
