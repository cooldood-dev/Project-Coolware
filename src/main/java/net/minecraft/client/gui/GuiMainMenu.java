package net.minecraft.client.gui;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import org.apache.commons.io.Charsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GLContext;

import com.google.common.collect.Lists;

import coolware.Coolware;
import coolware.font.CFontRenderer;
import coolware.font.FontManager;
import coolware.gui.altmanager.GuiAltManager;
import coolware.gui.menu.ModernIconButton;
import coolware.gui.menu.ModernMenuCardButton;
import coolware.gui.menu.ParticleSystem;
import coolware.gui.menu.WaveRenderer;
import coolware.module.visuals.Theme;
import coolware.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.realms.RealmsBridge;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.demo.DemoWorldServer;
import net.minecraft.world.storage.ISaveFormat;
import net.minecraft.world.storage.WorldInfo;
import net.optifine.reflect.Reflector;

public class GuiMainMenu extends GuiScreen implements GuiYesNoCallback
{
    private static final AtomicInteger field_175373_f = new AtomicInteger(0);
    private static final Logger logger = LogManager.getLogger();
    private static final Random RANDOM = new Random();

    /** Coolware Client Logo Textures */
    private static final ResourceLocation COOLWARE_LOGO = new ResourceLocation("coolware", "logo.png");
    private static ResourceLocation dynamicLogo = null;
    private static boolean dynamicLogoLoaded = false;

    /** Interactive particle engine */
    private final ParticleSystem particleSystem = new ParticleSystem();

    /** Screen entrance fade-in animation */
    private float fadeProgress = 0.0F;

    /** Date formatters for top bar */
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM");

    /** Splash text */
    private String splashText;
    private GuiButton buttonResetDemo;

    private final Object threadLock = new Object();

    private String openGLWarning1;
    private String openGLWarning2;
    private String openGLWarningLink;
    private static final ResourceLocation splashTexts = new ResourceLocation("texts/splashes.txt");

    public static final String field_96138_a = "Please click " + EnumChatFormatting.UNDERLINE + "here" + EnumChatFormatting.RESET + " for more information.";
    private int field_92024_r;
    private int field_92023_s;
    private int field_92022_t;
    private int field_92021_u;
    private int field_92020_v;
    private int field_92019_w;

    private boolean field_183502_L;
    private GuiScreen field_183503_M;

    public GuiMainMenu()
    {
        this.openGLWarning2 = field_96138_a;
        this.field_183502_L = false;
        this.splashText = "Play · Create · Dominate";
        BufferedReader bufferedreader = null;

        try
        {
            List<String> list = Lists.<String>newArrayList();
            bufferedreader = new BufferedReader(new InputStreamReader(Minecraft.getMinecraft().getResourceManager().getResource(splashTexts).getInputStream(), Charsets.UTF_8));
            String s;

            while ((s = bufferedreader.readLine()) != null)
            {
                s = s.trim();

                if (!s.isEmpty())
                {
                    list.add(s);
                }
            }

            if (!list.isEmpty())
            {
                while (true)
                {
                    this.splashText = (String)list.get(RANDOM.nextInt(list.size()));

                    if (this.splashText.hashCode() != 125780783)
                    {
                        break;
                    }
                }
            }
        }
        catch (IOException ignored)
        {
        }
        finally
        {
            if (bufferedreader != null)
            {
                try
                {
                    bufferedreader.close();
                }
                catch (IOException ignored)
                {
                }
            }
        }

        this.openGLWarning1 = "";

        if (!GLContext.getCapabilities().OpenGL20 && !OpenGlHelper.areShadersSupported())
        {
            this.openGLWarning1 = I18n.format("title.oldgl1", new Object[0]);
            this.openGLWarning2 = I18n.format("title.oldgl2", new Object[0]);
            this.openGLWarningLink = "https://help.mojang.com/customer/portal/articles/325948?ref=game";
        }
    }

    private boolean func_183501_a()
    {
        return Minecraft.getMinecraft().gameSettings.getOptionOrdinalValue(GameSettings.Options.REALMS_NOTIFICATIONS) && this.field_183503_M != null;
    }

    @Override
    public void updateScreen()
    {
        if (this.func_183501_a())
        {
            this.field_183503_M.updateScreen();
        }
    }

    @Override
    public boolean doesGuiPauseGame()
    {
        return false;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
    }

    @Override
    public void initGui()
    {
        this.fadeProgress = 0.0F;
        this.particleSystem.init(this.width, this.height);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());

        if (calendar.get(Calendar.MONTH) + 1 == 12 && calendar.get(Calendar.DAY_OF_MONTH) == 24)
        {
            this.splashText = "Merry X-mas!";
        }
        else if (calendar.get(Calendar.MONTH) + 1 == 1 && calendar.get(Calendar.DAY_OF_MONTH) == 1)
        {
            this.splashText = "Happy new year!";
        }
        else if (calendar.get(Calendar.MONTH) + 1 == 10 && calendar.get(Calendar.DAY_OF_MONTH) == 31)
        {
            this.splashText = "OOoooOOOoooo! Spooky!";
        }

        // Central Glass Panel Dimensions (Compact & perfectly framed)
        int panelWidth = Math.min(540, this.width - 40);
        int panelHeight = 302;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2 + 4;

        // Menu Cards Dimensions
        int cardWidth = panelWidth - 44;
        int cardHeight = 52;
        int cardSpacing = 8;
        int startCardY = panelY + 68;
        int cardX = panelX + 22;

        if (this.mc.isDemo())
        {
            this.addDemoButtons(startCardY, cardHeight + cardSpacing);
        }
        else
        {
            // 1. Singleplayer Menu Card
            this.buttonList.add(new ModernMenuCardButton(1, cardX, startCardY, cardWidth, cardHeight,
                    ModernMenuCardButton.CardType.SINGLEPLAYER, "\uf144", "Singleplayer", "Explore and survive in solo worlds"));

            // 2. Multiplayer Menu Card
            this.buttonList.add(new ModernMenuCardButton(2, cardX, startCardY + (cardHeight + cardSpacing), cardWidth, cardHeight,
                    ModernMenuCardButton.CardType.MULTIPLAYER, "\uf0c0", "Multiplayer", "Join servers, Bedwars & PvP arenas"));

            // 3. Alt Manager Menu Card
            this.buttonList.add(new ModernMenuCardButton(100, cardX, startCardY + (cardHeight + cardSpacing) * 2, cardWidth, cardHeight,
                    ModernMenuCardButton.CardType.ALT_MANAGER, "\uf2c2", "Alt Manager", "Manage Microsoft & Mojang accounts"));
        }

        // Bottom Action Buttons Row inside panel (Settings & Exit - Compact 32px)
        int iconBtnSize = 32;
        int bottomRowY = startCardY + (cardHeight + cardSpacing) * 3 + 8;
        int spacing = 16;
        int totalBtnWidth = (iconBtnSize * 2) + spacing;
        int iconStartX = panelX + (panelWidth - totalBtnWidth) / 2;

        // Settings (Left)
        this.buttonList.add(new ModernIconButton(0, iconStartX, bottomRowY, iconBtnSize, ModernIconButton.ActionType.SETTINGS, "Settings", false));

        // Exit / Quit (Right)
        this.buttonList.add(new ModernIconButton(4, iconStartX + iconBtnSize + spacing, bottomRowY, iconBtnSize, ModernIconButton.ActionType.EXIT, "Quit Game", false));

        synchronized (this.threadLock)
        {
            this.field_92023_s = this.fontRendererObj.getStringWidth(this.openGLWarning1);
            this.field_92024_r = this.fontRendererObj.getStringWidth(this.openGLWarning2);
            int k = Math.max(this.field_92023_s, this.field_92024_r);
            this.field_92022_t = (this.width - k) / 2;
            this.field_92021_u = panelY - 24;
            this.field_92020_v = this.field_92022_t + k;
            this.field_92019_w = this.field_92021_u + 24;
        }

        this.mc.setConnectedToRealms(false);

        if (Minecraft.getMinecraft().gameSettings.getOptionOrdinalValue(GameSettings.Options.REALMS_NOTIFICATIONS) && !this.field_183502_L)
        {
            RealmsBridge realmsbridge = new RealmsBridge();
            this.field_183503_M = realmsbridge.getNotificationScreen(this);
            this.field_183502_L = true;
        }

        if (this.func_183501_a())
        {
            this.field_183503_M.setGuiSize(this.width, this.height);
            this.field_183503_M.initGui();
        }
    }

    private void addDemoButtons(int startY, int spacing)
    {
        int panelWidth = Math.min(540, this.width - 40);
        int cardWidth = panelWidth - 44;
        int cardX = (this.width - panelWidth) / 2 + 22;

        this.buttonList.add(new ModernMenuCardButton(11, cardX, startY, cardWidth, 52,
                ModernMenuCardButton.CardType.SINGLEPLAYER, "\uf144", I18n.format("menu.playdemo"), "Try the Minecraft demo world"));
        this.buttonResetDemo = new ModernMenuCardButton(12, cardX, startY + spacing, cardWidth, 52,
                ModernMenuCardButton.CardType.GENERIC, "\u21BA", I18n.format("menu.resetdemo"), "Reset your demo world");
        this.buttonList.add(this.buttonResetDemo);
        ISaveFormat isaveformat = this.mc.getSaveLoader();
        WorldInfo worldinfo = isaveformat.getWorldInfo("Demo_World");

        if (worldinfo == null)
        {
            this.buttonResetDemo.enabled = false;
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException
    {
        if (button.id == 0)
        {
            this.mc.displayGuiScreen(new GuiOptions(this, this.mc.gameSettings));
        }

        if (button.id == 100)
        {
            this.mc.displayGuiScreen(new GuiAltManager(this));
        }

        if (button.id == 5)
        {
            this.mc.displayGuiScreen(new GuiLanguage(this, this.mc.gameSettings, this.mc.getLanguageManager()));
        }

        if (button.id == 1)
        {
            this.mc.displayGuiScreen(new GuiSelectWorld(this));
        }

        if (button.id == 2)
        {
            this.mc.displayGuiScreen(new GuiMultiplayer(this));
        }

        if (button.id == 4)
        {
            this.mc.shutdown();
        }

        if (button.id == 6 && Reflector.GuiModList_Constructor.exists())
        {
            this.mc.displayGuiScreen((GuiScreen)Reflector.newInstance(Reflector.GuiModList_Constructor, new Object[] {this}));
        }

        if (button.id == 11)
        {
            this.mc.launchIntegratedServer("Demo_World", "Demo_World", DemoWorldServer.demoWorldSettings);
        }

        if (button.id == 12)
        {
            ISaveFormat isaveformat = this.mc.getSaveLoader();
            WorldInfo worldinfo = isaveformat.getWorldInfo("Demo_World");

            if (worldinfo != null)
            {
                GuiYesNo guiyesno = GuiSelectWorld.makeDeleteWorldYesNo(this, worldinfo.getWorldName(), 12);
                this.mc.displayGuiScreen(guiyesno);
            }
        }
    }

    @Override
    public void confirmClicked(boolean result, int id)
    {
        if (result && id == 12)
        {
            ISaveFormat isaveformat = this.mc.getSaveLoader();
            isaveformat.flushCache();
            isaveformat.deleteWorldDirectory("Demo_World");
            this.mc.displayGuiScreen(this);
        }
        else if (id == 13)
        {
            if (result)
            {
                try
                {
                    Class<?> oclass = Class.forName("java.awt.Desktop");
                    Object object = oclass.getMethod("getDesktop", new Class[0]).invoke((Object)null, new Object[0]);
                    oclass.getMethod("browse", new Class[] {URI.class}).invoke(object, new Object[] {new URI(this.openGLWarningLink)});
                }
                catch (Throwable throwable)
                {
                    logger.error("Couldn't open link", throwable);
                }
            }

            this.mc.displayGuiScreen(this);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        if (fadeProgress < 1.0F) {
            fadeProgress = Math.min(1.0F, fadeProgress + 0.05F);
        }

        Theme theme = Theme.getInstance();
        int accentColor = theme.getColor(0);

        // 1. Deep Obsidian Gradient Background (#070509 to #10070B)
        RenderUtil.drawHorizontalGradient(0, 0, this.width, this.height, 0xFF070509, 0xFF10070B);
        RenderUtil.drawGradientRect(0, 0, this.width, this.height, 0x33000000, 0x88000000);

        // 2. Ambient Radial Glow behind the central glass panel
        float centerX = this.width / 2.0F;
        float centerY = this.height / 2.0F;
        int glowAlpha = (int) (24.0F * fadeProgress);
        RenderUtil.drawCircle(centerX, centerY, 240.0F, (glowAlpha / 3 << 24) | (accentColor & 0xFFFFFF));
        RenderUtil.drawCircle(centerX, centerY, 150.0F, (glowAlpha << 24) | (accentColor & 0xFFFFFF));

        // 3. Floating Particle Nebula with Constellation Connectors
        this.particleSystem.updateAndRender(this.width, this.height, mouseX, mouseY);

        // 4. Smooth Sinusoidal Fluid Waves (Dark and elegant behind UI)
        float time = (Minecraft.getSystemTime() % 1000000L) / 400.0F;
        WaveRenderer.renderWaves(this.width, this.height, time);

        // 5. Top Bar - Header Navigation & Status Badges
        renderTopBar(accentColor, mouseX, mouseY);

        // 6. Central Glassmorphic Dashboard Panel
        int panelWidth = Math.min(540, this.width - 40);
        int panelHeight = 302;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2 + 4;

        renderCenterGlassPanel(panelX, panelY, panelWidth, panelHeight, accentColor);

        // 7. Render Buttons
        super.drawScreen(mouseX, mouseY, partialTicks);

        // 8. Render Tooltips for Circular Action Buttons
        for (GuiButton btn : this.buttonList) {
            if (btn instanceof ModernIconButton) {
                ((ModernIconButton) btn).drawTooltip(this.mc, mouseX, mouseY);
            }
        }

        // 9. Bottom Footer Information Bar
        renderFooter(accentColor);
    }

    private void renderTopBar(int accentColor, int mouseX, int mouseY) {
        Date now = new Date();
        String timeStr = timeFormat.format(now);
        String dateStr = dateFormat.format(now);
        String user = (this.mc.getSession() != null && this.mc.getSession().getUsername() != null) ? this.mc.getSession().getUsername() : "Player";

        // --- Left: User Profile Pill ---
        int userTextWidth = (FontManager.sfSemiBold14 != null) ? FontManager.sfSemiBold14.getStringWidth(user) : this.fontRendererObj.getStringWidth(user);
        float userPillW = userTextWidth + 56.0F;
        float userPillH = 28.0F;
        float userPillX = 18.0F;
        float userPillY = 16.0F;

        boolean userHovered = mouseX >= userPillX && mouseY >= userPillY && mouseX <= userPillX + userPillW && mouseY <= userPillY + userPillH;

        // User Pill Glass Background
        int userBgAlpha = userHovered ? 0x99 : 0x66;
        RenderUtil.drawRoundedGradient(userPillX, userPillY, userPillW, userPillH, 14.0F, (userBgAlpha << 24) | 0x1E141E, (userBgAlpha + 0x20 << 24) | 0x0D090E);
        RenderUtil.drawRoundedOutline(userPillX, userPillY, userPillW, userPillH, 14.0F, 1.2F, userHovered ? (0xDD000000 | (accentColor & 0xFFFFFF)) : 0x28FFFFFF);

        // Profile Vector Icon (Bolder)
        RenderUtil.drawProfileIcon(userPillX + 15.0F, userPillY + 14.0F, 13.0F, userHovered ? (0xFF000000 | accentColor) : 0xFFFFFFFF);

        // Username
        float userTextY = userPillY + (userPillH - (FontManager.sfSemiBold14 != null ? FontManager.sfSemiBold14.getHeight() : 8)) / 2.0F + 1.0F;
        if (FontManager.sfSemiBold14 != null) {
            FontManager.sfSemiBold14.drawStringWithShadow(user, userPillX + 30.0F, userTextY, 0xFFF2EEF2);
        } else {
            this.fontRendererObj.drawStringWithShadow("\u00a7l" + user, (int) (userPillX + 30.0F), (int) (userPillY + 9.5F), 0xFFF2EEF2);
        }

        // Pulsing Online status indicator dot (Green)
        float pulse = (float) (Math.sin(System.currentTimeMillis() / 300.0) * 0.35 + 0.65);
        int dotAlpha = (int) (pulse * 255);
        int statusDotColor = (dotAlpha << 24) | 0x00FF88;
        RenderUtil.drawCircle(userPillX + userPillW - 12.0F, userPillY + 14.0F, 3.2F, statusDotColor);

        // --- Right: Clock / Date / FPS Badge ---
        String fpsStr = Minecraft.getDebugFPS() + " FPS";
        
        CFontRenderer font = FontManager.sfSemiBold14;
        int timeW = (font != null) ? font.getStringWidth(timeStr) : this.fontRendererObj.getStringWidth(timeStr);
        int dateW = (font != null) ? font.getStringWidth(dateStr) : this.fontRendererObj.getStringWidth(dateStr);
        int fpsW  = (font != null) ? font.getStringWidth(fpsStr)  : this.fontRendererObj.getStringWidth(fpsStr);

        float padLeft = 14.0F;
        float clockRadius = 6.0F;
        float iconSize = 12.0F;
        float iconGap = 8.0F;
        float sectionGap = 16.0F;
        float divGap = 12.0F;
        float padRight = 14.0F;

        float clockPillW = padLeft + iconSize + iconGap + timeW + sectionGap + iconSize + iconGap + dateW + divGap + 1.0F + divGap + fpsW + padRight;
        float clockPillH = 28.0F;
        float clockPillX = this.width - clockPillW - 18.0F;
        float clockPillY = 16.0F;

        RenderUtil.drawRoundedGradient(clockPillX, clockPillY, clockPillW, clockPillH, 14.0F, 0x661E141E, 0x880D090E);
        RenderUtil.drawRoundedOutline(clockPillX, clockPillY, clockPillW, clockPillH, 14.0F, 1.2F, 0x28FFFFFF);

        float curX = clockPillX + padLeft;
        float centerY = clockPillY + clockPillH / 2.0F;
        float textY = clockPillY + (clockPillH - (font != null ? font.getHeight() : 8)) / 2.0F + 1.0F;

        // 1. Clock Vector Icon (Bold & Crisp)
        RenderUtil.drawClockIcon(curX + iconSize / 2.0F, centerY, clockRadius, 0xFFFFFFFF);
        curX += iconSize + iconGap;

        // 2. Time Text (Bold)
        if (font != null) {
            font.drawStringWithShadow(timeStr, curX, textY, 0xFFF2EEF2);
        } else {
            this.fontRendererObj.drawStringWithShadow(timeStr, (int) curX, (int) (clockPillY + 9.5F), 0xFFF2EEF2);
        }
        curX += timeW + sectionGap;

        // 3. Calendar Vector Icon (Bold & Crisp)
        RenderUtil.drawCalendarIcon(curX + iconSize / 2.0F, centerY, 12.0F, 0xFFFFFFFF);
        curX += iconSize + iconGap;

        // 4. Date Text (Bold)
        if (font != null) {
            font.drawStringWithShadow(dateStr, curX, textY, 0xFFF2EEF2);
        } else {
            this.fontRendererObj.drawStringWithShadow(dateStr, (int) curX, (int) (clockPillY + 9.5F), 0xFFF2EEF2);
        }
        curX += dateW + divGap;

        // 5. Sleek Vertical Divider Bar
        RenderUtil.drawSmoothLine(curX, clockPillY + 7.0F, curX, clockPillY + clockPillH - 7.0F, 1.2F, 0x44FFFFFF);
        curX += 1.0F + divGap;

        // 6. FPS Text (Bold Vibrant Red)
        if (font != null) {
            font.drawStringWithShadow(fpsStr, curX, textY, 0xFFFF4054);
        } else {
            this.fontRendererObj.drawStringWithShadow("\u00a7c" + fpsStr, (int) curX, (int) (clockPillY + 9.5F), 0xFFFF4054);
        }
    }

    private ResourceLocation getOrLoadLogo() {
        if (!dynamicLogoLoaded) {
            try {
                BufferedImage bImg = null;
                File directFile = new File("C:/Users/Admin/Documents/CoolwareV2.01/CoolwareV2/Coolware v2/MavenMCP-1.8.9-master/logo.png");
                if (directFile.exists()) {
                    bImg = ImageIO.read(directFile);
                }
                if (bImg == null) {
                    File localFile = new File("logo.png");
                    if (localFile.exists()) {
                        bImg = ImageIO.read(localFile);
                    }
                }
                if (bImg == null) {
                    InputStream is = GuiMainMenu.class.getResourceAsStream("/assets/coolware/logo.png");
                    if (is != null) {
                        bImg = ImageIO.read(is);
                        is.close();
                    }
                }
                if (bImg != null) {
                    dynamicLogo = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("coolware_main_logo_tex", new DynamicTexture(bImg));
                    dynamicLogoLoaded = true;
                }
            } catch (Exception ignored) {
            }
        }
        return (dynamicLogo != null) ? dynamicLogo : COOLWARE_LOGO;
    }

    private void renderCenterGlassPanel(int x, int y, int w, int h, int accentColor) {
        // 1. Ambient Drop Shadow / Glow
        RenderUtil.drawSoftGlow(x, y, w, h, 16.0F, (0x28 << 24) | (accentColor & 0xFFFFFF), 3);

        // 2. Translucent Frosted Glass Body (rgba(10, 8, 12, 0.78))
        RenderUtil.drawRoundedGradient(x, y, w, h, 16.0F, 0xCC0D0A0F, 0xDD070509);

        // 3. Border Outline (1px solid rgba(255,255,255,0.12))
        RenderUtil.drawRoundedOutline(x, y, w, h, 16.0F, 1.0F, 0x1EFFFFFF);

        // 4. Coolware Logo Texture (Square 38x38 crisp image from logo.png)
        float logoX = x + 22.0F;
        float logoY = y + 14.0F;
        float logoSize = 38.0F;

        ResourceLocation logoRes = getOrLoadLogo();
        try {
            RenderUtil.drawTexture(logoRes, logoX, logoY, logoSize, logoSize, 0xFFFFFFFF);
        } catch (Exception ignored) {
        }

        // 5. Brand Header Title "COOLWARE"
        String title = "COOLWARE";
        float titleX = logoX + logoSize + 12.0F;
        float titleY = y + 14.0F;

        if (FontManager.sfBold28 != null) {
            FontManager.sfBold28.drawStringWithShadow(title, titleX, titleY, 0xFFF2EEF2);
        } else if (FontManager.sfBold22 != null) {
            FontManager.sfBold22.drawStringWithShadow(title, titleX, titleY, 0xFFF2EEF2);
        } else {
            this.fontRendererObj.drawStringWithShadow("\u00a7l" + title, titleX, titleY, 0xFFF2EEF2);
        }

        // 6. Version Pill Badge "v2.0"
        int titleW = (FontManager.sfBold28 != null) ? FontManager.sfBold28.getStringWidth(title) : ((FontManager.sfBold22 != null) ? FontManager.sfBold22.getStringWidth(title) : this.fontRendererObj.getStringWidth(title));
        float badgeX = titleX + titleW + 8.0F;
        float badgeY = titleY + 4.0F;
        String badgeText = "v" + Coolware.VERSION;
        int badgeTextW = (FontManager.sfSemiBold14 != null) ? FontManager.sfSemiBold14.getStringWidth(badgeText) : this.fontRendererObj.getStringWidth(badgeText);
        float badgeW = badgeTextW + 10.0F;
        float badgeH = 15.0F;

        RenderUtil.drawRoundedRect(badgeX, badgeY, badgeW, badgeH, 4.0F, 0x44E52B3F);
        RenderUtil.drawRoundedOutline(badgeX, badgeY, badgeW, badgeH, 4.0F, 1.0F, 0xAAE52B3F);

        if (FontManager.sfSemiBold14 != null) {
            FontManager.sfSemiBold14.drawCenteredString(badgeText, badgeX + badgeW / 2.0F, badgeY + 1.5F, 0xFFFF4054);
        } else {
            this.fontRendererObj.drawString(badgeText, (int) (badgeX + 5), (int) (badgeY + 3), 0xFFFF4054);
        }

        // 7. Subtitle / Splash
        String sub = "Play \u00b7 Create \u00b7 Dominate";
        if (this.splashText != null && !this.splashText.isEmpty() && !this.splashText.equals("missingno")) {
            sub = this.splashText;
        }

        float subY = titleY + 20.0F;
        if (FontManager.sfRegular14 != null) {
            FontManager.sfRegular14.drawStringWithShadow("\u00a77" + sub, titleX, subY, 0xFF8A838C);
        } else {
            this.fontRendererObj.drawStringWithShadow("\u00a77" + sub, titleX, subY, 0xFF8A838C);
        }

        // 8. Subtle Divider Line below header
        float divY = y + 58.0F;
        RenderUtil.drawHorizontalGradient(x + 22.0F, divY, x + w - 22.0F, divY + 1.0F, 0x44E52B3F, 0x11FFFFFF);
    }

    private void renderFooter(int accentColor) {
        String footerLeft = "Coolware Client \u00a78| \u00a7fMinecraft 1.8.9 + OptiFine M5";
        String footerRight = "Press \u00a7cRSHIFT\u00a7f for ClickGUI \u00a78| \u00a7fCreated by \u00a7cCoolware Team";

        float footerY = this.height - 18.0F;

        if (FontManager.sfRegular14 != null) {
            FontManager.sfRegular14.drawStringWithShadow(footerLeft, 18.0F, footerY, 0xFF8A838C);
            FontManager.sfRegular14.drawStringWithShadow(footerRight, this.width - FontManager.sfRegular14.getStringWidth(footerRight) - 18.0F, footerY, 0xFF8A838C);
        } else {
            this.fontRendererObj.drawStringWithShadow(footerLeft, 18.0F, footerY, 0xFF8A838C);
            this.fontRendererObj.drawStringWithShadow(footerRight, this.width - this.fontRendererObj.getStringWidth(footerRight) - 18.0F, footerY, 0xFF8A838C);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        // Check top-left user pill click -> open Alt Manager
        float userPillW = 120.0F;
        float userPillH = 28.0F;
        if (mouseButton == 0 && mouseX >= 18.0F && mouseX <= 18.0F + userPillW && mouseY >= 16.0F && mouseY <= 16.0F + userPillH) {
            this.mc.displayGuiScreen(new GuiAltManager(this));
            return;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);

        synchronized (this.threadLock)
        {
            if (this.openGLWarning1.length() > 0 && mouseX >= this.field_92022_t && mouseX <= this.field_92020_v && mouseY >= this.field_92021_u && mouseY <= this.field_92019_w)
            {
                GuiConfirmOpenLink guiconfirmopenlink = new GuiConfirmOpenLink(this, this.openGLWarningLink, 13, true);
                guiconfirmopenlink.disableSecurityWarning();
                this.mc.displayGuiScreen(guiconfirmopenlink);
            }
        }
    }
}
