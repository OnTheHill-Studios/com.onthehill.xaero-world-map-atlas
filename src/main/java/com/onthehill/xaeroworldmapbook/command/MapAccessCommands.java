package com.onthehill.xaeroworldmapbook.command;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import com.onthehill.xaeroworldmapbook.XaeroWorldMapBook;
import com.onthehill.xaeroworldmapbook.config.KeybindItemRequirement;
import com.onthehill.xaeroworldmapbook.config.MapAccessConfig;
import com.onthehill.xaeroworldmapbook.config.MinimapAccessRequirement;
import com.onthehill.xaeroworldmapbook.config.MinimapAdvancementGate;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Registers the server-authoritative {@code /xaero-world-map-book-admin mapaccess}
 * command tree. Every command here is op-gated per the studio config
 * standard — the {@code -admin} root name is a routing convention only, it
 * is the {@code .requires(...)} calls below that actually restrict these
 * commands to operators.
 *
 * <p>This tree does <em>not</em> have its own {@code gui} subcommand — per
 * the studio config standard, a mod ships exactly one consolidated config
 * screen, opened via {@code /xaero-world-map-book-admin gui} (registered
 * separately, alongside the client-facing {@code /xaero-world-map-book gui}).
 * The mandatory "allow non-op read-only view" setting, however, lives here:
 * {@link MapAccessConfig} is this mod's only remaining server-authoritative
 * config object, so its own {@code allow-read-only} command below is that
 * one shared setting's sole command surface.
 */
public final class MapAccessCommands
{
    private MapAccessCommands() { }

    /**
     * Registers the {@code /xaero-world-map-book-admin mapaccess} subtree under the existing {@code -admin} root.
     *
     * @param configSupplier Lazy accessor for the current server config — resolved fresh on every invocation
     *     rather than captured at registration time, since the config is populated later than command registration.
     * @param onValidUpdateApplied Callback invoked once a command has applied a validated change, so the caller
     *     can persist and re-broadcast.
     */
    public static void registerServerCommands(
        Supplier<MapAccessConfig> configSupplier,
        BiConsumer<ServerPlayer, MapAccessConfig> onValidUpdateApplied)
    {
        SuggestionProvider<net.minecraft.commands.CommandSourceStack> keybindRequirementSuggestions =
            (context, builder) ->
            {
                for (KeybindItemRequirement value : KeybindItemRequirement.values())
                {
                    builder.suggest(value.name().toLowerCase(java.util.Locale.ROOT));
                }
                return builder.buildFuture();
            };

        SuggestionProvider<net.minecraft.commands.CommandSourceStack> minimapRequirementSuggestions =
            (context, builder) ->
            {
                for (MinimapAccessRequirement value : MinimapAccessRequirement.values())
                {
                    builder.suggest(value.name().toLowerCase(java.util.Locale.ROOT));
                }
                return builder.buildFuture();
            };

        SuggestionProvider<net.minecraft.commands.CommandSourceStack> minimapAdvancementGateSuggestions =
            (context, builder) ->
            {
                for (MinimapAdvancementGate value : MinimapAdvancementGate.values())
                {
                    builder.suggest(value.name().toLowerCase(java.util.Locale.ROOT));
                }
                return builder.buildFuture();
            };

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal(XaeroWorldMapBook.MOD_ID + "-admin")
                .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                .then(Commands.literal("mapaccess")
                    .then(Commands.literal("keybind-enabled")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(context -> applyKeybindOpenEnabled(context, configSupplier, onValidUpdateApplied))))
                    .then(Commands.literal("keybind-item-requirement")
                        .then(Commands.argument("value", StringArgumentType.word())
                            .suggests(keybindRequirementSuggestions)
                            .executes(context -> applyKeybindItemRequirement(context, configSupplier, onValidUpdateApplied))))
                    .then(Commands.literal("minimap-access-requirement")
                        .then(Commands.argument("value", StringArgumentType.word())
                            .suggests(minimapRequirementSuggestions)
                            .executes(context -> applyMinimapAccessRequirement(context, configSupplier, onValidUpdateApplied))))
                    .then(Commands.literal("chunks-required-for-well-traveled")
                        .then(Commands.argument("value", IntegerArgumentType.integer())
                            .executes(context -> applyChunksRequiredForWellTraveled(context, configSupplier, onValidUpdateApplied))))
                    .then(Commands.literal("minimap-advancement-gate")
                        .then(Commands.argument("value", StringArgumentType.word())
                            .suggests(minimapAdvancementGateSuggestions)
                            .executes(context -> applyMinimapAdvancementGate(context, configSupplier, onValidUpdateApplied))))
                    .then(Commands.literal("creative-bypass-enabled")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(context -> applyCreativeBypassEnabled(context, configSupplier, onValidUpdateApplied))))
                    .then(Commands.literal("allow-read-only")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(context -> applyAllowNonOpReadOnlyView(context, configSupplier, onValidUpdateApplied))))
                    .then(Commands.literal("reset")
                        .executes(context ->
                        {
                            MapAccessConfig config = configSupplier.get();
                            config.resetToDefaults();
                            notifyAndFeedback(context, config, onValidUpdateApplied, "xaero-world-map-book map access config reset to defaults");
                            return 1;
                        })))));
    }

    private static int applyKeybindOpenEnabled(
        com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> context,
        Supplier<MapAccessConfig> configSupplier,
        BiConsumer<ServerPlayer, MapAccessConfig> onValidUpdateApplied)
    {
        boolean proposed = BoolArgumentType.getBool(context, "value");
        MapAccessConfig config = configSupplier.get();
        config.apply(proposed, config.getKeybindItemRequirement(), config.getMinimapAccessRequirement(),
            config.getChunksRequiredForWellTraveled(), config.getMinimapAdvancementGate(), config.isCreativeBypassEnabled(),
            config.isAllowNonOpReadOnlyView());
        notifyAndFeedback(context, config, onValidUpdateApplied, "xaero-world-map-book keybind-enabled set to " + proposed);
        return 1;
    }

    private static int applyKeybindItemRequirement(
        com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> context,
        Supplier<MapAccessConfig> configSupplier,
        BiConsumer<ServerPlayer, MapAccessConfig> onValidUpdateApplied)
    {
        String raw = StringArgumentType.getString(context, "value");
        KeybindItemRequirement proposed;
        try
        {
            proposed = KeybindItemRequirement.valueOf(raw.toUpperCase(java.util.Locale.ROOT));
        }
        catch (IllegalArgumentException exception)
        {
            context.getSource().sendFailure(Component.literal("Unknown keybind item requirement '" + raw + "'."));
            return 0;
        }

        MapAccessConfig config = configSupplier.get();
        config.apply(config.isKeybindOpenEnabled(), proposed, config.getMinimapAccessRequirement(),
            config.getChunksRequiredForWellTraveled(), config.getMinimapAdvancementGate(), config.isCreativeBypassEnabled(),
            config.isAllowNonOpReadOnlyView());
        notifyAndFeedback(context, config, onValidUpdateApplied, "xaero-world-map-book keybind-item-requirement set to " + raw);
        return 1;
    }

    private static int applyMinimapAccessRequirement(
        com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> context,
        Supplier<MapAccessConfig> configSupplier,
        BiConsumer<ServerPlayer, MapAccessConfig> onValidUpdateApplied)
    {
        String raw = StringArgumentType.getString(context, "value");
        MinimapAccessRequirement proposed;
        try
        {
            proposed = MinimapAccessRequirement.valueOf(raw.toUpperCase(java.util.Locale.ROOT));
        }
        catch (IllegalArgumentException exception)
        {
            context.getSource().sendFailure(Component.literal("Unknown minimap access requirement '" + raw + "'."));
            return 0;
        }

        MapAccessConfig config = configSupplier.get();
        config.apply(config.isKeybindOpenEnabled(), config.getKeybindItemRequirement(), proposed,
            config.getChunksRequiredForWellTraveled(), config.getMinimapAdvancementGate(), config.isCreativeBypassEnabled(),
            config.isAllowNonOpReadOnlyView());
        notifyAndFeedback(context, config, onValidUpdateApplied, "xaero-world-map-book minimap-access-requirement set to " + raw);
        return 1;
    }

    private static int applyChunksRequiredForWellTraveled(
        com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> context,
        Supplier<MapAccessConfig> configSupplier,
        BiConsumer<ServerPlayer, MapAccessConfig> onValidUpdateApplied)
    {
        int proposed = IntegerArgumentType.getInteger(context, "value");
        List<String> violations = MapAccessConfig.validate(proposed);
        if (!violations.isEmpty())
        {
            context.getSource().sendFailure(Component.literal(String.join("; ", violations)));
            return 0;
        }

        MapAccessConfig config = configSupplier.get();
        config.apply(config.isKeybindOpenEnabled(), config.getKeybindItemRequirement(), config.getMinimapAccessRequirement(),
            proposed, config.getMinimapAdvancementGate(), config.isCreativeBypassEnabled(), config.isAllowNonOpReadOnlyView());
        notifyAndFeedback(context, config, onValidUpdateApplied, "xaero-world-map-book chunks-required-for-well-traveled set to " + proposed);
        return 1;
    }

    private static int applyMinimapAdvancementGate(
        com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> context,
        Supplier<MapAccessConfig> configSupplier,
        BiConsumer<ServerPlayer, MapAccessConfig> onValidUpdateApplied)
    {
        String raw = StringArgumentType.getString(context, "value");
        MinimapAdvancementGate proposed;
        try
        {
            proposed = MinimapAdvancementGate.valueOf(raw.toUpperCase(java.util.Locale.ROOT));
        }
        catch (IllegalArgumentException exception)
        {
            context.getSource().sendFailure(Component.literal("Unknown minimap advancement gate '" + raw + "'."));
            return 0;
        }

        MapAccessConfig config = configSupplier.get();
        config.apply(config.isKeybindOpenEnabled(), config.getKeybindItemRequirement(), config.getMinimapAccessRequirement(),
            config.getChunksRequiredForWellTraveled(), proposed, config.isCreativeBypassEnabled(), config.isAllowNonOpReadOnlyView());
        notifyAndFeedback(context, config, onValidUpdateApplied, "xaero-world-map-book minimap-advancement-gate set to " + raw);
        return 1;
    }

    private static int applyCreativeBypassEnabled(
        com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> context,
        Supplier<MapAccessConfig> configSupplier,
        BiConsumer<ServerPlayer, MapAccessConfig> onValidUpdateApplied)
    {
        boolean proposed = BoolArgumentType.getBool(context, "value");
        MapAccessConfig config = configSupplier.get();
        config.apply(config.isKeybindOpenEnabled(), config.getKeybindItemRequirement(), config.getMinimapAccessRequirement(),
            config.getChunksRequiredForWellTraveled(), config.getMinimapAdvancementGate(), proposed, config.isAllowNonOpReadOnlyView());
        notifyAndFeedback(context, config, onValidUpdateApplied, "xaero-world-map-book creative-bypass-enabled set to " + proposed);
        return 1;
    }

    private static int applyAllowNonOpReadOnlyView(
        com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> context,
        Supplier<MapAccessConfig> configSupplier,
        BiConsumer<ServerPlayer, MapAccessConfig> onValidUpdateApplied)
    {
        boolean proposed = BoolArgumentType.getBool(context, "value");
        MapAccessConfig config = configSupplier.get();
        config.apply(config.isKeybindOpenEnabled(), config.getKeybindItemRequirement(), config.getMinimapAccessRequirement(),
            config.getChunksRequiredForWellTraveled(), config.getMinimapAdvancementGate(), config.isCreativeBypassEnabled(), proposed);
        notifyAndFeedback(context, config, onValidUpdateApplied, "xaero-world-map-book allow-read-only set to " + proposed);
        return 1;
    }

    private static void notifyAndFeedback(
        com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> context,
        MapAccessConfig config,
        BiConsumer<ServerPlayer, MapAccessConfig> onValidUpdateApplied,
        String feedbackMessage)
    {
        ServerPlayer player = context.getSource().getPlayer();
        if (player != null)
        {
            onValidUpdateApplied.accept(player, config);
        }
        context.getSource().sendSuccess(() -> Component.literal(feedbackMessage), true);
    }
}
