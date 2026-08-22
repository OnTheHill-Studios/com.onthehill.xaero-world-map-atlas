package com.onthehill.xaeroworldmapbook.client;

import com.onthehill.xaeroworldmapbook.XaeroWorldMapBook;
import com.onthehill.xaeroworldmapbook.client.config.MapAccessClientState;
import com.onthehill.xaeroworldmapbook.client.integration.XaeroMinimapVisibilityHook;
import com.onthehill.xaeroworldmapbook.client.integration.XaeroWorldMapBridge;
import com.onthehill.xaeroworldmapbook.client.network.MapAccessClientNetworkHandler;
import com.onthehill.xaeroworldmapbook.item.ModItems;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.lwjgl.glfw.GLFW;

/**
 * Client entrypoint. Wires up Spec 001's map-access gating: the gated
 * keybind, the Atlas's right-click open behavior, map-access networking
 * receivers, and the per-tick refresh of {@link MapAccessClientState} (and,
 * when Xaero's Minimap is present, the live push of its cached
 * Minimap-visibility decision into Xaero's own config option via
 * {@code XaeroMinimapVisibilityHook}).
 */
public class XaeroWorldMapBookClient implements ClientModInitializer
{
    private static final String XAERO_MINIMAP_MOD_ID = "xaerominimap";

    private static KeyMapping openWorldMapKey;
    private static int ticksSinceLastDenialMessage = Integer.MAX_VALUE;

    @Override
    public void onInitializeClient()
    {
        MapAccessClientNetworkHandler.registerReceivers();
        ClientModCommands.registerClientCommands();

        openWorldMapKey = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                "key.xaero-world-map-book.open_world_map",
                GLFW.GLFW_KEY_M,
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath(XaeroWorldMapBook.MOD_ID, "main"))));

        UseItemCallback.EVENT.register(this::onUseItem);

        ClientTickEvents.END_CLIENT_TICK.register(client ->
        {
            MapAccessClientState.tick();
            pushMinimapVisibility();
            handleOpenWorldMapKeyPress(client);
        });
    }

    private InteractionResult onUseItem(Player player, Level level, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() || !stack.is(ModItems.ATLAS))
        {
            return InteractionResult.PASS;
        }

        // Right-clicking an item you are, by definition, holding already
        // satisfies every possible holding-location requirement this spec
        // defines, so no further gate check is needed on this path.
        XaeroWorldMapBridge.openWorldMap();
        return InteractionResult.SUCCESS;
    }

    private void handleOpenWorldMapKeyPress(Minecraft client)
    {
        // Only increment while still below the cooldown cap — NOT a plain
        // increment, and NOT `Math.min(ticksSinceLastDenialMessage + 1, cap)`
        // either (an earlier version of this fix used exactly that and was
        // still broken): starting from a large initial value, the `+ 1`
        // addition itself overflows in plain int arithmetic *before*
        // Math.min ever runs, so Math.min ends up comparing an
        // already-wrapped huge-negative number against the cap and always
        // picks the negative one. A negative value compares as permanently
        // "< cooldown" forever after, silencing every future denial message
        // for the rest of the session — this was confirmed live via the
        // debug log after the first fix attempt. Stopping the increment
        // once it reaches the cap (rather than trying to clamp the result of
        // an addition that may have already overflowed) can never overflow,
        // no matter how long the session runs.
        if (ticksSinceLastDenialMessage < MapAccessClientState.DENIAL_MESSAGE_COOLDOWN_TICKS)
        {
            ticksSinceLastDenialMessage++;
        }

        while (openWorldMapKey.consumeClick())
        {
            debug(
                "Gated keybind press consumed: canUseKeybind={}, denialReason={}",
                MapAccessClientState.canUseKeybind(), MapAccessClientState.keybindDenialReason());

            if (MapAccessClientState.canUseKeybind())
            {
                XaeroWorldMapBridge.openWorldMap();
                continue;
            }

            notifyDenied(client);
        }
    }

    /**
     * Shows the throttled, reason-specific denial message for whatever
     * {@link MapAccessClientState#keybindDenialReason()} currently reports. Called both from this class's own
     * gated keybind handler above (when this mod's own {@code M} press is blocked) and from
     * {@code client.mixin.XaeroWorldMapOpenGateMixin} (when Xaero's own native {@code M} keybind is blocked
     * instead) — a blocked attempt should never be silent, and should always name the actual missing requirement,
     * regardless of which of the two paths actually caught it.
     * <p>
     * Posted via {@link net.minecraft.world.entity.player.Player#sendSystemMessage} rather than
     * {@code Hud#setOverlayMessage} — per the project owner's explicit request that this read at the same size as
     * ordinary chat text. {@code LocalPlayer}'s override of {@code sendSystemMessage} adds straight to this
     * client's own chat log without a server round-trip (it's the same client-local path vanilla's own client
     * commands use for their feedback), which renders through the chat HUD's normal font size — the action-bar
     * overlay {@code setOverlayMessage} draws through renders noticeably larger, which is what prompted this
     * change.
     *
     * @param client The current client instance.
     */
    public static void notifyDenied(Minecraft client)
    {
        if (ticksSinceLastDenialMessage < MapAccessClientState.DENIAL_MESSAGE_COOLDOWN_TICKS)
        {
            debug(
                "notifyDenied called but throttled: ticksSinceLastDenialMessage={} < cooldown={}",
                ticksSinceLastDenialMessage, MapAccessClientState.DENIAL_MESSAGE_COOLDOWN_TICKS);
            return;
        }

        String langKey = switch (MapAccessClientState.keybindDenialReason())
        {
            case KEYBIND_DISABLED -> "gui.xaero-world-map-book.map_access_denied.keybind_disabled";
            case NEEDS_ITEM_IN_HOTBAR -> "gui.xaero-world-map-book.map_access_denied.needs_item_in_hotbar";
            case NEEDS_ITEM_IN_INVENTORY -> "gui.xaero-world-map-book.map_access_denied.needs_item_in_inventory";
            case NEEDS_ADVANCEMENT -> "gui.xaero-world-map-book.map_access_denied.needs_advancement";
            case NONE -> null;
        };

        if (langKey == null)
        {
            // Shouldn't happen — this method is only ever called from a path
            // that already confirmed the press is blocked — but fail closed
            // (no message) rather than showing a nonsensical one.
            XaeroWorldMapBook.LOGGER.warn(
                "notifyDenied called with denialReason=NONE — no message shown. This indicates a caller invoked "
                    + "notifyDenied without first confirming the press was actually blocked.");
            return;
        }

        if (client.player == null)
        {
            return;
        }

        debug("Showing map-access denial chat message: langKey={}", langKey);
        ticksSinceLastDenialMessage = 0;
        client.player.sendSystemMessage(Component.translatable(langKey));
    }

    private static void debug(String format, Object... args)
    {
        XaeroWorldMapBook.debug(format, args);
    }

    private void pushMinimapVisibility()
    {
        if (FabricLoader.getInstance().isModLoaded(XAERO_MINIMAP_MOD_ID))
        {
            XaeroMinimapVisibilityHook.setVisible(MapAccessClientState.canShowMinimap());
        }
    }
}
