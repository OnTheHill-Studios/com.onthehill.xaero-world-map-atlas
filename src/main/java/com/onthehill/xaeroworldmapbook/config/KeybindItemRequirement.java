package com.onthehill.xaeroworldmapbook.config;

/**
 * How strictly the gated {@code M} keybind (see {@code progression.MapAccessEvaluator})
 * requires the Atlas to be held before it will open Xaero's World Map.
 */
public enum KeybindItemRequirement
{
    /** The Atlas must currently occupy one of the player's 9 hotbar slots. */
    HOTBAR,

    /** The Atlas must occupy any of the player's 36 main-inventory slots (hotbar or storage rows). */
    INVENTORY,

    /** No item-location check at all — usable by keybind once the {@code craft_atlas} advancement is earned. */
    ADVANCEMENT
}
