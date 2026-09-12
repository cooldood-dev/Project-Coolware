package com.github.cooldood.utils.minecraft;

import com.github.cooldood.bridge.net.minecraft.DataWatchedBridge;
import com.github.cooldood.modules.impl.combat.AntiBot;
import com.github.cooldood.utils.client.C;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.DataWatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * AntiBotUtil - Centralized target validation and entity filtering with AntiBot integration.
 */
public class AntiBotUtil {

    public static boolean isValidTarget(Entity entity, boolean visual) {
        return entity != C.p()
                && entity instanceof EntityPlayer
                && !entity.isDead
                && !isBot(entity)
                && !isTeam(entity, visual);
    }

    public static boolean isBot(Entity entity) {
        if (!(entity instanceof EntityLivingBase)) return false;
        return AntiBot.isBot((EntityLivingBase) entity);
    }

    public static boolean isTeam(Entity entity, boolean visual) {
        if (!(entity instanceof EntityLivingBase)) return false;
        EntityLivingBase entityLivingBase = (EntityLivingBase) entity;

        return C.p() != null && (C.p().isOnSameTeam(entityLivingBase)
                || getTeamColour(C.p()) == getTeamColour(entityLivingBase));
    }

    private static int getTeamColour(EntityLivingBase entityLivingBaseIn) {
        ScorePlayerTeam scoreplayerteam = (ScorePlayerTeam) entityLivingBaseIn.getTeam();

        if (scoreplayerteam != null) {
            String s = FontRenderer.getFormatFromString(scoreplayerteam.getColorPrefix());

            if (s.length() >= 2) {
                if ("0123456789abcdef".indexOf(s.charAt(1)) != -1) {
                    return C.mc.fontRendererObj.getColorCode(s.charAt(1));
                }
                return -1;
            }
        }

        return -1;
    }

    public static List<EntityLivingBase> getAllValidTargets(boolean visual) {
        if (C.w() == null) return new ArrayList<>();
        return C.w().getEntities(EntityLivingBase.class, (entity) -> isValidTarget(entity, visual));
    }

    public static List<EntityLivingBase> getPossibleTargets(double reach, boolean throughWalls, boolean rotate) {
        if (C.w() == null) return new ArrayList<>();
        return C.w().getEntities(EntityLivingBase.class, entity -> canEntityBeHit(entity, reach, throughWalls, rotate));
    }

    public static boolean canEntityBeHit(EntityLivingBase entity, double range, boolean throughWalls, boolean rotate) {
        return isValidTarget(entity, false)
                && getDistanceToEntity(entity) <= range
                && (!rotate || getTargetRotationPoint(entity, range, throughWalls, false) != null);
    }

    public static double getDistanceToEntity(EntityLivingBase entity) {
        if (C.p() == null || entity == null) return 0.0;
        return C.p().getPositionEyes(1).distanceTo(getClosestPointToEntity(entity));
    }

    public static Vec3 getClosestPointToEntity(EntityLivingBase target) {
        return WorldUtil.getClosestPoint(target.getEntityBoundingBox());
    }

    public static Vec3 getTargetRotationPoint(EntityLivingBase entity, double range, boolean throughWalls, boolean randomValid) {
        AxisAlignedBB targetBoundingBox = entity.getEntityBoundingBox();

        Vec3 closestPoint = getClosestPointToEntity(entity);
        if (!randomValid) {
            RotationUtil.Rotation bestRotation = RotationUtil.getRotation(closestPoint);
            if (WorldUtil.getMouseOver(bestRotation, range, throughWalls) == entity) {
                return closestPoint;
            }
        }

        ArrayList<Vec3> possibleRotations = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            possibleRotations.add(new Vec3(
                    i % 2 == 0 ? targetBoundingBox.minX : targetBoundingBox.maxX,
                    i % 4 >= 2 ? targetBoundingBox.minY : targetBoundingBox.maxY,
                    i % 8 >= 4 ? targetBoundingBox.minZ : targetBoundingBox.maxZ
            ));
        }

        possibleRotations.add(new Vec3(entity.posX, (targetBoundingBox.maxY + targetBoundingBox.minY) / 2, entity.posZ));
        possibleRotations.add(new Vec3(entity.posX, targetBoundingBox.minY, entity.posZ));
        possibleRotations.add(new Vec3(entity.posX, targetBoundingBox.maxY, entity.posZ));
        possibleRotations.add(closestPoint);
        possibleRotations.sort(Comparator.comparingDouble(randomValid ? point -> Math.random() : point -> C.p().getPositionEyes(1).distanceTo(point)));

        for (Vec3 possibleRotationVector : possibleRotations) {
            RotationUtil.Rotation possibleRotation = RotationUtil.getRotation(possibleRotationVector);
            if (WorldUtil.getMouseOver(possibleRotation, range, throughWalls) == entity) {
                return possibleRotationVector;
            }
        }

        return null;
    }

    public static float getAbsorption(EntityLivingBase entity) {
        if (entity == null || entity.getDataWatcher() == null) return 0.0f;
        DataWatcher.WatchableObject absorptionTag = DataWatchedBridge.from(entity.getDataWatcher()).bridge$getWatchedObject(17);
        return absorptionTag != null && absorptionTag.getObject() instanceof Float ? (float) absorptionTag.getObject() : 0.0f;
    }
}
