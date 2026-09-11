package com.github.cooldood.utils.minecraft;

import com.github.cooldood.bridge.net.minecraft.DataWatchedBridge;
import com.github.cooldood.bridge.net.minecraft.client.entity.AbstractClientPlayerBridge;
import com.github.cooldood.modules.impl.client.Targets;
import com.github.cooldood.utils.client.C;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.network.NetworkPlayerInfo;
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

public class TargetUtil {
    public static boolean isValidTarget(Entity entity, boolean visual) {
        return entity != C.p() && entity instanceof EntityPlayer && !entity.isDead && !isBot(entity) && !isTeam(entity, visual);
    }

    public static boolean isBot(Entity entity) {
        if (!Targets.antiBot || !(entity instanceof EntityPlayer)) return false;
        if (!(entity instanceof AbstractClientPlayer)) return true;

        NetworkPlayerInfo playerInfo = AbstractClientPlayerBridge.from(entity).bridge$getPlayerInfo();
        if (playerInfo == null) return true;

        return playerInfo.getResponseTime() <= Targets.botPingMode.ping;
    }

    public static boolean isTeam(Entity entity, boolean visual) {
        if (!Targets.teamsCheck || !(entity instanceof EntityLivingBase)) return false;
        if (visual && Targets.showVisuals) return false;

        EntityLivingBase entityLivingBase = (EntityLivingBase) entity;

        return Targets.clientCheck && C.p().isOnSameTeam(entityLivingBase)
                || (Targets.colourTeams && getTeamColour(C.p()) == getTeamColour(entityLivingBase));
    }

    // stolen from net.minecraft.client.renderer.entity.RendererLivingEntity.setScoreTeamColor
    private static int getTeamColour(EntityLivingBase entityLivingBaseIn) {
        ScorePlayerTeam scoreplayerteam = (ScorePlayerTeam)entityLivingBaseIn.getTeam();

        if (scoreplayerteam != null)
        {
            String s = FontRenderer.getFormatFromString(scoreplayerteam.getColorPrefix());

            if (s.length() >= 2)
            {
                if ("0123456789abcdef".indexOf(s.charAt(1)) != -1) {
                    return C.mc.fontRendererObj.getColorCode(s.charAt(1));
                }
                return -1;
            }
        }

        return -1;
    }

    public static List<EntityLivingBase> getAllValidTargets(boolean visual) {
        return C.w().getEntities(EntityLivingBase.class, (entity) -> isValidTarget(entity, visual));
    }

    public static List<EntityLivingBase> getPossibleTargets(double reach, boolean throughWalls, boolean rotate) {
        return C.w().getEntities(EntityLivingBase.class, entity -> canEntityBeHit(entity, reach, throughWalls, rotate));
    }

    public static boolean canEntityBeHit(EntityLivingBase entity, double range, boolean throughWalls, boolean rotate) {
        return isValidTarget(entity, false)
                && getDistanceToEntity(entity) <= range
                // if the rotation code wont lock onto the entity, dont bother targeting them
                && (!rotate || getTargetRotationPoint(  entity, range, throughWalls, false) != null);
    }

    public static double getDistanceToEntity(EntityLivingBase entity) {
        return C.p().getPositionEyes(1).distanceTo(getClosestPointToEntity(entity));
    }

    public static Vec3 getClosestPointToEntity(EntityLivingBase target) {
        return WorldUtil.getClosestPoint(target.getEntityBoundingBox());
    }

    public static Vec3 getTargetRotationPoint(EntityLivingBase entity, double range, boolean throughWalls, boolean randomValid) {
        AxisAlignedBB targetBoundingBox = entity.getEntityBoundingBox();

        Vec3 closestPoint = TargetUtil.getClosestPointToEntity(entity).subtract(0, 0, 0);
        if (!randomValid) {
            RotationUtil.Rotation bestRotation = RotationUtil.getRotation(closestPoint);

            // if best rotation works, lock it in.
            if (WorldUtil.getMouseOver(bestRotation, range, throughWalls) == entity) {
                return closestPoint;
            }
        }

        ArrayList<Vec3> possibleRotations = new ArrayList<>();
        // added in binary order (000, 100, 010, 110, 001, 101, 011, 111)
        for (int i = 0; i < 8; i++) {
            possibleRotations.add(new Vec3(
                    i % 2 == 0 ? targetBoundingBox.minX : targetBoundingBox.maxX,
                    i % 4 >= 2 ? targetBoundingBox.minY : targetBoundingBox.maxY,
                    i % 8 >= 4 ? targetBoundingBox.minZ : targetBoundingBox.maxZ
            ));
        }

        // maybe the middle is meta
        possibleRotations.add(new Vec3(entity.posX, (targetBoundingBox.maxY + targetBoundingBox.minY) / 2, entity.posZ));
        possibleRotations.add(new Vec3(entity.posX, targetBoundingBox.minY, entity.posZ));
        possibleRotations.add(new Vec3(entity.posX, targetBoundingBox.maxY, entity.posZ));
        possibleRotations.add(closestPoint);
        possibleRotations.sort(Comparator.comparingDouble(randomValid ? point -> Math.random() : point -> C.p().getPositionEyes(1).distanceTo(point)));

        // try other rotations, surely one will work
        for (Vec3 possibleRotationVector : possibleRotations) {
            RotationUtil.Rotation possibleRotation = RotationUtil.getRotation(possibleRotationVector);
            if (WorldUtil.getMouseOver(possibleRotation, range, throughWalls) == entity) {
                return possibleRotationVector;
            }
        }

        // after all that, we still fail.
        return null;
    }

    public static float getAbsorption(EntityLivingBase entity) {
        DataWatcher.WatchableObject absorptionTag = DataWatchedBridge.from(entity.getDataWatcher()).bridge$getWatchedObject(17);
        return absorptionTag.getObject() instanceof Float ? (float)absorptionTag.getObject() : 0;
    }
}
