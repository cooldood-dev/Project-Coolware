package com.github.cooldood.modules.impl.render;

import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.ModuleManager;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.modules.RegisterSubModule;
import com.github.cooldood.modules.impl.client.ThemeModule;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.render.FontUtil;
import com.github.cooldood.utils.render.RenderUtil;
import com.github.cooldood.utils.render.draggable.Draggable;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@RegisterModule(
        name = "Armour Display",
        description = "Displays equipped armor and held items with durability.",
        category = Category.RENDER
)
public class ArmourDisplay extends Module {

    @RegisterSubModule(name = "Mode", description = "Layout orientation")
    public static DisplayOrientation mode = DisplayOrientation.Horizontal;
    public enum DisplayOrientation {
        Horizontal, Vertical
    }

    @RegisterSubModule(name = "Show Held Item", description = "Also show the currently held item")
    public static boolean showHeldItem = true;

    @RegisterSubModule(name = "Show Durability Bar", description = "Render durability bar under items")
    public static boolean showDurabilityBar = true;

    @RegisterSubModule(name = "Show Percentage", description = "Render remaining durability percentage")
    public static boolean showPercentage = true;

    private static final float CORNER_RAD = 4.5f;

    public static Draggable armourDisplayDraggable = new Draggable(
            "ArmourDisplayHUD",
            () -> {
                if (C.p() == null) return new double[]{80, 24};

                List<ItemStack> items = new ArrayList<>();
                // Armor 3 (Helmet) down to 0 (Boots)
                for (int i = 3; i >= 0; i--) {
                    ItemStack stack = C.p().inventory.armorInventory[i];
                    if (stack != null) items.add(stack);
                }
                if (showHeldItem && C.p().getCurrentEquippedItem() != null) {
                    items.add(C.p().getCurrentEquippedItem());
                }

                if (items.isEmpty()) {
                    if (C.mc.currentScreen instanceof com.github.cooldood.screens.ClickGUI.ClickGUIScreen) {
                        Color bg = ThemeModule.panelBackground;
                        Color accent = ThemeModule.getAccentColor();
                        RenderUtil.drawRoundedRect(0, 0, 80, 24, CORNER_RAD, bg);
                        RenderUtil.drawRoundedRect(2, 2, 2, 20, 1, accent);
                        FontUtil.drawString("Armour", 8, 8, 9, Color.WHITE, false);
                        return new double[]{80, 24};
                    }
                    return new double[]{0, 0};
                }

                Color panelBg = ThemeModule.panelBackground;
                Color accent = ThemeModule.getAccentColor();

                if (mode == DisplayOrientation.Horizontal) {
                    float slotW = 22f;
                    float slotH = showPercentage ? 32f : 24f;
                    float totalW = items.size() * slotW + 4f;

                    RenderUtil.drawRoundedRect(0, 0, totalW, slotH, CORNER_RAD, panelBg);

                    float x = 2f;
                    for (ItemStack stack : items) {
                        renderItem(stack, x, 2f);
                        x += slotW;
                    }

                    return new double[]{totalW, slotH};
                } else {
                    float slotW = showPercentage ? 44f : 24f;
                    float slotH = 20f;
                    float totalH = items.size() * slotH + 4f;

                    RenderUtil.drawRoundedRect(0, 0, slotW, totalH, CORNER_RAD, panelBg);

                    float y = 2f;
                    for (ItemStack stack : items) {
                        renderItem(stack, 2f, y);
                        y += slotH;
                    }

                    return new double[]{slotW, totalH};
                }
            },
            e -> ModuleManager.isEnabled(ArmourDisplay.class),
            e -> true
    );

    private static void renderItem(ItemStack stack, float x, float y) {
        if (stack == null) return;

        GlStateManager.pushMatrix();
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        RenderHelper.enableGUIStandardItemLighting();

        C.mc.getRenderItem().renderItemAndEffectIntoGUI(stack, (int) x + 2, (int) y + 2);
        if (showDurabilityBar) {
            C.mc.getRenderItem().renderItemOverlays(C.mc.fontRendererObj, stack, (int) x + 2, (int) y + 2);
        }

        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();

        if (showPercentage && stack.isItemStackDamageable()) {
            int maxDamage = stack.getMaxDamage();
            int currentDamage = stack.getItemDamage();
            int remaining = maxDamage - currentDamage;
            int percent = (int) ((remaining / (float) maxDamage) * 100);

            Color pctColor = percent > 60 ? new Color(100, 255, 100) : percent > 25 ? new Color(255, 200, 50) : new Color(255, 80, 80);

            if (mode == DisplayOrientation.Horizontal) {
                String str = percent + "%";
                float strW = FontUtil.getStringWidth(str, 7);
                FontUtil.drawString(str, x + 10 - strW / 2f, y + 22, 7, pctColor, false);
            } else {
                String str = percent + "%";
                FontUtil.drawString(str, x + 22, y + 7, 8, pctColor, false);
            }
        }
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {}

    static {
        armourDisplayDraggable.x = 0.5;
        armourDisplayDraggable.y = 0.85;
    }
}
