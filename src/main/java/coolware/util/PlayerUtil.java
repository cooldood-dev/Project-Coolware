package coolware.util;

import coolware.module.combat.KillAura;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.Vec3;

public class PlayerUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean canAttack() {
        return !isUsingItem();
    }

    public static boolean attack(Entity target) {
        if (mc.thePlayer == null || mc.playerController == null || target == null) return false;
        swingHand();
        mc.playerController.attackEntity(mc.thePlayer, target);
        return true;
    }

    private static int lastSwing = -1;

    public static void swingHand() {
        if (mc.thePlayer == null) return;
        if (lastSwing == MovementUtil.ticks) return;
        mc.thePlayer.swingItem();
        lastSwing = MovementUtil.ticks;
    }

    private static int lastUnblock = -1;
    private static boolean wasBlocking = false;

    public static int getLastUnblock() {
        if (wasBlocking && !isUsingItem()) lastUnblock = MovementUtil.ticks;
        return lastUnblock;
    }

    public static boolean isUsingItem() {
        if (mc.thePlayer == null) return false;
        return mc.thePlayer.isUsingItem();
    }

    public static void updateLastBlocking() {
        wasBlocking = isUsingItem();
    }

    public static RotationUtil.Rotation lastRotation() {
        return RotationUtil.getPreviousServerRotation();
    }

    public static RotationUtil.Rotation currentRotation() {
        return RotationUtil.getServerRotation();
    }

    public static boolean movingTowardsEntity(EntityLivingBase entity) {
        if (mc.thePlayer == null || entity == null) return false;
        double prevDistance = new Vec3(mc.thePlayer.prevPosX, mc.thePlayer.prevPosY, mc.thePlayer.prevPosZ).distanceTo(entity.getPositionVector());
        double currentDistance = mc.thePlayer.getPositionVector().distanceTo(entity.getPositionVector());
        return currentDistance < prevDistance;
    }

    public static int ticksUntilInRange(EntityLivingBase entity, double range) {
        if (mc.thePlayer == null || entity == null) return -1;
        Vec3 playerPos = mc.thePlayer.getPositionVector();
        Vec3 targetPos = entity.getPositionVector();

        double startDistance = playerPos.distanceTo(targetPos);
        playerPos = playerPos.add(new Vec3(playerPos.xCoord - mc.thePlayer.prevPosX, 0, playerPos.zCoord - mc.thePlayer.prevPosZ));
        targetPos = targetPos.add(new Vec3(targetPos.xCoord - entity.lastTickPosX, 0, targetPos.zCoord - entity.lastTickPosZ));
        double nextDistance = playerPos.distanceTo(targetPos);
        double distanceChange = startDistance - nextDistance;

        if (distanceChange <= 0) return -1;
        return (int) Math.ceil((startDistance - range) / distanceChange);
    }

    private static long lastAttackedTime = 0L;

    public static void onPlayerAttacked() {
        lastAttackedTime = System.currentTimeMillis();
        switchToSword();
    }

    public static boolean isBeingAttacked() {
        if (mc.thePlayer != null && (mc.thePlayer.hurtTime > 0 || mc.thePlayer.hurtResistantTime > 0)) {
            return true;
        }
        return (System.currentTimeMillis() - lastAttackedTime) < 1500L;
    }

    public static void switchToSword() {
        if (mc.thePlayer == null) return;
        if (ItemUtil.isHoldingSword()) return;

        int swordHotbarSlot = -1;
        double bestDamage = -1.0;

        for (int i = 0; i < 9; ++i) {
            net.minecraft.item.ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof net.minecraft.item.ItemSword) {
                double damage = ItemUtil.getAttackBonus(stack);
                if (damage > bestDamage) {
                    bestDamage = damage;
                    swordHotbarSlot = i;
                }
            }
        }

        if (swordHotbarSlot != -1) {
            mc.thePlayer.inventory.currentItem = swordHotbarSlot;
            return;
        }

        // If not in hotbar, search whole inventory (slots 9..35)
        int bestInvSlot = ItemUtil.findSwordInInventorySlot(9, false);
        if (bestInvSlot != -1) {
            int targetHotbar = mc.thePlayer.inventory.currentItem;
            if (targetHotbar < 0 || targetHotbar > 8) targetHotbar = 0;
            // Swap slot with hotbar slot
            mc.playerController.windowClick(0, bestInvSlot, targetHotbar, 2, mc.thePlayer);
            mc.thePlayer.inventory.currentItem = targetHotbar;
        }
    }
}
