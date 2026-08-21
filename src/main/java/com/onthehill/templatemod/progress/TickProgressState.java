package com.onthehill.templatemod.progress;

/**
 * Pure server-side tick-advancement logic for the template mod's example
 * progress value. Holds no Minecraft dependencies so it can be unit tested
 * directly; the Fabric-facing tick hook that calls this lives in
 * {@code TemplateMod}'s {@code ServerTickEvents.END_SERVER_TICK} handler.
 */
public final class TickProgressState
{
    private TickProgressState() { }

    /**
     * Advances a progress value by one server tick's worth of the given
     * rate, wrapping back down once it reaches or passes 1.
     *
     * @param currentProgress Current progress value, expected in [0, 1).
     * @param ratePerTick Amount added per tick. Expected in (0, 1).
     * @return The advanced progress value, always in [0, 1).
     * @implNote next = currentProgress + ratePerTick; if next >= 1.0, next -= 1.0.
     *     A single subtraction is sufficient because ratePerTick is always less
     *     than 1, so next can never reach or exceed 2.0.
     */
    public static float advance(float currentProgress, float ratePerTick)
    {
        float next = currentProgress + ratePerTick;
        if (next >= 1.0f)
        {
            next -= 1.0f;
        }
        return next;
    }
}
