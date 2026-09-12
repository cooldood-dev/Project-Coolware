package com.github.cooldood.managers.impl;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.PlayerUpdateEvent;
import com.github.cooldood.events.impl.WorldUnloadEvent;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.minecraft.RotationUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import org.lwjgl.util.vector.Vector2f;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RotationLearnerManager {

    public static final RotationLearnerManager INSTANCE = new RotationLearnerManager();

    public static final File PRESET_DIR = new File(C.mc.mcDataDir, "coolware/rotationpresets");
    public static final String BUNDLED_PATH = "/assets/minecraft/coolware/rotationpresets/";
    public static final String BUNDLED_MANIFEST = BUNDLED_PATH + "manifest.txt";
    public static final double CAPTURE_RANGE = 6.0;
    public static final int RESERVOIR_CAPACITY = 300;
    public static final int FLUSH_INTERVAL = 20;
    private static final Random RANDOM = new Random();

    private static final ThreadPoolExecutor IO_EXECUTOR = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(512),
            r -> {
                Thread thread = new Thread(r, "yuri-rotation-io");
                thread.setDaemon(true);
                return thread;
            },
            new ThreadPoolExecutor.DiscardOldestPolicy()
    );

    private static final Map<Integer, Float> lastYaw = new HashMap<>();
    private static final Map<Integer, Float> lastPitch = new HashMap<>();
    private static final Map<Integer, Boolean> lastSwing = new HashMap<>();

    private static volatile boolean recording;
    private static volatile String activePresetName;
    private static BufferedWriter activeWriter;
    private static int flushCounter;
    private static volatile RotationModel activeModel;
    private static volatile String activeModelName;
    private static volatile float lastAppliedYawDelta;
    private static volatile float lastAppliedPitchDelta;

    private RotationLearnerManager() {
    }

    @SubscribeEvent
    public static void onPreUpdate(PlayerUpdateEvent event) {
        if (C.w() == null || C.p() == null) return;
        for (EntityPlayer other : C.w().playerEntities) {
            if (other == C.p()) continue;
            int id = other.getEntityId();
            float yaw = other.rotationYaw;
            float pitch = other.rotationPitch;
            boolean swinging = other.isSwingInProgress;

            Float prevYaw = lastYaw.get(id);
            Float prevPitch = lastPitch.get(id);
            Boolean prevSwing = lastSwing.get(id);

            if (recording && prevSwing != null && !prevSwing && swinging && prevYaw != null && C.p().getDistanceToEntity(other) <= CAPTURE_RANGE) {
                float yawDelta = MathHelper.wrapAngleTo180_float(yaw - prevYaw);
                float pitchDelta = pitch - prevPitch;
                if (yawDelta != 0f && pitchDelta != 0f) {
                    queueSample(yawDelta, pitchDelta);
                }
            }
            lastYaw.put(id, yaw);
            lastPitch.put(id, pitch);
            lastSwing.put(id, swinging);
        }
    }

    @SubscribeEvent
    public static void onWorldJoin(WorldUnloadEvent event) {
        lastYaw.clear();
        lastPitch.clear();
        lastSwing.clear();
    }

    private static void queueSample(float y, float p) {
        IO_EXECUTOR.execute(() -> writeSampleInternal(y, p));
    }

    private static void writeSampleInternal(float y, float p) {
        if (activeWriter == null) return;
        try {
            activeWriter.write(y + "," + p);
            activeWriter.newLine();
            flushCounter++;
            if (flushCounter >= FLUSH_INTERVAL) {
                activeWriter.flush();
                flushCounter = 0;
            }
        } catch (IOException ignored) {}
    }

    public static void startRecording(String name) {
        stopRecording();
        recording = true;
        activePresetName = name;
        IO_EXECUTOR.execute(() -> {
            try {
                PRESET_DIR.mkdirs();
                activeWriter = new BufferedWriter(new FileWriter(getPresetFile(name), true));
                flushCounter = 0;
            } catch (IOException ignored) {}
        });
    }

    public static void stopRecording() {
        if (!recording) return;
        recording = false;
        IO_EXECUTOR.execute(RotationLearnerManager::closeWriterQuietly);
    }

    private static void closeWriterQuietly() {
        if (activeWriter == null) return;
        try {
            activeWriter.flush();
            activeWriter.close();
        } catch (IOException ignored) {}
        activeWriter = null;
    }

    public static boolean loadPreset(String name) {
        File file = getPresetFile(name);
        RotationModel model;
        if (file.exists()) {
            model = parsePresetStream(fileStreamOrNull(file));
        } else {
            model = parsePresetStream(RotationLearnerManager.class.getResourceAsStream(BUNDLED_PATH + name + ".csv"));
        }
        if (model == null) return false;
        activeModel = model;
        activeModelName = name;
        return true;
    }

    private static InputStream fileStreamOrNull(File f) {
        try {
            return new FileInputStream(f);
        } catch (IOException e) {
            return null;
        }
    }

    public static RotationModel parsePresetStream(InputStream input) {
        if (input == null) return null;
        double yawSum = 0, pitchSum = 0, yawSumSq = 0, pitchSumSq = 0;
        int count = 0;
        List<Float> yawReservoir = new ArrayList<>(RESERVOIR_CAPACITY);
        List<Float> pitchReservoir = new ArrayList<>(RESERVOIR_CAPACITY);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int comma = line.indexOf(',');
                if (comma <= 0) continue;
                float y, p;
                try {
                    y = Float.parseFloat(line.substring(0, comma).trim());
                    p = Float.parseFloat(line.substring(comma + 1).trim());
                } catch (NumberFormatException e) {
                    continue;
                }
                yawSum += y;
                pitchSum += p;
                yawSumSq += (double) y * y;
                pitchSumSq += (double) p * p;
                count++;
                if (yawReservoir.size() < RESERVOIR_CAPACITY) {
                    yawReservoir.add(y);
                    pitchReservoir.add(p);
                } else {
                    int replaceIndex = RANDOM.nextInt(count);
                    if (replaceIndex < RESERVOIR_CAPACITY) {
                        yawReservoir.set(replaceIndex, y);
                        pitchReservoir.set(replaceIndex, p);
                    }
                }
            }
        } catch (IOException ignored) {
        } finally {
            try {
                input.close();
            } catch (IOException ignored) {}
        }
        if (count == 0) return null;
        float yawMean = (float) (yawSum / count);
        float pitchMean = (float) (pitchSum / count);
        double yawVariance = Math.max(0.0, (yawSumSq / count) - (double) yawMean * yawMean);
        double pitchVariance = Math.max(0.0, (pitchSumSq / count) - (double) pitchMean * pitchMean);
        float yawStdDev = (float) Math.sqrt(yawVariance);
        float pitchStdDev = (float) Math.sqrt(pitchVariance);
        return new RotationModel(yawMean, pitchMean, yawStdDev, pitchStdDev, count, yawReservoir, pitchReservoir);
    }

    public static void unloadPreset() {
        activeModel = null;
        activeModelName = null;
    }

    public static List<String> listPresets() {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        PRESET_DIR.mkdirs();
        File[] files = PRESET_DIR.listFiles((dir, name) -> name.endsWith(".csv"));
        if (files != null) {
            for (File f : files) {
                String name = f.getName();
                names.add(name.substring(0, name.length() - 4));
            }
        }
        names.addAll(listBundledPresets());
        return new ArrayList<>(names);
    }

    private static List<String> listBundledPresets() {
        List<String> bundled = new ArrayList<>();
        InputStream stream = RotationLearnerManager.class.getResourceAsStream(BUNDLED_MANIFEST);
        if (stream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        bundled.add(line);
                    }
                }
            } catch (IOException ignored) {}
        }
        return bundled;
    }

    public static boolean isBundledPreset(String name) {
        return !getPresetFile(name).exists() && listBundledPresets().contains(name);
    }

    public static boolean deletePreset(String name) {
        File file = getPresetFile(name);
        return file.exists() && file.delete();
    }

    public static boolean isRecording() {
        return recording;
    }

    public static String getActivePresetName() {
        return activePresetName;
    }

    public static boolean hasModelLoaded() {
        return activeModel != null;
    }

    public static String getLoadedModelName() {
        return activeModelName;
    }

    public static void resetSmoothing() {
        lastAppliedYawDelta = 0f;
        lastAppliedPitchDelta = 0f;
    }

    public static Vector2f humanize(Vector2f rotation) {
        return humanize(rotation, 1.0f, 1.0f);
    }

    public static Vector2f humanize(Vector2f rotation, float weight, float ease) {
        RotationModel model = activeModel;
        if (model == null) return rotation;
        float rawYaw, rawPitch;
        if (!model.yawReservoir.isEmpty() && RANDOM.nextBoolean()) {
            int idx = RANDOM.nextInt(model.yawReservoir.size());
            rawYaw = model.yawReservoir.get(idx);
            rawPitch = model.pitchReservoir.get(idx);
        } else {
            rawYaw = (float) (model.yawMean + RANDOM.nextGaussian() * model.yawStdDev);
            rawPitch = (float) (model.pitchMean + RANDOM.nextGaussian() * model.pitchStdDev);
        }
        float clampedEase = MathHelper.clamp_float(ease, 0.01f, 1.0f);
        float easedYaw = lastAppliedYawDelta + (rawYaw - lastAppliedYawDelta) * clampedEase;
        float easedPitch = lastAppliedPitchDelta + (rawPitch - lastAppliedPitchDelta) * clampedEase;
        lastAppliedYawDelta = easedYaw;
        lastAppliedPitchDelta = easedPitch;
        float yaw = rotation.x + easedYaw * weight;
        float pitch = MathHelper.clamp_float(rotation.y + easedPitch * weight, -90f, 90f);
        return new Vector2f(yaw, pitch);
    }

    private static File getPresetFile(String name) {
        return new File(PRESET_DIR, name + ".csv");
    }

    public static String exportPresetRaw(String name) {
        File file = getPresetFile(name);
        if (!file.exists()) return null;
        try {
            byte[] bytes = readAllBytes(file);
            return Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            return null;
        }
    }

    public static boolean importPresetRaw(String name, String base64) {
        if (name == null || base64 == null) return false;
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            PRESET_DIR.mkdirs();
            File file = getPresetFile(name);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(bytes);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] readAllBytes(File f) throws IOException {
        try (FileInputStream fis = new FileInputStream(f);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int read;
            while ((read = fis.read(buf)) != -1) {
                baos.write(buf, 0, read);
            }
            return baos.toByteArray();
        }
    }

    public static class RotationModel {
        final float yawMean, pitchMean, yawStdDev, pitchStdDev;
        final int sampleCount;
        final List<Float> yawReservoir, pitchReservoir;

        RotationModel(float yawMean, float pitchMean, float yawStdDev, float pitchStdDev, int sampleCount, List<Float> yawReservoir, List<Float> pitchReservoir) {
            this.yawMean = yawMean;
            this.pitchMean = pitchMean;
            this.yawStdDev = yawStdDev;
            this.pitchStdDev = pitchStdDev;
            this.sampleCount = sampleCount;
            this.yawReservoir = yawReservoir;
            this.pitchReservoir = pitchReservoir;
        }
    }
}