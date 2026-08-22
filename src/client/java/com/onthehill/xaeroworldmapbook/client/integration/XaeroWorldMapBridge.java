package com.onthehill.xaeroworldmapbook.client.integration;

import net.minecraft.client.Minecraft;

import xaero.map.WorldMapSession;
import xaero.map.gui.GuiMap;

/**
 * The one place this mod calls into Xaero's World Map's real open-screen
 * entrypoint, verified by decompiling {@code xaeroworldmap-fabric-26.2-1.45.0.jar}
 * (the real, current release for this project's targeted Minecraft version —
 * see {@code gradle.properties}' {@code xaero_world_map_version} comment):
 *
 * <ul>
 *   <li>{@code xaero.map.WorldMapSession.getCurrentSession()} — public static accessor for the live session.</li>
 *   <li>{@code WorldMapSession#getMapProcessor()} / {@code #isUsable()} — public instance accessors.</li>
 *   <li>{@code new xaero.map.gui.GuiMap(Screen, Screen, MapProcessor, Entity)} — public constructor,
 *       confirmed via {@code javap -p} against the real jar.</li>
 *   <li>{@code Minecraft#gui}'s {@code net.minecraft.client.gui.Gui#setScreen(Screen)} — the exact method
 *       Xaero's own compiled bytecode calls to open the screen (confirmed via {@code javap -c} disassembly of
 *       {@code xaero.map.controls.ControlsHandler#keyDown}), used here identically rather than guessed from
 *       Minecraft's own more commonly-seen {@code setScreenAndShow}.</li>
 * </ul>
 *
 * <p>Also holds the short-lived "this open call is ours, let it through"
 * bypass flag {@code XaeroWorldMapOpenGateMixin} reads, so it can distinguish
 * this mod's own deliberate calls from Xaero's own native {@code M} keybind
 * still trying to call the same real open-screen path (see that Mixin's own
 * Javadoc for the full choke-point rationale).
 */
public final class XaeroWorldMapBridge
{
    private static boolean bypassActive = false;

    private XaeroWorldMapBridge() { }

    /**
     * Opens Xaero's World Map screen unconditionally, via Xaero's own real construction path. Callers are
     * responsible for having already applied whatever gate check is appropriate for their own entry point (this
     * mod's right-click item-use path applies none at all, since holding the item you just right-clicked already
     * satisfies every possible holding-location requirement; this mod's own gated keybind applies
     * {@code progression.MapAccessEvaluator#canUseKeybind} first).
     */
    public static void openWorldMap()
    {
        WorldMapSession session = WorldMapSession.getCurrentSession();
        if (session == null || !session.isUsable())
        {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        bypassActive = true;
        try
        {
            GuiMap guiMap = new GuiMap(null, null, session.getMapProcessor(), minecraft.getCameraEntity());
            minecraft.gui.setScreen(guiMap);
        }
        finally
        {
            bypassActive = false;
        }
    }

    /**
     * Whether the current call stack is inside this bridge's own {@link #openWorldMap()} — read by
     * {@code client.mixin.XaeroWorldMapOpenGateMixin} to let this mod's own deliberate opens through
     * unconditionally, since the gate check for those paths already ran before this bridge was called.
     *
     * @return {@code true} if {@link #openWorldMap()} is currently on the call stack.
     */
    public static boolean isBypassActive()
    {
        return bypassActive;
    }
}
