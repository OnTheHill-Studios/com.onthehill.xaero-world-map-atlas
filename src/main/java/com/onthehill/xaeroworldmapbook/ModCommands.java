package com.onthehill.xaeroworldmapbook;

import java.util.List;
import java.util.function.Supplier;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;

import com.onthehill.xaeroworldmapbook.config.ServerProgressConfig;
import com.onthehill.xaeroworldmapbook.network.OpenAdminScreenPayload;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Registers the server-authoritative command tree under
 * {@code /xaero-world-map-book-admin}. Every command here is op-gated per the studio
 * config standard — the {@code -admin} root name is a routing convention
 * only, it is the {@code .requires(...)} calls below that actually restrict
 * these commands to operators.
 *
 * <p>Client-authoritative commands live separately under
 * {@code client.ClientModCommands}, registered through
 * {@code ClientCommandRegistrationCallback} instead, so the two roots can
 * never collide even partially.
 */
public final class ModCommands
{
    private ModCommands() { }

    /**
     * Registers the {@code /xaero-world-map-book-admin} command tree.
     *
     * @param configSupplier Lazy accessor for the current server config —
     *     resolved fresh on every invocation rather than captured at
     *     registration time, since the config is populated later than
     *     command registration (see the Fabric mod standard's rule on
     *     command executors resolving deferred state lazily).
     * @param onValidUpdateApplied Callback invoked once a command has applied
     *     a validated change, so the caller can persist and re-broadcast.
     */
    public static void registerServerCommands(
        Supplier<ServerProgressConfig> configSupplier,
        java.util.function.BiConsumer<ServerPlayer, ServerProgressConfig> onValidUpdateApplied)
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
                    }))
                .then(Commands.literal("config")
                    .then(Commands.literal("progress-rate")
                        .then(Commands.argument("value", FloatArgumentType.floatArg())
                            .executes(context ->
                            {
                                float proposed = FloatArgumentType.getFloat(context, "value");
                                List<String> violations = ServerProgressConfig.validate(proposed);
                                if (!violations.isEmpty())
                                {
                                    context.getSource().sendFailure(
                                        Component.literal(String.join("; ", violations)));
                                    return 0;
                                }

                                ServerProgressConfig config = configSupplier.get();
                                config.apply(proposed, config.isAllowNonOpReadOnlyView());
                                ServerPlayer player = context.getSource().getPlayer();
                                if (player != null)
                                {
                                    onValidUpdateApplied.accept(player, config);
                                }
                                context.getSource().sendSuccess(
                                    () -> Component.literal("xaero-world-map-book progress rate set to " + proposed), true);
                                return 1;
                            })))
                    .then(Commands.literal("allow-read-only")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(context ->
                            {
                                boolean proposed = BoolArgumentType.getBool(context, "value");
                                ServerProgressConfig config = configSupplier.get();
                                config.apply(config.getProgressRate(), proposed);
                                ServerPlayer player = context.getSource().getPlayer();
                                if (player != null)
                                {
                                    onValidUpdateApplied.accept(player, config);
                                }
                                context.getSource().sendSuccess(
                                    () -> Component.literal("xaero-world-map-book non-op read-only view set to " + proposed), true);
                                return 1;
                            })))
                    .then(Commands.literal("reset")
                        .executes(context ->
                        {
                            ServerProgressConfig config = configSupplier.get();
                            config.resetToDefaults();
                            ServerPlayer player = context.getSource().getPlayer();
                            if (player != null)
                            {
                                onValidUpdateApplied.accept(player, config);
                            }
                            context.getSource().sendSuccess(
                                () -> Component.literal("xaero-world-map-book server config reset to defaults"), true);
                            return 1;
                        })))));
    }
}
