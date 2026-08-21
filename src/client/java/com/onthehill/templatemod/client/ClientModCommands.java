package com.onthehill.templatemod.client;

import java.nio.file.Path;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import com.onthehill.templatemod.TemplateMod;
import com.onthehill.templatemod.client.config.ClientVisualizationConfig;
import com.onthehill.templatemod.client.config.ProgressVisualizationMode;
import com.onthehill.templatemod.client.screen.ProgressConfigScreen;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Registers the client-authoritative command tree under
 * {@code /template-mod}, handled entirely locally via
 * {@code ClientCommandRegistrationCallback}. This root and
 * {@code /template-mod-admin} (see {@code ModCommands}) deliberately share
 * no literal beyond the bare mod-id prefix, and nothing under
 * {@code /template-mod-admin} is ever registered here — see the studio
 * config standard's command-root separation rule for why that split matters.
 */
public final class ClientModCommands
{
    private ClientModCommands() { }

    /**
     * Registers the {@code /template-mod} command tree.
     *
     * @param configSupplier Lazy accessor for the client config.
     */
    public static void registerClientCommands(java.util.function.Supplier<ClientVisualizationConfig> configSupplier)
    {
        SuggestionProvider<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> visualizationSuggestions =
            (context, builder) ->
            {
                for (ProgressVisualizationMode mode : ProgressVisualizationMode.values())
                {
                    builder.suggest(mode.name().toLowerCase(java.util.Locale.ROOT));
                }
                return builder.buildFuture();
            };

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommands.literal(TemplateMod.MOD_ID)
                .then(ClientCommands.literal("gui")
                    .executes(context ->
                    {
                        Minecraft client = Minecraft.getInstance();
                        client.execute(() -> client.setScreenAndShow(
                            ProgressConfigScreen.createOnClientTab(client.gui.screen())));
                        return 1;
                    }))
                .then(ClientCommands.literal("config")
                    .then(ClientCommands.literal("visualization")
                        .then(ClientCommands.argument("mode", StringArgumentType.word())
                            .suggests(visualizationSuggestions)
                            .executes(context ->
                            {
                                String raw = StringArgumentType.getString(context, "mode");
                                ProgressVisualizationMode mode;
                                try
                                {
                                    mode = ProgressVisualizationMode.valueOf(raw.toUpperCase(java.util.Locale.ROOT));
                                }
                                catch (IllegalArgumentException exception)
                                {
                                    context.getSource().sendError(
                                        Component.literal("Unknown visualization mode '" + raw + "'. Valid: bar, radial"));
                                    return 0;
                                }

                                ClientVisualizationConfig config = configSupplier.get();
                                config.setVisualizationMode(mode);
                                config.save(clientConfigPath());
                                context.getSource().sendFeedback(
                                    Component.literal("template-mod visualization set to " + raw));
                                return 1;
                            })))
                    .then(ClientCommands.literal("reset")
                        .executes(context ->
                        {
                            ClientVisualizationConfig config = configSupplier.get();
                            config.resetToDefaults();
                            config.save(clientConfigPath());
                            context.getSource().sendFeedback(
                                Component.literal("template-mod client config reset to defaults"));
                            return 1;
                        })))));
    }

    /**
     * Path to the client-side JSON config file.
     *
     * @return The resolved path under the game's config directory.
     */
    public static Path clientConfigPath()
    {
        return FabricLoader.getInstance().getConfigDir().resolve(TemplateMod.MOD_ID).resolve("client.json");
    }
}
