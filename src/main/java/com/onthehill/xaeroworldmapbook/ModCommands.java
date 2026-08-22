package com.onthehill.xaeroworldmapbook;

import com.onthehill.xaeroworldmapbook.network.OpenAdminScreenPayload;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Registers the {@code /xaero-world-map-book-admin gui} entry point — the op-gated command that opens this mod's
 * one consolidated config screen positioned on the admin tab. Every other server-authoritative command lives under
 * its own feature-specific tree instead (see {@code command.MapAccessCommands}), but the {@code gui} literal itself
 * stays here as shared infrastructure any future server-authoritative feature's commands can sit alongside without
 * re-registering their own copy of it.
 */
public final class ModCommands
{
    private ModCommands() { }

    /**
     * Registers the {@code /xaero-world-map-book-admin gui} command.
     */
    public static void registerServerCommands()
    {
        // This MC version replaced int-based op levels with a PermissionCheck
        // system; LEVEL_GAMEMASTERS is the direct equivalent of legacy op level 2.
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal(XaeroWorldMapBook.MOD_ID + "-admin")
                .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                .then(Commands.literal("gui")
                    .executes(context ->
                    {
                        ServerPlayer player = context.getSource().getPlayer();
                        if (player == null)
                        {
                            context.getSource().sendFailure(Component.literal(
                                "This command opens a client screen and must be run by a player, not the console/RCON."));
                            return 0;
                        }
                        ServerPlayNetworking.send(player, new OpenAdminScreenPayload());
                        return 1;
                    }))));
    }
}
