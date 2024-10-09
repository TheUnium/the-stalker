package me.unium.stalker.entity;

import me.unium.stalker.event.PlayerDamageEvent;
import me.unium.stalker.event.PlayerEatEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import me.unium.stalker.event.StalkerEventHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StalkerEntity extends PathAwareEntity {
    private static final Logger LOGGER = LogManager.getLogger("StalkerMod");

    private PlayerEntity targetPlayer;
    private StalkerState currentState = StalkerState.IDLE;
    private StalkerState previousState = StalkerState.IDLE;
    private int timeSinceSeenTicks = 0;
    private int aggressionMeter = 0;
    private int stareAtPlayerTicks = 0;
    private int ticksSinceLastAggressionUpdate = 0;

    private final StalkerBehavior behavior;

    public StalkerEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.setStepHeight(1.0f);
        this.ignoreCameraFrustum = true;
        this.setPersistent();
        this.behavior = new StalkerBehavior(this);
        StalkerEventHandler.setCurrentStalker(this);
        LOGGER.info("Stalker {} created and set as current", this.getUuid());
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        StalkerEventHandler.removeCurrentStalker();
        LOGGER.info("Stalker {} removed", this.getUuid());
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new AdvancedStalkPlayerGoal(this));
    }

    public static DefaultAttributeContainer.Builder createStalkerAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, Float.MAX_VALUE)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 100.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0D);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource damageSource) {
        return true;
    }

    @Override
    public boolean isPushedByFluids() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.getWorld().isClient) {
            behavior.updateStalkerBehavior();
            behavior.dbgInfo();
            behavior.updateAggressionMeter(0);
            behavior.spawnPositionParticles();
        }
    }

    public void onPlayerEat(PlayerEatEvent event) {
        LOGGER.info("Stalker {} received PlayerEatEvent for player {}", this.getUuid(), event.getPlayer().getName().getString());
        int oldAggression = aggressionMeter;
        behavior.updateAggressionMeter(3);
        LOGGER.info("Stalker {} aggression increased from {} to {}", this.getUuid(), oldAggression, aggressionMeter);
    }

    public void onPlayerDamage(PlayerDamageEvent event) {
        LOGGER.info("Stalker {} received PlayerDamageEvent for player {}", this.getUuid(), event.getPlayer().getName().getString());
        behavior.updateAggressionMeter(3);
    }

    public PlayerEntity getTargetPlayer() { return targetPlayer; }
    public void setTargetPlayer(PlayerEntity player) { this.targetPlayer = player; }
    public StalkerState getCurrentState() { return currentState; }
    public void setCurrentState(StalkerState state) { this.currentState = state; }
    public StalkerState getPreviousState() { return previousState; }
    public void setPreviousState(StalkerState state) { this.previousState = state; }
    public int getTimeSinceSeenTicks() { return timeSinceSeenTicks; }
    public void setTimeSinceSeenTicks(int ticks) { this.timeSinceSeenTicks = ticks; }
    public int getAggressionMeter() { return aggressionMeter; }
    public void setAggressionMeter(int aggression) { this.aggressionMeter = aggression; }
    public int getStareAtPlayerTicks() { return stareAtPlayerTicks; }
    public void setStareAtPlayerTicks(int ticks) { this.stareAtPlayerTicks = ticks; }
    public int getTicksSinceLastAggressionUpdate() { return ticksSinceLastAggressionUpdate; }
    public void setTicksSinceLastAggressionUpdate(int ticks) { this.ticksSinceLastAggressionUpdate = ticks; }
    public StalkerBehavior getBehavior() { return behavior; }
}