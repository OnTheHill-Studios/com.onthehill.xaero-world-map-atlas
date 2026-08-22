package com.onthehill.xaeroworldmapbook.client.mixin;

import com.onthehill.xaeroworldmapbook.XaeroWorldMapBook;
import com.onthehill.xaeroworldmapbook.client.XaeroWorldMapBookClient;
import com.onthehill.xaeroworldmapbook.client.config.MapAccessClientState;
import com.onthehill.xaeroworldmapbook.client.integration.XaeroWorldMapBridge;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import xaero.map.controls.ControlsHandler;
import xaero.map.controls.ControlsRegister;

/**
 * The single choke-point Mixin gating Xaero's World Map open call. Per
 * Spec 001's External Mod Integration section, decompiling Xaero's own
 * closed-source jar purely to learn the class/method shape needed to write
 * this mod's own, unrelated gating feature (never to copy or recreate
 * Xaero's own logic) is authorized by the project owner.
 *
 * <p><strong>What was verified, via {@code javap -c} disassembly of the real
 * {@code xaeroworldmap-fabric-26.2-1.45.0.jar}:</strong> Xaero's own native
 * {@code M} keybind has no separate "open the map" helper method to gate —
 * {@code ControlsHandler#keyDown(KeyMapping, boolean, boolean)} inlines the
 * entire open call directly at its own {@code HEAD}, the instant it sees
 * {@code kb == ControlsRegister.keyOpenMap}:
 * {@code Minecraft.getInstance().gui.setScreen(new GuiMap(null, null, mapProcessor, cameraEntity))}.
 * That makes {@code keyDown} itself the single real choke point every
 * open-map attempt Xaero's own key handling can produce funnels through —
 * gating here, rather than trying to unbind the key (which only stops that
 * one key, not any other future trigger of the same call), covers every
 * path Xaero's own code can take.
 *
 * <p>This mod's own two open paths ({@code item.AtlasItem}'s right-click
 * handler and this mod's own gated keybind) never call into
 * {@code ControlsHandler} at all — they call
 * {@link XaeroWorldMapBridge#openWorldMap()} directly, which sets a
 * short-lived bypass flag around its own call to the same real
 * {@code Gui#setScreen}. This Mixin's cancellation only ever applies to a
 * call this mod did not itself just make.
 */
@Mixin(ControlsHandler.class)
public abstract class XaeroWorldMapOpenGateMixin
{
    @Inject(method = "keyDown", at = @At("HEAD"), cancellable = true)
    private void xaeroWorldMapBook$gateOpenMapKey(KeyMapping keyMapping, boolean tickEnd, boolean repeat, CallbackInfo callbackInfo)
    {
        if (keyMapping != ControlsRegister.keyOpenMap)
        {
            return;
        }

        XaeroWorldMapBook.debug(
            "Xaero's native open-map keybind fired: bypassActive={}, canUseKeybind={}, denialReason={}",
            XaeroWorldMapBridge.isBypassActive(), MapAccessClientState.canUseKeybind(), MapAccessClientState.keybindDenialReason());

        if (XaeroWorldMapBridge.isBypassActive())
        {
            // This mod's own deliberate call (right-click or this mod's own
            // gated keybind, whose check already passed) — let it through
            // unconditionally, since the gate was already applied before
            // XaeroWorldMapBridge.openWorldMap() made this call.
            return;
        }

        if (!MapAccessClientState.canUseKeybind())
        {
            callbackInfo.cancel();
            // Xaero's own native M keybind was just blocked by this gate —
            // the player pressed a key and nothing visibly happened unless
            // something says why. Shares the same throttled message/cooldown
            // as this mod's own gated keybind handler.
            XaeroWorldMapBookClient.notifyDenied(Minecraft.getInstance());
        }
    }
}
