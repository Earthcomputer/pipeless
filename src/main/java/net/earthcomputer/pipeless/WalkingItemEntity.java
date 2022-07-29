package net.earthcomputer.pipeless;

import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WalkingItemEntity extends ItemEntity {
    public static final int FOLLOW_DISTANCE = 32;

    @Nullable
    private LivingEntity target;
    @Nullable
    private final NavigationMob navigator;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYRot;
    private double lerpXRot;
    private int lerpSteps;

    public WalkingItemEntity(EntityType<? extends WalkingItemEntity> entityType, Level level) {
        super(entityType, level);
        this.navigator = level.isClientSide ? null : new NavigationMob(level);
    }

    public WalkingItemEntity(ItemEntity item) {
        super(Pipeless.WALKING_ITEM_ENTITY.get(), item.level);
        if (item instanceof WalkingItemEntity walkingItem) {
            this.target = walkingItem.target;
        }
        this.navigator = level.isClientSide ? null : new NavigationMob(level);
        this.deserializeNBT(item.serializeNBT());
    }

    private void setTarget(@Nullable LivingEntity target) {
        this.target = target;
        if (this.navigator != null) {
            this.navigator.target = target;
        }
    }

    public static boolean isHoldingTemptItem(LivingEntity entity) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (entity.getItemInHand(hand).is(PipelessTags.Items.WALKING_ITEM_TEMPT)) {
                return true;
            }
        }
        return false;
    }

    public static void onTemptingEntityTick(LivingEntity entity) {
        if (entity.tickCount % 3 != 0 || !WalkingItemEntity.isHoldingTemptItem(entity)) {
            return;
        }

        List<ItemEntity> items = entity.level.getEntitiesOfClass(
            ItemEntity.class,
            new AABB(entity.position().subtract(FOLLOW_DISTANCE, FOLLOW_DISTANCE, FOLLOW_DISTANCE),
                entity.position().add(FOLLOW_DISTANCE, FOLLOW_DISTANCE, FOLLOW_DISTANCE)),
            ent -> !(ent instanceof WalkingItemEntity) && entity.distanceToSqr(ent) <= FOLLOW_DISTANCE * FOLLOW_DISTANCE
        );

        for (ItemEntity item : items) {
            item.discard();
            WalkingItemEntity newEntity = new WalkingItemEntity(item);
            newEntity.setTarget(entity);
            entity.level.addFreshEntity(newEntity);
        }
    }

    @Override
    public void tick() {
        // check if target is still valid and turn into a normal item if not
        if (!this.level.isClientSide && (
                this.target == null
                || this.target.isRemoved()
                || this.target.level != this.level
                || this.distanceToSqr(this.target) > FOLLOW_DISTANCE * FOLLOW_DISTANCE
                || !isHoldingTemptItem(this.target)
            )
        ) {
            this.discard();
            ItemEntity newEntity = new ItemEntity(EntityType.ITEM, this.level);
            newEntity.deserializeNBT(this.serializeNBT());
            this.level.addFreshEntity(newEntity);
            return;
        }


        // navigator tick, for pathfinding
        if (this.navigator != null) {
            this.navigator.setPos(this.position());
            this.navigator.setYRot(this.getYRot());
            this.navigator.setXRot(this.getXRot());
            this.navigator.setYHeadRot(this.getYRot());
            this.navigator.setOnGround(this.isOnGround());
            this.navigator.setDeltaMovement(this.getDeltaMovement());
            this.navigator.tick();
            this.setPos(this.navigator.position());
            this.setYRot(this.navigator.getYHeadRot());
            this.setXRot(this.navigator.getXRot());
            this.setYHeadRot(this.navigator.getYHeadRot());
            this.setOnGround(this.navigator.isOnGround());
            this.setDeltaMovement(this.navigator.getDeltaMovement());
        }

        super.tick();

        // lerping for the client, since it doesn't do pathfinding
        if (this.level.isClientSide && this.lerpSteps > 0) {
            double newX = this.getX() + (this.lerpX - this.getX()) / (double)this.lerpSteps;
            double newY = this.getY() + (this.lerpY - this.getY()) / (double)this.lerpSteps;
            double newZ = this.getZ() + (this.lerpZ - this.getZ()) / (double)this.lerpSteps;
            double deltaYRot = Mth.wrapDegrees(this.lerpYRot - (double)this.getYRot());
            this.setYRot(this.getYRot() + (float)deltaYRot / this.lerpSteps);
            this.setXRot(this.getXRot() + (float)(this.lerpXRot - this.getXRot()) / this.lerpSteps);
            this.lerpSteps--;
            this.setPos(newX, newY, newZ);
            this.setRot(this.getYRot(), this.getXRot());
        }
    }

    @Override
    public void lerpTo(double pX, double pY, double pZ, float pYRot, float pXRot, int pLerpSteps, boolean pTeleport) {
        this.lerpX = pX;
        this.lerpY = pY;
        this.lerpZ = pZ;
        this.lerpYRot = pYRot;
        this.lerpXRot = pXRot;
        this.lerpSteps = pLerpSteps;
    }

    @Override
    public WalkingItemEntity copy() {
        return new WalkingItemEntity(this);
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.ALL;
    }

    @Override
    public float getSpin(float pPartialTicks) {
        return -(float) Math.toRadians(getViewYRot(pPartialTicks));
    }

    private static class NavigationMob extends PathfinderMob {
        @Nullable
        private LivingEntity target;

        protected NavigationMob(Level pLevel) {
            super(EntityType.PIG, pLevel);
        }

        @Override
        protected void registerGoals() {
            this.goalSelector.addGoal(0, new TemptGoal(this));
        }

        @Override
        public EntityDimensions getDimensions(Pose pPose) {
            return Pipeless.WALKING_ITEM_ENTITY.get().getDimensions();
        }

        @Override
        public boolean hurt(DamageSource pSource, float pAmount) {
            // noop
            return false;
        }
    }

    private static class TemptGoal extends Goal {
        private final NavigationMob mob;

        private TemptGoal(NavigationMob mob) {
            this.mob = mob;
        }

        @Override
        public boolean canUse() {
            return this.mob.target != null;
        }

        @Override
        public void tick() {
            assert this.mob.target != null;
            this.mob.getLookControl().setLookAt(this.mob.target, this.mob.getMaxHeadYRot() + 20, this.mob.getMaxHeadXRot());
            if (this.mob.distanceToSqr(this.mob.target) < 2.5 * 2.5) {
                this.mob.getNavigation().stop();
            } else {
                this.mob.getNavigation().moveTo(this.mob.target, 1.1);
            }
        }
    }
}
