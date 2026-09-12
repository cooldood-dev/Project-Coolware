package com.github.cooldood.utils.minecraft;

import com.github.cooldood.utils.client.C;
import com.google.common.base.Predicates;
import net.minecraft.entity.Entity;
import net.minecraft.util.*;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class RayCastUtils {
    public static MovingObjectPosition rayCast(Vector2f rotation, float range) {
        return rayCast(rotation, (double) range);
    }

    public static MovingObjectPosition rayCast(Vector2f rotation, double range) {
        if (C.p() == null || C.w() == null || rotation == null) return null;

        float partialTicks = 1.0F;
        Vec3 eyes = C.p().getPositionEyes(partialTicks);
        Vec3 look = WorldUtil.getVectorForRotation(rotation.y, rotation.x);
        Vec3 reach = eyes.addVector(look.xCoord * range, look.yCoord * range, look.zCoord * range);

        MovingObjectPosition blockHit = C.w().rayTraceBlocks(eyes, reach, false, false, false);
        double d1 = range;
        if (blockHit != null) {
            d1 = blockHit.hitVec.distanceTo(eyes);
        }

        Entity pointedEntity = null;
        Vec3 hitVec = null;
        List<Entity> list = C.w().getEntitiesInAABBexcluding(
                C.p(),
                C.p().getEntityBoundingBox().addCoord(look.xCoord * range, look.yCoord * range, look.zCoord * range).expand(1.0, 1.0, 1.0),
                Predicates.and(EntitySelectors.NOT_SPECTATING, Entity::canBeCollidedWith)
        );

        for (Entity entity1 : list) {
            float f1 = entity1.getCollisionBorderSize() + 0.15F;
            AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().expand(f1, f1, f1);
            MovingObjectPosition intercept = axisalignedbb.calculateIntercept(eyes, reach);

            if (axisalignedbb.isVecInside(eyes)) {
                if (d1 >= 0.0D) {
                    pointedEntity = entity1;
                    hitVec = intercept == null ? eyes : intercept.hitVec;
                    d1 = 0.0D;
                }
            } else if (intercept != null) {
                double d3 = eyes.distanceTo(intercept.hitVec);
                if (d3 < d1 || d1 == 0.0D) {
                    pointedEntity = entity1;
                    hitVec = intercept.hitVec;
                    d1 = d3;
                }
            }
        }

        if (pointedEntity != null) {
            return new MovingObjectPosition(pointedEntity, hitVec != null ? hitVec : eyes);
        }

        return blockHit;
    }
}
