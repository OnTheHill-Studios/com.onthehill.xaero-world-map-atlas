package com.onthehill.xaeroworldmapbook.config;

/**
 * The mode-specific half of the Minimap visibility gate (see
 * {@code progression.MapAccessEvaluator#canShowMinimap}) — always combined
 * with the floor requirement that {@code craft_atlas} has been awarded.
 */
public enum MinimapAccessRequirement
{
    /** The Atlas must be the current main-hand stack. */
    MAIN_HAND,

    /** The Atlas must be the current main-hand or offhand stack. Default mode. */
    MAIN_OR_OFFHAND,

    /** The Atlas must occupy one of the player's 9 hotbar slots (need not be selected). */
    HOTBAR,

    /** The Atlas must occupy any of the player's 36 main-inventory slots. */
    INVENTORY,

    /** No holding check at all — the {@code well_traveled} advancement must be earned instead. */
    ADVANCEMENT_ONLY
}
