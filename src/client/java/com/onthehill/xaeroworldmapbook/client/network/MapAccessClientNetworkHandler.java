package com.onthehill.xaeroworldmapbook.client.network;

import com.onthehill.xaeroworldmapbook.XaeroWorldMapBook;
import com.onthehill.xaeroworldmapbook.client.screen.ProgressConfigScreen;
import com.onthehill.xaeroworldmapbook.config.MapAccessConfig;
import com.onthehill.xaeroworldmapbook.network.MapAccessAdvancementStatusPayload;
import com.onthehill.xaeroworldmapbook.network.MapAccessConfigSyncPayload;
import com.onthehill.xaeroworldmapbook.network.OpenAdminScreenPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

/**
 * Registers this mod's client-side receivers for Spec 001's map-access
 * payloads. Lives under the client source set (not {@code network}, which is
 * shared with the {@code main} entrypoint) because {@code ClientPlayNetworking}
 * is a client-only class.
 *
 * <p>Also holds the last-synced {@link MapAccessConfig} snapshot
 * {@code client.config.MapAccessClientState} reads from every client tick.
 * Also registers the receiver for {@code OpenAdminScreenPayload} — the mod
 * has exactly one consolidated config screen
 * ({@code client.screen.ProgressConfigScreen}), and this is now the sole
 * remaining client-side network handler, so that payload's receiver lives
 * here rather than in a separate progress-feature-specific class.
 */
public final class MapAccessClientNetworkHandler
{
    private static boolean syncReceived = false;
    private static boolean operator = false;
    private static boolean permitted = false;
    private static MapAccessConfig lastSyncedConfig = defaultsConfig();
    private static boolean craftAtlasEarned = false;
    private static boolean wellTraveledEarned = false;
    private static boolean adventuringTimeEarned = false;

    private MapAccessClientNetworkHandler() { }

    /**
     * Registers every client-side receiver for Spec 001's payloads. Call once from
     * {@code XaeroWorldMapBookClient.onInitializeClient()}.
     */
    public static void registerReceivers()
    {
        ClientPlayNetworking.registerGlobalReceiver(MapAccessConfigSyncPayload.ID, (payload, context) ->
            context.client().execute(() ->
            {
                syncReceived = true;
                operator = payload.operator();
                permitted = payload.permitted();

                MapAccessConfig config = new MapAccessConfig();
                config.apply(
                    payload.keybindOpenEnabled(), payload.keybindItemRequirement(), payload.minimapAccessRequirement(),
                    payload.chunksRequiredForWellTraveled(), payload.minimapAdvancementGate(), payload.creativeBypassEnabled(),
                    payload.allowNonOpReadOnlyView());
                lastSyncedConfig = config;

                XaeroWorldMapBook.debug(
                    "Received MapAccessConfigSyncPayload: operator={}, permitted={}, keybindOpenEnabled={}, "
                        + "keybindItemRequirement={}, minimapAccessRequirement={}, chunksRequiredForWellTraveled={}, "
                        + "minimapAdvancementGate={}, creativeBypassEnabled={}, allowNonOpReadOnlyView={}",
                    operator, permitted, payload.keybindOpenEnabled(), payload.keybindItemRequirement(),
                    payload.minimapAccessRequirement(), payload.chunksRequiredForWellTraveled(),
                    payload.minimapAdvancementGate(), payload.creativeBypassEnabled(), payload.allowNonOpReadOnlyView());
            }));

        ClientPlayNetworking.registerGlobalReceiver(MapAccessAdvancementStatusPayload.ID, (payload, context) ->
            context.client().execute(() ->
            {
                craftAtlasEarned = payload.craftAtlasEarned();
                wellTraveledEarned = payload.wellTraveledEarned();
                adventuringTimeEarned = payload.adventuringTimeEarned();

                XaeroWorldMapBook.debug(
                    "Received MapAccessAdvancementStatusPayload: craftAtlasEarned={}, wellTraveledEarned={}, adventuringTimeEarned={}",
                    craftAtlasEarned, wellTraveledEarned, adventuringTimeEarned);
            }));

        ClientPlayNetworking.registerGlobalReceiver(OpenAdminScreenPayload.ID, (payload, context) ->
            context.client().execute(() ->
            {
                Minecraft client = context.client();
                client.setScreenAndShow(ProgressConfigScreen.create(client.gui.screen()));
            }));
    }

    /**
     * The last confirmed-current server config values, or a default-valued config if nothing has synced yet — read
     * by {@code client.config.MapAccessClientState} every tick for gate evaluation.
     *
     * @return The last-synced config, never {@code null}.
     */
    public static MapAccessConfig getLastSyncedConfigOrDefaults()
    {
        return lastSyncedConfig;
    }

    public static boolean isSyncReceived()
    {
        return syncReceived;
    }

    public static boolean isOperator()
    {
        return operator;
    }

    public static boolean isPermitted()
    {
        return permitted;
    }

    /**
     * Whether the local player currently holds {@code craft_atlas}, per the last {@link MapAccessAdvancementStatusPayload} received.
     *
     * @return The last-synced {@code craft_atlas} status.
     */
    public static boolean isCraftAtlasEarned()
    {
        return craftAtlasEarned;
    }

    /**
     * Whether the local player currently holds {@code well_traveled}, per the last {@link MapAccessAdvancementStatusPayload} received.
     *
     * @return The last-synced {@code well_traveled} status.
     */
    public static boolean isWellTraveledEarned()
    {
        return wellTraveledEarned;
    }

    /**
     * Whether the local player currently holds vanilla's own {@code minecraft:adventure/adventuring_time}, per the
     * last {@link MapAccessAdvancementStatusPayload} received.
     *
     * @return The last-synced {@code adventuring_time} status.
     */
    public static boolean isAdventuringTimeEarned()
    {
        return adventuringTimeEarned;
    }

    /**
     * Clears all synced state on disconnect, so a stale value from a previous server/world is never used after
     * joining a new one.
     */
    public static void reset()
    {
        syncReceived = false;
        operator = false;
        permitted = false;
        lastSyncedConfig = defaultsConfig();
        craftAtlasEarned = false;
        wellTraveledEarned = false;
        adventuringTimeEarned = false;
    }

    private static MapAccessConfig defaultsConfig()
    {
        MapAccessConfig config = new MapAccessConfig();
        config.apply(
            MapAccessConfig.DEFAULT_KEYBIND_OPEN_ENABLED,
            MapAccessConfig.DEFAULT_KEYBIND_ITEM_REQUIREMENT,
            MapAccessConfig.DEFAULT_MINIMAP_ACCESS_REQUIREMENT,
            MapAccessConfig.DEFAULT_CHUNKS_REQUIRED_FOR_WELL_TRAVELED,
            MapAccessConfig.DEFAULT_MINIMAP_ADVANCEMENT_GATE,
            MapAccessConfig.DEFAULT_CREATIVE_BYPASS_ENABLED,
            MapAccessConfig.DEFAULT_ALLOW_NON_OP_READ_ONLY_VIEW);
        return config;
    }
}
