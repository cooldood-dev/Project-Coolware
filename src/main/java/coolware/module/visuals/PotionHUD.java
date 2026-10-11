package coolware.module.visuals;

import coolware.config.ConfigManager;
import coolware.font.moonlight.Fonts;
import coolware.module.Category;
import coolware.module.Module;
import coolware.setting.ModeSetting;
import coolware.setting.NumberSetting;
import coolware.util.RoundedUtils;
import coolware.util.animation.ContinualAnimation;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;

public class PotionHUD extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", this, "Cool 1", "Cool 1", "Cool 2");
    private final NumberSetting posX = new NumberSetting("Position X", this, 10.0D, 0.0D, 4000.0D, 1.0D);
    private final NumberSetting posY = new NumberSetting("Position Y", this, 150.0D, 0.0D, 4000.0D, 1.0D);
    private final ModeSetting colorMode = new ModeSetting("Color Mode", this, "Theme", "Theme", "Rainbow");

    private final ContinualAnimation widthAnimation = new ContinualAnimation();
    private final ContinualAnimation heightAnimation = new ContinualAnimation();

    private float width = 0.0F;
    private float height = 0.0F;

    private float smoothX = -1;
    private float smoothY = -1;
    private long lastAnimTime = System.currentTimeMillis();

    private float renderedX;
    private float renderedY;
    private float renderedWidth;
    private float renderedHeight;
    private boolean dragging;
    private float dragOffsetX;
    private float dragOffsetY;
    private boolean positionChanged;

    public PotionHUD() {
        super("PotionHUD", "Displays active potion effects and status", Category.VISUALS, Keyboard.KEY_NONE);
        addSetting(mode);
        addSetting(posX);
        addSetting(posY);
        addSetting(colorMode);
        setToggled(true);
    }

    public int getColor(int counter) {
        if ("Rainbow".equalsIgnoreCase(colorMode.getMode())) {
            double rainbowState = Math.ceil((System.currentTimeMillis() - (long) counter * 20L) / 8.0);
            rainbowState %= 360.0;
            return Color.HSBtoRGB((float) (rainbowState / 360.0), 0.8F, 1.0F);
        }
        Theme theme = Theme.getInstance();
        return theme != null ? theme.getColor(counter) : 0xFF55AAFF;
    }

    public int getBgColor() {
        return new Color(21, 21, 24, 180).getRGB();
    }

    @Override
    public void onRender2D() {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        ArrayList<PotionEffect> potions = new ArrayList<PotionEffect>(mc.thePlayer.getActivePotionEffects());
        if (potions.isEmpty() && !(mc.currentScreen instanceof GuiChat)) {
            return;
        }

        ScaledResolution sr = new ScaledResolution(mc);

        // Cool 1: Moonlight corner mode
        if ("Cool 1".equalsIgnoreCase(mode.getMode())) {
            renderMoon(sr, potions);
            return;
        }

        // Cool 2: Sexy card mode (draggable)
        float targetX = posX.getValueFloat();
        float targetY = posY.getValueFloat();

        if (smoothX < 0 || smoothY < 0) {
            smoothX = targetX;
            smoothY = targetY;
            lastAnimTime = System.currentTimeMillis();
        } else {
            long now = System.currentTimeMillis();
            float delta = Math.min(1.0F, (now - lastAnimTime) / 1000.0F);
            lastAnimTime = now;
            float factor = 1.0F - (float) Math.pow(0.0001, delta);
            smoothX += (targetX - smoothX) * factor;
            smoothY += (targetY - smoothY) * factor;
        }

        float renderX = smoothX;
        float renderY = smoothY;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        renderSexy(renderX, renderY, potions);

        renderedX = renderX;
        renderedY = renderY;
        renderedWidth = Math.max(width, 40.0F);
        renderedHeight = Math.max(height, 20.0F);

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void renderSexy(float renderX, float renderY, ArrayList<PotionEffect> potions) {
        this.width = 92.0f;
        this.height = heightAnimation.getOutput();
        RoundedUtils.drawRound(renderX, renderY, this.width, this.height, 6.0f, new Color(getBgColor(), true));
        Fonts.interSemiBold.get(13.0f).drawString("Potions", renderX + 8.0f, renderY + 7.0f + 2.0f, -1);
        Fonts.nursultan.get(14.0f).drawString("E", renderX + this.width - 16.0f, renderY + 9.0f, getColor(0));
        float offset = renderY + 21.0f;
        for (PotionEffect potion : potions) {
            String name = I18n.format(Potion.potionTypes[potion.getPotionID()].getName()) + " " + (potion.getAmplifier() > 0 ? I18n.format("enchantment.level." + (potion.getAmplifier() + 1)) : "");
            String duration = Potion.getDurationString(potion);
            Fonts.interRegular.get(11.0f).drawString(name, renderX + 8.0f, offset, -1);
            Fonts.interRegular.get(11.0f).drawString(duration, renderX + this.width - 8.0f - (float) Fonts.interRegular.get(11.0f).getStringWidth(duration), offset, -1);
            offset += 10.0f;
        }
        heightAnimation.animate((float) (20 + potions.size() * 10), 20);
    }

    private void renderMoon(ScaledResolution sr, ArrayList<PotionEffect> potions) {
        potions.sort(Comparator.comparingDouble(effect -> -Fonts.interMedium.get(19.0f).getStringWidth(I18n.format(Potion.potionTypes[effect.getPotionID()].getName()))));
        float y = mc.currentScreen instanceof GuiChat ? -14.0f : -3.0f;
        for (PotionEffect potionEffect : potions) {
            Potion potionType = Potion.potionTypes[potionEffect.getPotionID()];
            String potionName = I18n.format(potionType.getName());
            String type = " §7-";
            if (potionEffect.getAmplifier() == 1) {
                potionName = potionName + " 2";
            } else if (potionEffect.getAmplifier() == 2) {
                potionName = potionName + " 3";
            } else if (potionEffect.getAmplifier() == 3) {
                potionName = potionName + " 4";
            }
            if (potionEffect.getDuration() < 600 && potionEffect.getDuration() > 300) {
                type = type + " §f" + Potion.getDurationString(potionEffect);
            } else if (potionEffect.getDuration() < 300) {
                type = type + " §f" + Potion.getDurationString(potionEffect);
            } else if (potionEffect.getDuration() > 600) {
                type = type + " §f" + Potion.getDurationString(potionEffect);
            }
            GlStateManager.pushMatrix();
            Fonts.interMedium.get(17.0f).drawStringWithShadow(potionName, (float) sr.getScaledWidth() - (float) Fonts.interSemiBold.get(17.0f).getStringWidth(type + potionName) - 2.0f, (float) (sr.getScaledHeight() - 9) + y, new Color(potionType.getLiquidColor()).getRGB());
            Fonts.interMedium.get(17.0f).drawStringWithShadow(type, (float) sr.getScaledWidth() - (float) Fonts.interMedium.get(17.0f).getStringWidth(type) - 2.0f, (float) (sr.getScaledHeight() - 9) + y, new Color(255, 255, 255).getRGB());
            GlStateManager.popMatrix();
            y -= 9.5f;
        }
    }

    public boolean beginDrag(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0 || !isToggled()) {
            return false;
        }
        if (!"Cool 2".equalsIgnoreCase(mode.getMode())) {
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

    public ModeSetting getMode() {
        return mode;
    }
}
