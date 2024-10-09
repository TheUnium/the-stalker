package me.unium.stalker.entity;

import net.minecraft.entity.ai.goal.Goal;

public class AdvancedStalkPlayerGoal extends Goal {
    private final StalkerEntity stalker;
    private static final double STALKING_RANGE = 50.0D;

    public AdvancedStalkPlayerGoal(StalkerEntity stalker) {
        this.stalker = stalker;
    }

    @Override
    public boolean canStart() {
        return stalker.getTargetPlayer() != null;
    }

    @Override
    public boolean shouldContinue() {
        return stalker.getTargetPlayer() != null && stalker.getTargetPlayer().isAlive() &&
                stalker.squaredDistanceTo(stalker.getTargetPlayer()) <= STALKING_RANGE * STALKING_RANGE;
    }

    @Override
    public void tick() {
        if (stalker.getCurrentState() == StalkerState.STALKING || stalker.getCurrentState() == StalkerState.ATTACKING) {
            stalker.getLookControl().lookAt(stalker.getTargetPlayer(), 100.0F, 100.0F);

            if (stalker.getCurrentState() == StalkerState.STALKING) {
                stalker.getBehavior().moveAroundPlayerRandomly();
            }
        }
    }
}