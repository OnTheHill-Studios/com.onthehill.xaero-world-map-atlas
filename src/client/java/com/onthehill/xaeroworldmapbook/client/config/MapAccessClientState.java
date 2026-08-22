package com.onthehill.xaeroworldmapbook.client.config;

import com.onthehill.xaeroworldmapbook.XaeroWorldMapBook;
import com.onthehill.xaeroworldmapbook.client.network.MapAccessClientNetworkHandler;
import com.onthehill.xaeroworldmapbook.config.KeybindItemRequirement;
import com.onthehill.xaeroworldmapbook.config.MapAccessConfig;
import com.onthehill.xaeroworldmapbook.config.MinimapAccessRequirement;
import com.onthehill.xaeroworldmapbook.config.MinimapAdvancementGate;
import com.onthehill.xaeroworldmapbook.item.ModItems;
import com.onthehill.xaeroworldmapbook.progression.KeybindDenialReason;
import com.onthehill.xaeroworldmapbook.progression.MapAccessEvaluator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Per-client-tick cache of "can open the World Map right now" / "can show
 * the Minimap right now," recomputed once per
 * {@code ClientTickEvents.END_CLIENT_TICK} rather than once per render call
 * or Mixin invocation — every consumer (the gated keybind handler,
 * {@code client.mixin.XaeroWorldMapOpenGateMixin}, and
 * {@code client.integration.XaeroMinimapVisibilityHook}) only ever reads
 * these two cached booleans, never recomputing the underlying inventory/
 * hand/advancement checks itself.
 *
 * <p>Advancement status ({@code craft_atlas}/{@code well_traveled}) is read
 * from {@link MapAccessClientNetworkHandler}, not queried directly here —
 * see {@code network.MapAccessAdvancementStatusPayload}'s Javadoc for why:
 * this Minecraft version's client-side advancement tracking exposes no
 * public accessor for a specific advancement's progress outside its own
 * listener-driven advancement-screen UI.
 */
public final class MapAccessClientState
{
    /**
     * Ticks a blocked keybind attempt's action-bar denial message stays throttled for — 3 real-time seconds, long
     * enough that holding/mashing the key doesn't spam the overlay, short enough that a player who changes what
     * they're holding gets fresh feedback quickly.
     */
    public static final int DENIAL_MESSAGE_COOLDOWN_TICKS = 60;

    private static boolean canUseKeybind = false;
    private static boolean canShowMinimap = false;
    private static KeybindDenialReason keybindDenialReason = KeybindDenialReason.NONE;
    private static KeybindDenialReason lastLoggedDenialReason = null;

    private MapAccessClientState() { }

    /**
     * Recomputes both cached booleans from current client state. Call once per {@code END_CLIENT_TICK}.
     */
    public static void tick()
    {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null)
        {
            canUseKeybind = false;
            canShowMinimap = false;
            return;
        }

        MapAccessConfig lastSynced = MapAccessClientNetworkHandler.getLastSyncedConfigOrDefaults();
        boolean craftAtlasEarned = MapAccessClientNetworkHandler.isCraftAtlasEarned();

        boolean creativeBypassActive = lastSynced.isCreativeBypassEnabled() && player.isCreative();

        boolean advancementOnlyGateSatisfied = lastSynced.getMinimapAdvancementGate() == MinimapAdvancementGate.ADVENTURING_TIME
            ? MapAccessClientNetworkHandler.isAdventuringTimeEarned()
            : MapAccessClientNetworkHandler.isWellTraveledEarned();

        Inventory inventory = player.getInventory();
        boolean atlasInHotbar = isAtlasInHotbar(inventory);
        boolean atlasInMainInventory = isAtlasInMainInventoryStorage(inventory);
        boolean atlasInMainHand = player.getMainHandItem().is(ModItems.ATLAS);
        boolean atlasInOffhand = player.getItemBySlot(EquipmentSlot.OFFHAND).is(ModItems.ATLAS);

        KeybindItemRequirement keybindItemRequirement = lastSynced.getKeybindItemRequirement();
        MinimapAccessRequirement minimapAccessRequirement = lastSynced.getMinimapAccessRequirement();

        keybindDenialReason = MapAccessEvaluator.describeKeybindDenialReason(
            lastSynced.isKeybindOpenEnabled(), creativeBypassActive, keybindItemRequirement,
            atlasInHotbar, atlasInMainInventory, craftAtlasEarned);
        canUseKeybind = keybindDenialReason == KeybindDenialReason.NONE;

        if (keybindDenialReason != lastLoggedDenialReason)
        {
            lastLoggedDenialReason = keybindDenialReason;
            XaeroWorldMapBook.debug(
                "keybindDenialReason changed to {} (keybindOpenEnabled={}, creativeBypassActive={}, "
                    + "keybindItemRequirement={}, atlasInHotbar={}, atlasInMainInventory={}, craftAtlasEarned={})",
                keybindDenialReason, lastSynced.isKeybindOpenEnabled(), creativeBypassActive, keybindItemRequirement,
                atlasInHotbar, atlasInMainInventory, craftAtlasEarned);
        }

        canShowMinimap = MapAccessEvaluator.canShowMinimap(
            creativeBypassActive, minimapAccessRequirement, craftAtlasEarned,
            atlasInMainHand, atlasInOffhand, atlasInHotbar, atlasInMainInventory, advancementOnlyGateSatisfied);
    }

    /**
     * Whether the gated keybind is currently allowed to open Xaero's World Map, per the last full tick's computed
     * result.
     *
     * @return The cached keybind-gate decision.
     */
    public static boolean canUseKeybind()
    {
        return canUseKeybind;
    }

    /**
     * Specifically why the gated keybind is currently blocked (or {@link KeybindDenialReason#NONE} if it isn't),
     * per the last full tick's computed result — read by the denial-message code so a blocked attempt names the
     * actual missing requirement instead of a generic message.
     *
     * @return The cached denial reason.
     */
    public static KeybindDenialReason keybindDenialReason()
    {
        return keybindDenialReason;
    }

    /**
     * Whether Xaero's Minimap should currently be visible, per the last full tick's computed result.
     *
     * @return The cached Minimap-gate decision.
     */
    public static boolean canShowMinimap()
    {
        return canShowMinimap;
    }

    private static boolean isAtlasInHotbar(Inventory inventory)
    {
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++)
        {
            if (inventory.getItem(slot).is(ModItems.ATLAS))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean isAtlasInMainInventoryStorage(Inventory inventory)
    {
        int hotbarSize = Inventory.getSelectionSize();
        for (int slot = hotbarSize; slot < inventory.getContainerSize(); slot++)
        {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(ModItems.ATLAS))
            {
                return true;
            }
        }
        return false;
    }
}
