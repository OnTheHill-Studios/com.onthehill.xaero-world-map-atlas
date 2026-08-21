package com.onthehill.templatemod.progress;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProgressMathTest
{
    @Test
    void extrapolate_severalTicksElapsed_addsAccumulatedRate()
    {
        // Arrange
        float syncedProgress = 0.2f;
        float ratePerTick = 0.01f;
        int ticksElapsedSinceSync = 10;

        // Act
        float result = ProgressMath.extrapolate(syncedProgress, ratePerTick, ticksElapsedSinceSync);

        // Assert
        assertEquals(0.3f, result, 0.001f);
    }

    @Test
    void extrapolate_zeroTicksElapsed_returnsSyncedProgressUnchanged()
    {
        // Arrange
        float syncedProgress = 0.42f;
        float ratePerTick = 0.05f;
        int ticksElapsedSinceSync = 0;

        // Act
        float result = ProgressMath.extrapolate(syncedProgress, ratePerTick, ticksElapsedSinceSync);

        // Assert
        assertEquals(0.42f, result, 0.001f);
    }

    @Test
    void extrapolate_manyTicksElapsed_wrapsAroundMultipleTimes()
    {
        // Arrange — 250 ticks at 0.01/tick is 2.5 full cycles past the synced value
        float syncedProgress = 0.0f;
        float ratePerTick = 0.01f;
        int ticksElapsedSinceSync = 250;

        // Act
        float result = ProgressMath.extrapolate(syncedProgress, ratePerTick, ticksElapsedSinceSync);

        // Assert
        assertEquals(0.5f, result, 0.001f);
    }

    @Test
    void extrapolate_negativeTicksElapsed_throwsIllegalArgument()
    {
        // Arrange
        float syncedProgress = 0.1f;
        float ratePerTick = 0.01f;
        int ticksElapsedSinceSync = -1;

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> ProgressMath.extrapolate(syncedProgress, ratePerTick, ticksElapsedSinceSync));
    }
}
