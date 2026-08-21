package com.onthehill.templatemod.client.config;

/**
 * Client-authoritative choice of how the example progress value is rendered
 * on screen. This is purely a local rendering preference — it has no bearing
 * on world/gameplay state other players see, so it lives only in the client
 * config file and is never synced to or validated by the server.
 */
public enum ProgressVisualizationMode
{
    /**
     * A horizontal bar that fills left-to-right and empties instantly on reset.
     */
    BAR,

    /**
     * A radial pie/wedge that sweeps clockwise like a loading icon and empties on reset.
     */
    RADIAL
}
