package me.unium.stalker.event;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public class PlayerEatEvent {
    private final PlayerEntity player;
    private final ItemStack food;

    public PlayerEatEvent(PlayerEntity player, ItemStack food) {
        this.player = player;
        this.food = food;
    }

    public PlayerEntity getPlayer() {
        return player;
    }

    public ItemStack getFood() {
        return food;
    }
}