package com.github.cooldood.modules.impl.movement;

import com.github.cooldood.bridge.net.minecraft.client.settings.KeyBindingBridge;
import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.*;
import com.github.cooldood.modules.*;
import com.github.cooldood.modules.impl.client.ThemeModule;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.minecraft.*;
import com.github.cooldood.utils.render.EasingUtil;
import com.github.cooldood.utils.render.Render3dUtil;
import com.github.cooldood.utils.render.RenderUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

@RegisterModule(
        name = "Scaffold",
        description = "Automatically places blocks under you.",
        category = Category.MOVEMENT,
        dangerous = true
)
// this code is so messy i hate it blehhhh
// todo: safewalk, better rotations
public class Scaffold extends Module {
    @RegisterSubModule(name = "Conditions")
    public static SubCategory conditions = new SubCategory();
    @RegisterSubModule(name = "Blocks Only", description = "Only scaffold if holding blocks", parent = "Conditions")
    public static boolean blocksOnly = false;

    @RegisterSubModule(name = "Moving Backwards", description = "Only scaffold if holding back key", parent = "Conditions")
    public static boolean movingBackwards = false;
    @RegisterSubModule(name = "Right Click Down", description = "Only scaffold if right click is down", parent = "Conditions")
    public static boolean rightClickOnly = false;
    @RegisterSubModule(name = "Crouch Down", description = "Only scaffold if crouch key is down", parent = "Conditions")
    public static boolean crouchDownOnly = false;
    @RegisterSubModule(name = "Uncrouch", description = "Doesn't actually sneak while holding crouch", parent = "Crouch Down")
    public static boolean uncrouchAuto = true;

    @RegisterSubModule(name = "Pitch Range", description = "Only scaffold if holding back key", parent = "Conditions")
    public static boolean pitchRange = false;
    @RegisterSubModule(name = "Min Pitch", parent = "Pitch Range", description = "-90 is looking straight up", min = -90, max = 90)
    public static int minPitch = 35;
    @RegisterSubModule(name = "Max Pitch", parent = "Pitch Range", description = "-90 is looking straight up", min = -90, max = 90)
    public static int maxPitch = 90;

    @RegisterSubModule(name = "Basics")
    public static SubCategory basicCategory = new SubCategory();

    @RegisterSubModule(name = "Block Place Reach", min = 2, max = 6, increment = 0.1, parent = "Basics")
    public static float blockReach = 4.5f;

    @RegisterSubModule(name = "Use Largest Stack", description = "Always switches to largest stack of blocks", parent = "Basics")
    public static boolean useLargestStack = false;

    @RegisterSubModule(name = "Swap Time", parent = "Use Largest Stack", description = "Blocks places between switching blocks", min = 1, max = 10)
    public static int swapTime = 5;

    @RegisterSubModule(name = "Same Y", parent = "Basics", description = "Maintains the same Y level unless jumping and right clicking")
    public static boolean sameY = false;

    @RegisterSubModule(name = "Tower")
    public static SubCategory towerCategory = new SubCategory();

    @RegisterSubModule(name = "Tower Mode", parent = "Tower", description = "Only tower within certain pitch range")
    public static TowerMode towerMode = TowerMode.Legit;

    public enum TowerMode {
        None,
        Legit,
        Vanilla
    }

    public static boolean defaultKeepY = true;

    @RegisterSubModule(name = "Keep Y")
    public static KeepYMode keepY = KeepYMode.None;

    public enum KeepYMode {
        None,
        Vanilla,
        Extra,
        Telly,
        ExtraTelly
    }

    @RegisterSubModule(name = "Keep Y On Press", parent = "Keep Y", modeParentString = {"Vanilla", "Extra", "Telly", "ExtraTelly"})
    public static boolean keepYonPress = false;

    @RegisterSubModule(name = "No Keep Y On Jump Potion", parent = "Keep Y", modeParentString = {"Vanilla", "Extra", "Telly", "ExtraTelly"})
    public static boolean disableWhileJumpActive = false;

    @RegisterSubModule(name = "Only Off Ground", parent = "Tower Mode", modeParentString = {"Legit", "Vanilla"}, description = "Only tower if holding your jump key")
    public static boolean onlyOffGround = true;

    @RegisterSubModule(name = "Only If Space Down", parent = "Tower Mode", modeParentString = {"Legit", "Vanilla"}, description = "Only tower if holding your jump key")
    public static boolean onlyIfSpaceDown = true;

    @RegisterSubModule(name = "Tower Pitch Range", parent = "Tower Mode", modeParentString = {"Legit", "Vanilla"}, description = "Only tower within certain pitch range")
    public static boolean onlyTowerLookingUp = true;

    @RegisterSubModule(name = "Tower Min Pitch", parent = "Tower Pitch Range", description = "-90 is looking straight up", min = -90, max = 90)
    public static int towerMinPitch = -90;

    @RegisterSubModule(name = "Tower Max Pitch", parent = "Tower Pitch Range", description = "-90 is looking straight up", min = -90, max = 90)
    public static int towerMaxPitch = 0;

    @RegisterSubModule(name = "Visuals")
    public static SubCategory visuals = new SubCategory();

    @RegisterSubModule(name = "Auto F5", parent = "Visuals")
    public static boolean autoF5 = true;

    @RegisterSubModule(name = "Show Target Block", parent = "Visuals")
    public static boolean showTargetBlock = true;
    @RegisterSubModule(name = "Target Block Colour", parent = "Show Target Block")
    public static Color targetBlockColour = new Color(227, 155, 248);

    @RegisterSubModule(name = "Show Previous Blocks", parent = "Visuals")
    public static boolean showPreviousBlocks = true;
    @RegisterSubModule(name = "Fade Time", parent = "Show Previous Blocks", min = 50, max = 10000, increment = 50)
    public static long showPreviousBlocksTime = 3000;

    @RegisterSubModule(name = "Item Spoof", description = "Shows the block being placed in your hand", parent = "Visuals")
    public static boolean itemSpoof = false;

    @RegisterSubModule(name = "Bypass")
    public static SubCategory bypass = new SubCategory();

    // todo: crouch ticks option
    @RegisterSubModule(name = "Crouch On Edge", parent = "Bypass")
    public static boolean crouchOnEdge = false;
    @RegisterSubModule(name = "Crouch In Air", parent = "Crouch On Edge")
    public static boolean crouchInAir = false;

    @RegisterSubModule(name = "Manual Place", parent = "Bypass", description = "Manually click to place")
    public static boolean manualPlace = false;

    @RegisterSubModule(name = "Rotate", parent = "Bypass", description = "Only places when looking at best target block")
    public static boolean rotate = true;
    @RegisterSubModule(name = "Rotation Mode", parent = "Rotate")
    public static BridgingMode bridgingMode = BridgingMode.Normal;
    @RegisterSubModule(name = "Only Place Best", parent = "Rotate", description = "Only places when looking at best target block")
    public static boolean onlyPlaceBest = true;
    @RegisterSubModule(name = "No Duplicate Rot", description = "Bypasses grims DuplicateRotPlace check", parent = "Rotate")
    public static boolean noDuplicateRot = true;

    public enum BridgingMode {
        Normal,
        Telly,
        Telly_Plus
    }

    @RegisterSubModule(name = "Only When Jumping", description = "Only telly bridge if space held down", parent = "Rotation Mode", modeParentString = {"Telly", "Telly_Plus"})
    public static boolean spaceDownOnly = true;

    @RegisterSubModule(name = "Telly Mode", description = "Ticks To Snap Back To Looking Forward After Landing", max = 5, parent = "Rotation Mode", modeParentString = {"Telly", "Telly_Plus"})
    public static Telly_Mode tellyMode = Telly_Mode.Hypixel;
    @AllArgsConstructor
    public enum Telly_Mode {
        Hypixel(true, EasingUtil.EasingFunctions.Ease_Out_Bounce, 3, 1, 1, 1),
        Grim(false, EasingUtil.EasingFunctions.Normal, 0, 1, 0, 0),
        Custom(false, null, -1, -1, -1, -1) {
            @Override
            public boolean isSmoothRotationTelly() {
                return Scaffold.smoothRotationTelly;
            }
            @Override
            public EasingUtil.EasingFunctions getEasingFunction() {
                return Scaffold.easingFunction;
            }
            @Override
            public int getRotationTicks() {
                return Scaffold.rotationTicks;
            }
            @Override
            public int getTellyTicks() {
                return Scaffold.tellyTicks;
            }
            @Override
            public int getTellyPlaceDelay() {
                return Scaffold.tellyPlaceDelay;
            }
            @Override
            public int getTellyForwardTicks() {
                return Scaffold.tellyForwardTicks;
            }
        };

        @Getter public final boolean smoothRotationTelly;
        @Getter public final EasingUtil.EasingFunctions easingFunction;
        @Getter public final int rotationTicks;
        @Getter public final int tellyTicks;
        @Getter public final int tellyPlaceDelay;
        @Getter public final int tellyForwardTicks;
    }


    @RegisterSubModule(name = "Smooth Rotation", description = "Doesn't snap back to placing blocks", parent = "Telly Mode", modeParentString = "Custom")
    public static boolean smoothRotationTelly = true;

    @RegisterSubModule(name = "Easing", description = "Doesn't snap back to placing blocks", parent = "Smooth Rotation")
    public static EasingUtil.EasingFunctions easingFunction = EasingUtil.EasingFunctions.Ease_Out_Expo;

    @RegisterSubModule(name = "Rotation Ticks", description = "Ticks taken to finish rotating", max = 5, parent = "Smooth Rotation")
    public static int rotationTicks = 3;

    @RegisterSubModule(name = "Telly Ticks", description = "Ticks To Snap Back To Looking Forward After Landing", max = 5, parent = "Telly Mode", modeParentString = "Custom")
    public static int tellyTicks = 1;

    @RegisterSubModule(name = "Telly Place Delay", description = "Ticks Before Placing After Snapping Back", max = 5, parent = "Telly Mode", modeParentString = "Custom")
    public static int tellyPlaceDelay = 4;

    @RegisterSubModule(name = "Telly Forward Ticks", description = "Ticks Before Jumping", max = 5, parent = "Telly Mode", modeParentString = "Custom")
    public static int tellyForwardTicks = 1;

    @Getter private static boolean shouldScaffold = false;

    public static int getSlot() {
        return InventoryUtil.biggestBlockSlot();
    }

    private static final ConcurrentHashMap<BlockPos, Long> previousInteractions = new ConcurrentHashMap<>();

    private static float lastPlacedDeltaX = -1;

    private static int blocksPlaced = 0;

    private static int tellyTicksCounter = 0;
    private static int tellyPlaceDelayCounter = 0;

    private static boolean tellyBlockPlaced = true;
    private static int tellyForwardTicksCount = -1;

    private static boolean lastJump = false;

    @SubscribeEvent
    public static void onKeyInput(MovementInputEvent event) {
        if (shouldScaffold) {
            tellyForwardTicksCount = C.p().onGround && tellyBlockPlaced ? tellyForwardTicksCount+1 : -1;

            if (shouldTelly()) {
                event.movementInput.jump = shouldTellyJump(event.movementInput.jump);
                tellyBlockPlaced &= !event.movementInput.jump;
            }
            else {
                event.movementInput.jump |= towerMode == TowerMode.Legit && shouldTower();
            }

            lastJump = C.mc.gameSettings.keyBindJump.isKeyDown();
        }
    }

    private static boolean didPlace = false;
    private static int lastPlaceStack = -1;
    private static boolean overridingSneak = false;

    private static BlockTarget targetBlock = null;

    @SubscribeEvent(priority = 3000)
    public static void onRotationEvent(RotationEvent event) {
        tryPlace = false;
        didPlace |= C.p().inventory.currentItem == lastPlaceStack && InventoryUtil.isSlotEmpty(C.p().inventory.currentItem) && shouldScaffold;

        int bestStack = InventoryUtil.biggestBlockSlot();

        if (!InventoryUtil.isValidBlock() && (blocksOnly && !didPlace) || bestStack == -1 || !shouldScaffold()) {
            disable();
            return;
        }

        if (!InventoryUtil.isValidBlock() || (useLargestStack && blocksPlaced % swapTime == 0)) {
            if (previousStack == -1) previousStack = C.p().inventory.currentItem;
            C.p().inventory.currentItem = bestStack;
        }

        if (!shouldScaffold) enable();

        didPlace = false;

        Vec3 positionToRotateFrom = C.p().getPositionVector();
        if (!shouldPlaceBlock()) {
            Vec3 predictedNextPosition = getPredictedNextPosition();
            if (predictedNextPosition != null) positionToRotateFrom = predictedNextPosition;
        }

        targetBlock = getBestTargetBlock(positionToRotateFrom);
        if (targetBlock == null) return;

        boolean isKeepYTelly = (keepY == KeepYMode.Telly || keepY == KeepYMode.ExtraTelly);
        if ((shouldTelly() || isKeepYTelly) && C.p().onGround && (tellyBlockPlaced || tellyMode.getTellyForwardTicks() == 0 || isKeepYTelly)) {
            tellyTicksCounter = 0;
            tellyPlaceDelayCounter = 0;
        }

        if (shouldTelly() || isKeepYTelly) {
            tellyTicksCounter++;
            if (tellyTicksCounter <= tellyMode.getTellyTicks()) return;
        }

        tellyPlaceDelayCounter++;

        if (shouldRotate()) rotate(positionToRotateFrom, targetBlock, event);
        if (!manualPlace) tryPlace = true;
    }

    @SubscribeEvent
    public static void onPlayerUpdate(PlayerUpdateEvent event) {
        if (!shouldScaffold()) return;

        boolean isTellyPlus = bridgingMode == BridgingMode.Telly_Plus;
        boolean manualJumpHeld = C.mc.gameSettings.keyBindJump.isKeyDown();

        if (C.p().onGround) {
            if (stage > 0) {
                stage--;
            }
            if (stage < 0) {
                stage++;
            }
            if (stage == 0
                    && (keepY != KeepYMode.None || isTellyPlus)
                    && (!keepYonPress || PlayerUtil.isUsingItem())
                    && (!disableWhileJumpActive || !C.p().isPotionActive(net.minecraft.potion.Potion.jump))
                    && !manualJumpHeld) {
                stage = 1;
            }
            if (isTellyKeepYActive()) {
                if (manualJumpHeld || tellyKeepY == 256) {
                    tellyKeepY = MathHelper.floor_double(C.p().posY);
                }
            } else {
                tellyKeepY = 256;
            }

            if (isTellyPlus) {
                if (manualJumpHeld || startY == 256) {
                    startY = MathHelper.floor_double(C.p().posY);
                }
            } else {
                startY = (shouldKeepY && !manualJumpHeld) ? startY : MathHelper.floor_double(C.p().posY);
                shouldKeepY = false;
            }
        } else if ((keepY == KeepYMode.Extra || keepY == KeepYMode.ExtraTelly || isTellyPlus) && stage > 0 && !manualJumpHeld) {
            int nextBlockY = MathHelper.floor_double(C.p().posY + C.p().motionY);
            if (nextBlockY <= startY && C.p().posY > (double) (startY + 1)) {
                shouldKeepY = true;
            }
        }

        if (InventoryUtil.isValidBlock() && crouchDownOnly && uncrouchAuto) {
            KeyBindingBridge.from(C.mc.gameSettings.keyBindSneak).bridge$setDown(false);
        }

        if (!shouldPlaceBlock() || !InventoryUtil.isValidBlock()) {
            if (overridingSneak && (C.p().onGround || !crouchInAir)) {
                KeyBindingBridge.from(C.mc.gameSettings.keyBindSneak).bridge$setDown(Keyboard.isKeyDown(C.mc.gameSettings.keyBindSneak.getKeyCode()));
                overridingSneak = false;
            }
            return;
        }

        if (shouldTower() && towerMovement()) setShouldTower();

        if (crouchOnEdge) {
            if (C.p().onGround || crouchInAir) {
                KeyBindingBridge.from(C.mc.gameSettings.keyBindSneak).bridge$setDown(true);
                overridingSneak = true;
            }
        }


        if (!tryPlace) return;

        tryPlace();
    }

    private static boolean tryPlace = false;

    @SubscribeEvent
    public static void onRightClick(ClickMouseEvent.Right event) {
        if (!shouldScaffold() || !InventoryUtil.isValidBlock()) return;

        if (WorldUtil.isOverAir()) {
            event.setCancelled(true);
            tryPlace = true;
        }
    }

    private static boolean isSameYBlocking(EnumFacing sideHit) {
        if (!sameY) return false;
        if (sideHit != EnumFacing.UP) return false;
        // Exception: Holding Jump AND Right Click allows UP placement (tower)
        boolean jumpHeld = C.mc.gameSettings.keyBindJump.isKeyDown();
        boolean rightClickHeld = C.mc.gameSettings.keyBindUseItem.isKeyDown();
        return !(jumpHeld && rightClickHeld);
    }

    private static void tryPlace() {
        MovingObjectPosition rayTrace = WorldUtil.rayTrace(blockReach, PlayerUtil.currentRotation());

        if (rayTrace == null || rayTrace.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;
        if (isSameYBlocking(rayTrace.sideHit)) return;
        if (rotate && onlyPlaceBest && (targetBlock == null || !rayTrace.getBlockPos().offset(rayTrace.sideHit).equals(targetBlock.pos.offset(targetBlock.direction)))) return;
        if (!rotate && shouldKeepY() && rayTrace.sideHit == EnumFacing.UP) return;
        if (rayTrace.getBlockPos().offset(rayTrace.sideHit).getY() > C.p().posY) return;
        if (shouldTelly() && tellyPlaceDelayCounter < tellyMode.getTellyPlaceDelay() + Math.max(0, tellyMode.getRotationTicks()-1)) return;

        if (C.mc.playerController.onPlayerRightClick(C.p(), C.w(), C.p().getHeldItem(), rayTrace.getBlockPos(), rayTrace.sideHit, rayTrace.hitVec)) {
            PlayerUtil.swingHand();
            if (InventoryUtil.isSlotEmpty(C.p().inventory.currentItem))
                C.p().inventory.removeStackFromSlot(C.p().inventory.currentItem);

            lastPlacedDeltaX = Math.abs(PlayerUtil.currentRotation().yaw - PlayerUtil.lastRotation().yaw);
            blocksPlaced++;
            previousInteractions.put(rayTrace.getBlockPos().offset(rayTrace.sideHit), System.currentTimeMillis());

            lastPlaceStack = C.p().inventory.currentItem;

            tellyBlockPlaced = C.p().onGround;
        }
    }

    private static boolean shouldTellyJump(boolean jumpDown) {
        if (bridgingMode == BridgingMode.Telly_Plus) {
            return MovementUtil.groundTicks >= 1;
        }
        return (tellyBlockPlaced && tellyForwardTicksCount >= tellyMode.getTellyForwardTicks())
                || (jumpDown && !lastJump)
                || tellyMode.getTellyForwardTicks() == 0;
    }

    private static boolean shouldScaffold() {
        return (!crouchDownOnly || Keyboard.isKeyDown(C.mc.gameSettings.keyBindSneak.getKeyCode()))
                && (!rightClickOnly || C.mc.gameSettings.keyBindUseItem.isKeyDown())
                && (!pitchRange || (C.p().rotationPitch <= maxPitch && C.p().rotationPitch >= minPitch))
                && (!movingBackwards || Keyboard.isKeyDown(C.mc.gameSettings.keyBindBack.getKeyCode()));
    }

    private static boolean shouldTelly() {
        return rotate && (bridgingMode == BridgingMode.Telly_Plus || (bridgingMode == BridgingMode.Telly && (!spaceDownOnly || C.mc.gameSettings.keyBindJump.isKeyDown())));
    }

    private static boolean shouldPlaceBlock() {
        return WorldUtil.isOverAir()
                && (C.p().onGround || !shouldKeepY() || WorldUtil.isOverAir(C.p().getPositionVector().subtract(0, 1, 0)));
    }

    private static boolean shouldRotate() {
        return rotate && (bridgingMode != BridgingMode.Normal || shouldPlaceBlock());
    }

    // 1 second time travel hack
    private static Vec3 getPredictedNextPosition() {
        Vec3 pos = C.p().getPositionVector();
        double velocityX = C.p().posX - C.p().prevPosX;
        double velocityZ = C.p().posZ - C.p().prevPosZ;

        for (int i = 1; i <= 20; i++) {
            pos = pos.add(new Vec3(velocityX, 0, velocityZ));
            if (WorldUtil.isOverAir(pos)) return pos;
        }


        return null;
    }

    @SubscribeEvent
    public static void onRenderWorldEvent(RenderWorldEvent event) {
        if (showPreviousBlocks) {
            previousInteractions.forEach((blockPos, time) -> {
                if (System.currentTimeMillis() - time > showPreviousBlocksTime) {
                    previousInteractions.remove(blockPos);
                    return;
                }

                double animationValue = (double) (System.currentTimeMillis() - time) / showPreviousBlocksTime;

                Color color = RenderUtil.getColorsFade(time / 20d, ThemeModule.getThemeColours(), 0.2f);

                Render3dUtil.draw3dBox(
                        blockPos.getX(),
                        blockPos.getY(),
                        blockPos.getZ(),
                        1,
                        1,
                        1,
                        RenderUtil.setOpacity(color, 0.5 * (1 - animationValue)),
                        event.partialTicks,
                        !previousInteractions.containsKey(blockPos.offset(EnumFacing.DOWN)),
                        !previousInteractions.containsKey(blockPos.offset(EnumFacing.UP)),
                        !previousInteractions.containsKey(blockPos.offset(EnumFacing.NORTH)),
                        !previousInteractions.containsKey(blockPos.offset(EnumFacing.SOUTH)),
                        !previousInteractions.containsKey(blockPos.offset(EnumFacing.WEST)),
                        !previousInteractions.containsKey(blockPos.offset(EnumFacing.EAST)),
                        false
                );
            });
        }

        if (showTargetBlock && targetBlock != null) {
            BlockPos blockPos = targetBlock.pos.offset(targetBlock.direction);

            Render3dUtil.draw3dBox(
                    blockPos.getX(),
                    blockPos.getY(),
                    blockPos.getZ(),
                    1,
                    1,
                    1,
                    targetBlockColour,
                    event.partialTicks
            );
        }
    }


    private static boolean shouldTower = false;

    private static int stage = 0;
    private static int startY = 256;
    private static boolean shouldKeepY = false;
    private static int tellyKeepY = 256;

    private static boolean isTellyKeepYActive() {
        return shouldTelly() && (keepY != KeepYMode.None || bridgingMode == BridgingMode.Telly_Plus);
    }

    private static boolean shouldKeepY() {
        if (bridgingMode == BridgingMode.Telly_Plus) {
            return !C.mc.gameSettings.keyBindJump.isKeyDown() && stage > 0;
        }
        return keepY != KeepYMode.None && stage > 0;
    }

    private static boolean shouldTower() {
        if (towerMode == TowerMode.None) return false;

        // only stop and start towering when on ground
        if (C.p().onGround || MovementUtil.airTicks == 1) setShouldTower();

        return shouldTower;
    }

    private static void setShouldTower() {
        shouldTower = (!onlyOffGround || !C.p().onGround)
                && (!onlyIfSpaceDown || C.mc.gameSettings.keyBindJump.isKeyDown())
                && (!onlyTowerLookingUp || C.p().rotationPitch <= towerMaxPitch && C.p().rotationPitch >= towerMinPitch);
    }

    /// returns true when the player can stop towering if they want to
    // get em high
    private static boolean towerMovement() {
        switch (towerMode) {
            case Legit:
                return true;
            case Vanilla:
                // slightly more reliable than airTicks % 3
                int playerYto2Decimals = (int) ((C.p().posY % 1) * 100);
                switch (playerYto2Decimals) {
                    case 0:
                        //getJumpUpwardsMotion() is always 0.42F
                        // todo: implement jumpboost check maybe?
                        // if (this.isPotionActive(Potion.jump)) this.motionY += (double)((float)(this.getActivePotionEffect(Potion.jump).getAmplifier() + 1) * 0.1F);
                        C.p().motionY = 0.42F;
                        break;
                    case 41: // 0.42f ~= 0.419999, and im not rounding, im chopping off the digits
                        C.p().motionY = 0.33F;
                        break;
                    case 75: // 42 + 33
                        // should always be ~0.25, but it could lose accuracy over time
                        C.p().motionY = 1 - (C.p().posY % 1);
                        return true;
                }
        }

        return false;
    }

    private static BlockTarget getBestTargetBlock(Vec3 position) {
        int playerY = MathHelper.floor_double(C.p().posY);
        int targetY;
        if (isTellyKeepYActive() && tellyKeepY != 256) {
            targetY = tellyKeepY;
        } else {
            targetY = (stage != 0 && !shouldKeepY ? Math.min(playerY, startY) : playerY);
        }
        BlockPos blockPosition = new BlockPos(position.xCoord, targetY, position.zCoord);
        BlockPos point1 = blockPosition.add(-blockReach, -blockReach, -blockReach);
        BlockPos point2 = blockPosition.add(blockReach, -1, blockReach);
        Iterator<BlockPos> blocksInRange = BlockPos.getAllInBox(point1, point2).iterator();

        double bestDistance = Integer.MAX_VALUE;
        BlockTarget bestBlock = null;

        while (blocksInRange.hasNext()) {
            BlockPos blockPos = blocksInRange.next();
            Block currentBlock = C.w().getBlockState(blockPos).getBlock();

            if (currentBlock == null || InventoryUtil.isBlockInteractable(currentBlock) || !InventoryUtil.isSolidBlock(currentBlock)) continue;

            for (EnumFacing facing : EnumFacing.values()) {
                BlockPos blockPosOffset = blockPos.offset(facing);

                // obviously placing a block above you isnt helpful.
                if (facing == EnumFacing.DOWN) continue;
                if (facing == EnumFacing.UP && (shouldKeepY() || isSameYBlocking(facing))) continue;

                if (InventoryUtil.isSolidBlock(C.w().getBlockState(blockPosOffset).getBlock())) continue;
                if (blockPosOffset.getY() + 1 > C.p().posY) continue;

                Vec3 offsetBlockCentre = new Vec3(blockPosOffset.getX() + 0.5, blockPosOffset.getY() + 0.5, blockPosOffset.getZ() + 0.5);
                double distance = position.distanceTo(offsetBlockCentre);

                if (distance > bestDistance) continue;

                bestDistance = distance;
                bestBlock = new BlockTarget(blockPos, facing);
            }
        }

        if (bestDistance > blockReach) return null;
        return bestBlock;
    }

    // todo: take next block into account.
    //  use the same sorta thing as killaura rotations, this is an fps killer.
    // maybe doing the block searching in here would be smarter but idk
    private static void rotate(Vec3 playerPosition, BlockTarget blockTarget, RotationEvent event) {
        if (bridgingMode == BridgingMode.Telly_Plus && MovementUtil.groundTicks == 1) {
            event.rotation = new RotationUtil.Rotation(PlayerUtil.lastRotation().pitch, C.p().rotationYaw);
            return;
        }

        MovingObjectPosition blockHitResult = WorldUtil.rayTrace(blockReach, playerPosition, PlayerUtil.lastRotation());

        BlockPos currentBlock = blockHitResult.getBlockPos().offset(blockHitResult.sideHit);
        BlockPos targetBlock = blockTarget.pos.offset(blockTarget.direction);

        // the previous blockpos is good, dont change.
        if (!currentBlock.equals(targetBlock)) {
            float closestPitch = 91;
            float closestYaw = 181;
            boolean foundRotation = false;

            // searches for closest yaw + pitch change
            // <= Math.abs(closestYaw) to stop worthless loops!
            for (float yaw = -closestYaw+1; yaw <= Math.abs(closestYaw); yaw++) {
                for (float pitch = 90; pitch >= 0; pitch--) {
                    RotationUtil.Rotation gcdedRotation = RotationUtil.applyGcd(new RotationUtil.Rotation(pitch, PlayerUtil.lastRotation().yaw + yaw));
                    float yawChange = gcdedRotation.yaw - PlayerUtil.lastRotation().yaw;

                    // check from grim:
                    /* https://github.com/GrimAnticheat/Grim/blob/2.0/common/src/main/java/ac/grim/grimac/checks/impl/scaffolding/DuplicateRotPlace.java
                    // where deltaX = rotationUpdate.getDeltaXRotABS();
                    // and this.lastPlacedDeltaX = deltaX onPostFlyingBlockPlace
                    if (deltaX > 2) {
                        float xDiff = Math.abs(deltaX - lastPlacedDeltaX);
                        if (xDiff < 0.0001) {
                            flagAndAlert("x=" + xDiff + " xdots=" + xDiffDots + " y=" + deltaY);
                        }
                        // rewards if else
                    }
                     */
                    float deltaX = Math.abs(yawChange);
                    if (deltaX > 2 && noDuplicateRot) {
                        float xDiff = Math.abs(deltaX - lastPlacedDeltaX);
                        if (xDiff < 0.0001) continue;
                    }

                    MovingObjectPosition raycast = WorldUtil.rayTrace(blockReach, playerPosition, gcdedRotation);
                    if (raycast == null) continue;
                    BlockPos raycastBlock = raycast.getBlockPos().offset(raycast.sideHit);

                    if (raycastBlock.equals(targetBlock) && (!shouldKeepY() || raycast.sideHit != EnumFacing.UP) && !isSameYBlocking(raycast.sideHit)) {
                        float bestRotationChange = Math.abs(PlayerUtil.lastRotation().pitch - closestPitch) + Math.abs(closestYaw);
                        float currentRotationChange = Math.abs(PlayerUtil.lastRotation().pitch - gcdedRotation.pitch) + Math.abs(yawChange);
                        if (currentRotationChange < bestRotationChange) {
                            closestYaw = yawChange;
                            closestPitch = gcdedRotation.pitch;
                            foundRotation = true;
                        }
                    }
                }
            }

            if (foundRotation) {
                RotationUtil.Rotation bestRotation = new RotationUtil.Rotation(closestPitch, PlayerUtil.lastRotation().yaw + closestYaw);

                if (shouldSmoothRotate()) {
                    event.rotation = RotationUtil.getEasedRotation(PlayerUtil.lastRotation(), bestRotation, tellyMode.getEasingFunction(), getRotationLerp());
                }
                else {
                    event.rotation = bestRotation;
                }

                return;
            }
        }

        if (bridgingMode != BridgingMode.Normal) {
            event.rotation = PlayerUtil.lastRotation();
        }
    }

    private static boolean shouldSmoothRotate() {
        return shouldTelly()
                && bridgingMode != BridgingMode.Telly_Plus
                && tellyPlaceDelayCounter <= tellyMode.getRotationTicks()
                && tellyMode.isSmoothRotationTelly();
    }

    private static double getRotationLerp() {
        return (double) tellyPlaceDelayCounter / tellyMode.getRotationTicks();
    }

    private static int previousPerspective = 0;
    private static int previousStack = -1;

    private static void enable() {
        shouldScaffold = true;
        blocksPlaced = 0;

        if (autoF5) {
            previousPerspective = C.mc.gameSettings.thirdPersonView;
            C.mc.gameSettings.thirdPersonView = 1;
        }
    }

    @Override
    protected void onEnable() {}

    private static void disable() {
        if (shouldScaffold) {
            shouldScaffold = false;
            shouldTower = false;
            targetBlock = null;
            startY = 256;
            stage = 0;
            shouldKeepY = false;
            tellyKeepY = 256;

            if (previousStack != -1 && C.isInGame()) {
                C.p().inventory.currentItem = previousStack;
                previousStack = -1;
            }
            if (autoF5) {
                C.mc.gameSettings.thirdPersonView = previousPerspective;
            }
            overridingSneak = false;
            KeyBindingBridge.from(C.mc.gameSettings.keyBindSneak).bridge$setDown(Keyboard.isKeyDown(C.mc.gameSettings.keyBindSneak.getKeyCode()));
        }
    }

    @Override
    protected void onDisable() {
        disable();
    }

    @Override
    public String arrayListExtraInfo() {
        return rotate ? bridgingMode.name() : "Safewalk";
    }

    // no records :( java 8
    @AllArgsConstructor
    private static class BlockTarget {
        public BlockPos pos;
        public EnumFacing direction;
    }
}