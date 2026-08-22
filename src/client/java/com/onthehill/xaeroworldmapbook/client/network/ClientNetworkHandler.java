package com.onthehill.xaeroworldmapbook.client.network;

import com.onthehill.xaeroworldmapbook.client.screen.ProgressConfigScreen;
import com.onthehill.xaeroworldmapbook.network.AdminConfigSyncPayload;
import com.onthehill.xaeroworldmapbook.network.OpenAdminScreenPayload;
import com.onthehill.xaeroworldmapbook.network.ProgressSyncPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

/**
 * Registers this mod's client-side packet receivers. Lives under the client
 * source set (not {@code network}, which is shared with the {@code main}
 * entrypoint) because {@code ClientPlayNetworking} is a client-only class —
 * registering it from a class reachable from {@code main} would crash a
 * dedicated server at classload time.
 *
 * <p>Also holds the last-synced progress state the HUD renderer and config
 * screen read from, since it is the natural place packets land.
 */
public final class ClientNetworkHandler
{
    private static float lastSyncedProgress = 0.0f;
    private static float lastSyncedRatePerTick = com.onthehill.xaeroworldmapbook.config.ServerProgressConfig.DEFAULT_PROGRESS_RATE;
    private static int ticksSinceLastSync = 0;

    private static boolean adminSyncReceived = false;
    private static boolean adminOperator = false;
    private static boolean adminPermitted = false;
    private static float adminProgressRate = com.onthehill.xaeroworldmapbook.config.ServerProgressConfig.DEFAULT_PROGRESS_RATE;
    private static boolean adminAllowNonOpReadOnlyView = com.onthehill.xaeroworldmapbook.config.ServerProgressConfig.DEFAULT_ALLOW_NON_OP_READ_ONLY_VIEW;

    private ClientNetworkHandler() { }

    /**
     * Registers every client-side receiver. Call once from
     * {@code XaeroWorldMapBookClient.onInitializeClient()}.
     */
    public static void registerReceivers()
    {
        ClientPlayNetworking.registerGlobalReceiver(ProgressSyncPayload.ID, (payload, context) ->
            context.client().execute(() ->
            {
                lastSyncedProgress = payload.progress();
                lastSyncedRatePerTick = payload.ratePerTick();
                ticksSinceLastSync = 0;
            }));

        ClientPlayNetworking.registerGlobalReceiver(AdminConfigSyncPayload.ID, (payload, context) ->
            context.client().execute(() ->
            {
                adminSyncReceived = true;
                adminOperator = payload.operator();
                adminPermitted = payload.permitted();
                adminProgressRate = payload.progressRate();
                adminAllowNonOpReadOnlyView = payload.allowNonOpReadOnlyView();
            }));

        ClientPlayNetworking.registerGlobalReceiver(OpenAdminScreenPayload.ID, (payload, context) ->
            context.client().execute(() ->
            {
                Minecraft client = context.client();
                client.setScreenAndShow(ProgressConfigScreen.createOnAdminTab(client.gui.screen()));
            }));
    }

    /**
     * Called once per client tick to advance the "ticks since last sync"
     * counter the HUD renderer extrapolates from.
     */
    public static void onClientTick()
    {
        ticksSinceLastSync++;
    }

    public static float getLastSyncedProgress()
    {
        return lastSyncedProgress;
    }

    public static float getLastSyncedRatePerTick()
    {
        return lastSyncedRatePerTick;
    }

    public static int getTicksSinceLastSync()
    {
        return ticksSinceLastSync;
    }

    public static boolean isAdminSyncReceived()
    {
        return adminSyncReceived;
    }

    public static boolean isAdminOperator()
    {
        return adminOperator;
    }

    public static boolean isAdminPermitted()
    {
        return adminPermitted;
    }

    public static float getAdminProgressRate()
    {
        return adminProgressRate;
    }

    public static boolean isAdminAllowNonOpReadOnlyView()
    {
        return adminAllowNonOpReadOnlyView;
    }

    /**
     * Clears all synced state on disconnect, so a stale value from a
     * previous server/world is never shown after joining a new one.
     */
    public static void reset()
    {
        lastSyncedProgress = 0.0f;
        lastSyncedRatePerTick = com.onthehill.xaeroworldmapbook.config.ServerProgressConfig.DEFAULT_PROGRESS_RATE;
        ticksSinceLastSync = 0;
        adminSyncReceived = false;
        adminOperator = false;
        adminPermitted = false;
    }
}
