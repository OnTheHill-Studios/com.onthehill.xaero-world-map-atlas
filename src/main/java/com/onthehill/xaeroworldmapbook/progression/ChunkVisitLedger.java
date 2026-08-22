package com.onthehill.xaeroworldmapbook.progression;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pure, Minecraft-independent record of which chunks a single player has
 * physically visited, keyed by a packed {@code long} that must already
 * disambiguate dimension from raw chunk position — see
 * {@link ChunkVisitTracker} for how the real packed key is constructed from
 * a live {@code ServerPlayer}. This class has no opinion on how that packing
 * works; it only ever compares the {@code long} values it is given.
 *
 * <p>Never shrinks: a chunk once recorded stays recorded for the lifetime of
 * this ledger, matching the "well traveled" milestone's intent as a
 * monotonically-increasing exploration count rather than a currently-loaded
 * set.
 */
public final class ChunkVisitLedger
{
    private static final Logger LOGGER = LoggerFactory.getLogger("xaero-world-map-book/chunk-visit-ledger");
    private static final Gson GSON = new Gson();

    private final Set<Long> visitedChunkKeys = new HashSet<>();

    /**
     * Records a visit to the chunk identified by the given packed key.
     *
     * @param chunkKey Packed chunk identifier, already disambiguated across
     *     dimensions by the caller (see {@link ChunkVisitTracker}).
     * @return {@code true} if this chunk had never been recorded before by
     *     this ledger; {@code false} if it was already present.
     */
    public boolean recordVisit(long chunkKey)
    {
        return this.visitedChunkKeys.add(chunkKey);
    }

    /**
     * The total number of distinct chunks recorded so far.
     *
     * @return Count of distinct chunk keys this ledger has ever recorded.
     */
    public int size()
    {
        return this.visitedChunkKeys.size();
    }

    /**
     * Writes this ledger's visited-chunk set to the given path as JSON, so it persists across logout/relog and a
     * server restart.
     *
     * @param path Path to this player's persisted ledger file.
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
                GSON.toJson(this.visitedChunkKeys, writer);
            }
        }
        catch (IOException exception)
        {
            LOGGER.error("Failed to save chunk visit ledger to {}.", path, exception);
        }
    }

    /**
     * Loads a ledger from the given path, or returns a fresh empty ledger if the file does not exist or is
     * unreadable.
     *
     * @param path Path to a player's persisted ledger file.
     * @return The loaded (or freshly-created empty) ledger.
     */
    public static ChunkVisitLedger loadOrCreate(Path path)
    {
        ChunkVisitLedger ledger = new ChunkVisitLedger();

        if (Files.exists(path))
        {
            try (Reader reader = Files.newBufferedReader(path))
            {
                Set<Long> loaded = GSON.fromJson(reader, new TypeToken<Set<Long>>() { }.getType());
                if (loaded != null)
                {
                    ledger.visitedChunkKeys.addAll(loaded);
                }
            }
            catch (IOException exception)
            {
                LOGGER.warn("Failed to read chunk visit ledger at {}, starting empty.", path, exception);
            }
        }

        return ledger;
    }
}
