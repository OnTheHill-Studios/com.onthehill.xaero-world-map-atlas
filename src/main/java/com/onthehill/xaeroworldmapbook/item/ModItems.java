package com.onthehill.xaeroworldmapbook.item;

import java.util.function.Supplier;

import com.onthehill.xaeroworldmapbook.XaeroWorldMapBook;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

/**
 * Centralized item registration for this mod, per {@code fabric-mod-standards.md}'s
 * {@code Mod<Category>} registry-holder convention.
 */
public final class ModItems
{
    private ModItems() { }

    /**
     * The Atlas — gates access to Xaero's World Map and, once held or
     * sufficiently earned, Xaero's Minimap. See Spec 001 for the full gating
     * rules.
     */
    public static final Item ATLAS = register("atlas", Item.Properties::new);

    private static Item register(String path, Supplier<Item.Properties> propertiesFactory)
    {
        ResourceKey<Item> key = keyOf(path);
        Item item = new Item(propertiesFactory.get().setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    private static ResourceKey<Item> keyOf(String path)
    {
        return ResourceKey.create(Registries.ITEM, XaeroWorldMapBook.id(path));
    }

    /**
     * The vanilla Tools and Utilities creative tab's registry key — {@code net.minecraft.world.item.CreativeModeTabs}
     * declares its own {@code TOOLS_AND_UTILITIES} constant, but it (like every other tab constant on that class)
     * is {@code private}, so it has to be rebuilt here from the same {@code "tools_and_utilities"} path (confirmed
     * via {@code javap -c} disassembly of the real client jar) rather than referenced directly.
     */
    private static final ResourceKey<CreativeModeTab> TOOLS_AND_UTILITIES_TAB =
        ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("tools_and_utilities"));

    /**
     * Forces this class to classload, triggering its static field registration. Called explicitly from
     * {@code XaeroWorldMapBook.onInitialize()} — relying on incidental classloading would be fragile. Also adds
     * {@link #ATLAS} to the vanilla Tools and Utilities creative tab, which is what makes it discoverable via the
     * creative inventory's search tab too — an item that is registered but never added to any
     * {@code CreativeModeTabEvents} output never appears in Search either, since Search only aggregates whatever
     * the other tabs were given.
     */
    public static void init()
    {
        CreativeModeTabEvents.modifyOutputEvent(TOOLS_AND_UTILITIES_TAB).register(output -> output.accept(ATLAS));
    }
}
