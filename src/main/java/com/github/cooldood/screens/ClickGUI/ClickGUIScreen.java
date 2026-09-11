package com.github.cooldood.screens.ClickGUI;

import com.github.cooldood.Main;
import com.github.cooldood.events.impl.KeyPressedEvent;
import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.ModuleManager;
import com.github.cooldood.modules.SubModule;
import com.github.cooldood.modules.impl.client.ClickGUIModule;
import com.github.cooldood.screens.ClickGUI.SubModuleRenderers.*;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.client.KeybindHandler;
import com.github.cooldood.utils.client.ScreenUtil;
import com.github.cooldood.utils.render.EasingUtil;
import com.github.cooldood.utils.render.FontUtil;
import com.github.cooldood.utils.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.MathHelper;
import org.apache.commons.io.FileUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ClickGUIScreen extends GuiScreen {
    // ─── Layout constants ─────────────────────────────────────────────────────
    public static final int fontSize = 10;
    public static int GUI_TAB_WIDTH = 160;

    // The virtual anchor — panels are translated to sit at this local origin.
    public static final int BASE_X = -GUI_TAB_WIDTH / 2;
    public static final int BASE_Y = 0;

    // Claymorphic panel corners
    public static final float PANEL_RADIUS   = 0f;
    public static final float MODULE_RADIUS  = 0f;
    public static final float SUBMOD_RADIUS  = 0f;

    // Horizontal padding so modules are inset from the panel edges
    public static final int PANEL_PADDING    = 0;

    // ─── Colour palette ───────────────────────────────────────────────────────
    public static final Color COL_PANEL_BG      = new Color(15, 15, 18, 190);
    public static final Color COL_MODULE_BG     = new Color(0, 0, 0, 0);
    public static final Color COL_MODULE_HOVER  = new Color(255, 255, 255, 20);
    public static final Color COL_SUBMOD_BG     = new Color(0, 0, 0, 0);
    public static Color COL_ACCENT              = new Color(181, 166, 242);
    public static final Color COL_SHADOW        = new Color(0, 0, 0, 60);
    public static final Color COL_TOOLTIP       = new Color(15, 15, 18, 190);
    public static final Color COL_TEXT_DIM      = new Color(180, 180, 190, 255);

    // ─── Runtime state ────────────────────────────────────────────────────────
    public static float fpsMultiplier = 1;
    public static SubModule currentSubModule;
    public static int mouseButton = -1;
    public static boolean leftMouseDown = false;

    // ─── Renderer singletons ──────────────────────────────────────────────────
    protected static final CategoryRenderer categoryRenderer = new CategoryRenderer();
    protected static final ModuleRenderer moduleRenderer     = new ModuleRenderer();

    protected static final BooleanSubModuleRenderer  booleanSubModuleRenderer  = new BooleanSubModuleRenderer();
    protected static final EnumSubModuleRenderer     enumSubModuleRenderer     = new EnumSubModuleRenderer();
    protected static final SliderSubModuleRenderer   sliderSubModuleRenderer   = new SliderSubModuleRenderer();
    protected static final ColourModuleRenderer      colourSubModuleRenderer   = new ColourModuleRenderer();
    protected static final SubCategoryRenderer       subCategoryRenderer       = new SubCategoryRenderer();

    public static Color secondaryColor = COL_TEXT_DIM;

    // ─── Tooltip state ────────────────────────────────────────────────────────
    protected static Module    moduleHovered;
    protected static SubModule subModuleHovered;
    protected static long      hoverTime;

    private static final long minimumHoverTime    = 500;
    private static final int  hoverBoxXindent     = 6;
    private static final int  hoverBoxYindent     = 3;
    private static final int  hoverBoxTextSize    = 7;

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void initGui() {}

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (!ModuleManager.isEnabled(ClickGUIModule.class)) {
            C.mc.displayGuiScreen(null);
            ClickGUIScreen.saveCategoryPositions();
            return;
        }

        COL_ACCENT = com.github.cooldood.modules.impl.client.ThemeModule.primaryColor;

        fpsMultiplier = Math.max(Minecraft.getDebugFPS() * 0.1f, 2);
        int scrolledAmount = Mouse.getDWheel() / 5;

        // Draw darkened background (blur removed as it caused white screen)
        RenderUtil.drawRect(0, 0, C.res().getScaledWidth(), C.res().getScaledHeight(), new Color(0, 0, 0, 80));

        GL11.glPushMatrix();

        List<Module> modules = ModuleManager.getModules();

        if (!modules.contains(moduleHovered)) moduleHovered = null;

        for (Category category : Category.values()) {
            GL11.glPushMatrix();

            List<Module> modulesInCategory = ModuleManager.getModulesByCategory(category, modules);

            // smooth-drag animation
            category.renderX += (category.posX - category.renderX) / fpsMultiplier;
            if (Math.abs(category.renderX - category.posX) < 0.01) category.renderX = category.posX;
            float draggingRotationX = MathHelper.clamp_float((category.posX - category.renderX) / 3, -100, 100);

            category.renderY += (category.posY - category.renderY) / fpsMultiplier;
            if (Math.abs(category.renderY - category.posY) < 0.01) category.renderY = category.posY;
            double draggingRotationY = MathHelper.clamp_float((category.posY - category.renderY) / 3, -100, 100);

            if (ClickGUIModule.fancyDragging) {
                GL11.glTranslated(category.renderX - BASE_X, category.renderY - BASE_Y, 0);
                GL11.glRotated(draggingRotationX, 0, 0, 1);
                GL11.glRotated(draggingRotationY, 1, 0, 0);
            } else {
                GL11.glTranslated(category.posX - BASE_X, category.posY - BASE_Y, 0);
            }

            // ── category header ──
            categoryRenderer.handleMouse(category, mouseX, mouseY);
            categoryRenderer.render(category);

            // ── scissor + scroll ──
            RenderUtil.glScissor(BASE_X, BASE_Y,
                    GUI_TAB_WIDTH, C.res().getScaledHeight());

            category.renderScroll += (category.scroll - category.renderScroll) / fpsMultiplier;
            if (Math.abs(category.scroll - category.renderScroll) < 0.01) category.renderScroll = category.scroll;

            GL11.glTranslated(0, category.renderScroll, 0);

            if (category.shouldShow()) {
                double categoryAnimationProgress = EasingUtil.getAnimation(category.name());
                if (categoryAnimationProgress != -1) GL11.glScaled(1, categoryAnimationProgress, 1);

                for (Module module : modulesInCategory) {
                    moduleRenderer.handleMouse(module, mouseX, mouseY);
                    moduleRenderer.render(module, mouseX, mouseY);

                    double moduleAnimationProgress = EasingUtil.getAnimation(module.getUniqueKey(""));
                    if (moduleAnimationProgress != -1) GL11.glScaled(1, moduleAnimationProgress, 1);

                    if (!module.isOpen() && moduleAnimationProgress == -1) continue;

                    for (SubModule subModule : module.getChildren()) {
                        if (!subModule.shouldRender()) {
                            if (subModule == subModuleHovered) subModuleHovered = null;
                            continue;
                        }

                        double parentAnimationProgress = subModule.getAnimationProgress();
                        if (parentAnimationProgress != -1) GL11.glScaled(1, parentAnimationProgress, 1);

                        SubModuleRenderer.handle(mouseX, mouseY, subModule);

                        if (parentAnimationProgress != -1) GL11.glScaled(1, 1 / parentAnimationProgress, 1);
                    }

                    if (moduleAnimationProgress != -1) GL11.glScaled(1, 1 / moduleAnimationProgress, 1);
                }
            }

            // scroll clamping
            float[] translation = RenderUtil.getCurrentTranslation();
            float categoryTotalHeight = Math.max(
                    translation[1] - category.renderY - categoryRenderer.CATEGORY_HEIGHT,
                    categoryRenderer.CATEGORY_HEIGHT);

            if (ScreenUtil.isMouseOver(BASE_X, BASE_Y - categoryTotalHeight, GUI_TAB_WIDTH, categoryTotalHeight, mouseX, mouseY)) {
                category.scroll += scrolledAmount;
                scrolledAmount = 0;
            }

            if (translation[4] == 1 && category.open) {
                category.scroll = MathHelper.clamp_float(
                        category.scroll,
                        -categoryTotalHeight + category.renderScroll + categoryRenderer.CATEGORY_HEIGHT,
                        0
                );
            }

            GL11.glPopMatrix();
            RenderUtil.disableScissor();
        }

        GL11.glPopMatrix();
        mouseButton = -1;

        // ── Tooltip ──────────────────────────────────────────────────────────
        String hoverText = moduleHovered != null
                ? moduleHovered.getAnnotation().description()
                : subModuleHovered != null ? subModuleHovered.getAnnotation().description() : "";

        if (!hoverText.isEmpty() && System.currentTimeMillis() - ClickGUIScreen.hoverTime >= minimumHoverTime) {
            float hoverBoxH = FontUtil.getFontHeight(hoverBoxTextSize) + hoverBoxYindent * 2;
            int   hoverBoxW = FontUtil.getStringWidth(hoverText, hoverBoxTextSize) + (hoverBoxXindent * 2);

            drawClayTooltip(mouseX + 4, mouseY - hoverBoxH - 4, hoverBoxW, hoverBoxH);
            FontUtil.drawString(hoverText, mouseX + 4 + hoverBoxXindent, mouseY - hoverBoxH - 4 + hoverBoxYindent,
                    hoverBoxTextSize, Color.WHITE, true);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    // ─── Shared claymorphic panel drawing ─────────────────────────────────────

    public static void drawClayPanel(float panelHeight, Color accentColor) {
        float x = BASE_X;
        float y = BASE_Y;
        float w = GUI_TAB_WIDTH;

        RenderUtil.drawRect(x, y, w, panelHeight, COL_PANEL_BG);
        RenderUtil.drawRect(x + 4, y + panelHeight - 1, w - 8, 1, COL_ACCENT);
    }

    public static void drawModuleCard(float height, Color bg) {
        float x = BASE_X + PANEL_PADDING;
        float y = BASE_Y;
        float w = GUI_TAB_WIDTH - PANEL_PADDING * 2;

        RenderUtil.drawRect(x, y, w, height, COL_PANEL_BG);
        if (bg.getAlpha() > 0) {
            RenderUtil.drawRect(x, y, w, height, bg);
        }
    }

    public static void drawSubModuleCard(float height) {
        float x = BASE_X + PANEL_PADDING;
        float y = BASE_Y;
        float w = GUI_TAB_WIDTH - PANEL_PADDING * 2;

        RenderUtil.drawRect(x, y, w, height, COL_PANEL_BG);
        if (COL_SUBMOD_BG.getAlpha() > 0) {
            RenderUtil.drawRect(x, y, w, height, COL_SUBMOD_BG);
        }
    }

    public static void drawClayTooltip(float x, float y, float w, float h) {
        RenderUtil.drawBlurRect(x, y, w, h, 20);
        RenderUtil.drawRect(x, y, w, h, COL_TOOLTIP);
        RenderUtil.drawRectOutline(x, y, w, h, 1f, COL_ACCENT);
    }

    // ─── Keyboard / mouse overrides ───────────────────────────────────────────

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (KeybindHandler.listeningModule != null) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                KeybindHandler.removeKeybind(KeybindHandler.listeningModule);
                KeybindHandler.listeningModule = null;
            } else KeybindHandler.onKeyPressed(new KeyPressedEvent(keyCode, true));
        } else if (keyCode == Keyboard.KEY_ESCAPE) {
            ModuleManager.setEnabled(ClickGUIModule.class, false);
        } else if (subModuleHovered != null && subModuleHovered.isSlider()) {
            double increment = subModuleHovered.getAnnotation().increment();
            if ((subModuleHovered.getField().getType() == long.class
                    || subModuleHovered.getField().getType() == int.class) && increment < 1) increment = 1;
            double value = Double.parseDouble(subModuleHovered.get().toString());
            if (keyCode == Keyboard.KEY_RIGHT) subModuleHovered.set(value + increment);
            if (keyCode == Keyboard.KEY_LEFT)  subModuleHovered.set(value - increment);
        }
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        ClickGUIScreen.mouseButton = mouseButton;
        if (mouseButton == 0) leftMouseDown = true;
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        ClickGUIScreen.mouseButton = -1;
        if (mouseButton == 0) {
            leftMouseDown = false;
            categoryRenderer.currentDraggingCategory = null;
            currentSubModule = null;
        }
    }

    // ─── Category position persistence ────────────────────────────────────────

    private static final String categorySavingFile =
            Main.extraSavedFeaturesPath + "categoryPositions" + Main.configExtension;

    public static void saveCategoryPositions() {
        try {
            HashMap<String, float[]> posJSON = new HashMap<>();
            for (Category category : Category.values())
                posJSON.put(category.name(), new float[]{category.posX, category.posY});
            Files.createDirectories(Paths.get(Main.extraSavedFeaturesPath));
            Files.write(Paths.get(categorySavingFile), C.gson.toJson(posJSON).getBytes());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void loadCategoryPositions() {
        try {
            if (Files.exists(Paths.get(categorySavingFile))) {
                String configFileText = FileUtils.readFileToString(new File(categorySavingFile));
                HashMap<String, ArrayList<Double>> posJSON = C.gson.fromJson(configFileText, HashMap.class);
                for (Category category : Category.values()) {
                    if (posJSON.containsKey(category.name())) {
                        ArrayList<Double> xy = posJSON.get(category.name());
                        category.posX = category.renderX = xy.get(0).floatValue();
                        category.posY = category.renderY = xy.get(1).floatValue();
                    }
                }
            } else {
                System.out.println("No category positions found, saving default positions.");
                int startX = 20;
                int startY = 20;
                for (Category category : Category.values()) {
                    category.posX = category.renderX = startX;
                    category.posY = category.renderY = startY;
                    startX += GUI_TAB_WIDTH + 10;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}