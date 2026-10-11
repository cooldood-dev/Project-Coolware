package coolware.util;

import coolware.module.combat.KillAura;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.network.NetworkPlayerInfo;
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
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean isValidTarget(Entity entity, boolean visual) {
        return entity != mc.thePlayer && entity instanceof EntityPlayer && !entity.isDead && !isBot(entity) && !isTeam(entity, visual);
    }

    public static boolean isBot(Entity entity) {
        if (KillAura.antiBot == null || !KillAura.antiBot.isEnabled() || !(entity instanceof EntityPlayer)) return false;
        if (!(entity instanceof AbstractClientPlayer)) return true;

        AbstractClientPlayer player = (AbstractClientPlayer) entity;
        NetworkPlayerInfo playerInfo = player.getPlayerInfo();
        if (playerInfo == null) return true;

        int threshold = 0;
        String mode = KillAura.botPingMode != null ? KillAura.botPingMode.getMode() : "Hypixel";
        if ("Negative".equalsIgnoreCase(mode)) {
            threshold = -1;
        } else if ("Cracked".equalsIgnoreCase(mode)) {
            threshold = 1;
        } else {
            threshold = 0;
        }

        return playerInfo.getResponseTime() <= threshold;
    }

    public static boolean isTeam(Entity entity, boolean visual) {
        if (KillAura.teamsCheck == null || !KillAura.teamsCheck.isEnabled() || !(entity instanceof EntityLivingBase)) return false;

        EntityLivingBase entityLivingBase = (EntityLivingBase) entity;

        boolean clientCheck = KillAura.clientTeams != null && KillAura.clientTeams.isEnabled();
        boolean colourTeams = KillAura.colourTeams != null && KillAura.colourTeams.isEnabled();

        return (clientCheck && mc.thePlayer.isOnSameTeam(entityLivingBase))
                || (colourTeams && getTeamColour(mc.thePlayer) == getTeamColour(entityLivingBase));
    }

    private static int getTeamColour(EntityLivingBase entityLivingBaseIn) {
        if (entityLivingBaseIn == null) return -1;
        ScorePlayerTeam scoreplayerteam = (ScorePlayerTeam) entityLivingBaseIn.getTeam();

        if (scoreplayerteam != null) {
            String s = FontRenderer.getFormatFromString(scoreplayerteam.getColorPrefix());
            if (s.length() >= 2) {
                if ("0123456789abcdef".indexOf(s.charAt(1)) != -1) {
                    return mc.fontRendererObj.getColorCode(s.charAt(1));
                }
                return -1;
            }
        }
        return -1;
    }

    public static double getDistanceToEntity(EntityLivingBase entity) {
        if (mc.thePlayer == null || entity == null) return 0.0D;
        return mc.thePlayer.getPositionEyes(1.0F).distanceTo(getClosestPointToEntity(entity));
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

        ArrayList<Vec3> possibleRotations = new ArrayList<Vec3>();
        for (int i = 0; i < 8; i++) {
            possibleRotations.add(new Vec3(
                    i % 2 == 0 ? targetBoundingBox.minX : targetBoundingBox.maxX,
                    i % 4 >= 2 ? targetBoundingBox.minY : targetBoundingBox.maxY,
                    i % 8 >= 4 ? targetBoundingBox.minZ : targetBoundingBox.maxZ
            ));
        }

        possibleRotations.add(new Vec3(entity.posX, (targetBoundingBox.maxY + targetBoundingBox.minY) / 2.0D, entity.posZ));
        possibleRotations.add(new Vec3(entity.posX, targetBoundingBox.minY, entity.posZ));
        possibleRotations.add(new Vec3(entity.posX, targetBoundingBox.maxY, entity.posZ));
        possibleRotations.add(closestPoint);

        if (randomValid) {
            possibleRotations.sort(Comparator.comparingDouble(point -> Math.random()));
        } else {
            possibleRotations.sort(Comparator.comparingDouble(point -> mc.thePlayer.getPositionEyes(1.0F).distanceTo(point)));
        }

        for (Vec3 possibleRotationVector : possibleRotations) {
            RotationUtil.Rotation possibleRotation = RotationUtil.getRotation(possibleRotationVector);
            if (WorldUtil.getMouseOver(possibleRotation, range, throughWalls) == entity) {
                return possibleRotationVector;
            }
        }

        return null;
    }

    public static float getAbsorption(EntityLivingBase entity) {
        if (entity == null) return 0.0F;
        return entity.getAbsorptionAmount();
    }
}
