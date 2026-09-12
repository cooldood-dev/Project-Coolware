package com.github.cooldood.managers.impl;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.PlayerUpdateEvent;
import com.github.cooldood.events.impl.RespawnEvent;
import com.github.cooldood.events.impl.WorldUnloadEvent;
import com.github.cooldood.utils.client.C;
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

    @Setter
    private static List<Targets> targets = Arrays.asList(Targets.PLAYERS, Targets.HOSTILES);

    @Getter
    @Setter
    private static float seekRange = 6.0f;

    @Getter
    @Setter
    private static int switchTime = 2;

    private static int targetIndex = 0;

    public TargetManager(float seekRange) {
        mode = Mode.ADAPTIVE;
        targets = Arrays.asList(Targets.PLAYERS, Targets.HOSTILES);
        TargetManager.seekRange = seekRange;
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
    }

    @SubscribeEvent
    public static void onRespawn(RespawnEvent event) {
        target = null;
    }

    public static void updateTargets() {
        if (C.p() == null || C.w() == null) {
            target = null;
            return;
        }
        targetList = getTargets();
        if (targetList.isEmpty()) {
            target = null;
            return;
        }
        selectTarget();
    }

    private static void selectTarget() {
        if (targetList.isEmpty()) {
            target = null;
            return;
        }
        if (mode == Mode.SINGLE) {
            target = (EntityLivingBase) targetList.get(0);
        } else if (mode == Mode.SWITCH) {
            if (targetIndex >= targetList.size()) {
                targetIndex = 0;
            }
            if (switchTimer.hasTimeElapsed(switchTime * 100L, true)) {
                targetIndex = (targetIndex + 1) % targetList.size();
                switchTimer.reset();
            }
            target = (EntityLivingBase) targetList.get(targetIndex);
        } else if (mode == Mode.ADAPTIVE) {
            target = (EntityLivingBase) targetList.stream()
                    .min(Comparator.comparingDouble(e -> C.p().getDistanceToEntity(e)))
                    .orElse(null);
        } else {
            throw new IllegalStateException("Unexpected value: " + mode);
        }
    }

    public static List<Entity> getTargets() {
        if (C.w() == null || C.p() == null) return new CopyOnWriteArrayList<>();
        return C.w().loadedEntityList.stream()
                .filter(e -> e instanceof EntityLivingBase)
                .filter(e -> e != C.p())
                .filter(e -> !e.isDead)
                .filter(e -> ((EntityLivingBase) e).getHealth() > 0)
                .filter(e -> C.p().getDistanceToEntity(e) <= seekRange)
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

    public static boolean inTeam(@NonNull ICommandSender a, @NonNull ICommandSender b) {
        String s = "\u00a7" + teamColor(a);
        return a.getDisplayName().getFormattedText().contains(s) && b.getDisplayName().getFormattedText().contains(s);
    }

    private static @NonNull String teamColor(@NonNull ICommandSender player) {
        Matcher m = Pattern.compile("\u00a7(.).*\u00a7r").matcher(player.getDisplayName().getFormattedText());
        return m.find() ? m.group(1) : "f";
    }
}
