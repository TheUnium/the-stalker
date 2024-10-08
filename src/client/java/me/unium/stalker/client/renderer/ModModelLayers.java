package me.unium.stalker.client.renderer;

import me.unium.stalker.Stalker;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

public class ModModelLayers {
    public static final EntityModelLayer STALKER = new EntityModelLayer(new Identifier(Stalker.MOD_ID, "stalker"), "main");
}