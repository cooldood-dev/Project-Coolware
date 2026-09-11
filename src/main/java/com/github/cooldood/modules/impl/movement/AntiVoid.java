package com.github.cooldood.modules.impl.movement;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.RotationEvent;
import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.modules.ModuleManager;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.minecraft.InventoryUtil;
import com.github.cooldood.utils.minecraft.PlayerUtil;
import com.github.cooldood.utils.minecraft.RotationUtil;
import com.github.cooldood.utils.minecraft.WorldUtil;
import net.minecraft.block.Block;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import java.util.Iterator;

import com.github.cooldood.modules.RegisterSubModule;

@RegisterModule(
        name = "AntiVoid",
        description = "Automatically prevents you from falling into the void by instantly placing a block.",
        category = Category.MOVEMENT
)
public class AntiVoid extends Module {

    private static final float REACH = 6.0f;

    @RegisterSubModule(name = "Fall Distance", description = "Fall distance needed to activate", min = 1, max = 10, increment = 0.5)
    public static float triggerFallDistance = 3.0f;

    @RegisterSubModule(name = "Prediction Ticks", description = "How far ahead to predict falling", min = 1, max = 40, increment = 1)
    public static int predictionTicks = 20;
    
    @RegisterSubModule(name = "Place Delay", description = "Ticks to wait between placing blocks", min = 0, max = 20, increment = 1)
    public static int placeDelay = 0;
    
    @RegisterSubModule(name = "Max Blocks", description = "Maximum blocks to place per save event", min = 1, max = 10, increment = 1)
    public static int maxBlocks = 2;

    private static int blocksPlaced = 0;
    private static int ticksSinceLastPlace = 0;
    private static boolean wasSaving = false;

    @SubscribeEvent
    public static void onRotation(RotationEvent event) {
        if (!ModuleManager.isEnabled(AntiVoid.class)) return;
        
        boolean shouldSave = false;
        
        if (C.p().fallDistance > triggerFallDistance || C.p().posY < 0) {
            if (isPositionOverVoid(C.p().getPositionVector())) {
                shouldSave = true;
            }
        }
        
        if (!shouldSave) {
            double velocityX = C.p().posX - C.p().prevPosX;
            double velocityZ = C.p().posZ - C.p().prevPosZ;
            
            if (Math.abs(velocityX) > 0.05 || Math.abs(velocityZ) > 0.05) {
                Vec3 pos = C.p().getPositionVector();
                for (int i = 1; i <= predictionTicks; i++) {
                    pos = pos.addVector(velocityX, 0, velocityZ);
                    if (isPositionOverVoid(pos)) {
                        shouldSave = true;
                        break;
                    }
                }
            }
        }
        
        if (shouldSave) {
            if (!wasSaving) {
                blocksPlaced = 0;
                ticksSinceLastPlace = placeDelay; // allow first block instantly
            }
            wasSaving = true;
            
            if (blocksPlaced < maxBlocks) {
                if (ticksSinceLastPlace >= placeDelay) {
                    int bestSlot = InventoryUtil.biggestBlockSlot();
                    if (bestSlot == -1) return;
                    
                    BlockTarget target = getBestTargetBlock(C.p().getPositionVector());
                    if (target != null) {
                        Vec3 hitVec = new Vec3(target.pos.getX() + 0.5 + target.direction.getFrontOffsetX() * 0.5,
                                               target.pos.getY() + 0.5 + target.direction.getFrontOffsetY() * 0.5,
                                               target.pos.getZ() + 0.5 + target.direction.getFrontOffsetZ() * 0.5);
                        
                        RotationUtil.Rotation rot = getRotationsToVec(hitVec);
                        event.rotation = rot;
                        
                        int oldSlot = C.p().inventory.currentItem;
                        C.p().inventory.currentItem = bestSlot;
                        
                        if (C.mc.playerController.onPlayerRightClick(C.p(), C.w(), C.p().inventory.getStackInSlot(bestSlot), target.pos, target.direction, hitVec)) {
                            PlayerUtil.swingHand();
                            blocksPlaced++;
                            ticksSinceLastPlace = 0;
                        }
                        
                        C.p().inventory.currentItem = oldSlot;
                    }
                } else {
                    ticksSinceLastPlace++;
                }
            }
        } else {
            wasSaving = false;
        }
    }
    
    private static RotationUtil.Rotation getRotationsToVec(Vec3 vec) {
        double diffX = vec.xCoord - C.p().posX;
        double diffY = vec.yCoord - (C.p().posY + C.p().getEyeHeight());
        double diffZ = vec.zCoord - C.p().posZ;
        
        double dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);
        float yaw = (float) (Math.atan2(diffZ, diffX) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) -(Math.atan2(diffY, dist) * 180.0D / Math.PI);
        
        return new RotationUtil.Rotation(pitch, yaw);
    }
    
    private static boolean isPositionOverVoid(Vec3 pos) {
        for (int y = (int) C.p().posY; y >= 0; y--) {
            BlockPos blockPos = new BlockPos(pos.xCoord, y, pos.zCoord);
            if (!C.w().isAirBlock(blockPos)) {
                return false;
            }
        }
        return true;
    }

    private static BlockTarget getBestTargetBlock(Vec3 position) {
        int playerY = MathHelper.floor_double(C.p().posY);
        BlockPos blockPosition = new BlockPos(position.xCoord, playerY, position.zCoord);
        BlockPos point1 = blockPosition.add(-REACH, -REACH, -REACH);
        BlockPos point2 = blockPosition.add(REACH, -1, REACH);
        Iterator<BlockPos> blocksInRange = BlockPos.getAllInBox(point1, point2).iterator();

        double bestDistance = Integer.MAX_VALUE;
        BlockTarget bestBlock = null;

        while (blocksInRange.hasNext()) {
            BlockPos blockPos = blocksInRange.next();
            Block currentBlock = C.w().getBlockState(blockPos).getBlock();

            if (currentBlock == null || InventoryUtil.isBlockInteractable(currentBlock) || !InventoryUtil.isSolidBlock(currentBlock)) continue;

            for (EnumFacing facing : EnumFacing.values()) {
                BlockPos blockPosOffset = blockPos.offset(facing);

                if (facing == EnumFacing.UP) continue; // we want to build out/down

                if (InventoryUtil.isSolidBlock(C.w().getBlockState(blockPosOffset).getBlock())) continue;
                if (blockPosOffset.getY() + 1 > C.p().posY) continue;

                Vec3 offsetBlockCentre = new Vec3(blockPosOffset.getX() + 0.5, blockPosOffset.getY() + 0.5, blockPosOffset.getZ() + 0.5);
                double distance = position.distanceTo(offsetBlockCentre);

                if (distance > bestDistance) continue;

                bestDistance = distance;
                bestBlock = new BlockTarget(blockPos, facing);
            }
        }

        if (bestDistance > REACH) return null;
        return bestBlock;
    }

    private static class BlockTarget {
        public BlockPos pos;
        public EnumFacing direction;
        
        public BlockTarget(BlockPos pos, EnumFacing direction) {
            this.pos = pos;
            this.direction = direction;
        }
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
    }
}
