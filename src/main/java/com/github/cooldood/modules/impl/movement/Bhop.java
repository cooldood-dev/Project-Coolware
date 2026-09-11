package com.github.cooldood.modules.impl.movement;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.MovementInputEvent;
import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.modules.RegisterSubModule;
import com.github.cooldood.utils.minecraft.MovementUtil;
import com.github.cooldood.bridge.net.minecraft.client.MinecraftBridge;
import com.github.cooldood.utils.client.C;

@RegisterModule(
        name = "Bhop",
        description = "Bunny hops to increase movement speed.",
        category = Category.MOVEMENT,
        dangerous = true
)
public class Bhop extends Module {
    @RegisterSubModule(name = "Min Timer", min = 0.1, max = 0.5, increment = 0.01)
    public static double minTimer = 0.41D;

    @RegisterSubModule(name = "Max Timer", min = 1.0, max = 2.0, increment = 0.01)
    public static double maxTimer = 1.26D;

    @RegisterSubModule(name = "Fall Cap Blocks", min = 0.2, max = 3.0, increment = 0.1)
    public static double fallCapBlocks = 0.7D;

    @RegisterSubModule(name = "Fall Timer", min = 0.5, max = 1.5, increment = 0.01)
    public static double fallTimer = 1.0D;

    @RegisterSubModule(name = "Idle Timer", min = 0.1, max = 1.0, increment = 0.05)
    public static double idleTimer = 0.4D;

    @RegisterSubModule(name = "Disable in liquid")
    public static boolean disableInLiquid = true;

    @RegisterSubModule(name = "Disable while sneaking")
    public static boolean disableWhileSneaking = true;

    private static final int PHASE_IDLE = 0;
    private static final int PHASE_RISING = 1;
    private static final int PHASE_FALLING = 2;
    private static final double RESERVE_CAP = 0.25D;
    private static final double SPEED_GAIN = 0.6D;

    private int phase = PHASE_IDLE;
    private long phaseStart;
    private double peakY;
    private double lastY;
    private double launchY;
    private boolean wasOnGround = true;
    private double reserve;
    private long lastTimerTick;

    @SubscribeEvent
    public static void onMoveInput(MovementInputEvent event) {
        Bhop module = (Bhop) com.github.cooldood.modules.ModuleManager.getModule(Bhop.class);
        if (module != null) {
            module.handleMoveInput(event);
        }
    }

    private void handleMoveInput(MovementInputEvent event) {
        if (C.mc.thePlayer == null) return;

        final double y = C.mc.thePlayer.posY;
        final boolean onGround = C.mc.thePlayer.onGround;
        final long now = System.currentTimeMillis();

        double dt = (now - lastTimerTick) / 1000.0D;
        if (dt <= 0.0D || dt > 0.1D) dt = 0.05D;
        lastTimerTick = now;

        if ((C.mc.thePlayer.isInWater() || C.mc.thePlayer.isInLava()) && disableInLiquid
                || C.mc.thePlayer.isSneaking() && disableWhileSneaking) {
            resetState();
            setTimer(1.0D);
            wasOnGround = onGround;
            lastY = y;
            return;
        }

        final boolean moving = MovementUtil.isMoving(false);
        if (onGround && moving) {
            event.movementInput.jump = true;
        }

        if (wasOnGround && !onGround) {
            phase = PHASE_RISING;
            phaseStart = now;
            launchY = y;
            peakY = y;
        }

        final double appliedTimer;
        if (!onGround) {
            if (phase == PHASE_IDLE) {
                phase = PHASE_FALLING;
                phaseStart = now;
                launchY = Math.min(launchY, y);
                peakY = Math.max(peakY, y);
            }

            final double arcHeight = Math.max(0.01D, peakY - launchY);
            if (phase == PHASE_RISING) {
                final double r = clamp((y - launchY) / arcHeight, 0.0D, 1.0D);
                final double curve = Math.sin(Math.PI * 0.5D * r);
                final double base = minTimer + (maxTimer - minTimer) * curve;
                appliedTimer = moving ? Math.min(maxTimer, base + reserve * SPEED_GAIN) : 1.0D;

                final double elapsed = (now - phaseStart) / 1000.0D;
                if (y <= lastY && elapsed > 0.05D) {
                    phase = PHASE_FALLING;
                    phaseStart = now;
                    peakY = lastY;
                } else if (y > peakY) {
                    peakY = y;
                }
            } else {
                final double fallDistance = peakY - y;
                if (fallDistance <= fallCapBlocks) {
                    final double f = clamp(fallDistance / fallCapBlocks, 0.0D, 1.0D);
                    final double curve = Math.cos(Math.PI * 0.5D * f);
                    final double base = minTimer + (maxTimer - minTimer) * curve;
                    appliedTimer = moving ? Math.min(maxTimer, base + reserve * SPEED_GAIN) : 1.0D;
                } else {
                    appliedTimer = moving ? fallTimer : 1.0D;
                }
            }
        } else {
            phase = PHASE_IDLE;
            phaseStart = 0L;
            appliedTimer = moving ? 1.0D : (reserve < RESERVE_CAP ? idleTimer : 1.0D);
        }

        if (!onGround) {
            if (appliedTimer > 1.0D) reserve -= (appliedTimer - 1.0D) * dt;
        } else if (appliedTimer < 1.0D) {
            reserve += (1.0D - appliedTimer) * dt;
        }
        reserve = clamp(reserve, 0.0D, RESERVE_CAP);

        setTimer(appliedTimer);
        wasOnGround = onGround;
        lastY = y;
    }

    private void resetState() {
        phase = PHASE_IDLE;
        phaseStart = 0L;
        reserve = 0.0D;
    }

    @Override
    protected void onEnable() {
        resetState();
        wasOnGround = true;
        lastTimerTick = 0L;
        if (C.mc.thePlayer != null) {
            lastY = C.mc.thePlayer.posY;
        }
        setTimer(1.0D);
    }

    @Override
    protected void onDisable() {
        resetState();
        setTimer(1.0D);
    }

    @Override
    public String arrayListExtraInfo() {
        return String.format("Timer max %.2f", maxTimer);
    }

    private void setTimer(double speed) {
        MinecraftBridge.from(C.mc).bridge$getTimer().bridge$setTimerSpeed((float) speed);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
