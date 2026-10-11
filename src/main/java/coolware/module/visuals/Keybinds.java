package coolware.module.visuals;

import coolware.Coolware;
import coolware.config.ConfigManager;
import coolware.font.moonlight.FontRenderer;
import coolware.font.moonlight.Fonts;
import coolware.gui.hud.IslandBlur;
import coolware.module.Category;
import coolware.module.Module;
import coolware.setting.BooleanSetting;
import coolware.setting.NumberSetting;
import coolware.util.RenderUtil;
import coolware.util.SvgUtil;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class Keybinds extends Module {

    private final NumberSetting posX = new NumberSetting("Position X", this, 6.0D, 0.0D, 4000.0D, 1.0D);
    private final NumberSetting posY = new NumberSetting("Position Y", this, 90.0D, 0.0D, 4000.0D, 1.0D);
    private final BooleanSetting blur = new BooleanSetting("Blur", this, true);
    private final NumberSetting blurRadius = new NumberSetting("Blur Strength", this, 18.0D, 4.0D, 30.0D, 1.0D);
    private final BooleanSetting shadow = new BooleanSetting("Shadow", this, true);
    private final NumberSetting bgAlpha = new NumberSetting("Background Alpha", this, 60.0D, 10.0D, 255.0D, 5.0D);

    private static final ResourceLocation KEYBINDS_ICON = new ResourceLocation("minecraft", "textures/coolware/keybinds.svg");

    private float renderedX;
    private float renderedY;
    private float renderedWidth = 110.0F;
    private float renderedHeight = 30.0F;

    private boolean dragging;
    private float dragOffsetX;
    private float dragOffsetY;
    private boolean positionChanged;

    private float smoothX = -1;
    private float smoothY = -1;
    private float smoothWidth = 110.0F;
    private float smoothHeight = 30.0F;
    private long lastAnimTime = System.currentTimeMillis();

    public Keybinds() {
        super("Keybinds", "Displays active keybinds for toggled modules in a sleek glassmorphic panel", Category.VISUALS, Keyboard.KEY_NONE);
        addSetting(posX);
        addSetting(posY);
        addSetting(blur);
        addSetting(blurRadius);
        addSetting(shadow);
        addSetting(bgAlpha);
        setToggled(true);
    }

    @Override
    public void onRender2D() {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        List<Module> boundModules = new ArrayList<Module>();
        if (Coolware.getInstance() != null && Coolware.getInstance().getModuleManager() != null) {
            for (Module m : Coolware.getInstance().getModuleManager().getModules()) {
                // Must be toggled, must have a valid keybind, ignore non-actionable panels
                if (m.isToggled() && m.getKeyCode() != Keyboard.KEY_NONE && m.getKeyCode() != 0) {
                    if (!m.getName().equalsIgnoreCase("ClickGUI") && !m.getName().equalsIgnoreCase("Keybinds")) {
                        boundModules.add(m);
                    }
                }
            }
        }

        // Hide if no bound toggled modules and not currently editing/chatting
        if (boundModules.isEmpty() && !(mc.currentScreen instanceof GuiChat)) {
            return;
        }

        // Fonts
        FontRenderer titleFont = Fonts.nunito.get(16.0F);
        FontRenderer nameFont = Fonts.interMedium.get(13.0F);
        FontRenderer keyFont = Fonts.interMedium.get(12.0F);

        // Header metrics
        float headerHeight = 22.0F;
        float rowHeight = 15.0F;
        float padding = 8.0F;

        // Calculate dynamic width based on entries
        float maxContentWidth = 85.0F;
        for (Module m : boundModules) {
            String modName = m.getName();
            String keyName = getKeyDisplay(m.getKeyCode());
            float nameW = nameFont.getStringWidth(modName);
            float keyW = keyFont.getStringWidth(keyName);
            float totalRowW = nameW + keyW + 20.0F;
            if (totalRowW > maxContentWidth) {
                maxContentWidth = totalRowW;
            }
        }
        if (boundModules.isEmpty()) {
            maxContentWidth = Math.max(maxContentWidth, nameFont.getStringWidth("No active binds") + 20.0F);
        }

        float targetWidth = Math.max(116.0F, maxContentWidth + padding * 2);
        int entryCount = boundModules.isEmpty() ? 1 : boundModules.size();
        float targetHeight = headerHeight + (entryCount * rowHeight) + 4.0F;

        // Coordinates interpolation
        float targetX = posX.getValueFloat();
        float targetY = posY.getValueFloat();

        long now = System.currentTimeMillis();
        float delta = Math.min(1.0F, Math.max(0.001F, (now - lastAnimTime) / 1000.0F));
        lastAnimTime = now;
        float factor = 1.0F - (float) Math.pow(0.0001, delta);

        if (smoothX < 0 || smoothY < 0) {
            smoothX = targetX;
            smoothY = targetY;
            smoothWidth = targetWidth;
            smoothHeight = targetHeight;
        } else {
            smoothX += (targetX - smoothX) * factor;
            smoothY += (targetY - smoothY) * factor;
            smoothWidth += (targetWidth - smoothWidth) * factor;
            smoothHeight += (targetHeight - smoothHeight) * factor;
        }

        float x = smoothX;
        float y = smoothY;
        float width = smoothWidth;
        float height = smoothHeight;
        float cornerRadius = 6.0F;

        // 1. Shader Blur (Pure glassmorphic blurred background)
        if (blur.isEnabled()) {
            IslandBlur.renderBlurRounded(x, y, width, height, blurRadius.getValueFloat(), cornerRadius);
        }

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        // 2. Subtle contact drop shadow beneath the card (soft, clean, never muddying the glass plate)
        if (shadow.isEnabled() && !blur.isEnabled()) {
            RenderUtil.drawRoundedRect(x, y + 1.5F, width, height, cornerRadius, 0x22000000);
        }

        // 3. Translucent Acrylic Glass Backplate (Clean translucent frosted dark glass)
        int alphaVal = (int) Math.max(15.0D, Math.min(120.0D, bgAlpha.getValue()));
        int bgTop = (alphaVal << 24) | 0x00141418;
        int bgBot = ((int) Math.min(145, alphaVal * 1.15F) << 24) | 0x000A0A0E;
        RenderUtil.drawRoundedGradient(x, y, width, height, cornerRadius, bgTop, bgBot);

        // 4. Subtle Specular Highlight / Glass Edge Sheen Outline
        RenderUtil.drawRoundedOutline(x, y, width, height, cornerRadius, 1.0F, 0x24FFFFFF);

        // 5. Header Area
        float iconSize = 13.0F;
        float iconX = x + padding;
        float iconY = y + (headerHeight - iconSize) / 2.0F;

        // Render SVG icon (crisp white)
        SvgUtil.drawSvg(KEYBINDS_ICON, iconX, iconY, iconSize, iconSize, 0xEEFFFFFF);

        // Title text "Keybinds"
        float titleTextX = iconX + iconSize + 6.0F;
        float titleTextY = y + (headerHeight - titleFont.getHeight()) / 2.0F + 1.0F;
        titleFont.drawString("Keybinds", titleTextX, titleTextY, 0xFFFFFFFF);

        // Subtle divider line under header (clean neutral frosted line)
        float dividerY = y + headerHeight - 0.5F;
        RenderUtil.drawRect(x + padding, dividerY, x + width - padding, dividerY + 1.0F, 0x14FFFFFF);

        // 6. Active Keybind Items
        float currentY = y + headerHeight + 3.0F;

        // Calculate maximum key width so all keys share the exact same aligned column
        float maxKeyColWidth = 10.0F;
        for (Module m : boundModules) {
            float kw = keyFont.getStringWidth(getKeyDisplay(m.getKeyCode()));
            if (kw > maxKeyColWidth) {
                maxKeyColWidth = kw;
            }
        }
        float keyColRight = x + width - padding;
        float keyColCenter = keyColRight - (maxKeyColWidth / 2.0F);

        if (boundModules.isEmpty()) {
            nameFont.drawString("No active binds", x + padding + 2.0F, currentY + 3.0F, 0x77FFFFFF);
        } else {
            for (Module m : boundModules) {
                String modName = m.getName();
                String keyStr = getKeyDisplay(m.getKeyCode());

                // Module name on the left (pure crisp slightly softened white)
                nameFont.drawString(modName, x + padding + 2.0F, currentY + (rowHeight - nameFont.getHeight()) / 2.0F, 0xEEEDED);

                // Perfectly aligned key letter column (centered on the fixed column line)
                float keyStrW = keyFont.getStringWidth(keyStr);
                float keyTextX = keyColCenter - (keyStrW / 2.0F);
                float keyTextY = currentY + (rowHeight - keyFont.getHeight()) / 2.0F + 0.5F;
                keyFont.drawString(keyStr, keyTextX, keyTextY, 0xFFA0A0A6);

                currentY += rowHeight;
            }
        }

        renderedX = x;
        renderedY = y;
        renderedWidth = width;
        renderedHeight = height;

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private String getKeyDisplay(int keyCode) {
        if (keyCode == Keyboard.KEY_NONE || keyCode == 0) return "NONE";
        String name = Keyboard.getKeyName(keyCode);
        if (name == null || name.isEmpty()) return "K" + keyCode;
        return name.toUpperCase();
    }

    public boolean beginDrag(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0 || !isToggled()) {
            return false;
        }
        if (mouseX >= renderedX && mouseX <= renderedX + renderedWidth &&
                mouseY >= renderedY && mouseY <= renderedY + renderedHeight) {
            dragging = true;
            dragOffsetX = mouseX - posX.getValueFloat();
            dragOffsetY = mouseY - posY.getValueFloat();
            positionChanged = false;
            return true;
        }
        return false;
    }

    public boolean drag(int mouseX, int mouseY, int mouseButton) {
        if (!dragging || mouseButton != 0) {
            return false;
        }
        ScaledResolution resolution = new ScaledResolution(mc);
        float newX = mouseX - dragOffsetX;
        float newY = mouseY - dragOffsetY;
        newX = Math.max(0.0F, Math.min(newX, resolution.getScaledWidth() - renderedWidth));
        newY = Math.max(0.0F, Math.min(newY, resolution.getScaledHeight() - renderedHeight));
        posX.setValue(newX);
        posY.setValue(newY);
        positionChanged = true;
        return true;
    }

    public boolean endDrag(int mouseButton) {
        if (!dragging || mouseButton != 0) {
            return false;
        }
        dragging = false;
        if (positionChanged) {
            ConfigManager.saveConfig(ConfigManager.DEFAULT_CONFIG);
            positionChanged = false;
        }
        return true;
    }

    public float getRenderedX() {
        return renderedX;
    }

    public float getRenderedY() {
        return renderedY;
    }

    public float getRenderedWidth() {
        return renderedWidth;
    }

    public float getRenderedHeight() {
        return renderedHeight;
    }
}
