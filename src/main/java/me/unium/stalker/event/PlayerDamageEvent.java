package me.unium.stalker.event;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;

public class PlayerDamageEvent {
    private final PlayerEntity player;
    private final DamageSource source;
    private final float amount;

    public PlayerDamageEvent(PlayerEntity player, DamageSource source, float amount) {
        this.player = player;
        this.source = source;
        this.amount = amount;
    }

    public PlayerEntity getPlayer() {
        return player;
    }

    public DamageSource getSource() {
        return source;
    }

    public float getAmount() {
        return amount;
    }
}