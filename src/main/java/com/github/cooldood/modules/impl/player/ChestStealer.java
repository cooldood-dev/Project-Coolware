package com.github.cooldood.modules.impl.player;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.PacketEvent;
import com.github.cooldood.events.impl.PlayerUpdateEvent;
import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.ModuleManager;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.modules.RegisterSubModule;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.minecraft.ChatUtil;
import com.github.cooldood.utils.minecraft.ItemUtil;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.resources.I18n;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.*;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.WorldSettings.GameType;
import org.apache.commons.lang3.RandomUtils;

@RegisterModule(
        name = "Chest Stealer",
        description = "Provides Chest Stealer functionality for the client.",
        category = Category.PLAYER
)
public class ChestStealer extends Module {
    @RegisterSubModule(name = "Min Delay", min = 0, max = 20, increment = 1)
    public static int minDelay = 1;

    @RegisterSubModule(name = "Max Delay", min = 0, max = 20, increment = 1)
    public static int maxDelay = 2;

    @RegisterSubModule(name = "Open Delay", min = 0, max = 20, increment = 1)
    public static int openDelay = 1;

    @RegisterSubModule(name = "Auto Close")
    public static boolean autoClose = false;

    @RegisterSubModule(name = "Name Check")
    public static boolean nameCheck = true;

    @RegisterSubModule(name = "Hypixel Mode")
    public static boolean hypixelMode = false;

    @RegisterSubModule(name = "Skip Trash")
    public static boolean skipTrash = true;

    @RegisterSubModule(name = "More Armor")
    public static boolean moreArmor = false;

    @RegisterSubModule(name = "More Sword")
    public static boolean moreSword = false;

    private static int clickDelay = 0;
    private static int oDelay = 0;
    private static boolean inChest = false;
    private static boolean warnedFull = false;

    private static boolean isValidGameMode() {
        GameType gameType = C.mc.playerController.getCurrentGameType();
        return gameType == GameType.SURVIVAL || gameType == GameType.ADVENTURE;
    }

    private static boolean isMoreArmor(ItemStack itemStack) {
        if (itemStack == null) return false;
        if (!moreArmor) return false;
        if (! (itemStack.getItem() instanceof ItemArmor)) return false;
        ItemArmor.ArmorMaterial armorMaterial = ((ItemArmor) itemStack.getItem()).getArmorMaterial();
        if (armorMaterial == ItemArmor.ArmorMaterial.DIAMOND) return true;
        return armorMaterial == ItemArmor.ArmorMaterial.IRON && itemStack.isItemEnchanted();
    }

    private static boolean isMoreSword(ItemStack itemStack) {
        if (itemStack == null) return false;
        if (!moreSword) return false;
        if (! (itemStack.getItem() instanceof ItemSword)) return false;
        Item.ToolMaterial swordMaterial = ItemUtil.getSwordMaterial(itemStack);
        if (swordMaterial == Item.ToolMaterial.EMERALD) return true;
        if (EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, itemStack) != 0) return true;
        return swordMaterial == Item.ToolMaterial.IRON && itemStack.isItemEnchanted();
    }

    private static boolean isInvManagerRequire(ItemStack itemStack) {
        if (itemStack == null) return false;
        if (ItemUtil.ItemType.Block.contains(itemStack)) {
            return !ModuleManager.isEnabled(InvManager.class) || ItemUtil.findInventorySlot(ItemUtil.ItemType.Block) < InvManager.blocks;
        }
        if (ItemUtil.ItemType.Projectile.contains(itemStack)) {
            return !ModuleManager.isEnabled(InvManager.class) || ItemUtil.findInventorySlot(ItemUtil.ItemType.Projectile) < InvManager.projectiles;
        }
        if (ItemUtil.ItemType.FishRod.contains(itemStack)) {
            return ItemUtil.findInventorySlot(ItemUtil.ItemType.Projectile) == 0;
        }
        if (ItemUtil.ItemType.Arrow.contains(itemStack)) {
            return !ModuleManager.isEnabled(InvManager.class) || ItemUtil.findInventorySlot(ItemUtil.ItemType.Arrow) < InvManager.arrow;
        }
        return false;
    }

    private static void shiftClick(int windowId, int slotId) {
        C.mc.playerController.windowClick(windowId, slotId, 0, 1, C.p());
    }

    @SubscribeEvent
    public static void onUpdate(PlayerUpdateEvent event) {
        if (clickDelay > 0) {
            clickDelay--;
        }
        if (oDelay > 0) {
            oDelay--;
        }
        if (!(C.mc.currentScreen instanceof GuiChest)) {
            inChest = false;
        } else {
            Container container = ((GuiChest) C.mc.currentScreen).inventorySlots;
            if (!(container instanceof ContainerChest)) {
                inChest = false;
            } else {
                if (!inChest) {
                    inChest = true;
                    warnedFull = false;
                    oDelay = openDelay + 1;
                }
                if (oDelay <= 0 && clickDelay <= 0) {
                    if (isValidGameMode()) {
                        IInventory inventory = ((ContainerChest) container).getLowerChestInventory();
                        if (hypixelMode) {
                            String inventoryName = inventory.getName();
                            String stripped = inventoryName == null
                                    ? ""
                                    : EnumChatFormatting.getTextWithoutFormattingCodes(inventoryName).trim();
                            if (!stripped.isEmpty()) {
                                return;
                            }
                        } else if (nameCheck) {
                            String inventoryName = inventory.getName();
                            if (!inventoryName.equals(I18n.format("container.chest")) && !inventoryName.equals(I18n.format("container.chestDouble"))) {
                                return;
                            }
                        }
                        if (C.p().inventory.getFirstEmptyStack() == -1) {
                            if (!warnedFull) {
                                ChatUtil.prefixMessage("&cYour inventory is full!");
                                warnedFull = true;
                            }
                            if (autoClose) {
                                C.p().closeScreen();
                            }
                        } else {
                            if (skipTrash) {
                                int bestSword = -1;
                                double bestDamage = 0.0;
                                int[] bestArmorSlots = new int[]{-1, -1, -1, -1};
                                double[] bestArmorProtection = new double[]{0.0, 0.0, 0.0, 0.0};
                                int bestPickaxeSlot = -1;
                                float bestPickaxeEfficiency = 1.0F;
                                int bestShovelSlot = -1;
                                float bestShovelEfficiency = 1.0F;
                                int bestAxeSlot = -1;
                                float bestAxeEfficiency = 1.0F;
                                int bestBow = -1;
                                double bestBowDamage = 0.0;
                                for (int i = 0; i < inventory.getSizeInventory(); i++) {
                                    if (container.getSlot(i).getHasStack()) {
                                        ItemStack stack = container.getSlot(i).getStack();
                                        Item item = stack.getItem();
                                        if (item instanceof ItemSword) {
                                            double damage = ItemUtil.getAttackBonus(stack);
                                            if (bestSword == -1 || damage > bestDamage) {
                                                bestSword = i;
                                                bestDamage = damage;
                                            }
                                        } else if (item instanceof ItemArmor) {
                                            int armorType = ((ItemArmor) item).armorType;
                                            double protectionLevel = ItemUtil.getArmorProtection(stack);
                                            if (bestArmorSlots[armorType] == -1 || protectionLevel > bestArmorProtection[armorType]) {
                                                bestArmorSlots[armorType] = i;
                                                bestArmorProtection[armorType] = protectionLevel;
                                            }
                                        } else if (item instanceof ItemPickaxe) {
                                            float efficiency = ItemUtil.getToolEfficiency(stack);
                                            if (bestPickaxeSlot == -1 || efficiency > bestPickaxeEfficiency) {
                                                bestPickaxeSlot = i;
                                                bestPickaxeEfficiency = efficiency;
                                            }
                                        } else if (item instanceof ItemSpade) {
                                            float efficiency = ItemUtil.getToolEfficiency(stack);
                                            if (bestShovelSlot == -1 || efficiency > bestShovelEfficiency) {
                                                bestShovelSlot = i;
                                                bestShovelEfficiency = efficiency;
                                            }
                                        } else if (item instanceof ItemAxe) {
                                            float efficiency = ItemUtil.getToolEfficiency(stack);
                                            if (bestAxeSlot == -1 || efficiency > bestAxeEfficiency) {
                                                bestAxeSlot = i;
                                                bestAxeEfficiency = efficiency;
                                            }
                                        } else if (item instanceof ItemBow) {
                                            double damage = ItemUtil.getBowAttackBonus(stack);
                                            if (bestBow == -1 || damage > bestBowDamage) {
                                                bestBow = i;
                                                bestBowDamage = damage;
                                            }
                                        }
                                    }
                                }
                                int swordInInventorySlot = ItemUtil.findSwordInInventorySlot(0, true);
                                double damage = swordInInventorySlot != -1 ? ItemUtil.getAttackBonus(C.p().inventory.getStackInSlot(swordInInventorySlot)) : 0.0;
                                if (bestDamage > damage) {
                                    shiftClick(container.windowId, bestSword);
                                    return;
                                }
                                for (int i = 0; i < 4; i++) {
                                    int slot = ItemUtil.findArmorInventorySlot(i, true);
                                    double protectionLevel = slot != -1
                                            ? ItemUtil.getArmorProtection(C.p().inventory.getStackInSlot(slot))
                                            : 0.0;
                                    if (bestArmorProtection[i] > protectionLevel) {
                                        shiftClick(container.windowId, bestArmorSlots[i]);
                                        return;
                                    }
                                }
                                int pickaxeSlot = ItemUtil.findInventorySlot("pickaxe", 0, true);
                                float pickaxeEfficiency = pickaxeSlot != -1 ? ItemUtil.getToolEfficiency(C.p().inventory.getStackInSlot(pickaxeSlot)) : 1.0F;
                                if (bestPickaxeEfficiency > pickaxeEfficiency) {
                                    shiftClick(container.windowId, bestPickaxeSlot);
                                    return;
                                }
                                int shovelSlot = ItemUtil.findInventorySlot("shovel", 0, true);
                                float shovelEfficiency = shovelSlot != -1 ? ItemUtil.getToolEfficiency(C.p().inventory.getStackInSlot(shovelSlot)) : 1.0F;
                                if (bestShovelEfficiency > shovelEfficiency) {
                                    shiftClick(container.windowId, bestShovelSlot);
                                    return;
                                }
                                int axeSlot = ItemUtil.findInventorySlot("axe", 0, true);
                                float efficiency = axeSlot != -1 ? ItemUtil.getToolEfficiency(C.p().inventory.getStackInSlot(axeSlot)) : 1.0F;
                                if (bestAxeEfficiency > efficiency) {
                                    shiftClick(container.windowId, bestAxeSlot);
                                    return;
                                }
                                int bowSlot = ItemUtil.findBowInventorySlot(0, true);
                                double bowDamage = bowSlot != -1 ? ItemUtil.getBowAttackBonus(C.p().inventory.getStackInSlot(bowSlot)) : 0.0;
                                if (bestBowDamage > bowDamage) {
                                    shiftClick(container.windowId, bestBow);
                                    return;
                                }
                            }
                            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                                if (container.getSlot(i).getHasStack()) {
                                    ItemStack stack = container.getSlot(i).getStack();
                                    if (!skipTrash || !ItemUtil.isNotSpecialItem(stack) || isMoreArmor(stack) || isMoreSword(stack) || isInvManagerRequire(stack)) {
                                        shiftClick(container.windowId, i);
                                        return;
                                    }
                                }
                            }
                            if (autoClose) {
                                C.p().closeScreen();
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onWindowClick(PacketEvent.Send event) {
        if (event.packet instanceof C0EPacketClickWindow) {
            clickDelay = RandomUtils.nextInt(minDelay + 1, maxDelay + 2);
        }
    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {

    }
}
