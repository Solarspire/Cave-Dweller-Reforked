package fade.cave_dweller.entities;

import fade.cave_dweller.config.ServerConfig;
import fade.cave_dweller.entities.goals.CaveDwellerBreakDoorGoal;
import fade.cave_dweller.entities.goals.CaveDwellerBreakInvisGoal;
import fade.cave_dweller.entities.goals.CaveDwellerChaseGoal;
import fade.cave_dweller.entities.goals.CaveDwellerFleeGoal;
import fade.cave_dweller.entities.goals.CaveDwellerStareGoal;
import fade.cave_dweller.entities.goals.CaveDwellerStrollGoal;
import fade.cave_dweller.entities.goals.CaveDwellerTargetSeesMeGoal;
import fade.cave_dweller.entities.goals.CaveDwellerTargetTooCloseGoal;
import fade.cave_dweller.entities.goals.Roll;
import fade.cave_dweller.network.CaveSoundPacket;
import fade.cave_dweller.network.NetworkHandler;
import fade.cave_dweller.registry.ModSounds;
import fade.cave_dweller.util.Utils;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

public class CaveDwellerEntity extends EntityMob implements IAnimatable {
    private static final int[][] PRIMARY_APPROACH_OFFSETS = new int[][] {
        {0, 0},
        {1, 0},
        {-1, 0},
        {0, 1},
        {0, -1},
        {1, 1},
        {1, -1},
        {-1, 1},
        {-1, -1}
    };
    private static final int[][] EXTENDED_APPROACH_OFFSETS = new int[][] {
        {2, 0},
        {-2, 0},
        {0, 2},
        {0, -2}
    };
    private static final int[] DESTINATION_VERTICAL_OFFSETS = new int[] {0, 1, -1, 2, -2};

    public enum PathSearchMode {
        UPRIGHT(0, 2.7D),
        CROUCH(24, 1.7D),
        CRAWL(512, 0.5D);

        private final int penalty;
        private final double pathHeight;

        PathSearchMode(int penalty, double pathHeight) {
            this.penalty = penalty;
            this.pathHeight = pathHeight;
        }

        public int getPenalty() {
            return this.penalty;
        }

        public double getPathHeight() {
            return this.pathHeight;
        }
    }

    public static final class ChasePathPlan {
        private final Path path;
        private final PathSearchMode mode;
        private final BlockPos destination;
        private final int score;

        private ChasePathPlan(Path path, PathSearchMode mode, BlockPos destination, int score) {
            this.path = path;
            this.mode = mode;
            this.destination = destination;
            this.score = score;
        }

        public Path getPath() {
            return this.path;
        }

        public PathSearchMode getMode() {
            return this.mode;
        }

        public BlockPos getDestination() {
            return this.destination;
        }

        public int getScore() {
            return this.score;
        }
    }

    public static final DataParameter<Boolean> FLEEING_ACCESSOR =
        EntityDataManager.createKey(CaveDwellerEntity.class, DataSerializers.BOOLEAN);
    public static final DataParameter<Boolean> CROUCHING_ACCESSOR =
        EntityDataManager.createKey(CaveDwellerEntity.class, DataSerializers.BOOLEAN);
    public static final DataParameter<Boolean> CRAWLING_ACCESSOR =
        EntityDataManager.createKey(CaveDwellerEntity.class, DataSerializers.BOOLEAN);
    public static final DataParameter<Boolean> SPOTTED_ACCESSOR =
        EntityDataManager.createKey(CaveDwellerEntity.class, DataSerializers.BOOLEAN);
    public static final DataParameter<Boolean> CLIMBING_ACCESSOR =
        EntityDataManager.createKey(CaveDwellerEntity.class, DataSerializers.BOOLEAN);
    public static final DataParameter<Boolean> AGGRESSIVE_ACCESSOR =
        EntityDataManager.createKey(CaveDwellerEntity.class, DataSerializers.BOOLEAN);

    private final AnimationFactory factory = new AnimationFactory(this);
    public Roll currentRoll = Roll.STROLL;
    public boolean isFleeing;
    public boolean hasSpawned;
    public boolean pleaseStopMoving;
    public boolean targetIsFacingMe;
    private int ticksTillRemove = Utils.secondsToTicks(ServerConfig.timeUntilLeave);
    private int chaseSoundClock;
    private boolean alreadyPlayedFleeSound;
    private boolean alreadyPlayedSpottedSound;
    private boolean startedPlayingChaseSound;
    private boolean alreadyPlayedDeathSound;
    private boolean climbHandoffActive;

    public CaveDwellerEntity(World world) {
        super(world);
        this.setSize(0.5F, 2.7F);
        this.stepHeight = 1.0F;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(FLEEING_ACCESSOR, false);
        this.dataManager.register(CROUCHING_ACCESSOR, false);
        this.dataManager.register(CRAWLING_ACCESSOR, false);
        this.dataManager.register(SPOTTED_ACCESSOR, false);
        this.dataManager.register(CLIMBING_ACCESSOR, false);
        this.dataManager.register(AGGRESSIVE_ACCESSOR, false);
    }

    @Override
    protected void initEntityAI() {
        this.ensureGoalEntries();
    }

    private void ensureGoalEntries() {
        if (!this.hasTask(this.tasks, CaveDwellerChaseGoal.class)) {
            this.tasks.addTask(1, new CaveDwellerChaseGoal(this, true));
        }
        if (!this.hasTask(this.tasks, CaveDwellerFleeGoal.class)) {
            this.tasks.addTask(1, new CaveDwellerFleeGoal(this, 20.0F, 1.0D));
        }
        if (!this.hasTask(this.tasks, CaveDwellerBreakInvisGoal.class)) {
            this.tasks.addTask(2, new CaveDwellerBreakInvisGoal(this));
        }
        if (!this.hasTask(this.tasks, CaveDwellerStareGoal.class)) {
            this.tasks.addTask(2, new CaveDwellerStareGoal(this));
        }
        if (ServerConfig.canBreakDoor && !this.hasTask(this.tasks, CaveDwellerBreakDoorGoal.class)) {
            this.tasks.addTask(2, new CaveDwellerBreakDoorGoal(this, difficulty -> true));
        }
        if (!this.hasTask(this.tasks, CaveDwellerStrollGoal.class)) {
            this.tasks.addTask(3, new CaveDwellerStrollGoal(this, 0.35D));
        }

        if (!this.hasTask(this.targetTasks, CaveDwellerTargetTooCloseGoal.class)) {
            this.targetTasks.addTask(1, new CaveDwellerTargetTooCloseGoal(this, 12.0F));
        }
        if (!this.hasTask(this.targetTasks, CaveDwellerTargetSeesMeGoal.class)) {
            this.targetTasks.addTask(2, new CaveDwellerTargetSeesMeGoal(this));
        }
    }

    private boolean hasTask(EntityAITasks tasks, Class<? extends EntityAIBase> taskClass) {
        for (EntityAITasks.EntityAITaskEntry entry : tasks.taskEntries) {
            if (taskClass.isInstance(entry.action)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(ServerConfig.maximumHealth);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(ServerConfig.attackDamage);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(ServerConfig.movementSpeed);
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(100.0D);
    }

    @Override
    public void onLivingUpdate() {
        this.ticksTillRemove--;
        if (this.ticksTillRemove <= 0) {
            this.disappear();
            return;
        }

        if (this.tasks.taskEntries.isEmpty() || this.targetTasks.taskEntries.isEmpty()) {
            this.ensureGoalEntries();
        }

        if (this.getAttackTarget() != null) {
            this.targetIsFacingMe = this.isLookingAtMe(this.getAttackTarget(), false);
        }

        if (!this.world.isRemote) {
            BlockPos pos = this.getPosition();
            boolean isAboveSolid = this.isCollisionSolid(pos.up());
            boolean isTwoAboveSolid = this.isCollisionSolid(pos.up(2));
            boolean isThreeAboveSolid = this.isCollisionSolid(pos.up(3));

            Vec3i offset = this.getDirectionVector();
            boolean isFacingSolid = this.isCollisionSolid(pos.offset(this.getHorizontalFacing()));
            if (isFacingSolid) {
                offset = new Vec3i(offset.getX(), offset.getY() + 1, offset.getZ());
            }

            BlockPos offsetPos = pos.add(offset.getX(), offset.getY(), offset.getZ());
            boolean isOffsetFacingSolid = this.isCollisionSolid(offsetPos);
            boolean isOffsetFacingAboveSolid = this.isCollisionSolid(offsetPos.up());
            boolean isOffsetFacingTwoAboveSolid = this.isCollisionSolid(offsetPos.up(2));

            boolean shouldCrouch =
                isTwoAboveSolid
                    || (!isOffsetFacingSolid && !isOffsetFacingAboveSolid && (isOffsetFacingTwoAboveSolid || (isFacingSolid && isThreeAboveSolid)));
            boolean shouldCrawl = isAboveSolid || (!isOffsetFacingSolid && isOffsetFacingAboveSolid) || (isFacingSolid && isTwoAboveSolid);
            EntityLivingBase target = this.getAttackTarget();
            boolean hasClimbSurface = this.hasClimbSurface();
            BlockPos climbLedgePos = this.findCurrentClimbLedgePos(target);
            boolean shouldClimb = (this.collidedHorizontally && hasClimbSurface) || this.shouldFinishClimbHandoff(hasClimbSurface, climbLedgePos);

            if (shouldClimb) {
                // Prevent crawl/crouch state thrash while climbing walls.
                shouldCrouch = false;
                shouldCrawl = false;
            }

            if (this.isAggressive() || this.isFleeing) {
                this.setSpotted(false);
            }

            this.setClimbing(shouldClimb);
            this.setCrouching(shouldCrouch);
            this.setCrawling(shouldCrawl);
            this.climbHandoffActive = shouldClimb && !this.isStandingOnSolidSurface() && (hasClimbSurface || climbLedgePos != null);
            this.stabilizeClimbingToCenter();
        }

        if (this.isSpotted()) {
            this.playSpottedSound();
        }

        this.updateEntitySize();
        this.getNavigator().setSpeed(this.getSpeedModifier());

        super.onLivingUpdate();
    }

    @Override
    public boolean startRiding(Entity entityIn, boolean force) {
        if (!ServerConfig.allowRiding) {
            return false;
        }
        return super.startRiding(entityIn, force);
    }

    @Override
    public void travel(float strafe, float vertical, float forward) {
        if (this.pleaseStopMoving) {
            return;
        }
        if (this.isInWater() && ServerConfig.depthStriderBonus > 0.0D) {
            this.motionX *= 1.0D + ServerConfig.depthStriderBonus * 0.01D;
            this.motionZ *= 1.0D + ServerConfig.depthStriderBonus * 0.01D;
        }
        super.travel(strafe, vertical, forward);
    }

    @Override
    public boolean isOnLadder() {
        return this.isClimbing();
    }

    public boolean isClimbing() {
        if (!ServerConfig.canClimb) {
            return false;
        }
        if (this.getAttackTarget() == null) {
            return false;
        }
        return !this.isCrawling() && this.dataManager.get(CLIMBING_ACCESSOR);
    }

    public void setClimbing(boolean climbing) {
        if (this.dataManager.get(CLIMBING_ACCESSOR) != climbing) {
            this.dataManager.set(CLIMBING_ACCESSOR, climbing);
        }
    }

    @Override
    protected PathNavigate createNavigator(World worldIn) {
        CaveDwellerNavigation navigation = new CaveDwellerNavigation(this, worldIn);
        navigation.setCanSwim(true);
        return navigation;
    }

    private boolean isMoving() {
        float avgVelocity = (float) (Math.abs(this.motionX) + Math.abs(this.motionZ)) / 2.0F;
        return avgVelocity > 0.03F;
    }

    public void reRoll() {
        this.currentRoll = Roll.fromValue(new Random().nextInt(3));
    }

    public void pickRoll(List<Roll> rolls) {
        if (rolls == null || rolls.isEmpty()) {
            this.currentRoll = Roll.STROLL;
            return;
        }
        this.currentRoll = rolls.get(new Random().nextInt(rolls.size()));
    }

    @Nullable
    public ChasePathPlan buildPathPlanToTarget(@Nullable EntityLivingBase target) {
        if (target == null) {
            return null;
        }

        ChasePathPlan bestNonCrawl = this.chooseBetterPlan(
            this.buildBestPathPlan(target, PathSearchMode.UPRIGHT),
            this.buildBestPathPlan(target, PathSearchMode.CROUCH)
        );
        if (bestNonCrawl != null) {
            return bestNonCrawl;
        }

        return this.buildBestPathPlan(target, PathSearchMode.CRAWL);
    }

    @Nullable
    public Path buildPathToTarget(@Nullable EntityLivingBase target) {
        ChasePathPlan plan = this.buildPathPlanToTarget(target);
        return plan == null ? null : plan.getPath();
    }

    public double getSpeedModifier() {
        if (this.isCrawling()) {
            return 0.35D;
        }
        if (this.isAggressive()) {
            return 0.85D;
        }
        return 0.85D;
    }

    public void disappear() {
        this.playDisappearSound();
        this.setDead();
    }

    public void playChaseSound() {
        if (this.startedPlayingChaseSound || this.isMoving()) {
            if (this.chaseSoundClock <= 0) {
                int choice = this.rand.nextInt(4);
                if (choice == 0) {
                    this.playEntitySound(ModSounds.CHASE_1, 3.0F, 1.0F);
                } else if (choice == 1) {
                    this.playEntitySound(ModSounds.CHASE_2, 3.0F, 1.0F);
                } else if (choice == 2) {
                    this.playEntitySound(ModSounds.CHASE_3, 3.0F, 1.0F);
                } else {
                    this.playEntitySound(ModSounds.CHASE_4, 3.0F, 1.0F);
                }

                this.startedPlayingChaseSound = true;
                this.resetChaseSoundClock();
            }

            this.chaseSoundClock--;
        }
    }

    public void playDisappearSound() {
        this.playBlockPosSound(getResource(ModSounds.DISAPPEAR), 3.0F, 1.0F);
    }

    public void playFleeSound() {
        if (!this.alreadyPlayedFleeSound) {
            int choice = this.rand.nextInt(2);
            if (choice == 0) {
                this.playEntitySound(ModSounds.FLEE_1, 3.0F, 1.0F);
            } else {
                this.playEntitySound(ModSounds.FLEE_2, 3.0F, 1.0F);
            }
            this.alreadyPlayedFleeSound = true;
        }
    }

    private void playSpottedSound() {
        if (!this.alreadyPlayedSpottedSound) {
            this.playEntitySound(ModSounds.SPOTTED, 3.0F, 1.0F);
            this.alreadyPlayedSpottedSound = true;
        }
    }

    private void resetChaseSoundClock() {
        this.chaseSoundClock = Utils.secondsToTicks(5);
    }

    @Override
    protected void playStepSound(BlockPos pos, Block blockIn) {
        super.playStepSound(pos, blockIn);
        this.playEntitySound(this.chooseStep(), 1.0F, 1.0F);
    }

    private void playEntitySound(@Nullable SoundEvent soundEvent, float volume, float pitch) {
        if (soundEvent == null) {
            return;
        }
        this.world.playSound(
            null,
            this.posX,
            this.posY,
            this.posZ,
            soundEvent,
            SoundCategory.HOSTILE,
            volume,
            pitch
        );
    }

    private void playBlockPosSound(@Nullable ResourceLocation soundResource, float volume, float pitch) {
        if (soundResource == null || this.world.isRemote) {
            return;
        }
        int radius = 60;
        SimpleNetworkWrapper channel = NetworkHandler.CHANNEL;
        for (EntityPlayer player : this.world.playerEntities) {
            if (!(player instanceof EntityPlayerMP)) {
                continue;
            }
            if (player.getDistanceSq(this) <= (double) (radius * radius)) {
                channel.sendTo(new CaveSoundPacket(soundResource, player.getPosition(), volume, pitch), (EntityPlayerMP) player);
            }
        }
    }

    private SoundEvent chooseStep() {
        int choice = this.rand.nextInt(4);
        if (choice == 1) {
            return ModSounds.CHASE_STEP_2;
        } else if (choice == 2) {
            return ModSounds.CHASE_STEP_3;
        } else if (choice == 3) {
            return ModSounds.CHASE_STEP_4;
        }
        return ModSounds.CHASE_STEP_1;
    }

    private SoundEvent chooseHurtSound() {
        int choice = this.rand.nextInt(4);
        if (choice == 1) {
            return ModSounds.DWELLER_HURT_2;
        } else if (choice == 2) {
            return ModSounds.DWELLER_HURT_3;
        } else if (choice == 3) {
            return ModSounds.DWELLER_HURT_4;
        }
        return ModSounds.DWELLER_HURT_1;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return this.chooseHurtSound();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.DWELLER_DEATH;
    }

    @Override
    public void onDeath(DamageSource cause) {
        super.onDeath(cause);
        if (!this.alreadyPlayedDeathSound) {
            this.playBlockPosSound(getResource(ModSounds.DWELLER_DEATH), 2.0F, 1.0F);
            this.alreadyPlayedDeathSound = true;
        }
    }

    public boolean isLookingAtMe(Entity target, boolean directlyLooking) {
        if (!Utils.isValidPlayer(target)) {
            return false;
        }
        if (target.getPositionEyes(1.0F).squareDistanceTo(this.getPositionEyes(1.0F)) > ServerConfig.spottingRange * ServerConfig.spottingRange) {
            return false;
        }

        Vec3d viewVector = target.getLook(1.0F).normalize();
        Vec3d difference = new Vec3d(this.posX - target.posX, this.posY - target.posY, this.posZ - target.posZ).normalize();
        double dot = viewVector.dotProduct(difference);

        if (directlyLooking && target instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) target;
            return dot > 0.99D && player.canEntityBeSeen(this);
        }
        return dot > 0.3D;
    }

    public boolean teleportToTarget() {
        EntityLivingBase target = this.getAttackTarget();
        if (target == null) {
            return false;
        }

        Vec3d targetPosition = new Vec3d(this.posX - target.posX, this.posY + 0.5D - target.posY, this.posZ - target.posZ).normalize();
        double radius = 16.0D;
        double d1 = this.posX + (this.rand.nextDouble() - 0.5D) * (radius / 2.0D) - targetPosition.x * radius;
        double d2 = this.posY + (this.rand.nextInt((int) radius) - radius / 2.0D) - targetPosition.y * radius;
        double d3 = this.posZ + (this.rand.nextDouble() - 0.5D) * (radius / 2.0D) - targetPosition.z * radius;

        BlockPos.MutableBlockPos validPosition =
            new BlockPos.MutableBlockPos(MathHelper.floor(d1), MathHelper.floor(d2), MathHelper.floor(d3));
        while (validPosition.getY() > 0 && !this.world.getBlockState(validPosition).getMaterial().blocksMovement()) {
            validPosition.move(EnumFacing.DOWN);
        }

        this.setPositionAndUpdate(validPosition.getX(), validPosition.getY(), validPosition.getZ());
        return true;
    }

    private Vec3i getDirectionVector() {
        EnumFacing facing = this.getHorizontalFacing();
        return new Vec3i(facing.getXOffset(), facing.getYOffset(), facing.getZOffset());
    }

    private boolean isCollisionSolid(BlockPos pos) {
        IBlockState state = this.world.getBlockState(pos);
        AxisAlignedBB collision = state.getCollisionBoundingBox(this.world, pos);
        return state.getMaterial().blocksMovement() && collision != Block.NULL_AABB;
    }

    private boolean shouldFinishClimbHandoff(boolean hasClimbSurface, @Nullable BlockPos climbLedgePos) {
        if (!ServerConfig.canClimb || !this.climbHandoffActive || this.isStandingOnSolidSurface()) {
            return false;
        }

        return hasClimbSurface || climbLedgePos != null;
    }

    private boolean isStandingOnSolidSurface() {
        return this.onGround && this.isCollisionSolid(this.getPosition().down());
    }

    private boolean hasClimbSurface() {
        BlockPos pos = this.getPosition();
        for (EnumFacing facing : EnumFacing.HORIZONTALS) {
            BlockPos front = pos.offset(facing);
            if (this.isCollisionSolid(front) || this.isCollisionSolid(front.up())) {
                return true;
            }
        }
        return false;
    }

    private void stabilizeClimbingToCenter() {
        if (!this.isClimbing()) {
            return;
        }

        BlockPos ledgePos = this.getClimbLedgePos();
        double desiredX = MathHelper.floor(this.posX) + 0.5D;
        double desiredZ = MathHelper.floor(this.posZ) + 0.5D;
        if (ledgePos != null) {
            desiredX = ledgePos.getX() + 0.5D;
            desiredZ = ledgePos.getZ() + 0.5D;
        }

        double centeredX = this.posX + MathHelper.clamp(desiredX - this.posX, -0.18D, 0.18D);
        double centeredZ = this.posZ + MathHelper.clamp(desiredZ - this.posZ, -0.18D, 0.18D);
        // Keep the dweller attached to the climb path, but let it shift onto a valid ledge block at the top.
        this.setPosition(centeredX, this.posY, centeredZ);
        this.motionX = 0.0D;
        this.motionZ = 0.0D;

        if (this.shouldKeepClimbingUpward()) {
            double climbSpeed = ledgePos != null ? 0.16D : 0.22D;
            this.motionY = Math.max(this.motionY, climbSpeed);
        } else if (this.motionY < 0.0D) {
            this.motionY = 0.0D;
        }
        this.fallDistance = 0.0F;
    }

    boolean shouldKeepClimbingUpward() {
        if (!this.isClimbing() || this.isStandingOnSolidSurface()) {
            return false;
        }

        BlockPos ledgePos = this.getClimbLedgePos();
        if (ledgePos != null) {
            return true;
        }

        return this.hasClimbSurface() && !this.hasBlockedClimbHeadroom();
    }

    @Nullable
    BlockPos getClimbLedgePos() {
        if (!this.isClimbing()) {
            return null;
        }

        return this.findCurrentClimbLedgePos(this.getAttackTarget());
    }

    @Nullable
    private BlockPos findCurrentClimbLedgePos(@Nullable EntityLivingBase target) {
        BlockPos pos = this.getPosition();
        EnumFacing preferred = this.getPreferredClimbFacing(target);
        BlockPos ledgePos = this.findClimbLedgePos(pos, preferred);
        if (ledgePos != null) {
            return ledgePos;
        }

        for (EnumFacing facing : EnumFacing.HORIZONTALS) {
            if (facing == preferred) {
                continue;
            }

            ledgePos = this.findClimbLedgePos(pos, facing);
            if (ledgePos != null) {
                return ledgePos;
            }
        }

        return null;
    }

    private BlockPos findClimbLedgePos(BlockPos pos, EnumFacing facing) {
        BlockPos front = pos.offset(facing);
        BlockPos ledgePos = this.findStandableClimbPos(front.up());
        if (ledgePos != null) {
            return ledgePos;
        }

        if (this.isCollisionSolid(front.up())) {
            return this.findStandableClimbPos(front.up(2));
        }

        return null;
    }

    private BlockPos findStandableClimbPos(BlockPos pos) {
        return this.canStandAt(pos) ? pos : null;
    }

    private boolean canStandAt(BlockPos pos) {
        return this.canOccupyAt(pos, PathSearchMode.UPRIGHT.getPathHeight());
    }

    private boolean hasBlockedClimbHeadroom() {
        if (!this.isClimbing()) {
            return false;
        }

        int headY = MathHelper.floor(this.getEntityBoundingBox().maxY + 0.05D);
        BlockPos headPos = new BlockPos(MathHelper.floor(this.posX), headY, MathHelper.floor(this.posZ));
        return this.isCollisionSolid(headPos);
    }

    public boolean shouldAbortBlockedClimb() {
        return this.isClimbing() && this.hasBlockedClimbHeadroom() && this.getClimbLedgePos() == null;
    }

    private EnumFacing getPreferredClimbFacing(@Nullable EntityLivingBase target) {
        if (target == null) {
            return this.getHorizontalFacing();
        }

        double dx = target.posX - this.posX;
        double dz = target.posZ - this.posZ;
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx >= 0.0D ? EnumFacing.EAST : EnumFacing.WEST;
        }

        if (Math.abs(dz) > 0.0D) {
            return dz >= 0.0D ? EnumFacing.SOUTH : EnumFacing.NORTH;
        }

        return this.getHorizontalFacing();
    }

    void applyPathSearchMode(PathSearchMode mode) {
        switch (mode) {
            case UPRIGHT:
                this.setCrawling(false);
                this.setCrouching(false);
                break;
            case CROUCH:
                this.setCrawling(false);
                this.setCrouching(true);
                break;
            case CRAWL:
                this.setCrawling(true);
                break;
            default:
                break;
        }
    }

    void restorePathSearchPose(boolean wasCrouching, boolean wasCrawling) {
        this.setCrawling(false);
        this.setCrouching(false);
        if (wasCrawling) {
            this.setCrawling(true);
        } else if (wasCrouching) {
            this.setCrouching(true);
        }
    }

    private boolean canOccupyAt(BlockPos pos, double height) {
        if (!this.isCollisionSolid(pos.down())) {
            return false;
        }

        double halfWidth = this.width / 2.0D;
        AxisAlignedBB box = new AxisAlignedBB(
            pos.getX() + 0.5D - halfWidth,
            pos.getY(),
            pos.getZ() + 0.5D - halfWidth,
            pos.getX() + 0.5D + halfWidth,
            pos.getY() + height,
            pos.getZ() + 0.5D + halfWidth
        );
        return this.world.getCollisionBoxes(this, box).isEmpty();
    }

    @Nullable
    private ChasePathPlan buildBestPathPlan(EntityLivingBase target, PathSearchMode mode) {
        if (!(this.getNavigator() instanceof CaveDwellerNavigation)) {
            return null;
        }

        CaveDwellerNavigation navigation = (CaveDwellerNavigation) this.getNavigator();
        BlockPos targetPos = target.getPosition();
        Path directPath = navigation.findPathToEntityLiving(target, mode);
        ChasePathPlan best = this.createPathPlan(directPath, mode, this.getPathDestination(directPath, targetPos), targetPos);

        for (BlockPos candidate : this.collectTargetApproachPositions(target, mode, false)) {
            Path path = navigation.findPathToPos(candidate, mode);
            best = this.chooseBetterPlan(best, this.createPathPlan(path, mode, candidate, targetPos));
        }

        if (best != null) {
            return best;
        }

        for (BlockPos candidate : this.collectTargetApproachPositions(target, mode, true)) {
            Path path = navigation.findPathToPos(candidate, mode);
            best = this.chooseBetterPlan(best, this.createPathPlan(path, mode, candidate, targetPos));
        }

        return best;
    }

    private ChasePathPlan chooseBetterPlan(@Nullable ChasePathPlan current, @Nullable ChasePathPlan candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null || candidate.getScore() < current.getScore()) {
            return candidate;
        }
        return current;
    }

    @Nullable
    private ChasePathPlan createPathPlan(@Nullable Path path, PathSearchMode mode, BlockPos destination, BlockPos targetPos) {
        if (path == null || path.getCurrentPathLength() <= 0) {
            return null;
        }

        PathPoint finalPoint = path.getFinalPathPoint();
        if (finalPoint == null) {
            return null;
        }

        if (this.requiresClimb(path, destination) && !this.canUseClimbDestination(destination, mode)) {
            return null;
        }

        PathPoint targetPoint = new PathPoint(targetPos.getX(), targetPos.getY(), targetPos.getZ());
        PathPoint destinationPoint = new PathPoint(destination.getX(), destination.getY(), destination.getZ());
        int score =
            mode.getPenalty()
                + path.getCurrentPathLength() * 8
                + Math.round(finalPoint.distanceToSquared(targetPoint) * 8.0F)
                + Math.round(destinationPoint.distanceToSquared(targetPoint) * 6.0F)
                + Math.abs(destination.getY() - targetPos.getY()) * 10;
        return new ChasePathPlan(path, mode, destination, score);
    }

    private List<BlockPos> collectTargetApproachPositions(EntityLivingBase target, PathSearchMode mode, boolean extendedSearch) {
        BlockPos targetPos = target.getPosition();
        Set<BlockPos> candidates = new LinkedHashSet<BlockPos>();
        this.addPathDestinationCandidate(candidates, targetPos, mode);

        int[][] offsets = extendedSearch ? EXTENDED_APPROACH_OFFSETS : PRIMARY_APPROACH_OFFSETS;
        for (int[] offset : offsets) {
            this.addPathDestinationCandidate(candidates, targetPos.add(offset[0], 0, offset[1]), mode);
        }

        if (candidates.isEmpty()) {
            candidates.add(targetPos);
        }

        return new ArrayList<BlockPos>(candidates);
    }

    private void addPathDestinationCandidate(Set<BlockPos> candidates, BlockPos pos, PathSearchMode mode) {
        BlockPos candidate = this.findPathDestinationCandidate(pos, mode);
        if (candidate != null) {
            candidates.add(candidate);
        }
    }

    @Nullable
    private BlockPos findPathDestinationCandidate(BlockPos pos, PathSearchMode mode) {
        BlockPos best = null;
        int bestScore = Integer.MAX_VALUE;

        for (int yOffset : DESTINATION_VERTICAL_OFFSETS) {
            BlockPos candidate = this.adjustPathDestinationCandidate(pos.add(0, yOffset, 0), mode);
            if (candidate == null) {
                continue;
            }

            int score = Math.abs(candidate.getY() - pos.getY()) * 4 + Math.abs(yOffset);
            if (best == null || score < bestScore) {
                best = candidate;
                bestScore = score;
            }
        }

        return best;
    }

    @Nullable
    private BlockPos adjustPathDestinationCandidate(BlockPos pos, PathSearchMode mode) {
        if (this.canOccupyAt(pos, mode.getPathHeight())) {
            return pos;
        }

        BlockPos adjusted = this.findOpenPositionAboveGround(pos, mode, 4);
        if (adjusted != null) {
            return adjusted;
        }

        return this.findOpenPositionAboveObstacle(pos, mode, 4);
    }

    @Nullable
    private BlockPos findOpenPositionAboveGround(BlockPos pos, PathSearchMode mode, int searchDepth) {
        BlockPos cursor = pos;
        for (int i = 0; i < searchDepth; i++) {
            if (this.isCollisionSolid(cursor.down()) && this.canOccupyAt(cursor, mode.getPathHeight())) {
                return cursor;
            }

            if (cursor.getY() <= 1 || this.isCollisionSolid(cursor)) {
                break;
            }

            cursor = cursor.down();
        }

        if (this.isCollisionSolid(cursor) && this.canOccupyAt(cursor.up(), mode.getPathHeight())) {
            return cursor.up();
        }

        return null;
    }

    @Nullable
    private BlockPos findOpenPositionAboveObstacle(BlockPos pos, PathSearchMode mode, int searchHeight) {
        BlockPos cursor = pos;
        for (int i = 0; i < searchHeight; i++) {
            if (!this.isCollisionSolid(cursor) && this.canOccupyAt(cursor, mode.getPathHeight())) {
                return cursor;
            }

            cursor = cursor.up();
        }

        return null;
    }

    private boolean requiresClimb(Path path, BlockPos destination) {
        return path.getCurrentPathLength() == 1 && destination.getY() > this.getPosition().getY() + this.stepHeight;
    }

    private boolean canUseClimbDestination(BlockPos destination, PathSearchMode mode) {
        return this.canOccupyAt(destination, mode.getPathHeight()) && !this.isCollisionSolid(destination.up());
    }

    private BlockPos getPathDestination(@Nullable Path path, BlockPos fallback) {
        if (path == null) {
            return fallback;
        }

        PathPoint finalPoint = path.getFinalPathPoint();
        return finalPoint == null ? fallback : new BlockPos(finalPoint.x, finalPoint.y, finalPoint.z);
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    public void setCrawling(boolean shouldCrawl) {
        boolean changed = false;
        boolean wasCrouching = this.dataManager.get(CROUCHING_ACCESSOR);
        boolean wasCrawling = this.dataManager.get(CRAWLING_ACCESSOR);
        if (shouldCrawl) {
            if (wasCrouching) {
                this.dataManager.set(CROUCHING_ACCESSOR, false);
                changed = true;
            }
        }
        if (wasCrawling != shouldCrawl) {
            this.dataManager.set(CRAWLING_ACCESSOR, shouldCrawl);
            changed = true;
        }
        if (changed) {
            this.updateEntitySize();
        }
    }

    public boolean isCrawling() {
        return this.dataManager.get(CRAWLING_ACCESSOR);
    }

    public void setCrouching(boolean crouching) {
        boolean changed = false;
        boolean wasCrouching = this.dataManager.get(CROUCHING_ACCESSOR);
        boolean wasCrawling = this.dataManager.get(CRAWLING_ACCESSOR);
        if (wasCrouching != crouching) {
            this.dataManager.set(CROUCHING_ACCESSOR, crouching);
            changed = true;
        }
        if (crouching) {
            if (wasCrawling) {
                this.dataManager.set(CRAWLING_ACCESSOR, false);
                changed = true;
            }
        }
        if (changed) {
            this.updateEntitySize();
        }
    }

    public boolean isCrouching() {
        return this.dataManager.get(CROUCHING_ACCESSOR);
    }

    public void setFleeing(boolean fleeing) {
        if (this.dataManager.get(FLEEING_ACCESSOR) != fleeing) {
            this.dataManager.set(FLEEING_ACCESSOR, fleeing);
        }
    }

    public boolean isFleeing() {
        return this.dataManager.get(FLEEING_ACCESSOR);
    }

    public void setSpotted(boolean spotted) {
        boolean previous = this.dataManager.get(SPOTTED_ACCESSOR);
        if (previous != spotted) {
            this.dataManager.set(SPOTTED_ACCESSOR, spotted);
        }
        if (previous && !spotted) {
            this.alreadyPlayedSpottedSound = false;
        }
    }

    public boolean isSpotted() {
        return this.dataManager.get(SPOTTED_ACCESSOR);
    }

    public boolean isAggressive() {
        return this.dataManager.get(AGGRESSIVE_ACCESSOR);
    }

    public void setAggressive(boolean aggressive) {
        if (this.dataManager.get(AGGRESSIVE_ACCESSOR) != aggressive) {
            this.dataManager.set(AGGRESSIVE_ACCESSOR, aggressive);
        }
    }

    private void updateEntitySize() {
        float width = 0.5F;
        float height;
        if (this.isCrawling()) {
            height = 0.5F;
        } else if (this.isCrouching()) {
            height = 1.7F;
        } else {
            height = 2.7F;
        }

        if (this.width != width || this.height != height) {
            this.setSize(width, height);
        }
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        if (CROUCHING_ACCESSOR.equals(key) || CRAWLING_ACCESSOR.equals(key)) {
            this.updateEntitySize();
        }
    }

    @Nullable
    private static ResourceLocation getResource(@Nullable SoundEvent sound) {
        return sound == null ? null : sound.getRegistryName();
    }

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        AnimationBuilder builder = new AnimationBuilder();

        boolean isCurrentAboveSolid = this.isCollisionSolid(this.getPosition().up());
        boolean unsure = this.isCrawling() && this.isCollisionSolid(this.getPosition());
        boolean isCurrentTwoAboveSolid = this.isCollisionSolid(this.getPosition().up(2));

        if (!isCurrentAboveSolid && !unsure) {
            if (isCurrentTwoAboveSolid) {
                if (event.isMoving()) {
                    builder.addAnimation("animation.cave_dweller.crouch_run_new", true);
                } else {
                    builder.addAnimation("animation.cave_dweller.crouch_idle", true);
                }
            } else if (this.isAggressive()) {
                if (event.isMoving()) {
                    builder.addAnimation("animation.cave_dweller.new_run", true);
                } else {
                    builder.addAnimation("animation.cave_dweller.run_idle", true);
                }
            } else if (this.isFleeing()) {
                if (event.isMoving()) {
                    builder.addAnimation("animation.cave_dweller.flee", true);
                } else {
                    builder.addAnimation("animation.cave_dweller.run_idle", true);
                }
            } else if (!this.pleaseStopMoving && (!this.isSpotted() || event.isMoving())) {
                if (event.isMoving()) {
                    builder.addAnimation("animation.cave_dweller.calm_move", true);
                } else {
                    builder.addAnimation("animation.cave_dweller.calm_idle", true);
                }
            } else {
                builder.addAnimation("animation.cave_dweller.spotted", false);
            }
        } else {
            builder.addAnimation("animation.cave_dweller.crawl", true);
        }

        event.getController().setAnimation(builder);
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<CaveDwellerEntity>(this, "controller", 3, this::predicate));
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }
}
