package coolware.module.player;

import coolware.Coolware;
import coolware.module.Category;
import coolware.module.Module;
import coolware.module.combat.KillAura;
import coolware.module.movement.Scaffold;
import coolware.setting.BooleanSetting;
import coolware.setting.ModeSetting;
import coolware.setting.NumberSetting;
import coolware.util.*;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BedBreaker extends Module {
    public static ModeSetting mode;
    public static BooleanSetting whitelistBed;
    public static BooleanSetting preRotate;
    public static BooleanSetting tickRotate;
    public static BooleanSetting tickSwap;
    public static BooleanSetting noAutoblock;
    public static BooleanSetting noKillAura;
    public static BooleanSetting noScaffold;
    public static NumberSetting ticksBetweenBlocks;

    // ScaleHack MoveFix for BedBreaker ONLY
    public static BooleanSetting movementFix;
    public static ModeSetting moveFixMode;

    private static BlockPos currentBedTarget;
    private static BlockPos lastTarget;
    private static BlockPos currentTarget;

    private static boolean hasTickSwapped = false;
    private static int lastHotbarSlot = -1;
    private static float currentBreakProgress = 0.0F;

    private static int lastBreakTick = -1;
    private static int rotationTick = -1;

    private static boolean inBedwarsGame = false;
    private static boolean justStartedGame = false;
    private static int bestSlot = -1;

    private static BlockPos bedHead = null;
    private static BlockPos bedFoot = null;

    private static BedBreaker instance;

    public BedBreaker() {
        super("BedBreaker", "Automatically breaks beds and obstacles.", Category.PLAYER, 0);
        instance = this;

        mode = new ModeSetting("Mode", this, "Hypixel", new String[]{"Vanilla", "Hypixel"});
        whitelistBed = new BooleanSetting("Whitelist Own Bed", this, true);
        preRotate = new BooleanSetting("Post Rotate", this, false);
        tickRotate = new BooleanSetting("Tick Rotate", this, false);
        tickSwap = new BooleanSetting("Tick Swap", this, false);
        noAutoblock = new BooleanSetting("Override Autoblock", this, true);
        noKillAura = new BooleanSetting("Override Kill Aura", this, true);
        noScaffold = new BooleanSetting("Override Scaffold", this, false);
        ticksBetweenBlocks = new NumberSetting("Ticks Between Blocks", this, 5.0, 0.0, 6.0, 0.1);

        movementFix = new BooleanSetting("Movement Fix", this, true);
        moveFixMode = new ModeSetting("Move Fix Mode", this, "Straight", new String[]{"Vanilla", "Smooth", "Straight"});

        addSetting(mode);
        addSetting(whitelistBed);
        addSetting(preRotate);
        addSetting(tickRotate);
        addSetting(tickSwap);
        addSetting(noAutoblock);
        addSetting(noKillAura);
        addSetting(noScaffold);
        addSetting(ticksBetweenBlocks);
        addSetting(movementFix);
        addSetting(moveFixMode);
    }

    public static BedBreaker getInstance() {
        return instance;
    }

    public static BlockPos getLastTarget() {
        return lastTarget;
    }

    public static BlockPos getCurrentTarget() {
        return currentTarget;
    }

    public static boolean isNoKillAura() {
        return noKillAura != null && noKillAura.isEnabled();
    }

    public static boolean isNoAutoblock() {
        return noAutoblock != null && noAutoblock.isEnabled();
    }

    public static boolean isActivelyTargeting() {
        return instance != null && instance.isToggled() && currentTarget != null;
    }

    public static boolean wantsMovementFix() {
        return instance != null && instance.isToggled()
                && movementFix != null && movementFix.isEnabled()
                && currentTarget != null
                && RotationUtil.silentActive;
    }

    public static String getMoveFixMode() {
        return moveFixMode != null ? moveFixMode.getMode() : "Straight";
    }

    @Override
    public void onEnable() {
        BlockTracker.beginTracking(Blocks.bed);
    }

    @Override
    public void onDisable() {
        BlockTracker.stopTracking(Blocks.bed);
        currentBedTarget = null;
        currentTarget = null;
        lastTarget = null;
        hasTickSwapped = false;
        if (mc.thePlayer != null && lastHotbarSlot != -1) {
            mc.thePlayer.inventory.currentItem = lastHotbarSlot;
        }
        lastHotbarSlot = -1;
        bestSlot = -1;
        rotationTick = -1;
        currentBreakProgress = 0.0F;
        if (!Scaffold.wantsMoveFix() && !KillAura.isActivelyAiming()) {
            RotationUtil.resetSilentRotation();
        }
    }

    @Override
    public void onUpdate() {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        checkForStartBedwars();

        // If player is attacked, instantly switch to sword!
        if (PlayerUtil.isBeingAttacked()) {
            PlayerUtil.switchToSword();
        }

        // When being attacked during bed break, prioritize KillAura
        boolean attacked = PlayerUtil.isBeingAttacked();
        KillAura ka = Coolware.getInstance().getModuleManager().getModule(KillAura.class);
        boolean kaHasTarget = ka != null && ka.isToggled() && KillAura.target != null;
        if (attacked && kaHasTarget) {
            // Prioritize KillAura: reset current bed target so killaura has full rotation and attack control
            currentBedTarget = null;
            currentTarget = null;
            if (lastHotbarSlot != -1) {
                mc.thePlayer.inventory.currentItem = lastHotbarSlot;
                lastHotbarSlot = -1;
            }
            rotationTick = -1;
            return;
        }

        if (noScaffold != null && !noScaffold.isEnabled() && Scaffold.isShouldScaffold()) {
            return;
        }

        if (justStartedGame) {
            findOwnBed();
        }

        if (MovementUtil.ticks - lastBreakTick < (int) ticksBetweenBlocks.getValue()
                || (mc.playerController != null && mc.playerController.getCurrentGameType().isAdventure())) {
            return;
        }

        getTarget();

        if (currentTarget == null) {
            rotationTick = -1;
            if (!Scaffold.wantsMoveFix() && !KillAura.isActivelyAiming()) {
                RotationUtil.resetSilentRotation();
            }
            return;
        }

        Vec3 bestRotationSpot = WorldUtil.getClosestPointToBlock(currentTarget);
        if (bestRotationSpot == null) {
            rotationTick = -1;
            return;
        }

        if (rotationTick == -1) {
            rotationTick = MovementUtil.ticks;
        }

        if (shouldRotate()) {
            RotationUtil.Rotation rot = RotationUtil.getRotation(new Vec3(
                    currentTarget.getX() + 0.5,
                    currentTarget.getY() + 0.5,
                    currentTarget.getZ() + 0.5
            ));
            RotationUtil.setSilentRotation(rot.yaw, rot.pitch);
            mc.thePlayer.rotationYawHead = rot.yaw;
            mc.thePlayer.renderYawOffset = rot.yaw;
            FakeRotation.set(rot.yaw, rot.pitch);
        }

        // Attack bed
        attackBed();
    }

    private void attackBed() {
        EnumFacing facing = RotationUtil.getFacing().getOpposite();

        if (currentTarget == null || PlayerUtil.isUsingItem()
                || (!preRotate.isEnabled() && MovementUtil.ticks - rotationTick <= 0)) {
            return;
        }

        PlayerUtil.swingHand();

        if (currentTarget != lastTarget) {
            lastTarget = currentTarget;
            PacketUtil.sendPacket(new C07PacketPlayerDigging(
                    C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, currentTarget, facing));
            lastHotbarSlot = mc.thePlayer.inventory.currentItem;
            bestSlot = InventoryUtil.getBestSlotForBlock(currentTarget);
            mc.thePlayer.inventory.currentItem = bestSlot;
            currentBreakProgress = 0.0F;
        } else if (tickSwap.isEnabled() && !hasTickSwapped) {
            mc.thePlayer.inventory.currentItem = lastHotbarSlot;
            hasTickSwapped = true;
        }

        if (currentBreakProgress >= 1.0F) {
            PacketUtil.sendPacket(new C07PacketPlayerDigging(
                    C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, currentTarget, facing));
            mc.theWorld.setBlockState(currentTarget, Blocks.air.getDefaultState(), 3);
            if (lastHotbarSlot != -1) {
                mc.thePlayer.inventory.currentItem = lastHotbarSlot;
            }
            currentTarget = null;
            lastTarget = null;
            lastBreakTick = MovementUtil.ticks;
            rotationTick = -1;
            currentBreakProgress = 0.0F;
            bestSlot = -1;
            hasTickSwapped = false;
            return;
        }

        currentBreakProgress += InventoryUtil.blockStrength(
                mc.thePlayer.inventory.getStackInSlot(bestSlot), currentTarget);

        if (currentBreakProgress >= 1.0F && tickSwap.isEnabled()) {
            mc.thePlayer.inventory.currentItem = bestSlot;
        }
    }

    @Override
    public void onRender3D(float partialTicks) {
        if (currentTarget == null) return;

        Render3dUtil.draw3dBox(
                currentTarget.getX(),
                currentTarget.getY(),
                currentTarget.getZ(),
                1.0,
                MathHelper.clamp_float(currentBreakProgress, 0.0F, 1.0F),
                1.0,
                RenderUtil.getProgressColour(currentBreakProgress),
                partialTicks
        );
    }

    public static boolean shouldRotate() {
        if (currentTarget == null) return false;

        return !tickRotate.isEnabled() || rotationTick == MovementUtil.ticks
                || (!preRotate.isEnabled() && rotationTick >= MovementUtil.ticks - 2)
                || (bestSlot != -1 && currentBreakProgress + InventoryUtil.blockStrength(
                mc.thePlayer.inventory.getStackInSlot(bestSlot), currentTarget) >= 1.0F);
    }

    private static void checkForStartBedwars() {
        if (mc.theWorld == null) return;
        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        if (scoreboard == null) {
            inBedwarsGame = false;
            return;
        }

        net.minecraft.scoreboard.ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        if (objective == null || !objective.getDisplayName().contains("BED WARS")) {
            inBedwarsGame = false;
            return;
        }

        if (inBedwarsGame) return;

        Collection<Score> scores = scoreboard.getSortedScores(objective);
        for (Score score : scores) {
            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            String formatted = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());
            if (formatted.contains("\u2713")) {
                inBedwarsGame = true;
                justStartedGame = true;
                return;
            }
        }

        inBedwarsGame = false;
    }

    private static void findOwnBed() {
        if (mc.thePlayer == null || mc.thePlayer.capabilities.allowFlying) return;

        List<BlockPos> bedPositions = BlockTracker.getBlockPositions(Blocks.bed);

        for (BlockPos blockPos : bedPositions) {
            if (mc.thePlayer.getDistance(blockPos.getX(), blockPos.getY(), blockPos.getZ()) > 30) continue;
            IBlockState state = mc.theWorld.getBlockState(blockPos);
            if (state.getBlock() != Blocks.bed || !state.getProperties().containsKey(BlockBed.PART)
                    || !state.getProperties().containsKey(BlockBed.FACING)) continue;

            EnumFacing facing = state.getValue(BlockBed.FACING);
            boolean isFoot = state.getValue(BlockBed.PART) == BlockBed.EnumPartType.FOOT;

            bedHead = !isFoot ? blockPos.offset(facing.getOpposite()) : blockPos;
            bedFoot = isFoot ? blockPos.offset(facing) : blockPos;

            justStartedGame = false;
            return;
        }
    }

    private static boolean isValidBed(BlockPos blockPos) {
        if (whitelistBed.isEnabled() && (blockPos.equals(bedHead) || blockPos.equals(bedFoot))) {
            return false;
        }

        IBlockState state = mc.theWorld.getBlockState(blockPos);
        if (state.getBlock() != Blocks.bed) return false;
        return state.getProperties().containsKey(BlockBed.PART) && state.getProperties().containsKey(BlockBed.FACING);
    }

    private static boolean canAttackBed(BlockPos blockPos) {
        return canAttackBlock(blockPos) && WorldUtil.getBestBlockSurroundingBed(blockPos) == null;
    }

    private static boolean canAttackBlock(BlockPos blockPos) {
        if (mc.thePlayer == null) return false;
        Vec3 closestPoint = WorldUtil.getClosestPointToBlock(blockPos);
        return closestPoint != null && mc.thePlayer.getPositionEyes(1.0F).distanceTo(closestPoint) <= 4.5;
    }

    private static void getTarget() {
        if (currentTarget != null && currentBedTarget != null) {
            if (!isValidBed(currentBedTarget) || !canAttackBlock(currentTarget)) {
                currentBedTarget = null;
                currentTarget = null;
                lastTarget = null;
                currentBreakProgress = 0.0F;
                rotationTick = -1;
            } else {
                if (canAttackBed(currentBedTarget)) currentTarget = currentBedTarget;
                return;
            }
        }

        ArrayList<BlockPos> beds = BlockTracker.getBlockPositions(Blocks.bed);
        double closestDistance = Double.MAX_VALUE;

        for (BlockPos blockPos : beds) {
            if (!isValidBed(blockPos)) continue;
            if (!canAttackBlock(blockPos)) continue;

            double distance = mc.thePlayer.getPositionEyes(1.0F).distanceTo(WorldUtil.getClosestPointToBlock(blockPos));
            if (distance < closestDistance) {
                closestDistance = distance;
                currentTarget = currentBedTarget = blockPos;
            }
        }

        if (currentTarget == null || !mode.is("Hypixel")) return;

        BlockPos bestSurrounding = WorldUtil.getBestBlockSurroundingBed(currentBedTarget);
        if (bestSurrounding == null) return;

        float blockStrength = InventoryUtil.blockStrength(
                mc.thePlayer.inventory.getStackInSlot(InventoryUtil.getBestSlotForBlock(bestSurrounding)),
                bestSurrounding
        );
        if (blockStrength == Float.POSITIVE_INFINITY) return;

        currentTarget = bestSurrounding;
    }
}
