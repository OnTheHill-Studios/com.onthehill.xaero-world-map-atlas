package com.onthehill.xaeroworldmapbook;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.onthehill.xaeroworldmapbook.command.MapAccessCommands;
import com.onthehill.xaeroworldmapbook.config.MapAccessConfig;
import com.onthehill.xaeroworldmapbook.item.ModItems;
import com.onthehill.xaeroworldmapbook.network.MapAccessAdvancementStatusPayload;
import com.onthehill.xaeroworldmapbook.network.MapAccessNetworking;
import com.onthehill.xaeroworldmapbook.progression.ChunkVisitLedger;
import com.onthehill.xaeroworldmapbook.progression.ChunkVisitTracker;
import com.onthehill.xaeroworldmapbook.progression.WellTraveledTrigger;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.commands.Commands;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common (main) entrypoint. Hosts Spec 001's map-access gating:
 * {@code MapAccessConfig} load/persist, map-access networking, the
 * {@code -admin mapaccess} command tree, the registered
 * {@link WellTraveledTrigger}, and the server-tick-driven
 * {@link ChunkVisitTracker}.
 *
 * <p>Nothing in {@link #onInitialize()} does genuinely server-only work
 * eagerly — config loading and the tick hooks are deferred into
 * {@code ServerLifecycleEvents.SERVER_STARTED}, per the Fabric mod standard,
 * so a pure client that never hosts anything never touches a server config
 * file or runs a live tick handler for it.
 */
public class XaeroWorldMapBook implements ModInitializer
{
    public static final String MOD_ID = "xaero-world-map-book";

    /**
     * How many server ticks pass between advancement-status re-checks for each connected player. Not every tick,
     * since {@link #sendAdvancementStatusIfChanged(ServerPlayer)} only actually sends a packet when the status
     * changed, but the check itself still doesn't need to run every single tick.
     */
    private static final int ADVANCEMENT_STATUS_CHECK_INTERVAL_TICKS = 20;

    /**
     * Vanilla's own "visited every biome type" advancement — confirmed via the real vanilla advancement data
     * (not guessed) at {@code data/minecraft/advancement/adventure/adventuring_time.json}. The alternate
     * {@link com.onthehill.xaeroworldmapbook.config.MinimapAdvancementGate#ADVENTURING_TIME} option checks this
     * instead of this mod's own {@code well_traveled}.
     */
    private static final Identifier ADVENTURING_TIME_ID = Identifier.fromNamespaceAndPath("minecraft", "adventure/adventuring_time");

    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /**
     * Whether this mod's own diagnostic {@code LOGGER.debug(...)} calls (the map-access gate's keybind/toast/sync
     * tracing added for Spec 001) actually fire. Off by default — set the {@code -Dxaero-world-map-book.debug=true}
     * JVM system property to turn it on for a specific run when diagnosing a live gating/toast issue again, per the
     * project owner's explicit request that this diagnostic logging not run unconditionally in normal play. This is
     * independent of the SLF4J/log4j level the run is configured with; every call site guarded by this flag calls
     * through a small {@code debug(...)} wrapper instead of {@code LOGGER.debug(...)} directly, so enabling it here
     * is the single place that turns all of them on or off together.
     */
    public static final boolean DEBUG_LOGGING_ENABLED = Boolean.getBoolean(MOD_ID + ".debug");

    private static MapAccessConfig mapAccessConfig;
    private static final Map<UUID, ChunkVisitLedger> chunkVisitLedgers = new HashMap<>();
    private static ChunkVisitTracker chunkVisitTracker;
    private static final Map<UUID, MapAccessAdvancementStatusPayload> lastSentAdvancementStatus = new HashMap<>();

    @Override
    public void onInitialize()
    {
        ModItems.init();
        WellTraveledTrigger.register(id("well_traveled"));

        // Type registration is unconditional and safe on every physical
        // install — see MapAccessNetworking's own Javadoc for why this must
        // not be bundled with the deferred receiver registration below.
        MapAccessNetworking.registerPayloadTypes();

        // Registering the event listeners themselves is safe pre-server-start;
        // the executors inside resolve the config lazily via a Supplier,
        // since it is not populated until SERVER_STARTED fires below.
        ModCommands.registerServerCommands();
        MapAccessCommands.registerServerCommands(() -> mapAccessConfig, XaeroWorldMapBook::onMapAccessConfigApplied);

        ServerLifecycleEvents.SERVER_STARTED.register(XaeroWorldMapBook::onServerStarted);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
        {
            sendFullSyncTo(handler.getPlayer());
            loadChunkVisitLedgerFor(handler.getPlayer());
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
        {
            ServerPlayer player = handler.getPlayer();
            saveChunkVisitLedgerFor(player);
            if (chunkVisitTracker != null)
            {
                chunkVisitTracker.onPlayerDisconnect(player.getUUID());
            }
            lastSentAdvancementStatus.remove(player.getUUID());
        });
    }

    private static void onServerStarted(MinecraftServer server)
    {
        Path mapAccessConfigPath = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID).resolve("map-access.json");
        mapAccessConfig = MapAccessConfig.loadOrCreate(mapAccessConfigPath);
        chunkVisitTracker = new ChunkVisitTracker(chunkVisitLedgers);

        MapAccessNetworking.registerServerReceivers(() -> mapAccessConfig, XaeroWorldMapBook::onMapAccessConfigApplied);
        ServerTickEvents.END_SERVER_TICK.register(XaeroWorldMapBook::onAdvancementStatusCheckTick);
        ServerTickEvents.END_SERVER_TICK.register(XaeroWorldMapBook::onChunkVisitTrackerTick);
    }

    private static int ticksSinceLastAdvancementStatusCheck = 0;

    private static void onAdvancementStatusCheckTick(MinecraftServer server)
    {
        ticksSinceLastAdvancementStatusCheck++;
        if (ticksSinceLastAdvancementStatusCheck >= ADVANCEMENT_STATUS_CHECK_INTERVAL_TICKS)
        {
            ticksSinceLastAdvancementStatusCheck = 0;
            for (ServerPlayer player : server.getPlayerList().getPlayers())
            {
                sendAdvancementStatusIfChanged(player);
            }
        }
    }

    /**
     * Sends {@code player} their current {@code craft_atlas}/{@code well_traveled} status, but only if it differs
     * from what was last sent — see {@link MapAccessAdvancementStatusPayload}'s Javadoc for why this networked
     * push exists at all (this Minecraft version's client-side advancement tracking has no public accessor this
     * mod's gate checks could otherwise read).
     *
     * @param player The player to check and potentially notify.
     */
    private static void sendAdvancementStatusIfChanged(ServerPlayer player)
    {
        MapAccessAdvancementStatusPayload current = new MapAccessAdvancementStatusPayload(
            isAdvancementDone(player, "craft_atlas"), isAdvancementDone(player, "well_traveled"),
            isAdvancementDone(player, ADVENTURING_TIME_ID));
        MapAccessAdvancementStatusPayload last = lastSentAdvancementStatus.get(player.getUUID());
        if (!current.equals(last))
        {
            debug("Advancement status changed for {}: {} -> {}", player.getGameProfile().name(), last, current);
            lastSentAdvancementStatus.put(player.getUUID(), current);
            ServerPlayNetworking.send(player, current);
        }
    }

    /**
     * Logs at debug level only when {@link #DEBUG_LOGGING_ENABLED} is set — every diagnostic call site in this mod
     * (client and server alike) goes through this or the equivalent client-side wrapper rather than calling
     * {@link #LOGGER}{@code .debug(...)} directly, so the flag is the single place that turns all of them on or off.
     */
    public static void debug(String format, Object... args)
    {
        if (DEBUG_LOGGING_ENABLED)
        {
            LOGGER.debug(format, args);
        }
    }

    private static void onChunkVisitTrackerTick(MinecraftServer server)
    {
        chunkVisitTracker.tick(server.getPlayerList().getPlayers(), XaeroWorldMapBook::onNewChunkVisited);
    }

    private static void onNewChunkVisited(ServerPlayer player, int newTotalChunkCount)
    {
        if (!isAdvancementDone(player, "craft_atlas"))
        {
            // Per the spec's own floor rule: well_traveled can only ever be
            // awarded after craft_atlas — stop tracking the threshold
            // comparison's outcome for a player who hasn't earned it yet,
            // rather than repeatedly awarding a trigger that will not result
            // in the advancement anyway.
            return;
        }

        if (isAdvancementDone(player, "well_traveled"))
        {
            return;
        }

        if (newTotalChunkCount >= mapAccessConfig.getChunksRequiredForWellTraveled())
        {
            WellTraveledTrigger.INSTANCE.trigger(player);
        }
    }

    private static boolean isAdvancementDone(ServerPlayer player, String pathInThisModsNamespace)
    {
        return isAdvancementDone(player, id(pathInThisModsNamespace));
    }

    /**
     * Checks whether {@code player} has been awarded the advancement identified by {@code advancementId} —
     * accepts an arbitrary {@link Identifier} (not just one in this mod's own namespace) so vanilla advancements
     * like {@link #ADVENTURING_TIME_ID} can be queried the same way as this mod's own.
     *
     * @param player The player to check.
     * @param advancementId The advancement's full identifier, in whichever namespace it actually belongs to.
     * @return {@code true} if the advancement exists and this player has completed it.
     */
    private static boolean isAdvancementDone(ServerPlayer player, Identifier advancementId)
    {
        MinecraftServer server = player.level().getServer();
        if (server == null)
        {
            return false;
        }
        AdvancementHolder holder = server.getAdvancements().get(advancementId);
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    private static void loadChunkVisitLedgerFor(ServerPlayer player)
    {
        Path ledgerPath = chunkVisitLedgerPath(player.getUUID());
        chunkVisitLedgers.put(player.getUUID(), ChunkVisitLedger.loadOrCreate(ledgerPath));
    }

    private static void saveChunkVisitLedgerFor(ServerPlayer player)
    {
        ChunkVisitLedger ledger = chunkVisitLedgers.get(player.getUUID());
        if (ledger != null)
        {
            ledger.save(chunkVisitLedgerPath(player.getUUID()));
        }
    }

    private static Path chunkVisitLedgerPath(UUID playerId)
    {
        return FabricLoader.getInstance().getConfigDir().resolve(MOD_ID)
            .resolve("chunk-visits").resolve(playerId + ".json");
    }

    private static void onMapAccessConfigApplied(ServerPlayer requester, MapAccessConfig config)
    {
        LOGGER.debug(
            "Map-access config applied by {}: keybindOpenEnabled={}, keybindItemRequirement={}, "
                + "minimapAccessRequirement={}, chunksRequiredForWellTraveled={}, minimapAdvancementGate={}, creativeBypassEnabled={}",
            requester.getGameProfile().name(), config.isKeybindOpenEnabled(), config.getKeybindItemRequirement(),
            config.getMinimapAccessRequirement(), config.getChunksRequiredForWellTraveled(),
            config.getMinimapAdvancementGate(), config.isCreativeBypassEnabled());

        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID).resolve("map-access.json");
        config.save(configPath);

        MinecraftServer server = requester.level().getServer();
        if (server == null)
        {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            boolean operator = Commands.LEVEL_GAMEMASTERS.check(player.permissions());
            ServerPlayNetworking.send(player, MapAccessNetworking.buildSyncPayloadFor(config, operator));
        }
    }

    private static void sendFullSyncTo(ServerPlayer player)
    {
        if (mapAccessConfig != null)
        {
            boolean operator = Commands.LEVEL_GAMEMASTERS.check(player.permissions());
            ServerPlayNetworking.send(player, MapAccessNetworking.buildSyncPayloadFor(mapAccessConfig, operator));
            sendAdvancementStatusIfChanged(player);
        }
    }

    public static Identifier id(String path)
    {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
