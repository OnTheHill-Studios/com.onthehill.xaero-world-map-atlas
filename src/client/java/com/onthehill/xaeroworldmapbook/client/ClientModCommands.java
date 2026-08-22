package com.onthehill.xaeroworldmapbook.client;

import com.onthehill.xaeroworldmapbook.XaeroWorldMapBook;
import com.onthehill.xaeroworldmapbook.client.screen.ProgressConfigScreen;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.client.Minecraft;

/**
 * Registers the client-facing {@code /xaero-world-map-book gui} command,
 * handled entirely locally via {@code ClientCommandRegistrationCallback}.
 * This root and {@code /xaero-world-map-book-admin} (see {@code ModCommands})
 * deliberately share no literal beyond the bare mod-id prefix — see the
 * studio config standard's command-root separation rule for why that split
 * matters.
 *
 * <p>This mod has no remaining client-authoritative config fields (the
 * earlier example tick-progress feature's visualization mode/color setting
 * was removed as leftover template scaffolding), so this command exists
 * purely to give every player — not just operators — a way to open the
 * one consolidated config screen and see whatever it is permitted to show
 * them.
 */
public final class ClientModCommands
{
    private ClientModCommands() { }

    /**
     * Registers the {@code /xaero-world-map-book gui} command.
     */
    public static void registerClientCommands()
    {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommands.literal(XaeroWorldMapBook.MOD_ID)
                .then(ClientCommands.literal("gui")
                    .executes(context ->
                    {
                        Minecraft client = Minecraft.getInstance();
                        client.execute(() -> client.setScreenAndShow(
                            ProgressConfigScreen.create(client.gui.screen())));
                        return 1;
                    }))));
    }
}
