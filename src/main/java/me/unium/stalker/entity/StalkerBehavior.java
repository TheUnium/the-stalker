package me.unium.stalker.entity;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.world.World;

import java.util.Random;

public class StalkerBehavior {
    private static final double STALKING_RANGE = 50.0D;
    private static final double ATTACK_RANGE = 5.0D;
    private static final int TELEPORT_THRESHOLD_TICKS = 200;
    private static final int MIN_TELEPORT_DISTANCE = 15;
    private static final int MAX_TELEPORT_DISTANCE = 60;
    private static final int AGGRESSION_ATTACK_THRESHOLD = 100;
    private static final int STARE_DURATION = 60;
    private static final int AGGRESSION_UPDATE_INTERVAL = 1200;

    private final StalkerEntity stalker;
    private final Random random = new Random();

    public StalkerBehavior(StalkerEntity stalker) {
        this.stalker = stalker;
    }

    public void updateStalkerBehavior() {
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
    }

    private void idleStatus(double distanceToPlayer) {
        if (distanceToPlayer < STALKING_RANGE * STALKING_RANGE && shouldStartStalking()) {
            stalker.setCurrentState(StalkerState.STALKING);
            spooooookySound();
        }
    }

    private void stalkingState(double distanceToPlayer) {
        if (stalker.getAggressionMeter() >= AGGRESSION_ATTACK_THRESHOLD) {
            if (stalkerBeingObserved()) {
                stalker.setCurrentState(StalkerState.ATTACKING);
            } else {
                moveTowardsPlayer();
            }
        } else {
            if (stalkerBeingObserved()) {
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
        if (!stalkerBeingObserved() || random.nextDouble() < 0.1 || stalker.getAggressionMeter() >= AGGRESSION_ATTACK_THRESHOLD) {
            attackPlayer();
            stalker.setAggressionMeter(0);
        } else {
            stalker.setCurrentState(StalkerState.STALKING);
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
        if (stalker.getTargetPlayer() != null) {
            Vec3d randomOffset = new Vec3d(
                    random.nextDouble() - 0.5,
                    0,
                    random.nextDouble() - 0.5
            ).multiply(10);
            Vec3d targetPos = stalker.getTargetPlayer().getPos().add(randomOffset);
            stalker.getNavigation().startMovingTo(targetPos.x, targetPos.y, targetPos.z, 1.0);
        }
    }

    private void moveTowardsPlayer() {
        if (stalker.getTargetPlayer() != null) {
            stalker.getNavigation().startMovingTo(stalker.getTargetPlayer().getX(), stalker.getTargetPlayer().getY(), stalker.getTargetPlayer().getZ(), 1.0);
        }
    }

    private void attackPlayer() {
        if (stalker.getTargetPlayer() != null) {
            stalker.tryAttack(stalker.getTargetPlayer());
            attackSound();
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
                return;
            }
        }
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

    public void updateAggressionMeter(int amount) {
        stalker.setTicksSinceLastAggressionUpdate(stalker.getTicksSinceLastAggressionUpdate() + 1);

        if (stalker.getTicksSinceLastAggressionUpdate() >= AGGRESSION_UPDATE_INTERVAL) {
            stalker.setAggressionMeter(stalker.getAggressionMeter() + 50);
            stalker.setTicksSinceLastAggressionUpdate(0);
        }

        if (stalker.getAggressionMeter() >= 100) {
            spooooookySound();
        }

        if (stalkerBeingObserved()) {
            stalker.setAggressionMeter(stalker.getAggressionMeter() - 1);
        }

        if (amount > 0) {
            stalker.setAggressionMeter(stalker.getAggressionMeter() + amount);
        }

        long timeOfDay = stalker.getWorld().getTimeOfDay() % 24000;
        if (timeOfDay == 13000) {
            stalker.setAggressionMeter(stalker.getAggressionMeter() + 25);
        } else if (timeOfDay == 0) {
            stalker.setAggressionMeter(stalker.getAggressionMeter() - 25);
        }

        stalker.setAggressionMeter(Math.max(0, Math.min(100, stalker.getAggressionMeter())));
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
                            state: %s | coords: (%.2f, %.2f, %.2f) | distance: %.2f | last seen: %d ticks | aggression: %d/100
                            """,
                            stalker.getCurrentState(),
                            stalker.getX(), stalker.getY(), stalker.getZ(),
                            Math.sqrt(stalker.squaredDistanceTo(stalker.getTargetPlayer())),
                            stalker.getTimeSinceSeenTicks(),
                            stalker.getAggressionMeter()
                    );
                    player.sendMessage(net.minecraft.text.Text.literal(debugInfo), true);
                }
            });
        }
    }
}