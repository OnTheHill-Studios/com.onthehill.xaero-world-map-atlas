package com.onthehill.xaeroworldmapbook.progression;

import com.onthehill.xaeroworldmapbook.config.KeybindItemRequirement;
import com.onthehill.xaeroworldmapbook.config.MinimapAccessRequirement;

/**
 * Pure, Minecraft-independent gate-decision logic for both of Spec 001's
 * access checks. Every input is a plain boolean/enum the Minecraft-facing
 * caller (a Mixin, a keybind handler, {@code client.config.MapAccessClientState})
 * is responsible for computing from real inventory/advancement state — this
 * class has no dependency on any {@code net.minecraft.*} type, so it can be
 * unit-tested directly per {@code java-coding-standards.md}'s testability rule.
 */
public final class MapAccessEvaluator
{
    private MapAccessEvaluator() { }

    /**
     * Whether the gated keybind is currently allowed to open Xaero's World Map.
     *
     * @param keybindOpenEnabled Whether the keybind is enabled at all; {@code false} short-circuits every other
     *     check, including the Creative bypass — that toggle is a deliberate whole-feature off switch, not a
     *     requirement the bypass is meant to skip.
     * @param creativeBypassActive Whether the Creative-mode bypass currently applies to this player — the caller
     *     computes this as {@code creativeBypassEnabled && player.isCreative()}. When {@code true} (and the keybind
     *     is enabled), every item/advancement requirement below is skipped.
     * @param requirement Which item-location rule to apply.
     * @param atlasInHotbar Whether an Atlas currently occupies one of the player's 9 hotbar slots.
     * @param atlasInMainInventory Whether an Atlas currently occupies one of the player's 27 main-inventory
     *     storage slots (excluding the hotbar, which is reported separately via {@code atlasInHotbar}).
     * @param craftAtlasAdvancementEarned Whether the player has been awarded the {@code craft_atlas} advancement.
     * @return {@code true} if the keybind press should be allowed to open the map.
     */
    public static boolean canUseKeybind(
        boolean keybindOpenEnabled,
        boolean creativeBypassActive,
        KeybindItemRequirement requirement,
        boolean atlasInHotbar,
        boolean atlasInMainInventory,
        boolean craftAtlasAdvancementEarned)
    {
        return describeKeybindDenialReason(keybindOpenEnabled, creativeBypassActive, requirement,
            atlasInHotbar, atlasInMainInventory, craftAtlasAdvancementEarned) == KeybindDenialReason.NONE;
    }

    /**
     * The single shared source of truth {@link #canUseKeybind} itself delegates to — computes not just whether a
     * keybind press should be blocked, but specifically why, so the Minecraft-facing caller can show a denial
     * message naming the actual missing requirement (a hotbar item, an inventory item, or an advancement) rather
     * than a generic "you can't do that."
     *
     * @param keybindOpenEnabled Whether the keybind is enabled at all; {@code false} short-circuits every other
     *     check, including the Creative bypass — that toggle is a deliberate whole-feature off switch, not a
     *     requirement the bypass is meant to skip.
     * @param creativeBypassActive Whether the Creative-mode bypass currently applies to this player.
     * @param requirement Which item-location rule to apply.
     * @param atlasInHotbar Whether an Atlas currently occupies one of the player's 9 hotbar slots.
     * @param atlasInMainInventory Whether an Atlas currently occupies one of the player's 27 main-inventory
     *     storage slots (excluding the hotbar, which is reported separately via {@code atlasInHotbar}).
     * @param craftAtlasAdvancementEarned Whether the player has been awarded the {@code craft_atlas} advancement.
     * @return {@link KeybindDenialReason#NONE} if the press should be allowed through, otherwise the specific
     *     reason it should not be.
     */
    public static KeybindDenialReason describeKeybindDenialReason(
        boolean keybindOpenEnabled,
        boolean creativeBypassActive,
        KeybindItemRequirement requirement,
        boolean atlasInHotbar,
        boolean atlasInMainInventory,
        boolean craftAtlasAdvancementEarned)
    {
        if (!keybindOpenEnabled)
        {
            return KeybindDenialReason.KEYBIND_DISABLED;
        }

        if (creativeBypassActive)
        {
            return KeybindDenialReason.NONE;
        }

        return switch (requirement)
        {
            case HOTBAR -> atlasInHotbar ? KeybindDenialReason.NONE : KeybindDenialReason.NEEDS_ITEM_IN_HOTBAR;
            case INVENTORY -> (atlasInHotbar || atlasInMainInventory)
                ? KeybindDenialReason.NONE : KeybindDenialReason.NEEDS_ITEM_IN_INVENTORY;
            case ADVANCEMENT -> craftAtlasAdvancementEarned
                ? KeybindDenialReason.NONE : KeybindDenialReason.NEEDS_ADVANCEMENT;
        };
    }

    /**
     * Whether Xaero's Minimap should currently be visible to this player.
     *
     * @param creativeBypassActive Whether the Creative-mode bypass currently applies to this player — see
     *     {@link #canUseKeybind}'s Javadoc for how the caller computes this. When {@code true}, both the floor and
     *     the mode-specific check below are skipped.
     * @param requirement Which mode-specific holding rule to apply.
     * @param craftAtlasAdvancementEarned Floor requirement: whether the player has been awarded
     *     {@code craft_atlas}. Checked unconditionally, before the mode-specific rule, in every mode.
     * @param atlasInMainHand Whether the Atlas is the player's current main-hand stack.
     * @param atlasInOffhand Whether the Atlas is the player's current offhand stack.
     * @param atlasInHotbar Whether an Atlas currently occupies one of the player's 9 hotbar slots.
     * @param atlasInMainInventory Whether an Atlas currently occupies one of the player's 27 main-inventory
     *     storage slots (excluding the hotbar, which is reported separately via {@code atlasInHotbar}).
     * @param advancementOnlyGateSatisfied Whether the advancement {@link MinimapAccessRequirement#ADVANCEMENT_ONLY}
     *     currently requires has been earned — the caller resolves which real advancement that means (this mod's
     *     own {@code well_traveled}, or vanilla's {@code adventuring_time}) from
     *     {@code MapAccessConfig#getMinimapAdvancementGate()} before calling this method; this class has no opinion
     *     on which advancement it is, only whether it's satisfied.
     * @return {@code true} if the Minimap should currently render for this player.
     */
    public static boolean canShowMinimap(
        boolean creativeBypassActive,
        MinimapAccessRequirement requirement,
        boolean craftAtlasAdvancementEarned,
        boolean atlasInMainHand,
        boolean atlasInOffhand,
        boolean atlasInHotbar,
        boolean atlasInMainInventory,
        boolean advancementOnlyGateSatisfied)
    {
        if (creativeBypassActive)
        {
            return true;
        }

        if (!craftAtlasAdvancementEarned)
        {
            return false;
        }

        return switch (requirement)
        {
            case MAIN_HAND -> atlasInMainHand;
            case MAIN_OR_OFFHAND -> atlasInMainHand || atlasInOffhand;
            case HOTBAR -> atlasInHotbar;
            case INVENTORY -> atlasInHotbar || atlasInMainInventory;
            case ADVANCEMENT_ONLY -> advancementOnlyGateSatisfied;
        };
    }
}
