package me.unium.stalker;

import me.unium.stalker.registry.ModEntities;
import me.unium.stalker.event.StalkerEventHandler;
import me.unium.stalker.command.ShowAggressionCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class Stalker implements ModInitializer {
    public static final String MOD_ID = "stalker";

    @Override
    public void onInitialize() {
        ModEntities.registerEntities();
        StalkerEventHandler.registerEvents();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ShowAggressionCommand.register(dispatcher);
        });
    }
}