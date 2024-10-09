package me.unium.stalker.registry;

import me.unium.stalker.Stalker;
import me.unium.stalker.entity.StalkerEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class ModEntities {
    public static final EntityType<StalkerEntity> STALKER = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(Stalker.MOD_ID, "stalker"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, (EntityType<StalkerEntity> entityType, World world) ->
                            StalkerEntity.getInstance(entityType, world))
                    .dimensions(EntityDimensions.fixed(0.6f, 1.8f))
                    .build()
    );

    public static void registerEntities() {
        FabricDefaultAttributeRegistry.register(STALKER, StalkerEntity.createStalkerAttributes());
    }
}