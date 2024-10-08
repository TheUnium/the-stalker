package me.unium.stalker.client;

import me.unium.stalker.client.model.StalkerEntityModel;
import me.unium.stalker.client.renderer.ModModelLayers;
import me.unium.stalker.client.renderer.StalkerEntityRenderer;
import me.unium.stalker.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class StalkerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.STALKER, StalkerEntityRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.STALKER, StalkerEntityModel::getTexturedModelData);
    }
}