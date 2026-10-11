package coolware.module.visuals;

import coolware.config.ConfigManager;
import coolware.font.moonlight.Fonts;
import coolware.module.Category;
import coolware.module.Module;
import coolware.module.combat.KillAura;
import coolware.setting.BooleanSetting;
import coolware.setting.ModeSetting;
import coolware.setting.NumberSetting;
import coolware.util.ColorUtils;
import coolware.util.ParticleRenderer;
import coolware.util.RenderUtil;
import coolware.util.RoundedUtils;
import coolware.util.animation.ContinualAnimation;
import coolware.util.animation.DecelerateAnimation;
import coolware.util.animation.Direction;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.*;

public class TargetHUD extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", this, "Exhi", "Exhi", "Moon");

    private final BooleanSetting particles = new BooleanSetting("Particles", this, true);
    private final NumberSetting posX = new NumberSetting("Position X", this, 200.0D, 0.0D, 4000.0D, 1.0D);
    private final NumberSetting posY = new NumberSetting("Position Y", this, 200.0D, 0.0D, 4000.0D, 1.0D);

    private final Map<EntityPlayer, DecelerateAnimation> animationEntityPlayerMap = new HashMap<EntityPlayer, DecelerateAnimation>();
    private final Map<EntityPlayer, ContinualAnimation> healthAnimationMap = new HashMap<EntityPlayer, ContinualAnimation>();

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

    public TargetHUD() {
        super("TargetHUD", "Displays detailed information and health of current target", Category.VISUALS, Keyboard.KEY_NONE);
        addSetting(mode);
        addSetting(particles);
        addSetting(posX);
        addSetting(posY);
        setToggled(true);
    }

    public ContinualAnimation getHealthAnim(EntityPlayer player) {
        ContinualAnimation anim = healthAnimationMap.get(player);
        if (anim == null) {
            anim = new ContinualAnimation();
            healthAnimationMap.put(player, anim);
        }
        return anim;
    }

    public int getColor(int counter) {
        Theme theme = Theme.getInstance();
        return theme != null ? theme.getColor(counter) : 0xFF55AAFF;
    }

    public int getBgColor() {
        return new Color(21, 21, 24, 180).getRGB();
    }

    public float getTHUDWidth(Entity entity) {
        if (entity == null) return 130.0f;
        String name = entity.getName();
        switch (mode.getMode()) {
            case "Exhi":
                return Math.max(124.0f, Fonts.interBold.get(17).getStringWidth(name) + 54.0f);
            case "Moon":
                return 35 + Fonts.interSemiBold.get(18).getStringWidth(name) + 33;
            default:
                return 130.0f;
        }
    }

    public float getTHUDHeight() {
        switch (mode.getMode()) {
            case "Exhi":
                return 38.0f;
            case "Moon":
                return 40.5f;
            default:
                return 40.0f;
        }
    }

    public void updateTargets() {
        if (mc.theWorld == null || mc.thePlayer == null) {
            animationEntityPlayerMap.clear();
            return;
        }

        EntityPlayer activeTarget = null;
        if (KillAura.target != null && KillAura.target.entity instanceof EntityPlayer) {
            activeTarget = (EntityPlayer) KillAura.target.entity;
        } else if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit instanceof EntityPlayer) {
            activeTarget = (EntityPlayer) mc.objectMouseOver.entityHit;
        }

        if (mc.currentScreen instanceof GuiChat) {
            animationEntityPlayerMap.putIfAbsent(mc.thePlayer, new DecelerateAnimation(175, 1));
            animationEntityPlayerMap.get(mc.thePlayer).setDirection(Direction.FORWARDS);
        } else {
            if (activeTarget != null && !activeTarget.isDead && activeTarget != mc.thePlayer) {
                animationEntityPlayerMap.putIfAbsent(activeTarget, new DecelerateAnimation(175, 1));
                animationEntityPlayerMap.get(activeTarget).setDirection(Direction.FORWARDS);
            }

            Iterator<Map.Entry<EntityPlayer, DecelerateAnimation>> iterator = animationEntityPlayerMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<EntityPlayer, DecelerateAnimation> entry = iterator.next();
                EntityPlayer p = entry.getKey();
                DecelerateAnimation anim = entry.getValue();

                if (p == mc.thePlayer || (activeTarget != null && p != activeTarget) || activeTarget == null || p.isDead) {
                    anim.setDirection(Direction.BACKWARDS);
                    if (anim.finished(Direction.BACKWARDS)) {
                        iterator.remove();
                    }
                }
            }
        }
    }

    @Override
    public void onRender2D() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        updateTargets();

        if (animationEntityPlayerMap.isEmpty()) return;

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

        float baseX = smoothX;
        float baseY = smoothY;
        int count = 0;
        float lastTargetWidth = 0;

        renderedX = baseX;
        renderedY = baseY;
        renderedWidth = 0;
        renderedHeight = getTHUDHeight();

        for (Map.Entry<EntityPlayer, DecelerateAnimation> entry : animationEntityPlayerMap.entrySet()) {
            EntityPlayer target = entry.getKey();
            DecelerateAnimation anim = entry.getValue();
            if (count > 9) break;

            float currentWidth = getTHUDWidth(target);
            float currentHeight = getTHUDHeight();

            float renderX = (float) (baseX + ((count % 3) * (lastTargetWidth + 4)) * anim.getOutput());
            float renderY = (float) (baseY + ((count / 3) * (currentHeight + 4)) * anim.getOutput());

            renderTarget(target, renderX, renderY, currentWidth, currentHeight, anim);

            lastTargetWidth = currentWidth;
            if (renderX + currentWidth - baseX > renderedWidth) {
                renderedWidth = renderX + currentWidth - baseX;
            }
            count++;
        }
    }

    private void renderTarget(EntityPlayer target, float x, float y, float width, float height, DecelerateAnimation animation) {
        if (target == null || target.isDead) return;
        ContinualAnimation healthAnim = getHealthAnim(target);

        GlStateManager.pushMatrix();
        try {
            if (!"Exhi".equals(mode.getMode())) {
                GlStateManager.translate(x + width / 2.0F, y + height / 2.0F, 0);
                GlStateManager.scale(animation.getOutput(), animation.getOutput(), animation.getOutput());
                GlStateManager.translate(-(x + width / 2.0F), -(y + height / 2.0F), 0);
            }

            switch (mode.getMode()) {
                case "Exhi": {
                    float maxHealth = target.getMaxHealth();
                    if (maxHealth <= 0.0f) maxHealth = 20.0f;
                    float health = Math.max(0.0f, target.getHealth());
                    float totalHealth = health + target.getAbsorptionAmount();
                    float progress = MathHelper.clamp_float(health / maxHealth, 0.0f, 1.0f);
                    if ((double) health < 19.5 || health < 10.0f || health < 15.0f || health < 5.0f || health < 2.0f) {
                        progress = Math.max(0.0f, progress - 0.175f);
                    }
                    float healthLocation = 50.0f * progress;
                    Color color = new Color(ColorUtils.getHealthColor(target));
                    GlStateManager.pushMatrix();
                    try {
                        GlStateManager.translate(x, y, 0.0);

                        RenderUtil.drawExhiRect(0, -2, width, height, 1);

                        RenderUtil.drawRect(42.5f, 9.5f, 42.5f + 61.5f, 9.5f + 3.5f, ColorUtils.darker(color.getRGB(), 0.2f));
                        RenderUtil.drawRect(42.5f, 9.5f, 42.5f + 11 + healthLocation, 9.5f + 3.5f, color.getRGB());

                        RenderUtil.drawBorderedRectDim(42.0f, 9.0f, 61.5f, 4.5f, 0.5f, new Color(0, 0, 0, 0).getRGB(), new Color(0, 0, 0).getRGB());
                        for (int i = 1; i < 10; ++i) {
                            float separator = 5.882353f * i;
                            RenderUtil.drawRect(43.5f + separator, 9.0f, 43.5f + separator + 0.5f, 9.0f + 4.5f, new Color(0, 0, 0).getRGB());
                        }
                        Fonts.interBold.get(17).drawString(target.getName(), 42.0f, 1.0f, -1);
                        int dist = mc.thePlayer != null ? (int) mc.thePlayer.getDistanceToEntity(target) : 0;
                        Fonts.interRegular.get(10, false).drawString("HP: " + (int) totalHealth + " | Dist: " + dist, 42.5f, 15.5f, -1);

                        List<ItemStack> exItems = new ArrayList<ItemStack>();
                        if (target.getHeldItem() != null) exItems.add(target.getHeldItem());
                        if (target.inventory != null && target.inventory.armorInventory != null) {
                            for (int index = 3; index >= 0; index--) {
                                ItemStack stack = target.inventory.armorInventory[index];
                                if (stack != null) exItems.add(stack);
                            }
                        }
                        float exX = 0;
                        for (ItemStack stack : exItems) {
                            RenderUtil.renderItemStack(stack, exX + 28 + 16, 19, 1.0f, true, 0.5f);
                            exX += 16;
                        }

                        GlStateManager.scale(0.31, 0.31, 0.31);
                        GlStateManager.translate(73.0f, 102.0f, 40.0f);
                        RenderUtil.drawEntityOnScreen(target.rotationYaw, target.rotationPitch, target);
                    } finally {
                        GlStateManager.popMatrix();
                    }
                    break;
                }

                case "Moon": {
                    float maxHealth = target.getMaxHealth();
                    if (maxHealth <= 0.0f) maxHealth = 20.0f;
                    float healthPercentage = MathHelper.clamp_float(target.getHealth() / maxHealth, 0.0f, 1.0f);
                    float space = (width - 48) / 100;
                    healthAnim.animate((100 * space) * healthPercentage, 30);

                    RoundedUtils.drawRound(x, y, width, height, 8, new Color(getBgColor(), true));
                    RoundedUtils.drawRound(x + 42, y + 26.5f, (100 * space), 8, 4, new Color(0, 0, 0, 150));
                    String text = String.format("%.1f", Math.max(0.0f, target.getHealth()));

                    RoundedUtils.drawRound(x + 42, y + 26.5f, healthAnim.getOutput(), 8.5f, 4, new Color(getColor(0)));
                    RenderUtil.renderPlayer2D(target, x + 2.5f, y + 2.5f, 35, 10, -1);
                    Fonts.interSemiBold.get(13).drawStringWithShadow(text + "HP", x + 40, y + 17, -1);
                    Fonts.interSemiBold.get(18).drawStringWithShadow(target.getName(), x + 40, y + 6, -1);

                    if (particles.isEnabled()) {
                        ParticleRenderer.renderParticle(target, x + 2.5f, y + 2.5f);
                    }
                    break;
                }
            }
        } catch (Throwable t) {
            // Guard against any rendering anomaly with external target entity
        } finally {
            GlStateManager.popMatrix();
        }
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

    public ModeSetting getMode() {
        return mode;
    }
}
