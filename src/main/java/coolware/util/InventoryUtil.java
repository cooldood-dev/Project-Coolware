package coolware.util;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;

import java.util.Arrays;

public class InventoryUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean isSlotEmpty(int slot) {
        if (mc.thePlayer == null) return true;
        ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
        return stack == null || stack.stackSize <= 0;
    }

    public static int biggestBlockSlot() {
        if (mc.thePlayer == null) return -1;
        int biggestSlot = -1;

        for (int i = 0; i < InventoryPlayer.getHotbarSize(); i++) {
            Item item = getItemInSlot(i);
            if (!isValidBlock(item)) continue;

            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack == null) continue;

            if (biggestSlot != -1) {
                ItemStack bestStack = mc.thePlayer.inventory.getStackInSlot(biggestSlot);
                if (bestStack != null && stack.stackSize < bestStack.stackSize) continue;
            }

            biggestSlot = i;
        }

        return biggestSlot;
    }

    public static Item getHeldItem() {
        if (mc.thePlayer == null || mc.thePlayer.inventory.getCurrentItem() == null) return null;
        return mc.thePlayer.inventory.getCurrentItem().getItem();
    }

    public static Item getItemInSlot(int i) {
        if (mc.thePlayer == null || mc.thePlayer.inventory.getStackInSlot(i) == null) return null;
        return mc.thePlayer.inventory.getStackInSlot(i).getItem();
    }

    public static boolean isValidBlock() {
        return isValidBlock(getHeldItem());
    }

    public static boolean isValidBlock(Item stack) {
        if (stack == null) return false;
        Block block = Block.getBlockFromItem(stack);
        if (block == null) return false;

        return isValidBlock(block);
    }

    public static boolean isValidBlock(Block block) {
        if (block == null) return false;
        return !isBlockFalling(block) && !isBlockInteractable(block) && isFullBlock(block);
    }

    public static boolean isBlockInteractable(Block block) {
        if (block == null) return false;
        if (block == Blocks.tnt || block instanceof BlockStairs || block == Blocks.end_stone || block == Blocks.redstone_ore || block == Blocks.cobblestone) {
            return false;
        }

        Class<?> clazz = block.getClass();
        while (Arrays.stream(clazz.getDeclaredMethods()).noneMatch(m -> m.getName().equals("onBlockActivated"))) {
            Class<?> superClazz = clazz.getSuperclass();
            if (superClazz == null || superClazz == Block.class) return false;
            clazz = superClazz;
        }
        return true;
    }

    public static boolean isBlockFalling(Block block) {
        return block == Blocks.sand || block == Blocks.gravel || block == Blocks.dragon_egg || block == Blocks.anvil;
    }

    public static boolean isSolidBlock(Block block) {
        return block != null && block.isCollidable() && block.getMaterial().isSolid();
    }

    public static boolean isFullBlock(Block block) {
        if (block == null || block instanceof BlockStairs) return false;
        if (mc.theWorld == null) return true;

        AxisAlignedBB collisionBox = block.getCollisionBoundingBox(mc.theWorld, BlockPos.ORIGIN, block.getDefaultState());
        if (collisionBox == null) return false;

        return collisionBox.minX == 0 && collisionBox.minY == 0 && collisionBox.minZ == 0
                && collisionBox.maxX == 1 && collisionBox.maxY == 1 && collisionBox.maxZ == 1;
    }

    public static int getBestSlotForBlock(BlockPos blockPos) {
        if (mc.thePlayer == null) return 0;
        int bestSlot = 0;
        float fastest = 0;

        for (int i = 0; i < 9; i++) {
            float breakSpeed = blockStrength(mc.thePlayer.inventory.getStackInSlot(i), blockPos);

            if (breakSpeed > fastest || (breakSpeed == fastest && mc.thePlayer.inventory.currentItem == i)) {
                fastest = breakSpeed;
                bestSlot = i;
            }
        }

        return bestSlot;
    }

    public static float blockStrength(ItemStack stack, BlockPos pos) {
        if (mc.theWorld == null) return 0.0F;
        IBlockState state = mc.theWorld.getBlockState(pos);

        float hardness = state.getBlock().getBlockHardness(mc.theWorld, pos);
        if (hardness < 0.0F) {
            return 0.0F;
        }

        if (!canHarvestBlock(state.getBlock(), stack)) {
            return getBreakSpeed(state, stack) / hardness / 100F;
        } else {
            return getBreakSpeed(state, stack) / hardness / 30F;
        }
    }

    public static boolean canHarvestBlock(Block blockIn, ItemStack stack) {
        if (blockIn.getMaterial().isToolNotRequired()) {
            return true;
        } else {
            return stack != null && stack.canHarvestBlock(blockIn);
        }
    }

    public static float getBreakSpeed(IBlockState state, ItemStack stack) {
        if (mc.thePlayer == null) return 0.0F;
        float f = (stack == null ? 1.0F : stack.getStrVsBlock(state.getBlock()));
        if (f > 1.0F) {
            int i = EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, stack);

            if (i > 0) {
                f += (float) (i * i + 1);
            }
        }

        if (mc.thePlayer.isPotionActive(Potion.digSpeed)) {
            f *= 1.0F + (float) (mc.thePlayer.getActivePotionEffect(Potion.digSpeed).getAmplifier() + 1) * 0.2F;
        }

        if (mc.thePlayer.isPotionActive(Potion.digSlowdown)) {
            float f1 = 1.0F;

            switch (mc.thePlayer.getActivePotionEffect(Potion.digSlowdown).getAmplifier()) {
                case 0:
                    f1 = 0.3F;
                    break;
                case 1:
                    f1 = 0.09F;
                    break;
                case 2:
                    f1 = 0.0027F;
                    break;
                case 3:
                default:
                    f1 = 8.1E-4F;
            }

            f *= f1;
        }

        if (mc.thePlayer.isInsideOfMaterial(Material.water) && !EnchantmentHelper.getAquaAffinityModifier(mc.thePlayer)) {
            f /= 5.0F;
        }

        if (!mc.thePlayer.onGround) {
            f /= 5.0F;
        }

        return (f < 0 ? 0 : f);
    }
}
