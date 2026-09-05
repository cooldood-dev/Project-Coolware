package com.github.cooldood.utils.minecraft;

import com.github.cooldood.utils.client.C;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;

import java.util.Arrays;

public class InventoryUtil {
    public static boolean isSlotEmpty(int slot) {
        return C.p().inventory.getStackInSlot(slot) == null || C.p().inventory.getStackInSlot(slot).stackSize <= 0;
    }

    public static int biggestBlockSlot() {
        int biggestSlot = -1;

        for (int i = 0; i < InventoryPlayer.getHotbarSize(); i++) {
            if (!isValidBlock(getItemInSlot(i))) continue;

            if (biggestSlot != -1 && C.p().inventory.getStackInSlot(i).stackSize < C.p().inventory.getStackInSlot(biggestSlot).stackSize) continue;

            biggestSlot = i;
        }

        return biggestSlot;
    }

    public static Item getHeldItem() {
        return C.p().inventory.getCurrentItem() == null ? null : C.p().inventory.getCurrentItem().getItem();
    }

    public static Item getItemInSlot(int i) {
        return C.p().inventory.getStackInSlot(i) == null ? null : C.p().inventory.getStackInSlot(i).getItem();
    }

    public static boolean isValidBlock() {
        return isValidBlock(getHeldItem());
    }

    public static boolean isValidBlock(Item stack) {
        Block block = Block.getBlockFromItem(stack);
        if (block == null) return false;

        return isValidBlock(block);
    }

    public static boolean isValidBlock(Block block) {
        return !isBlockFalling(block) && !isBlockInteractable(block) && isFullBlock(block);
    }

    public static boolean isBlockInteractable(Block block) {
        if (block == Blocks.tnt || block instanceof BlockStairs || block == Blocks.end_stone || block == Blocks.redstone_ore || block == Blocks.cobblestone) return false;

        Class<?> clazz = block.getClass();

        // shoutout to 112batman! just using the getMethod function and returning true if it errors is SLOW
        while(Arrays.stream(clazz.getDeclaredMethods()).noneMatch(m -> m.getName().equals("onBlockActivated"))) {
            Class<?> superClazz = clazz.getSuperclass();
            if(superClazz == null || superClazz == Block.class) return false;
            clazz = superClazz;
        }
        return true;
    }

    public static boolean isBlockFalling(Block block) {
        return block == Blocks.sand || block == Blocks.gravel || block == Blocks.dragon_egg || block == Blocks.anvil;
    }

    public static boolean isSolidBlock(Block block) {
        return block.isCollidable() && block.getMaterial().isSolid();
    }

    public static boolean isFullBlock(Block block) {
        if (block instanceof BlockStairs) return false;

        AxisAlignedBB collisionBox = block.getCollisionBoundingBox(C.w(), BlockPos.ORIGIN, block.getDefaultState());
        if (collisionBox == null) return false;

        return collisionBox.minX == 0 && collisionBox.minY == 0 && collisionBox.minZ == 0
                && collisionBox.maxX == 1 && collisionBox.maxY == 1 && collisionBox.maxZ == 1;
    }

    public static int getBestSlotForBlock(BlockPos blockPos) {
        int bestSlot = 0;
        float fastest = 0;

        for (int i = 0; i < 9; i++) {
            float breakSpeed = blockStrength(C.p().inventory.getStackInSlot(i), blockPos);

            if (breakSpeed > fastest || (breakSpeed == fastest && C.p().inventory.currentItem == i)) {
                fastest = breakSpeed;
                bestSlot = i;
            }
        }

        return bestSlot;
    }

    // net.minecraftforge.common.ForgeHooks.blockStrength
    public static float blockStrength(ItemStack stack, BlockPos pos)
    {
        IBlockState state = C.w().getBlockState(pos);

        float hardness = state.getBlock().getBlockHardness(C.w(), pos);
        if (hardness < 0.0F)
        {
            return 0.0F;
        }

        if (!canHarvestBlock(state.getBlock(), stack))
        {
            return getBreakSpeed(state, stack) / hardness / 100F;
        }
        else
        {
            return getBreakSpeed(state, stack) / hardness / 30F;
        }
    }

    public static boolean canHarvestBlock(Block blockIn, ItemStack stack)
    {
        if (blockIn.getMaterial().isToolNotRequired())
        {
            return true;
        }
        else
        {
            return stack != null && stack.canHarvestBlock(blockIn);
        }
    }

    // net.minecraft.entity.player.EntityPlayer.getBreakSpeed
    public static float getBreakSpeed(IBlockState state, ItemStack stack)
    {
        float f = (stack == null ? 1.0F : stack.getItem().getDigSpeed(stack, state));
        if (f > 1.0F)
        {
            int i = EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, stack);

            if (i > 0)
            {
                f += (float)(i * i + 1);
            }
        }

        if (C.p().isPotionActive(Potion.digSpeed))
        {
            f *= 1.0F + (float)(C.p().getActivePotionEffect(Potion.digSpeed).getAmplifier() + 1) * 0.2F;
        }

        if (C.p().isPotionActive(Potion.digSlowdown))
        {
            float f1 = 1.0F;

            switch (C.p().getActivePotionEffect(Potion.digSlowdown).getAmplifier())
            {
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

        if (C.p().isInsideOfMaterial(Material.water) && !EnchantmentHelper.getAquaAffinityModifier(C.p()))
        {
            f /= 5.0F;
        }

        if (!C.p().onGround)
        {
            f /= 5.0F;
        }

        return (f < 0 ? 0 : f);
    }
}
