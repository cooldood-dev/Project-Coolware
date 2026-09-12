package com.github.cooldood.managers.impl;

import com.github.cooldood.utils.minecraft.MathUtils;
import com.github.cooldood.utils.minecraft.PolarNoiseUtils;
import com.github.cooldood.utils.minecraft.RotationUtils;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import com.github.cooldood.utils.client.C;

public class PolarRotationManager {

    private static final long SEED = MathUtils.getRandomInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
    private static final Random POLAR_RNG = new Random(SEED);

    // Flick
    private static boolean flicking;
    private static long flickStartTime, flickDurationMs;
    private static float flickDirectionYaw, flickDirectionPitch, flickSpeed, flickRecoverySpeed, flickStrength;

    private static double noiseTime = POLAR_RNG.nextDouble() * 10000;
    private static double noiseDriftX = POLAR_RNG.nextDouble() * 1000;
    private static double noiseDriftY = POLAR_RNG.nextDouble() * 1000;

    // Chaos
    private static double chaosFreqA, chaosFreqB, chaosPhaseA, chaosPhaseB, chaosAmpA, chaosAmpB;

    // Orbit1
    private static double orbitFreq, orbitPhase, orbitRadiusYaw, orbitRadiusPitch;
    private static long orbitReseedAt;

    // Orbit2
    private static double orbit2Freq, orbit2Phase, orbit2RadiusYaw, orbit2RadiusPitch;
    private static long orbit2ReseedAt;

    // Orbit3
    private static double orbit3Freq, orbit3Phase, orbit3RadiusYaw, orbit3RadiusPitch;
    private static long orbit3ReseedAt;

    // Lissajous
    private static double lissFreqX, lissFreqY, lissPhase, lissAmpX, lissAmpY;
    private static long lissReseedAt;

    // Spirograph
    private static double spiroPetal, spiroAmp, spiroPhase;
    private static long spiroReseedAt;

    // Walk
    private static double walkX, walkY, walkTargetX, walkTargetY, walkSpeed;
    private static long walkReseedAt;

    // Logistic
    private static double logisticR, logisticX;
    private static long logisticReseedAt;

    // Beats
    private static double beatFreqA, beatFreqB, beatPhase;
    private static long beatReseedAt;

    // Axis
    private static double yawNoiseAmp, yawNoiseFreq, pitchNoiseAmp, pitchNoiseFreq;
    private static long axisReseedAt;

    // Base
    private static double baseBlend;
    private static long baseReseedAt;

    private static Vec3 heldPoint;
    private static long holdUntil;
    private static long nextReseedAt;

    // Weights
    private static double weightAim, weightCenter, weightEye;
    private static long weightReseedAt;

    static {
        reseedChaos();
        reseedOrbit();
        reseedWeights();
        reseedOrbit2();
        reseedOrbit3();
        reseedLissajous();
        reseedSpirograph();
        reseedWalk();
        reseedLogistic();
        reseedBeats();
        reseedAxis();
        reseedBase();
    }

    private static void reseedChaos() {
        chaosFreqA = 0.0015 + POLAR_RNG.nextDouble() * 0.003;
        chaosFreqB = 0.002 + POLAR_RNG.nextDouble() * 0.004;
        chaosPhaseA = POLAR_RNG.nextDouble() * Math.PI * 2;
        chaosPhaseB = POLAR_RNG.nextDouble() * Math.PI * 2;
        chaosAmpA = 0.4 + POLAR_RNG.nextDouble() * 0.8;
        chaosAmpB = 0.2 + POLAR_RNG.nextDouble() * 0.5;
    }

    private static void reseedOrbit() {
        long now = System.currentTimeMillis();
        orbitFreq = 0.0006 + POLAR_RNG.nextDouble() * 0.0014;
        orbitPhase = POLAR_RNG.nextDouble() * Math.PI * 2;
        orbitRadiusYaw = 0.3 + POLAR_RNG.nextDouble() * 1.1;
        orbitRadiusPitch = 0.15 + POLAR_RNG.nextDouble() * 0.6;
        orbitReseedAt = now + 2000 + POLAR_RNG.nextInt(4000);
    }

    private static void reseedWeights() {
        long now = System.currentTimeMillis();
        weightAim = 3.0 + (POLAR_RNG.nextDouble() - 0.5) * 1.6;
        weightCenter = 2.0 + (POLAR_RNG.nextDouble() - 0.5) * 1.2;
        weightEye = 0.3 + (POLAR_RNG.nextDouble() - 0.5) * 0.3;
        weightReseedAt = now + 800 + POLAR_RNG.nextInt(1600);
    }

    private static void reseedOrbit2() {
        long now = System.currentTimeMillis();
        orbit2Freq = 0.0009 + POLAR_RNG.nextDouble() * 0.0025;
        orbit2Phase = POLAR_RNG.nextDouble() * Math.PI * 2;
        orbit2RadiusYaw = 0.1 + POLAR_RNG.nextDouble() * 0.7;
        orbit2RadiusPitch = 0.05 + POLAR_RNG.nextDouble() * 0.4;
        orbit2ReseedAt = now + 900 + POLAR_RNG.nextInt(2600);
    }

    private static void reseedOrbit3() {
        long now = System.currentTimeMillis();
        orbit3Freq = 0.0012 + POLAR_RNG.nextDouble() * 0.003;
        orbit3Phase = POLAR_RNG.nextDouble() * Math.PI * 2;
        orbit3RadiusYaw = 0.05 + POLAR_RNG.nextDouble() * 0.45;
        orbit3RadiusPitch = 0.03 + POLAR_RNG.nextDouble() * 0.25;
        orbit3ReseedAt = now + 600 + POLAR_RNG.nextInt(2000);
    }

    private static void reseedLissajous() {
        long now = System.currentTimeMillis();
        lissFreqX = 1 + POLAR_RNG.nextInt(6);
        lissFreqY = 1 + POLAR_RNG.nextInt(6);
        lissPhase = POLAR_RNG.nextDouble() * Math.PI * 2;
        lissAmpX = 0.1 + POLAR_RNG.nextDouble() * 0.5;
        lissAmpY = 0.05 + POLAR_RNG.nextDouble() * 0.3;
        lissReseedAt = now + 1200 + POLAR_RNG.nextInt(3000);
    }

    private static void reseedSpirograph() {
        long now = System.currentTimeMillis();
        spiroPetal = 2 + POLAR_RNG.nextInt(6);
        spiroAmp = 0.1 + POLAR_RNG.nextDouble() * 0.6;
        spiroPhase = POLAR_RNG.nextDouble() * Math.PI * 2;
        spiroReseedAt = now + 1400 + POLAR_RNG.nextInt(3200);
    }

    private static void reseedWalk() {
        long now = System.currentTimeMillis();
        walkX = 0;
        walkY = 0;
        walkTargetX = (POLAR_RNG.nextDouble() - 0.5) * 2;
        walkTargetY = (POLAR_RNG.nextDouble() - 0.5) * 2;
        walkSpeed = 0.01 + POLAR_RNG.nextDouble() * 0.04;
        walkReseedAt = now + 700 + POLAR_RNG.nextInt(1800);
    }

    private static void reseedLogistic() {
        long now = System.currentTimeMillis();
        logisticR = 3.6 + POLAR_RNG.nextDouble() * 0.4;
        logisticX = POLAR_RNG.nextDouble();
        logisticReseedAt = now + 1000 + POLAR_RNG.nextInt(2600);
    }

    private static void reseedBeats() {
        long now = System.currentTimeMillis();
        beatFreqA = 0.0008 + POLAR_RNG.nextDouble() * 0.0015;
        beatFreqB = beatFreqA * (0.7 + POLAR_RNG.nextDouble() * 0.6);
        beatPhase = POLAR_RNG.nextDouble() * Math.PI * 2;
        beatReseedAt = now + 1500 + POLAR_RNG.nextInt(3000);
    }

    private static void reseedAxis() {
        long now = System.currentTimeMillis();
        yawNoiseAmp = 0.3 + POLAR_RNG.nextDouble() * 1.2;
        yawNoiseFreq = 0.4 + POLAR_RNG.nextDouble() * 1.4;
        pitchNoiseAmp = 0.15 + POLAR_RNG.nextDouble() * 0.7;
        pitchNoiseFreq = 0.3 + POLAR_RNG.nextDouble() * 1.2;
        axisReseedAt = now + 500 + POLAR_RNG.nextInt(1500);
    }

    private static void reseedBase() {
        long now = System.currentTimeMillis();
        baseBlend = 0.2 + POLAR_RNG.nextDouble() * 0.8;
        baseReseedAt = now + 900 + POLAR_RNG.nextInt(2200);
    }

    public static void reset() {
        flicking = false;
        heldPoint = null;
        holdUntil = 0;
        nextReseedAt = 0;
        reseedChaos();
        reseedOrbit();
        reseedWeights();
        reseedOrbit2();
        reseedOrbit3();
        reseedLissajous();
        reseedSpirograph();
        reseedWalk();
        reseedLogistic();
        reseedBeats();
        reseedAxis();
        reseedBase();
    }

    public static void resetFlick() {
        flicking = false;
    }

    public static void triggerFlick(float flickChance) {
        if (flicking) return;
        if (flickChance <= 0) return;
        if (MathUtils.getRandom(0.0, 100.0) > flickChance) return;
        long now = System.currentTimeMillis();
        float intensity = flickChance / 100.0f;
        float roll = POLAR_RNG.nextFloat();
        float rf = POLAR_RNG.nextFloat();
        if (intensity > 0.7f) {
            flickStrength = 10 + roll * 16;
            flickSpeed = 4.5f + rf * 3.5f;
            flickRecoverySpeed = 2 + rf * 2;
            flickDurationMs = 130 + POLAR_RNG.nextInt(140);
        } else if (intensity > 0.3f) {
            flickStrength = 5 + roll * 10;
            flickSpeed = 3 + rf * 2.5f;
            flickRecoverySpeed = 1.5f + rf * 1.5f;
            flickDurationMs = 160 + POLAR_RNG.nextInt(200);
        } else {
            flickStrength = 2 + roll * 5;
            flickSpeed = 2 + rf * 2;
            flickRecoverySpeed = 1 + rf * 1;
            flickDurationMs = 200 + POLAR_RNG.nextInt(260);
        }
        flicking = true;
        flickStartTime = now;
        flickDirectionYaw = POLAR_RNG.nextBoolean() ? 1f : -1f;
        flickDirectionPitch = POLAR_RNG.nextFloat() - 0.5f;
    }

    public static Vector2f getPolarRotation(EntityLivingBase target, double noiseScale, double warpStrength, int noiseOctaves, float flickChance, double seekRange) {
        if (target == null || C.p() == null) return null;
        long now = System.currentTimeMillis();
        AxisAlignedBB box = target.getEntityBoundingBox();
        Vec3 eyePos = new Vec3(C.p().posX, C.p().posY + C.p().getEyeHeight(), C.p().posZ);

        if (now >= weightReseedAt) reseedWeights();
        if (now >= orbitReseedAt) reseedOrbit();
        if (now >= orbit2ReseedAt) reseedOrbit2();
        if (now >= orbit3ReseedAt) reseedOrbit3();
        if (now >= lissReseedAt) reseedLissajous();
        if (now >= spiroReseedAt) reseedSpirograph();
        if (now >= walkReseedAt) reseedWalk();
        if (now >= logisticReseedAt) reseedLogistic();
        if (now >= beatReseedAt) reseedBeats();
        if (now >= axisReseedAt) reseedAxis();
        if (now >= baseReseedAt) reseedBase();

        Vec3 bestPoint;
        if (heldPoint != null && now < holdUntil) {
            bestPoint = heldPoint;
        } else {
            List<Vec3> scan = generateBodyScan(box);
            scan = subdivideByDistance(scan);
            scan = applyNoiseTransform(scan, noiseScale, warpStrength, noiseOctaves);
            bestPoint = selectWeightedPoint(scan, eyePos, target, seekRange);
            if (bestPoint == null) {
                bestPoint = new Vec3((box.minX + box.maxX) / 2.0, (box.minY + box.maxY) / 2.0, (box.minZ + box.maxZ) / 2.0);
            }
            heldPoint = bestPoint;
            if (POLAR_RNG.nextDouble() < 0.35) {
                holdUntil = now + 40 + POLAR_RNG.nextInt(90);
            } else {
                holdUntil = now;
            }
        }

        Vector2f baseRotation = RotationUtils.calculate(bestPoint);
        noiseTime += 0.001 + POLAR_RNG.nextDouble() * 0.001;
        noiseDriftX += (POLAR_RNG.nextDouble() - 0.5) * 0.02;
        noiseDriftY += (POLAR_RNG.nextDouble() - 0.5) * 0.02;

        // L1
        double yawAdd = PolarNoiseUtils.noise(noiseTime + noiseDriftX, 0) * 1.4;
        double pitchAdd = PolarNoiseUtils.noise(0, noiseTime + noiseDriftY) * 0.7;
        baseRotation = new Vector2f(baseRotation.x + (float) yawAdd, baseRotation.y + (float) pitchAdd);

        // L2
        yawAdd = Math.sin(now * chaosFreqA + chaosPhaseA) * chaosAmpA + Math.sin(now * chaosFreqB * 1.7 + chaosPhaseB) * chaosAmpA * 0.4;
        pitchAdd = Math.cos(now * chaosFreqB + chaosPhaseB) * chaosAmpB + Math.cos(now * chaosFreqA * 1.3 + chaosPhaseA) * chaosAmpB * 0.4;
        baseRotation = new Vector2f(baseRotation.x + (float) yawAdd, baseRotation.y + (float) pitchAdd);

        // L3
        yawAdd = Math.sin(now * orbitFreq + orbitPhase) * orbitRadiusYaw;
        pitchAdd = Math.cos(now * orbitFreq * 0.8 + orbitPhase) * orbitRadiusPitch;
        baseRotation = new Vector2f(baseRotation.x + (float) yawAdd, baseRotation.y + (float) pitchAdd);

        // L4
        yawAdd = Math.sin(now * orbit2Freq + orbit2Phase) * orbit2RadiusYaw;
        pitchAdd = Math.cos(now * orbit2Freq * 1.13 + orbit2Phase) * orbit2RadiusPitch;
        baseRotation = new Vector2f(baseRotation.x + (float) yawAdd, baseRotation.y + (float) pitchAdd);

        // L5
        yawAdd = Math.sin(now * orbit3Freq * 1.61 + orbit3Phase) * orbit3RadiusYaw;
        pitchAdd = Math.cos(now * orbit3Freq + orbit3Phase * 1.7) * orbit3RadiusPitch;
        baseRotation = new Vector2f(baseRotation.x + (float) yawAdd, baseRotation.y + (float) pitchAdd);

        // L6
        float lissT = now * 0.0006f;
        yawAdd = Math.sin(lissFreqX * lissT + lissPhase) * lissAmpX;
        pitchAdd = Math.sin(lissFreqY * lissT) * lissAmpY;
        baseRotation = new Vector2f(baseRotation.x + (float) yawAdd, baseRotation.y + (float) pitchAdd);

        // L7
        float spiroT = now * 0.0004f;
        yawAdd = Math.sin(spiroPetal * spiroT + spiroPhase) * Math.cos(spiroT) * spiroAmp;
        pitchAdd = Math.cos(spiroPetal * spiroT + spiroPhase) * Math.sin(spiroT * 1.3) * spiroAmp * 0.5;
        baseRotation = new Vector2f(baseRotation.x + (float) yawAdd, baseRotation.y + (float) pitchAdd);

        // L8
        walkX += (walkTargetX - walkX) * walkSpeed + (POLAR_RNG.nextDouble() - 0.5) * 0.03;
        walkY += (walkTargetY - walkY) * walkSpeed + (POLAR_RNG.nextDouble() - 0.5) * 0.02;
        double wDistSq = (walkTargetX - walkX) * (walkTargetX - walkX) + (walkTargetY - walkY) * (walkTargetY - walkY);
        if (wDistSq < 0.01) {
            walkTargetX = (POLAR_RNG.nextDouble() - 0.5) * 2;
            walkTargetY = (POLAR_RNG.nextDouble() - 0.5) * 2;
        }
        yawAdd = walkX;
        pitchAdd = walkY;
        baseRotation = new Vector2f(baseRotation.x + (float) yawAdd, baseRotation.y + (float) pitchAdd);

        // L9
        logisticX = logisticR * logisticX * (1.0 - logisticX);
        double logisticVal = (logisticX - 0.5) * 2.0;
        yawAdd = logisticVal * 0.5;
        pitchAdd = logisticVal * 0.35;
        baseRotation = new Vector2f(baseRotation.x + (float) yawAdd, baseRotation.y + (float) pitchAdd);

        // L10
        yawAdd = Math.sin(now * beatFreqA + beatPhase) * Math.sin(now * beatFreqB) * 0.6;
        pitchAdd = Math.cos(now * beatFreqA * 0.9) * Math.cos(now * beatFreqB + beatPhase) * 0.35;
        baseRotation = new Vector2f(baseRotation.x + (float) yawAdd, baseRotation.y + (float) pitchAdd);

        // L11
        yawAdd = PolarNoiseUtils.simplex2D(now * yawNoiseFreq * 0.002, noiseDriftX) * yawNoiseAmp;
        pitchAdd = PolarNoiseUtils.simplex2D(now * pitchNoiseFreq * 0.0017, noiseDriftY) * pitchNoiseAmp;
        baseRotation = new Vector2f(baseRotation.x + (float) yawAdd, baseRotation.y + (float) pitchAdd);

        // L12
        yawAdd = (PolarNoiseUtils.cellularNoise(noiseTime * 0.5, noiseDriftY * 0.3) - 0.5) * 0.4;
        pitchAdd = (PolarNoiseUtils.cellularNoise(noiseDriftX * 0.3, noiseTime * 0.5) - 0.5) * 0.25;
        baseRotation = new Vector2f(baseRotation.x + (float) yawAdd, baseRotation.y + (float) pitchAdd);

        // L13
        yawAdd = (PolarNoiseUtils.turbulence(noiseTime * 0.3, noiseDriftX, 3) - 0.9) * 0.7 * noiseScale * 0.2;
        pitchAdd = (PolarNoiseUtils.billow(noiseTime * 0.25, noiseDriftY, 3) - 0.9) * 0.4 * noiseScale * 0.2;
        baseRotation = new Vector2f(baseRotation.x + (float) yawAdd, baseRotation.y + (float) pitchAdd);

        // L14
        yawAdd = PolarNoiseUtils.ridgedNoise(noiseTime * 0.35, noiseDriftY, 4, 2.0, 0.7) * 0.5;
        pitchAdd = PolarNoiseUtils.ridgedNoise(noiseDriftX, noiseTime * 0.35, 4, 2.0, 0.7) * 0.3;
        baseRotation = new Vector2f(baseRotation.x + (float) yawAdd, baseRotation.y + (float) pitchAdd);

        // L15
        double warpAngle = Math.sin(now * 0.0003) * 0.5 + baseBlend;
        double rotatingWarp = PolarNoiseUtils.domainWarpRotating(noiseTime * 0.2, noiseDriftY, warpStrength * 0.15, noiseScale * 0.2, noiseOctaves, warpAngle);
        yawAdd = rotatingWarp * 0.6;
        pitchAdd = rotatingWarp * 0.35;
        baseRotation = new Vector2f(baseRotation.x + (float) yawAdd, baseRotation.y + (float) pitchAdd);

        // L16
        double dual = PolarNoiseUtils.dualFbm(noiseTime * 0.18, noiseDriftY, noiseOctaves, 2.0, 0.5, 2.6, 0.35, baseBlend);
        yawAdd = dual * 0.45;
        pitchAdd = dual * 0.28;
        baseRotation = new Vector2f(baseRotation.x + (float) yawAdd, baseRotation.y + (float) pitchAdd);

        // L17
        double blended = PolarNoiseUtils.blendedNoise(noiseTime * 0.22, noiseDriftX, noiseOctaves, 0.4, 0.35, 0.25);
        yawAdd = blended * 0.55;
        pitchAdd = blended * 0.32;
        baseRotation = new Vector2f(baseRotation.x + (float) yawAdd, baseRotation.y + (float) pitchAdd);

        // L18
        double flutter = PolarNoiseUtils.jitter(noiseTime * 0.11) * 0.18;
        yawAdd = flutter;
        pitchAdd = flutter * 0.6;
        baseRotation = new Vector2f(baseRotation.x + (float) yawAdd, baseRotation.y + (float) pitchAdd);

        if (flicking) {
            baseRotation = applyFlick(baseRotation);
        }
        return baseRotation;
    }

    private static Vector2f applyFlick(Vector2f r) {
        long elapsed = System.currentTimeMillis() - flickStartTime;
        float progress = elapsed / (float) flickDurationMs;
        if (progress >= 1.0f) {
            flicking = false;
            return r;
        }
        float offYaw, offPitch;
        if (progress < 0.35f) {
            float p = progress / 0.35f;
            float eased = p * p;
            offYaw = flickDirectionYaw * flickStrength * eased * flickSpeed * 0.18f;
            offPitch = flickDirectionPitch * flickStrength * eased * flickSpeed * 0.12f;
        } else {
            float p = (progress - 0.35f) / 0.65f;
            float eased = 1.0f - (1.0f - p) * (1.0f - p);
            float overshoot = (float) Math.sin(p * Math.PI * 1.4f) * 0.18f;
            float settle = (1.0f - eased) + overshoot * (1.0f - eased);
            offYaw = flickDirectionYaw * flickStrength * settle * flickRecoverySpeed * 0.12f;
            offPitch = flickDirectionPitch * flickStrength * settle * flickRecoverySpeed * 0.09f;
        }
        return new Vector2f(r.x + offYaw, r.y + offPitch);
    }

    private static List<Vec3> generateBodyScan(AxisAlignedBB box) {
        double w = box.maxX - box.minX;
        double h = box.maxY - box.minY;
        double d = box.maxZ - box.minZ;
        double midX = (box.minX + box.maxX) / 2.0;
        double midZ = (box.minZ + box.maxZ) / 2.0;
        double cloudRotation = POLAR_RNG.nextDouble() * Math.PI * 2;
        double eccentricity = 0.7 + POLAR_RNG.nextDouble() * 0.6;
        List<Vec3> points = new ArrayList<>();

        // Torso
        int torsoCount = 6 + POLAR_RNG.nextInt(6);
        double torsoY = box.minY + h * (0.40 + POLAR_RNG.nextDouble() * 0.18);
        double torsoRadius = Math.min(w, d) * (0.30 + POLAR_RNG.nextDouble() * 0.22);
        for (int i = 0; i < torsoCount; i++) {
            double angle = cloudRotation + Math.PI * 2 * i / torsoCount + (POLAR_RNG.nextDouble() - 0.5) * 0.5;
            points.add(new Vec3(midX + Math.cos(angle) * torsoRadius * eccentricity, torsoY + (POLAR_RNG.nextDouble() - 0.5) * h * 0.22, midZ + Math.sin(angle) * torsoRadius));
        }

        // Upper
        int upperCount = 2 + POLAR_RNG.nextInt(5);
        double upperY = box.minY + h * (0.70 + POLAR_RNG.nextDouble() * 0.18);
        double upperRadius = Math.min(w, d) * (0.20 + POLAR_RNG.nextDouble() * 0.2);
        for (int i = 0; i < upperCount; i++) {
            double angle = cloudRotation + Math.PI * 2 * i / upperCount + POLAR_RNG.nextDouble() * 0.6;
            points.add(new Vec3(midX + Math.cos(angle) * upperRadius, upperY + (POLAR_RNG.nextDouble() - 0.5) * h * 0.12, midZ + Math.sin(angle) * upperRadius));
        }

        // Lower
        int lowerCount = 2 + POLAR_RNG.nextInt(5);
        double lowerY = box.minY + h * (0.15 + POLAR_RNG.nextDouble() * 0.18);
        double lowerRadius = Math.min(w, d) * (0.26 + POLAR_RNG.nextDouble() * 0.2);
        for (int i = 0; i < lowerCount; i++) {
            double angle = cloudRotation + Math.PI * 2 * i / lowerCount + POLAR_RNG.nextDouble() * 0.6;
            points.add(new Vec3(midX + Math.cos(angle) * lowerRadius, lowerY + (POLAR_RNG.nextDouble() - 0.5) * h * 0.12, midZ + Math.sin(angle) * lowerRadius));
        }

        // Limbs
        int limbCount = 1 + POLAR_RNG.nextInt(4);
        for (int i = 0; i < limbCount; i++) {
            double side = POLAR_RNG.nextBoolean() ? 1.0 : -1.0;
            double angle = cloudRotation + POLAR_RNG.nextDouble() * Math.PI * 2;
            double radius = Math.min(w, d) * (0.45 + POLAR_RNG.nextDouble() * 0.45);
            points.add(new Vec3(midX + Math.cos(angle) * radius * side, box.minY + h * (0.25 + POLAR_RNG.nextDouble() * 0.55), midZ + Math.sin(angle) * radius * side));
        }

        if (POLAR_RNG.nextDouble() < 0.6) {
            points.add(new Vec3(midX + (POLAR_RNG.nextDouble() - 0.5) * w * 0.3, box.maxY - h * 0.02, midZ + (POLAR_RNG.nextDouble() - 0.5) * d * 0.3));
        }
        if (POLAR_RNG.nextDouble() < 0.4) {
            points.add(new Vec3(midX + (POLAR_RNG.nextDouble() - 0.5) * w * 0.3, box.minY + h * 0.02, midZ + (POLAR_RNG.nextDouble() - 0.5) * d * 0.3));
        }
        return points;
    }

    private static List<Vec3> subdivideByDistance(List<Vec3> pts) {
        if (pts.size() < 2) return pts;
        double totalDist = 0.0;
        int size = pts.size();
        for (int i = 0; i < size; i++) {
            totalDist += pts.get(i).distanceTo(pts.get((i + 1) % size));
        }
        double avgDist = totalDist / size;
        List<Vec3> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Vec3 current = pts.get(i);
            Vec3 next = pts.get((i + 1) % size);
            result.add(current);
            double segDist = current.distanceTo(next);
            if (segDist > avgDist * 0.5) {
                int subdivisions = MathUtils.getRandomInt(2, Math.min(8, (int) Math.ceil(segDist / avgDist * 2)));
                for (int s = 1; s < subdivisions; s++) {
                    double t = s / (double) subdivisions;
                    result.add(new Vec3(
                            current.xCoord + (next.xCoord - current.xCoord) * t,
                            current.yCoord + (next.yCoord - current.yCoord) * t,
                            current.zCoord + (next.zCoord - current.zCoord) * t
                    ));
                }
            }
        }
        return result;
    }

    private static List<Vec3> applyNoiseTransform(List<Vec3> points, double noiseScale, double warpStrength, int octaves) {
        List<Vec3> warped = new ArrayList<>();
        double chaosScale = 0.7 + POLAR_RNG.nextDouble() * 0.8;
        for (int i = 0; i < points.size(); i++) {
            Vec3 p = points.get(i);
            double nx = p.xCoord * noiseScale + i * 0.5 + noiseDriftX;
            double ny = p.yCoord * noiseScale + i * 0.3 + noiseDriftY;
            boolean useSimplex = POLAR_RNG.nextBoolean();
            double warpX, warpY;
            if (useSimplex) {
                warpX = PolarNoiseUtils.fbmSimplex(nx, ny, octaves, 2.0, 0.5) * warpStrength * chaosScale;
                warpY = PolarNoiseUtils.fbmSimplex(ny + 1.7, nx + 4.3, octaves, 2.2, 0.45) * warpStrength * chaosScale;
            } else {
                warpX = PolarNoiseUtils.domainWarp(nx, ny, warpStrength, noiseScale, octaves) * warpStrength * chaosScale;
                warpY = PolarNoiseUtils.domainWarp(ny + 3.7, nx + 7.1, warpStrength, noiseScale, octaves) * warpStrength * chaosScale;
            }
            double fbmX = PolarNoiseUtils.fbm(nx + warpX, ny + warpY, octaves, 2.0, 0.5) * warpStrength * 0.5 * chaosScale;
            double fbmY = PolarNoiseUtils.fbm(nx + 5.3 + warpX, ny + 9.1 + warpY, octaves, 2.0, 0.5) * warpStrength * 0.5 * chaosScale;
            warped.add(new Vec3(p.xCoord + warpX + fbmX, p.yCoord + warpY + fbmY, p.zCoord + (warpX + fbmX) * 0.5));
        }
        return warped;
    }

    private static Vec3 selectWeightedPoint(List<Vec3> points, Vec3 eyePos, EntityLivingBase target, double seekRange) {
        AxisAlignedBB targetBox = target.getEntityBoundingBox();
        Vec3 targetCenter = new Vec3((targetBox.minX + targetBox.maxX) / 2.0, (targetBox.minY + targetBox.maxY) / 2.0, (targetBox.minZ + targetBox.maxZ) / 2.0);
        float currentYaw = C.p().rotationYaw;
        float currentPitch = C.p().rotationPitch;
        List<Vec3> candidates = new ArrayList<>();
        List<Double> scores = new ArrayList<>();
        for (Vec3 point : points) {
            float[] rot = RotationUtils.getRotationsTo(eyePos, point);
            float deltaYaw = MathHelper.wrapAngleTo180_float(rot[0] - currentYaw);
            float deltaPitch = rot[1] - currentPitch;
            double aimError = Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);
            double centerDist = point.distanceTo(targetCenter);
            double eyeDist = point.distanceTo(eyePos);
            if (eyeDist > seekRange) continue;
            double score = aimError * weightAim + centerDist * weightCenter + eyeDist * weightEye;
            candidates.add(point);
            scores.add(score);
        }
        if (candidates.isEmpty()) return null;
        int poolSize = Math.min(candidates.size(), 4 + POLAR_RNG.nextInt(3));
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) order.add(i);
        order.sort(Comparator.comparingDouble(scores::get));
        List<Integer> pool = order.subList(0, poolSize);
        double[] weights = new double[poolSize];
        double totalWeight = 0.0;
        for (int i = 0; i < poolSize; i++) {
            weights[i] = 1.0 / (1.0 + scores.get(pool.get(i)));
            totalWeight += weights[i];
        }
        double roll = POLAR_RNG.nextDouble() * totalWeight;
        double acc = 0.0;
        for (int i = 0; i < poolSize; i++) {
            acc += weights[i];
            if (roll <= acc) {
                return candidates.get(pool.get(i));
            }
        }
        return candidates.get(pool.get(0));
    }
}