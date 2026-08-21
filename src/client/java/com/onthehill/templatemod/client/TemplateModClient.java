package com.onthehill.templatemod.client;

import com.onthehill.templatemod.TemplateMod;
import com.onthehill.templatemod.client.config.ClientVisualizationConfig;
import com.onthehill.templatemod.client.network.ClientNetworkHandler;
import com.onthehill.templatemod.client.render.ProgressHudRenderer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

/**
 * Client entrypoint. Also wires up the example tick-progress feature's
 * client-side pieces — see {@code TemplateMod}'s Javadoc for why this
 * template keeps the example feature's wiring inline in the entrypoint
 * classes rather than a separate feature-registration class.
 */
public class TemplateModClient implements ClientModInitializer
{
    private static ClientVisualizationConfig clientConfig;

    @Override
    public void onInitializeClient()
    {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.
        clientConfig = ClientVisualizationConfig.loadOrCreate(ClientModCommands.clientConfigPath());

        ClientNetworkHandler.registerReceivers();
        ClientModCommands.registerClientCommands(() -> clientConfig);

        ClientTickEvents.END_CLIENT_TICK.register(client -> ClientNetworkHandler.onClientTick());

        HudElementRegistry.addLast(TemplateMod.id("progress_hud"), new ProgressHudRenderer(clientConfig));
    }

    /**
     * The single shared {@link ClientVisualizationConfig} instance every
     * client-side reader/writer of the visualization mode must use —
     * {@link ProgressHudRenderer} holds a direct reference to this exact
     * object, captured once at mod init. Anything that needs to change the
     * visualization mode (the config screen, in particular) must mutate
     * <em>this</em> instance rather than loading its own separate copy from
     * disk — a separate {@code ClientVisualizationConfig.loadOrCreate(...)}
     * call returns a distinct object that can be saved to disk correctly but
     * will never be seen by the already-constructed HUD renderer, which
     * keeps reading its own original instance until the next game restart.
     *
     * @return The shared client visualization config instance.
     */
    public static ClientVisualizationConfig getClientConfig()
    {
        return clientConfig;
    }
}
