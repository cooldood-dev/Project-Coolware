package coolware.module.player;

import coolware.module.Category;
import coolware.module.Module;
import coolware.setting.BooleanSetting;
import coolware.setting.NumberSetting;
import coolware.util.ItemUtil;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldSettings.GameType;
import org.apache.commons.lang3.RandomUtils;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;

public class InvManager extends Module {
    public static NumberSetting minDelaySetting;
    public static NumberSetting maxDelaySetting;
    public static NumberSetting openDelaySetting;
    public static BooleanSetting autoArmorSetting;
    public static NumberSetting autoArmorIntervalSetting;
    public static BooleanSetting dropTrashSetting;
    public static BooleanSetting checkDurabilitySetting;

    public static NumberSetting swordSlotSetting;
    public static NumberSetting pickaxeSlotSetting;
    public static NumberSetting shovelSlotSetting;
    public static NumberSetting axeSlotSetting;
    public static NumberSetting blocksSlotSetting;
    public static NumberSetting blocksSetting;
    public static NumberSetting projectileSlotSetting;
    public static NumberSetting projectilesSetting;
    public static NumberSetting goldAppleSlotSetting;
    public static NumberSetting arrowSetting;
    public static NumberSetting bowSlotSetting;

    public static BooleanSetting spoofSetting;
    public boolean stop = false;
    public boolean stop2 = false;

    private int actionDelay = 0;
    private int oDelay = 0;
    private boolean inventoryOpen = false;
    private long autoArmorTime = System.currentTimeMillis();

    public boolean isSpoofEnabled() {
        return spoofSetting != null && spoofSetting.isEnabled();
    }

    public InvManager() {
        super("InvManager", "Automatically sorts inventory and equips best armor", Category.PLAYER, Keyboard.KEY_NONE);

        minDelaySetting = new NumberSetting("Min Delay", this, 1.0, 0.0, 20.0, 1.0);
        maxDelaySetting = new NumberSetting("Max Delay", this, 2.0, 0.0, 20.0, 1.0);
        openDelaySetting = new NumberSetting("Open Delay", this, 1.0, 0.0, 20.0, 1.0);
        autoArmorSetting = new BooleanSetting("Auto Armor", this, true);
        autoArmorIntervalSetting = new NumberSetting("Auto Armor Interval", this, 0.0, 0.0, 100.0, 1.0);
        dropTrashSetting = new BooleanSetting("Drop Trash", this, false);
        checkDurabilitySetting = new BooleanSetting("Check Durability", this, true);

        swordSlotSetting = new NumberSetting("Sword Slot", this, 1.0, 1.0, 9.0, 1.0);
        pickaxeSlotSetting = new NumberSetting("Pickaxe Slot", this, 3.0, 1.0, 9.0, 1.0);
        shovelSlotSetting = new NumberSetting("Shovel Slot", this, 4.0, 1.0, 9.0, 1.0);
        axeSlotSetting = new NumberSetting("Axe Slot", this, 5.0, 1.0, 9.0, 1.0);
        blocksSlotSetting = new NumberSetting("Blocks Slot", this, 2.0, 1.0, 9.0, 1.0);
        blocksSetting = new NumberSetting("Blocks", this, 128.0, 64.0, 2304.0, 64.0);
        projectileSlotSetting = new NumberSetting("Projectile Slot", this, 7.0, 1.0, 9.0, 1.0);
        projectilesSetting = new NumberSetting("Projectiles", this, 64.0, 16.0, 2304.0, 16.0);
        goldAppleSlotSetting = new NumberSetting("Gold Apple Slot", this, 9.0, 1.0, 9.0, 1.0);
        arrowSetting = new NumberSetting("Arrow", this, 256.0, 0.0, 2304.0, 64.0);
        bowSlotSetting = new NumberSetting("Bow Slot", this, 8.0, 1.0, 9.0, 1.0);

        spoofSetting = new BooleanSetting("Spoof", this, false);

        addSetting(minDelaySetting);
        addSetting(maxDelaySetting);
        addSetting(openDelaySetting);
        addSetting(spoofSetting);
        addSetting(autoArmorSetting);
        addSetting(autoArmorIntervalSetting);
        addSetting(dropTrashSetting);
        addSetting(checkDurabilitySetting);
        addSetting(swordSlotSetting);
        addSetting(pickaxeSlotSetting);
        addSetting(shovelSlotSetting);
        addSetting(axeSlotSetting);
        addSetting(blocksSlotSetting);
        addSetting(blocksSetting);
        addSetting(projectileSlotSetting);
        addSetting(projectilesSetting);
        addSetting(goldAppleSlotSetting);
        addSetting(arrowSetting);
        addSetting(bowSlotSetting);
    }

    @Override
    public void onEnable() {
        actionDelay = 0;
        oDelay = 0;
        inventoryOpen = false;
        resetAutoArmorTime();
    }

    @Override
    public void onDisable() {
        inventoryOpen = false;
    }

    private boolean isValidGameMode() {
        if (mc.playerController == null) return false;
        GameType gameType = mc.playerController.getCurrentGameType();
        return gameType == GameType.SURVIVAL || gameType == GameType.ADVENTURE;
    }

    private int convertSlotIndex(int slot) {
        if (slot >= 36) {
            return 8 - (slot - 36);
        } else {
            return slot <= 8 ? slot + 36 : slot;
        }
    }

    private void clickSlot(int windowId, int slotId, int mouseButtonClicked, int mode) {
        if (mc.playerController != null && mc.thePlayer != null) {
            mc.playerController.windowClick(windowId, slotId, mouseButtonClicked, mode, mc.thePlayer);
            int minD = minDelaySetting.getValueInt();
            int maxD = maxDelaySetting.getValueInt();
            if (maxD < minD) maxD = minD;
            actionDelay = RandomUtils.nextInt(minD + 1, maxD + 2);
        }
    }

    private int getStackSize(int slot) {
        if (slot == -1 || mc.thePlayer == null) {
            return 0;
        } else {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
            return stack != null ? stack.stackSize : 0;
        }
    }

    private void resetAutoArmorTime() {
        autoArmorTime = System.currentTimeMillis();
    }

    private boolean autoArmorTimeElapsed() {
        return System.currentTimeMillis() - autoArmorTime > autoArmorIntervalSetting.getValueInt() * 50L;
    }

    @Override
    public void onUpdate() {
        if (stop || stop2) return;
        if (actionDelay > 0) {
            actionDelay--;
        }
        if (oDelay > 0) {
            oDelay--;
        }
        boolean spoof = isSpoofEnabled();
        if (!(mc.currentScreen instanceof GuiInventory) && !(spoof && mc.currentScreen == null)) {
            inventoryOpen = false;
        } else if (mc.currentScreen instanceof GuiInventory && !(((GuiInventory) mc.currentScreen).inventorySlots instanceof ContainerPlayer)) {
            inventoryOpen = false;
        } else {
            if (!inventoryOpen) {
                inventoryOpen = true;
                oDelay = openDelaySetting.getValueInt() + 1;
                resetAutoArmorTime();
            }
            if (oDelay <= 0 && actionDelay <= 0) {
                if (isValidGameMode() && mc.thePlayer != null) {
                    boolean checkDurability = checkDurabilitySetting.isEnabled();
                    ArrayList<Integer> equippedArmorSlots = new ArrayList<Integer>(Arrays.asList(-1, -1, -1, -1));
                    ArrayList<Integer> inventoryArmorSlots = new ArrayList<Integer>(Arrays.asList(-1, -1, -1, -1));
                    for (int i = 0; i < 4; i++) {
                        equippedArmorSlots.set(i, ItemUtil.findArmorInventorySlot(i, true));
                        inventoryArmorSlots.set(i, ItemUtil.findArmorInventorySlot(i, false));
                    }
                    int preferredSwordHotbarSlot = swordSlotSetting.getValueInt() - 1;
                    int inventorySwordSlot = ItemUtil.findSwordInInventorySlot(preferredSwordHotbarSlot, checkDurability);
                    if (inventorySwordSlot == -1)
                        inventorySwordSlot = ItemUtil.findSwordInInventorySlot(preferredSwordHotbarSlot, false);
                    int preferredPickaxeHotbarSlot = pickaxeSlotSetting.getValueInt() - 1;
                    int inventoryPickaxeSlot = ItemUtil.findInventorySlot("pickaxe", preferredPickaxeHotbarSlot, checkDurability);
                    if (inventoryPickaxeSlot == -1)
                        inventoryPickaxeSlot = ItemUtil.findInventorySlot("pickaxe", preferredPickaxeHotbarSlot, false);
                    int preferredShovelHotbarSlot = shovelSlotSetting.getValueInt() - 1;
                    int inventoryShovelSlot = ItemUtil.findInventorySlot("shovel", preferredShovelHotbarSlot, checkDurability);
                    if (inventoryShovelSlot == -1)
                        inventoryShovelSlot = ItemUtil.findInventorySlot("shovel", preferredShovelHotbarSlot, false);
                    int preferredAxeHotbarSlot = axeSlotSetting.getValueInt() - 1;
                    int inventoryAxeSlot = ItemUtil.findInventorySlot("axe", preferredAxeHotbarSlot, checkDurability);
                    if (inventoryAxeSlot == -1)
                        inventoryAxeSlot = ItemUtil.findInventorySlot("axe", preferredAxeHotbarSlot, false);
                    int preferredBlocksHotbarSlot = blocksSlotSetting.getValueInt() - 1;
                    int inventoryBlocksSlot = ItemUtil.findInventorySlot(preferredBlocksHotbarSlot, ItemUtil.ItemType.Block);
                    int preferredProjectileHotbarSlot = projectileSlotSetting.getValueInt() - 1;
                    int inventoryProjectileSlot = ItemUtil.findInventorySlot(preferredProjectileHotbarSlot, ItemUtil.ItemType.Projectile);
                    if (inventoryProjectileSlot == -1)
                        inventoryProjectileSlot = ItemUtil.findInventorySlot(preferredProjectileHotbarSlot, ItemUtil.ItemType.FishRod);
                    int preferredGoldAppleHotbarSlot = goldAppleSlotSetting.getValueInt() - 1;
                    int inventoryGoldAppleSlot = ItemUtil.findInventorySlot(preferredGoldAppleHotbarSlot, ItemUtil.ItemType.GoldApple);
                    int preferredBowHotbarSlot = bowSlotSetting.getValueInt() - 1;
                    int inventoryBowSlot = ItemUtil.findBowInventorySlot(preferredBowHotbarSlot, checkDurability);
                    if (inventoryBowSlot == -1)
                        inventoryBowSlot = ItemUtil.findBowInventorySlot(preferredBowHotbarSlot, false);

                    if (autoArmorSetting.isEnabled() && autoArmorTimeElapsed()) {
                        for (int i = 0; i < 4; i++) {
                            int equippedSlot = equippedArmorSlots.get(i);
                            int inventorySlot = inventoryArmorSlots.get(i);
                            if (equippedSlot != -1 || inventorySlot != -1) {
                                int playerArmorSlot = 39 - i;
                                if (equippedSlot != playerArmorSlot && inventorySlot != playerArmorSlot) {
                                    if (mc.thePlayer.inventory.getStackInSlot(playerArmorSlot) != null) {
                                        if (mc.thePlayer.inventory.getFirstEmptyStack() != -1) {
                                            clickSlot(mc.thePlayer.inventoryContainer.windowId, convertSlotIndex(playerArmorSlot), 0, 1);
                                        } else {
                                            clickSlot(mc.thePlayer.inventoryContainer.windowId, convertSlotIndex(playerArmorSlot), 1, 4);
                                        }
                                    } else {
                                        int armorToEquipSlot = equippedSlot != -1 ? equippedSlot : inventorySlot;
                                        clickSlot(mc.thePlayer.inventoryContainer.windowId, convertSlotIndex(armorToEquipSlot), 0, 1);
                                        resetAutoArmorTime();
                                    }
                                    return;
                                }
                            }
                        }
                    }
                    LinkedHashSet<Integer> usedHotbarSlots = new LinkedHashSet<Integer>();
                    if (preferredSwordHotbarSlot >= 0 && preferredSwordHotbarSlot <= 8 && inventorySwordSlot != -1) {
                        usedHotbarSlots.add(preferredSwordHotbarSlot);
                        if (inventorySwordSlot != preferredSwordHotbarSlot) {
                            clickSlot(mc.thePlayer.inventoryContainer.windowId, convertSlotIndex(inventorySwordSlot), preferredSwordHotbarSlot, 2);
                            return;
                        }
                    }
                    if (preferredPickaxeHotbarSlot >= 0 && preferredPickaxeHotbarSlot <= 8 && !usedHotbarSlots.contains(preferredPickaxeHotbarSlot) && inventoryPickaxeSlot != -1) {
                        usedHotbarSlots.add(preferredPickaxeHotbarSlot);
                        if (inventoryPickaxeSlot != preferredPickaxeHotbarSlot) {
                            clickSlot(mc.thePlayer.inventoryContainer.windowId, convertSlotIndex(inventoryPickaxeSlot), preferredPickaxeHotbarSlot, 2);
                            return;
                        }
                    }
                    if (preferredShovelHotbarSlot >= 0 && preferredShovelHotbarSlot <= 8 && !usedHotbarSlots.contains(preferredShovelHotbarSlot) && inventoryShovelSlot != -1) {
                        usedHotbarSlots.add(preferredShovelHotbarSlot);
                        if (inventoryShovelSlot != preferredShovelHotbarSlot) {
                            clickSlot(mc.thePlayer.inventoryContainer.windowId, convertSlotIndex(inventoryShovelSlot), preferredShovelHotbarSlot, 2);
                            return;
                        }
                    }
                    if (preferredAxeHotbarSlot >= 0 && preferredAxeHotbarSlot <= 8 && !usedHotbarSlots.contains(preferredAxeHotbarSlot) && inventoryAxeSlot != -1) {
                        usedHotbarSlots.add(preferredAxeHotbarSlot);
                        if (inventoryAxeSlot != preferredAxeHotbarSlot) {
                            clickSlot(mc.thePlayer.inventoryContainer.windowId, convertSlotIndex(inventoryAxeSlot), preferredAxeHotbarSlot, 2);
                            return;
                        }
                    }
                    if (preferredBlocksHotbarSlot >= 0 && preferredBlocksHotbarSlot <= 8 && !usedHotbarSlots.contains(preferredBlocksHotbarSlot) && inventoryBlocksSlot != -1) {
                        usedHotbarSlots.add(preferredBlocksHotbarSlot);
                        if (inventoryBlocksSlot != preferredBlocksHotbarSlot) {
                            clickSlot(mc.thePlayer.inventoryContainer.windowId, convertSlotIndex(inventoryBlocksSlot), preferredBlocksHotbarSlot, 2);
                            return;
                        }
                    }
                    if (preferredProjectileHotbarSlot >= 0 && preferredProjectileHotbarSlot <= 8 && !usedHotbarSlots.contains(preferredProjectileHotbarSlot) && inventoryProjectileSlot != -1) {
                        usedHotbarSlots.add(preferredProjectileHotbarSlot);
                        if (inventoryProjectileSlot != preferredProjectileHotbarSlot) {
                            clickSlot(mc.thePlayer.inventoryContainer.windowId, convertSlotIndex(inventoryProjectileSlot), preferredProjectileHotbarSlot, 2);
                            return;
                        }
                    }
                    if (preferredGoldAppleHotbarSlot >= 0 && preferredGoldAppleHotbarSlot <= 8 && !usedHotbarSlots.contains(preferredGoldAppleHotbarSlot) && inventoryGoldAppleSlot != -1) {
                        usedHotbarSlots.add(preferredGoldAppleHotbarSlot);
                        if (inventoryGoldAppleSlot != preferredGoldAppleHotbarSlot) {
                            clickSlot(mc.thePlayer.inventoryContainer.windowId, convertSlotIndex(inventoryGoldAppleSlot), preferredGoldAppleHotbarSlot, 2);
                            return;
                        }
                    }
                    if (preferredBowHotbarSlot >= 0 && preferredBowHotbarSlot <= 8 && !usedHotbarSlots.contains(preferredBowHotbarSlot) && inventoryBowSlot != -1) {
                        usedHotbarSlots.add(preferredBowHotbarSlot);
                        if (inventoryBowSlot != preferredBowHotbarSlot) {
                            clickSlot(mc.thePlayer.inventoryContainer.windowId, convertSlotIndex(inventoryBowSlot), preferredBowHotbarSlot, 2);
                            return;
                        }
                    }
                    if (dropTrashSetting.isEnabled()) {
                        int currentBlockCount = getStackSize(inventoryBlocksSlot);
                        int currentProjectileCount = getStackSize(inventoryProjectileSlot);
                        for (int i = 0; i < 36; i++) {
                            if (!equippedArmorSlots.contains(i)
                                    && !inventoryArmorSlots.contains(i)
                                    && inventorySwordSlot != i
                                    && inventoryPickaxeSlot != i
                                    && inventoryShovelSlot != i
                                    && inventoryAxeSlot != i
                                    && inventoryBlocksSlot != i
                                    && inventoryProjectileSlot != i
                                    && inventoryGoldAppleSlot != i
                                    && inventoryBowSlot != i) {
                                ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
                                if (stack != null) {
                                    boolean isBlock = ItemUtil.isBlock(stack);
                                    boolean isProjectile = ItemUtil.isProjectile(stack);
                                    if (isBlock) {
                                        currentBlockCount += stack.stackSize;
                                    }
                                    if (isProjectile) {
                                        currentProjectileCount += stack.stackSize;
                                    }
                                    if (isBlock ? currentBlockCount > blocksSetting.getValueInt() :
                                            isProjectile ? currentProjectileCount > projectilesSetting.getValueInt() :
                                                    ItemUtil.isNotSpecialItem(stack)) {
                                        clickSlot(mc.thePlayer.inventoryContainer.windowId, convertSlotIndex(i), 1, 4);
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
