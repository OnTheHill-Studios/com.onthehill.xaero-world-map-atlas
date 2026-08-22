package com.onthehill.xaeroworldmapbook.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server-authoritative configuration for the example tick-progress feature.
 * Every field here governs world/gameplay state that must be identical for
 * every connected player, so the server's own JSON file is the single source
 * of truth — clients only ever see these values via {@code AdminConfigSyncPayload}.
 *
 * <p>All three of this mod's config surfaces (the admin GUI tab, the
 * {@code /xaero-world-map-book-admin config} commands, and this JSON file) call
 * {@link #validate(float)} before applying a change, so no surface can drift
 * out of sync with what another surface considers valid.
 */
public final class ServerProgressConfig
{
    private static final Logger LOGGER = LoggerFactory.getLogger("xaero-world-map-book/config");

    /**
     * Default per-tick progress rate: completes one full cycle in 100 ticks (5 seconds).
     */
    public static final float DEFAULT_PROGRESS_RATE = 0.01f;

    /**
     * Default for whether non-operator players may see a read-only view of
     * server-authoritative settings. Must default to {@code true} per the
     * studio config standard's mandatory read-only-view setting.
     */
    public static final boolean DEFAULT_ALLOW_NON_OP_READ_ONLY_VIEW = true;

    /**
     * Smallest permitted progress rate — small but strictly positive, so the
     * value always visibly advances rather than appearing to stall.
     */
    public static final float MIN_PROGRESS_RATE = 0.0001f;

    /**
     * Largest permitted progress rate. Must stay below 1.0 so at least one
     * tick is always visible before the value resets, per the feature's own
     * "rate is less than 1" requirement.
     */
    public static final float MAX_PROGRESS_RATE = 0.999f;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private float progressRate = DEFAULT_PROGRESS_RATE;
    private boolean allowNonOpReadOnlyView = DEFAULT_ALLOW_NON_OP_READ_ONLY_VIEW;

    /**
     * Per-tick amount the example progress value advances by. Range: (0, 1) exclusive.
     *
     * @return The currently configured progress rate.
     */
    public float getProgressRate()
    {
        return this.progressRate;
    }

    /**
     * Whether non-operator players may see a read-only view of server-authoritative settings.
     *
     * @return {@code true} if non-op players receive real synced values for the admin tab.
     */
    public boolean isAllowNonOpReadOnlyView()
    {
        return this.allowNonOpReadOnlyView;
    }

    /**
     * Applies a fully-validated set of changes atomically. Callers must run
     * {@link #validate(float)} first and only call this once the proposed
     * change set is confirmed valid — this method does not itself reject bad input.
     *
     * @param progressRate New progress rate. Must already be validated.
     * @param allowNonOpReadOnlyView New read-only-view setting.
     */
    public void apply(float progressRate, boolean allowNonOpReadOnlyView)
    {
        this.progressRate = progressRate;
        this.allowNonOpReadOnlyView = allowNonOpReadOnlyView;
    }

    /**
     * Resets every field on this config object back to its default value.
     * Used by both the admin GUI tab's section-wide reset and the
     * {@code /xaero-world-map-book-admin config reset} command.
     */
    public void resetToDefaults()
    {
        this.progressRate = DEFAULT_PROGRESS_RATE;
        this.allowNonOpReadOnlyView = DEFAULT_ALLOW_NON_OP_READ_ONLY_VIEW;
    }

    /**
     * Validates a proposed progress rate against this config's bounds. This
     * is the single shared validation path — the admin GUI's save handler,
     * the {@code /xaero-world-map-book-admin config progress-rate} command, and
     * JSON-load fallback all call this same method rather than each
     * re-implementing the range check.
     *
     * @param proposedProgressRate The candidate value to validate.
     * @return A list of human-readable violation messages naming the field and its bounds.
     *     Empty if the value is valid.
     */
    public static List<String> validate(float proposedProgressRate)
    {
        List<String> violations = new ArrayList<>();
        if (proposedProgressRate < MIN_PROGRESS_RATE || proposedProgressRate > MAX_PROGRESS_RATE)
        {
            violations.add(String.format(
                "progressRate must be between %.4f and %.4f, got %.4f",
                MIN_PROGRESS_RATE, MAX_PROGRESS_RATE, proposedProgressRate));
        }
        return violations;
    }

    /**
     * Loads the server config from the given path, falling back to defaults
     * for any missing key and writing the merged result straight back to
     * disk so the on-disk file always reflects every currently-known field.
     * If the file does not exist yet, a fresh default-valued config is
     * created and saved.
     *
     * @param path Path to the server-side JSON config file.
     * @return The loaded (or newly-created default) config.
     */
    public static ServerProgressConfig loadOrCreate(Path path)
    {
        ServerProgressConfig config = new ServerProgressConfig();

        if (Files.exists(path))
        {
            try (Reader reader = Files.newBufferedReader(path))
            {
                ServerProgressConfig loaded = GSON.fromJson(reader, ServerProgressConfig.class);
                if (loaded != null)
                {
                    List<String> violations = validate(loaded.progressRate);
                    if (violations.isEmpty())
                    {
                        config.progressRate = loaded.progressRate;
                    }
                    else
                    {
                        LOGGER.warn("Ignoring invalid progressRate loaded from {}: {}", path, violations);
                    }
                    config.allowNonOpReadOnlyView = loaded.allowNonOpReadOnlyView;
                }
            }
            catch (IOException exception)
            {
                LOGGER.warn("Failed to read server config at {}, using defaults.", path, exception);
            }
        }

        config.save(path);
        return config;
    }

    /**
     * Writes this config to the given path as pretty-printed JSON.
     *
     * @param path Path to the server-side JSON config file.
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
            LOGGER.error("Failed to save server config to {}.", path, exception);
        }
    }
}
