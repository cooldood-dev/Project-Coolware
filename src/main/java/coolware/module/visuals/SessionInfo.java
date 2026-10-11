package coolware.module.visuals;

import coolware.config.ConfigManager;
import coolware.font.moonlight.FontRenderer;
import coolware.font.moonlight.Fonts;
import coolware.module.Category;
import coolware.module.Module;
import coolware.setting.ModeSetting;
import coolware.setting.NumberSetting;
import coolware.util.RenderUtil;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S45PacketTitle;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SessionInfo extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", this, "Modern", "Modern", "Cool");
    private final NumberSetting posX = new NumberSetting("Position X", this, 5.0D, 0.0D, 4000.0D, 1.0D);
    private final NumberSetting posY = new NumberSetting("Position Y", this, 27.0D, 0.0D, 4000.0D, 1.0D);
    private final ModeSetting colorMode = new ModeSetting("Color Mode", this, "Theme", "Theme", "Custom", "Rainbow");
    private final NumberSetting backgroundAlpha = new NumberSetting("Background Alpha", this, 120.0D, 0.0D, 255.0D, 1.0D);

    // Ambient 3.0 session stats
    public int kills = 0;
    public int deaths = 0;
    public int wins = 0;
    public long startTime = System.currentTimeMillis();

    // Legacy compat
    public int killed = 0;
    public int won = 0;
    public int lost = 0;
    public int match = 0;
    public int matchKilled = 0;
    public int prevMatchKilled = 0;

    private float renderedX;
    private float renderedY;
    private float renderedWidth;
    private float renderedHeight;
    private boolean dragging;
    private float dragOffsetX;
    private float dragOffsetY;
    private boolean positionChanged;

    private float smoothX = -1;
    private float smoothY = -1;
    private long lastAnimTime = System.currentTimeMillis();

    // Ambient timer tracking
    private long lastStopwatchTime = System.currentTimeMillis();
    private long lastKillCountdown = System.currentTimeMillis();
    private String time = "00:00:00";
    private long seconds = 0;

    private final String[] killStrings = {"was &+'s final", "by &+"};

    private static final Map<String, ResourceLocation> ICONS = new HashMap<String, ResourceLocation>();

    static {
        ICONS.put("wins", new ResourceLocation("coolware/textures/sessioninfo/win.png"));
        ICONS.put("kills", new ResourceLocation("coolware/textures/sessioninfo/skull.png"));
        ICONS.put("deaths", new ResourceLocation("coolware/textures/sessioninfo/heart.png"));
        ICONS.put("clock", new ResourceLocation("coolware/textures/sessioninfo/clock.png"));
        ICONS.put("swords", new ResourceLocation("coolware/textures/sessioninfo/swords.png"));
        ICONS.put("stopwatch", new ResourceLocation("coolware/textures/sessioninfo/stopwatch.png"));
    }

    public SessionInfo() {
        super("SessionInfo", "Displays session statistics and match information", Category.VISUALS, Keyboard.KEY_NONE);
        addSetting(mode);
        addSetting(posX);
        addSetting(posY);
        addSetting(colorMode);
        addSetting(backgroundAlpha);
        setToggled(true);
    }

    public void resetSession() {
        this.deaths = 0;
        this.kills = 0;
        this.wins = 0;
        this.startTime = System.currentTimeMillis();
        this.killed = 0;
        this.won = 0;
        this.lost = 0;
    }

    public void onChatPacket(S02PacketChat packet) {
        if (packet == null || packet.getChatComponent() == null || mc.thePlayer == null) return;
        String text = packet.getChatComponent().getUnformattedText();

        // Ambient 3.0 chat detection
        if (!text.contains("Bed")) {
            List<String> modified = new ArrayList<String>();
            for (String element : this.killStrings) {
                modified.add(element.replace("&+", mc.thePlayer.getName()));
            }
            for (String s : modified) {
                if (text.contains(s)) {
                    ++this.kills;
                    ++this.killed;
                    this.prevMatchKilled = this.matchKilled;
                    ++this.matchKilled;
                    break;
                }
            }
        }

        // Cool mode fallback detection
        if (text.contains("was killed by " + mc.thePlayer.getName())) {
            if (this.kills == this.killed - 1) {
                ++this.kills;
            }
        }
        if (text.contains("You Died! Want to play again?")) {
            ++this.lost;
        }
    }

    public void onTitlePacket(S45PacketTitle packet) {
        if (packet == null || packet.getMessage() == null) return;
        String text = packet.getMessage().getUnformattedText();

        // Ambient 3.0 title detection
        if (text.contains("VICTORY!")) {
            ++this.wins;
            ++this.won;
        }

        if (text.contains("DIED") && (System.currentTimeMillis() - this.lastKillCountdown >= 10000L)) {
            ++this.deaths;
            this.lastKillCountdown = System.currentTimeMillis();
        }

        if (text.contains("GAME OVER!") || text.contains("DEFEAT!") || text.contains("YOU DIED!")) {
            ++this.lost;
        }
    }

    public void onWorldChange() {
        prevMatchKilled = matchKilled;
        matchKilled = 0;
        match += 1;
        if (match > 6) {
            match = 6;
        }
    }

    public int getColor(int counter) {
        if ("Rainbow".equalsIgnoreCase(colorMode.getMode())) {
            double rainbowState = Math.ceil((System.currentTimeMillis() - (long) counter * 20L) / 8.0);
            rainbowState %= 360.0;
            return Color.HSBtoRGB((float) (rainbowState / 360.0), 0.8F, 1.0F);
        }
        coolware.module.visuals.Theme theme = coolware.module.visuals.Theme.getInstance();
        return theme != null ? theme.getColor(counter) : 0xFF55AAFF;
    }

    public Color getThemeColor1() {
        return new Color(getColor(0), true);
    }

    public Color getThemeColor2() {
        return new Color(getColor(90), true);
    }

    public int getBgColor() {
        int alpha = (int) backgroundAlpha.getValue();
        return new Color(21, 21, 24, Math.max(0, Math.min(255, alpha))).getRGB();
    }

    @Override
    public void onRender2D() {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        // Update session time
        if (System.currentTimeMillis() - this.lastStopwatchTime >= 1000L) {
            long elapsed = System.currentTimeMillis() - this.startTime;
            this.seconds = elapsed / 1000L;
            long minutes = (elapsed / (1000L * 60L)) % 60L;
            long hours = (elapsed / (1000L * 60L * 60L)) % 24L;
            this.time = String.format("%02d:%02d:%02d", hours, minutes, this.seconds % 60L);
            this.lastStopwatchTime = System.currentTimeMillis();
        }

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

        if ("Modern".equalsIgnoreCase(mode.getMode())) {
            renderModernMode(renderX, renderY);
        } else {
            renderCoolMode(renderX, renderY);
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /**
     * Ambient 3.0 Modern SessionStats mode (100% exact copy from fr.ambient.module.impl.render.hud.SessionStats)
     */
    private void renderModernMode(float x, float y) {
        FontRenderer osb12 = Fonts.nunito.get(15);
        FontRenderer osr17 = Fonts.openSansRegular.get(17);
        FontRenderer osb17 = Fonts.nunito.get(15);
        FontRenderer nunito20 = Fonts.nunito.get(20);

        String[] statLabels = new String[] {
                "Wins:",
                "Kills:",
                "Deaths:"
        };

        String[] statValues = new String[] {
                String.valueOf(this.wins),
                String.valueOf(this.kills),
                String.valueOf(this.deaths)
        };

        float maxRowWidth = 0.0F;
        for (int i = 0; i < statLabels.length; i++) {
            float rowWidth = osb17.getStringWidth(statLabels[i]) + 18.0F + osb17.getStringWidth(statValues[i]);
            if (rowWidth > maxRowWidth) {
                maxRowWidth = rowWidth;
            }
        }

        float width = 58.0F + maxRowWidth + 12.0F;
        float height = 55.0F;

        // Ambient dark card background
        RenderUtil.drawRoundedRect(x, y, width, height, 7.0F, new Color(0x90121214, true));

        Color themeCol1 = getThemeColor1();
        Color themeCol2 = getThemeColor2();

        // Circle loading indicator
        RenderUtil.drawCircleLoading(x - 1.0F, y - 1.0F, 55.0F, 1.0F, new Color(0x65000000, true));
        RenderUtil.drawCircleLoading(x - 1.0F, y - 1.0F, 55.0F, (seconds % 3600L) / 3600.0F, themeCol2, themeCol1);

        String[] parts = this.time.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);

        if (hours != 0) {
            osb12.drawCenteredString(hours + "hr", x + 27.0F, y + height / 2.0F - 8.0F, -1);
            osb12.drawCenteredString(minutes + "min", x + 27.0F, y + height / 2.0F + 2.0F, -1);
        } else {
            nunito20.drawCenteredString(String.valueOf(minutes), x + 26.5F, y + height / 2.0F - 9.0F, -1);
            osb17.drawCenteredString("min", x + 26.5F, y + height / 2.0F + 2.0F, -1);
        }

        float boxY = y + 7.0F;
        for (int i = 0; i < statLabels.length; i++) {
            String label = statLabels[i];
            String value = statValues[i];

            osb17.drawString(label, x + 58.0F, boxY + 2.0F, -1);
            osb17.drawString(value, x + width - 12.0F - osb17.getStringWidth(value), boxY + 2.0F, themeCol2.getRGB());

            boxY += 14.0F;
        }

        renderedX = x;
        renderedY = y;
        renderedWidth = width;
        renderedHeight = height;
    }

    private void renderCoolMode(float renderX, float renderY) {
        float width = 140.0F;
        float height = Fonts.interSemiBold.get(20).getHeight() + Fonts.sfui.get(18).getHeight() * 3 + 3.0F;

        RenderUtil.drawRect(renderX, renderY, renderX + width, renderY + height, getBgColor());
        RenderUtil.drawHorizontalGradientSideways(renderX, renderY, width, 1.0F, getColor(0), getColor(90));

        float left = renderX + 2.0F;
        float right = renderX + width - 2.0F;
        float textY = renderY + 4.0F;

        Fonts.interSemiBold.get(20).drawStringWithShadow("Current Session", left, textY, -1);

        Fonts.sfui.get(18).drawStringWithShadow("Play Time:", left, textY + Fonts.interSemiBold.get(20).getHeight(), -1);
        Fonts.sfui.get(18).drawStringWithShadow(RenderUtil.sessionTime(), right - Fonts.sfui.get(18).getStringWidth(RenderUtil.sessionTime()), textY + Fonts.interSemiBold.get(20).getHeight(), -1);

        Fonts.sfui.get(18).drawStringWithShadow("Games Won:", left, textY + Fonts.interSemiBold.get(20).getHeight() + Fonts.sfui.get(18).getHeight(), -1);
        Fonts.sfui.get(18).drawStringWithShadow(this.wins + "", right - Fonts.sfui.get(18).getStringWidth(this.wins + ""), textY + Fonts.interSemiBold.get(20).getHeight() + Fonts.sfui.get(18).getHeight(), -1);

        Fonts.sfui.get(18).drawStringWithShadow("Players Killed:", left, textY + Fonts.interSemiBold.get(20).getHeight() + Fonts.sfui.get(18).getHeight() * 2, -1);
        Fonts.sfui.get(18).drawStringWithShadow(this.kills + "", right - Fonts.sfui.get(18).getStringWidth(this.kills + ""), textY + Fonts.interSemiBold.get(20).getHeight() + Fonts.sfui.get(18).getHeight() * 2, -1);

        renderedX = renderX;
        renderedY = renderY;
        renderedWidth = width;
        renderedHeight = height;
    }

    private void drawImage(String key, float x, float y, float w, float h, Color color) {
        ResourceLocation res = ICONS.get(key);
        if (res == null) return;

        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F);

        mc.getTextureManager().bindTexture(res);
        Gui.drawModalRectWithCustomSizedTexture((int) x, (int) y, 0.0F, 0.0F, (int) w, (int) h, w, h);

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
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
