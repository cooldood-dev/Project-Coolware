package com.github.cooldood.modules.impl.render;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.PacketEvent;
import com.github.cooldood.modules.*;
import com.github.cooldood.modules.impl.client.ThemeModule;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.minecraft.TargetUtil;
import com.github.cooldood.utils.render.EasingUtil;
import com.github.cooldood.utils.render.FontUtil;
import com.github.cooldood.utils.render.IconFont;
import com.github.cooldood.utils.render.RenderUtil;
import com.github.cooldood.utils.render.draggable.Draggable;
import com.github.cooldood.utils.tenacity.animations.ContinualAnimation;
import lombok.AllArgsConstructor;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.util.MathHelper;

import java.awt.Color;

@RegisterModule(
        name = "Target HUD",
        description = "Provides Target HUD functionality for the client.",
        category = Category.RENDER
)
public class TargetHUD extends Module {

    public static final ContinualAnimation healthAnimation = new ContinualAnimation();

    @RegisterSubModule(name = "Target Time", description = "Time before the module forgets its target", max = 5000, increment = 50)
    public static long targetTimeBeforeDementiaKicksIn = 250;

    @RegisterSubModule(name = "Mode")
    public static TargetHUDMode hudMode = TargetHUDMode.Mode_1;

    public enum TargetHUDMode {
        Mode_1,
        Mode_2
    }

    @RegisterSubModule(name = "Animation In")
    public static SubCategory animationIn = new SubCategory();

    @RegisterSubModule(name = "Ease In", parent = "Animation In")
    public static EasingUtil.EasingFunctions easeInFunction = EasingUtil.EasingFunctions.Ease_In_Out_Expo;

    @RegisterSubModule(name = "Pop In Time", description = "Time for the pop-in effect to finish", max = 1000, increment = 50, parent = "Animation In")
    public static long popInTime = 250;

    @RegisterSubModule(name = "Animation Out")
    public static SubCategory animationOut = new SubCategory();

    @RegisterSubModule(name = "Ease Out", parent = "Animation Out")
    public static EasingUtil.EasingFunctions easeOutFunction = EasingUtil.EasingFunctions.Ease_In_Out_Expo;

    @RegisterSubModule(name = "Pop Out Time", description = "Time for the pop-out effect to finish", max = 1000, increment = 50, parent = "Animation Out")
    public static long popOutTime = 250;

    private static Target target;

    // ── Fight Status Tracking ─────────────────────────────────────────────────
    private static float trackedPlayerHealth = -1f;
    private static float trackedTargetHealth = -1f;
    private static float playerHealthDelta   = 0f;
    private static float targetHealthDelta   = 0f;
    private static long  lastHealthSnapshot  = 0L;
    private static final long SNAPSHOT_INTERVAL = 1500L;

    // ── Mode 2 — extra animation state ────────────────────────────────────────
    private static float m2AnimatedWidth   = 0f;   // smooth width expand
    private static float m2PrevHealth      = -1f;  // for damage flash
    private static float m2DamageFlash     = 0f;   // 0→1 flash value
    private static long  m2LastDamageTime  = 0L;

    @AllArgsConstructor
    private static class Target {
        public EntityLivingBase entity;
        public long lastInteract;
    }

    @SubscribeEvent
    public static void registerTarget(PacketEvent.Send event) {
        if (C.mc.currentScreen instanceof GuiChat) {
            updateTarget(C.p());
            return;
        }

        if (!(event.packet instanceof C02PacketUseEntity)) return;

        C02PacketUseEntity attackPacket = (C02PacketUseEntity) event.packet;
        if (attackPacket.getAction() != C02PacketUseEntity.Action.ATTACK) return;

        Entity newTarget = attackPacket.getEntityFromWorld(C.w());
        if (!(newTarget instanceof EntityLivingBase) || !TargetUtil.isValidTarget(newTarget, true)) return;

        updateTarget((EntityLivingBase) newTarget);
    }

    // ── Mode 1 Layout Constants (Flat, Dark, ClickGUI Style) ─────────────────
    private static final Color BG_COLOR    = new Color(13, 13, 21);
    private static final float PAD         = 6f;
    private static final float INNER_PAD   = 6f;
    private static final float HEAD_SIZE   = 32f;
    private static final float BAR_H       = 3.5f;
    private static final int   NAME_SIZE   = 13;
    private static final int   INFO_SIZE   = 10;

    public static Draggable targetHUD = new Draggable(
            "targetHUD",
            () -> {
                double easingIn  = EasingUtil.getAnimation("thIn");
                double easingOut = EasingUtil.getAnimation("thOut");

                if (target == null) return new double[] {0, 0};

                if (target.lastInteract + popInTime + targetTimeBeforeDementiaKicksIn + popOutTime <= System.currentTimeMillis()) {
                    target = null;
                    return new double[] {0, 0};
                } else if (target.lastInteract + popInTime + targetTimeBeforeDementiaKicksIn <= System.currentTimeMillis() && easingOut == -1) {
                    EasingUtil.addAnimation("thOut", popOutTime, true, easeOutFunction);
                }

                EntityLivingBase entity = target.entity;

                if (hudMode == TargetHUDMode.Mode_2) {
                    return renderMode2(entity, easingIn, easingOut);
                }

                // ─────────────────────────────────────────────────────────────
                // MODE 1 — original, completely unchanged
                // ─────────────────────────────────────────────────────────────

                // ── Data & Strings ────────────────────────────────────────────
                float healthRaw    = entity.getHealth();
                float maxHealth    = Math.max(entity.getMaxHealth(), 1f);
                float healthNumber = Math.round(healthRaw * 10.0f) / 10.0f;

                double distRaw     = TargetUtil.getDistanceToEntity(entity);
                float distNumber   = Math.round(distRaw * 10.0) / 10.0f;

                String name        = entity.getName();
                String healthStr   = String.valueOf(healthNumber);
                String distStr     = String.valueOf(distNumber);
                String infoPrefix  = healthStr + "  -  " + distStr + " ";

                // ── Fight Status (winning / losing) ───────────────────────────
                float playerHealth = C.p() != null ? C.p().getHealth() : -1f;
                long  now          = System.currentTimeMillis();
                if (lastHealthSnapshot == 0L) {
                    trackedPlayerHealth = playerHealth;
                    trackedTargetHealth = healthRaw;
                    lastHealthSnapshot  = now;
                } else if (now - lastHealthSnapshot >= SNAPSHOT_INTERVAL) {
                    playerHealthDelta   = playerHealth - trackedPlayerHealth;
                    targetHealthDelta   = healthRaw    - trackedTargetHealth;
                    trackedPlayerHealth = playerHealth;
                    trackedTargetHealth = healthRaw;
                    lastHealthSnapshot  = now;
                }

                String statusLabel;
                Color  statusColorRGB;
                float  THRESHOLD = 0.5f;
                if (playerHealth <= 0f || trackedPlayerHealth < 0f) {
                    statusLabel    = "";
                    statusColorRGB = new Color(0, 0, 0, 0);
                } else if (targetHealthDelta < -THRESHOLD && playerHealthDelta >= targetHealthDelta) {
                    statusLabel    = "WINNING";
                    statusColorRGB = new Color(80, 210, 120);
                } else if (playerHealthDelta < -THRESHOLD && playerHealthDelta < targetHealthDelta) {
                    statusLabel    = "LOSING";
                    statusColorRGB = new Color(220, 70, 70);
                } else {
                    statusLabel    = "EVEN";
                    statusColorRGB = new Color(160, 160, 175);
                }

                // ── Panel sizing ──────────────────────────────────────────────
                float nameW     = FontUtil.getStringWidth(name, NAME_SIZE);
                float infoTextW = FontUtil.getStringWidth(infoPrefix, INFO_SIZE);
                float heartW    = IconFont.getWidth(IconFont.HEART, INFO_SIZE);
                float infoW     = infoTextW + heartW;
                float statusW   = statusLabel.isEmpty() ? 0f : FontUtil.getStringWidth(statusLabel, INFO_SIZE) + 4f;
                float nameRowW  = nameW + (statusLabel.isEmpty() ? 0f : 8f + statusW);
                float textColW  = Math.max(nameRowW, infoW);
                float panelW    = Math.max(PAD + HEAD_SIZE + INNER_PAD + textColW + PAD, 130f);

                float nameH     = FontUtil.getFontHeight(NAME_SIZE);
                float infoH     = FontUtil.getFontHeight(INFO_SIZE);
                float textColH  = nameH + 2f + BAR_H + 2f + infoH;
                float panelH    = Math.max(HEAD_SIZE + PAD * 2f, textColH + PAD * 2f);

                // ── Slide animation ───────────────────────────────────────────
                double xIn  = easingIn  == -1 ? 0 : (-C.res().getScaledWidth() - panelW) * (1.0 - easingIn);
                double xOut = easingOut == -1 ? 0 : ( C.res().getScaledWidth() + panelW) * easingOut;

                float x = (float)(xIn + xOut);
                float y = 0f;

                float alpha;
                if      (easingOut != -1) alpha = (float)(1.0 - easingOut);
                else if (easingIn  != -1) alpha = (float) easingIn;
                else                      alpha = 1f;
                alpha    = MathHelper.clamp_float(alpha, 0f, 1f);
                int aInt = (int)(alpha * 255);

                Color accent = ThemeModule.primaryColor;

                // ── Background ────────────────────────────────────────────────
                RenderUtil.drawRect(x, y, panelW, panelH,
                        new Color(BG_COLOR.getRed(), BG_COLOR.getGreen(), BG_COLOR.getBlue(),
                                (int)(210 * alpha)));

                RenderUtil.drawRect(x, y + panelH - 1f, panelW, 1f,
                        new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                                (int)(180 * alpha)));

                // ── Player head ───────────────────────────────────────────────
                float headX = x + PAD;
                float headY = y + (panelH - HEAD_SIZE) / 2f;
                RenderUtil.drawPlayerHead(headX, headY, HEAD_SIZE, HEAD_SIZE,
                        new Color(255, 255, 255, aInt), entity);

                // ── Text column ───────────────────────────────────────────────
                float colX      = x + PAD + HEAD_SIZE + INNER_PAD;
                float blockTopY = y + (panelH - textColH) / 2f;

                FontUtil.drawString(name, colX, blockTopY, NAME_SIZE,
                        new Color(230, 230, 235, aInt), true);

                if (!statusLabel.isEmpty()) {
                    float labelW  = FontUtil.getStringWidth(statusLabel, INFO_SIZE);
                    float labelX  = x + panelW - PAD - labelW;
                    float labelY  = blockTopY + (nameH - FontUtil.getFontHeight(INFO_SIZE)) / 2f;
                    Color labelC  = new Color(statusColorRGB.getRed(), statusColorRGB.getGreen(),
                                             statusColorRGB.getBlue(), aInt);
                    FontUtil.drawString(statusLabel, labelX, labelY, INFO_SIZE, labelC, true);
                }

                // ── Health bar ────────────────────────────────────────────────
                float barY = blockTopY + nameH + 2f;
                float barW = panelW - PAD - HEAD_SIZE - INNER_PAD - PAD;

                RenderUtil.drawRect(colX, barY, barW, BAR_H,
                        new Color(30, 30, 42, (int)(160 * alpha)));

                healthAnimation.animate(
                        MathHelper.clamp_float(healthRaw / maxHealth, 0f, 1f),
                        18
                );
                float animatedFraction = MathHelper.clamp_float(
                        healthAnimation.getOutput(), 0f, 1f
                );

                float fillW = barW * animatedFraction;
                if (fillW > 0.5f) {
                    RenderUtil.drawRect(colX, barY, fillW, BAR_H,
                            new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), aInt));
                }

                // ── Info text ────────────────────────────────────────────────
                float infoY = barY + BAR_H + 2f;
                FontUtil.drawString(infoPrefix, colX, infoY, INFO_SIZE,
                        new Color(155, 155, 175, aInt), true);

                float heartIconH = IconFont.getHeight(INFO_SIZE);
                float heartIconY = infoY + (infoH - heartIconH) / 2f;
                IconFont.drawIcon(IconFont.HEART, colX + infoTextW, heartIconY, INFO_SIZE,
                        new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), aInt));

                return new double[]{panelW, panelH};
            },
            e -> {
                if (C.mc.currentScreen instanceof GuiChat) {
                    updateTarget(C.p());
                }
                return ModuleManager.isEnabled(TargetHUD.class) && target != null;
            },
            e -> true
    );

    // ─────────────────────────────────────────────────────────────────────────
    // MODE 2 — Glassmorphic Dynamic Island TargetHUD
    // ─────────────────────────────────────────────────────────────────────────
    private static final float M2_PAD_X     = 10f;
    private static final float M2_PAD_Y     = 8f;
    private static final float M2_HEAD      = 28f;   // head avatar size
    private static final float M2_HEAD_GAP  = 8f;    // gap between head and text col
    private static final float M2_NAME_SZ   = 12;
    private static final float M2_INFO_SZ   = 9;
    private static final float M2_BAR_H     = 2f;    // thin modern health line
    private static final float M2_BAR_RADIUS= 1f;

    private static double[] renderMode2(EntityLivingBase entity, double easingIn, double easingOut) {
        // ── Data ─────────────────────────────────────────────────────────────
        float healthRaw    = entity.getHealth();
        float maxHealth    = Math.max(entity.getMaxHealth(), 1f);
        float healthFrac   = MathHelper.clamp_float(healthRaw / maxHealth, 0f, 1f);
        float healthRounded= Math.round(healthRaw * 10.0f) / 10.0f;

        double distRaw     = TargetUtil.getDistanceToEntity(entity);
        float  distRounded = Math.round(distRaw * 10.0) / 10.0f;

        String name        = entity.getName();
        String healthStr   = healthRounded + " HP";
        String distStr     = distRounded + "m";

        // ── Damage flash ─────────────────────────────────────────────────────
        if (m2PrevHealth >= 0f && healthRaw < m2PrevHealth - 0.05f) {
            m2DamageFlash   = 1f;
            m2LastDamageTime = System.currentTimeMillis();
        }
        m2PrevHealth = healthRaw;

        long flashElapsed = System.currentTimeMillis() - m2LastDamageTime;
        if (m2DamageFlash > 0f) {
            m2DamageFlash = Math.max(0f, 1f - (flashElapsed / 400f));
        }

        // ── Health animation ─────────────────────────────────────────────────
        healthAnimation.animate(healthFrac, 14);
        float animFrac = MathHelper.clamp_float(healthAnimation.getOutput(), 0f, 1f);

        // ── Sizing ───────────────────────────────────────────────────────────
        int nameSz = (int) M2_NAME_SZ;
        int infoSz = (int) M2_INFO_SZ;

        float nameW  = FontUtil.getStringWidth(name, nameSz);
        float hpW    = FontUtil.getStringWidth(healthStr, infoSz);
        float distW  = FontUtil.getStringWidth(distStr, infoSz);
        float sepW   = FontUtil.getStringWidth("  ·  ", infoSz);
        float infoW  = hpW + sepW + distW;

        float textColW  = Math.max(nameW, infoW);
        float nameH     = FontUtil.getFontHeight(nameSz);
        float infoH     = FontUtil.getFontHeight(infoSz);
        // text block: name + 3px gap + bar + 3px gap + info
        float textBlockH = nameH + 3f + M2_BAR_H + 3f + infoH;

        float islandH    = Math.max(M2_HEAD + M2_PAD_Y * 2f, textBlockH + M2_PAD_Y * 2f);
        float pillRadius = islandH / 2f;

        // Target content width (head + gap + text)
        float contentW   = M2_HEAD + M2_HEAD_GAP + textColW;
        float targetW    = M2_PAD_X + contentW + M2_PAD_X;
        targetW          = Math.max(targetW, 140f);

        // Smooth expand width
        if (m2AnimatedWidth < 1f) m2AnimatedWidth = targetW * 0.3f;
        float wDiff = targetW - m2AnimatedWidth;
        m2AnimatedWidth += wDiff * 0.18f;
        if (Math.abs(wDiff) < 0.3f) m2AnimatedWidth = targetW;
        float islandW = m2AnimatedWidth;

        // ── Alpha / slide ─────────────────────────────────────────────────────
        float alpha;
        if      (easingOut != -1) alpha = (float)(1.0 - easingOut);
        else if (easingIn  != -1) alpha = (float) easingIn;
        else                      alpha = 1f;
        alpha    = MathHelper.clamp_float(alpha, 0f, 1f);
        int aInt = (int)(alpha * 255);

        // Slide from top (y) — pill drops in from above
        float slideY = easingIn != -1 ? (float)(-(islandH + 8) * (1.0 - easingIn)) : 0f;
        if (easingOut != -1) slideY = (float)(-(islandH + 8) * easingOut);

        float x = 0f;
        float y = slideY;

        // ── Accent & colors ───────────────────────────────────────────────────
        Color accent = ThemeModule.primaryColor;

        // ── Background layers ─────────────────────────────────────────────────
        // 1. ambient drop shadow
        RenderUtil.drawRoundedRect(x - 2f, y + 2f, islandW + 4f, islandH + 4f,
                pillRadius + 2f, new Color(0, 0, 0, (int)(50 * alpha)));
        // 2. subtle outer border ring
        RenderUtil.drawRoundedRect(x - 0.5f, y - 0.5f, islandW + 1f, islandH + 1f,
                pillRadius + 0.5f, new Color(255, 255, 255, (int)(20 * alpha)));
        // 3. main dark glass body — damage flash tints it slightly red
        int flashR = (int)(12 + 60 * m2DamageFlash);
        int flashG = (int)(12 - 6  * m2DamageFlash);
        int flashB = (int)(15 - 6  * m2DamageFlash);
        RenderUtil.drawRoundedRect(x, y, islandW, islandH,
                pillRadius, new Color(flashR, flashG, flashB, (int)(228 * alpha)));
        // 4. top inner sheen
        RenderUtil.drawRoundedRect(x + pillRadius, y + 0.8f, islandW - pillRadius * 2f, 0.7f,
                0.4f, new Color(255, 255, 255, (int)(28 * alpha)));

        // ── Player head — rounded square inset on left ────────────────────────
        float headX = x + M2_PAD_X;
        float headY = y + (islandH - M2_HEAD) / 2f;
        // Soft shadow under head
        RenderUtil.drawRoundedRect(headX - 0.5f, headY + 1.5f, M2_HEAD + 1f, M2_HEAD + 1f,
                4f, new Color(0, 0, 0, (int)(60 * alpha)));
        // Head render (RenderUtil clips it internally)
        RenderUtil.drawPlayerHead(headX, headY, M2_HEAD, M2_HEAD,
                new Color(255, 255, 255, aInt), entity);

        // ── Text column ───────────────────────────────────────────────────────
        float colX      = headX + M2_HEAD + M2_HEAD_GAP;
        float blockTopY = y + (islandH - textBlockH) / 2f;

        // Player name — white, medium weight
        FontUtil.drawString(name, colX, blockTopY, nameSz,
                new Color(238, 238, 242, aInt), true);

        // ── Thin health progress line ─────────────────────────────────────────
        float barY = blockTopY + nameH + 3f;
        float barW = textColW;

        // Track — very dark
        RenderUtil.drawRoundedRect(colX, barY, barW, M2_BAR_H,
                M2_BAR_RADIUS, new Color(255, 255, 255, (int)(22 * alpha)));

        // Glowing fill — accent color, with damage flash
        float fillW = barW * animFrac;
        if (fillW > 0.5f) {
            // Base fill
            int fr = accent.getRed()   + (int)((255 - accent.getRed())   * m2DamageFlash * 0.6f);
            int fg = accent.getGreen() - (int)(accent.getGreen()          * m2DamageFlash * 0.5f);
            int fb = accent.getBlue()  - (int)(accent.getBlue()           * m2DamageFlash * 0.4f);
            fr = MathHelper.clamp_int(fr, 0, 255);
            fg = MathHelper.clamp_int(fg, 0, 255);
            fb = MathHelper.clamp_int(fb, 0, 255);
            RenderUtil.drawRoundedRect(colX, barY, fillW, M2_BAR_H,
                    M2_BAR_RADIUS, new Color(fr, fg, fb, aInt));

            // Subtle glow cap at end of fill
            if (fillW > 2f) {
                RenderUtil.drawRoundedRect(colX + fillW - 2f, barY - 0.5f, 3f, M2_BAR_H + 1f,
                        M2_BAR_RADIUS, new Color(fr, fg, fb, (int)(100 * alpha)));
            }
        }

        // ── Info row: HP · distance ───────────────────────────────────────────
        float infoY   = barY + M2_BAR_H + 3f;
        Color muted   = new Color(150, 150, 162, aInt);
        Color hpColor;
        if (healthFrac > 0.6f)      hpColor = new Color(130, 230, 150, aInt);
        else if (healthFrac > 0.3f) hpColor = new Color(230, 200, 80,  aInt);
        else                        hpColor = new Color(230, 90,  80,  aInt);

        FontUtil.drawString(healthStr, colX, infoY, infoSz, hpColor, false);
        FontUtil.drawString("  ·  ", colX + hpW, infoY, infoSz, muted, false);
        FontUtil.drawString(distStr, colX + hpW + sepW, infoY, infoSz, muted, false);

        return new double[]{islandW, islandH};
    }

    public static void updateTarget(EntityLivingBase entity) {
        double easingIn = EasingUtil.getAnimation("thIn");
        if (target == null && easingIn == -1) {
            EasingUtil.addAnimation("thIn", popInTime, false, easeInFunction);
            healthAnimation.animate(MathHelper.clamp_float(entity.getHealth() / entity.getMaxHealth(), 0f, 1f), 18);
            // Reset Mode 2 expand animation for the new target
            m2AnimatedWidth  = 0f;
            m2PrevHealth     = entity.getHealth();
            m2DamageFlash    = 0f;
        }

        target = new Target(entity, System.currentTimeMillis());
        trackedPlayerHealth = -1f;
        trackedTargetHealth = -1f;
        playerHealthDelta   = 0f;
        targetHealthDelta   = 0f;
        lastHealthSnapshot  = 0L;
    }

    @Override
    protected void onEnable() {
        target = null;
    }

    @Override
    protected void onDisable() {
        target = null;
    }
}
