package com.onthehill.templatemod.network;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.onthehill.templatemod.config.ServerProgressConfig;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Central coordination point for this mod's networking. Payload <em>type</em>
 * registration is unconditional and runs on every physical install per the
 * Fabric networking standard; server-side <em>receiver</em> registration is
 * deferred until a real server exists, since it depends on
 * {@code ServerProgressConfig} having been loaded first.
 *
 * <p>Client-side receiver registration lives in
 * {@code client.network.ClientNetworkHandler} instead of here, because
 * {@code ClientPlayNetworking} is a client-only class and this package is
 * reachable from the common {@code main} entrypoint.
 */
public final class ModNetworking
{
    private ModNetworking() { }

    /**
     * Registers every payload type this mod defines, in both directions.
     * Must be called unconditionally from {@code TemplateMod.onInitialize()} —
     * never deferred — so that a pure client's own receiver registration
     * always has a declared type to attach to.
     */
    public static void registerPayloadTypes()
    {
        PayloadTypeRegistry.clientboundPlay().register(ProgressSyncPayload.ID, ProgressSyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AdminConfigSyncPayload.ID, AdminConfigSyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OpenAdminScreenPayload.ID, OpenAdminScreenPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(AdminConfigUpdatePayload.ID, AdminConfigUpdatePayload.CODEC);
    }

    /**
     * Registers the server-side receiver for admin config updates sent from
     * the admin GUI tab. Called once real server state exists (see
     * {@code TemplateMod}'s {@code ServerLifecycleEvents.SERVER_STARTED} handler).
     *
     * @param configSupplier Lazy accessor for the current server config — a
     *     {@code Supplier} rather than a captured value, since the config is
     *     populated at the same deferred point this receiver is registered from.
     * @param onValidUpdateApplied Callback invoked with the requesting player
     *     and the newly-applied values once an update has passed validation
     *     and permission checks, so the caller can persist and re-broadcast.
     */
    public static void registerServerReceivers(
        Supplier<ServerProgressConfig> configSupplier,
        BiConsumer<ServerPlayer, ServerProgressConfig> onValidUpdateApplied)
    {
        ServerPlayNetworking.registerGlobalReceiver(AdminConfigUpdatePayload.ID, (payload, context) ->
        {
            ServerPlayer player = context.player();

            // Re-validate permission server-side — the admin tab being
            // disabled/greyed-out client-side for a non-op viewer is a UI
            // convenience only, never the actual security boundary.
            if (!Commands.LEVEL_GAMEMASTERS.check(player.permissions()))
            {
                return;
            }

            var violations = ServerProgressConfig.validate(payload.progressRate());
            if (!violations.isEmpty())
            {
                player.sendSystemMessage(
                    Component.literal("Rejected config update: " + String.join("; ", violations))
                );
                return;
            }

            ServerProgressConfig config = configSupplier.get();
            config.apply(payload.progressRate(), payload.allowNonOpReadOnlyView());
            onValidUpdateApplied.accept(player, config);
        });
    }

    /**
     * Builds the admin sync payload a specific player should receive, honoring
     * the mandatory non-op read-only-view setting: a non-op player only gets
     * real values when that setting is {@code true}, and gets zeroed
     * placeholders — never the real values — otherwise.
     *
     * @param config Current server config.
     * @param viewerIsOperator Whether the intended recipient currently has operator permission.
     * @return The payload to send to that specific recipient.
     */
    public static AdminConfigSyncPayload buildAdminSyncPayloadFor(ServerProgressConfig config, boolean viewerIsOperator)
    {
        boolean permitted = viewerIsOperator || config.isAllowNonOpReadOnlyView();
        if (!permitted)
        {
            return new AdminConfigSyncPayload(false, false, 0f, false);
        }
        return new AdminConfigSyncPayload(viewerIsOperator, true, config.getProgressRate(), config.isAllowNonOpReadOnlyView());
    }
}
