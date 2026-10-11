package coolware.module.combat;

import coolware.Coolware;
import coolware.module.Category;
import coolware.module.Module;
import coolware.module.movement.Scaffold;
import coolware.setting.BooleanSetting;
import coolware.setting.ModeSetting;
import coolware.setting.NumberSetting;
import coolware.util.RotationUtil;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.Potion;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovementInput;
import org.lwjgl.input.Keyboard;

public class Velocity extends Module {

    private int chanceCounter = 0;
    private boolean pendingExplosion = false;
    private boolean allowNext = true;
    private boolean jumpFlag = false;

    private int rotatoTickCounter = 0;
    private double knockbackX = 0;
    private double knockbackZ = 0;
    private float[] targetRotation = null;
    private int reduceTick = -1;
    private boolean pressed = false;
    private boolean hasReceivedVelocity = false;
    private int ticksSinceVelocity = -1;
    public static boolean extraAttacked = false;
    public static boolean velocityAttacked = false;

    private boolean ShouldJump = false;

    private int slapReduceTicks = 0;
    private int slapAnInt = 0;
    private boolean slot = false;
    private boolean attack = false;
    private boolean swing = false;
    private boolean block = false;
    private boolean inventory = false;
    private boolean dig = false;

    public final ModeSetting mode = new ModeSetting("Mode", this, "Vanilla", "Vanilla", "Jump", "Hypixel", "Watchdog");

    public final NumberSetting chance = new NumberSetting("Chance", this, 100.0D, 0.0D, 100.0D, 1.0D);
    public final NumberSetting horizontal = new NumberSetting("Horizontal", this, 0.0D, 0.0D, 100.0D, 1.0D);
    public final NumberSetting vertical = new NumberSetting("Vertical", this, 100.0D, 0.0D, 100.0D, 1.0D);
    public final NumberSetting explosionHorizontal = new NumberSetting("Explosions Horizontal", this, 100.0D, 0.0D, 100.0D, 1.0D);
    public final NumberSetting explosionVertical = new NumberSetting("Explosions Vertical", this, 100.0D, 0.0D, 100.0D, 1.0D);

    public final BooleanSetting reduce = new BooleanSetting("Reduce", this, true);
    public final NumberSetting attackTimes = new NumberSetting("Attack Times", this, 1.0D, 1.0D, 5.0D, 1.0D);
    private final BooleanSetting onlySprinting = new BooleanSetting("Only Sprinting", this, true);
    private final BooleanSetting reduceWhenCanAttack = new BooleanSetting("Reduce When Can Attack", this, true);
    public final BooleanSetting hypixelJump = new BooleanSetting("Jump", this, true);
    public final BooleanSetting rotate = new BooleanSetting("Rotate", this, false);
    public final NumberSetting rotateTick = new NumberSetting("Rotate Ticks", this, 3.0D, 1.0D, 12.0D, 1.0D);

    public final BooleanSetting slapReduce = new BooleanSetting("Slap Reduce", this, true);
    public final BooleanSetting tickExactEnable = new BooleanSetting("Tick Exact", this, true);
    public final NumberSetting tick500 = new NumberSetting("500", this, 3.0D, 0.0D, 20.0D, 1.0D);
    public final NumberSetting tick1000 = new NumberSetting("1000", this, 4.0D, 0.0D, 20.0D, 1.0D);
    public final NumberSetting tick2000 = new NumberSetting("2000", this, 4.0D, 0.0D, 20.0D, 1.0D);
    public final NumberSetting tick3000 = new NumberSetting("3000", this, 5.0D, 0.0D, 20.0D, 1.0D);
    public final NumberSetting tick4000 = new NumberSetting("4000", this, 6.0D, 0.0D, 20.0D, 1.0D);
    public final NumberSetting tick5000 = new NumberSetting("5000", this, 6.0D, 0.0D, 20.0D, 1.0D);
    public final NumberSetting tick6000 = new NumberSetting("6000", this, 7.0D, 0.0D, 20.0D, 1.0D);
    public final NumberSetting tick7000 = new NumberSetting("7000", this, 7.0D, 0.0D, 20.0D, 1.0D);
    public final NumberSetting tick8000 = new NumberSetting("8000", this, 8.0D, 0.0D, 20.0D, 1.0D);
    public final NumberSetting tick9000 = new NumberSetting("9000", this, 8.0D, 0.0D, 20.0D, 1.0D);
    public final NumberSetting tick10000 = new NumberSetting("10000", this, 9.0D, 0.0D, 20.0D, 1.0D);

    public final BooleanSetting fakeCheck = new BooleanSetting("Fake Check", this, true);
    public final BooleanSetting debugLog = new BooleanSetting("Debug Log", this, false);

    public Velocity() {
        super("Velocity", "Reduces or eliminates knockback taken.", Category.COMBAT, Keyboard.KEY_NONE);
        addSetting(mode);
        chance.setHidden(true);
        addSetting(chance);
        horizontal.setHidden(true);
        addSetting(horizontal);
        vertical.setHidden(true);
        addSetting(vertical);
        explosionHorizontal.setHidden(true);
        addSetting(explosionHorizontal);
        explosionVertical.setHidden(true);
        addSetting(explosionVertical);
        reduce.setHidden(true);
        addSetting(reduce);
        attackTimes.setHidden(true);
        addSetting(attackTimes);
        onlySprinting.setHidden(true);
        addSetting(onlySprinting);
        reduceWhenCanAttack.setHidden(true);
        addSetting(reduceWhenCanAttack);
        hypixelJump.setHidden(true);
        addSetting(hypixelJump);
        rotate.setHidden(true);
        addSetting(rotate);
        rotateTick.setHidden(true);
        addSetting(rotateTick);
        slapReduce.setHidden(true);
        addSetting(slapReduce);
        tickExactEnable.setHidden(true);
        addSetting(tickExactEnable);
        tick500.setHidden(true);
        addSetting(tick500);
        tick1000.setHidden(true);
        addSetting(tick1000);
        tick2000.setHidden(true);
        addSetting(tick2000);
        tick3000.setHidden(true);
        addSetting(tick3000);
        tick4000.setHidden(true);
        addSetting(tick4000);
        tick5000.setHidden(true);
        addSetting(tick5000);
        tick6000.setHidden(true);
        addSetting(tick6000);
        tick7000.setHidden(true);
        addSetting(tick7000);
        tick8000.setHidden(true);
        addSetting(tick8000);
        tick9000.setHidden(true);
        addSetting(tick9000);
        tick10000.setHidden(true);
        addSetting(tick10000);
        fakeCheck.setHidden(true);
        addSetting(fakeCheck);
        debugLog.setHidden(true);
        addSetting(debugLog);
    }

    private static Velocity getInstance() {
        if (Coolware.getInstance() != null && Coolware.getInstance().getModuleManager() != null) {
            return Coolware.getInstance().getModuleManager().getModule(Velocity.class);
        }
        return null;
    }

    private int getModeIndex() {
        String m = mode.getMode();
        if ("Vanilla".equalsIgnoreCase(m)) return 0;
        if ("Jump".equalsIgnoreCase(m)) return 1;
        if ("Hypixel".equalsIgnoreCase(m)) return 2;
        if ("Watchdog".equalsIgnoreCase(m) || "Slap_Attack".equalsIgnoreCase(m)) return 3;
        return 0;
    }

    private boolean isInLiquidOrWeb() {
        if (mc.thePlayer == null) return false;
        return mc.thePlayer.isInWater() || mc.thePlayer.isInLava() || mc.thePlayer.isInWeb();
    }

    public static boolean handleVelocityPacket(S12PacketEntityVelocity packet) {
        Velocity v = getInstance();
        if (v == null || !v.isToggled() || mc.thePlayer == null || mc.theWorld == null) {
            return false;
        }
        return v.onVelocityPacket(packet);
    }

    public static boolean handleExplosionPacket(S27PacketExplosion packet) {
        Velocity v = getInstance();
        if (v == null || !v.isToggled() || mc.thePlayer == null) {
            return false;
        }
        return v.onExplosionPacket(packet);
    }

    public static void handleEntityStatusPacket(S19PacketEntityStatus packet) {
        Velocity v = getInstance();
        if (v == null || !v.isToggled() || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }
        v.onEntityStatus(packet);
    }

    public static void onMovementInput(MovementInput movementInput) {
        Velocity v = getInstance();
        if (v == null || !v.isToggled() || mc.thePlayer == null || movementInput == null) {
            return;
        }
        // Directional move adjustment if velocity rotation is active
        v.handleMoveInput(movementInput);
    }

    public static void onLivingUpdate() {
        Velocity v = getInstance();
        if (v == null || !v.isToggled() || mc.thePlayer == null) {
            return;
        }
        v.handleLivingUpdate();
    }

    public static void onPacketSend(Packet<?> packet) {
        Velocity v = getInstance();
        if (v == null || !v.isToggled()) {
            return;
        }
        v.handlePacketSend(packet);
    }

    private boolean onVelocityPacket(S12PacketEntityVelocity packet) {
        if (packet.getEntityID() != mc.thePlayer.getEntityId()) {
            return false;
        }

        int curMode = getModeIndex();

        if (curMode == 2) {
            this.hasReceivedVelocity = true;
            this.ticksSinceVelocity = 0;
            this.jumpFlag = packet.getMotionY() > 0;
        }

        if (curMode == 3 && this.slapReduce.isEnabled()) {
            this.slapReduceTicks = this.calculateSlapTicks(packet.getMotionX(), packet.getMotionZ());
            if (this.debugLog.isEnabled()) {
                logMessage("Attack reduceTicks: " + this.slapReduceTicks);
            }
        }

        if (this.debugLog.isEnabled()) {
            logMessage(String.format("Velocity (tick: %d, x: %.2f, y: %.2f, z: %.2f)",
                    mc.thePlayer.ticksExisted,
                    (double) packet.getMotionX() / 8000.0D,
                    (double) packet.getMotionY() / 8000.0D,
                    (double) packet.getMotionZ() / 8000.0D
            ));
        }

        // Apply knockback modifications from onKnockback
        return applyKnockback(packet);
    }

    private boolean applyKnockback(S12PacketEntityVelocity packet) {
        int curMode = getModeIndex();

        if (!this.allowNext || !this.fakeCheck.isEnabled()) {
            this.allowNext = true;
            if (curMode <= 1) {
                this.chanceCounter = (this.chanceCounter % 100) + (int) this.chance.getValue();
                if (this.chanceCounter >= 100) {
                    if (curMode == 1) {
                        this.jumpFlag = packet.getMotionY() > 0;
                    }

                    if (this.horizontal.getValue() > 0.0D) {
                        double factor = this.horizontal.getValue() / 100.0D;
                        packet.setMotionX((int) (packet.getMotionX() * factor));
                        packet.setMotionZ((int) (packet.getMotionZ() * factor));
                    } else {
                        packet.setMotionX((int) (mc.thePlayer.motionX * 8000.0D));
                        packet.setMotionZ((int) (mc.thePlayer.motionZ * 8000.0D));
                    }

                    if (this.vertical.getValue() > 0.0D) {
                        double factor = this.vertical.getValue() / 100.0D;
                        packet.setMotionY((int) (packet.getMotionY() * factor));
                    } else {
                        packet.setMotionY((int) (mc.thePlayer.motionY * 8000.0D));
                    }
                }
            } else if (curMode == 2) {
                if (this.rotate.isEnabled() && packet.getMotionY() > 0) {
                    this.knockbackX = (double) packet.getMotionX() / 8000.0D;
                    this.knockbackZ = (double) packet.getMotionZ() / 8000.0D;
                    if (Math.abs(this.knockbackX) > 0.01 || Math.abs(this.knockbackZ) > 0.01) {
                        this.rotatoTickCounter = 1;
                    }
                }
                this.ticksSinceVelocity = 0;
                this.hasReceivedVelocity = true;
            }
        }
        return false;
    }

    private boolean onExplosionPacket(S27PacketExplosion packet) {
        int curMode = getModeIndex();
        if (curMode <= 1) {
            if (packet.func_149149_c() != 0.0F || packet.func_149144_d() != 0.0F || packet.func_149147_e() != 0.0F) {
                this.pendingExplosion = true;
                if (this.explosionHorizontal.getValue() == 0.0D || this.explosionVertical.getValue() == 0.0D) {
                    return true; // Cancel explosion motion
                }
                if (this.debugLog.isEnabled()) {
                    logMessage(String.format("Explosion (tick: %d, x: %.2f, y: %.2f, z: %.2f)",
                            mc.thePlayer.ticksExisted,
                            mc.thePlayer.motionX + (double) packet.func_149149_c(),
                            mc.thePlayer.motionY + (double) packet.func_149144_d(),
                            mc.thePlayer.motionZ + (double) packet.func_149147_e()
                    ));
                }
            }
        }
        return false;
    }

    private void onEntityStatus(S19PacketEntityStatus packet) {
        Entity entity = packet.getEntity(mc.theWorld);
        if (entity != null && entity.equals(mc.thePlayer) && packet.getOpCode() == 2) {
            this.allowNext = false;
        }
    }

    private void handleLivingUpdate() {
        int curMode = getModeIndex();
        boolean isMode1 = curMode == 1;
        boolean isMode2Jump = curMode == 2 && this.hypixelJump.isEnabled();

        if (this.jumpFlag && (isMode1 || isMode2Jump)) {
            this.jumpFlag = false;
            if (mc.thePlayer.onGround && mc.thePlayer.isSprinting() && !mc.thePlayer.isPotionActive(Potion.jump) && !this.isInLiquidOrWeb()) {
                mc.thePlayer.movementInput.jump = true;
            }
        }
    }

    private void handleMoveInput(MovementInput movementInput) {
        if (this.rotatoTickCounter > 0 && this.targetRotation != null) {
            float forward = movementInput.moveForward;
            float strafe = movementInput.moveStrafe;
            if (forward != 0.0F || strafe != 0.0F) {
                double angle = MathHelper.wrapAngleTo180_double(Math.toDegrees(direction(mc.thePlayer.rotationYaw, forward, strafe)));
                float closestForward = 0.0F;
                float closestStrafe = 0.0F;
                double closestDifference = Double.MAX_VALUE;

                for (float predictedForward = -1.0F; predictedForward <= 1.0F; predictedForward += 1.0F) {
                    for (float predictedStrafe = -1.0F; predictedStrafe <= 1.0F; predictedStrafe += 1.0F) {
                        if (predictedStrafe == 0.0F && predictedForward == 0.0F) continue;

                        double predictedAngle = MathHelper.wrapAngleTo180_double(Math.toDegrees(direction(this.targetRotation[0], predictedForward, predictedStrafe)));
                        double difference = Math.abs(MathHelper.wrapAngleTo180_double(angle - predictedAngle));

                        if (difference < closestDifference) {
                            closestDifference = difference;
                            closestForward = predictedForward;
                            closestStrafe = predictedStrafe;
                        }
                    }
                }

                movementInput.moveForward = closestForward;
                movementInput.moveStrafe = closestStrafe;
            }
        }
    }

    private static double direction(float rotationYaw, double moveForward, double moveStrafing) {
        if (moveForward < 0.0) {
            rotationYaw += 180.0F;
        }
        float forward = 1.0F;
        if (moveForward < 0.0) {
            forward = -0.5F;
        } else if (moveForward > 0.0) {
            forward = 0.5F;
        }
        if (moveStrafing > 0.0) {
            rotationYaw -= 90.0F * forward;
        }
        if (moveStrafing < 0.0) {
            rotationYaw += 90.0F * forward;
        }
        return Math.toRadians(rotationYaw);
    }

    private void handlePacketSend(Packet<?> packet) {
        if (getModeIndex() == 3) {
            if (packet instanceof C09PacketHeldItemChange) {
                this.slot = true;
            } else if (packet instanceof C0APacketAnimation) {
                this.swing = true;
            } else if (packet instanceof C02PacketUseEntity) {
                C02PacketUseEntity useEntity = (C02PacketUseEntity) packet;
                if (useEntity.getAction() == C02PacketUseEntity.Action.ATTACK) {
                    this.attack = true;
                }
            } else if (packet instanceof C08PacketPlayerBlockPlacement) {
                this.block = true;
            } else if (packet instanceof C07PacketPlayerDigging) {
                this.block = true;
                this.dig = true;
            } else if (packet instanceof C0DPacketCloseWindow ||
                    packet instanceof C0EPacketClickWindow ||
                    (packet instanceof C16PacketClientStatus &&
                            ((C16PacketClientStatus) packet).getStatus() == C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT)) {
                this.inventory = true;
            } else if (packet instanceof C03PacketPlayer) {
                this.resetBadPackets();
            }
        }
    }

    @Override
    public void onUpdate() {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        // Tick counter for velocity
        if (this.ticksSinceVelocity >= 0) {
            this.ticksSinceVelocity++;
        }
        if (this.ticksSinceVelocity >= 10) {
            this.ticksSinceVelocity = -1;
            this.ShouldJump = false;
        }

        handleJumpReset();

        int curMode = getModeIndex();

        // Mode 2: Hypixel
        if (curMode == 2) {
            if (this.reduce.isEnabled()) {
                if (velocityAttacked) {
                    EntityLivingBase auraTarget = KillAura.getTargetEntity();
                    KillAura ka = getKillAura();
                    if (auraTarget != null && ka != null && ka.isToggled()) {
                        mc.getNetHandler().addToSendQueue(new C0APacketAnimation());
                        mc.getNetHandler().addToSendQueue(new C02PacketUseEntity(auraTarget, C02PacketUseEntity.Action.ATTACK));
                        mc.thePlayer.motionX *= 0.6D;
                        mc.thePlayer.motionZ *= 0.6D;
                        mc.thePlayer.setSprinting(false);
                    }
                    velocityAttacked = false;
                }

                if (this.hasReceivedVelocity) {
                    if (this.reduceTick >= (int) this.attackTimes.getValue()) {
                        this.reduceTick = 0;
                        this.hasReceivedVelocity = false;
                    }
                    EntityLivingBase auraTarget = KillAura.getTargetEntity();
                    if (auraTarget != null) {
                        if (mc.thePlayer.isSprinting() || !this.onlySprinting.isEnabled()) {
                            if (!this.reduceWhenCanAttack.isEnabled() || KillAura.blockTick == 0) {
                                mc.getNetHandler().addToSendQueue(new C0APacketAnimation());
                                mc.getNetHandler().addToSendQueue(new C02PacketUseEntity(auraTarget, C02PacketUseEntity.Action.ATTACK));
                                mc.thePlayer.motionX *= 0.6D;
                                mc.thePlayer.motionZ *= 0.6D;
                                mc.thePlayer.setSprinting(false);
                            }
                        }
                    }
                    this.reduceTick++;
                }
            }

            int maxTick = (int) this.rotateTick.getValue();
            if (this.rotatoTickCounter > 0 && this.rotatoTickCounter <= maxTick) {
                if (this.rotatoTickCounter == 1) {
                    double deltaX = -this.knockbackX;
                    double deltaZ = -this.knockbackZ;
                    this.targetRotation = getRotationsTo(deltaX, 0.0D, deltaZ, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
                }
                if (this.targetRotation != null) {
                    RotationUtil.setSilentRotation(this.targetRotation[0], this.targetRotation[1]);
                }
                this.rotatoTickCounter++;
                if (this.rotatoTickCounter > maxTick) {
                    this.rotatoTickCounter = 0;
                    this.targetRotation = null;
                    this.knockbackX = 0;
                    this.knockbackZ = 0;
                    if (!Scaffold.wantsMoveFix() && !KillAura.isActivelyAiming()) {
                        RotationUtil.resetSilentRotation();
                    }
                }
            }
        }

        // Mode 3: Slap_Attack
        if (curMode == 3 && this.slapReduce.isEnabled()) {
            if (this.slapReduceTicks > 0) {
                this.slapReduceTicks--;
                EntityLivingBase target = KillAura.getTargetEntity();
                KillAura ka = getKillAura();
                if (ka != null && ka.isToggled() && target != null) {
                    if (!mc.thePlayer.isInWeb() && mc.thePlayer.isSprinting() && isMoving() && target != mc.thePlayer && !this.badPackets()) {
                        mc.getNetHandler().addToSendQueue(new C0APacketAnimation());
                        mc.getNetHandler().addToSendQueue(new C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK));
                        mc.thePlayer.motionX *= 0.6D;
                        mc.thePlayer.motionZ *= 0.6D;
                        mc.thePlayer.setSprinting(false);
                        this.slapAnInt++;
                        if (this.debugLog.isEnabled()) {
                            logMessage("Attack reduce " + this.slapAnInt);
                        }
                    }
                }
            }
        }
    }

    private void handleJumpReset() {
        if (!this.ShouldJump) return;

        Scaffold scaffold = getScaffold();
        if (mc.thePlayer == null || mc.currentScreen instanceof GuiInventory || (scaffold != null && scaffold.isToggled())) return;
        if (this.ticksSinceVelocity >= 0) {
            if (this.ticksSinceVelocity == 0) {
                this.pressed = mc.gameSettings.keyBindJump.isKeyDown();
            }
            if (this.ticksSinceVelocity <= 2 && mc.thePlayer.onGround) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), true);
            }
        }
        if (this.ticksSinceVelocity >= 4 && this.ticksSinceVelocity <= 9) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), this.pressed);
        }
    }

    private float[] getRotationsTo(double targetX, double targetY, double targetZ, float currentYaw, float currentPitch) {
        double horizontalDistance = Math.sqrt(targetX * targetX + targetZ * targetZ);
        float yawDelta = MathHelper.wrapAngleTo180_float((float) (Math.atan2(targetZ, targetX) * 180.0D / Math.PI) - 90.0F - currentYaw);
        float pitchDelta = MathHelper.wrapAngleTo180_float((float) (-Math.atan2(targetY, horizontalDistance) * 180.0D / Math.PI) - currentPitch);
        return new float[]{currentYaw + yawDelta, currentPitch + pitchDelta};
    }

    private boolean isMoving() {
        if (mc.thePlayer == null || mc.thePlayer.movementInput == null) return false;
        return mc.thePlayer.movementInput.moveForward != 0.0F
                || mc.thePlayer.movementInput.moveStrafe != 0.0F
                || Math.abs(mc.thePlayer.motionX) > 0.01D
                || Math.abs(mc.thePlayer.motionZ) > 0.01D;
    }

    private int calculateSlapTicks(int motionX, int motionZ) {
        double kb = Math.hypot(motionX, motionZ);
        if (!tickExactEnable.isEnabled()) {
            double ticks = 6.43153527E-4 * kb + 2.9419087136;
            int result = (int) Math.round(ticks);
            if (result < 1) result = 1;
            if (result > 10) result = 10;
            return result;
        }
        if (kb <= 500) return (int) tick500.getValue();
        if (kb <= 1000) return (int) tick1000.getValue();
        if (kb <= 2000) return (int) tick2000.getValue();
        if (kb <= 3000) return (int) tick3000.getValue();
        if (kb <= 4000) return (int) tick4000.getValue();
        if (kb <= 5000) return (int) tick5000.getValue();
        if (kb <= 6000) return (int) tick6000.getValue();
        if (kb <= 7000) return (int) tick7000.getValue();
        if (kb <= 8000) return (int) tick8000.getValue();
        if (kb <= 9000) return (int) tick9000.getValue();
        return (int) tick10000.getValue();
    }

    private boolean badPackets() {
        return this.slot || this.attack || this.swing || this.block || this.inventory || this.dig;
    }

    private void resetBadPackets() {
        this.slot = false;
        this.swing = false;
        this.attack = false;
        this.block = false;
        this.inventory = false;
        this.dig = false;
    }

    private KillAura getKillAura() {
        if (Coolware.getInstance() != null && Coolware.getInstance().getModuleManager() != null) {
            return Coolware.getInstance().getModuleManager().getModule(KillAura.class);
        }
        return null;
    }

    private Scaffold getScaffold() {
        if (Coolware.getInstance() != null && Coolware.getInstance().getModuleManager() != null) {
            return Coolware.getInstance().getModuleManager().getModule(Scaffold.class);
        }
        return null;
    }

    private void logMessage(String msg) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "[Velocity] " + EnumChatFormatting.WHITE + msg));
        }
    }

    @Override
    public void onEnable() {
        this.pendingExplosion = false;
        this.allowNext = true;
        this.rotatoTickCounter = 0;
        this.targetRotation = null;
        this.knockbackX = 0;
        this.knockbackZ = 0;
        this.reduceTick = -1;
        this.hasReceivedVelocity = false;
        this.ticksSinceVelocity = -1;
        extraAttacked = false;
        velocityAttacked = false;
        this.jumpFlag = false;
        this.ShouldJump = false;
        this.slapReduceTicks = 0;
        this.slapAnInt = 0;
        this.resetBadPackets();
    }

    @Override
    public void onDisable() {
        this.pendingExplosion = false;
        this.allowNext = true;
        this.hasReceivedVelocity = false;
        this.rotatoTickCounter = 0;
        this.targetRotation = null;
        this.knockbackX = 0;
        this.knockbackZ = 0;
        this.reduceTick = -1;
        this.ticksSinceVelocity = -1;
        extraAttacked = false;
        velocityAttacked = false;
        this.jumpFlag = false;
        this.ShouldJump = false;
        this.slapReduceTicks = 0;
        this.slapAnInt = 0;
        this.resetBadPackets();
        if (!Scaffold.wantsMoveFix() && !KillAura.isActivelyAiming()) {
            RotationUtil.resetSilentRotation();
        }
    }
}
