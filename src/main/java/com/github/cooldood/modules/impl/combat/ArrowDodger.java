package com.github.cooldood.modules.impl.combat;

import com.github.cooldood.events.impl.PlayerUpdateEvent;
import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.utils.client.C;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockLiquid;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;

@RegisterModule(
        name = "ArrowDodger",
        description = "Automatically dodges incoming arrows by moving left or right.",
        category = Category.COMBAT
)
public class ArrowDodger extends Module {

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
    }

    public static void onPlayerUpdateEvent(PlayerUpdateEvent event) {
        for (Entity e : C.w().loadedEntityList) {
            if (e instanceof EntityArrow) {
                EntityArrow arrow = (EntityArrow) e;
                
                
                double dist = C.p().getDistanceToEntity(arrow);
                if (dist <= 10.0) {
                    double motionSq = arrow.motionX * arrow.motionX + arrow.motionY * arrow.motionY + arrow.motionZ * arrow.motionZ;
                    if (motionSq > 0.1) {
                        Vec3 arrowPos = new Vec3(arrow.posX, arrow.posY, arrow.posZ);
                        Vec3 arrowMotion = new Vec3(arrow.motionX, arrow.motionY, arrow.motionZ).normalize();
                        
                        boolean danger = false;
                        AxisAlignedBB playerBB = C.p().getEntityBoundingBox().expand(0.8, 0.8, 0.8);
                        
                        for (int i = 0; i < 20; i++) {
                            Vec3 checkPos = arrowPos.addVector(arrowMotion.xCoord * i, arrowMotion.yCoord * i, arrowMotion.zCoord * i);
                            if (isInside(playerBB, checkPos)) {
                                danger = true;
                                break;
                            }
                        }
                        
                        if (danger) {
                            dodge();
                            return; // Only dodge one arrow per tick
                        }
                    }
                }
            }
        }
    }

    private static boolean isInside(AxisAlignedBB bb, Vec3 vec) {
        return vec.xCoord >= bb.minX && vec.xCoord <= bb.maxX &&
               vec.yCoord >= bb.minY && vec.yCoord <= bb.maxY &&
               vec.zCoord >= bb.minZ && vec.zCoord <= bb.maxZ;
    }

    private static void dodge() {
        float yaw = C.p().rotationYaw;
        
        // Left
        double leftX = C.p().posX - Math.sin(Math.toRadians(yaw - 90)) * 1.5;
        double leftZ = C.p().posZ + Math.cos(Math.toRadians(yaw - 90)) * 1.5;
        
        // Right
        double rightX = C.p().posX - Math.sin(Math.toRadians(yaw + 90)) * 1.5;
        double rightZ = C.p().posZ + Math.cos(Math.toRadians(yaw + 90)) * 1.5;
        
        boolean leftSafe = isSafe(leftX, C.p().posY, leftZ);
        boolean rightSafe = isSafe(rightX, C.p().posY, rightZ);
        
        double speed = 0.5; // Dodge speed
        
        if (leftSafe && rightSafe) {
            // Default to left or whatever is further from arrow? Left is fine.
            strafe(yaw - 90, speed);
        } else if (leftSafe) {
            strafe(yaw - 90, speed);
        } else if (rightSafe) {
            strafe(yaw + 90, speed);
        }
    }

    private static void strafe(float angle, double speed) {
        C.p().motionX = -Math.sin(Math.toRadians(angle)) * speed;
        C.p().motionZ = Math.cos(Math.toRadians(angle)) * speed;
    }

    private static boolean isSafe(double x, double y, double z) {
        BlockPos pos = new BlockPos(x, y, z);
        Block head = C.w().getBlockState(pos.up()).getBlock();
        Block foot = C.w().getBlockState(pos).getBlock();
        
        if (!(head instanceof BlockAir) || !(foot instanceof BlockAir)) return false;
        
        for (int i = (int) y - 1; i > 0; i--) {
            Block block = C.w().getBlockState(new BlockPos(x, i, z)).getBlock();
            if (!(block instanceof BlockAir)) {
                if (block instanceof BlockLiquid || block instanceof BlockFire) {
                    return false;
                }
                return true; // Found solid ground
            }
        }
        return false; // Void
    }
}
