package me.unium.stalker.entity;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.world.World;
import java.util.Random;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Random;

public class StalkerBehavior {
    private static final double STALKING_RANGE = 50.0D;
    private static final double ATTACK_RANGE = 5.0D;
    private static final int TELEPORT_THRESHOLD_TICKS = 200;
    private static final int MIN_TELEPORT_DISTANCE = 15;
    private static final int MAX_TELEPORT_DISTANCE = 60;
    private static final int STARE_DURATION = 60;
    private static final int AGGRESSION_UPDATE_INTERVAL = 1200;
    private static final int SHIELD_DISABLE_DURATION = 200;
    private static final int MIN_ATTACK_DURATION_TICKS = 600;
    private static final int MAX_ATTACK_DURATION_TICKS = 1200;
    private static final double DAMAGE_RANGE = 4.0D; // no more bedrock moments on java :pensive:
    private static final int MAX_FOLLOW_TICKS = 40;

    private final StalkerEntity stalker;
    private final Random random = new Random();
    private int attackDurationTicks = 0;
    private int currentAttackDuration = 0;
    private int followPlayerTicks = 0;

    private static final Logger LOGGER = LogManager.getLogger("StalkerMod");
    public StalkerBehavior(StalkerEntity stalker) {
        this.stalker = stalker;
    }

    public void updateStalkerBehavior() {
        try {
            PlayerEntity nearestPlayer = stalker.getWorld().getClosestPlayer(stalker, STALKING_RANGE);
            if (nearestPlayer == null) {
                stalker.setCurrentState(StalkerState.IDLE);
                return;
            }

            stalker.setTargetPlayer(nearestPlayer);
            double distanceToPlayer = stalker.squaredDistanceTo(stalker.getTargetPlayer());

            switch (stalker.getCurrentState()) {
                case IDLE:
                    idleStatus(distanceToPlayer);
                    break;
                case STALKING:
                    stalkingState(distanceToPlayer);
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

            if (stalker.getCurrentState() != stalker.getPreviousState()) {
                stalker.setPreviousState(stalker.getCurrentState());
                if (stalker.getWorld() instanceof ServerWorld) {
                    ((ServerWorld) stalker.getWorld()).getServer().sendMessage(
                            net.minecraft.text.Text.literal(String.format("state changed to %s", stalker.getCurrentState()))
                    );
                }
            }
        } catch (Exception e) {
            LOGGER.error("shits fucked in updateStalkerBehavior() : ", e);
            stalker.setCurrentState(StalkerState.IDLE);
        }
    }

    private void idleStatus(double distanceToPlayer) {
        if (distanceToPlayer < STALKING_RANGE * STALKING_RANGE && shouldStartStalking()) {
            stalker.setCurrentState(StalkerState.STALKING);
            spooooookySound();
        }
    }

    private void stalkingState(double distanceToPlayer) {
        float attackProbability = stalker.getAggressionMeter() / 150.0f;

        if (stalkerBeingObserved()) {
            // if triggered :
            // if stalker is more than 7 blocks away, it will slowly advance towards the player for 40 ticks
            // when its just 7 blocks away, theres a 50-50 chance of it attacking or taking cover
            if (followPlayerTicks > 0) {
                followPlayerTicks--;
                if (followPlayerTicks <= 0 || distanceToPlayer <= 7 * 7) {
                    if (random.nextBoolean()) {
                        stalker.setCurrentState(StalkerState.ATTACKING);
                    } else {
                        stalker.setCurrentState(StalkerState.TAKING_COVER);
                        takeCover();
                    }
                    followPlayerTicks = 0;
                } else {
                    if (stalker.getNavigation().isIdle()) {
                        Vec3d playerPos = stalker.getTargetPlayer().getPos();
                        Vec3d stalkerPos = stalker.getPos();
                        Vec3d direction = playerPos.subtract(stalkerPos).normalize();
                        Vec3d targetPos = stalkerPos.add(direction.multiply(2));
                        stalker.getNavigation().startMovingTo(targetPos.x, targetPos.y, targetPos.z, 0.5);
                    }
                    spooooookySound();
                }
            } else if (random.nextFloat() < 0.2) {
                followPlayerTicks = MAX_FOLLOW_TICKS;
            } else {
                if (random.nextDouble() < 0.1) {
                    stalker.setCurrentState(StalkerState.STARING);
                    stalker.setStareAtPlayerTicks(0);
                } else if (random.nextBoolean()) {
                    stalker.setCurrentState(StalkerState.HIDDEN);
                    stalker.setTimeSinceSeenTicks(0);
                    teleportRandomly();
                    stalker.setInvisible(true);
                } else {
                    stalker.setCurrentState(StalkerState.TAKING_COVER);
                    takeCover();
                }
                spooooookySound();
            }
        } else {
            followPlayerTicks = 0;
            if (random.nextFloat() < attackProbability) {
                moveTowardsPlayer();
            } else if (distanceToPlayer < ATTACK_RANGE * ATTACK_RANGE) {
                stalker.setCurrentState(StalkerState.HIDDEN);
                stalker.setTimeSinceSeenTicks(0);
                teleportRandomly();
                stalker.setInvisible(true);
            } else {
                moveAroundPlayerRandomly();
            }
        }
    }

    private void attackingState() {
        if (attackDurationTicks == 0) {
            currentAttackDuration = MIN_ATTACK_DURATION_TICKS + random.nextInt(MAX_ATTACK_DURATION_TICKS - MIN_ATTACK_DURATION_TICKS);
        }

        attackDurationTicks++;

        if (attackDurationTicks < currentAttackDuration) {
            moveTowardsPlayer();
            attackPlayer();
        } else {
            float continueAttackProbability = stalker.getAggressionMeter() / 200.0f;

            if (random.nextFloat() < continueAttackProbability) {
                moveTowardsPlayer();
                attackPlayer();
                stalker.setAggressionMeter(0.0f);
            } else {
                stalker.setCurrentState(StalkerState.STALKING);
                attackDurationTicks = 0;
            }
        }
    }

    private void hiddenState() {
        stalker.setTimeSinceSeenTicks(stalker.getTimeSinceSeenTicks() + 1);
        if (stalker.getTimeSinceSeenTicks() > TELEPORT_THRESHOLD_TICKS) {
            teleportRandomly();
            stalker.setCurrentState(StalkerState.STALKING);
            stalker.setInvisible(false);
            spooooookySound();
        }
    }

    private void takingCoverState() {
        if (!stalkerBeingObserved()) {
            stalker.setCurrentState(StalkerState.STALKING);
            stalker.setInvisible(false);
        } else if (stalker.getTimeSinceSeenTicks() > TELEPORT_THRESHOLD_TICKS) {
            teleportRandomly();
            stalker.setCurrentState(StalkerState.STALKING);
            stalker.setInvisible(false);
            spooooookySound();
        } else {
            stalker.setTimeSinceSeenTicks(stalker.getTimeSinceSeenTicks() + 1);
            takeCover();
        }
    }

    private void staringState() {
        stalker.setStareAtPlayerTicks(stalker.getStareAtPlayerTicks() + 1);
        if (stalker.getStareAtPlayerTicks() >= STARE_DURATION || !stalkerBeingObserved()) {
            stalker.setCurrentState(StalkerState.STALKING);
        } else {
            stalker.getLookControl().lookAt(stalker.getTargetPlayer(), 100.0F, 100.0F);
            if (stalker.getStareAtPlayerTicks() % 20 == 0) {
                spooooookySound();
            }
        }
    }

    private boolean shouldStartStalking() {
        return random.nextDouble() < 0.1;
    }

    private boolean stalkerBeingObserved() {
        if (stalker.getTargetPlayer() == null) return false;
        Vec3d playerLook = stalker.getTargetPlayer().getRotationVec(1.0F);
        Vec3d toStalker = stalker.getPos().subtract(stalker.getTargetPlayer().getEyePos()).normalize();
        double dot = playerLook.dotProduct(toStalker);
        return dot > 0.85;
    }

    public void moveAroundPlayerRandomly() {
        PlayerEntity targetPlayer = stalker.getTargetPlayer();

        if (targetPlayer == null) {
            LOGGER.warn("shits fucked. player null.");
            return;
        }

        if (targetPlayer.isRemoved() || !targetPlayer.isAlive()) {
            LOGGER.info("target player got sent to the shadow realm.");
            stalker.setTargetPlayer(null);
            return;
        }

        Vec3d randomOffset = new Vec3d(
                random.nextDouble() - 0.5,
                0,
                random.nextDouble() - 0.5
        ).multiply(10);

        Vec3d targetPos = stalker.getTargetPlayer().getPos().add(randomOffset);

        double x = targetPos.x;
        double y = targetPos.y;
        double z = targetPos.z;

        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            LOGGER.warn("fucked up player pos ({}, {}, {})", x, y, z);
            return;
        }

        if (stalker.getNavigation() == null) {
            LOGGER.error("stalker nav is null, cant move towards player");
            return;
        }

        boolean success = stalker.getNavigation().startMovingTo(x, y, z, 1.0);
        if (!success) {
            LOGGER.warn("couldnt start moving towards player @ ({}, {}, {})", x, y, z);
        }
    }

    private void moveTowardsPlayer() {
        if (stalker == null || stalker.getTargetPlayer() == null) {
            LOGGER.warn("shits fucked. stalker or player null.");
            return;
        }

        try {
            PlayerEntity targetPlayer = stalker.getTargetPlayer();
            if (targetPlayer.isRemoved() || !targetPlayer.isAlive()) {
                LOGGER.info("target player got sent to the shadow realm.");
                stalker.setTargetPlayer(null);
                return;
            }

            double x = targetPlayer.getX();
            double y = targetPlayer.getY();
            double z = targetPlayer.getZ();

            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
                LOGGER.warn("fucked up player pos ({}, {}, {})", x, y, z);
                return;
            }

            if (stalker.getNavigation() == null) {
                LOGGER.error("stalker nav is null, cant move towards player");
                return;
            }

            boolean success = stalker.getNavigation().startMovingTo(x, y, z, 1.0);
            if (!success) {
                LOGGER.warn("couldnt start moving towards player @ ({}, {}, {})", x, y, z);
            }
        } catch (Exception e) {
            LOGGER.error("a wild error appeared! ", e);
        }
    }

    private void attackPlayer() {
        PlayerEntity targetPlayer = stalker.getTargetPlayer();
        if (targetPlayer != null) {
            double distanceToPlayer = stalker.squaredDistanceTo(targetPlayer);

            if (distanceToPlayer <= DAMAGE_RANGE * DAMAGE_RANGE) {
                if (targetPlayer.isBlocking()) {
                    targetPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, SHIELD_DISABLE_DURATION, 100));
                    targetPlayer.getItemCooldownManager().set(targetPlayer.getActiveItem().getItem(), SHIELD_DISABLE_DURATION);
                }

                stalker.tryAttack(targetPlayer);
                attackSound();
            }
        }
    }

    private void teleportRandomly() {
        if (stalker.getTargetPlayer() == null) return;
        for (int attempts = 0; attempts < 50; attempts++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = MIN_TELEPORT_DISTANCE + random.nextDouble() * (MAX_TELEPORT_DISTANCE - MIN_TELEPORT_DISTANCE);

            double x = stalker.getTargetPlayer().getX() + Math.cos(angle) * distance;
            double z = stalker.getTargetPlayer().getZ() + Math.sin(angle) * distance;
            double y = findSafeY(new BlockPos((int)x, (int)stalker.getTargetPlayer().getY(), (int)z));

            if (y != -1) {
                stalker.teleport(x, y, z);
                LOGGER.info("stalker tpd to {}, {}, {}", x, y, z);
                return;
            }
        }
        LOGGER.warn("coulnt find a safe tp location after 50 atts");
    }

    private double findSafeY(BlockPos pos) {
        World world = stalker.getWorld();
        BlockPos ground = world.getTopPosition(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, pos);
        if (world.getBlockState(ground).isAir() && world.getBlockState(ground.down()).isSolid()) {
            return ground.getY();
        }
        return -1;
    }

    private void takeCover() {
        if (stalker.getTargetPlayer() == null) return;
        Vec3d playerLook = stalker.getTargetPlayer().getRotationVec(1.0F).normalize();
        Vec3d perpendicular = new Vec3d(-playerLook.z, 0, playerLook.x).normalize();
        Vec3d potentialHidingSpot = stalker.getPos().add(perpendicular.multiply(3));
        BlockPos hidingPos = new BlockPos((int)potentialHidingSpot.x, (int)potentialHidingSpot.y, (int)potentialHidingSpot.z);

        if (posObstructed(hidingPos)) {
            stalker.getNavigation().startMovingTo(potentialHidingSpot.x, potentialHidingSpot.y, potentialHidingSpot.z, 1.0);
        }
    }

    private boolean posObstructed(BlockPos pos) {
        World world = stalker.getWorld();
        BlockState state = world.getBlockState(pos);
        return !state.isAir() && state.isOpaque();
    }

    public void updateAggressionMeter(float amount) {
        stalker.setTicksSinceLastAggressionUpdate(stalker.getTicksSinceLastAggressionUpdate() + 1);

        if (stalker.getTicksSinceLastAggressionUpdate() >= AGGRESSION_UPDATE_INTERVAL) {
            stalker.setAggressionMeter(stalker.getAggressionMeter() + 10.0f);
            stalker.setTicksSinceLastAggressionUpdate(0);
        }

        if (stalkerBeingObserved()) {
            if (Math.random() < 0.5) {
                stalker.setAggressionMeter(stalker.getAggressionMeter() + 0.1f);
            } else {
                stalker.setAggressionMeter(stalker.getAggressionMeter() - 0.1f);
            }
        }

        if (amount > 0) {
            stalker.setAggressionMeter(stalker.getAggressionMeter() + amount);
        }

        long timeOfDay = stalker.getWorld().getTimeOfDay() % 24000;
        if (timeOfDay == 13000) {
            stalker.setAggressionMeter(stalker.getAggressionMeter() + 25.0f);
        } else if (timeOfDay == 0) {
            stalker.setAggressionMeter(stalker.getAggressionMeter() - 25.0f);
        }

        stalker.setAggressionMeter(Math.max(0.0f, Math.min(100.0f, stalker.getAggressionMeter())));
    }

    private void spooooookySound() {
        stalker.playSound(SoundEvents.ENTITY_ENDERMAN_AMBIENT, 1.0F, 0.5F);
    }

    private void attackSound() {
        stalker.playSound(SoundEvents.ENTITY_ENDERMAN_SCREAM, 1.0F, 0.5F);
    }

    public void spawnPositionParticles() {
        if (stalker.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                    ParticleTypes.END_ROD,
                    stalker.getX(),
                    stalker.getY() + 0.5,
                    stalker.getZ(),
                    10,
                    0.1,
                    0.1,
                    0.1,
                    0.05
            );
        }
    }

    public void dbgInfo() {
        if (stalker.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.getServer().getPlayerManager().getPlayerList().forEach(player -> {
                if (player == stalker.getTargetPlayer()) {
                    String debugInfo = String.format("""
                            state: %s | coords: (%.2f, %.2f, %.2f) | distance: %.2f | last seen: %d ticks | aggression: %.5f/100 | attack chance: %.5f | attack duration: %d/%d
                            """,
                            stalker.getCurrentState(),
                            stalker.getX(), stalker.getY(), stalker.getZ(),
                            Math.sqrt(stalker.squaredDistanceTo(stalker.getTargetPlayer())),
                            stalker.getTimeSinceSeenTicks(),
                            stalker.getAggressionMeter(),
                            stalker.getAggressionMeter() / 200.0f,
                            attackDurationTicks,
                            currentAttackDuration
                    );
                    player.sendMessage(net.minecraft.text.Text.literal(debugInfo), true);
                }
            });
        }
    }
}