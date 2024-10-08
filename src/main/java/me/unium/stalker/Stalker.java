package me.unium.stalker;

import me.unium.stalker.registry.ModEntities;
import net.fabricmc.api.ModInitializer;

public class Stalker implements ModInitializer {
    public static final String MOD_ID = "stalker";

    @Override
    public void onInitialize() {
        ModEntities.registerEntities();
    }
}