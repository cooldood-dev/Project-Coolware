package com.github.cooldood.modules.impl.player;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.PacketEvent;
import com.github.cooldood.events.impl.PlayerUpdateEvent;
import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.modules.RegisterSubModule;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.minecraft.ItemUtil;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.world.WorldSettings.GameType;
import org.apache.commons.lang3.RandomUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;

@RegisterModule(
        name = "Inv Manager",
        description = "Provides Inv Manager functionality for the client.",
        category = Category.PLAYER
)
public class InvManager extends Module {
    @RegisterSubModule(name = "Min Delay", min = 0, max = 20, increment = 1)
    public static int minDelay = 1;

    @RegisterSubModule(name = "Max Delay", min = 0, max = 20, increment = 1)
    public static int maxDelay = 2;

    @RegisterSubModule(name = "Open Delay", min = 0, max = 20, increment = 1)
    public static int openDelay = 1;

    @RegisterSubModule(name = "Auto Armor")
    public static boolean autoArmor = true;

    @RegisterSubModule(name = "Auto Armor Interval", parent = "Auto Armor", min = 0, max = 100, increment = 1)
    public static int autoArmorInterval = 0;

    @RegisterSubModule(name = "Drop Trash")
    public static boolean dropTrash = false;

    @RegisterSubModule(name = "Check Durability")
    public static boolean checkDurability = true;

    @RegisterSubModule(name = "Sword Slot", min = 0, max = 9, increment = 1)
    public static int swordSlot = 1;

    @RegisterSubModule(name = "Pickaxe Slot", min = 0, max = 9, increment = 1)
    public static int pickaxeSlot = 3;

    @RegisterSubModule(name = "Shovel Slot", min = 0, max = 9, increment = 1)
    public static int shovelSlot = 4;

    @RegisterSubModule(name = "Axe Slot", min = 0, max = 9, increment = 1)
    public static int axeSlot = 5;

    @RegisterSubModule(name = "Blocks Slot", min = 0, max = 9, increment = 1)
    public static int blocksSlot = 2;

    @RegisterSubModule(name = "Blocks", min = 64, max = 2304, increment = 1)
    public static int blocks = 128;

    @RegisterSubModule(name = "Projectile Slot", min = 0, max = 9, increment = 1)
    public static int projectileSlot = 7;

    @RegisterSubModule(name = "Projectiles", min = 16, max = 2304, increment = 1)
    public static int projectiles = 64;

    @RegisterSubModule(name = "Gold Apple Slot", min = 0, max = 9, increment = 1)
    public static int goldAppleSlot = 9;

    @RegisterSubModule(name = "Arrow", min = 0, max = 2304, increment = 1)
    public static int arrow = 256;

    @RegisterSubModule(name = "Bow Slot", min = 0, max = 9, increment = 1)
    public static int bowSlot = 8;

    private static int actionDelay = 0;
    private static int oDelay = 0;
    private static boolean inventoryOpen = false;
    private static long autoArmorTime = System.currentTimeMillis();

    private static boolean isValidGameMode() {
        GameType gameType = C.mc.playerController.getCurrentGameType();
        return gameType == GameType.SURVIVAL || gameType == GameType.ADVENTURE;
    }

    private static int convertSlotIndex(int slot) {
        if (slot >= 36) {
            return 8 - (slot - 36);
        } else {
            return slot <= 8 ? slot + 36 : slot;
        }
    }

    private static void clickSlot(int windowId, int slotId, int mouseButtonClicked, int mode) {
        C.mc.playerController.windowClick(windowId, slotId, mouseButtonClicked, mode, C.p());
    }

    private static int getStackSize(int slot) {
        if (slot == -1) {
            return 0;
        } else {
            ItemStack stack = C.p().inventory.getStackInSlot(slot);
            return stack != null ? stack.stackSize : 0;
        }
    }

    @SubscribeEvent
    public static void onUpdate(PlayerUpdateEvent event) {
        if (actionDelay > 0) {
            actionDelay--;
        }
        if (oDelay > 0) {
            oDelay--;
        }
        if (!(C.mc.currentScreen instanceof GuiInventory)) {
            inventoryOpen = false;
        } else if (!(((GuiInventory) C.mc.currentScreen).inventorySlots instanceof ContainerPlayer)) {
            inventoryOpen = false;
        } else {
            if (!inventoryOpen) {
                inventoryOpen = true;
                oDelay = openDelay + 1;
                resetAutoArmorTime();
            }
            if (oDelay <= 0 && actionDelay <= 0) {
                if (isValidGameMode()) {
                    ArrayList<Integer> equippedArmorSlots = new ArrayList<>(Arrays.asList(-1, -1, -1, -1));
                    ArrayList<Integer> inventoryArmorSlots = new ArrayList<>(Arrays.asList(-1, -1, -1, -1));
                    for (int i = 0; i < 4; i++) {
                        equippedArmorSlots.set(i, ItemUtil.findArmorInventorySlot(i, true));
                        inventoryArmorSlots.set(i, ItemUtil.findArmorInventorySlot(i, false));
                    }
                    int preferredSwordHotbarSlot = swordSlot - 1;
                    int inventorySwordSlot = ItemUtil.findSwordInInventorySlot(preferredSwordHotbarSlot, checkDurability);
                    if (inventorySwordSlot == -1)
                        inventorySwordSlot = ItemUtil.findSwordInInventorySlot(preferredSwordHotbarSlot, false);
                    int preferredPickaxeHotbarSlot = pickaxeSlot - 1;
                    int inventoryPickaxeSlot = ItemUtil.findInventorySlot("pickaxe", preferredPickaxeHotbarSlot, checkDurability);
                    if (inventoryPickaxeSlot == -1)
                        inventoryPickaxeSlot = ItemUtil.findInventorySlot("pickaxe", preferredPickaxeHotbarSlot, false);
                    int preferredShovelHotbarSlot = shovelSlot - 1;
                    int inventoryShovelSlot = ItemUtil.findInventorySlot("shovel", preferredShovelHotbarSlot, checkDurability);
                    if (inventoryShovelSlot == -1)
                        inventoryShovelSlot = ItemUtil.findInventorySlot("shovel", preferredShovelHotbarSlot, false);
                    int preferredAxeHotbarSlot = axeSlot - 1;
                    int inventoryAxeSlot = ItemUtil.findInventorySlot("axe", preferredAxeHotbarSlot, checkDurability);
                    if (inventoryAxeSlot == -1)
                        inventoryAxeSlot = ItemUtil.findInventorySlot("axe", preferredAxeHotbarSlot, false);
                    int preferredBlocksHotbarSlot = blocksSlot - 1;
                    int inventoryBlocksSlot = ItemUtil.findInventorySlot(preferredBlocksHotbarSlot, ItemUtil.ItemType.Block);
                    int preferredProjectileHotbarSlot = projectileSlot - 1;
                    int inventoryProjectileSlot = ItemUtil.findInventorySlot(preferredProjectileHotbarSlot, ItemUtil.ItemType.Projectile);
                    if (inventoryProjectileSlot == -1)
                        inventoryProjectileSlot = ItemUtil.findInventorySlot(preferredProjectileHotbarSlot, ItemUtil.ItemType.FishRod);
                    int preferredGoldAppleHotbarSlot = goldAppleSlot - 1;
                    int inventoryGoldAppleSlot = ItemUtil.findInventorySlot(preferredGoldAppleHotbarSlot, ItemUtil.ItemType.GoldApple);
                    int preferredBowHotbarSlot = bowSlot - 1;
                    int inventoryBowSlot = ItemUtil.findBowInventorySlot(preferredBowHotbarSlot, checkDurability);
                    if (inventoryBowSlot == -1)
                        inventoryBowSlot = ItemUtil.findBowInventorySlot(preferredBowHotbarSlot, false);
                    if (autoArmor && autoArmorTimeElapsed()) {
                        for (int i = 0; i < 4; i++) {
                            int equippedSlot = equippedArmorSlots.get(i);
                            int inventorySlot = inventoryArmorSlots.get(i);
                            if (equippedSlot != -1 || inventorySlot != -1) {
                                int playerArmorSlot = 39 - i;
                                if (equippedSlot != playerArmorSlot && inventorySlot != playerArmorSlot) {
                                    if (C.p().inventory.getStackInSlot(playerArmorSlot) != null) {
                                        if (C.p().inventory.getFirstEmptyStack() != -1) {
                                            clickSlot(C.p().inventoryContainer.windowId, convertSlotIndex(playerArmorSlot), 0, 1);
                                        } else {
                                            clickSlot(C.p().inventoryContainer.windowId, convertSlotIndex(playerArmorSlot), 1, 4);
                                        }
                                    } else {
                                        int armorToEquipSlot = equippedSlot != -1 ? equippedSlot : inventorySlot;
                                        clickSlot(C.p().inventoryContainer.windowId, convertSlotIndex(armorToEquipSlot), 0, 1);
                                        resetAutoArmorTime();
                                    }
                                    return;
                                }
                            }
                        }
                    }
                    LinkedHashSet<Integer> usedHotbarSlots = new LinkedHashSet<>();
                    if (preferredSwordHotbarSlot >= 0 && preferredSwordHotbarSlot <= 8 && inventorySwordSlot != -1) {
                        usedHotbarSlots.add(preferredSwordHotbarSlot);
                        if (inventorySwordSlot != preferredSwordHotbarSlot) {
                            clickSlot(C.p().inventoryContainer.windowId, convertSlotIndex(inventorySwordSlot), preferredSwordHotbarSlot, 2);
                            return;
                        }
                    }
                    if (preferredPickaxeHotbarSlot >= 0 && preferredPickaxeHotbarSlot <= 8 && !usedHotbarSlots.contains(preferredPickaxeHotbarSlot) && inventoryPickaxeSlot != -1) {
                        usedHotbarSlots.add(preferredPickaxeHotbarSlot);
                        if (inventoryPickaxeSlot != preferredPickaxeHotbarSlot) {
                            clickSlot(C.p().inventoryContainer.windowId, convertSlotIndex(inventoryPickaxeSlot), preferredPickaxeHotbarSlot, 2);
                            return;
                        }
                    }
                    if (preferredShovelHotbarSlot >= 0 && preferredShovelHotbarSlot <= 8 && !usedHotbarSlots.contains(preferredShovelHotbarSlot) && inventoryShovelSlot != -1) {
                        usedHotbarSlots.add(preferredShovelHotbarSlot);
                        if (inventoryShovelSlot != preferredShovelHotbarSlot) {
                            clickSlot(C.p().inventoryContainer.windowId, convertSlotIndex(inventoryShovelSlot), preferredShovelHotbarSlot, 2);
                            return;
                        }
                    }
                    if (preferredAxeHotbarSlot >= 0 && preferredAxeHotbarSlot <= 8 && !usedHotbarSlots.contains(preferredAxeHotbarSlot) && inventoryAxeSlot != -1) {
                        usedHotbarSlots.add(preferredAxeHotbarSlot);
                        if (inventoryAxeSlot != preferredAxeHotbarSlot) {
                            clickSlot(C.p().inventoryContainer.windowId, convertSlotIndex(inventoryAxeSlot), preferredAxeHotbarSlot, 2);
                            return;
                        }
                    }
                    if (preferredBlocksHotbarSlot >= 0 && preferredBlocksHotbarSlot <= 8 && !usedHotbarSlots.contains(preferredBlocksHotbarSlot) && inventoryBlocksSlot != -1) {
                        usedHotbarSlots.add(preferredBlocksHotbarSlot);
                        if (inventoryBlocksSlot != preferredBlocksHotbarSlot) {
                            clickSlot(C.p().inventoryContainer.windowId, convertSlotIndex(inventoryBlocksSlot), preferredBlocksHotbarSlot, 2);
                            return;
                        }
                    }
                    if (preferredProjectileHotbarSlot >= 0 && preferredProjectileHotbarSlot <= 8 && !usedHotbarSlots.contains(preferredProjectileHotbarSlot) && inventoryProjectileSlot != -1) {
                        usedHotbarSlots.add(preferredProjectileHotbarSlot);
                        if (inventoryProjectileSlot != preferredProjectileHotbarSlot) {
                            clickSlot(C.p().inventoryContainer.windowId, convertSlotIndex(inventoryProjectileSlot), preferredProjectileHotbarSlot, 2);
                            return;
                        }
                    }
                    if (preferredGoldAppleHotbarSlot >= 0 && preferredGoldAppleHotbarSlot <= 8 && !usedHotbarSlots.contains(preferredGoldAppleHotbarSlot) && inventoryGoldAppleSlot != -1) {
                        usedHotbarSlots.add(preferredGoldAppleHotbarSlot);
                        if (inventoryGoldAppleSlot != preferredGoldAppleHotbarSlot) {
                            clickSlot(C.p().inventoryContainer.windowId, convertSlotIndex(inventoryGoldAppleSlot), preferredGoldAppleHotbarSlot, 2);
                            return;
                        }
                    }
                    if (preferredBowHotbarSlot >= 0 && preferredBowHotbarSlot <= 8 && !usedHotbarSlots.contains(preferredBowHotbarSlot) && inventoryBowSlot != -1) {
                        usedHotbarSlots.add(preferredBowHotbarSlot);
                        if (inventoryBowSlot != preferredBowHotbarSlot) {
                            clickSlot(C.p().inventoryContainer.windowId, convertSlotIndex(inventoryBowSlot), preferredBowHotbarSlot, 2);
                            return;
                        }
                    }
                    if (dropTrash) {
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
                                ItemStack stack = C.p().inventory.getStackInSlot(i);
                                if (stack != null) {
                                    boolean isBlock = ItemUtil.isBlock(stack);
                                    boolean isProjectile = ItemUtil.isProjectile(stack);
                                    if (isBlock) {
                                        currentBlockCount += stack.stackSize;
                                    }
                                    if (isProjectile) {
                                        currentProjectileCount += stack.stackSize;
                                    }
                                    if (isBlock ? currentBlockCount > blocks :
                                            isProjectile ? currentProjectileCount > projectiles :
                                                    ItemUtil.isNotSpecialItem(stack)) {
                                        clickSlot(C.p().inventoryContainer.windowId, convertSlotIndex(i), 1, 4);
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

    @SubscribeEvent
    public static void onWindowClick(PacketEvent.Send event) {
        if (event.packet instanceof C0EPacketClickWindow) {
            actionDelay = RandomUtils.nextInt(minDelay + 1, maxDelay + 2);
        }
    }

    private static void resetAutoArmorTime() {
        autoArmorTime = System.currentTimeMillis();
    }

    private static boolean autoArmorTimeElapsed() {
        return System.currentTimeMillis() - autoArmorTime > autoArmorInterval * 50L;
    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {

    }
}
