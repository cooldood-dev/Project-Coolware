package com.github.cooldood.utils.minecraft;

import com.github.cooldood.utils.client.C;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.init.Blocks;
import net.minecraft.util.*;

import java.util.Arrays;
import java.util.List;

public class WorldUtil {
    public static Vec3 getEyes(Vec3 vec3) {
        return new Vec3(vec3.xCoord, vec3.yCoord + C.p().getEyeHeight(), vec3.zCoord);
    }

    public static boolean isOverAir() {
        return isOverAir(C.p().getPositionVector());
    }

    public static boolean isOverAir(Vec3 vec3) {
        return C.w().getBlockState(new BlockPos(vec3.subtract(0,1,0))).getBlock().equals(Blocks.air);
    }

    public static MovingObjectPosition rayTrace(double blockReachDistance, RotationUtil.Rotation rotation) {
        return rayTrace(blockReachDistance, 1, rotation, rotation);
    }

    public static MovingObjectPosition rayTrace(double blockReachDistance, Vec3 playerPos, RotationUtil.Rotation rotation) {
        return rayTrace(blockReachDistance, 1, playerPos.add(new Vec3(0,C.p().getEyeHeight(),0)), rotation, rotation);
    }

    public static MovingObjectPosition rayTrace(double blockReachDistance, float partialTicks, RotationUtil.Rotation rotationPrev, RotationUtil.Rotation rotationCurrent) {
        return rayTrace(blockReachDistance, partialTicks, C.p().getPositionEyes(partialTicks), rotationPrev, rotationCurrent);
    }

    // from net.minecraft.entity.Entity
    public static MovingObjectPosition rayTrace(double blockReachDistance, float partialTicks, Vec3 position, RotationUtil.Rotation rotationPrev, RotationUtil.Rotation rotationCurrent) {
        float f = rotationPrev.pitch + (rotationCurrent.pitch - rotationPrev.pitch) * partialTicks;
        float f1 = rotationPrev.yaw + (rotationCurrent.yaw - rotationPrev.yaw) * partialTicks;
        Vec3 vec31 = getVectorForRotation(f, f1);

        Vec3 vec32 = position.addVector(vec31.xCoord * blockReachDistance, vec31.yCoord * blockReachDistance, vec31.zCoord * blockReachDistance);

        return C.p().worldObj.rayTraceBlocks(position, vec32, false, false, true);
    }

    // from net.minecraft.entity.Entity
    public static Vec3 getVectorForRotation(float pitch, float yaw) {
        float f = MathHelper.cos(-yaw * 0.017453292F - (float)Math.PI);
        float f1 = MathHelper.sin(-yaw * 0.017453292F - (float)Math.PI);
        float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3(f1 * f2, f3, f * f2);
    }

    // net.minecraft.client.renderer.EntityRenderer.getMouseOver
    public static Entity getMouseOver(RotationUtil.Rotation rotation, double reach, boolean ignoreWalls)
    {
        Entity entity = C.mc.getRenderViewEntity();

        float partialTicks = 1;

        if (entity != null)
        {
            if (C.mc.theWorld != null)
            {
                Entity pointedEntity = null;
                MovingObjectPosition objectMouseOver = ignoreWalls ? null : rayTrace(reach+2, rotation);
                double d1 = reach;
                Vec3 vec3 = entity.getPositionEyes(partialTicks);

                if (objectMouseOver != null)
                {
                    d1 = objectMouseOver.hitVec.distanceTo(vec3);
                }

                Vec3 vec31 = getVectorForRotation(rotation.pitch, rotation.yaw);
                Vec3 vec32 = vec3.addVector(vec31.xCoord * reach, vec31.yCoord * reach, vec31.zCoord * reach);
                Vec3 vec33 = null;
                float f = 1.0F;
                List<Entity> list = C.mc.theWorld.getEntitiesInAABBexcluding(entity, entity.getEntityBoundingBox().addCoord(vec31.xCoord * reach, vec31.yCoord * reach, vec31.zCoord * reach).expand((double)f, (double)f, (double)f), Predicates.and(EntitySelectors.NOT_SPECTATING, new Predicate<Entity>()
                {
                    public boolean apply(Entity p_apply_1_)
                    {
                        return p_apply_1_.canBeCollidedWith();
                    }
                }));
                double d2 = d1;

                for (int j = 0; j < list.size(); ++j)
                {
                    Entity entity1 = (Entity)list.get(j);
                    float f1 = entity1.getCollisionBorderSize();
                    AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().expand((double)f1, (double)f1, (double)f1);
                    MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec32);

                    if (axisalignedbb.isVecInside(vec3))
                    {
                        if (d2 >= 0.0D)
                        {
                            pointedEntity = entity1;
                            vec33 = movingobjectposition == null ? vec3 : movingobjectposition.hitVec;
                            d2 = 0.0D;
                        }
                    }
                    else if (movingobjectposition != null)
                    {
                        double d3 = vec3.distanceTo(movingobjectposition.hitVec);

                        if (d3 < d2 || d2 == 0.0D)
                        {
                            if (entity1 == entity.ridingEntity && !entity.canRiderInteract())
                            {
                                if (d2 == 0.0D)
                                {
                                    pointedEntity = entity1;
                                    vec33 = movingobjectposition.hitVec;
                                }
                            }
                            else
                            {
                                pointedEntity = entity1;
                                vec33 = movingobjectposition.hitVec;
                                d2 = d3;
                            }
                        }
                    }
                }

                if (pointedEntity != null && vec3.distanceTo(vec33) > reach)
                {
                    pointedEntity = null;
                    objectMouseOver = new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, vec33, (EnumFacing)null, new BlockPos(vec33));
                }

                if (pointedEntity != null && (d2 < d1 || objectMouseOver == null))
                {
                    if (pointedEntity instanceof EntityLivingBase || pointedEntity instanceof EntityItemFrame)
                    {
                        return pointedEntity;
                    }
                }

            }
        }

        return null;
    }

    public static BlockPos getBestBlockSurroundingBed(BlockPos blockPos) {
        IBlockState state = C.w().getBlockState(blockPos);

        EnumFacing facing = state.getValue(BlockBed.FACING);

        boolean isFoot = state.getValue(BlockBed.PART) == BlockBed.EnumPartType.FOOT;
        BlockPos headPos = !isFoot ? blockPos.offset(facing.getOpposite()) : blockPos;
        BlockPos footPos = isFoot ? blockPos.offset(facing) : blockPos;

        List<BlockPos> possiblePositions = Arrays.asList(
                headPos.offset(EnumFacing.UP),
                headPos.offset(facing.getOpposite()),
                headPos.offset(facing.rotateY()),
                headPos.offset(facing.rotateYCCW()),

                footPos.offset(EnumFacing.UP),
                footPos.offset(facing),
                footPos.offset(facing.rotateY()),
                footPos.offset(facing.rotateYCCW())
        );

        float bestBreakSpeed = 0;
        BlockPos bestPos = null;
        double bestDistance = 0;

        for (BlockPos pos : possiblePositions) {
            float breakSpeed = InventoryUtil.blockStrength(C.p().inventory.getStackInSlot(InventoryUtil.getBestSlotForBlock(pos)), pos);

            // non solid blocks are ignored
            if (!InventoryUtil.isFullBlock(C.w().getBlockState(pos).getBlock())) return null;

            double distance = C.p().getPositionEyes(1).distanceTo(new Vec3(pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5));

            if (breakSpeed > bestBreakSpeed || bestPos == null || (breakSpeed == bestBreakSpeed && bestDistance > distance)) {
                bestBreakSpeed = breakSpeed;
                bestPos = pos;
                bestDistance = distance;
            }
        }

        return bestPos;
    }

    public static Vec3 getClosestPointToBlock(BlockPos blockPos) {
        Block block = C.w().getBlockState(blockPos).getBlock();
        AxisAlignedBB collisionBox = block.getCollisionBoundingBox(C.w(), BlockPos.ORIGIN, block.getDefaultState());
        if (collisionBox == null) return null;

        return getClosestPoint(collisionBox.offset(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
    }

    public static Vec3 getClosestPoint(AxisAlignedBB collisionBox) {
        Vec3 eyePos = C.p().getPositionEyes(1);

        double posX = eyePos.xCoord;
        double posY = eyePos.yCoord;
        double posZ = eyePos.zCoord;

        if (eyePos.xCoord < collisionBox.minX) posX = collisionBox.minX;
        else if (eyePos.xCoord > collisionBox.maxX) posX = collisionBox.maxX;

        if (eyePos.yCoord < collisionBox.minY) posY = collisionBox.minY;
        else if (eyePos.yCoord > collisionBox.maxY) posY = collisionBox.maxY;

        if (eyePos.zCoord < collisionBox.minZ) posZ = collisionBox.minZ;
        else if (eyePos.zCoord > collisionBox.maxZ) posZ = collisionBox.maxZ;

        return new Vec3(posX, posY, posZ);
    }
}
