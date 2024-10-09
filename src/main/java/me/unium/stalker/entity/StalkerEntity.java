package me.unium.stalker.entity;

import me.unium.stalker.event.PlayerDamageEvent;
import me.unium.stalker.event.PlayerEatEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.sound.SoundEvents;
import net.minecraft.particle.ParticleTypes;
import me.unium.stalker.event.StalkerEventHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

public class StalkerEntity extends PathAwareEntity {
    private static final Logger LOGGER = LogManager.getLogger("StalkerMod");

    private PlayerEntity targetPlayer;
    private State currentState = State.IDLE;
    private State previousState = State.IDLE;
    private int timeSinceSeenTicks = 0;
    private int aggressionMeter = 0;
    private final Random random = new Random();
    private int stareAtPlayerTicks = 0;
    private int ticksSinceLastAggressionUpdate = 0;

    private static final double STALKING_RANGE = 50.0D;
    private static final double ATTACK_RANGE = 5.0D;
    private static final int TELEPORT_THRESHOLD_TICKS = 200;
    private static final int MIN_TELEPORT_DISTANCE = 15;
    private static final int MAX_TELEPORT_DISTANCE = 60;
    private static final int AGGRESSION_ATTACK_THRESHOLD = 100;
    private static final int STARE_DURATION = 60;
    private static final int AGGRESSION_UPDATE_INTERVAL = 1200;

    private enum State {
        IDLE, STALKING, ATTACKING, HIDDEN, TAKING_COVER, STARING
    }

    public StalkerEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.setStepHeight(1.0f);
        this.ignoreCameraFrustum = true;
        this.setPersistent();
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
            updateStalkerBehavior();
            dbgInfo();
            updateAggressionMeter(0);
            spawnPositionParticles();
        }
    }

    private void updateStalkerBehavior() {
        PlayerEntity nearestPlayer = this.getWorld().getClosestPlayer(this, STALKING_RANGE);
        if (nearestPlayer == null) {
            currentState = State.IDLE;
            return;
        }

        targetPlayer = nearestPlayer;
        double distanceToPlayer = this.squaredDistanceTo(targetPlayer);

        switch (currentState) {
            case IDLE:
                idleStatus(distanceToPlayer);
                break;
            case STALKING:
                stalingState(distanceToPlayer);
                break;
            case ATTACKING:
                attackingState();
                break;
            case HIDDEN:
                hiddenState();
                break;
            case TAKING_COVER:
                takingCoverState();
                break;
            case STARING:
                staringState();
                break;
        }

        if (currentState != previousState) {
            previousState = currentState;
            if (this.getWorld() instanceof ServerWorld) {
                ((ServerWorld) this.getWorld()).getServer().sendMessage(
                        net.minecraft.text.Text.literal(String.format("state changed to %s", currentState))
                );
            }
        }
    }

    private void idleStatus(double distanceToPlayer) {
        if (distanceToPlayer < STALKING_RANGE * STALKING_RANGE && shouldStartStalking()) {
            currentState = State.STALKING;
            spooooookySound();
        }
    }

    private void stalingState(double distanceToPlayer) {
        if (aggressionMeter >= AGGRESSION_ATTACK_THRESHOLD) {
            if (stalkerBeingObserved()) {
                currentState = State.ATTACKING;
            } else {
                moveTowardsPlayer();
            }
        } else {
            if (stalkerBeingObserved()) {
                if (random.nextDouble() < 0.1) {
                    currentState = State.STARING;
                    stareAtPlayerTicks = 0;
                } else if (random.nextBoolean()) {
                    currentState = State.HIDDEN;
                    timeSinceSeenTicks = 0;
                    teleportRandomly();
                    setInvisible(true);
                } else {
                    currentState = State.TAKING_COVER;
                    takeCover();
                }
                spooooookySound();
            } else if (distanceToPlayer < ATTACK_RANGE * ATTACK_RANGE) {
                currentState = State.HIDDEN;
                timeSinceSeenTicks = 0;
                teleportRandomly();
                setInvisible(true);
            } else {
                moveAroundPlayerRandomly();
            }
        }
    }

    private void attackingState() {
        if (!stalkerBeingObserved() || this.getRandom().nextDouble() < 0.1 || aggressionMeter >= AGGRESSION_ATTACK_THRESHOLD) {
            attackPlayer();
            aggressionMeter = 0;
        } else {
            currentState = State.STALKING;
        }
    }

    private void hiddenState() {
        timeSinceSeenTicks++;
        if (timeSinceSeenTicks > TELEPORT_THRESHOLD_TICKS) {
            teleportRandomly();
            currentState = State.STALKING;
            setInvisible(false);
            spooooookySound();
        }
    }

    private void takingCoverState() {
        if (!stalkerBeingObserved()) {
            currentState = State.STALKING;
            setInvisible(false);
        } else if (timeSinceSeenTicks > TELEPORT_THRESHOLD_TICKS) {
            teleportRandomly();
            currentState = State.STALKING;
            setInvisible(false);
            spooooookySound();
        } else {
            timeSinceSeenTicks++;
            takeCover();
        }
    }

    private void staringState() {
        stareAtPlayerTicks++;
        if (stareAtPlayerTicks >= STARE_DURATION || !stalkerBeingObserved()) {
            currentState = State.STALKING;
        } else {
            this.getLookControl().lookAt(targetPlayer, 100.0F, 100.0F);
            if (stareAtPlayerTicks % 20 == 0) {
                spooooookySound();
            }
        }
    }

    private boolean shouldStartStalking() {
        return random.nextDouble() < 0.1;
    }

    private boolean stalkerBeingObserved() {
        if (targetPlayer == null) return false;
        Vec3d playerLook = targetPlayer.getRotationVec(1.0F);
        Vec3d toStalker = this.getPos().subtract(targetPlayer.getEyePos()).normalize();
        double dot = playerLook.dotProduct(toStalker);
        return dot > 0.85;
    }

    private void moveAroundPlayerRandomly() {
        if (targetPlayer != null) {
            Vec3d randomOffset = new Vec3d(
                    random.nextDouble() - 0.5,
                    0,
                    random.nextDouble() - 0.5
            ).multiply(10);
            Vec3d targetPos = targetPlayer.getPos().add(randomOffset);
            this.getNavigation().startMovingTo(targetPos.x, targetPos.y, targetPos.z, 1.0);
        }
    }

    private void moveTowardsPlayer() {
        if (targetPlayer != null) {
            this.getNavigation().startMovingTo(targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ(), 1.0);
        }
    }

    private void attackPlayer() {
        if (targetPlayer != null) {
            this.tryAttack(targetPlayer);
            attackSound();
        }
    }

    private void teleportRandomly() {
        if (targetPlayer == null) return;
        for (int attempts = 0; attempts < 50; attempts++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = MIN_TELEPORT_DISTANCE + random.nextDouble() * (MAX_TELEPORT_DISTANCE - MIN_TELEPORT_DISTANCE);

            double x = targetPlayer.getX() + Math.cos(angle) * distance;
            double z = targetPlayer.getZ() + Math.sin(angle) * distance;
            double y = findSafeY(new BlockPos((int)x, (int)targetPlayer.getY(), (int)z));

            if (y != -1) {
                this.teleport(x, y, z);
                return;
            }
        }
    }

    private double findSafeY(BlockPos pos) {
        World world = this.getWorld();
        BlockPos ground = world.getTopPosition(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, pos);
        if (world.getBlockState(ground).isAir() && world.getBlockState(ground.down()).isSolid()) {
            return ground.getY();
        }

        return -1;
    }

    private void takeCover() {
        if (targetPlayer == null) return;
        Vec3d playerLook = targetPlayer.getRotationVec(1.0F).normalize();
        Vec3d perpendicular = new Vec3d(-playerLook.z, 0, playerLook.x).normalize();
        Vec3d potentialHidingSpot = this.getPos().add(perpendicular.multiply(3));
        BlockPos hidingPos = new BlockPos((int)potentialHidingSpot.x, (int)potentialHidingSpot.y, (int)potentialHidingSpot.z);

        if (posObstructed(hidingPos)) {
            this.getNavigation().startMovingTo(potentialHidingSpot.x, potentialHidingSpot.y, potentialHidingSpot.z, 1.0);
        }
    }

    private boolean posObstructed(BlockPos pos) {
        World world = this.getWorld();
        BlockState state = world.getBlockState(pos);
        return !state.isAir() && state.isOpaque();
    }

    public void onPlayerEat(PlayerEatEvent event) {
        LOGGER.info("Stalker {} received PlayerEatEvent for player {}", this.getUuid(), event.getPlayer().getName().getString());
        int oldAggression = aggressionMeter;
        updateAggressionMeter(3);
        LOGGER.info("Stalker {} aggression increased from {} to {}", this.getUuid(), oldAggression, aggressionMeter);
    }

    public void onPlayerDamage(PlayerDamageEvent event) {
        LOGGER.info("Stalker {} received PlayerDamageEvent for player {}", this.getUuid(), event.getPlayer().getName().getString());
        updateAggressionMeter(3);
    }

    public int getAggressionMeter() {
        return aggressionMeter;
    }

    private void updateAggressionMeter(int amount) {
        // todo: fix this shit, its broken as fuck and idk why
        ticksSinceLastAggressionUpdate++;

        if (ticksSinceLastAggressionUpdate >= AGGRESSION_UPDATE_INTERVAL) {
            aggressionMeter += 50;
            ticksSinceLastAggressionUpdate = 0;
        }

        if (aggressionMeter >= 100) {
            spooooookySound();
        }

        if (stalkerBeingObserved()) {
            aggressionMeter -= 1;
        }

        if (amount > 0) {
            aggressionMeter += amount;
        }

        long timeOfDay = this.getWorld().getTimeOfDay() % 24000;
        if (timeOfDay == 13000) {
            aggressionMeter += 25;
        } else if (timeOfDay == 0) {
            aggressionMeter -= 25;
        }

        aggressionMeter = Math.max(0, Math.min(100, aggressionMeter));
    }

    private void spooooookySound() {
        this.playSound(SoundEvents.ENTITY_ENDERMAN_AMBIENT, 1.0F, 0.5F);
    }

    private void attackSound() {
        this.playSound(SoundEvents.ENTITY_ENDERMAN_SCREAM, 1.0F, 0.5F);
    }

    private void spawnPositionParticles() {
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                    ParticleTypes.END_ROD,
                    this.getX(),
                    this.getY() + 0.5,
                    this.getZ(),
                    10,
                    0.1,
                    0.1,
                    0.1,
                    0.05
            );
        }
    }

    private void dbgInfo() {
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.getServer().getPlayerManager().getPlayerList().forEach(player -> {
                if (player == targetPlayer) {
                    String debugInfo = String.format("""
                            state: %s | coords: (%.2f, %.2f, %.2f) | distance: %.2f | last seen: %d ticks | aggression: %d/100
                            """,
                            currentState,
                            this.getX(), this.getY(), this.getZ(),
                            Math.sqrt(this.squaredDistanceTo(targetPlayer)),
                            timeSinceSeenTicks,
                            getAggressionMeter()
                    );
                    player.sendMessage(net.minecraft.text.Text.literal(debugInfo), true);
                }
            });
        }
    }

    private class AdvancedStalkPlayerGoal extends Goal {
        private final StalkerEntity stalker;

        public AdvancedStalkPlayerGoal(StalkerEntity stalker) {
            this.stalker = stalker;
        }

        @Override
        public boolean canStart() {
            return targetPlayer != null;
        }

        @Override
        public boolean shouldContinue() {
            return targetPlayer != null && targetPlayer.isAlive() &&
                    stalker.squaredDistanceTo(targetPlayer) <= STALKING_RANGE * STALKING_RANGE;
        }

        @Override
        public void tick() {
            if (currentState == State.STALKING || currentState == State.ATTACKING) {
                stalker.getLookControl().lookAt(targetPlayer, 100.0F, 100.0F);

                if (currentState == State.STALKING) {
                    stalker.moveAroundPlayerRandomly();
                }
            }
        }
    }
}