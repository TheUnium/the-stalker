package me.unium.stalker.client.model;

import me.unium.stalker.entity.StalkerEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;

public class StalkerEntityModel extends EntityModel<StalkerEntity> {
    private final ModelPart base;

    public StalkerEntityModel(ModelPart modelPart) {
        this.base = modelPart.getChild("base");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();

        modelPartData.addChild("base", ModelPartBuilder.create()
                        .uv(0, 0)
                        .cuboid(-6F, -12F, -6F, 12F, 24F, 12F),
                ModelTransform.pivot(0F, 12F, 0F));

        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void setAngles(StalkerEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        base.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }
}