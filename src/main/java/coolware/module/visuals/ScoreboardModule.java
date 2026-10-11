package coolware.module.visuals;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import coolware.config.ConfigManager;
import coolware.module.Category;
import coolware.module.Module;
import coolware.setting.BooleanSetting;
import coolware.setting.NumberSetting;
import coolware.util.RenderUtil;
import coolware.util.RoundedUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

public class ScoreboardModule extends Module {

    private final NumberSetting posX = new NumberSetting("Position X", this, -1.0D, -1.0D, 4000.0D, 1.0D);
    private final NumberSetting posY = new NumberSetting("Position Y", this, -1.0D, -1.0D, 4000.0D, 1.0D);
    private final NumberSetting scale = new NumberSetting("Scale", this, 1.0D, 0.5D, 2.0D, 0.05D);
    private final BooleanSetting dropShadow = new BooleanSetting("Drop Shadow", this, true);
    private final BooleanSetting roundedCorners = new BooleanSetting("Rounded", this, true);
    private final NumberSetting cornerRadius = new NumberSetting("Radius", this, 5.0D, 1.0D, 12.0D, 0.5D);
    private final BooleanSetting spoofIP = new BooleanSetting("Spoof IP", this, true);
    private final BooleanSetting showPoints = new BooleanSetting("Show Red Points", this, true);

    private static final Pattern SERVER_IP_REGEX = Pattern.compile("(?i)(?:www\\.)?[a-zA-Z0-9\\-_]+(?:\\.[a-zA-Z0-9\\-_]+)*(?:\\.net|\\.com|\\.org|\\.club|\\.gg|\\.me|\\.xyz|\\.io|\\.cn|\\.ru|\\.de|\\.eu|\\.vip)");

    private float renderedX;
    private float renderedY;
    private float renderedWidth;
    private float renderedHeight;
    private boolean dragging;
    private float dragOffsetX;
    private float dragOffsetY;
    private boolean positionChanged;

    public ScoreboardModule() {
        super("Scoreboard", "Customize, scale, reposition, and stylize the in-game scoreboard", Category.VISUALS, Keyboard.KEY_NONE);
        addSetting(posX);
        addSetting(posY);
        addSetting(scale);
        addSetting(dropShadow);
        addSetting(roundedCorners);
        addSetting(cornerRadius);
        addSetting(spoofIP);
        addSetting(showPoints);
        setToggled(true);
    }

    public boolean renderCustomScoreboard(ScoreObjective objective, ScaledResolution scaledRes) {
        if (!isToggled() || objective == null) {
            return false;
        }

        Scoreboard scoreboard = objective.getScoreboard();
        Collection<Score> collection = scoreboard.getSortedScores(objective);
        List<Score> list = Lists.newArrayList(Iterables.filter(collection, new Predicate<Score>() {
            public boolean apply(Score score) {
                return score != null && score.getPlayerName() != null && !score.getPlayerName().startsWith("#");
            }
        }));

        if (list.size() > 15) {
            collection = Lists.newArrayList(Iterables.skip(list, collection.size() - 15));
        } else {
            collection = list;
        }

        if (collection.isEmpty()) {
            return false;
        }

        float currentScale = scale.getValueFloat();
        String title = objective.getDisplayName();
        int fontHeight = mc.fontRendererObj.FONT_HEIGHT;
        int maxTextWidth = mc.fontRendererObj.getStringWidth(title);

        boolean displayPoints = showPoints.isEnabled();
        for (Score score : collection) {
            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            String formattedName = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());
            if (spoofIP.isEnabled()) {
                formattedName = filterScoreboardText(formattedName);
            }
            String pointsText = displayPoints ? (": " + EnumChatFormatting.RED + score.getScorePoints()) : "";
            maxTextWidth = Math.max(maxTextWidth, mc.fontRendererObj.getStringWidth(formattedName + pointsText));
        }

        int lineCount = collection.size();
        float radius = roundedCorners.isEnabled() ? Math.min(cornerRadius.getValueFloat(), 8.0F) : 0.0F;
        float horizontalPadding = Math.max(12.0F, radius + 4.0F);
        float contentWidth = (float) maxTextWidth + (horizontalPadding * 2.0F) + (displayPoints ? 12.0F : 0.0F);
        float headerHeight = fontHeight + 6.0F;
        float bodyHeight = lineCount * (fontHeight + 2.0F) + 6.0F;
        float totalHeight = headerHeight + bodyHeight + 3.0F;

        float scaledContentWidth = contentWidth * currentScale;
        float scaledContentHeight = totalHeight * currentScale;

        float defaultX = scaledRes.getScaledWidth() - scaledContentWidth - 8.0F;
        float defaultY = (scaledRes.getScaledHeight() - scaledContentHeight) / 2.0F;

        float boardX = posX.getValueFloat() < 0 ? defaultX : posX.getValueFloat();
        float boardY = posY.getValueFloat() < 0 ? defaultY : posY.getValueFloat();

        boardX = Math.max(0.0F, Math.min(boardX, scaledRes.getScaledWidth() - scaledContentWidth));
        boardY = Math.max(0.0F, Math.min(boardY, scaledRes.getScaledHeight() - scaledContentHeight));

        renderedX = boardX;
        renderedY = boardY;
        renderedWidth = scaledContentWidth;
        renderedHeight = scaledContentHeight;

        GlStateManager.pushMatrix();
        GlStateManager.translate(boardX, boardY, 0.0F);
        GlStateManager.scale(currentScale, currentScale, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        // Drop shadow
        if (dropShadow.isEnabled()) {
            Color shadowColor = new Color(0, 0, 0, 110);
            if (radius > 0.0F) {
                RoundedUtils.drawRoundShadow(0.0F, 0.0F, contentWidth, totalHeight, radius, 4.0F, shadowColor);
            } else {
                RenderUtil.drawRect(-2.0F, -1.0F, contentWidth + 2.0F, totalHeight + 3.0F, shadowColor.getRGB());
            }
        }

        // Background
        Color backgroundColor = new Color(15, 15, 18, 185);
        if (radius > 0.0F) {
            RoundedUtils.drawRound(0.0F, 0.0F, contentWidth, totalHeight, radius, backgroundColor);
        } else {
            RenderUtil.drawRect(0.0F, 0.0F, contentWidth, totalHeight, backgroundColor.getRGB());
        }

        // Header separator line
        Color sepColor = new Color(255, 255, 255, 30);
        RenderUtil.drawRect(horizontalPadding * 0.5F, headerHeight - 0.5F, contentWidth - (horizontalPadding * 0.5F), headerHeight + 0.5F, sepColor.getRGB());

        // Draw title
        float titleX = (contentWidth - mc.fontRendererObj.getStringWidth(title)) / 2.0F;
        float titleY = 4.0F;
        mc.fontRendererObj.drawStringWithShadow(title, titleX, titleY, 0xFFFFFFFF);

        // Draw lines
        float lineY = headerHeight + 3.0F;
        int lineIndex = 0;
        for (Score score : collection) {
            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            String formattedName = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());
            if (spoofIP.isEnabled()) {
                formattedName = filterScoreboardText(formattedName);
            }

            mc.fontRendererObj.drawStringWithShadow(formattedName, horizontalPadding, lineY + (lineCount - 1 - lineIndex) * (fontHeight + 2.0F), 0xFFE0E0E0);

            if (displayPoints) {
                String points = EnumChatFormatting.RED + "" + score.getScorePoints();
                float pointsX = contentWidth - horizontalPadding - mc.fontRendererObj.getStringWidth(points);
                mc.fontRendererObj.drawStringWithShadow(points, pointsX, lineY + (lineCount - 1 - lineIndex) * (fontHeight + 2.0F), 0xFFFFFFFF);
            }

            lineIndex++;
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();

        return true;
    }

    private String filterScoreboardText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        String lowerClean = cleanForMatch(text).toLowerCase();

        // 1. Direct contains checks for known server names & domains
        if (lowerClean.contains("hypixel") || lowerClean.contains("minemen") ||
                lowerClean.contains("loyisa") || lowerClean.contains("pvptemple") ||
                lowerClean.contains("invadedlands") || lowerClean.contains("blocksmc") ||
                lowerClean.contains("cubecraft") || lowerClean.contains("mineplex") ||
                lowerClean.contains("gommehd") || lowerClean.contains("jartex") ||
                lowerClean.contains("pika-network")) {
            return "§e§lcoolware.xyz";
        }

        // 2. Generic domain match (e.g. *.net, *.com, *.org, *.club, etc.)
        if (SERVER_IP_REGEX.matcher(lowerClean).find()) {
            return "§e§lcoolware.xyz";
        }

        return text;
    }

    private String cleanForMatch(String input) {
        if (input == null) return "";
        // Remove standard minecraft formatting codes
        String noColor = EnumChatFormatting.getTextWithoutFormattingCodes(input);
        // Also remove any stray section symbols or unprintable control characters
        return noColor.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
    }

    public boolean beginDrag(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0 || !isToggled()) {
            return false;
        }
        if (mouseX >= renderedX && mouseX <= renderedX + renderedWidth &&
                mouseY >= renderedY && mouseY <= renderedY + renderedHeight) {
            dragging = true;
            dragOffsetX = mouseX - renderedX;
            dragOffsetY = mouseY - renderedY;
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
}
