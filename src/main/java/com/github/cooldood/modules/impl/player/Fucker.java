package com.github.cooldood.modules.impl.player;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.MotionEvent;
import com.github.cooldood.events.impl.PlayerUpdateEvent;
import com.github.cooldood.events.impl.RenderScoreboardEvent;
import com.github.cooldood.events.impl.RenderWorldEvent;
import com.github.cooldood.events.impl.RotationEvent;
import com.github.cooldood.events.impl.WorldUnloadEvent;
import com.github.cooldood.events.impl.RotationEvent;
import com.github.cooldood.modules.*;
import com.github.cooldood.modules.impl.movement.Scaffold;
import com.github.cooldood.modules.impl.render.BedESP;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.minecraft.*;
import com.github.cooldood.utils.render.Render3dUtil;
import com.github.cooldood.utils.render.RenderUtil;
import lombok.Getter;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RegisterModule(
        name = "Fucker",
        description = "Automatically destroys surrounding blocks.",
        category = Category.PLAYER,
        dangerous = true
)
public class Fucker extends Module {
    @RegisterSubModule(name = "Mode")
    public static Mode mode = Mode.Hypixel;
    public enum Mode {
        Vanilla,
        Hypixel
    }

    @RegisterSubModule(name = "Whitelist Own Bed")
    public static boolean whitelistBed = true;

    @RegisterSubModule(name = "Post Rotate", description = "Rotates in the same tick it swings")
    public static boolean preRotate = false;

    @RegisterSubModule(name = "Tick Rotate", description = "Only rotates on first and last tick of breaking block")
    public static boolean tickRotate = false;

    @RegisterSubModule(name = "Tick Swap", description = "Only swaps items on first and last tick of breaking block")
    public static boolean tickSwap = false;

    @RegisterSubModule(name = "Override Autoblock")
    public static boolean noAutoblock = true;

    @RegisterSubModule(name = "Override Kill Aura")
    public static boolean noKillAura = true;

    @RegisterSubModule(name = "Override Scaffold")
    public static boolean noScaffold = false;

    @RegisterSubModule(name = "Ticks Between Blocks", min = 0, max = 6, increment = 0.1, parent = "Basics")
    public static int ticksBetweenBlocks = 5;

    private static BlockPos currentBedTarget;
    @Getter
    private static BlockPos lastTarget, currentTarget;

    private static boolean hasTickSwapped = false;
    private static int lastHotbarSlot = -1;

    private static float currentBreakProgress = 0;

    private static int lastBreakTick = -1;
    private static int rotationTick = -1;

    private static boolean inBedwarsGame = false;
    private static boolean justStartedGame = false;

    private static int bestSlot = -1;

    // higher priority than killaura, velocity and scaffold
    @SubscribeEvent(priority = 4000)
    public static void findTargetAndRotate(RotationEvent event) {
        if (!noScaffold && Scaffold.isShouldScaffold()) return;

        if (MovementUtil.ticks - lastBreakTick < ticksBetweenBlocks || C.mc.playerController.getCurrentGameType().isAdventure()) return;

        getTarget();

        if (currentTarget == null) {
            rotationTick = -1;
            return;
        }

        Vec3 bestRotationSpot = WorldUtil.getClosestPointToBlock(currentTarget);
        if (bestRotationSpot == null) {
            rotationTick = -1;
            return;
        }

        if (rotationTick == -1) rotationTick = MovementUtil.ticks;

        if (shouldRotate()) {
            event.rotation = RotationUtil.getRotation(new Vec3(currentTarget.getX()+0.5, currentTarget.getY()+0.5, currentTarget.getZ()+0.5));
        }
    }

    @SubscribeEvent
    public static void attackBed(MotionEvent event) {
        EnumFacing facing = RotationUtil.getFacing().getOpposite();

        if (currentTarget == null || PlayerUtil.isUsingItem() || (!preRotate && MovementUtil.ticks - rotationTick <= 0)) return;

        PlayerUtil.swingHand();

        if (currentTarget != lastTarget) {
            lastTarget = currentTarget;
            PacketUtil.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, currentTarget, facing));
            lastHotbarSlot = C.p().inventory.currentItem;
            bestSlot = C.p().inventory.currentItem = InventoryUtil.getBestSlotForBlock(currentTarget);
            currentBreakProgress = 0;
        }
        else if (tickSwap && !hasTickSwapped) {
            C.p().inventory.currentItem = lastHotbarSlot;
            hasTickSwapped = true;
        }

        if (currentBreakProgress >= 1) {
            PacketUtil.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, currentTarget, facing));
            C.w().setBlockState(currentTarget, Blocks.air.getDefaultState(), 3);
            if (lastHotbarSlot != -1) C.p().inventory.currentItem = lastHotbarSlot;
            currentTarget = lastTarget = null;
            lastBreakTick = MovementUtil.ticks;
            rotationTick = -1;
            currentBreakProgress = 0;
            bestSlot = -1;
            hasTickSwapped = false;
            return;
        }

        currentBreakProgress += InventoryUtil.blockStrength(C.p().inventory.getStackInSlot(bestSlot), currentTarget);
        if (currentBreakProgress >= 1 && tickSwap) {
            C.p().inventory.currentItem = bestSlot;
        }
    }

    @SubscribeEvent
    public static void drawCurrentTarget(RenderWorldEvent event) {
        if (currentTarget == null) return;

        Render3dUtil.draw3dBox(currentTarget.getX(), currentTarget.getY(), currentTarget.getZ(), 1, MathHelper.clamp_float(currentBreakProgress, 0, 1), 1, RenderUtil.getProgressColour(currentBreakProgress), event.partialTicks);
    }

    @SubscribeEvent
    public static void onPlayerUpdate(PlayerUpdateEvent event) {
        if (!whitelistBed || !inBedwarsGame) return;
        
        // Find own bed if not found yet, and player has recently spawned
        if (bedHead == null && C.p().ticksExisted < 100 && C.p().onGround) {
            findOwnBed();
        }
    }

    @SubscribeEvent
    public static void checkForStartBedwars(RenderScoreboardEvent event) {
        if (!event.scoreObjective.getDisplayName().contains("BED WARS")) {
            inBedwarsGame = false;
            return;
        }

        if (inBedwarsGame) return;

        Collection<Score> scores = event.scoreObjective.getScoreboard().getSortedScores(event.scoreObjective);

        for (Score score : scores) {
            ScorePlayerTeam scoreplayerteam1 = event.scoreObjective.getScoreboard().getPlayersTeam(score.getPlayerName());
            String s1 = ScorePlayerTeam.formatPlayerName(scoreplayerteam1, score.getPlayerName());

            if (s1.contains("✓")) {
                if (!inBedwarsGame) {
                    bedHead = null; // Reset on new game
                    bedFoot = null;
                }
                inBedwarsGame = true;
                return;
            }
        }

        inBedwarsGame = false;
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldUnloadEvent event) {
        bedHead = null;
        bedFoot = null;
        inBedwarsGame = false;
    }

    private static BlockPos bedHead = null;
    private static BlockPos bedFoot = null;

    // todo: move this
    private static void findOwnBed() {
        if (C.p().capabilities.allowFlying) return;

        List<BlockPos> bedPositions = BlockTracker.getBlockPositions(Blocks.bed);

        for (BlockPos blockPos : bedPositions) {
            if (C.p().getDistance(blockPos.getX(), blockPos.getY(), blockPos.getZ()) > 30) continue;
            IBlockState state = C.w().getBlockState(blockPos);
            if (state.getBlock() != Blocks.bed || !state.getProperties().containsKey(BlockBed.PART) || !state.getProperties().containsKey(BlockBed.FACING)) continue;

            EnumFacing facing = state.getValue(BlockBed.FACING);
            boolean isFoot = state.getValue(BlockBed.PART) == BlockBed.EnumPartType.FOOT;

            bedHead = !isFoot ? blockPos.offset(facing.getOpposite()) : blockPos;
            bedFoot = isFoot ? blockPos.offset(facing) : blockPos;

            return;
        }
    }

    public static boolean shouldRotate() {
        if (currentTarget == null) return false;

        return !tickRotate || rotationTick == MovementUtil.ticks || (!preRotate && rotationTick >= MovementUtil.ticks - 2)
                || (bestSlot != -1 && currentBreakProgress + InventoryUtil.blockStrength(C.p().inventory.getStackInSlot(bestSlot), currentTarget) >= 1);
    }

    private static boolean isValidBed(BlockPos blockPos) {
        if (whitelistBed && (blockPos.equals(bedHead) || blockPos.equals(bedFoot))) return false;

        IBlockState state = C.w().getBlockState(blockPos);

        if (state.getBlock() != Blocks.bed) return false;
        return state.getProperties().containsKey(BlockBed.PART) && state.getProperties().containsKey(BlockBed.FACING);
    }

    private static boolean canAttackBed(BlockPos blockPos) {
        return canAttackBlock(blockPos) && WorldUtil.getBestBlockSurroundingBed(blockPos) == null;
    }

    private static boolean canAttackBlock(BlockPos blockPos) {
        Vec3 closestPoint = WorldUtil.getClosestPointToBlock(blockPos);

        return closestPoint != null && C.p().getPositionEyes(1).distanceTo(closestPoint) <= 4.5;
    }

    // todo: change block selection if a weaker block appears.
    private static void getTarget() {
        if (currentTarget != null && currentBedTarget != null) {
            if (!isValidBed(currentBedTarget) || !canAttackBlock(currentTarget)) {
                currentBedTarget = currentTarget = lastTarget = null;
                currentBreakProgress = 0;
                rotationTick = -1;
            }
            else {
                if (canAttackBed(currentBedTarget)) currentTarget = currentBedTarget;
                return;
            }
        }

        ArrayList<BlockPos> beds = BlockTracker.getBlockPositions(Blocks.bed);

        double closestDistance = Double.MAX_VALUE;

        for (BlockPos blockPos : beds) {
            if (!isValidBed(blockPos)) continue;
            if (!canAttackBlock(blockPos)) continue;

            double distance = C.p().getPositionEyes(1).distanceTo(WorldUtil.getClosestPointToBlock(blockPos));
            if (distance < closestDistance) {
                closestDistance = distance;
                currentTarget = currentBedTarget = blockPos;
            }
        }

        if (currentTarget == null || mode != Mode.Hypixel) return;

        BlockPos bestSurrounding = WorldUtil.getBestBlockSurroundingBed(currentBedTarget);
        if (bestSurrounding == null) return;
        float blockStrength = InventoryUtil.blockStrength(C.p().inventory.getStackInSlot(InventoryUtil.getBestSlotForBlock(bestSurrounding)), bestSurrounding);
        if (blockStrength == Float.POSITIVE_INFINITY) return;

        currentTarget = bestSurrounding;
    }

    @Override
    protected void onEnable() {
        if (!ModuleManager.isEnabled(BedESP.class)) BlockTracker.beginTracking(Blocks.bed);
    }

    @Override
    protected void onDisable() {
        if (!ModuleManager.isEnabled(BedESP.class)) BlockTracker.stopTracking(Blocks.bed);
        currentBedTarget = currentTarget = lastTarget = null;
        hasTickSwapped = false;
        if (lastHotbarSlot != -1) C.p().inventory.currentItem = lastHotbarSlot;
        lastHotbarSlot = -1;
    }
}
