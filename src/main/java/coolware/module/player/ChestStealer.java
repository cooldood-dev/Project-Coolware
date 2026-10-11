package coolware.module.player;

import coolware.Coolware;
import coolware.module.Category;
import coolware.module.Module;
import coolware.setting.BooleanSetting;
import coolware.setting.NumberSetting;
import coolware.util.ItemUtil;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.resources.I18n;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.*;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.WorldSettings.GameType;
import org.apache.commons.lang3.RandomUtils;
import org.lwjgl.input.Keyboard;

public class ChestStealer extends Module {
    public static NumberSetting minDelaySetting;
    public static NumberSetting maxDelaySetting;
    public static NumberSetting openDelaySetting;
    public static BooleanSetting autoCloseSetting;
    public static BooleanSetting nameCheckSetting;
    public static BooleanSetting hypixelModeSetting;
    public static BooleanSetting skipTrashSetting;
    public static BooleanSetting moreArmorSetting;
    public static BooleanSetting moreSwordSetting;

    private int clickDelay = 0;
    private int oDelay = 0;
    private boolean inChest = false;
    private boolean warnedFull = false;

    public ChestStealer() {
        super("ChestStealer", "Automatically loots chests when opened", Category.PLAYER, Keyboard.KEY_NONE);

        minDelaySetting = new NumberSetting("Min Delay", this, 1.0, 0.0, 20.0, 1.0);
        maxDelaySetting = new NumberSetting("Max Delay", this, 2.0, 0.0, 20.0, 1.0);
        openDelaySetting = new NumberSetting("Open Delay", this, 1.0, 0.0, 20.0, 1.0);
        autoCloseSetting = new BooleanSetting("Auto Close", this, false);
        nameCheckSetting = new BooleanSetting("Name Check", this, true);
        hypixelModeSetting = new BooleanSetting("Hypixel Mode", this, false);
        skipTrashSetting = new BooleanSetting("Skip Trash", this, true);
        moreArmorSetting = new BooleanSetting("More Armor", this, false);
        moreSwordSetting = new BooleanSetting("More Sword", this, false);

        addSetting(minDelaySetting);
        addSetting(maxDelaySetting);
        addSetting(openDelaySetting);
        addSetting(autoCloseSetting);
        addSetting(nameCheckSetting);
        addSetting(hypixelModeSetting);
        addSetting(skipTrashSetting);
        addSetting(moreArmorSetting);
        addSetting(moreSwordSetting);
    }

    @Override
    public void onEnable() {
        clickDelay = 0;
        oDelay = 0;
        inChest = false;
        warnedFull = false;
    }

    @Override
    public void onDisable() {
        inChest = false;
        warnedFull = false;
    }

    private boolean isValidGameMode() {
        if (mc.playerController == null) return false;
        GameType gameType = mc.playerController.getCurrentGameType();
        return gameType == GameType.SURVIVAL || gameType == GameType.ADVENTURE;
    }

    private boolean isMoreArmor(ItemStack itemStack) {
        if (itemStack == null) return false;
        if (!moreArmorSetting.isEnabled()) return false;
        if (!(itemStack.getItem() instanceof ItemArmor)) return false;
        ItemArmor.ArmorMaterial armorMaterial = ((ItemArmor) itemStack.getItem()).getArmorMaterial();
        if (armorMaterial == ItemArmor.ArmorMaterial.DIAMOND) return true;
        return armorMaterial == ItemArmor.ArmorMaterial.IRON && itemStack.isItemEnchanted();
    }

    private boolean isMoreSword(ItemStack itemStack) {
        if (itemStack == null) return false;
        if (!moreSwordSetting.isEnabled()) return false;
        if (!(itemStack.getItem() instanceof ItemSword)) return false;
        Item.ToolMaterial swordMaterial = ItemUtil.getSwordMaterial(itemStack);
        if (swordMaterial == Item.ToolMaterial.EMERALD) return true;
        if (EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, itemStack) != 0) return true;
        return swordMaterial == Item.ToolMaterial.IRON && itemStack.isItemEnchanted();
    }

    private boolean isInvManagerRequire(ItemStack itemStack) {
        if (itemStack == null) return false;
        InvManager invManager = Coolware.getInstance() != null && Coolware.getInstance().getModuleManager() != null
                ? Coolware.getInstance().getModuleManager().getModule(InvManager.class) : null;
        boolean invManagerEnabled = invManager != null && invManager.isToggled();

        if (ItemUtil.ItemType.Block.contains(itemStack)) {
            return !invManagerEnabled || ItemUtil.findInventorySlot(ItemUtil.ItemType.Block) < InvManager.blocksSetting.getValueInt();
        }
        if (ItemUtil.ItemType.Projectile.contains(itemStack)) {
            return !invManagerEnabled || ItemUtil.findInventorySlot(ItemUtil.ItemType.Projectile) < InvManager.projectilesSetting.getValueInt();
        }
        if (ItemUtil.ItemType.FishRod.contains(itemStack)) {
            return ItemUtil.findInventorySlot(ItemUtil.ItemType.Projectile) == 0;
        }
        if (ItemUtil.ItemType.Arrow.contains(itemStack)) {
            return !invManagerEnabled || ItemUtil.findInventorySlot(ItemUtil.ItemType.Arrow) < InvManager.arrowSetting.getValueInt();
        }
        return false;
    }

    private void shiftClick(int windowId, int slotId) {
        if (mc.playerController != null && mc.thePlayer != null) {
            mc.playerController.windowClick(windowId, slotId, 0, 1, mc.thePlayer);
            int minD = minDelaySetting.getValueInt();
            int maxD = maxDelaySetting.getValueInt();
            if (maxD < minD) maxD = minD;
            clickDelay = RandomUtils.nextInt(minD + 1, maxD + 2);
        }
    }

    @Override
    public void onUpdate() {
        if (clickDelay > 0) {
            clickDelay--;
        }
        if (oDelay > 0) {
            oDelay--;
        }
        if (!(mc.currentScreen instanceof GuiChest)) {
            inChest = false;
        } else {
            Container container = ((GuiChest) mc.currentScreen).inventorySlots;
            if (!(container instanceof ContainerChest)) {
                inChest = false;
            } else {
                if (!inChest) {
                    inChest = true;
                    warnedFull = false;
                    oDelay = openDelaySetting.getValueInt() + 1;
                }
                if (oDelay <= 0 && clickDelay <= 0) {
                    if (isValidGameMode() && mc.thePlayer != null) {
                        IInventory inventory = ((ContainerChest) container).getLowerChestInventory();
                        if (hypixelModeSetting.isEnabled()) {
                            String inventoryName = inventory.getName();
                            String stripped = inventoryName == null
                                    ? ""
                                    : EnumChatFormatting.getTextWithoutFormattingCodes(inventoryName).trim();
                            if (!stripped.isEmpty()) {
                                return;
                            }
                        } else if (nameCheckSetting.isEnabled()) {
                            String inventoryName = inventory.getName();
                            if (!inventoryName.equals(I18n.format("container.chest")) && !inventoryName.equals(I18n.format("container.chestDouble"))) {
                                return;
                            }
                        }
                        if (mc.thePlayer.inventory.getFirstEmptyStack() == -1) {
                            if (!warnedFull) {
                                mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Your inventory is full!"));
                                warnedFull = true;
                            }
                            if (autoCloseSetting.isEnabled()) {
                                mc.thePlayer.closeScreen();
                            }
                        } else {
                            if (skipTrashSetting.isEnabled()) {
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
                                double damage = swordInInventorySlot != -1 ? ItemUtil.getAttackBonus(mc.thePlayer.inventory.getStackInSlot(swordInInventorySlot)) : 0.0;
                                if (bestDamage > damage) {
                                    shiftClick(container.windowId, bestSword);
                                    return;
                                }
                                for (int i = 0; i < 4; i++) {
                                    int slot = ItemUtil.findArmorInventorySlot(i, true);
                                    double protectionLevel = slot != -1
                                            ? ItemUtil.getArmorProtection(mc.thePlayer.inventory.getStackInSlot(slot))
                                            : 0.0;
                                    if (bestArmorProtection[i] > protectionLevel) {
                                        shiftClick(container.windowId, bestArmorSlots[i]);
                                        return;
                                    }
                                }
                                int pickaxeSlot = ItemUtil.findInventorySlot("pickaxe", 0, true);
                                float pickaxeEfficiency = pickaxeSlot != -1 ? ItemUtil.getToolEfficiency(mc.thePlayer.inventory.getStackInSlot(pickaxeSlot)) : 1.0F;
                                if (bestPickaxeEfficiency > pickaxeEfficiency) {
                                    shiftClick(container.windowId, bestPickaxeSlot);
                                    return;
                                }
                                int shovelSlot = ItemUtil.findInventorySlot("shovel", 0, true);
                                float shovelEfficiency = shovelSlot != -1 ? ItemUtil.getToolEfficiency(mc.thePlayer.inventory.getStackInSlot(shovelSlot)) : 1.0F;
                                if (bestShovelEfficiency > shovelEfficiency) {
                                    shiftClick(container.windowId, bestShovelSlot);
                                    return;
                                }
                                int axeSlot = ItemUtil.findInventorySlot("axe", 0, true);
                                float efficiency = axeSlot != -1 ? ItemUtil.getToolEfficiency(mc.thePlayer.inventory.getStackInSlot(axeSlot)) : 1.0F;
                                if (bestAxeEfficiency > efficiency) {
                                    shiftClick(container.windowId, bestAxeSlot);
                                    return;
                                }
                                int bowSlot = ItemUtil.findBowInventorySlot(0, true);
                                double bowDamage = bowSlot != -1 ? ItemUtil.getBowAttackBonus(mc.thePlayer.inventory.getStackInSlot(bowSlot)) : 0.0;
                                if (bestBowDamage > bowDamage) {
                                    shiftClick(container.windowId, bestBow);
                                    return;
                                }
                            }

                            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                                if (container.getSlot(i).getHasStack()) {
                                    ItemStack stack = container.getSlot(i).getStack();
                                    if (!skipTrashSetting.isEnabled() || !ItemUtil.isNotSpecialItem(stack) || isMoreArmor(stack) || isMoreSword(stack) || isInvManagerRequire(stack)) {
                                        shiftClick(container.windowId, i);
                                        return;
                                    }
                                }
                            }
                            if (autoCloseSetting.isEnabled()) {
                                mc.thePlayer.closeScreen();
                            }
                        }
                    }
                }
            }
        }
    }
}
