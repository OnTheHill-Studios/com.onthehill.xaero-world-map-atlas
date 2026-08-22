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
 * Server-authoritative configuration for Spec 001's Atlas item-gating
 * feature. Every field here governs world/gameplay state that must be
 * identical for every connected player, so the server's own JSON file is the
 * single source of truth — clients only ever see these values via
 * {@code network.MapAccessConfigSyncPayload}.
 *
 * <p>This is now the mod's only server-authoritative config object (the
 * earlier example tick-progress feature and its own
 * {@code ServerProgressConfig} were removed as leftover template
 * scaffolding), so it also carries the mod's one mandatory "allow non-op
 * read-only view" setting directly — there is no longer a second config
 * object for it to be shared with.
 *
 * <p>All three of this feature's config surfaces (the admin GUI tab, the
 * {@code /xaero-world-map-book-admin mapaccess} commands, and this JSON file)
 * call {@link #validate(int)} before applying a change, so no surface can
 * drift out of sync with what another surface considers valid.
 */
public final class MapAccessConfig
{
    private static final Logger LOGGER = LoggerFactory.getLogger("xaero-world-map-book/config");

    public static final boolean DEFAULT_KEYBIND_OPEN_ENABLED = true;
    public static final KeybindItemRequirement DEFAULT_KEYBIND_ITEM_REQUIREMENT = KeybindItemRequirement.HOTBAR;
    public static final MinimapAccessRequirement DEFAULT_MINIMAP_ACCESS_REQUIREMENT = MinimapAccessRequirement.MAIN_OR_OFFHAND;
    public static final int DEFAULT_CHUNKS_REQUIRED_FOR_WELL_TRAVELED = 300;
    public static final MinimapAdvancementGate DEFAULT_MINIMAP_ADVANCEMENT_GATE = MinimapAdvancementGate.WELL_TRAVELED;

    /**
     * Default for whether Creative-mode players bypass every Atlas/advancement requirement entirely. Must default
     * to {@code false} — an admin has to deliberately opt into the bypass, it must never come out enabled by
     * omission.
     */
    public static final boolean DEFAULT_CREATIVE_BYPASS_ENABLED = false;

    /**
     * Default for whether non-operator players may see a read-only view of server-authoritative settings. Must
     * default to {@code true} per the studio config standard's mandatory read-only-view setting.
     */
    public static final boolean DEFAULT_ALLOW_NON_OP_READ_ONLY_VIEW = true;

    /** Smallest permitted chunk-travel milestone — must always require at least one visited chunk. */
    public static final int MIN_CHUNKS_REQUIRED_FOR_WELL_TRAVELED = 1;

    /** Largest permitted chunk-travel milestone, chosen as a generous upper bound against fat-fingered admin input. */
    public static final int MAX_CHUNKS_REQUIRED_FOR_WELL_TRAVELED = 1_000_000;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private boolean keybindOpenEnabled = DEFAULT_KEYBIND_OPEN_ENABLED;
    private KeybindItemRequirement keybindItemRequirement = DEFAULT_KEYBIND_ITEM_REQUIREMENT;
    private MinimapAccessRequirement minimapAccessRequirement = DEFAULT_MINIMAP_ACCESS_REQUIREMENT;
    private int chunksRequiredForWellTraveled = DEFAULT_CHUNKS_REQUIRED_FOR_WELL_TRAVELED;
    private MinimapAdvancementGate minimapAdvancementGate = DEFAULT_MINIMAP_ADVANCEMENT_GATE;
    private boolean creativeBypassEnabled = DEFAULT_CREATIVE_BYPASS_ENABLED;
    private boolean allowNonOpReadOnlyView = DEFAULT_ALLOW_NON_OP_READ_ONLY_VIEW;

    /**
     * Whether the gated {@code M} keybind is enabled at all.
     *
     * @return {@code true} if the keybind may currently open Xaero's World Map, subject to the item requirement.
     */
    public boolean isKeybindOpenEnabled()
    {
        return this.keybindOpenEnabled;
    }

    /**
     * Which item-location rule the gated keybind currently enforces.
     *
     * @return The active {@link KeybindItemRequirement}.
     */
    public KeybindItemRequirement getKeybindItemRequirement()
    {
        return this.keybindItemRequirement;
    }

    /**
     * Which holding rule the Minimap visibility gate currently enforces, on top of the {@code craft_atlas} floor.
     *
     * @return The active {@link MinimapAccessRequirement}.
     */
    public MinimapAccessRequirement getMinimapAccessRequirement()
    {
        return this.minimapAccessRequirement;
    }

    /**
     * Distinct chunks a player must have physically visited, after earning {@code craft_atlas}, to be awarded
     * {@code well_traveled}.
     *
     * @return The currently configured chunk-travel milestone.
     */
    public int getChunksRequiredForWellTraveled()
    {
        return this.chunksRequiredForWellTraveled;
    }

    /**
     * Which advancement satisfies {@link MinimapAccessRequirement#ADVANCEMENT_ONLY}'s mode-specific check.
     *
     * @return The active {@link MinimapAdvancementGate}.
     */
    public MinimapAdvancementGate getMinimapAdvancementGate()
    {
        return this.minimapAdvancementGate;
    }

    /**
     * Whether Creative-mode players bypass every Atlas/advancement requirement entirely — both the gated keybind's
     * item/advancement check and the Minimap's floor-plus-mode-specific check. Does <em>not</em> bypass
     * {@link #isKeybindOpenEnabled()} being {@code false} — that is a deliberate whole-feature off switch, not a
     * requirement to bypass.
     *
     * @return {@code true} if Creative-mode players skip every requirement check.
     */
    public boolean isCreativeBypassEnabled()
    {
        return this.creativeBypassEnabled;
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
     * {@link #validate(int)} first and only call this once the proposed
     * change set is confirmed valid — this method does not itself reject bad input.
     *
     * @param keybindOpenEnabled New keybind-enabled setting.
     * @param keybindItemRequirement New keybind item-location rule.
     * @param minimapAccessRequirement New Minimap holding rule.
     * @param chunksRequiredForWellTraveled New chunk-travel milestone. Must already be validated.
     * @param minimapAdvancementGate New advancement source for {@code ADVANCEMENT_ONLY} mode.
     * @param creativeBypassEnabled New Creative-mode bypass setting.
     * @param allowNonOpReadOnlyView New read-only-view setting.
     */
    public void apply(
        boolean keybindOpenEnabled,
        KeybindItemRequirement keybindItemRequirement,
        MinimapAccessRequirement minimapAccessRequirement,
        int chunksRequiredForWellTraveled,
        MinimapAdvancementGate minimapAdvancementGate,
        boolean creativeBypassEnabled,
        boolean allowNonOpReadOnlyView)
    {
        this.keybindOpenEnabled = keybindOpenEnabled;
        this.keybindItemRequirement = keybindItemRequirement;
        this.minimapAccessRequirement = minimapAccessRequirement;
        this.chunksRequiredForWellTraveled = chunksRequiredForWellTraveled;
        this.minimapAdvancementGate = minimapAdvancementGate;
        this.creativeBypassEnabled = creativeBypassEnabled;
        this.allowNonOpReadOnlyView = allowNonOpReadOnlyView;
    }

    /**
     * Resets every field on this config object back to its default value.
     * Used by both the admin GUI tab's section-wide reset and the
     * {@code /xaero-world-map-book-admin mapaccess reset} command.
     */
    public void resetToDefaults()
    {
        this.keybindOpenEnabled = DEFAULT_KEYBIND_OPEN_ENABLED;
        this.keybindItemRequirement = DEFAULT_KEYBIND_ITEM_REQUIREMENT;
        this.minimapAccessRequirement = DEFAULT_MINIMAP_ACCESS_REQUIREMENT;
        this.chunksRequiredForWellTraveled = DEFAULT_CHUNKS_REQUIRED_FOR_WELL_TRAVELED;
        this.minimapAdvancementGate = DEFAULT_MINIMAP_ADVANCEMENT_GATE;
        this.creativeBypassEnabled = DEFAULT_CREATIVE_BYPASS_ENABLED;
        this.allowNonOpReadOnlyView = DEFAULT_ALLOW_NON_OP_READ_ONLY_VIEW;
    }

    /**
     * Validates a proposed chunk-travel milestone against this config's bounds. This is the single shared
     * validation path — the admin GUI's save handler, the {@code /xaero-world-map-book-admin mapaccess} command,
     * and JSON-load fallback all call this same method rather than each re-implementing the range check.
     *
     * @param proposedChunksRequiredForWellTraveled The candidate value to validate.
     * @return A list of human-readable violation messages naming the field and its bounds. Empty if the value is valid.
     */
    public static List<String> validate(int proposedChunksRequiredForWellTraveled)
    {
        List<String> violations = new ArrayList<>();
        if (proposedChunksRequiredForWellTraveled < MIN_CHUNKS_REQUIRED_FOR_WELL_TRAVELED
            || proposedChunksRequiredForWellTraveled > MAX_CHUNKS_REQUIRED_FOR_WELL_TRAVELED)
        {
            violations.add(String.format(
                "chunksRequiredForWellTraveled must be between %d and %d, got %d",
                MIN_CHUNKS_REQUIRED_FOR_WELL_TRAVELED, MAX_CHUNKS_REQUIRED_FOR_WELL_TRAVELED,
                proposedChunksRequiredForWellTraveled));
        }
        return violations;
    }

    /**
     * Loads the server config from the given path, falling back to defaults for any missing/invalid key and
     * writing the merged result straight back to disk. If the file does not exist yet, a fresh default-valued
     * config is created and saved.
     *
     * @param path Path to the server-side JSON config file.
     * @return The loaded (or newly-created default) config.
     */
    public static MapAccessConfig loadOrCreate(Path path)
    {
        MapAccessConfig config = new MapAccessConfig();

        if (Files.exists(path))
        {
            try (Reader reader = Files.newBufferedReader(path))
            {
                MapAccessConfig loaded = GSON.fromJson(reader, MapAccessConfig.class);
                if (loaded != null)
                {
                    config.keybindOpenEnabled = loaded.keybindOpenEnabled;
                    config.keybindItemRequirement = loaded.keybindItemRequirement != null
                        ? loaded.keybindItemRequirement : DEFAULT_KEYBIND_ITEM_REQUIREMENT;
                    config.minimapAccessRequirement = loaded.minimapAccessRequirement != null
                        ? loaded.minimapAccessRequirement : DEFAULT_MINIMAP_ACCESS_REQUIREMENT;
                    config.minimapAdvancementGate = loaded.minimapAdvancementGate != null
                        ? loaded.minimapAdvancementGate : DEFAULT_MINIMAP_ADVANCEMENT_GATE;
                    config.creativeBypassEnabled = loaded.creativeBypassEnabled;
                    config.allowNonOpReadOnlyView = loaded.allowNonOpReadOnlyView;

                    List<String> violations = validate(loaded.chunksRequiredForWellTraveled);
                    if (violations.isEmpty())
                    {
                        config.chunksRequiredForWellTraveled = loaded.chunksRequiredForWellTraveled;
                    }
                    else
                    {
                        LOGGER.warn("Ignoring invalid chunksRequiredForWellTraveled loaded from {}: {}", path, violations);
                    }
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
