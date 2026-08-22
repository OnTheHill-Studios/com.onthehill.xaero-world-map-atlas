package com.onthehill.xaeroworldmapbook.config;

/**
 * Which advancement satisfies {@link MinimapAccessRequirement#ADVANCEMENT_ONLY}'s mode-specific check — on top of
 * the {@code craft_atlas} floor, which always applies regardless of this setting.
 */
public enum MinimapAdvancementGate
{
    /** This mod's own {@code well_traveled} advancement (distinct chunks visited). */
    WELL_TRAVELED,

    /** Vanilla's own {@code minecraft:adventure/adventuring_time} advancement (visited every biome type). */
    ADVENTURING_TIME
}
