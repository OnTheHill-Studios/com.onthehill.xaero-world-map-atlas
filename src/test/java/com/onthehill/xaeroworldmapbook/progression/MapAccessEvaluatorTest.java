package com.onthehill.xaeroworldmapbook.progression;

import com.onthehill.xaeroworldmapbook.config.KeybindItemRequirement;
import com.onthehill.xaeroworldmapbook.config.MinimapAccessRequirement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapAccessEvaluatorTest
{
    @Test
    void canUseKeybind_hotbarModeWithAtlasInHotbar_returnsTrue()
    {
        // Arrange
        boolean keybindOpenEnabled = true;
        KeybindItemRequirement requirement = KeybindItemRequirement.HOTBAR;

        // Act
        boolean result = MapAccessEvaluator.canUseKeybind(keybindOpenEnabled, false, requirement, true, false, false);

        // Assert
        assertTrue(result);
    }

    @Test
    void canUseKeybind_keybindDisabledRegardlessOfItemLocation_returnsFalse()
    {
        // Arrange
        boolean keybindOpenEnabled = false;
        KeybindItemRequirement requirement = KeybindItemRequirement.HOTBAR;

        // Act
        boolean result = MapAccessEvaluator.canUseKeybind(keybindOpenEnabled, false, requirement, true, true, true);

        // Assert
        assertFalse(result);
    }

    @Test
    void canUseKeybind_advancementModeWithoutAtlasButAdvancementEarned_returnsTrue()
    {
        // Arrange
        boolean keybindOpenEnabled = true;
        KeybindItemRequirement requirement = KeybindItemRequirement.ADVANCEMENT;

        // Act
        boolean result = MapAccessEvaluator.canUseKeybind(keybindOpenEnabled, false, requirement, false, false, true);

        // Assert
        assertTrue(result);
    }

    @Test
    void canUseKeybind_hotbarModeWithAtlasOnlyInMainInventoryRow_returnsFalse()
    {
        // Arrange
        boolean keybindOpenEnabled = true;
        KeybindItemRequirement requirement = KeybindItemRequirement.HOTBAR;

        // Act
        boolean result = MapAccessEvaluator.canUseKeybind(keybindOpenEnabled, false, requirement, false, true, false);

        // Assert
        assertFalse(result);
    }

    @Test
    void canUseKeybind_creativeBypassActiveWithNoAtlasAnywhere_returnsTrue()
    {
        // Arrange
        boolean keybindOpenEnabled = true;
        KeybindItemRequirement requirement = KeybindItemRequirement.HOTBAR;

        // Act
        boolean result = MapAccessEvaluator.canUseKeybind(keybindOpenEnabled, true, requirement, false, false, false);

        // Assert
        assertTrue(result);
    }

    @Test
    void canUseKeybind_creativeBypassActiveButKeybindDisabled_returnsFalse()
    {
        // Arrange — the bypass skips item/advancement requirements, not the master on/off switch.
        boolean keybindOpenEnabled = false;
        KeybindItemRequirement requirement = KeybindItemRequirement.HOTBAR;

        // Act
        boolean result = MapAccessEvaluator.canUseKeybind(keybindOpenEnabled, true, requirement, false, false, false);

        // Assert
        assertFalse(result);
    }

    @Test
    void describeKeybindDenialReason_keybindDisabled_returnsKeybindDisabled()
    {
        // Arrange & Act
        KeybindDenialReason reason = MapAccessEvaluator.describeKeybindDenialReason(
            false, false, KeybindItemRequirement.HOTBAR, true, true, true);

        // Assert
        assertEquals(KeybindDenialReason.KEYBIND_DISABLED, reason);
    }

    @Test
    void describeKeybindDenialReason_hotbarModeWithoutAtlas_returnsNeedsItemInHotbar()
    {
        // Arrange & Act
        KeybindDenialReason reason = MapAccessEvaluator.describeKeybindDenialReason(
            true, false, KeybindItemRequirement.HOTBAR, false, true, false);

        // Assert
        assertEquals(KeybindDenialReason.NEEDS_ITEM_IN_HOTBAR, reason);
    }

    @Test
    void describeKeybindDenialReason_inventoryModeWithoutAtlasAnywhere_returnsNeedsItemInInventory()
    {
        // Arrange & Act
        KeybindDenialReason reason = MapAccessEvaluator.describeKeybindDenialReason(
            true, false, KeybindItemRequirement.INVENTORY, false, false, false);

        // Assert
        assertEquals(KeybindDenialReason.NEEDS_ITEM_IN_INVENTORY, reason);
    }

    @Test
    void describeKeybindDenialReason_advancementModeWithoutAdvancement_returnsNeedsAdvancement()
    {
        // Arrange & Act
        KeybindDenialReason reason = MapAccessEvaluator.describeKeybindDenialReason(
            true, false, KeybindItemRequirement.ADVANCEMENT, false, false, false);

        // Assert
        assertEquals(KeybindDenialReason.NEEDS_ADVANCEMENT, reason);
    }

    @Test
    void describeKeybindDenialReason_creativeBypassActive_returnsNone()
    {
        // Arrange & Act
        KeybindDenialReason reason = MapAccessEvaluator.describeKeybindDenialReason(
            true, true, KeybindItemRequirement.HOTBAR, false, false, false);

        // Assert
        assertEquals(KeybindDenialReason.NONE, reason);
    }

    @Test
    void canShowMinimap_mainOrOffhandModeWithAtlasInOffhandAndFloorMet_returnsTrue()
    {
        // Arrange
        MinimapAccessRequirement requirement = MinimapAccessRequirement.MAIN_OR_OFFHAND;

        // Act
        boolean result = MapAccessEvaluator.canShowMinimap(false, requirement, true, false, true, false, false, false);

        // Assert
        assertTrue(result);
    }

    @Test
    void canShowMinimap_floorNotMet_returnsFalseRegardlessOfMode()
    {
        // Arrange & Act & Assert
        for (MinimapAccessRequirement requirement : MinimapAccessRequirement.values())
        {
            boolean result = MapAccessEvaluator.canShowMinimap(false, requirement, false, true, true, true, true, true);
            assertFalse(result, "Expected false for mode " + requirement + " when the craft_atlas floor is not met");
        }
    }

    @Test
    void canShowMinimap_advancementOnlyModeWithAtlasHeldButGateNotSatisfied_returnsFalse()
    {
        // Arrange
        MinimapAccessRequirement requirement = MinimapAccessRequirement.ADVANCEMENT_ONLY;

        // Act
        boolean result = MapAccessEvaluator.canShowMinimap(false, requirement, true, true, true, true, true, false);

        // Assert
        assertFalse(result);
    }

    @Test
    void canShowMinimap_mainHandModeWithAtlasInOffhandOnly_returnsFalse()
    {
        // Arrange
        MinimapAccessRequirement requirement = MinimapAccessRequirement.MAIN_HAND;

        // Act
        boolean result = MapAccessEvaluator.canShowMinimap(false, requirement, true, false, true, false, false, false);

        // Assert
        assertFalse(result);
    }

    @Test
    void canShowMinimap_creativeBypassActiveWithFloorNotMet_returnsTrue()
    {
        // Arrange — the bypass skips the floor entirely, unlike every other mode.
        MinimapAccessRequirement requirement = MinimapAccessRequirement.ADVANCEMENT_ONLY;

        // Act
        boolean result = MapAccessEvaluator.canShowMinimap(true, requirement, false, false, false, false, false, false);

        // Assert
        assertTrue(result);
    }
}
