package com.hyperfactions.worldmap;

import com.hyperfactions.util.Logger;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.worldmap.IWorldMap;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapLoadException;
import com.hypixel.hytale.server.core.universe.world.worldmap.provider.IWorldMapProvider;

/**
 * World map provider that returns the HyperFactions world map generator.
 * This provider is set on the WorldConfig to replace the default world map
 * with our custom implementation that renders claim overlays.
 */
public class HyperFactionsWorldMapProvider implements IWorldMapProvider {

    public static final String ID = "HyperFactions";
    public static final BuilderCodec<HyperFactionsWorldMapProvider> CODEC =
            BuilderCodec.builder(HyperFactionsWorldMapProvider.class, HyperFactionsWorldMapProvider::new).build();

    @Override
    public IWorldMap getGenerator(World world) throws WorldMapLoadException {
        Logger.info("HyperFactionsWorldMapProvider.getGenerator() called for world: %s", world.getName());
        IWorldMap generator = HyperFactionsWorldMap.INSTANCE;
        Logger.info("Returning HyperFactionsWorldMap instance: %s", generator.getClass().getName());
        return generator;
    }
}
