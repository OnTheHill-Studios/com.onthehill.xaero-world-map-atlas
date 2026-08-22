package com.onthehill.xaeroworldmapbook.client.config;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Small, dependency-free color-conversion helpers shared by the color picker
 * screen/widgets ({@code client.screen.ProgressColorPickerScreen} and its
 * supporting widgets) and {@link ClientVisualizationConfig}'s own hex
 * storage/parsing. Public (unlike the equivalent
 * {@code com.onthehill.climbing} class, which is package-private) because
 * this template splits config data classes ({@code client.config}) from
 * screen classes ({@code client.screen}) into separate packages, and both
 * sides need to agree on exactly the same parsing/formatting/HSV-conversion
 * rules.
 */
public final class ProgressColorUtil
{
    private static final Pattern HEX_PATTERN = Pattern.compile("(?i)^#?[0-9A-F]{6}$");

    private ProgressColorUtil()
    {
    }

    /**
     * Whether {@code text} is a valid hex color, with or without a leading
     * {@code #}.
     *
     * @param text The candidate string, possibly {@code null}.
     * @return {@code true} if {@code text} is exactly six hex digits,
     *     optionally prefixed with {@code #}.
     */
    public static boolean isValidHex(String text)
    {
        return text != null && HEX_PATTERN.matcher(text.trim()).matches();
    }

    /**
     * Parses a hex color string (with or without a leading {@code #}) into
     * a packed {@code 0xRRGGBB} int, falling back to {@code fallback} if
     * {@code text} doesn't match.
     *
     * @param text The candidate string, possibly malformed or {@code null}.
     * @param fallback The value returned when {@code text} is not a valid
     *     hex color.
     * @return The parsed {@code 0xRRGGBB} color, or {@code fallback}.
     */
    public static int parseHexRgb(String text, int fallback)
    {
        if (!isValidHex(text))
        {
            return fallback;
        }

        String trimmed = text.trim();
        String digits = trimmed.startsWith("#") ? trimmed.substring(1) : trimmed;
        return Integer.parseInt(digits, 16) & 0xFFFFFF;
    }

    /**
     * Formats a packed {@code 0xRRGGBB} int as an uppercase {@code "#RRGGBB"} string.
     *
     * @param rgb The packed color.
     * @return The formatted hex string.
     */
    public static String toHex(int rgb)
    {
        return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }

    /**
     * Converts a packed {@code 0xRRGGBB} color to HSV.
     *
     * @param rgb The packed color.
     * @return {@code [hue(0-360), saturation(0-1), value(0-1)]}.
     */
    public static float[] rgbToHsv(int rgb)
    {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        float[] hsv = new float[3];
        java.awt.Color.RGBtoHSB(r, g, b, hsv);
        hsv[0] = hsv[0] * 360.0f;
        return hsv;
    }

    /**
     * Converts HSV to a packed {@code 0xRRGGBB} color.
     *
     * @param hue Hue in {@code [0, 360)}.
     * @param saturation Saturation in {@code [0, 1]}.
     * @param value Value/brightness in {@code [0, 1]}.
     * @return The packed {@code 0xRRGGBB} color.
     */
    public static int hsvToRgb(float hue, float saturation, float value)
    {
        int packed = java.awt.Color.HSBtoRGB(hue / 360.0f, clamp01(saturation), clamp01(value));
        return packed & 0xFFFFFF;
    }

    private static float clamp01(float value)
    {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
