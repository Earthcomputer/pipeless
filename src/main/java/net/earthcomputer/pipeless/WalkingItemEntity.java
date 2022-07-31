package net.earthcomputer.pipeless;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
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

    private float limbSwingAmount;
    private float limbSwingSpeed;
    private float prevLimbSwingAmount;
    private float prevLimbSwingSpeed;

    private static final int FADE_TIME = 20;
    private static final EntityDataAccessor<Byte> STATE_DATA = SynchedEntityData.defineId(WalkingItemEntity.class, EntityDataSerializers.BYTE);
    private int fadeTicks = FADE_TIME;

    public WalkingItemEntity(EntityType<? extends WalkingItemEntity> entityType, Level level) {
        super(entityType, level);
        this.navigator = level.isClientSide ? null : new NavigationMob(level);
    }

    public WalkingItemEntity(ItemEntity item) {
        super(Pipeless.WALKING_ITEM_ENTITY.get(), item.level);
        this.navigator = level.isClientSide ? null : new NavigationMob(level);
        this.deserializeNBT(item.serializeNBT());
        setRot((float) Math.toDegrees(-item.getSpin(1)), item.getXRot());
        this.bobOffs = item.bobOffs;

        if (item instanceof WalkingItemEntity walkingItem) {
            this.target = walkingItem.target;
        } else {
            this.setState(State.FADE_IN);
            this.fadeTicks = FADE_TIME;
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.getEntityData().define(STATE_DATA, (byte) State.FADE_IN.ordinal());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        super.onSyncedDataUpdated(pKey);
        if (pKey == STATE_DATA) {
            if (getState() == State.NORMAL) {
                this.fadeTicks = 0;
            }
        }
    }

    private State getState() {
        byte index = this.getEntityData().get(STATE_DATA);
        if (index >= 0 && index < State.VALUES.length) {
            return State.VALUES[index];
        } else {
            return State.FADE_IN;
        }
    }

    private void setState(State state) {
        this.getEntityData().set(STATE_DATA, (byte) state.ordinal());
    }

    private void setTarget(@Nullable LivingEntity target) {
        this.target = target;
        if (this.navigator != null) {
            this.navigator.target = target;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putByte("State", this.getEntityData().get(STATE_DATA));
        pCompound.putInt("FadeTicks", this.fadeTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.getEntityData().set(STATE_DATA, pCompound.getByte("State"));
        this.fadeTicks = pCompound.getInt("FadeTicks");
    }

    public static boolean isHoldingTemptItem(LivingEntity entity) {
        return entity.isHolding(stack -> stack.is(PipelessTags.Items.WALKING_ITEM_TEMPT));
    }

    public static void onTemptingEntityTick(LivingEntity entity) {
        if (entity.tickCount % 3 != 0 || !WalkingItemEntity.isHoldingTemptItem(entity)) {
            return;
        }

        List<ItemEntity> items = entity.level.getEntitiesOfClass(
            ItemEntity.class,
            new AABB(entity.position().subtract(FOLLOW_DISTANCE, FOLLOW_DISTANCE, FOLLOW_DISTANCE),
                entity.position().add(FOLLOW_DISTANCE, FOLLOW_DISTANCE, FOLLOW_DISTANCE)),
            ent -> {
                if (ent instanceof WalkingItemEntity walkingItem) {
                    if (walkingItem.isFadingOut() || walkingItem.target == null) {
                        return true;
                    } else if (walkingItem.target == entity) {
                        return false;
                    } else {
                        double distanceToThisSqr = walkingItem.distanceToSqr(entity);
                        return distanceToThisSqr <= FOLLOW_DISTANCE * FOLLOW_DISTANCE
                            && walkingItem.distanceToSqr(walkingItem.target) > distanceToThisSqr;
                    }
                }
                return entity.distanceToSqr(ent) <= FOLLOW_DISTANCE * FOLLOW_DISTANCE
                    && !entity.getPersistentData().getBoolean("PreventRemoteMovement");
            }
        );

        for (ItemEntity item : items) {
            if (item instanceof WalkingItemEntity walkingItem) {
                if (walkingItem.isFadingOut()) {
                    walkingItem.setFadingIn();
                }
                walkingItem.target = entity;
            } else {
                item.discard();
                WalkingItemEntity newEntity = new WalkingItemEntity(item);
                newEntity.setTarget(entity);
                entity.level.addFreshEntity(newEntity);
            }
        }
    }

    @Override
    public void tick() {
        if (this.isFadingOut()) {
            if (!this.level.isClientSide && this.fadeTicks >= FADE_TIME) {
                this.discard();
                ItemEntity newEntity = new ItemEntity(EntityType.ITEM, this.level);
                newEntity.deserializeNBT(this.serializeNBT());
                newEntity.bobOffs = this.getTargetBobOffset();
                this.level.addFreshEntity(newEntity);
            } else {
                this.fadeTicks++;
                super.tick();
            }
            return;
        }

        // check if target is still valid and turn into a normal item if not
        if (!this.level.isClientSide && (
                this.target == null
                || this.target.isRemoved()
                || this.target.level != this.level
                || this.distanceToSqr(this.target) > FOLLOW_DISTANCE * FOLLOW_DISTANCE
                || !isHoldingTemptItem(this.target)
            )
        ) {
            if (this.getState() == State.NORMAL) {
                this.fadeTicks = 0;
            }
            this.setState(State.FADE_OUT);
            super.tick();
            return;
        }

        if (this.getState() == State.FADE_IN) {
            if (!this.level.isClientSide && this.fadeTicks <= 0) {
                this.setState(State.NORMAL);
            }
            this.fadeTicks--;
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

        // animation
        if (this.level.isClientSide) {
            final boolean isFlying = false;
            this.prevLimbSwingAmount = this.limbSwingAmount;
            this.prevLimbSwingSpeed = this.limbSwingSpeed;
            double dx = this.getX() - this.xo;
            double dy = isFlying ? this.getY() - this.yo : 0;
            double dz = this.getZ() - this.zo;
            float targetSwingSpeed = (float) Math.sqrt(dx * dx + dy * dy + dz * dz) * 4;
            if (targetSwingSpeed > 1) {
                targetSwingSpeed = 1;
            }
            this.limbSwingSpeed += (targetSwingSpeed - this.limbSwingSpeed) * 0.4;
            this.limbSwingAmount += this.limbSwingSpeed;
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

    public float getLimbSwingAmount(float partialTicks) {
        return this.prevLimbSwingAmount + (this.limbSwingAmount - this.prevLimbSwingAmount) * partialTicks;
    }

    public float getLimbSwingSpeed(float partialTicks) {
        return this.prevLimbSwingSpeed + (this.limbSwingSpeed - this.prevLimbSwingSpeed) * partialTicks;
    }

    /**
     * Gets the bob offset that this item is about to fade into, if applicable
     */
    public float getTargetBobOffset() {
        if (this.getState() == State.FADE_OUT) {
            // spin = (age + 1) / 20 + bobOffs
            // rearrange:
            // bobOffs = spin - (age + 1) / 20
            return this.getSpin(1) - (this.getAge() + 1) / 20f;
        } else {
            return this.bobOffs;
        }
    }

    public float getFadeBlend(float partialTicks) {
        if (this.getState() == State.NORMAL) {
            return 1;
        }

        float progress = this.getState() == State.FADE_IN ? this.fadeTicks - partialTicks : this.fadeTicks + partialTicks;
        return (float) ((Math.cos(progress * (Math.PI / FADE_TIME)) + 1) * 0.5);
    }

    public boolean isFadingOut() {
        return this.getState() == State.FADE_OUT;
    }

    public void setFadingIn() {
        this.setState(State.FADE_IN);
    }

    public enum State {
        FADE_IN,
        NORMAL,
        FADE_OUT,
        ;

        private static final State[] VALUES = values();
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
