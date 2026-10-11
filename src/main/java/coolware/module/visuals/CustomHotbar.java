package coolware.module.visuals;

import coolware.module.Category;
import coolware.module.Module;
import coolware.setting.BooleanSetting;
import coolware.setting.ModeSetting;
import coolware.setting.NumberSetting;
import coolware.util.RenderUtil;
import coolware.util.RoundedUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.input.Keyboard;

import java.awt.Color;

public class CustomHotbar extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", this, "Modern", "Modern", "Rounded", "Clean");
    private final BooleanSetting smoothSlot = new BooleanSetting("Smooth Slot", this, true);
    private final BooleanSetting outline = new BooleanSetting("Outline", this, true);
    private final NumberSetting rounding = new NumberSetting("Rounding", this, 6.0D, 2.0D, 12.0D, 1.0D);

    private float animatedSlotX = -1.0F;
    private long lastTime = System.currentTimeMillis();

    public CustomHotbar() {
        super("CustomHotbar", "Modern custom animated hotbar with rounded aesthetics", Category.VISUALS, Keyboard.KEY_NONE);
        addSetting(mode);
        addSetting(smoothSlot);
        addSetting(outline);
        addSetting(rounding);
        setToggled(true);
    }

    public boolean renderHotbarBackground(ScaledResolution sr, EntityPlayer player) {
        if (!isToggled() || player == null) {
            return false;
        }

        int centerX = sr.getScaledWidth() / 2;
        float barX = centerX - 91.0F;
        float barY = sr.getScaledHeight() - 22.0F;
        float barWidth = 182.0F;
        float barHeight = 22.0F;
        float radius = rounding.getValueFloat();

        long now = System.currentTimeMillis();
        float delta = Math.min(1.0F, (now - lastTime) / 1000.0F);
        lastTime = now;

        int activeHotbarSlot = coolware.module.movement.Scaffold.isItemSpoofing()
                ? coolware.module.movement.Scaffold.getVisualSlot()
                : player.inventory.currentItem;
        float targetSlotX = barX - 1.0F + (activeHotbarSlot * 20.0F);
        if (animatedSlotX < 0 || !smoothSlot.isEnabled()) {
            animatedSlotX = targetSlotX;
        } else {
            animatedSlotX += (targetSlotX - animatedSlotX) * Math.min(1.0F, delta * 18.0F);
        }

        Color themeColor = Theme.getInstance() != null ? new Color(Theme.getInstance().getColor(0)) : new Color(0x7A, 0x8A, 0xF9);
        Color bgColor;
        Color slotColor;
        Color slotOutlineColor;

        String curMode = mode.getMode();
        if ("Rounded".equalsIgnoreCase(curMode)) {
            bgColor = new Color(18, 18, 22, 175);
            slotColor = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 60);
            slotOutlineColor = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 220);
        } else if ("Clean".equalsIgnoreCase(curMode)) {
            bgColor = new Color(10, 10, 14, 150);
            slotColor = new Color(255, 255, 255, 30);
            slotOutlineColor = new Color(255, 255, 255, 120);
        } else {
            // Modern (Default Ambient Style)
            bgColor = new Color(14, 14, 18, 190);
            slotColor = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 45);
            slotOutlineColor = themeColor;
        }

        // 1. Draw hotbar container background
        RoundedUtils.drawRound(barX, barY, barWidth, barHeight, radius, bgColor);

        if (outline.isEnabled()) {
            RenderUtil.drawRoundedOutline(barX, barY, barWidth, barHeight, radius, 1.0F, new Color(255, 255, 255, 25).getRGB());
        }

        // 2. Draw active selected slot pill/indicator
        float selectorRadius = Math.max(3.0F, radius - 1.0F);
        RoundedUtils.drawRound(animatedSlotX, barY - 1.0F, 24.0F, 24.0F, selectorRadius, slotColor);
        if (outline.isEnabled()) {
            RenderUtil.drawRoundedOutline(animatedSlotX, barY - 1.0F, 24.0F, 24.0F, selectorRadius, 1.2F, slotOutlineColor.getRGB());
        }

        return true;
    }

    public ModeSetting getMode() {
        return mode;
    }
}
