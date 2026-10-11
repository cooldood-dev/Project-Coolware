package coolware.module.visuals;

import coolware.module.Category;
import coolware.module.Module;
import coolware.module.visuals.esp.ESPColor;
import coolware.module.visuals.esp.PlayerChams;
import coolware.module.visuals.esp.PlayerGlow;
import coolware.setting.BooleanSetting;
import coolware.setting.NumberSetting;
import coolware.util.RenderUtil;
import coolware.util.render.ColorUtil;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScorePlayerTeam;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class ESP extends Module {
    public final BooleanSetting box3d = new BooleanSetting("3D Box", this, true);
    public final BooleanSetting boxFilled = new BooleanSetting("Filled Box", this, true);
    public final NumberSetting boxAlpha = new NumberSetting("Box Alpha", this, 40.0D, 0.0D, 255.0D, 1.0D);
    public final NumberSetting outlineWidth = new NumberSetting("Outline Width", this, 1.5D, 0.5D, 5.0D, 0.1D);

    public final BooleanSetting glow = new BooleanSetting("Glow", this, false);
    public final BooleanSetting chams = new BooleanSetting("Chams", this, false);
    public final BooleanSetting staticColor = new BooleanSetting("Static Color", this, false);

    public final BooleanSetting lobbyPlayers = new BooleanSetting("Lobby Players", this, true);
    public final BooleanSetting botCheck = new BooleanSetting("Anti Bot", this, true);
    public final BooleanSetting showBots = new BooleanSetting("Show Bots", this, false);

    public final BooleanSetting nametags = new BooleanSetting("Nametags", this, true);
    public final NumberSetting nametagsScale = new NumberSetting("Nametag Scale", this, 1.0D, 0.5D, 2.0D, 0.1D);
    public final NumberSetting nametagAlpha = new NumberSetting("Nametag Alpha", this, 135.0D, 20.0D, 255.0D, 5.0D);
    public final BooleanSetting nametagsHealthBar = new BooleanSetting("Health Bar", this, true);

    private final ESPColor espColor = new ESPColor(new Color(0xFFFF3344), Color.RED, new Color(0xFFFF3344));
    private final PlayerChams playerChams = new PlayerChams(espColor);
    private final PlayerGlow playerGlow = new PlayerGlow(espColor);

    public ESP() {
        super("ESP", "Highlights players and entities with 3D boxes, shaders, and custom glassmorphic nametags", Category.VISUALS, Keyboard.KEY_NONE);
        addSetting(box3d);
        addSetting(boxFilled);
        addSetting(boxAlpha);
        addSetting(outlineWidth);
        addSetting(nametags);
        addSetting(nametagsScale);
        addSetting(nametagAlpha);
        addSetting(nametagsHealthBar);
        addSetting(glow);
        addSetting(chams);
        addSetting(staticColor);
        addSetting(lobbyPlayers);
        addSetting(botCheck);
        addSetting(showBots);
    }

    private void updateColor() {
        Color primary = new Color(Theme.getInstance().getColor(0));
        espColor.setNormalColor(primary);
        espColor.setTargetColor(primary);
        espColor.setHurtColor(Color.RED);
    }

    /**
     * Determines whether a player entity is an NPC or AntiCheat bot (e.g. Hypixel Watchdog).
     */
    public boolean isBot(EntityPlayer player) {
        if (player == null || player == mc.thePlayer) {
            return false;
        }

        if (mc.isSingleplayer()) {
            return false;
        }

        // Invisible players orbiting close overhead with scrambled or special names are classic Watchdog bots
        if (player.isInvisible() && player.posY > mc.thePlayer.posY + 1.2 && Math.abs(player.posX - mc.thePlayer.posX) < 8.0 && Math.abs(player.posZ - mc.thePlayer.posZ) < 8.0) {
            return true;
        }

        if (player.getName().startsWith("§k")) {
            return true;
        }

        // Tablist player info check
        if (mc.getNetHandler() != null) {
            NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(player.getUniqueID());
            if (info == null) {
                info = mc.getNetHandler().getPlayerInfo(player.getName());
            }

            if (info == null) {
                // If not in tab list, typical NPC/bot behavior
                return true;
            }

            // Bots often have negative or zero ping response
            if (info.getResponseTime() < 0) {
                return true;
            }

            ScorePlayerTeam team = info.getPlayerTeam();
            if (team != null && team.getTeamName().isEmpty() && "§c".equals(team.getColorPrefix())) {
                return true;
            }
        } else if (player instanceof AbstractClientPlayer) {
            NetworkPlayerInfo info = ((AbstractClientPlayer) player).getPlayerInfo();
            if (info == null || info.getResponseTime() < 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a player should be rendered by ESP.
     */
    public boolean shouldRenderPlayer(EntityPlayer player) {
        if (player == null || player.isDead) {
            return false;
        }

        if (player == mc.thePlayer) {
            return mc.gameSettings.thirdPersonView != 0;
        }

        if (botCheck.isEnabled() && isBot(player)) {
            return showBots.isEnabled();
        }

        return true;
    }

    public List<EntityPlayer> getValidPlayers() {
        List<EntityPlayer> list = new ArrayList<EntityPlayer>();
        if (mc.theWorld == null) return list;

        // Collect all players (including lobby players from world entity list)
        if (lobbyPlayers.isEnabled()) {
            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (entity instanceof EntityPlayer) {
                    EntityPlayer player = (EntityPlayer) entity;
                    if (shouldRenderPlayer(player) && !list.contains(player)) {
                        list.add(player);
                    }
                }
            }
        } else {
            for (EntityPlayer player : mc.theWorld.playerEntities) {
                if (shouldRenderPlayer(player) && !list.contains(player)) {
                    list.add(player);
                }
            }
        }

        return list;
    }

    public Color getPlayerColor(EntityPlayer player) {
        if (player == null) return espColor.getColor();
        if (player.hurtTime > 0) return Color.RED;
        if (isBot(player)) return new Color(0xFFFF9900, true);
        return espColor.getColor();
    }

    @Override
    public void onRender3D(float partialTicks) {
        if (!isToggled() || mc.theWorld == null || mc.thePlayer == null || mc.getRenderManager() == null) return;

        updateColor();

        List<EntityPlayer> players = getValidPlayers();
        if (players.isEmpty()) return;

        // 1. Draw 3D bounding boxes around all detected players
        if (box3d.isEnabled()) {
            float width = (float) outlineWidth.getValue();
            int fillA = (int) boxAlpha.getValue();

            for (EntityPlayer player : players) {
                Color baseColor = getPlayerColor(player);
                Color outline = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 255);
                Color fill = boxFilled.isEnabled()
                        ? new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), Math.max(0, Math.min(255, fillA)))
                        : new Color(0, 0, 0, 0);

                RenderUtil.drawEntity3DBox(player, partialTicks, outline, fill, width);
            }
        }

        // 2. Chams pass
        if (chams.isEnabled()) {
            playerChams.render3D(partialTicks, staticColor.isEnabled());
        }

        // 3. Glow shader pass
        if (glow.isEnabled()) {
            playerGlow.render3D(partialTicks);
        }

        // 4. Custom glassmorphic pill nametags pass
        if (nametags.isEnabled()) {
            for (EntityPlayer player : players) {
                renderNametagCard(player, partialTicks);
            }
        }
    }

    private void renderNametagCard(EntityPlayer player, float partialTicks) {
        if (player == null || mc.getRenderManager() == null) return;

        double renderPosX = mc.getRenderManager().renderPosX;
        double renderPosY = mc.getRenderManager().renderPosY;
        double renderPosZ = mc.getRenderManager().renderPosZ;

        double pX = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double) partialTicks - renderPosX;
        double pY = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double) partialTicks - renderPosY;
        double pZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double) partialTicks - renderPosZ;

        // Position slightly above player head (or adjusted for sneak)
        double tagY = pY + player.height + 0.48D;
        if (player.isSneaking()) {
            tagY -= 0.2D;
        }

        // Calculate distance from render camera for scaling
        Entity viewer = mc.getRenderViewEntity() != null ? mc.getRenderViewEntity() : mc.thePlayer;
        double dist = viewer != null ? viewer.getDistance(player.posX, player.posY, player.posZ) : 10.0D;
        dist = Math.max(1.0D, dist);

        // Smooth distance scaling so distant tags remain legible
        float scale = (float) (0.016D + 0.0022D * dist);
        scale *= (float) nametagsScale.getValue();
        scale = Math.min(0.28F, Math.max(0.015F, scale));

        // Format name and health string
        String name = player.getGameProfile() != null && player.getGameProfile().getName() != null
                ? player.getGameProfile().getName()
                : player.getName();
        if (name == null || name.isEmpty()) {
            name = "Player";
        }

        float health = Math.max(0.0F, player.getHealth() + player.getAbsorptionAmount());
        float maxHealth = Math.max(1.0F, player.getMaxHealth() + player.getAbsorptionAmount());
        float healthPct = Math.min(1.0F, Math.max(0.0F, health / maxHealth));

        String healthStr;
        if (health % 1.0F == 0.0F) {
            healthStr = String.valueOf((int) health);
        } else {
            healthStr = String.format(java.util.Locale.US, "%.1f", health);
        }

        net.minecraft.client.gui.FontRenderer mcFont = mc.fontRendererObj;

        int nameW = mcFont.getStringWidth(name);
        int healthW = mcFont.getStringWidth(healthStr);

        // Generous pill layout metrics so text never spills out or feels cramped
        float pillPadH = 8.0F; // Generous horizontal breathing room inside pill ends
        float gap = 6.0F;
        float contentW = (float) (nameW + gap + healthW);
        float pillW = contentW + pillPadH * 2.0F;
        float pillH = 17.0F;
        float pillRadius = pillH / 2.0F; // Symmetrical circular pill ends

        float drawX = -pillW / 2.0F;
        float drawY = -pillH / 2.0F;

        // Health-based color coding
        int healthColor;
        if (healthPct > 0.65F) {
            healthColor = 0xFF55FF55; // Crisp Minecraft bright green
        } else if (healthPct > 0.35F) {
            healthColor = 0xFFFFAA00; // Warning gold/amber
        } else {
            healthColor = 0xFFFF5555; // Danger bright red
        }

        net.minecraft.client.renderer.GlStateManager.pushMatrix();
        net.minecraft.client.renderer.GlStateManager.translate((float) pX, (float) tagY, (float) pZ);
        net.minecraft.client.renderer.GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        net.minecraft.client.renderer.GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        net.minecraft.client.renderer.GlStateManager.scale(-scale, -scale, scale);

        net.minecraft.client.renderer.GlStateManager.disableLighting();
        net.minecraft.client.renderer.GlStateManager.depthMask(false);
        net.minecraft.client.renderer.GlStateManager.disableDepth();
        net.minecraft.client.renderer.GlStateManager.enableBlend();
        net.minecraft.client.renderer.GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        // 1. Soft Glass Contact Drop Shadows (Deep, realistic ambient shadow)
        RenderUtil.drawSoftGlow(drawX, drawY + 1.2F, pillW, pillH, pillRadius, 0x55000000, 3);
        RenderUtil.drawSoftGlow(drawX, drawY + 0.6F, pillW, pillH, pillRadius, 0x77000000, 2);

        // 2. Translucent Glassmorphic Acrylic Backplate (True neutral translucent dark glass, NOT opaque black)
        int alphaVal = (int) Math.max(20.0D, Math.min(255.0D, nametagAlpha.getValue()));
        int bgTop = (alphaVal << 24) | 0x0016161D; // Translucent frosted glass obsidian
        int bgBot = (Math.min(255, (int)(alphaVal * 1.15F)) << 24) | 0x000E0E14;
        RenderUtil.drawRoundedGradient(drawX, drawY, pillW, pillH, pillRadius, bgTop, bgBot);

        // 3. Frosted Glass Specular Rim Highlight (Clean neutral border sheen)
        RenderUtil.drawRoundedOutline(drawX, drawY, pillW, pillH, pillRadius, 1.0F, 0x30FFFFFF);

        // 4. Subtle Micro Health Underline / Indicator (if enabled)
        if (nametagsHealthBar.isEnabled()) {
            float barW = Math.max(2.0F, (pillW - pillPadH * 2.0F) * healthPct);
            float barX = drawX + pillPadH;
            float barY = drawY + pillH - 2.5F;
            RenderUtil.drawRoundedRect(barX, barY, barW, 1.2F, 0.6F, (healthColor & 0x00FFFFFF) | 0xCC000000);
        }

        // 5. Crystal-sharp Text Rendering (Rendered with Minecraft FontRenderer with drop shadow, ZERO blur)
        net.minecraft.client.renderer.GlStateManager.enableTexture2D();
        net.minecraft.client.renderer.GlStateManager.enableAlpha();
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        float textY = drawY + (pillH - mcFont.FONT_HEIGHT) / 2.0F - (nametagsHealthBar.isEnabled() ? 0.7F : 0.0F);
        float nameX = drawX + pillPadH;

        // Draw Player Name
        mcFont.drawStringWithShadow(name, nameX, textY, 0xFFFFFFFF);

        // Draw Health
        float healthX = nameX + nameW + gap;
        mcFont.drawStringWithShadow(healthStr, healthX, textY, healthColor);

        net.minecraft.client.renderer.GlStateManager.depthMask(true);
        net.minecraft.client.renderer.GlStateManager.enableDepth();
        net.minecraft.client.renderer.GlStateManager.enableLighting();
        net.minecraft.client.renderer.GlStateManager.disableBlend();
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        net.minecraft.client.renderer.GlStateManager.popMatrix();
    }
}
