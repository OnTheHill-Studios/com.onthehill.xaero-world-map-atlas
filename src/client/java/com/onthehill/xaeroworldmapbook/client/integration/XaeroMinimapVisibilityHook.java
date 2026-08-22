package com.onthehill.xaeroworldmapbook.client.integration;

import xaero.common.HudMod;
import xaero.hud.minimap.common.config.option.MinimapProfiledConfigOptions;
import xaero.lib.client.config.ClientConfigManager;
import xaero.lib.common.config.profile.ConfigProfile;

/**
 * Flips Xaero's own public Minimap-visibility config option every client
 * tick per {@code client.config.MapAccessClientState}'s cached gate result —
 * ordinary use of a published setting, no Mixin needed for the Minimap gate
 * at all.
 *
 * <p>Verified by decompiling {@code xaerominimap-fabric-26.2-26.4.2.jar}
 * (the real, current release for this project's targeted Minecraft version —
 * see {@code gradle.properties}' {@code xaero_minimap_version} comment): its
 * own bundled {@code xaero.hud.minimap.controls.key.function.ToggleMapFunction}
 * (the handler behind Xaero's own built-in minimap-toggle keybind) does
 * exactly this same
 * {@code HudMod.INSTANCE.getHudConfigs().getClientConfigManager().getCurrentProfile()
 * .set(MinimapProfiledConfigOptions.DISPLAY_MINIMAP, boolean)} call — every
 * class and method referenced here (including the nested
 * {@code xaerolib} dependency's {@code ClientConfigManager}/{@code ConfigProfile})
 * confirmed public via {@code javap -p} against the real jars.
 */
public final class XaeroMinimapVisibilityHook
{
    private XaeroMinimapVisibilityHook() { }

    /**
     * Sets whether Xaero's Minimap should currently render, via Xaero's own published config option.
     *
     * @param visible The gate's current decision, per {@code progression.MapAccessEvaluator#canShowMinimap}.
     */
    public static void setVisible(boolean visible)
    {
        HudMod hudMod = HudMod.INSTANCE;
        if (hudMod == null)
        {
            // Xaero's Minimap is a soft dependency — this hook is only ever
            // invoked once its presence has already been confirmed
            // elsewhere, but guard defensively rather than NPE if that
            // invariant is ever violated.
            return;
        }

        ClientConfigManager configManager = hudMod.getHudConfigs().getClientConfigManager();
        ConfigProfile profile = configManager.getCurrentProfile();
        profile.set(MinimapProfiledConfigOptions.DISPLAY_MINIMAP, visible);
    }
}
