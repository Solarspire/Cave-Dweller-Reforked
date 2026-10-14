package de.cadentem.cave_dweller.entities;

import de.cadentem.cave_dweller.config.ServerConfig;
import de.cadentem.cave_dweller.entities.goals.CaveDwellerBreakDoorGoal;
import de.cadentem.cave_dweller.entities.goals.CaveDwellerBreakInvisGoal;
import de.cadentem.cave_dweller.entities.goals.CaveDwellerChaseGoal;
import de.cadentem.cave_dweller.entities.goals.CaveDwellerFleeGoal;
import de.cadentem.cave_dweller.entities.goals.CaveDwellerStareGoal;
import de.cadentem.cave_dweller.entities.goals.CaveDwellerStrollGoal;
import de.cadentem.cave_dweller.entities.goals.CaveDwellerTargetSeesMeGoal;
import de.cadentem.cave_dweller.entities.goals.CaveDwellerTargetTooCloseGoal;
import de.cadentem.cave_dweller.entities.goals.Roll;
import de.cadentem.cave_dweller.registry.ModSounds;
import de.cadentem.cave_dweller.util.Utils;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.object.LoopType;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;

public class CaveDwellerEntity
extends Monster
implements GeoEntity {
    public static final com.geckolib.constant.dataticket.DataTicket<RawAnimation> ANIMATION =
            com.geckolib.constant.dataticket.DataTicket.create("cave_dweller_animation", new com.google.common.reflect.TypeToken<RawAnimation>() {});
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache((GeoAnimatable)this);
    private final RawAnimation CHASE = RawAnimation.begin().then("animation.cave_dweller.new_run", LoopType.LOOP);
    private final RawAnimation CHASE_IDLE = RawAnimation.begin().then("animation.cave_dweller.run_idle", LoopType.LOOP);
    private final RawAnimation CROUCH_RUN = RawAnimation.begin().then("animation.cave_dweller.crouch_run_new", LoopType.LOOP);
    private final RawAnimation CROUCH_IDLE = RawAnimation.begin().then("animation.cave_dweller.crouch_idle", LoopType.LOOP);
    private final RawAnimation CALM_RUN = RawAnimation.begin().then("animation.cave_dweller.calm_move", LoopType.LOOP);
    private final RawAnimation CALM_STILL = RawAnimation.begin().then("animation.cave_dweller.calm_idle", LoopType.LOOP);
    private final RawAnimation IS_SPOTTED = RawAnimation.begin().then("animation.cave_dweller.spotted", LoopType.HOLD_ON_LAST_FRAME);
    private final RawAnimation CRAWL = RawAnimation.begin().then("animation.cave_dweller.crawl", LoopType.LOOP);
    private final RawAnimation FLEE = RawAnimation.begin().then("animation.cave_dweller.flee", LoopType.LOOP);
    public static final EntityDataAccessor<Boolean> FLEEING_ACCESSOR = SynchedEntityData.defineId(CaveDwellerEntity.class, (EntityDataSerializer)EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> CROUCHING_ACCESSOR = SynchedEntityData.defineId(CaveDwellerEntity.class, (EntityDataSerializer)EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> CRAWLING_ACCESSOR = SynchedEntityData.defineId(CaveDwellerEntity.class, (EntityDataSerializer)EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> SPOTTED_ACCESSOR = SynchedEntityData.defineId(CaveDwellerEntity.class, (EntityDataSerializer)EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> CLIMBING_ACCESSOR = SynchedEntityData.defineId(CaveDwellerEntity.class, (EntityDataSerializer)EntityDataSerializers.BOOLEAN);
    public Roll currentRoll = Roll.STROLL;
    public boolean isFleeing;
    public boolean hasSpawned;
    public boolean pleaseStopMoving;
    public boolean targetIsFacingMe;
    private int ticksTillRemove;
    private int chaseSoundClock;
    private boolean alreadyPlayedFleeSound;
    private static final byte START_STARE_SOUND = 100;
    private static final byte STOP_STARE_SOUND = 101;
    // Client entrypoints install playback; dedicated servers keep the no-op handler.
    public static BiConsumer<CaveDwellerEntity, Boolean> clientStareAudio = (entity, playing) -> {};
    private boolean stareSoundPlaying;
    private boolean startedPlayingChaseSound;
    private boolean alreadyPlayedDeathSound;

    public CaveDwellerEntity(EntityType<? extends CaveDwellerEntity> entityType, Level level) {
        super(entityType, level);
        this.refreshDimensions();
        this.ticksTillRemove = Utils.secondsToTicks((Integer)ServerConfig.TIME_UNTIL_LEAVE.get());
        this.setPathfindingMalus(PathType.UNPASSABLE_RAIL, 0.0f);
    }

    @Nullable
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor level, @NotNull DifficultyInstance difficulty, @NotNull EntitySpawnReason reason, @Nullable SpawnGroupData spawnData) {
        this.setAttribute(this.getAttribute(Attributes.MAX_HEALTH), (Double)ServerConfig.MAX_HEALTH.get());
        this.setAttribute(this.getAttribute(Attributes.ATTACK_DAMAGE), (Double)ServerConfig.ATTACK_DAMAGE.get());
        this.setAttribute(this.getAttribute(Attributes.ATTACK_SPEED), (Double)ServerConfig.ATTACK_SPEED.get());
        this.setAttribute(this.getAttribute(Attributes.MOVEMENT_SPEED), (Double)ServerConfig.MOVEMENT_SPEED.get());
        this.setAttribute(this.getAttribute(Attributes.STEP_HEIGHT), 1.0);
        this.setAttribute(this.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY), ServerConfig.DEPTH_STRIDER_BONUS.get() / 3.0);
        return super.finalizeSpawn(level, difficulty, reason, spawnData);
    }

    private void setAttribute(AttributeInstance attribute, double value) {
        if (attribute != null) {
            attribute.setBaseValue(value);
            if (attribute.getAttribute() == Attributes.MAX_HEALTH) {
                this.setHealth((float)value);
            } else if (attribute.getAttribute() == Attributes.MOVEMENT_SPEED) {
                this.setSpeed((float)value);
            }
        }
    }

    public static AttributeSupplier getAttributeBuilder() {
        double maxHealth = 60.0;
        double attackDamage = 6.0;
        double attackSpeed = 0.35;
        double movementSpeed = 0.3;
        double followRange = 100.0;
        return CaveDwellerEntity.createMobAttributes().add(Attributes.MAX_HEALTH, maxHealth).add(Attributes.ATTACK_DAMAGE, attackDamage).add(Attributes.ATTACK_SPEED, attackSpeed).add(Attributes.MOVEMENT_SPEED, movementSpeed).add(Attributes.FOLLOW_RANGE, followRange).add(Attributes.STEP_HEIGHT, 1.0).add(Attributes.WATER_MOVEMENT_EFFICIENCY, 0.5).build();
    }

    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLEEING_ACCESSOR, false);
        builder.define(CROUCHING_ACCESSOR, false);
        builder.define(CRAWLING_ACCESSOR, false);
        builder.define(SPOTTED_ACCESSOR, false);
        builder.define(CLIMBING_ACCESSOR, false);
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(1, (Goal)new CaveDwellerChaseGoal(this, true));
        this.goalSelector.addGoal(1, (Goal)new CaveDwellerFleeGoal(this, 20.0f, 1.0));
        this.goalSelector.addGoal(2, (Goal)new CaveDwellerBreakInvisGoal(this));
        this.goalSelector.addGoal(2, (Goal)new CaveDwellerStareGoal(this));
        if (((Boolean)ServerConfig.CAN_BREAK_DOOR.get()).booleanValue()) {
            this.goalSelector.addGoal(2, (Goal)new CaveDwellerBreakDoorGoal((Mob)this, difficulty -> true));
        }
        this.goalSelector.addGoal(3, (Goal)new CaveDwellerStrollGoal(this, 0.35));
        this.targetSelector.addGoal(1, (Goal)new CaveDwellerTargetTooCloseGoal(this, 12.0f));
        this.targetSelector.addGoal(2, (Goal)new CaveDwellerTargetSeesMeGoal(this));
    }

    public void disappear() {
        this.stopStareSound();
        this.playDisappearSound();
        this.discard();
    }

    public boolean hasSpawned() {
        return this.hasSpawned;
    }

    protected boolean canRide(@NotNull Entity vehicle) {
        if (((Boolean)ServerConfig.ALLOW_RIDING.get()).booleanValue()) {
            return super.canRide(vehicle);
        }
        return false;
    }

    public boolean startRiding(@NotNull Entity vehicle, boolean force, boolean sendEvent) {
        if (((Boolean)ServerConfig.ALLOW_RIDING.get()).booleanValue()) {
            return super.startRiding(vehicle, force, sendEvent);
        }
        return false;
    }

    public void tick() {
        if (!level().isClientSide() && --this.ticksTillRemove <= 0) {
            this.disappear();
        }
        if (this.goalSelector.getAvailableGoals().isEmpty() || this.targetSelector.getAvailableGoals().isEmpty()) {
            this.registerGoals();
            this.goalSelector.tick();
            this.targetSelector.tick();
        }
        if (this.getTarget() != null) {
            this.targetIsFacingMe = this.isLookingAtMe((Entity)this.getTarget(), false);
        }
        if (this.level() instanceof ServerLevel) {
            boolean shouldCrawl;
            boolean isAboveSolid = this.level().getBlockState(this.blockPosition().above()).isSolid();
            boolean isTwoAboveSolid = this.level().getBlockState(this.blockPosition().above(2)).isSolid();
            boolean isThreeAboveSolid = this.level().getBlockState(this.blockPosition().above(3)).isSolid();
            Vec3i offset = this.getDirectionVector();
            boolean isFacingSolid = this.level().getBlockState(this.blockPosition().relative(this.getDirection())).isSolid();
            if (isFacingSolid) {
                offset = offset.offset(0, 1, 0);
            }
            boolean isOffsetFacingSolid = this.level().getBlockState(this.blockPosition().offset(offset)).isSolid();
            boolean isOffsetFacingAboveSolid = this.level().getBlockState(this.blockPosition().offset(offset).above()).isSolid();
            boolean isOffsetFacingTwoAboveSolid = this.level().getBlockState(this.blockPosition().offset(offset).above(2)).isSolid();
            boolean shouldCrouch = isTwoAboveSolid || !isOffsetFacingSolid && !isOffsetFacingAboveSolid && (isOffsetFacingTwoAboveSolid || isFacingSolid && isThreeAboveSolid);
            boolean bl = shouldCrawl = isAboveSolid || !isOffsetFacingSolid && isOffsetFacingAboveSolid || isFacingSolid && isTwoAboveSolid;
            if (this.isAggressive() || this.isFleeing) {
                this.entityData.set(SPOTTED_ACCESSOR, false);
            }
            this.setClimbing(this.horizontalCollision);
            this.entityData.set(CROUCHING_ACCESSOR, shouldCrouch);
            this.setCrawling(shouldCrawl);
        }
        this.refreshDimensions();
        this.getNavigation().setSpeedModifier(this.getSpeedModifier());
        super.tick();
    }

    public double getSpeedModifier() {
        return this.isCrawling() ? 0.35 : (this.isCrouching() ? 0.6 : 0.85);
    }

    @NotNull
    public EntityDimensions getDefaultDimensions(@NotNull Pose pose) {
        if (((Boolean)this.entityData.get(CRAWLING_ACCESSOR)).booleanValue()) {
            return EntityDimensions.fixed(0.5f, 0.5f);
        }
        if (((Boolean)this.entityData.get(CROUCHING_ACCESSOR)).booleanValue()) {
            return EntityDimensions.fixed(0.5f, 1.7f);
        }
        return super.getDefaultDimensions(pose);
    }

    private boolean isMoving() {
        Vec3 velocity = this.getDeltaMovement();
        float avgVelocity = (float)(Math.abs(velocity.x) + Math.abs(velocity.z)) / 2.0f;
        return avgVelocity > 0.03f;
    }

    public void reRoll() {
        this.currentRoll = Roll.fromValue(new Random().nextInt(3));
    }

    public void pickRoll(@NotNull List<Roll> rolls) {
        this.currentRoll = rolls.get(new Random().nextInt(rolls.size()));
    }

    public boolean onClimbable() {
        return this.isClimbing();
    }

    public boolean isClimbing() {
        if (!((Boolean)ServerConfig.CAN_CLIMB.get()).booleanValue()) {
            return false;
        }
        if (this.getTarget() != null) {
            return !this.isCrawling() && !this.isCrouching() && (Boolean)this.entityData.get(CLIMBING_ACCESSOR) != false;
        }
        return false;
    }

    public void setClimbing(boolean isClimbing) {
        this.entityData.set(CLIMBING_ACCESSOR, isClimbing);
    }

    @NotNull
    protected PathNavigation createNavigation(@NotNull Level level) {
        CaveDwellerNavigation navigation = new CaveDwellerNavigation(this, level);
        navigation.setMaxVisitedNodesMultiplier(4.0f);
        navigation.setCanOpenDoors(true);
        navigation.getNodeEvaluator().setCanPassDoors(true);
        return navigation;
    }

    public RawAnimation selectAnimation(boolean moving) {
        boolean isCurrentAboveSolid = this.level().getBlockState(this.blockPosition().above()).isSolid();
        boolean unsure = this.isCrawling() && this.level().getBlockState(this.blockPosition()).isSolid();
        boolean isCurrentTwoAboveSolid = this.level().getBlockState(this.blockPosition().above(2)).isSolid();
        if (isCurrentAboveSolid || unsure) {
            return this.CRAWL;
        }
        if (isCurrentTwoAboveSolid) {
            if (moving) {
                return this.CROUCH_RUN;
            }
            return this.CROUCH_IDLE;
        }
        if (this.isAggressive()) {
            if (moving) {
                return this.CHASE;
            }
            return this.CHASE_IDLE;
        }
        if (((Boolean)this.entityData.get(FLEEING_ACCESSOR)).booleanValue()) {
            if (moving) {
                return this.FLEE;
            }
            return this.CHASE_IDLE;
        }
        if (this.pleaseStopMoving || ((Boolean)this.entityData.get(SPOTTED_ACCESSOR)).booleanValue() && !moving) {
            return this.IS_SPOTTED;
        }
        if (moving) {
            return this.CALM_RUN;
        }
        return this.CALM_STILL;
    }

    private PlayState predicate(AnimationTest<CaveDwellerEntity> state) {
        return state.setAndContinue(state.getDataOrDefault(ANIMATION, this.CALM_STILL));
    }

    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>("controller", 3, this::predicate));
    }

    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    protected void playStepSound(@NotNull BlockPos pPos, @NotNull BlockState pState) {
        super.playStepSound(pPos, pState);
        this.playEntitySound(this.chooseStep());
    }

    private void playEntitySound(SoundEvent soundEvent) {
        this.playEntitySound(soundEvent, 1.0f, 1.0f);
    }

    private void playEntitySound(SoundEvent soundEvent, float volume, float pitch) {
        this.level().playSound(null, (Entity)this, soundEvent, SoundSource.HOSTILE, volume, pitch);
    }

    private void playBlockPosSound(Identifier soundResource, float volume, float pitch) {
        Level level = this.level();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            int radius = 60;
            serverLevel.getPlayers(player -> player.distanceToSqr((Entity)this) <= (double)(radius * radius)).forEach(player -> de.cadentem.cave_dweller.CaveDweller.playPrivateSound(player, soundResource, volume, pitch));
        }
    }

    public void playChaseSound() {
        this.stopStareSound();
        if (this.startedPlayingChaseSound || this.isMoving()) {
            if (this.chaseSoundClock <= 0) {
                Random rand = new Random();
                switch (rand.nextInt(4)) {
                    case 0: {
                        this.playEntitySound((SoundEvent)ModSounds.CHASE_1.get(), 3.0f, 1.0f);
                        break;
                    }
                    case 1: {
                        this.playEntitySound((SoundEvent)ModSounds.CHASE_2.get(), 3.0f, 1.0f);
                        break;
                    }
                    case 2: {
                        this.playEntitySound((SoundEvent)ModSounds.CHASE_3.get(), 3.0f, 1.0f);
                        break;
                    }
                    case 3: {
                        this.playEntitySound((SoundEvent)ModSounds.CHASE_4.get(), 3.0f, 1.0f);
                    }
                }
                this.startedPlayingChaseSound = true;
                this.resetChaseSoundClock();
            }
            --this.chaseSoundClock;
        }
    }

    public void playDisappearSound() {
        this.playBlockPosSound(((SoundEvent)ModSounds.DISAPPEAR.get()).location(), 3.0f, 1.0f);
    }

    public void playFleeSound() {
        this.stopStareSound();
        if (!this.alreadyPlayedFleeSound) {
            Random rand = new Random();
            switch (rand.nextInt(2)) {
                case 0: {
                    this.playEntitySound((SoundEvent)ModSounds.FLEE_1.get(), 3.0f, 1.0f);
                    break;
                }
                case 1: {
                    this.playEntitySound((SoundEvent)ModSounds.FLEE_2.get(), 3.0f, 1.0f);
                }
            }
            this.alreadyPlayedFleeSound = true;
        }
    }

    public void playStareSound() {
        if (this.level() instanceof ServerLevel server && !this.stareSoundPlaying) {
            this.stareSoundPlaying = true;
            server.broadcastEntityEvent(this, START_STARE_SOUND);
        }
    }

    public void stopStareSound() {
        if (!this.stareSoundPlaying) return;
        this.stareSoundPlaying = false;
        if (this.level() instanceof ServerLevel server) {
            server.broadcastEntityEvent(this, STOP_STARE_SOUND);
        } else {
            clientStareAudio.accept(this, false);
        }
    }

    public boolean isStareSoundPlaying() {
        return this.stareSoundPlaying && this.isAlive() && !this.isRemoved();
    }

    @Override
    public void handleEntityEvent(byte event) {
        if (event == START_STARE_SOUND || event == STOP_STARE_SOUND) {
            this.stareSoundPlaying = event == START_STARE_SOUND;
            clientStareAudio.accept(this, this.stareSoundPlaying);
        } else {
            super.handleEntityEvent(event);
        }
    }

    @Override
    public void setAggressive(boolean aggressive) {
        // This stop packet precedes any chase sound packet on the same connection.
        if (aggressive) this.stopStareSound();
        super.setAggressive(aggressive);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        this.stopStareSound();
        super.remove(reason);
    }

    private void resetChaseSoundClock() {
        this.chaseSoundClock = Utils.secondsToTicks(5);
    }

    private SoundEvent chooseStep() {
        Random rand = new Random();
        return switch (rand.nextInt(4)) {
            case 1 -> (SoundEvent)ModSounds.CHASE_STEP_2.get();
            case 2 -> (SoundEvent)ModSounds.CHASE_STEP_3.get();
            case 3 -> (SoundEvent)ModSounds.CHASE_STEP_4.get();
            default -> (SoundEvent)ModSounds.CHASE_STEP_1.get();
        };
    }

    private SoundEvent chooseHurtSound() {
        Random rand = new Random();
        return switch (rand.nextInt(4)) {
            case 1 -> (SoundEvent)ModSounds.DWELLER_HURT_2.get();
            case 2 -> (SoundEvent)ModSounds.DWELLER_HURT_3.get();
            case 3 -> (SoundEvent)ModSounds.DWELLER_HURT_4.get();
            default -> (SoundEvent)ModSounds.DWELLER_HURT_1.get();
        };
    }

    protected void playHurtSound(@NotNull DamageSource pSource) {
        SoundEvent soundevent = this.chooseHurtSound();
        this.playEntitySound(soundevent, 2.0f, 1.0f);
    }

    public void setCrawling(boolean shouldCrawl) {
        if (shouldCrawl) {
            this.getEntityData().set(CROUCHING_ACCESSOR, false);
        }
        this.getEntityData().set(CRAWLING_ACCESSOR, shouldCrawl);
        this.refreshDimensions();
    }

    public boolean isCrawling() {
        return (Boolean)this.entityData.get(CRAWLING_ACCESSOR);
    }

    protected void tickDeath() {
        this.stopStareSound();
        super.tickDeath();
        if (!this.alreadyPlayedDeathSound) {
            this.playBlockPosSound(((SoundEvent)ModSounds.DWELLER_DEATH.get()).location(), 2.0f, 1.0f);
            this.alreadyPlayedDeathSound = true;
        }
    }

    public boolean isLookingAtMe(Entity target, boolean directlyLooking) {
        if (!Utils.isValidPlayer(target)) {
            return false;
        }
        if (target.getEyePosition(1.0f).distanceTo(this.getPosition(1.0f)) > (double)((Integer)ServerConfig.SPOTTING_RANGE.get()).intValue()) {
            return false;
        }
        Vec3 viewVector = target.getViewVector(1.0f).normalize();
        Vec3 difference = new Vec3(this.getX() - target.getX(), this.getEyeY() - target.getEyeY(), this.getZ() - target.getZ());
        difference = difference.normalize();
        double dot = viewVector.dot(difference);
        if (directlyLooking && target instanceof Player) {
            Player player = (Player)target;
            return dot > 0.99 && player.hasLineOfSight((Entity)this);
        }
        return dot > 0.3;
    }

    public boolean teleportToTarget() {
        LivingEntity target = this.getTarget();
        if (target == null) {
            return false;
        }
        Vec3 targetPosition = new Vec3(this.getX() - target.getX(), this.getY(0.5) - target.getEyeY(), this.getZ() - target.getZ());
        targetPosition = targetPosition.normalize();
        double radius = 16.0;
        double d1 = this.getX() + (this.getRandom().nextDouble() - 0.5) * (radius / 2.0) - targetPosition.x * radius;
        double d2 = this.getY() + ((double)this.getRandom().nextInt((int)radius) - radius / 2.0) - targetPosition.y * radius;
        double d3 = this.getZ() + (this.getRandom().nextDouble() - 0.5) * (radius / 2.0) - targetPosition.z * radius;
        BlockPos.MutableBlockPos validPosition = new BlockPos.MutableBlockPos(d1, d2, d3);
        while (validPosition.getY() > this.level().getMinY()) {
            BlockState ground = this.level().getBlockState(validPosition);
            if (ground.isSolid() && !ground.is(Blocks.COBWEB) && !ground.is(Blocks.BAMBOO_SAPLING)) break;
            validPosition.move(Direction.DOWN);
        }
        this.teleportTo(validPosition.getX(), validPosition.getY(), validPosition.getZ());
        return true;
    }

    private Vec3i getDirectionVector() {
        return new Vec3i(this.getDirection().getStepX(), this.getDirection().getStepY(), this.getDirection().getStepZ());
    }

    protected SoundEvent getHurtSound(@NotNull DamageSource damageSourceIn) {
        return this.chooseHurtSound();
    }

    protected SoundEvent getDeathSound() {
        return (SoundEvent)ModSounds.DWELLER_DEATH.get();
    }

    protected float getSoundVolume() {
        return 0.4f;
    }

    private int obstructedHits;

    @Override
    public void travel(Vec3 direction) {
        if (!pleaseStopMoving) super.travel(direction);
    }

    @Override
    public void knockback(double strength, double x, double z, DamageSource source, float damage, boolean flag) {
        if (!isClimbing()) super.knockback(strength, x, z, source, damage, flag);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean obstructed = source.is(net.minecraft.world.damagesource.DamageTypes.DROWN)
                || source.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD);
        if (obstructed || source.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL)
                || source.is(net.minecraft.world.damagesource.DamageTypes.FALL)) {
            if (obstructed && !level().isClientSide() && ++obstructedHits > 5) {
                obstructedHits = 0;
                if (!teleportToTarget()) {
                    String key = level().dimension().identifier().toString();
                    de.cadentem.cave_dweller.CaveDweller.speedUpTimers(key,
                            (int)(ServerConfig.CAN_SPAWN_MIN.get() * 0.3),
                            (int)(ServerConfig.RESET_NOISE_MIN.get() * 0.3));
                    disappear();
                }
                this.markHurt();
            }
            return false;
        }
        return super.hurtServer(level, source, amount);
    }
}
