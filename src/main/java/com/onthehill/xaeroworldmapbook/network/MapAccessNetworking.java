package com.onthehill.xaeroworldmapbook.network;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.onthehill.xaeroworldmapbook.config.MapAccessConfig;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Central coordination point for Spec 001's map-access networking. Payload
 * <em>type</em> registration is unconditional and runs on every physical
 * install per the Fabric networking standard; server-side <em>receiver</em>
 * registration is deferred until a real server exists, since it depends on
 * {@code MapAccessConfig} having been loaded first.
 *
 * <p>Opening the consolidated admin screen on the map-access-relevant tab
 * reuses {@code OpenAdminScreenPayload} (the existing Spec 000 payload) —
 * there is only ever one consolidated admin screen for the whole mod, so
 * this feature does not define its own separate open-screen payload.
 */
public final class MapAccessNetworking
{
    private MapAccessNetworking() { }

    /**
     * Registers every map-access payload type, in both directions. Must be
     * called unconditionally from {@code XaeroWorldMapBook.onInitialize()} —
     * never deferred — so that a pure client's own receiver registration
     * always has a declared type to attach to.
     */
    public static void registerPayloadTypes()
    {
        PayloadTypeRegistry.clientboundPlay().register(MapAccessConfigSyncPayload.ID, MapAccessConfigSyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MapAccessAdvancementStatusPayload.ID, MapAccessAdvancementStatusPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OpenAdminScreenPayload.ID, OpenAdminScreenPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(MapAccessConfigUpdatePayload.ID, MapAccessConfigUpdatePayload.CODEC);
    }

    /**
     * Registers the server-side receiver for admin config updates sent from
     * the admin GUI tab. Called once real server state exists.
     *
     * @param configSupplier Lazy accessor for the current server config.
     * @param onValidUpdateApplied Callback invoked with the requesting player and the newly-applied config once an
     *     update has passed validation and permission checks, so the caller can persist and re-broadcast.
     */
    public static void registerServerReceivers(
        Supplier<MapAccessConfig> configSupplier,
        BiConsumer<ServerPlayer, MapAccessConfig> onValidUpdateApplied)
    {
        ServerPlayNetworking.registerGlobalReceiver(MapAccessConfigUpdatePayload.ID, (payload, context) ->
        {
            ServerPlayer player = context.player();

            // Re-validate permission server-side — the admin tab being
            // disabled/greyed-out client-side for a non-op viewer is a UI
            // convenience only, never the actual security boundary.
            if (!Commands.LEVEL_GAMEMASTERS.check(player.permissions()))
            {
                return;
            }

            var violations = MapAccessConfig.validate(payload.chunksRequiredForWellTraveled());
            if (!violations.isEmpty())
            {
                player.sendSystemMessage(Component.literal("Rejected map access config update: " + String.join("; ", violations)));
                return;
            }

            MapAccessConfig config = configSupplier.get();
            config.apply(
                payload.keybindOpenEnabled(),
                payload.keybindItemRequirement(),
                payload.minimapAccessRequirement(),
                payload.chunksRequiredForWellTraveled(),
                payload.minimapAdvancementGate(),
                payload.creativeBypassEnabled(),
                payload.allowNonOpReadOnlyView());
            onValidUpdateApplied.accept(player, config);
        });
    }

    /**
     * Builds the map-access sync payload a specific player should receive, honoring the mandatory non-op
     * read-only-view setting: a non-op player only gets real values when that setting is {@code true}, and gets
     * placeholder defaults — never the real values — otherwise.
     *
     * @param config Current server config.
     * @param viewerIsOperator Whether the intended recipient currently has operator permission.
     * @return The payload to send to that specific recipient.
     */
    public static MapAccessConfigSyncPayload buildSyncPayloadFor(MapAccessConfig config, boolean viewerIsOperator)
    {
        boolean permitted = viewerIsOperator || config.isAllowNonOpReadOnlyView();
        if (!permitted)
        {
            return new MapAccessConfigSyncPayload(
                false, false, false, MapAccessConfig.DEFAULT_KEYBIND_ITEM_REQUIREMENT,
                MapAccessConfig.DEFAULT_MINIMAP_ACCESS_REQUIREMENT, 0,
                MapAccessConfig.DEFAULT_MINIMAP_ADVANCEMENT_GATE, false, false);
        }
        return new MapAccessConfigSyncPayload(
            viewerIsOperator, true,
            config.isKeybindOpenEnabled(), config.getKeybindItemRequirement(), config.getMinimapAccessRequirement(),
            config.getChunksRequiredForWellTraveled(), config.getMinimapAdvancementGate(), config.isCreativeBypassEnabled(),
            config.isAllowNonOpReadOnlyView());
    }
}
