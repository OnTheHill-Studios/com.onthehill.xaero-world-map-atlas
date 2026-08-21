package com.onthehill.templatemod.progress;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TickProgressStateTest
{
    @Test
    void advance_midRangeProgress_addsRateWithoutWrapping()
    {
        // Arrange
        float currentProgress = 0.5f;
        float ratePerTick = 0.1f;

        // Act
        float result = TickProgressState.advance(currentProgress, ratePerTick);

        // Assert
        assertEquals(0.6f, result, 0.001f);
    }

    @Test
    void advance_zeroProgress_addsRateFromZero()
    {
        // Arrange
        float currentProgress = 0.0f;
        float ratePerTick = 0.01f;

        // Act
        float result = TickProgressState.advance(currentProgress, ratePerTick);

        // Assert
        assertEquals(0.01f, result, 0.001f);
    }

    @Test
    void advance_reachesExactlyOne_wrapsToZero()
    {
        // Arrange
        float currentProgress = 0.99f;
        float ratePerTick = 0.01f;

        // Act
        float result = TickProgressState.advance(currentProgress, ratePerTick);

        // Assert
        assertEquals(0.0f, result, 0.001f);
    }

    @Test
    void advance_overshootsPastOne_wrapsWithRemainder()
    {
        // Arrange — a near-maximum rate pushes progress past 1 with a remainder
        float currentProgress = 0.95f;
        float ratePerTick = 0.9999f;

        // Act
        float result = TickProgressState.advance(currentProgress, ratePerTick);

        // Assert
        assertEquals(0.9499f, result, 0.001f);
    }
}
