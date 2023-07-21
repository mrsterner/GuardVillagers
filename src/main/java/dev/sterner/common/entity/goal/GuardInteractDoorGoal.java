package dev.sterner.common.entity.goal;

import dev.sterner.common.entity.GuardEntity;
import net.minecraft.entity.ai.goal.LongDoorInteractGoal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.Hand;

import java.util.List;

public class GuardInteractDoorGoal extends LongDoorInteractGoal {
    private final GuardEntity guard;

    public GuardInteractDoorGoal(GuardEntity pMob, boolean pCloseDoor) {
        super(pMob, pCloseDoor);
        this.guard = pMob;
    }

    @Override
    public boolean canStart() {
        return super.canStart();
    }

    @Override
    public void start() {
        if (areOtherMobsComingThroughDoor(guard)) {
            super.start();
            guard.swingHand(Hand.MAIN_HAND);
        }
    }

    private boolean areOtherMobsComingThroughDoor(GuardEntity pEntity) {
        List<? extends PathAwareEntity> nearbyEntityList = pEntity.getWorld().getNonSpectatingEntities(PathAwareEntity.class, pEntity.getBoundingBox().expand(4.0D));
        if (!nearbyEntityList.isEmpty()) {
            for (PathAwareEntity mob : nearbyEntityList) {
                if (mob.getBlockPos().isWithinDistance(pEntity.getPos(), 2.0D))
                    return isMobComingThroughDoor(mob);
            }
        }
        return false;
    }

    private boolean isMobComingThroughDoor(PathAwareEntity pEntity) {
        if (pEntity.getNavigation() == null) {
            return false;
        } else {
            Path path = pEntity.getNavigation().getCurrentPath();
            if (path == null || path.isFinished()) {
                return false;
            } else {
                PathNode node = path.getLastNode();
                if (node == null) {
                    return false;
                } else {
                    PathNode node1 = path.getCurrentNode();
                    return pEntity.getBlockPos().equals(node.getBlockPos()) || pEntity.getBlockPos().equals(node1.getBlockPos());
                }
            }
        }
    }
}
