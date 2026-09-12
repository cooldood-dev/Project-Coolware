package com.github.cooldood.managers.impl;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.PlayerUpdateEvent;
import com.github.cooldood.events.impl.RespawnEvent;
import com.github.cooldood.events.impl.WorldUnloadEvent;
import com.github.cooldood.modules.impl.combat.KillAura;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.minecraft.RotationUtils;
import com.github.cooldood.utils.minecraft.TimerUtils;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;

import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TargetManager {

    @Setter
    private static EntityLivingBase target;

    @Getter
    private static List<Entity> targetList = new CopyOnWriteArrayList<>();

    private static TimerUtils switchTimer = new TimerUtils();

    @Getter
    @Setter
    private static Mode mode = Mode.ADAPTIVE;

    @Getter
    @Setter
    private static KillAura.Sorting sorting = KillAura.Sorting.Distance;

    @Setter
    private static List<Targets> targets = Arrays.asList(Targets.PLAYERS, Targets.HOSTILES);

    @Getter
    @Setter
    private static float seekRange = 6.0f;

    @Getter
    @Setter
    private static long switchDelay = 300L;

    @Getter
    @Setter
    private static int switchTime = 2;

    private static int targetIndex = 0;

    public TargetManager(float seekRange) {
        mode = Mode.ADAPTIVE;
        sorting = KillAura.Sorting.Distance;
        targets = Arrays.asList(Targets.PLAYERS, Targets.HOSTILES);
        TargetManager.seekRange = seekRange;
        switchDelay = 300L;
        switchTime = 2;
    }

    public TargetManager() {
        this(6.0f);
    }

    public static void configure(List<Targets> t) {
        targets = t;
    }

    public static EntityLivingBase getTarget() {
        return target;
    }

    public enum Mode {
        SINGLE("Single"),
        SWITCH("Switch"),
        ADAPTIVE("Adaptive");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum Targets {
        PLAYERS("Players"),
        TEAMMATES("Teammates"),
        INVISIBLES("Invisibles"),
        HOSTILES("Hostiles"),
        ANIMALS("Animals");

        public final String name;

        Targets(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @SubscribeEvent
    public static void onPreUpdate(PlayerUpdateEvent event) {
        updateTargets();
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldUnloadEvent event) {
        target = null;
        targetIndex = 0;
        switchTimer.reset();
    }

    @SubscribeEvent
    public static void onRespawn(RespawnEvent event) {
        target = null;
        targetIndex = 0;
        switchTimer.reset();
    }

    public static void updateTargets() {
        if (C.p() == null || C.w() == null) {
            target = null;
            targetList = new CopyOnWriteArrayList<>();
            return;
        }
        List<Entity> list = getTargets();
        list.sort(getComparator(sorting));
        targetList = new CopyOnWriteArrayList<>(list);
        if (targetList.isEmpty()) {
            target = null;
            targetIndex = 0;
            return;
        }
        selectTarget();
    }

    private static void selectTarget() {
        if (targetList.isEmpty()) {
            target = null;
            targetIndex = 0;
            return;
        }
        if (mode == Mode.SINGLE) {
            if (target == null || !targetList.contains(target)) {
                target = (EntityLivingBase) targetList.get(0);
            }
        } else if (mode == Mode.SWITCH) {
            if (targetIndex >= targetList.size()) {
                targetIndex = 0;
            }
            if (switchTimer.hasTimeElapsed(switchDelay, true)) {
                targetIndex = (targetIndex + 1) % targetList.size();
            }
            target = (EntityLivingBase) targetList.get(targetIndex);
        } else if (mode == Mode.ADAPTIVE) {
            target = (EntityLivingBase) targetList.get(0);
        } else {
            throw new IllegalStateException("Unexpected value: " + mode);
        }
    }

    public static void switchTarget() {
        if (targetList.isEmpty()) {
            target = null;
            targetIndex = 0;
            return;
        }
        targetIndex = (targetIndex + 1) % targetList.size();
        target = (EntityLivingBase) targetList.get(targetIndex);
        switchTimer.reset();
    }

    public static Comparator<Entity> getComparator(KillAura.Sorting sortMode) {
        if (sortMode == null) sortMode = KillAura.Sorting.Distance;
        switch (sortMode) {
            case Health:
                return Comparator.comparingDouble((Entity e) -> {
                    if (e instanceof EntityLivingBase) {
                        EntityLivingBase el = (EntityLivingBase) e;
                        return el.getHealth() + el.getAbsorptionAmount();
                    }
                    return Double.MAX_VALUE;
                }).thenComparingDouble(TargetManager::getDistance);
            case Angle:
                return Comparator.comparingDouble((Entity e) -> {
                    if (C.p() == null || !(e instanceof EntityLivingBase)) return Double.MAX_VALUE;
                    Vec3 eyes = C.p().getPositionEyes(1.0f);
                    Vec3 targetPos = new Vec3(e.posX, e.posY + e.getEyeHeight() * 0.75, e.posZ);
                    float[] rot = RotationUtils.getRotationsTo(eyes, targetPos);
                    float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(rot[0] - C.p().rotationYaw));
                    float pitchDiff = Math.abs(MathHelper.wrapAngleTo180_float(rot[1] - C.p().rotationPitch));
                    return Math.hypot(yawDiff, pitchDiff);
                }).thenComparingDouble(TargetManager::getDistance);
            case HurtTime:
                return Comparator.comparingInt((Entity e) -> {
                    if (e instanceof EntityLivingBase) {
                        return ((EntityLivingBase) e).hurtResistantTime;
                    }
                    return Integer.MAX_VALUE;
                }).thenComparingDouble(TargetManager::getDistance);
            case Distance:
            default:
                return Comparator.comparingDouble(TargetManager::getDistance);
        }
    }

    public static double getDistance(Entity e) {
        if (C.p() == null || e == null) return 999.0;
        Vec3 eyes = C.p().getPositionEyes(1.0f);
        AxisAlignedBB box = e.getEntityBoundingBox();
        double closestX = MathHelper.clamp_double(eyes.xCoord, box.minX, box.maxX);
        double closestY = MathHelper.clamp_double(eyes.yCoord, box.minY, box.maxY);
        double closestZ = MathHelper.clamp_double(eyes.zCoord, box.minZ, box.maxZ);
        return Math.min(C.p().getDistanceToEntity(e), eyes.distanceTo(new Vec3(closestX, closestY, closestZ)));
    }

    public static List<Entity> getTargets() {
        if (C.w() == null || C.p() == null) return new CopyOnWriteArrayList<>();
        return C.w().loadedEntityList.stream()
                .filter(e -> e instanceof EntityLivingBase)
                .filter(e -> e != C.p())
                .filter(e -> !e.isDead)
                .filter(e -> ((EntityLivingBase) e).getHealth() > 0)
                .filter(e -> getDistance(e) <= seekRange)
                .filter(TargetManager::isValidEntity)
                .collect(Collectors.toList());
    }

    public static boolean isValidEntity(Entity entity) {
        if (entity instanceof EntityArmorStand) {
            return false;
        }
        if (entity.isInvisible() && (targets == null || !targets.contains(Targets.INVISIBLES))) {
            return false;
        }
        if (entity instanceof EntityPlayer) {
            boolean teammate = inTeam(C.p(), entity);
            return teammate ? (targets != null && targets.contains(Targets.TEAMMATES)) : (targets != null && targets.contains(Targets.PLAYERS));
        }
        if (targets != null && targets.contains(Targets.HOSTILES) && entity instanceof EntityMob) {
            return true;
        }
        if (targets != null && targets.contains(Targets.ANIMALS) && entity instanceof EntityAnimal) {
            return true;
        }
        return false;
    }

    public static boolean inTeam(ICommandSender a, ICommandSender b) {
        if (a == null || b == null) return false;
        if (a == b) return true;
        if (a instanceof EntityLivingBase && b instanceof EntityLivingBase) {
            EntityLivingBase elA = (EntityLivingBase) a;
            EntityLivingBase elB = (EntityLivingBase) b;
            if (elA.isOnSameTeam(elB)) return true;
            if (elA.getTeam() instanceof ScorePlayerTeam && elB.getTeam() instanceof ScorePlayerTeam) {
                ScorePlayerTeam sptA = (ScorePlayerTeam) elA.getTeam();
                ScorePlayerTeam sptB = (ScorePlayerTeam) elB.getTeam();
                if (sptA.isSameTeam(sptB)) return true;
                String prefixA = sptA.getColorPrefix();
                String prefixB = sptB.getColorPrefix();
                if (prefixA != null && prefixB != null && !prefixA.isEmpty() && prefixA.equals(prefixB)) {
                    return true;
                }
            }
        }
        String colorA = teamColor(a);
        String colorB = teamColor(b);
        if (colorA != null && colorB != null && !colorA.equalsIgnoreCase("f") && !colorA.equalsIgnoreCase("7") && !colorA.equalsIgnoreCase("r")) {
            return colorA.equalsIgnoreCase(colorB);
        }
        return false;
    }

    private static String teamColor(ICommandSender player) {
        if (player == null) return null;
        String formatted = player.getDisplayName().getFormattedText();
        Matcher m = Pattern.compile("\u00a7([0-9a-fk-or])").matcher(formatted);
        return m.find() ? m.group(1) : null;
    }
}
