package com.onthehill.xaeroworldmapbook.client;

import com.onthehill.xaeroworldmapbook.client.screen.ProgressConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Mod Menu integration — client-only class, registered via the "modmenu"
 * entrypoint in {@code fabric.mod.json}, never referenced from {@code main}
 * or {@code client}'s own init methods.
 *
 * <p>Per the studio config standard's soft-dependency rule, this class is
 * only ever loaded by Mod Menu itself calling into the "modmenu" entrypoint —
 * nothing in this mod's own code path references it — so the mod loads and
 * works correctly with Mod Menu absent even though this class is compiled
 * against Mod Menu's API ({@code modCompileOnly}, see {@code build.gradle}).
 */
public final class XaeroWorldMapBookMenuIntegration implements ModMenuApi
{
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory()
    {
        // Same construction path "/xaero-world-map-book gui" and
        // "/xaero-world-map-book-admin gui" both use — one screen-construction
        // path, multiple entry points into it (see ProgressConfigScreen's own Javadoc).
        return ProgressConfigScreen::create;
    }
}
