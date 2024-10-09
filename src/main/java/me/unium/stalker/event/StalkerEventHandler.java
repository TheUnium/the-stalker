package me.unium.stalker.event;

import me.unium.stalker.entity.StalkerEntity;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.TypedActionResult;
import net.minecraft.server.MinecraftServer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

public class StalkerEventHandler {
    private static StalkerEntity currentStalker;
    private static final Logger LOGGER = LogManager.getLogger("StalkerMod");

    public static StalkerEntity getCurrentStalker() {
        return currentStalker;
    }

    public static void registerEvents() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (stack.isFood() && player.canConsume(false)) {
                LOGGER.info("Player {} is eating {}", player.getName().getString(), stack.getItem().getName().getString());
                notifyStalker(new PlayerEatEvent(player, stack));
            }
            return TypedActionResult.pass(stack);
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof PlayerEntity player) {
                LOGGER.info("Player {} took {} damage from {}", player.getName().getString(), amount, source.getName());
                notifyStalker(new PlayerDamageEvent(player, source, amount));
            }
            return true;
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            LOGGER.info("Player {} joined the game", handler.player.getName().getString());
        });
    }

    private static void notifyStalker(Object event) {
        if (currentStalker == null) {
            LOGGER.warn("Event occurred but no Stalker is currently set");
            return;
        }

        if (event instanceof PlayerEatEvent) {
            LOGGER.info("Notifying Stalker of PlayerEatEvent");
            currentStalker.onPlayerEat((PlayerEatEvent) event);
        } else if (event instanceof PlayerDamageEvent) {
            LOGGER.info("Notifying Stalker of PlayerDamageEvent");
            currentStalker.onPlayerDamage((PlayerDamageEvent) event);
        }
    }

    public static void setCurrentStalker(StalkerEntity stalker) {
        currentStalker = stalker;
        LOGGER.info("Current Stalker set to {}", stalker.getUuid());
    }

    public static void removeCurrentStalker() {
        if (currentStalker != null) {
            LOGGER.info("Removing current Stalker {}", currentStalker.getUuid());
            currentStalker = null;
        }
    }
}