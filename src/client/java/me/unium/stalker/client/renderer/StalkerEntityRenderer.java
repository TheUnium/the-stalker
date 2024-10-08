package me.unium.stalker.client.renderer;

import me.unium.stalker.Stalker;
import me.unium.stalker.client.model.StalkerEntityModel;
import me.unium.stalker.entity.StalkerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class StalkerEntityRenderer extends MobEntityRenderer<StalkerEntity, StalkerEntityModel> {
    private static final Identifier TEXTURE = new Identifier(Stalker.MOD_ID, "textures/entity/stalker.png");

    public StalkerEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new StalkerEntityModel(context.getPart(ModModelLayers.STALKER)), 0.5f);
    }

    @Override
    public Identifier getTexture(StalkerEntity entity) {
        return TEXTURE;
    }
}
