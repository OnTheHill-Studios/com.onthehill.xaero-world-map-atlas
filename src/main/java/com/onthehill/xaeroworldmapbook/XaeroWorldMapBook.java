package com.onthehill.xaeroworldmapbook;

import java.nio.file.Path;

import com.onthehill.xaeroworldmapbook.config.ServerProgressConfig;
import com.onthehill.xaeroworldmapbook.network.ModNetworking;
import com.onthehill.xaeroworldmapbook.network.ProgressSyncPayload;
import com.onthehill.xaeroworldmapbook.progress.TickProgressState;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.Commands;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common (main) entrypoint. Also hosts the example tick-progress feature's
 * server-side state and lifecycle wiring, kept here rather than in its own
 * class to keep this template's Spec 000 example easy to read end-to-end —
 * a real feature-sized mod should factor per-feature state out of its main
 * entrypoint class as it grows past template scope.
 *
 * <p>Nothing in {@link #onInitialize()} does genuinely server-only work
 * eagerly — config loading and the tick hook are deferred into
 * {@code ServerLifecycleEvents.SERVER_STARTED}, per the Fabric mod standard,
 * so a pure client that never hosts anything never touches the server config
 * file or runs a live tick handler for it.
 */
public class XaeroWorldMapBook implements ModInitializer
{
    public static final String MOD_ID = "xaero-world-map-book";

    /**
     * How many server ticks pass between progress resync broadcasts to all
     * connected clients. Clients extrapolate smoothly between broadcasts via
     * {@code ProgressMath.extrapolate} rather than requiring a packet every tick.
     */
    private static final int PROGRESS_BROADCAST_INTERVAL_TICKS = 20;

    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static ServerProgressConfig serverConfig;
    private static float currentProgress = 0.0f;
    private static int ticksSinceLastBroadcast = 0;

    @Override
    public void onInitialize()
    {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.
        LOGGER.info("Hello Fabric world!");

        // Type registration is unconditional and safe on every physical
        // install — see ModNetworking's own Javadoc for why this must not
        // be bundled with the deferred receiver registration below.
        ModNetworking.registerPayloadTypes();

        // Registering the event listener itself is safe pre-server-start;
        // the executors inside resolve `serverConfig` lazily via a Supplier,
        // since it is not populated until SERVER_STARTED fires below.
        ModCommands.registerServerCommands(() -> serverConfig, XaeroWorldMapBook::onAdminConfigApplied);

        ServerLifecycleEvents.SERVER_STARTED.register(XaeroWorldMapBook::onServerStarted);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
            sendFullSyncTo(handler.getPlayer()));
    }

    private static void onServerStarted(MinecraftServer server)
    {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID).resolve("server.json");
        serverConfig = ServerProgressConfig.loadOrCreate(configPath);
        currentProgress = 0.0f;
        ticksSinceLastBroadcast = 0;

        ModNetworking.registerServerReceivers(() -> serverConfig, XaeroWorldMapBook::onAdminConfigApplied);
        ServerTickEvents.END_SERVER_TICK.register(XaeroWorldMapBook::onServerTick);
    }

    private static void onServerTick(MinecraftServer server)
    {
        currentProgress = TickProgressState.advance(currentProgress, serverConfig.getProgressRate());

        ticksSinceLastBroadcast++;
        if (ticksSinceLastBroadcast >= PROGRESS_BROADCAST_INTERVAL_TICKS)
        {
            ticksSinceLastBroadcast = 0;
            ProgressSyncPayload payload = new ProgressSyncPayload(currentProgress, serverConfig.getProgressRate());
            for (ServerPlayer player : server.getPlayerList().getPlayers())
            {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    private static void onAdminConfigApplied(ServerPlayer requester, ServerProgressConfig config)
    {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID).resolve("server.json");
        config.save(configPath);

        MinecraftServer server = requester.level().getServer();
        if (server == null)
        {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            ServerPlayNetworking.send(player, ModNetworking.buildAdminSyncPayloadFor(
                config, Commands.LEVEL_GAMEMASTERS.check(player.permissions())));
        }
    }

    private static void sendFullSyncTo(ServerPlayer player)
    {
        if (serverConfig == null)
        {
            return;
        }
        ServerPlayNetworking.send(player, new ProgressSyncPayload(currentProgress, serverConfig.getProgressRate()));
        ServerPlayNetworking.send(player, ModNetworking.buildAdminSyncPayloadFor(
            serverConfig, Commands.LEVEL_GAMEMASTERS.check(player.permissions())));
    }

    public static Identifier id(String path)
    {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
