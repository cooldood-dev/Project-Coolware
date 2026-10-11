package coolware.module.movement;

import coolware.font.moonlight.FontRenderer;
import coolware.font.moonlight.Fonts;
import coolware.gui.hud.IslandBlur;
import coolware.module.Category;
import coolware.module.Module;
import coolware.setting.BooleanSetting;
import coolware.setting.ModeSetting;
import coolware.setting.NumberSetting;
import coolware.util.FakeRotation;
import coolware.util.InventoryUtil;
import coolware.util.RenderUtil;
import coolware.util.RotationUtil;
import coolware.util.WorldUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAnvil;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockWorkbench;
import net.minecraft.block.material.Material;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.BooleanSupplier;

/**
 * Scaffold, as Rise 6 does it (github.com/NoHackClient/Rise-6.9.5): the block
 * under your feet, the face of a solid neighbour that faces it, and an aim
 * that is only moved when it stops landing on that face. Three modes:
 *
 *   Normal  keep the aim on the face; re-aim only when it falls off.
 *   Snap    of the faces the target block can go against, turn to the one
 *           nearest the look you have and flick onto it hard then settle,
 *           starting a tick before the feet leave the edge; look where you
 *           walk only between placements.
 *   Telly   on the ground, look straight ahead; sprint-jump (Jump On RMB) and
 *           keep looking ahead until the jump is Turn Height up, then turn
 *           back, always round the same shoulder, to straight behind you.
 *           From there the yaw holds and only the pitch works. Without
 *           right-click held it bridges as Normal does.
 *
 * Any mode: Same Y holds the layer you started on while you're in the air
 * without holding jump; Extra Block (Same Y) puts one block on top of the
 * bridge where you land, every second jump; Predict Eye wants each place to
 * land from where the move takes your eyes this tick as well; Watchdog Tower
 * holds the place and faces your walk while you hold jump on the ground;
 * Sneak (Normal, Snap) sneaks on an edge until the block is in; Block Count
 * shows the blocks you have left; Jump On RMB jumps off the ground while you
 * hold right-click and move.
 *
 * Runs in onUpdate, which Coolware calls at the start of the tick, before the
 * player's update, so the look set here is the one this tick's movement
 * packet carries. The look is snapped to the mouse grid from the last one sent
 * and written to RotationUtil as is, so the ray cast that gates each place sees
 * exactly what is sent -- what Grim checks a 1.8 placement against.
 *
 * Fake Rotations changes only what your model shows (the look sent is
 * untouched): the server's look, your own, straight back at the bridge like a
 * real backwards scaffold, or a spin. See FakeRotation.
 *
 * Ported from the C++ client's ScaffoldModule.
 */
public class Scaffold extends Module {
    private static Scaffold instance;

    private static final int NORMAL = 0, SNAP = 1, TELLY = 2;
    private static final int FAKE_SERVER = 0, FAKE_REAL = 1, FAKE_BACK = 2, FAKE_SPIN = 3;

    // EnumFacing order: down, up, north, south, west, east.
    private static final int FACING_DOWN = 0, FACING_UP = 1;
    private static final int[] OFFSET_X = { 0, 0, 0, 0, -1, 1 };
    private static final int[] OFFSET_Y = { -1, 1, 0, 0, 0, 0 };
    private static final int[] OFFSET_Z = { 0, 0, -1, 1, 0, 0 };

    private static final double REACH = 4.5;

    private final Random random = new Random();

    // -- settings ----------------------------------------------------------------

    private final ModeSetting mode;

    private final BooleanSetting swing;
    private final NumberSetting placeDelay;
    private final NumberSetting aimRandomness;
    private final NumberSetting turnSpeed;
    private final BooleanSetting sprint;
    private final BooleanSetting switchBack;
    private final BooleanSetting itemSpoof;
    private final BooleanSetting predictEye;
    private final BooleanSetting tower;
    private final BooleanSetting sneak;
    private final NumberSetting sneakDelay;
    private final BooleanSetting blockCount;
    private final BooleanSetting sameY;
    private final BooleanSetting extraBlock;
    private final BooleanSetting jumpOnRmb;
    private final ModeSetting fakeRotations;
    private final NumberSetting fakeSpinSpeed;

    private final NumberSetting easeTicks;
    private final NumberSetting steepness;
    private final BooleanSetting strafeForward;

    private final NumberSetting turnHeight;
    private final ModeSetting turnSide;
    private final BooleanSetting randomizeTicks;
    private final NumberSetting forwardPitch;
    private final NumberSetting lookJitter;

    // -- state -------------------------------------------------------------------

    private float targetYaw, targetPitch;   // Rise's targetYaw / targetPitch

    // The turn sent last tick, and |yaw| of the turn before the last place and
    // of the last turn: a place repeating either is what Grim's
    // DuplicateRotPlace flags.
    private float prevTurnYaw, prevTurnPitch;
    private float placeTurnYaw, pendingTurnYaw;
    private boolean turnedSincePlace;
    private int ticksOnAir;
    private int offGroundTicks;
    private int onGroundTicks;
    private double startY;
    private double jumpY;            // where the jump left the ground
    private boolean turnedBack;      // high enough this jump to look back
    private int oldSlot = -1;
    private boolean pressingJump;
    private boolean pressingSneak;
    private boolean holdingUse;      // the use key is held up by Jump On RMB
    private boolean extraStarted;    // this jump's extra block is in
    private boolean extraSkipNextJump, extraSkipThisJump;   // every second jump
    private boolean extraWasOnGround = true;
    private boolean havePrevPos;     // last tick's position, for the speed you really move at
    private double prevPosX, prevPosZ;
    private boolean haveExtra;       // where the last extra block went
    private int extraX, extraY, extraZ;
    private float strafeSide = 1.0F; // which 45 Strafe Forward looks off by
    private boolean snapPlaced;      // Snap placed last tick: look forward at once
    private int sneakTicks;          // sneak held this many more ticks
    private int blocksLeft;
    private int totalBlocks;
    private int initialBlocks;
    private float animatedProgress = 1.0F;
    private boolean holdingSprint;   // the sprint key is held up by Sprint off
    private WorldClient world;

    // Fake Rotations: Spin's angle, and Back's eased look (live once started).
    private float fakeSpinAngle;
    private boolean fakeBackLive;
    private float fakeYaw, fakePitch;

    // Snap's eased look: from the rotation the server last saw toward the goal,
    // walking the flick curve across Ease Ticks ticks.
    private boolean easing;
    private int easeTick, easeLength = 1;
    private float easeFromYaw, easeFromPitch;
    private float easeGoalYaw, easeGoalPitch;

    // Telly's cadence, picked once per landing.
    private float turnHeightWobble = 1.0F;
    private float backYawOffset;     // off straight-back, per jump

    // This tick's view of things, for the aim helpers.
    private Vec3 eye;
    private Vec3 nextEye;
    private Look server;
    private Look last;
    private float playerYaw;
    private float moveForward, moveStrafe;
    private double posX, posY, posZ;
    private boolean aimedThisTick;

    public Scaffold() {
        super("Scaffold", "Automatically places blocks below you.", Category.MOVEMENT, Keyboard.KEY_NONE);
        instance = this;

        mode = new ModeSetting("Mode", this, "Normal", "Normal", "Snap", "Telly");

        final BooleanSupplier isSnap = () -> mode.getIndex() == SNAP;
        final BooleanSupplier isTelly = () -> mode.getIndex() == TELLY;
        final BooleanSupplier notTelly = () -> mode.getIndex() != TELLY;
        final BooleanSupplier always = () -> true;

        swing = bool("Swing", true, always);
        placeDelay = number("Place Delay", 1, 1, 10, 1, always);
        aimRandomness = number("Aim Randomness", 0.1, 0, 1, 0.01, always);
        turnSpeed = number("Turn Speed", 75, 0, 180, 1, () -> mode.getIndex() != SNAP);
        sprint = bool("Sprint", true, notTelly);
        switchBack = bool("Switch Back", true, always);
        itemSpoof = bool("Item Spoof", false, always);
        predictEye = bool("Predict Eye", false, always);
        tower = bool("Watchdog Tower", false, always);
        sneak = bool("Sneak", false, notTelly);
        sneakDelay = number("Sneak Delay", 2, 0, 10, 1, () -> mode.getIndex() != TELLY && sneak.isEnabled());
        blockCount = bool("Block Count", true, always);
        sameY = bool("Same Y", true, always);
        extraBlock = bool("Extra Block", false, () -> sameY.isEnabled());
        jumpOnRmb = bool("Jump On RMB", false, always);
        fakeRotations = mode("Fake Rotations", "Server", always, "Server", "Real", "Back", "Spin");
        fakeSpinSpeed = number("Spin Speed", 30, 5, 90, 1, () -> fakeRotations.getIndex() == FAKE_SPIN);

        easeTicks = number("Ease Ticks", 3, 1, 8, 1, isSnap);
        steepness = number("Steepness", 6, 2, 12, 0.1, isSnap);
        strafeForward = bool("Strafe Forward", true, isSnap);

        turnHeight = number("Turn Height", 1.0, 0.4, 1.2, 0.01, isTelly);
        turnSide = mode("Turn Side", "Right", isTelly, "Right", "Left");
        randomizeTicks = bool("Randomize Ticks", true, isTelly);
        forwardPitch = number("Forward Pitch", 12, -30, 60, 0.5, isTelly);
        lookJitter = number("Look Jitter", 1.5, 0, 5, 0.1, isTelly);
    }

    private BooleanSetting bool(String name, boolean value, final BooleanSupplier shown) {
        return new BooleanSetting(name, this, value) {
            @Override
            public boolean isVisible() {
                return shown.getAsBoolean();
            }
        };
    }

    private NumberSetting number(String name, double value, double min, double max, double step, final BooleanSupplier shown) {
        return new NumberSetting(name, this, value, min, max, step) {
            @Override
            public boolean isVisible() {
                return shown.getAsBoolean();
            }
        };
    }

    private ModeSetting mode(String name, String value, final BooleanSupplier shown, String... modes) {
        return new ModeSetting(name, this, value, modes) {
            @Override
            public boolean isVisible() {
                return shown.getAsBoolean();
            }
        };
    }

    // -- hooks for EntityPlayerSP / MovementFixUtil ------------------------------

    /** Sprint off: walk while bridging. The sprint key is held up meanwhile. Telly is a sprint-jump, so it always sprints. */
    private boolean holdsSprint() {
        return isToggled() && mode.getIndex() != TELLY && !sprint.isEnabled();
    }

    /** Sprint off: no sprinting while Scaffold bridges. */
    public static boolean blocksSprint() {
        return instance != null && instance.holdsSprint();
    }

    public static boolean isShouldScaffold() {
        return instance != null && instance.isToggled();
    }

    /** Scaffold's look is silent: the keys are walked relative to it, as the C++ client's Silent correction does. */
    public static boolean wantsMoveFix() {
        return instance != null && instance.isToggled() && RotationUtil.silentActive;
    }

    public static boolean isItemSpoofing() {
        return instance != null && instance.isToggled() && instance.itemSpoof != null && instance.itemSpoof.isEnabled() && instance.oldSlot >= 0 && instance.oldSlot < 9;
    }

    public static int getVisualSlot() {
        if (isItemSpoofing()) {
            return instance.oldSlot;
        }
        return mc.thePlayer != null ? mc.thePlayer.inventory.currentItem : 0;
    }

    public static ItemStack getVisualHeldItem() {
        if (isItemSpoofing() && mc.thePlayer != null) {
            return mc.thePlayer.inventory.getStackInSlot(instance.oldSlot);
        }
        return mc.thePlayer != null ? mc.thePlayer.inventory.getCurrentItem() : null;
    }

    // -- small types -------------------------------------------------------------

    private static final class Look {
        final float yaw, pitch;

        Look(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    private static final class Placement {
        final int tx, ty, tz;   // the open block to fill
        final int fx, fy, fz;   // the solid one to click
        final int facing;       // the side of it that faces the open one

        Placement(int tx, int ty, int tz, int fx, int fy, int fz, int facing) {
            this.tx = tx; this.ty = ty; this.tz = tz;
            this.fx = fx; this.fy = fy; this.fz = fz;
            this.facing = facing;
        }
    }

    /** The block to fill, and which rule picked it. */
    private static final class Target {
        final int x, y, z;
        final String why;

        Target(int x, int y, int z, String why) {
            this.x = x; this.y = y; this.z = z;
            this.why = why;
        }

        Target because(String reason) {
            return new Target(x, y, z, reason);
        }
    }

    private static final class Hit {
        boolean block;
        int x, y, z, side;
        MovingObjectPosition raw;
    }

    // The looks that can reach a face: a grid of points over it, aimed at from
    // the eyes, gives the yaw span (around the middle's yaw) and pitch span.
    private static final class FaceSpan {
        static final float MARGIN = 1.5F;   // the grid and the patch move a look this far at most

        Look center;
        float yawLo, yawHi, pitchLo = 90.0F, pitchHi = -90.0F;
        boolean anyYaw;   // over the face (looking down on a top): every yaw reaches it

        boolean reachesPitch(float pitch) {
            return pitch >= pitchLo - MARGIN && pitch <= pitchHi + MARGIN;
        }
    }

    // -- math --------------------------------------------------------------------

    private static float angleDifference(float target, float from) {
        return MathHelper.wrapAngleTo180_float(target - from);
    }

    private static int floor(double value) {
        return MathHelper.floor_double(value);
    }

    private static float clamp(float value, float lo, float hi) {
        return Math.max(lo, Math.min(hi, value));
    }

    private static double clamp(double value, double lo, double hi) {
        return Math.max(lo, Math.min(hi, value));
    }

    // A practiced flick leaves fast and settles into the goal, instead of a
    // symmetric logistic that creeps away from the start and reads as a
    // machine animating a value. Sharpness is the power on the distance left.
    private static double snapEase(double t, double sharpness) {
        double clamped = clamp(t, 0.0, 1.0);
        double k = clamp(sharpness, 1.0, 4.0);
        return 1.0 - Math.pow(1.0 - clamped, k);
    }

    private static Look rotationTo(Vec3 from, double x, double y, double z) {
        double dx = x - from.xCoord, dy = y - from.yCoord, dz = z - from.zCoord;
        double flat = Math.sqrt(dx * dx + dz * dz);
        return new Look((float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F, (float) -Math.toDegrees(Math.atan2(dy, flat)));
    }

    private static Vec3 faceCenter(int x, int y, int z, int facing) {
        return new Vec3(x + 0.5 + OFFSET_X[facing] * 0.5, y + 0.5 + OFFSET_Y[facing] * 0.5, z + 0.5 + OFFSET_Z[facing] * 0.5);
    }

    private static float mouseStep() {
        float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        return f * f * f * 8.0F * 0.15F;
    }

    // Rise's applySensitivityPatch: whole mouse counts away from the last look sent.
    private static Look sensitivityPatch(Look target, Look last) {
        float gcd = mouseStep();
        if (!(gcd > 0.0F)) return target;

        float yaw = last.yaw + Math.round(angleDifference(target.yaw, last.yaw) / gcd) * gcd;
        float pitch = last.pitch + Math.round((target.pitch - last.pitch) / gcd) * gcd;
        return new Look(yaw, clamp(pitch, -90.0F, 90.0F));
    }

    // MoveUtil.direction in degrees: the way the keys walk you.
    private static float moveDirection(float yaw, float forward, float strafe) {
        float direction = yaw;
        if (forward < 0.0F) direction += 180.0F;

        float scale = forward < 0.0F ? -0.5F : (forward > 0.0F ? 0.5F : 1.0F);
        if (strafe > 0.0F) direction -= 90.0F * scale;
        else if (strafe < 0.0F) direction += 90.0F * scale;

        return direction;
    }

    // -- the world ---------------------------------------------------------------

    private Block blockAt(int x, int y, int z) {
        return mc.theWorld.getBlockState(new BlockPos(x, y, z)).getBlock();
    }

    private boolean replaceable(int x, int y, int z) {
        Block block = blockAt(x, y, z);
        return block == null || block.isReplaceable(mc.theWorld, new BlockPos(x, y, z));
    }

    private boolean air(int x, int y, int z) {
        Block block = blockAt(x, y, z);
        return block == null || block.getMaterial() == Material.air;
    }

    private boolean interactable(int x, int y, int z) {
        Block block = blockAt(x, y, z);
        return block instanceof BlockContainer || block instanceof BlockWorkbench || block instanceof BlockAnvil
                || block instanceof BlockBed || block instanceof BlockDoor || block instanceof BlockTrapDoor
                || block instanceof BlockFenceGate || block instanceof BlockFence || block instanceof BlockButton
                || block instanceof BlockLever;
    }

    private boolean solid(int x, int y, int z) {
        return !replaceable(x, y, z) && !interactable(x, y, z);
    }

    // What a look at this rotation from the eyes hits within reach.
    private Hit trace(Vec3 from, Look look) {
        Vec3 dir = WorldUtil.getVectorForRotation(look.pitch, look.yaw);
        Hit out = new Hit();
        out.raw = mc.theWorld.rayTraceBlocks(from, from.addVector(dir.xCoord * REACH, dir.yCoord * REACH, dir.zCoord * REACH));

        if (out.raw != null && out.raw.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && out.raw.getBlockPos() != null
                && out.raw.sideHit != null) {
            BlockPos at = out.raw.getBlockPos();
            out.x = at.getX();
            out.y = at.getY();
            out.z = at.getZ();
            out.side = out.raw.sideHit.getIndex();
            out.block = true;
        }

        return out;
    }

    // RayCastUtil (strict): the look is on the face to click.
    private static boolean overBlock(Hit ray, Placement p) {
        return ray.block && ray.x == p.fx && ray.y == p.fy && ray.z == p.fz && ray.side == p.facing;
    }

    // Lands on the face from the eyes -- and, given nextEye, from there too.
    private boolean lands(Look look, Placement p) {
        return overBlock(trace(eye, look), p) && (nextEye == null || overBlock(trace(nextEye, look), p));
    }

    // -- choosing the block ------------------------------------------------------

    private static final int[][] SIDES = { { -1, 0, 0 }, { 1, 0, 0 }, { 0, -1, 0 }, { 0, 1, 0 }, { 0, 0, -1 }, { 0, 0, 1 } };
    private static final int[][] FLAT_SIDES = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };

    // Offsets nearest-first from the block under your feet, 3 out and only the
    // three layers under you; on a tie, the layer under your feet. Clutch only.
    private static final List<int[]> ORDER = buildOrder();

    private static List<int[]> buildOrder() {
        List<int[]> out = new ArrayList<int[]>();

        for (int dx = -3; dx <= 3; ++dx) {
            for (int dy = -3; dy <= -1; ++dy) {
                for (int dz = -3; dz <= 3; ++dz) {
                    out.add(new int[] { dx, dy, dz, dx * dx + (dy + 1) * (dy + 1) + dz * dz });
                }
            }
        }

        // Collections.sort is stable.
        Collections.sort(out, new Comparator<int[]>() {
            @Override
            public int compare(int[] a, int[] b) {
                if (a[3] != b[3]) return Integer.compare(a[3], b[3]);
                return Boolean.compare(b[1] == -1, a[1] == -1);
            }
        });

        return out;
    }

    private boolean besideSolid(int x, int y, int z) {
        for (int[] side : SIDES) {
            if (solid(x + side[0], y + side[1], z + side[2])) return true;
        }

        return false;
    }

    // The block to fill: the one under your feet, nothing beside you.
    //
    //   - the block under your feet (on layer, else the layer under you and
    //     down to two more while those are open), when it touches something;
    //   - else one of its four sides that does, the one nearest where you came
    //     from: the step a diagonal or a sprint past the edge needs;
    //   - else the line back along your move to where the bridge is built,
    //     nearest your feet: a jump that carried you more than a block out;
    //   - else, only when clutching (falling off), the nearest open block next
    //     to anything solid within 3, as Rise does, to catch you.
    //
    // Standing on something (the block under your feet is solid): nothing.
    private Target placePossibility(double px, double py, double pz, double fromX, double fromZ, Integer layerY, boolean clutch) {
        int fx = floor(px), fy = floor(py), fz = floor(pz);
        int top = layerY != null ? layerY : fy - 1;
        int bottom = layerY != null ? layerY : fy - 3;

        for (int y = top; y >= bottom; --y) {
            if (!replaceable(fx, y, fz)) return null;

            if (besideSolid(fx, y, fz)) return new Target(fx, y, fz, y == top ? "under" : "under-low");

            Target best = null;
            double bestDistance = 0.0;

            for (int[] side : FLAT_SIDES) {
                int x = fx + side[0], z = fz + side[1];
                if (!replaceable(x, y, z) || !besideSolid(x, y, z)) continue;

                double dx = x + 0.5 - fromX, dz = z + 0.5 - fromZ;
                double distance = dx * dx + dz * dz;

                if (best == null || distance < bestDistance) {
                    best = new Target(x, y, z, y == top ? "side" : "side-low");
                    bestDistance = distance;
                }
            }

            if (best != null) return best;

            // Past the end of the bridge by more than a block (a telly's turn
            // back takes a few ticks of the jump): cells within 0.75 of the
            // line back along the move, up to 3.5 out, the one nearest your feet.
            double mx = px - fromX, mz = pz - fromZ;
            double len = Math.sqrt(mx * mx + mz * mz);

            if (len > 0.01) {
                double bx = -mx / len, bz = -mz / len;

                for (int dx = -3; dx <= 3; ++dx) {
                    for (int dz = -3; dz <= 3; ++dz) {
                        int x = fx + dx, z = fz + dz;
                        double cx = x + 0.5 - px, cz = z + 0.5 - pz;
                        double along = cx * bx + cz * bz;
                        double across = Math.abs(cx * bz - cz * bx);

                        if (along <= 0.0 || along > 3.5 || across > 0.75 || !replaceable(x, y, z) || !besideSolid(x, y, z)) continue;

                        double distance = cx * cx + cz * cz;

                        if (best == null || distance < bestDistance) {
                            best = new Target(x, y, z, y == top ? "behind" : "behind-low");
                            bestDistance = distance;
                        }
                    }
                }

                if (best != null) return best;
            }
        }

        if (!clutch) return null;

        for (int[] o : ORDER) {
            int x = fx + o[0], y = fy + o[1], z = fz + o[2];

            if ((layerY == null || y == layerY) && replaceable(x, y, z) && besideSolid(x, y, z)) {
                return new Target(x, y, z, "clutch");
            }
        }

        return null;
    }

    // EnumFacing order: down 0, up 1, north 2, south 3, west 4, east 5.
    private static final int[][] HORIZONTAL = { { 0, -1, 3 }, { 0, 1, 2 }, { -1, 0, 5 }, { 1, 0, 4 } };

    // Every face the target block can be placed against: each solid neighbour
    // beside it, then the one below. Snap picks among these only.
    private List<Placement> placeFaces(Target t) {
        List<Placement> out = new ArrayList<Placement>();

        for (int[] o : HORIZONTAL) {
            if (solid(t.x + o[0], t.y, t.z + o[1])) out.add(new Placement(t.x, t.y, t.z, t.x + o[0], t.y, t.z + o[1], o[2]));
        }

        if (solid(t.x, t.y - 1, t.z)) out.add(new Placement(t.x, t.y, t.z, t.x, t.y - 1, t.z, FACING_UP));

        return out;
    }

    // PlayerUtil.getEnumFacing: a solid neighbour beside the target, the one
    // nearest the way you're looking first; else the one below; else above.
    private Placement enumFacing(Target t, final float lookYaw, boolean allowDown) {
        List<int[]> found = new ArrayList<int[]>();

        for (int[] o : HORIZONTAL) {
            if (solid(t.x + o[0], t.y, t.z + o[1])) found.add(o);
        }

        if (!found.isEmpty()) {
            Collections.sort(found, new Comparator<int[]>() {
                @Override
                public int compare(int[] a, int[] b) {
                    return Float.compare(off(a), off(b));
                }

                private float off(int[] o) {
                    float direction = (float) Math.toDegrees(Math.atan2(o[1], o[0]));
                    return Math.abs(angleDifference(direction, lookYaw + 90.0F));
                }
            });

            int[] o = found.get(0);
            return new Placement(t.x, t.y, t.z, t.x + o[0], t.y, t.z + o[1], o[2]);
        }

        if (solid(t.x, t.y - 1, t.z)) return new Placement(t.x, t.y, t.z, t.x, t.y - 1, t.z, FACING_UP);
        if (allowDown && solid(t.x, t.y + 1, t.z)) return new Placement(t.x, t.y, t.z, t.x, t.y + 1, t.z, FACING_DOWN);

        return null;
    }

    private static FaceSpan spanOf(Vec3 from, Placement p) {
        Vec3 c = faceCenter(p.fx, p.fy, p.fz, p.facing);
        int sx = OFFSET_X[p.facing] == 0 ? 1 : 0, sy = OFFSET_Y[p.facing] == 0 ? 1 : 0, sz = OFFSET_Z[p.facing] == 0 ? 1 : 0;

        FaceSpan span = new FaceSpan();
        span.center = rotationTo(from, c.xCoord, c.yCoord, c.zCoord);

        for (int a = -2; a <= 2; ++a) {
            for (int b = -2; b <= 2; ++b) {
                for (int d = -2; d <= 2; ++d) {
                    Look r = rotationTo(from, c.xCoord + a * sx * 0.25, c.yCoord + b * sy * 0.25, c.zCoord + d * sz * 0.25);
                    float dYaw = angleDifference(r.yaw, span.center.yaw);

                    span.yawLo = Math.min(span.yawLo, dYaw);
                    span.yawHi = Math.max(span.yawHi, dYaw);
                    span.pitchLo = Math.min(span.pitchLo, r.pitch);
                    span.pitchHi = Math.max(span.pitchHi, r.pitch);
                }
            }
        }

        span.anyYaw = span.yawHi - span.yawLo > 170.0F;
        return span;
    }

    // -- aiming ------------------------------------------------------------------

    // The look, nearest the one the server has, that still lands on this face;
    // null when nothing reaches it. Rise's getRotations(0): from the middle of
    // the target block, look out level in eight directions; the points those
    // land on the clicked face, aimed at from your eyes, are the choices, and
    // the one nearest the current look wins. Else the middle of the face.
    private Look aimToPlace(Placement p) {
        double lowered = posY + (p.ty - floor(posY)) + 0.5 - (random.nextDouble() - 0.5) * aimRandomness.getValue();
        Vec3 from = new Vec3(posX, lowered, posZ);

        float wrapped = MathHelper.wrapAngleTo180_float(playerYaw);
        float rest = Math.abs(wrapped % 90.0F);
        boolean nearAxis = rest <= 10.0F || rest >= 80.0F;

        Look best = null;
        float bestDistance = 0.0F;

        for (int offset = nearAxis ? -135 : -180; offset <= (nearAxis ? 135 : 180); offset += 45) {
            Hit ray = trace(from, new Look(playerYaw + offset * 3.0F, 0.0F));

            if (!ray.block || ray.raw == null || ray.raw.hitVec == null) continue;

            Vec3 at = ray.raw.hitVec;
            Look aim = rotationTo(eye, at.xCoord, at.yCoord, at.zCoord);

            if (!overBlock(trace(eye, aim), p)) continue;

            float dYaw = angleDifference(aim.yaw, server.yaw), dPitch = aim.pitch - server.pitch;
            float distance = dYaw * dYaw + dPitch * dPitch;

            if (best == null || distance < bestDistance) {
                best = aim;
                bestDistance = distance;
            }
        }

        if (best != null) return best;

        Vec3 c = faceCenter(p.fx, p.fy, p.fz, p.facing);
        Look aim = rotationTo(eye, c.xCoord, c.yCoord, c.zCoord);

        return overBlock(trace(eye, aim), p) ? aim : null;
    }

    private void reaim(Placement p) {
        Look aim = aimToPlace(p);

        if (aim != null) {
            targetYaw = aim.yaw;
            targetPitch = aim.pitch;
            aimedThisTick = true;
        }
    }

    // Telly's look back: the yaw held straight behind the way you are WALKING
    // (give or take the jump's small offset) and only the pitch searched, from
    // the one that lands nearest the current look. On a diagonal the corner
    // face's edge sits on the straight-back line, so the yaw is pulled just
    // inside the face's span. A pitch whose neighbours land too is preferred,
    // so the mouse grid can't round it off the face. Nothing at that yaw: reaim.
    private void aimBack(Placement p) {
        Look aim = backAim(p);

        if (aim == null) {
            reaim(p);
            return;
        }

        targetYaw = aim.yaw;
        targetPitch = aim.pitch;
        aimedThisTick = true;
    }

    // The raycast aim itself, pure: the look, or null when no pitch at that
    // yaw lands. Telly uses it, and Snap picks among its faces with it.
    private Look backAim(Placement p) {
        float reference = server.pitch;
        FaceSpan span = spanOf(eye, p);

        float backYaw = moveDirection(playerYaw, moveForward, moveStrafe) + 180.0F + backYawOffset;

        if (!span.anyYaw) {
            float inset = Math.min(4.0F, (span.yawHi - span.yawLo) * 0.25F);
            float off = clamp(angleDifference(backYaw, span.center.yaw), span.yawLo + inset, span.yawHi - inset);

            backYaw = span.center.yaw + off;
        }

        boolean[] hits = new boolean[71];

        for (int i = 0; i < 71; ++i) {
            hits[i] = span.reachesPitch(20.0F + i) && overBlock(trace(eye, new Look(backYaw, 20.0F + i)), p);
        }

        Float best = null;
        boolean bestInner = false;

        for (int i = 0; i < 71; ++i) {
            if (!hits[i]) continue;

            float pitch = 20.0F + i;
            boolean inner = i > 0 && i < 70 && hits[i - 1] && hits[i + 1];

            if (best == null || (inner && !bestInner)
                    || (inner == bestInner && Math.abs(pitch - reference) < Math.abs(best - reference))) {
                best = pitch;
                bestInner = inner;
            }
        }

        return best == null ? null : new Look(backYaw, best);
    }

    // Snap's look ahead. Strafe Forward turns it 45 off the walk; the move fix
    // then walks you straight on with W+A or W+D held, sprinting, and the turn
    // back to the block is ~135 instead of ~180. The side is the one nearer the
    // look the server has, kept unless the other is clearly nearer.
    private float snapAheadYaw() {
        float walk = moveDirection(playerYaw, moveForward, moveStrafe);

        if (!strafeForward.isEnabled()) return walk;

        float kept = Math.abs(angleDifference(walk + 45.0F * strafeSide, server.yaw));
        float other = Math.abs(angleDifference(walk - 45.0F * strafeSide, server.yaw));

        if (other + 10.0F < kept) strafeSide = -strafeSide;

        return walk + 45.0F * strafeSide;
    }

    // -- inventory ---------------------------------------------------------------

    private static boolean bridgeBlock(ItemStack stack) {
        return stack != null && stack.stackSize > 0 && InventoryUtil.isValidBlock(stack.getItem());
    }

    // The last hotbar slot with bridging blocks, or the current one if none.
    private static int blockSlot() {
        int slot = mc.thePlayer.inventory.currentItem;

        for (int i = 0; i < 9; ++i) {
            if (bridgeBlock(mc.thePlayer.inventory.getStackInSlot(i))) slot = i;
        }

        return slot;
    }

    private static boolean haveBlocks() {
        for (int i = 0; i < 9; ++i) {
            if (bridgeBlock(mc.thePlayer.inventory.getStackInSlot(i))) return true;
        }

        return false;
    }

    // -- keys --------------------------------------------------------------------

    private static void press(KeyBinding key, boolean pressed) {
        KeyBinding.setKeyBindState(key.getKeyCode(), pressed);
    }

    // The key as you hold it, not as a module holds it.
    private static boolean physical(KeyBinding key) {
        return GameSettings.isKeyDown(key);
    }

    // Drop the clicks queued on a key (what isPressed() hands out), so the
    // press itself never reaches the game either.
    private static void clearPresses(KeyBinding key) {
        int guard = 0;
        while (key.isPressed() && ++guard < 64) {
            // drained
        }
    }

    // -- the look ----------------------------------------------------------------

    private static Look serverLook() {
        return RotationUtil.silentActive
                ? new Look(RotationUtil.serverYaw, RotationUtil.serverPitch)
                : new Look(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
    }

    // Sent as is: it's on the mouse grid already.
    private static void sendLook(Look look) {
        Look before = serverLook();
        RotationUtil.prevServerYaw = before.yaw;
        RotationUtil.prevServerPitch = before.pitch;
        RotationUtil.serverYaw = look.yaw;
        RotationUtil.serverPitch = look.pitch;
        RotationUtil.silentActive = true;
    }

    // -- lifecycle ---------------------------------------------------------------

    @Override
    public void onEnable() {
        if (mc.thePlayer == null) return;

        world = mc.theWorld;
        oldSlot = mc.thePlayer.inventory.currentItem;
        targetYaw = mc.thePlayer.rotationYaw - 180.0F;
        targetPitch = 90.0F;
        ticksOnAir = 0;
        offGroundTicks = 0;
        onGroundTicks = 0;
        startY = Math.floor(mc.thePlayer.posY);

        easing = false;
        easeTick = 0;
        turnHeightWobble = turnHeight.getValueFloat();
        jumpY = mc.thePlayer.posY;
        turnedBack = false;
        backYawOffset = 0.0F;
        sneakTicks = 0;
        havePrevPos = false;
        extraStarted = false;
        extraSkipNextJump = extraSkipThisJump = false;
        extraWasOnGround = true;

        // Initialize block counts on enable
        int hotbarCount = 0;
        int invCount = 0;
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (bridgeBlock(stack)) hotbarCount += stack.stackSize;
        }
        for (int i = 0; i < 36; ++i) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (bridgeBlock(stack)) invCount += stack.stackSize;
        }
        blocksLeft = hotbarCount;
        totalBlocks = invCount;
        initialBlocks = Math.max(hotbarCount, totalBlocks);
        animatedProgress = 1.0F;
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer != null && mc.gameSettings != null) {
            GameSettings settings = mc.gameSettings;

            if (switchBack.isEnabled() && oldSlot >= 0 && mc.thePlayer.inventory.currentItem != oldSlot) {
                mc.thePlayer.inventory.currentItem = oldSlot;   // the controller syncs it next tick
            }

            if (holdingSprint) press(settings.keyBindSprint, physical(settings.keyBindSprint));
            if (pressingJump) press(settings.keyBindJump, physical(settings.keyBindJump));
            if (holdingUse) press(settings.keyBindUseItem, physical(settings.keyBindUseItem));
            if (pressingSneak) press(settings.keyBindSneak, physical(settings.keyBindSneak));
        }

        RotationUtil.resetSilentRotation();
        FakeRotation.clear();
        fakeBackLive = false;

        pressingJump = false;
        pressingSneak = false;
        holdingUse = false;
        snapPlaced = false;
        sneakTicks = 0;
        holdingSprint = false;
        oldSlot = -1;
        easing = false;
        easeTick = 0;
        world = null;
    }

    @Override
    public void onUpdate() {
        if (mc.thePlayer == null || mc.theWorld == null || mc.playerController == null || mc.gameSettings == null) return;

        // A new world: off, as the C++ client does on a world load.
        if (world != null && world != mc.theWorld) {
            setToggled(false);
            return;
        }
        world = mc.theWorld;

        final EntityPlayerSP player = mc.thePlayer;
        final GameSettings settings = mc.gameSettings;

        // Telly only jumps on Jump On RMB. Not holding right-click (or Jump On
        // RMB off) it doesn't jump, so it bridges as Normal does.
        final boolean rmbHeldNow = jumpOnRmb.isEnabled() && physical(settings.keyBindUseItem);
        final int modeIndex = mode.getIndex() == TELLY && !rmbHeldNow ? NORMAL : mode.getIndex();

        posX = player.posX;
        posY = player.posY;
        posZ = player.posZ;
        final boolean onGround = player.onGround;

        // The keys as you hold them (the move fix rewrites the player's own input).
        moveForward = (settings.keyBindForward.isKeyDown() ? 1.0F : 0.0F) - (settings.keyBindBack.isKeyDown() ? 1.0F : 0.0F);
        moveStrafe = (settings.keyBindLeft.isKeyDown() ? 1.0F : 0.0F) - (settings.keyBindRight.isKeyDown() ? 1.0F : 0.0F);
        final boolean isMoving = moveForward != 0.0F || moveStrafe != 0.0F;
        final boolean jumpKey = physical(settings.keyBindJump);
        playerYaw = player.rotationYaw;

        // How far you really moved last tick: holding the keys in the air, the
        // sprint's push about cancels the air's drag, so this is what the next
        // ticks move too. A teleport-sized step is no speed.
        double stepX = player.motionX, stepZ = player.motionZ;

        if (havePrevPos && Math.abs(posX - prevPosX) < 2.0 && Math.abs(posZ - prevPosZ) < 2.0) {
            stepX = posX - prevPosX;
            stepZ = posZ - prevPosZ;
        }

        prevPosX = posX;
        prevPosZ = posZ;
        havePrevPos = true;

        offGroundTicks = onGround ? 0 : offGroundTicks + 1;
        onGroundTicks = onGround ? onGroundTicks + 1 : 0;

        // One cadence per landing: the turn height wanders a little.
        if (onGround && onGroundTicks == 1) {
            boolean wobble = modeIndex == TELLY && randomizeTicks.isEnabled();

            turnHeightWobble = turnHeight.getValueFloat() + (wobble ? (float) (random.nextDouble() * 0.1) : 0.0F);
            backYawOffset = (float) (random.nextDouble() - 0.5) * 2.0F * lookJitter.getValueFloat();
        }

        // Telly turns back once the jump has lifted you Turn Height above where
        // it left the ground -- or once it's well into the jump -- and stays
        // turned until you land.
        if (onGround) {
            jumpY = posY;
            turnedBack = false;
        } else if (posY - jumpY >= turnHeightWobble || offGroundTicks >= 4) {
            turnedBack = true;
        }

        // Jump On RMB: right-click held while you move. The use key stays up
        // meanwhile, or vanilla's own right-click places a block wherever the
        // crosshair is, beside you.
        final boolean hold = jumpOnRmb.isEnabled() && physical(settings.keyBindUseItem);
        final boolean rmbJump = hold && isMoving;

        if (hold) {
            press(settings.keyBindUseItem, false);
            clearPresses(settings.keyBindUseItem);
            holdingUse = true;
        } else if (holdingUse) {
            press(settings.keyBindUseItem, physical(settings.keyBindUseItem));
            holdingUse = false;
        }

        // Off the ground whenever you move holding right-click, as long as there
        // are blocks to bridge with. It is not your jump key, so Same Y keeps
        // the layer through it.
        final boolean jumpingNow;
        {
            boolean want = rmbJump && onGround && haveBlocks();

            if (want || pressingJump) press(settings.keyBindJump, want || physical(settings.keyBindJump));

            pressingJump = want;
            jumpingNow = want;
        }

        // Sprint off: hold the sprint key up and drop the sprint before the
        // player's update, so vanilla never sprints this tick.
        if (holdsSprint()) {
            press(settings.keyBindSprint, false);
            player.setSprinting(false);
            holdingSprint = true;
        } else if (holdingSprint) {
            press(settings.keyBindSprint, physical(settings.keyBindSprint));
            holdingSprint = false;
        }

        // Block Count: what the hotbar and full inventory have left to bridge with.
        {
            int hotbar = 0;
            int total = 0;

            for (int i = 0; i < 9; ++i) {
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (bridgeBlock(stack)) hotbar += stack.stackSize;
            }
            for (int i = 0; i < 36; ++i) {
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (bridgeBlock(stack)) total += stack.stackSize;
            }

            blocksLeft = hotbar;
            totalBlocks = total;
            if (total > initialBlocks || initialBlocks <= 0) {
                initialBlocks = total;
            }
        }

        // Sneak (Normal, Snap): on the ground over an open block -- an edge --
        // sneak, and keep sneaking Sneak Delay ticks after it is filled.
        {
            boolean use = sneak.isEnabled() && modeIndex != TELLY && !jumpKey && haveBlocks();

            if (use && onGround && replaceable(floor(posX), floor(posY) - 1, floor(posZ))) {
                sneakTicks = sneakDelay.getValueInt() + 1;
            } else if (sneakTicks > 0) {
                --sneakTicks;
            }

            boolean want = use && sneakTicks > 0;

            if (want || pressingSneak) press(settings.keyBindSneak, want || physical(settings.keyBindSneak));

            pressingSneak = want;
        }

        // SlotComponent.setSlot(SlotUtil.findBlock()); onPlayerRightClick syncs it.
        int slot = blockSlot();
        if (slot != player.inventory.currentItem) player.inventory.currentItem = slot;

        if (!bridgeBlock(player.getHeldItem())) {
            // Nothing to bridge with: the look goes back to your own.
            if (RotationUtil.silentActive) RotationUtil.resetSilentRotation();
            FakeRotation.clear();
            fakeBackLive = false;
            return;
        }

        // Same Y: build on the layer you started from while you're in the air
        // without holding jump (Telly's jumps, or a speed's hops).
        final boolean sameYNow = sameY.isEnabled() && !jumpKey && isMoving;
        final int belowY = floor(posY) - 1;
        final int startLayer = (int) startY - 1;

        if (jumpKey && !isMoving) {
            ticksOnAir = replaceable(floor(posX), belowY, floor(posZ)) ? ticksOnAir + 1 : 0;
        } else {
            boolean open = sameYNow ? replaceable(floor(posX), startLayer, floor(posZ)) : replaceable(floor(posX), belowY, floor(posZ));
            ticksOnAir = open ? ticksOnAir + 1 : 0;
        }

        boolean readyToPlace = ticksOnAir >= Math.max(1, placeDelay.getValueInt());

        // On solid ground now, but this tick's move ends over the open layer.
        final boolean edgeNext = ticksOnAir <= 0 && onGround
                && replaceable(floor(posX + player.motionX), sameYNow ? startLayer : belowY, floor(posZ + player.motionZ));

        server = serverLook();

        // The block under you; ahead of a solid one, the block this tick's move
        // (then the next two) takes you over, so the look can get there first
        // -- it is only placed once you are over it. Clutch: fallen below where
        // the jump left the ground, catch on anything near.
        final double motionX = player.motionX, motionZ = player.motionZ;
        final double cameX = posX - motionX, cameZ = posZ - motionZ;
        final Integer layer = sameYNow ? Integer.valueOf(startLayer) : null;
        final boolean clutch = !onGround && player.motionY < 0.0 && posY < jumpY - 0.5;

        Target target = placePossibility(posX, posY, posZ, cameX, cameZ, layer, clutch);

        for (int ahead = 1; target == null && ahead <= 3 && modeIndex != NORMAL; ++ahead) {
            Target found = placePossibility(posX + motionX * ahead, posY, posZ + motionZ * ahead, cameX, cameZ, layer, false);
            target = found != null ? found.because("ahead") : null;
        }

        // A block ahead is only aimed at, never placed: it is not under you yet.
        boolean aimOnly = target != null && "ahead".equals(target.why);

        // Extra Block (Same Y): once a jump, one block on top of the bridge, one
        // layer up, where you will land -- so you land on it and jump off it.
        // Every second jump; only once the bridge block under your feet is in.
        if (onGround) {
            extraStarted = false;
        } else if (extraWasOnGround) {
            extraSkipThisJump = extraSkipNextJump;
            extraSkipNextJump = false;
        }

        extraWasOnGround = onGround;

        if (extraBlock.isEnabled() && sameYNow && !onGround && !extraStarted && !extraSkipThisJump && ticksOnAir <= 0
                && posY >= startY + 1.0 && player.motionY < 0.2) {
            final int ey = (int) startY;

            // The ticks until the fall crosses the extra's top (vanilla's
            // gravity), and where your box lands then. Not within 4: not yet.
            int land = 0;
            {
                double y = posY, vy = player.motionY;

                for (int tick = 1; tick <= 4; ++tick) {
                    y += vy;

                    if (y < startY + 1.0) {
                        land = tick;
                        break;
                    }

                    vy = (vy - 0.08) * 0.98;
                }
            }

            final double lx = posX + stepX * land, lz = posZ + stepZ * land;
            Target best = null;
            double bestScore = 0.0;

            if (land > 0) {
                for (int x = floor(posX - 0.29); x <= floor(posX + 0.29); ++x) {
                    for (int z = floor(posZ - 0.29); z <= floor(posZ + 0.29); ++z) {
                        if (!replaceable(x, ey, z) || !solid(x, ey - 1, z)) continue;

                        // A cell you will land on; too fast for that, the bridge
                        // block before it, but only on the last tick still high enough.
                        boolean landsHere = x >= floor(lx - 0.29) && x <= floor(lx + 0.29) && z >= floor(lz - 0.29) && z <= floor(lz + 0.29);

                        if (!landsHere && land > 1) continue;

                        double ahead = (x + 0.5 - posX) * stepX + (z + 0.5 - posZ) * stepZ;
                        double score = ahead + (landsHere ? 100.0 : 0.0);

                        if (best == null || score > bestScore) {
                            best = new Target(x, ey, z, "extra");
                            bestScore = score;
                        }
                    }
                }
            }

            if (best != null) {
                target = best;
                readyToPlace = true;

                // The look-ahead's aim-only rule was for the target this replaced.
                aimOnly = false;
            }
        }

        final boolean extraDue = target != null && "extra".equals(target.why);

        // The extra always goes on the TOP of the bridge block under it.
        Placement place = target == null ? null
                : extraDue ? new Placement(target.x, target.y, target.z, target.x, target.y - 1, target.z, FACING_UP)
                : enumFacing(target, server.yaw, false);

        final double eyeHeight = player.getEyeHeight();
        eye = new Vec3(posX, posY + eyeHeight, posZ);

        // Predict Eye: where this tick's move takes the eyes, so a place only
        // goes out if it lands from both -- whichever the server checks it from.
        nextEye = predictEye.isEnabled()
                ? new Vec3(posX + player.motionX, posY + eyeHeight + (onGround ? 0.0 : player.motionY), posZ + player.motionZ)
                : null;

        aimedThisTick = false;
        boolean onFace = false;
        boolean forwardLook = false;   // Telly looks ahead this tick, not at the face

        // The last look sent, exactly as sent. Re-centring it around your own yaw
        // jumped it by a whole turn after a turn past 180 (Telly's long way
        // round), and the next look sent with it was a 360 jump: AimModulo360.
        final float lastYaw = server.yaw;
        last = new Look(lastYaw, server.pitch);

        if (place != null) {
            onFace = lands(server, place);

            switch (modeIndex) {
                case NORMAL:
                    if (readyToPlace && !onFace) reaim(place);
                    break;

                case SNAP: {
                    float currentYaw = playerYaw;
                    float eventYaw = server.yaw;
                    float yawDiffTo180 = coolware.module.movement.scaffold.Rotation.calculateYawDiffTo180(currentYaw, eventYaw);
                    boolean isDiag = (Math.abs(moveForward) > 0.0F && Math.abs(moveStrafe) > 0.0F);
                    float diagonalYaw = coolware.module.movement.scaffold.Rotation.calculateDiagonalYaw(currentYaw, yawDiffTo180, eventYaw, isDiag);

                    // Always pass sentinel so calculateModeRotation sets both yaw AND pitch (85°).
                    int rotMode = 1;
                    float[] modeRot = coolware.module.movement.scaffold.Rotation.calculateModeRotation(
                            rotMode, currentYaw, yawDiffTo180, diagonalYaw, -180.0F, 0.0F, onGround);

                    targetYaw = modeRot[0];
                    targetPitch = modeRot[1]; // 85.0F

                    // If we have a place target, aim pitch directly at the face center so
                    // the raycast always hits regardless of easing interpolation.
                    if (place != null) {
                        Vec3 fc = faceCenter(place.fx, place.fy, place.fz, place.facing);
                        Look faceAim = rotationTo(eye, fc.xCoord, fc.yCoord, fc.zCoord);
                        targetPitch = faceAim.pitch;
                    }

                    if (tower.isEnabled() && jumpKey) {
                        float[] towerRot = coolware.module.movement.scaffold.Rotation.applyTowerRotation(
                                eventYaw, currentYaw, targetYaw, targetPitch, onGroundTicks, 35.0F, 50.0F, 40.0F, 55.0F);
                        targetYaw = towerRot[0];
                        targetPitch = towerRot[1];
                    }

                    targetYaw = coolware.util.RotationUtil.wrapAngleDiff(targetYaw, eventYaw);
                    aimedThisTick = true;
                    break;
                }

                case TELLY:
                    // On the ground and through the rise, face the way you walk;
                    // high enough, turn back and hold the yaw, the pitch places.
                    if (!turnedBack) {
                        forwardLook = true;
                        targetYaw = playerYaw;
                        targetPitch = forwardPitch.getValueFloat();
                        readyToPlace = false;
                    } else if (!onFace) {
                        aimBack(place);
                    }
                    break;

                default:
                    break;
            }
        } else if (modeIndex != NORMAL) {
            targetYaw = modeIndex == SNAP ? snapAheadYaw() : playerYaw;

            if (modeIndex == TELLY) {
                forwardLook = true;
                targetPitch = forwardPitch.getValueFloat();
            }
        }

        // Snap: the tick after a place, straight forward at once -- no flick,
        // and no place. Only once the block under you is in, and not while the
        // extra block is going in (a base then the extra on two ticks running).
        final boolean snapForward = modeIndex == SNAP && snapPlaced && ticksOnAir <= 0 && !edgeNext && !extraDue;
        snapPlaced = false;

        if (snapForward) {
            targetYaw = snapAheadYaw();
            targetPitch = server.pitch;
            readyToPlace = false;
        }

        // Snap, Jump On RMB: the tick the jump goes off, look straight along the
        // walk and place nothing, so the sprint boost carries you straight.
        final boolean snapJump = modeIndex == SNAP && rmbJump && jumpingNow;

        if (snapJump) {
            targetYaw = moveDirection(playerYaw, moveForward, moveStrafe);
            targetPitch = server.pitch;
            readyToPlace = false;
        }

        // Watchdog Tower: holding jump on the ground, nothing is placed and the
        // yaw faces the way you walk; the place comes once you're off the ground.
        if (tower.isEnabled() && jumpKey && onGround) {
            readyToPlace = false;
            targetYaw = moveDirection(playerYaw, moveForward, moveStrafe);
            aimedThisTick = true;
        }

        // Next to your own yaw so it never jumps by whole turns.
        targetYaw = playerYaw + angleDifference(targetYaw, playerYaw);
        targetPitch = clamp(targetPitch, -90.0F, 90.0F);

        // Snap: a practiced flick that leaves hard and settles over a tick or
        // two; the length wanders by one and the sharpness each time. A new
        // goal far from the one in progress starts the arc again.
        if (modeIndex == SNAP && (snapForward || snapJump)) {
            easing = false;
        } else if (modeIndex == SNAP) {
            // When we are already in the air and must place now, snap immediately --
            // easing over multiple ticks causes the player to walk off the edge.
            final boolean urgentPlace = place != null && readyToPlace && ticksOnAir > 0;

            if (urgentPlace) {
                easing = false;
            } else {
                final Look goal = new Look(targetYaw, targetPitch);

                boolean restart = !easing || easeTick >= easeLength
                        || Math.abs(angleDifference(targetYaw, easeGoalYaw)) > 10.0F
                        || Math.abs(targetPitch - easeGoalPitch) > 10.0F;

                if (restart) {
                    easing = true;
                    easeTick = 0;
                    easeLength = Math.max(1, Math.min(6, easeTicks.getValueInt() + (random.nextDouble() < 0.5 ? -1 : 1)));
                    easeFromYaw = lastYaw;
                    easeFromPitch = server.pitch;
                    easeGoalYaw = targetYaw;
                    easeGoalPitch = targetPitch;
                }

                double t = clamp((easeTick + 1) / (double) easeLength, 0.0, 1.0);
                double e = snapEase(t, steepness.getValue() / 3.0);

                targetYaw = easeFromYaw + angleDifference(easeGoalYaw, easeFromYaw) * (float) e;
                targetPitch = clamp(easeFromPitch + (easeGoalPitch - easeFromPitch) * (float) e, -90.0F, 90.0F);

                ++easeTick;

                // A block is due and the flick hasn't reached the face yet: snap
                // the rest of the way this tick.
                if (place != null && readyToPlace && aimedThisTick
                        && !lands(sensitivityPatch(new Look(targetYaw, targetPitch), last), place)
                        && lands(sensitivityPatch(goal, last), place)) {
                    targetYaw = goal.yaw;
                    targetPitch = goal.pitch;
                    easing = false;
                }
            }
        }

        final float speedSetting = turnSpeed.getValueFloat();

        if (speedSetting > 0.0F && modeIndex == NORMAL) {
            // Normal: a re-aim moves at the turn speed instead of in one tick.
            if (aimedThisTick) {
                targetYaw = server.yaw + clamp(angleDifference(targetYaw, server.yaw), -speedSetting, speedSetting);
                targetPitch = clamp(server.pitch + clamp(targetPitch - server.pitch, -speedSetting, speedSetting), -90.0F, 90.0F);
            }
        } else if (modeIndex == TELLY) {
            // A human wrist, not a servo: capped well under a half-turn a tick,
            // and Turn Speed 0 becomes a fast flick rather than a snap.
            final float speed = Math.min(speedSetting > 0.0F ? speedSetting : 60.0F, 90.0F);
            final float side = turnSide.getIndex() == 0 ? 1.0F : -1.0F;   // yaw grows turning right

            if (forwardLook) {
                // The look snaps forward so the sprint picks up at once; the
                // pitch eases so the drop onto the block stays smooth.
                float jitter = lookJitter.getValueFloat();
                float goalYaw = playerYaw + (float) (random.nextDouble() - 0.5) * 2.0F * jitter;
                float goalPitch = clamp(forwardPitch.getValueFloat() + (float) (random.nextDouble() - 0.5) * 2.0F * jitter, -90.0F, 90.0F);

                targetYaw = goalYaw;
                targetPitch = clamp(server.pitch + ease(goalPitch - server.pitch, speed), -90.0F, 90.0F);
            } else {
                final float goalTurn = toward(onFace ? server.yaw : targetYaw, side);
                targetYaw = server.yaw + ease(goalTurn, speed);

                if (onFace && place != null) {
                    // Already on the face: drift down while it still is.
                    float lower = Math.min(90.0F, server.pitch + (float) (0.3 + random.nextDouble() * 0.9));
                    targetPitch = overBlock(trace(eye, new Look(targetYaw, lower)), place) ? lower : server.pitch;
                } else {
                    final float goalPitch = targetPitch;

                    targetPitch = clamp(server.pitch + ease(targetPitch - server.pitch, speed), -90.0F, 90.0F);

                    // The ease stops short of the goal, often just off the face.
                    // Where that costs a placement, take the whole step, still
                    // capped at the turn speed.
                    if (place != null && readyToPlace && aimedThisTick
                            && !overBlock(trace(eye, sensitivityPatch(new Look(targetYaw, targetPitch), last)), place)) {
                        float dPitch = goalPitch - server.pitch;
                        Look full = new Look(server.yaw + clamp(goalTurn, -speed, speed),
                                clamp(server.pitch + clamp(dPitch, -speed, speed), -90.0F, 90.0F));

                        if (overBlock(trace(eye, sensitivityPatch(full, last)), place)) {
                            targetYaw = full.yaw;
                            targetPitch = full.pitch;
                        }
                    }
                }
            }
        }

        Look sent = sensitivityPatch(new Look(targetYaw, targetPitch), last);

        // Grim's DuplicateRotPlace: on each place it takes |yaw| of the last
        // rotation it processed and flags when that is over 2 and equal (1e-4)
        // to the |yaw| it took at the place before. Yaw only; the pitch is just
        // printed (y=). A 1.8 place is checked when the next flying packet
        // arrives but BEFORE that packet's rotation (handleFlying: queued
        // places, then the RotationUpdate), so a place is judged by the last
        // turn sent on an EARLIER tick, never the one that goes out with it.
        //
        // The turn sent this tick is what the next place gets judged by. It
        // must differ from the |yaw| recorded at the last place, and from the
        // |yaw| a place this tick would record (the last turn sent before now).
        final float turnBefore = pendingTurnYaw;
        final boolean turnedBefore = turnedSincePlace;
        final boolean turnsNow;
        final float turnNow;
        {
            final float gcd = mouseStep();
            final float turnYaw = angleDifference(sent.yaw, lastYaw);
            final float turnPitch = sent.pitch - server.pitch;
            final boolean turning = Math.abs(turnYaw) > 1e-4F || Math.abs(turnPitch) > 1e-4F;

            // The same turn as last tick: break it on the pitch, towards the
            // target (an identical look twice is AimDuplicateLook's).
            if (gcd > 0.0F && turning && Math.abs(turnYaw - prevTurnYaw) < 1e-4F && Math.abs(turnPitch - prevTurnPitch) < 1e-4F) {
                float pitchStep = targetPitch >= sent.pitch ? gcd : -gcd;

                if (sent.pitch + pitchStep >= -90.0F && sent.pitch + pitchStep <= 90.0F) {
                    sent = new Look(sent.yaw, sent.pitch + pitchStep);
                }
            }

            // A clash: the nearest mouse step either way that clashes with
            // neither, the shorter turn first, one that still lands before one
            // that doesn't.
            final float turn = angleDifference(sent.yaw, lastYaw);

            if (gcd > 0.0F && turning && clashes(Math.abs(turn), turnBefore, turnedBefore)) {
                final float shorter = turn >= 0.0F ? -1.0F : 1.0F;
                Look landing = null, any = null;

                for (int steps = 1; steps <= 3 && landing == null; ++steps) {
                    for (float dir : new float[] { shorter, -shorter }) {
                        Look candidate = new Look(sent.yaw + dir * steps * gcd, sent.pitch);

                        if (clashes(Math.abs(angleDifference(candidate.yaw, lastYaw)), turnBefore, turnedBefore)) continue;

                        if (any == null) any = candidate;

                        if (place != null && overBlock(trace(eye, candidate), place)) {
                            landing = candidate;
                            break;
                        }
                    }
                }

                if (landing != null) sent = landing;
                else if (any != null) sent = any;
            }

            prevTurnYaw = angleDifference(sent.yaw, lastYaw);
            prevTurnPitch = sent.pitch - server.pitch;
            turnsNow = Math.abs(prevTurnYaw) > 1e-4F || Math.abs(prevTurnPitch) > 1e-4F;
            turnNow = Math.abs(prevTurnYaw);
        }

        // A place now would be judged by the turn already sent: if that one
        // repeats the last place's, wait a tick -- this tick's turn (guarded
        // above) becomes the one the next place is judged by.
        if (turnedBefore && turnBefore > 2.0F && Math.abs(turnBefore - placeTurnYaw) < 1e-4F) {
            readyToPlace = false;
        }

        // Silent: the server sees the turn, your camera does not.
        sendLook(sent);

        // Strict ray cast with the look that goes out this tick: right-click
        // only when it is on the face.
        boolean placed = false;

        if (place != null && readyToPlace && !aimOnly) {
            Hit ray = trace(eye, sent);
            // When falling in SNAP mode, skip the nextEye prediction guard — it
            // routinely misses and causes the player to fall off before placing.
            boolean snapFalling = modeIndex == SNAP && ticksOnAir > 0;
            boolean over = overBlock(ray, place)
                    && (snapFalling || nextEye == null || overBlock(trace(nextEye, sent), place));

            if (over && mc.playerController.onPlayerRightClick(player, mc.theWorld, player.getHeldItem(),
                    ray.raw.getBlockPos(), ray.raw.sideHit, ray.raw.hitVec)) {
                placed = true;

                if (swing.isEnabled()) player.swingItem();

                // Grim records a place's turn only when a rotation came since the
                // last one it judged: the turn sent before this tick.
                if (turnedBefore) placeTurnYaw = turnBefore;

                turnedSincePlace = false;

                if (extraDue) {
                    extraStarted = true;
                    extraSkipNextJump = true;
                    haveExtra = true;
                    extraX = place.tx;
                    extraY = place.ty;
                    extraZ = place.tz;
                }

                ticksOnAir = 0;
                snapPlaced = modeIndex == SNAP;
            }
        }

        // This tick's turn goes out after the place was sent: it is the
        // rotation the NEXT place is judged by.
        if (turnsNow) {
            pendingTurnYaw = turnNow;
            turnedSincePlace = true;
        }

        updateFake(player, place, placed, sent);

        // Same Y bookkeeping, every tick.
        if (jumpKey && posY % 1.0 > 0.5) startY = Math.floor(posY);

        if ((posY < startY || onGround) && !isMoving) startY = Math.floor(posY);

        // Standing on something: that is the layer -- unless it's the extra
        // block, which keeps the bridge's layer.
        boolean onExtra = haveExtra && floor(posX) == extraX && floor(posY) - 1 == extraY && floor(posZ) == extraZ;

        if (onGround && !onExtra && Math.floor(posY) != startY && !air(floor(posX), floor(posY) - 1, floor(posZ))) {
            startY = Math.floor(posY);
        }

        // Fallen below the layer: build where you are.
        if (posY < startY) startY = Math.floor(posY);
    }

    // Fake Rotations: what the model shows, nothing else. Server shows the look
    // actually sent; Real your own, as if nothing turned; Back aims back at the
    // bridge like a real backwards scaffold -- at a jittered point on the face
    // being placed on, or with nothing due back along the walk and down -- and
    // gets there like a hand (most of the way a tick, at most 45 yaw / 25
    // pitch, a little tremor, a dip on each place); Spin turns round on top of
    // your own pitch.
    private void updateFake(EntityPlayerSP player, Placement place, boolean placed, Look sent) {
        final int fake = fakeRotations.getIndex();

        if (fake != FAKE_BACK) fakeBackLive = false;

        switch (fake) {
            case FAKE_SERVER:
                FakeRotation.set(sent.yaw, sent.pitch);
                break;

            case FAKE_SPIN:
                fakeSpinAngle = MathHelper.wrapAngleTo180_float(fakeSpinAngle + fakeSpinSpeed.getValueFloat());
                FakeRotation.set(playerYaw + fakeSpinAngle, player.rotationPitch);
                break;

            case FAKE_BACK: {
                float goalYaw = moveDirection(playerYaw, moveForward, moveStrafe) + 180.0F;
                float goalPitch = 79.0F + (float) random.nextDouble() * 3.0F;

                if (!fakeBackLive) {
                    fakeYaw = goalYaw;
                    fakePitch = 80.0F;
                    fakeBackLive = true;
                }

                if (place != null) {
                    Vec3 c = faceCenter(place.fx, place.fy, place.fz, place.facing);
                    double spread = 0.3;
                    Look aim = rotationTo(eye,
                            c.xCoord + (random.nextDouble() - 0.5) * spread * (OFFSET_X[place.facing] == 0 ? 1 : 0),
                            c.yCoord + (random.nextDouble() - 0.5) * spread * (OFFSET_Y[place.facing] == 0 ? 1 : 0),
                            c.zCoord + (random.nextDouble() - 0.5) * spread * (OFFSET_Z[place.facing] == 0 ? 1 : 0));
                    goalYaw = aim.yaw;
                    goalPitch = aim.pitch;
                }

                float dYaw = angleDifference(goalYaw, fakeYaw);
                float dPitch = goalPitch - fakePitch;
                float share = (float) (0.45 + random.nextDouble() * 0.3);

                fakeYaw += clamp(dYaw * share, -45.0F, 45.0F) + (float) (random.nextDouble() - 0.5) * 0.6F;
                fakePitch += clamp(dPitch * share, -25.0F, 25.0F) + (float) (random.nextDouble() - 0.5) * 0.4F;

                if (placed) fakePitch += 1.0F + (float) random.nextDouble() * 1.5F;

                fakeYaw = playerYaw + angleDifference(fakeYaw, playerYaw);
                fakePitch = clamp(fakePitch, -90.0F, 90.0F);

                FakeRotation.set(fakeYaw, fakePitch);
                break;
            }

            default:
                FakeRotation.clear();
                break;
        }
    }

    // A turn of this |yaw| would repeat the one recorded at the last place, or
    // the one a place this tick would record.
    private boolean clashes(float absYaw, float turnBefore, boolean turnedBefore) {
        return absYaw > 2.0F && (Math.abs(absYaw - placeTurnYaw) < 1e-3F || (turnedBefore && Math.abs(absYaw - turnBefore) < 1e-3F));
    }

    // Degrees from the server's yaw to the goal, the long way round when the
    // short way would turn against dir; small corrections go either way.
    private float toward(float goal, float dir) {
        float delta = angleDifference(goal, server.yaw);

        if (delta * dir < 0.0F && Math.abs(delta) > 60.0F) delta += 360.0F * dir;

        return delta;
    }

    // Most of the way there each tick, never more than speed, never less than
    // a couple of degrees unless that's all that's left.
    private float ease(float delta, float speed) {
        float step = Math.min(speed, Math.max(Math.abs(delta) * (float) (0.78 + random.nextDouble() * 0.12),
                Math.min(Math.abs(delta), 2.0F)));
        return Math.copySign(step, delta);
    }

    @Override
    public void onRender2D() {
        if (!blockCount.isEnabled() || mc.thePlayer == null) return;

        int current = blocksLeft;
        int total = Math.max(initialBlocks, Math.max(totalBlocks, current));
        if (total <= 0) total = Math.max(1, current);

        float targetFraction = Math.max(0.0F, Math.min(1.0F, (float) current / (float) total));
        // Smoothly animate the progress bar
        animatedProgress += (targetFraction - animatedProgress) * 0.18F;
        animatedProgress = Math.max(0.0F, Math.min(1.0F, animatedProgress));

        ScaledResolution resolution = new ScaledResolution(mc);
        float centerX = resolution.getScaledWidth() / 2.0F;
        float cardY = resolution.getScaledHeight() / 2.0F + 14.0F;

        // Compact glassmorphic progress pill (reduced size, no numbers)
        float cardW = 52.0F;
        float cardH = 8.0F;
        float cardX = centerX - (cardW / 2.0F);
        float radius = 4.0F;

        // 1. Frosted background blur pass
        IslandBlur.renderBlurRounded(cardX, cardY, cardW, cardH, 12.0F, radius);

        net.minecraft.client.renderer.GlStateManager.pushMatrix();
        net.minecraft.client.renderer.GlStateManager.enableBlend();
        net.minecraft.client.renderer.GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        // 2. Translucent Frosted Glass Backplate
        RenderUtil.drawRoundedGradient(cardX, cardY, cardW, cardH, radius, 0x481E1E26, 0x5812121A);

        // 3. Specular rim highlight / glass border sheen
        RenderUtil.drawRoundedOutline(cardX, cardY, cardW, cardH, radius, 1.0F, 0x30FFFFFF);

        // 4. Color coding based on remaining proportion / count
        int barStartColor;
        int barEndColor;
        if (current <= 8 || targetFraction <= 0.15F) {
            barStartColor = 0xFFFF3B3B;
            barEndColor = 0xFFFF6E6E;
        } else if (current <= 24 || targetFraction <= 0.35F) {
            barStartColor = 0xFFFF9526;
            barEndColor = 0xFFFFC74D;
        } else {
            barStartColor = 0xFF3CE67A;
            barEndColor = 0xFF6CFFB0;
        }

        // 5. Internal slim progress track & animated fill
        float innerPadding = 2.0F;
        float barX = cardX + innerPadding;
        float barY = cardY + innerPadding;
        float barW = cardW - (innerPadding * 2.0F);
        float barH = cardH - (innerPadding * 2.0F);
        float barRadius = 2.0F;

        // Translucent track background
        RenderUtil.drawRoundedRect(barX, barY, barW, barH, barRadius, 0x22FFFFFF);

        // Filled progress portion
        float fillW = barW * animatedProgress;
        if (fillW >= 1.5F) {
            RenderUtil.drawRoundedGradient(barX, barY, fillW, barH, barRadius, barStartColor, barEndColor);
        }

        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        net.minecraft.client.renderer.GlStateManager.disableBlend();
        net.minecraft.client.renderer.GlStateManager.popMatrix();
    }
}
