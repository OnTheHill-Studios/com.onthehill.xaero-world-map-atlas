package com.onthehill.xaeroworldmapbook.datagen;

import java.util.concurrent.CompletableFuture;

import com.onthehill.xaeroworldmapbook.item.ModItems;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Generates the Atlas's shapeless crafting recipe: any compass + one empty
 * map (not a filled map) + one book &rarr; one Atlas. Per
 * {@code fabric-mod-standards.md}'s Datagen section, this is the sole source
 * of the recipe JSON — never hand-edit the generated output.
 */
public final class AtlasRecipeProvider extends FabricRecipeProvider
{
    public AtlasRecipeProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture)
    {
        super(output, registriesFuture);
    }

    @Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput recipeOutput)
    {
        return new RecipeProvider(registries, recipeOutput)
        {
            @Override
            public void buildRecipes()
            {
                HolderGetter<Item> items = registries.lookupOrThrow(Registries.ITEM);
                ShapelessRecipeBuilder.shapeless(items, RecipeCategory.TOOLS, ModItems.ATLAS)
                    .requires(ItemTags.COMPASSES)
                    .requires(Items.MAP)
                    .requires(Items.BOOK)
                    .unlockedBy("has_compass", has(ItemTags.COMPASSES))
                    .save(recipeOutput);
            }
        };
    }

    @Override
    public String getName()
    {
        return "Atlas Recipes";
    }
}
