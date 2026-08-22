package com.onthehill.xaeroworldmapbook.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-authoritative configuration for the example tick-progress feature.
 * Source of truth is this local JSON file only — nothing here is sent to or
 * validated by the server, per the studio config standard's client/server
 * classification rule.
 */
public final class ClientVisualizationConfig
{
    private static final Logger LOGGER = LoggerFactory.getLogger("xaero-world-map-book/client-config");

    /**
     * Default visualization mode shown to a player who has never changed the setting.
     */
    public static final ProgressVisualizationMode DEFAULT_VISUALIZATION_MODE = ProgressVisualizationMode.BAR;

    /**
     * Default visualization fill color, as {@code "#RRGGBB"} — matches the
     * hardcoded {@code 0xFF55FF55} fill color the HUD renderer used before
     * this setting existed, so a player who never touches this setting sees
     * no visual change.
     */
    public static final String DEFAULT_VISUALIZATION_COLOR = "#55FF55";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ProgressVisualizationMode visualizationMode = DEFAULT_VISUALIZATION_MODE;
    private String visualizationColor = DEFAULT_VISUALIZATION_COLOR;

    /**
     * Which visualization the HUD renderer should draw.
     *
     * @return The currently selected visualization mode.
     */
    public ProgressVisualizationMode getVisualizationMode()
    {
        return this.visualizationMode;
    }

    /**
     * Sets the visualization mode. Client-authoritative fields have no
     * server-side validation to centralize against, so this setter applies
     * directly — any {@code ProgressVisualizationMode} enum constant is
     * inherently valid.
     *
     * @param visualizationMode The new visualization mode.
     */
    public void setVisualizationMode(ProgressVisualizationMode visualizationMode)
    {
        this.visualizationMode = visualizationMode;
    }

    /**
     * The visualization's fill color, as an uppercase {@code "#RRGGBB"}
     * hex string — the same format {@link ProgressColorUtil#toHex(int)}
     * produces and {@link ProgressColorUtil#parseHexRgb(String, int)}
     * accepts.
     *
     * @return The currently configured fill color hex string.
     */
    public String getVisualizationColorHex()
    {
        return this.visualizationColor;
    }

    /**
     * Sets the visualization fill color. Client-authoritative, same as
     * {@link #setVisualizationMode}: no server-side validation to
     * centralize against.
     *
     * @param visualizationColor New color, as a {@code "#RRGGBB"} hex
     *     string (with or without the leading {@code #} — not normalized
     *     here; callers that need a normalized/validated value should go
     *     through {@link ProgressColorUtil} first, same as the config
     *     screen's color field does).
     */
    public void setVisualizationColorHex(String visualizationColor)
    {
        this.visualizationColor = visualizationColor;
    }

    /**
     * The visualization's fill color as a packed {@code 0xRRGGBB} int,
     * parsed from {@link #getVisualizationColorHex()} — the form the HUD
     * renderer actually draws with. Falls back to
     * {@link #DEFAULT_VISUALIZATION_COLOR} if the stored value is somehow
     * unparseable (e.g. hand-edited JSON), rather than crashing the
     * renderer.
     *
     * @return The packed {@code 0xRRGGBB} fill color.
     */
    public int getVisualizationColorRgb()
    {
        return ProgressColorUtil.parseHexRgb(this.visualizationColor,
            ProgressColorUtil.parseHexRgb(DEFAULT_VISUALIZATION_COLOR, 0x55FF55));
    }

    /**
     * Resets this config back to its default value. Used by both the config
     * GUI's client-tab section-wide reset and the
     * {@code /xaero-world-map-book config reset} command.
     */
    public void resetToDefaults()
    {
        this.visualizationMode = DEFAULT_VISUALIZATION_MODE;
        this.visualizationColor = DEFAULT_VISUALIZATION_COLOR;
    }

    /**
     * Loads the client config from the given path, falling back to defaults
     * for a missing file or an unreadable value, then writes the merged
     * result back to disk.
     *
     * @param path Path to the client-side JSON config file.
     * @return The loaded (or newly-created default) config.
     */
    public static ClientVisualizationConfig loadOrCreate(Path path)
    {
        ClientVisualizationConfig config = new ClientVisualizationConfig();

        if (Files.exists(path))
        {
            try (Reader reader = Files.newBufferedReader(path))
            {
                ClientVisualizationConfig loaded = GSON.fromJson(reader, ClientVisualizationConfig.class);
                if (loaded != null && loaded.visualizationMode != null)
                {
                    config.visualizationMode = loaded.visualizationMode;
                }
                if (loaded != null && ProgressColorUtil.isValidHex(loaded.visualizationColor))
                {
                    config.visualizationColor = loaded.visualizationColor;
                }
            }
            catch (IOException exception)
            {
                LOGGER.warn("Failed to read client config at {}, using defaults.", path, exception);
            }
        }

        config.save(path);
        return config;
    }

    /**
     * Writes this config to the given path as pretty-printed JSON.
     *
     * @param path Path to the client-side JSON config file.
     */
    public void save(Path path)
    {
        try
        {
            Path parent = path.getParent();
            if (parent != null)
            {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(path))
            {
                GSON.toJson(this, writer);
            }
        }
        catch (IOException exception)
        {
            LOGGER.error("Failed to save client config to {}.", path, exception);
        }
    }
}
