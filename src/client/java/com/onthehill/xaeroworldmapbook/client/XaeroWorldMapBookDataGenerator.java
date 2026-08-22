package com.onthehill.xaeroworldmapbook.client;

import com.onthehill.xaeroworldmapbook.datagen.AtlasRecipeProvider;
import com.onthehill.xaeroworldmapbook.datagen.ModAdvancementProvider;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class XaeroWorldMapBookDataGenerator implements DataGeneratorEntrypoint
{
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator)
    {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        pack.addProvider(AtlasRecipeProvider::new);
        pack.addProvider(ModAdvancementProvider::new);
    }
}
