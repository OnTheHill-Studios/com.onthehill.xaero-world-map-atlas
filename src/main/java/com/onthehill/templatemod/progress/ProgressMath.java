package com.onthehill.templatemod.progress;

/**
 * Pure extrapolation logic used to render a smooth, every-frame progress
 * value on the client between infrequent server resync packets, rather than
 * requiring the server to broadcast a packet every single tick. Holds no
 * Minecraft dependencies so it can be unit tested directly.
 */
public final class ProgressMath
{
    private ProgressMath() { }

    /**
     * Extrapolates the current progress value from the last confirmed
     * server-synced value, assuming the rate has stayed constant for the
     * elapsed ticks.
     *
     * @param syncedProgress Progress value at the moment of the last sync, in [0, 1).
     * @param ratePerTick Rate confirmed at that same sync, in (0, 1).
     * @param ticksElapsedSinceSync Number of client ticks elapsed since that sync. Must be non-negative.
     * @return Extrapolated progress value, always in [0, 1).
     * @throws IllegalArgumentException if ticksElapsedSinceSync is negative.
     * @implNote raw = syncedProgress + (ratePerTick * ticksElapsedSinceSync);
     *     result = raw mod 1.0, folded back into [0, 1) since Java's {@code %}
     *     can otherwise report a value in that range only for non-negative raw.
     */
    public static float extrapolate(float syncedProgress, float ratePerTick, int ticksElapsedSinceSync)
    {
        if (ticksElapsedSinceSync < 0)
        {
            throw new IllegalArgumentException(
                "ticksElapsedSinceSync must not be negative, was " + ticksElapsedSinceSync);
        }

        float raw = syncedProgress + (ratePerTick * ticksElapsedSinceSync);
        float wrapped = raw % 1.0f;
        return wrapped < 0.0f ? wrapped + 1.0f : wrapped;
    }
}
