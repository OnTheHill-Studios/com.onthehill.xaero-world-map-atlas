package com.onthehill.xaeroworldmapbook.progression;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * Impure, per-player driver for chunk-travel tracking: computes each online
 * player's current chunk position every {@link #CHECK_INTERVAL_TICKS} server
 * ticks, feeds newly-entered chunks into that player's persisted
 * {@link ChunkVisitLedger}, and reports the running distinct-chunk count so
 * the caller can decide when to award {@code well_traveled}.
 *
 * <p>Deliberately position-based rather than derived from a
 * {@code ServerLevel}'s already-generated-chunk count or render/simulation
 * distance — those reflect what the world has generated (including
 * pregeneration with no player ever present), not where a specific player
 * has actually walked, flown, or ridden.
 */
public final class ChunkVisitTracker
{
    /**
     * How many server ticks pass between chunk-position checks. A chunk is
     * 16 blocks wide and no legitimate player movement crosses that in under
     * several ticks even at extreme speed, so this throttle costs negligible
     * detection latency for a real, proportional reduction in per-tick,
     * per-online-player iteration cost.
     */
    public static final int CHECK_INTERVAL_TICKS = 5;

    private final Map<UUID, ChunkVisitLedger> ledgersByPlayer;
    private final Map<UUID, Long> lastKnownChunkByPlayer = new HashMap<>();
    private int ticksSinceLastCheck = 0;

    /**
     * @param ledgersByPlayer Backing store for each online player's persisted ledger, keyed by player UUID —
     *     supplied externally so the caller controls how/where ledgers are actually persisted across sessions.
     */
    public ChunkVisitTracker(Map<UUID, ChunkVisitLedger> ledgersByPlayer)
    {
        this.ledgersByPlayer = ledgersByPlayer;
    }

    /**
     * Packs a player's current dimension and chunk position into a single disambiguated key. Uses a
     * self-contained bit layout (24 bits of dimension-identifier hash, 20 bits each for chunk X/Z) rather than
     * reusing {@code ChunkPos.asLong()}'s own packing, so this class carries no dependency on that method's exact
     * bit layout remaining unchanged across Minecraft versions.
     *
     * @param dimension The player's current dimension.
     * @param chunkX The player's current chunk X coordinate.
     * @param chunkZ The player's current chunk Z coordinate.
     * @return A packed key unique to this (dimension, chunkX, chunkZ) combination.
     */
    public static long packChunkKey(ResourceKey<Level> dimension, int chunkX, int chunkZ)
    {
        long dimensionComponent = ((long) dimension.identifier().hashCode() & 0xFFFFFF) << 40;
        long xComponent = ((long) chunkX & 0xFFFFF) << 20;
        long zComponent = (long) chunkZ & 0xFFFFF;
        return dimensionComponent | xComponent | zComponent;
    }

    /**
     * Runs one check pass, if enough ticks have elapsed since the last one. Call once per server tick from
     * {@code ServerTickEvents.END_SERVER_TICK}.
     *
     * @param onlinePlayers Currently online players to check.
     * @param onNewChunkVisited Invoked with a player and their ledger's new total distinct-chunk count, once per
     *     player, only when that player entered a chunk they had not visited before.
     */
    public void tick(Iterable<ServerPlayer> onlinePlayers, BiConsumer<ServerPlayer, Integer> onNewChunkVisited)
    {
        this.ticksSinceLastCheck++;
        if (this.ticksSinceLastCheck < CHECK_INTERVAL_TICKS)
        {
            return;
        }
        this.ticksSinceLastCheck = 0;

        for (ServerPlayer player : onlinePlayers)
        {
            checkPlayer(player, onNewChunkVisited);
        }
    }

    private void checkPlayer(ServerPlayer player, BiConsumer<ServerPlayer, Integer> onNewChunkVisited)
    {
        BlockPos pos = player.blockPosition();
        ChunkPos chunkPos = ChunkPos.containing(pos);
        long packedKey = packChunkKey(player.level().dimension(), chunkPos.x(), chunkPos.z());

        UUID playerId = player.getUUID();
        Long lastKnown = this.lastKnownChunkByPlayer.get(playerId);
        if (lastKnown != null && lastKnown == packedKey)
        {
            return;
        }
        this.lastKnownChunkByPlayer.put(playerId, packedKey);

        ChunkVisitLedger ledger = this.ledgersByPlayer.computeIfAbsent(playerId, ignored -> new ChunkVisitLedger());
        if (ledger.recordVisit(packedKey))
        {
            onNewChunkVisited.accept(player, ledger.size());
        }
    }

    /**
     * Removes a player's in-memory "last known chunk" cache entry on disconnect, so a stale cached position from a
     * previous session never suppresses the first real check of their next one. The persisted ledger itself is
     * untouched — only this transient dedupe cache is cleared.
     *
     * @param playerId The disconnecting player's UUID.
     */
    public void onPlayerDisconnect(UUID playerId)
    {
        this.lastKnownChunkByPlayer.remove(playerId);
    }
}
