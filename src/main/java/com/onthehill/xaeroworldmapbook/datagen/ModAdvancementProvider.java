package com.onthehill.xaeroworldmapbook.datagen;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.onthehill.xaeroworldmapbook.XaeroWorldMapBook;
import com.onthehill.xaeroworldmapbook.item.ModItems;
import com.onthehill.xaeroworldmapbook.progression.WellTraveledTrigger;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.advancements.triggers.RecipeCraftedTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * Generates the two advancements Spec 001 defines: {@code craft_atlas}
 * (awarded on either crafting the recipe or obtaining an Atlas by any other
 * means) and {@code well_traveled} (awarded once
 * {@link com.onthehill.xaeroworldmapbook.progression.ChunkVisitTracker}
 * crosses the configured chunk-travel milestone, server-side, only after
 * {@code craft_atlas} is already held). Per
 * {@code fabric-mod-standards.md}'s Datagen section, this is the sole source
 * of the advancement JSON — never hand-edit the generated output.
 */
public final class ModAdvancementProvider extends FabricAdvancementProvider
{
    /**
     * Background texture for the {@code craft_atlas} root advancement's tab. A root advancement's
     * {@code display.background} is a plain texture {@link Identifier} — confirmed via the real vanilla
     * {@code data/minecraft/advancement/story/root.json}, whose own background
     * ({@code "minecraft:gui/advancements/backgrounds/stone"}) resolves to a byte-for-byte duplicate of
     * {@code textures/block/stone.png}. There is no dedicated "dirt" advancement-background sprite among vanilla's
     * own pre-made options (stone/adventure/end/husbandry/nether only), so this points directly at the real dirt
     * block's own texture instead of duplicating a file — {@code Identifier}-to-texture-path resolution doesn't
     * care which folder a texture lives in.
     */
    private static final Identifier CRAFT_ATLAS_BACKGROUND = Identifier.fromNamespaceAndPath("minecraft", "block/dirt");

    public ModAdvancementProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture)
    {
        super(output, registriesFuture);
    }

    @Override
    public void generateAdvancement(HolderLookup.Provider registries, Consumer<AdvancementHolder> consumer)
    {
        AdvancementHolder craftAtlas = Advancement.Builder.advancement()
            .display(
                ModItems.ATLAS,
                Component.translatable("advancements.xaero-world-map-book.craft_atlas.title"),
                Component.translatable("advancements.xaero-world-map-book.craft_atlas.description"),
                CRAFT_ATLAS_BACKGROUND,
                AdvancementType.TASK,
                true, true, false)
            .addCriterion("crafted_atlas", RecipeCraftedTrigger.TriggerInstance.craftedItem(
                ResourceKey.create(Registries.RECIPE, XaeroWorldMapBook.id("atlas"))))
            .addCriterion("obtained_atlas", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.ATLAS))
            .rewards(AdvancementRewards.EMPTY)
            .save(consumer, XaeroWorldMapBook.id("craft_atlas").toString());

        Advancement.Builder.advancement()
            .parent(craftAtlas)
            .display(
                ModItems.ATLAS,
                Component.translatable("advancements.xaero-world-map-book.well_traveled.title"),
                // Deliberately not stating the configured chunk count here:
                // advancement display text is static datapack data with no way
                // to read live server config at render time, so any number
                // baked in here would only ever reflect whatever the default
                // was at datagen time — silently going stale the moment an
                // admin changes chunksRequiredForWellTraveled afterward. Kept
                // intentionally vague rather than showing a number that could
                // be wrong.
                Component.translatable("advancements.xaero-world-map-book.well_traveled.description"),
                null,
                AdvancementType.GOAL,
                true, true, false)
            .addCriterion("well_traveled", WellTraveledTrigger.INSTANCE.createCriterion(
                new WellTraveledTrigger.TriggerInstance(Optional.empty())))
            .rewards(AdvancementRewards.EMPTY)
            .save(consumer, XaeroWorldMapBook.id("well_traveled").toString());
    }
}
