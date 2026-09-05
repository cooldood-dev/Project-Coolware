package com.github.cooldood.screens;

import com.github.cooldood.Main;
import com.github.cooldood.utils.alts.Login;
import com.github.cooldood.utils.alts.SessionUtil;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.client.FileChooserUtil;
import com.github.cooldood.utils.client.ScreenUtil;
import com.github.cooldood.utils.render.FontUtil;
import com.github.cooldood.utils.render.RenderUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.apache.commons.io.FileUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.List;

public class AltManagerScreen extends GuiScreen {

    public static final List<Login.Alt> alts = new ArrayList<>();

    // Status message for feedback
    public static String statusMessage = "";
    public static long statusTimestamp = 0;
    public static boolean isStatusError = false;

    // Search bar
    private GuiTextField searchField;

    // Input Modal State
    public enum InputModalType {
        NONE,
        REFRESH_TOKEN,
        ACCESS_TOKEN,
        COOKIE_TEXT,
        CRACKED,
        RENAME
    }

    private InputModalType activeModal = InputModalType.NONE;
    private GuiTextField modalInput;
    private Login.Alt targetAltForModal;

    // Scrolling
    public float scrollAmount = 0;
    public float renderedScroll = 0;
    private float maxScroll = 0;

    // Textures
    private DynamicTexture binTexture;
    private DynamicTexture pencilTexture;

    // Layout constants
    private final float SIDEBAR_WIDTH = 210;
    private final float HEADER_HEIGHT = 46;

    public static void setStatus(String message, boolean error) {
        statusMessage = message;
        isStatusError = error;
        statusTimestamp = System.currentTimeMillis();
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        if (!Files.exists(Login.altsPath)) Login.altsPath.toFile().mkdirs();

        try {
            java.io.InputStream binStream = Main.class.getResourceAsStream("/bin.png");
            if (binStream != null) binTexture = new DynamicTexture(ImageIO.read(binStream));

            java.io.InputStream pencilStream = Main.class.getResourceAsStream("/pencil.png");
            if (pencilStream != null) pencilTexture = new DynamicTexture(ImageIO.read(pencilStream));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Search text field
        searchField = new GuiTextField(100, C.mc.fontRendererObj, (int) SIDEBAR_WIDTH + 14, 13, 170, 20);
        searchField.setMaxStringLength(40);
        searchField.setCanLoseFocus(true);

        // Modal input text field (supports long refresh & access tokens)
        int modalW = Math.min(420, C.res().getScaledWidth() - 40);
        modalInput = new GuiTextField(101, C.mc.fontRendererObj, (C.res().getScaledWidth() - modalW) / 2 + 15, C.res().getScaledHeight() / 2 - 10, modalW - 30, 22);
        modalInput.setMaxStringLength(4096);
        modalInput.setCanLoseFocus(false);

        loadAlts();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    public static void loadAlts() {
        Gson gson = new GsonBuilder().setPrettyPrinting().enableComplexMapKeySerialization().create();
        alts.clear();

        File folder = Login.altsPath.toFile();
        if (folder.exists() && folder.listFiles() != null) {
            for (File file : folder.listFiles()) {
                try {
                    if (file.isDirectory() || !file.getName().endsWith(".json")) continue;
                    HashMap<String, String> json = gson.fromJson(FileUtils.readFileToString(file), HashMap.class);
                    if (json == null) continue;

                    Login.AltTypes altType = Login.AltTypes.Session;
                    if (json.containsKey("cookie")) altType = Login.AltTypes.Cookie;
                    if (json.containsKey("refreshTokenLogin")) altType = Login.AltTypes.Refresh_Token;
                    else if (json.containsKey("refreshToken")) altType = Login.AltTypes.Microsoft;

                    alts.add(new Login.Alt(json.get("name"), json.get("uuid"), altType, json));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void drawScreen(int mX, int mY, float partialTicks) {
        int screenWidth = C.res().getScaledWidth();
        int screenHeight = C.res().getScaledHeight();

        // 1. Background
        MainMenuScreen.drawBackground();

        // 2. Alt Cards Content Area (rendered with Scissor test)
        drawAltGrid(mX, mY, screenWidth, screenHeight);

        // 3. Top Header Bar over Alt Grid
        drawHeader(mX, mY, screenWidth);

        // 4. Left Sidebar Navigation
        drawSidebar(mX, mY, screenHeight);

        // 5. Toast notification banner
        drawToast(screenWidth, screenHeight);

        // 6. Modal dialog if active
        if (activeModal != InputModalType.NONE) {
            drawModal(mX, mY, screenWidth, screenHeight);
        }

        super.drawScreen(mX, mY, partialTicks);
    }

    private void drawSidebar(int mX, int mY, int screenHeight) {
        // Sidebar Glass background
        RenderUtil.drawBlurRect(0, 0, SIDEBAR_WIDTH, screenHeight, 8);
        RenderUtil.drawRect(0, 0, SIDEBAR_WIDTH, screenHeight, new Color(20, 20, 24, 210));
        RenderUtil.drawRect(SIDEBAR_WIDTH - 1, 0, 1, screenHeight, new Color(255, 255, 255, 20));

        // Title
        FontUtil.drawString("ALT MANAGER", 16, 16, 18, new Color(255, 255, 255, 240), true);
        FontUtil.drawString("Manage & switch your accounts", 16, 32, 7, new Color(150, 150, 160, 200), true);

        // Separator
        RenderUtil.drawRect(16, 46, SIDEBAR_WIDTH - 32, 1, new Color(255, 255, 255, 25));

        // Category: Quick Add
        FontUtil.drawString("LOGIN & ADD", 16, 56, 8, new Color(120, 120, 140, 220), true);

        float btnY = 70;
        float btnHeight = 24;
        float btnGap = 5;
        float btnWidth = SIDEBAR_WIDTH - 32;
        float btnX = 16;

        // Button: Refresh Token (Input / Clipboard)
        if (drawModernButton(btnX, btnY, btnWidth, btnHeight, "Refresh Token", "Direct input or token", new Color(0, 180, 216), mX, mY)) {
            openInputModal(InputModalType.REFRESH_TOKEN, "Input Microsoft Refresh Token", "Paste your OAuth refresh token here:");
        }
        btnY += btnHeight + btnGap;

        // Button: Access Token (Session)
        if (drawModernButton(btnX, btnY, btnWidth, btnHeight, "Access Token", "Direct input or session", new Color(239, 71, 111), mX, mY)) {
            openInputModal(InputModalType.ACCESS_TOKEN, "Input Minecraft Access Token", "Paste Bearer / Session access token:");
        }
        btnY += btnHeight + btnGap;

        // Button: Cookie File Picker
        if (drawModernButton(btnX, btnY, btnWidth, btnHeight, "Cookie File", "Native file dialog", new Color(6, 214, 160), mX, mY)) {
            FileChooserUtil.openFilePicker("Select Cookie File", file -> {
                if (file != null) {
                    try {
                        Login.loginCookie(file);
                        setStatus("Loading cookies from " + file.getName(), false);
                    } catch (Exception e) {
                        setStatus("Cookie error: " + e.getMessage(), true);
                    }
                }
            });
        }
        btnY += btnHeight + btnGap;

        // Button: Cookie Text
        if (drawModernButton(btnX, btnY, btnWidth, btnHeight, "Cookie (Text)", "Paste cookie string", new Color(42, 157, 143), mX, mY)) {
            openInputModal(InputModalType.COOKIE_TEXT, "Input Cookie String", "Paste exported cookies (Netscape / raw):");
        }
        btnY += btnHeight + btnGap;

        // Button: Microsoft Browser OAuth
        if (drawModernButton(btnX, btnY, btnWidth, btnHeight, "Browser OAuth", "Official Microsoft login", new Color(17, 138, 178), mX, mY)) {
            Login.loginMicrosoft();
            setStatus("Opened Microsoft Login in browser", false);
        }
        btnY += btnHeight + btnGap;

        // Button: Cracked Account
        if (drawModernButton(btnX, btnY, btnWidth, btnHeight, "Cracked / Offline", "Offline username", new Color(247, 127, 0), mX, mY)) {
            openInputModal(InputModalType.CRACKED, "Add Cracked Account", "Enter desired username (offline):");
        }
        btnY += btnHeight + 14;

        // Separator
        RenderUtil.drawRect(16, btnY, SIDEBAR_WIDTH - 32, 1, new Color(255, 255, 255, 25));
        btnY += 8;

        // Tools Section
        FontUtil.drawString("TOOLS", 16, btnY, 8, new Color(120, 120, 140, 220), true);
        btnY += 12;

        // Open Alts Folder
        if (drawModernButton(btnX, btnY, btnWidth, btnHeight, "Open Alts Folder", "Open JSON directory", new Color(130, 130, 140), mX, mY)) {
            try {
                Desktop.getDesktop().open(Login.altsPath.toFile());
                setStatus("Opened alts directory", false);
            } catch (Exception e) {
                setStatus("Could not open folder: " + e.getMessage(), true);
            }
        }
        btnY += btnHeight + btnGap;

        // Random Alt
        if (drawModernButton(btnX, btnY, btnWidth, btnHeight, "Random Alt", "Login to random alt", new Color(160, 100, 220), mX, mY)) {
            if (!alts.isEmpty()) {
                Login.Alt randomAlt = alts.get(new Random().nextInt(alts.size()));
                loginAlt(randomAlt);
            } else {
                setStatus("No alts available to pick randomly!", true);
            }
        }
        btnY += btnHeight + btnGap;

        // Reload Alts
        if (drawModernButton(btnX, btnY, btnWidth, btnHeight, "Reload List", "Rescan JSON alts", new Color(100, 180, 100), mX, mY)) {
            loadAlts();
            setStatus("Reloaded " + alts.size() + " accounts", false);
        }

        // Back button at bottom
        float backBtnY = screenHeight - 34;
        if (drawModernButton(btnX, backBtnY, btnWidth, 24, "← Back to Menu", "Return to main screen", new Color(230, 80, 80), mX, mY)) {
            C.mc.displayGuiScreen(new MainMenuScreen());
        }
    }

    private void drawHeader(int mX, int mY, int screenWidth) {
        float headerX = SIDEBAR_WIDTH;
        float headerW = screenWidth - SIDEBAR_WIDTH;

        // Blur & Glass bar
        RenderUtil.drawBlurRect(headerX, 0, headerW, HEADER_HEIGHT, 8);
        RenderUtil.drawRect(headerX, 0, headerW, HEADER_HEIGHT, new Color(22, 22, 26, 210));
        RenderUtil.drawRect(headerX, HEADER_HEIGHT - 1, headerW, 1, new Color(255, 255, 255, 20));

        // Search Field Box
        searchField.xPosition = (int) headerX + 16;
        searchField.yPosition = 12;
        searchField.width = Math.min(220, (int) headerW / 3);
        searchField.height = 22;

        RenderUtil.drawRoundedRect(searchField.xPosition - 2, searchField.yPosition - 2, searchField.width + 4, searchField.height + 4, 4, new Color(14, 14, 18, 180));
        RenderUtil.drawRoundedRectOutline(searchField.xPosition - 2, searchField.yPosition - 2, searchField.width + 4, searchField.height + 4, 4, 1,
                searchField.isFocused() ? new Color(0, 180, 216, 180) : new Color(255, 255, 255, 30));

        searchField.drawTextBox();

        if (searchField.getText().isEmpty() && !searchField.isFocused()) {
            FontUtil.drawString("Search alts by name or uuid...", searchField.xPosition + 4, searchField.yPosition + 6, 8, new Color(120, 120, 130, 180), true);
        }

        // Active Session Badge on Right
        String currentName = C.mc.getSession() != null ? C.mc.getSession().getUsername() : "Unknown";
        String currentSessionText = "Current: " + currentName;
        float currentW = FontUtil.getStringWidth(currentSessionText, 9) + 20;
        float currentX = screenWidth - currentW - 16;
        float currentY = 12;

        RenderUtil.drawRoundedRect(currentX, currentY, currentW, 22, 4, new Color(40, 167, 69, 40));
        RenderUtil.drawRoundedRectOutline(currentX, currentY, currentW, 22, 4, 1, new Color(40, 167, 69, 140));
        FontUtil.drawCenteredString(currentSessionText, currentX + currentW / 2f, currentY + 6, 9, new Color(99, 255, 102), true);

        // Total count badge
        String countText = alts.size() + " " + (alts.size() == 1 ? "Account" : "Accounts");
        float countW = FontUtil.getStringWidth(countText, 9) + 16;
        float countX = currentX - countW - 10;

        RenderUtil.drawRoundedRect(countX, currentY, countW, 22, 4, new Color(30, 30, 36, 180));
        RenderUtil.drawRoundedRectOutline(countX, currentY, countW, 22, 4, 1, new Color(255, 255, 255, 30));
        FontUtil.drawCenteredString(countText, countX + countW / 2f, currentY + 6, 9, new Color(200, 200, 210), true);
    }

    private void drawAltGrid(int mX, int mY, int screenWidth, int screenHeight) {
        float startX = SIDEBAR_WIDTH + 16;
        float startY = HEADER_HEIGHT + 14;
        float availableWidth = screenWidth - startX - 16;
        float availableHeight = screenHeight - startY - 10;

        if (availableWidth <= 50 || availableHeight <= 50) return;

        // Card measurements
        float cardWidth = 240;
        float cardHeight = 64;
        float gap = 12;

        int columns = Math.max(1, (int) ((availableWidth + gap) / (cardWidth + gap)));
        // Adjust cardWidth to fit columns evenly
        cardWidth = (availableWidth - (columns - 1) * gap) / columns;

        List<Login.Alt> filteredAlts = getFilteredAlts();

        int totalRows = (int) Math.ceil((double) filteredAlts.size() / columns);
        float contentHeight = totalRows * (cardHeight + gap);
        maxScroll = Math.max(0, contentHeight - availableHeight);

        // Smooth scroll damping
        renderedScroll += (scrollAmount - renderedScroll) / Math.max(Minecraft.getDebugFPS() * 0.15f, 2f);

        // Scissor area
        RenderUtil.glScissor(SIDEBAR_WIDTH, HEADER_HEIGHT, screenWidth - SIDEBAR_WIDTH, availableHeight + 10);

        GL11.glPushMatrix();
        GL11.glTranslated(0, -renderedScroll, 0);

        Login.Alt hoveredAltForTooltip = null;
        String tooltipText = null;

        for (int i = 0; i < filteredAlts.size(); i++) {
            Login.Alt alt = filteredAlts.get(i);
            int row = i / columns;
            int col = i % columns;

            float cardX = startX + col * (cardWidth + gap);
            float cardY = startY + row * (cardHeight + gap);

            // Culling optimization: don't render off-screen cards
            if (cardY + cardHeight - renderedScroll < HEADER_HEIGHT || cardY - renderedScroll > screenHeight) {
                continue;
            }

            boolean isCurrent = alt.uuid != null && C.mc.getSession() != null && alt.uuid.equals(C.mc.getSession().getPlayerID());
            boolean banned = isBanned(alt);
            boolean nameChangeAllowed = canNameChange(alt);

            // Card Hover check (taking scroll into account)
            boolean cardHovered = ScreenUtil.isMouseOver(cardX, cardY - renderedScroll, cardWidth, cardHeight, mX, mY);

            // Card Background
            Color cardBg = isCurrent
                    ? new Color(30, 60, 40, 170)
                    : (cardHovered ? new Color(35, 35, 45, 190) : new Color(25, 25, 32, 160));
            Color cardBorder = isCurrent
                    ? new Color(99, 255, 102, 180)
                    : (cardHovered ? new Color(0, 180, 216, 120) : new Color(255, 255, 255, 25));

            RenderUtil.drawBlurRect(cardX, cardY, cardWidth, cardHeight, 4);
            RenderUtil.drawRoundedRect(cardX, cardY, cardWidth, cardHeight, 6, cardBg);
            RenderUtil.drawRoundedRectOutline(cardX, cardY, cardWidth, cardHeight, 6, 1, cardBorder);

            // Head Avatar
            DynamicTexture headTexture = new DynamicTexture(alt.getHead());
            float headSize = 36;
            float headX = cardX + 10;
            float headY = cardY + (cardHeight - headSize) / 2f;

            boolean headHovered = ScreenUtil.isMouseOver(headX, headY - renderedScroll, headSize, headSize, mX, mY);
            RenderUtil.drawRoundedRect(headX - 1, headY - 1, headSize + 2, headSize + 2, 4, new Color(0, 0, 0, 80));
            RenderUtil.drawRectTextured(headX, headY, headSize, headSize, headHovered ? new Color(200, 240, 255) : Color.WHITE, headTexture.getGlTextureId());
            RenderUtil.drawRoundedRectOutline(headX - 1, headY - 1, headSize + 2, headSize + 2, 4, 1,
                    headHovered ? new Color(0, 180, 216, 200) : new Color(255, 255, 255, 30));

            // Alt Name
            float textStartX = headX + headSize + 10;
            float nameY = cardY + 10;
            Color nameColor = isCurrent ? new Color(99, 255, 102) : (cardHovered ? Color.WHITE : new Color(225, 225, 235));
            FontUtil.drawString(alt.name, textStartX, nameY, 13, nameColor, true);

            // Status Badges (Type, Ban, Name)
            float badgeY = cardY + 30;
            float badgeX = textStartX;

            // Type Badge
            String typeName = alt.type.name;
            float typeW = FontUtil.getStringWidth(typeName, 6) + 8;
            RenderUtil.drawRoundedRect(badgeX, badgeY, typeW, 11, 2, new Color(40, 40, 50, 180));
            RenderUtil.drawRoundedRectOutline(badgeX, badgeY, typeW, 11, 2, 1, new Color(255, 255, 255, 20));
            FontUtil.drawString(alt.type.colour + typeName, badgeX + 4, badgeY + 2, 6, Color.WHITE, true);
            badgeX += typeW + 4;

            // Ban Badge
            String banStr = banned ? "§cBanned" : "§aUnbanned";
            float banW = FontUtil.getStringWidth(banned ? "Banned" : "Unbanned", 6) + 8;
            RenderUtil.drawRoundedRect(badgeX, badgeY, banW, 11, 2, banned ? new Color(180, 40, 40, 40) : new Color(40, 180, 60, 40));
            RenderUtil.drawRoundedRectOutline(badgeX, badgeY, banW, 11, 2, 1, banned ? new Color(220, 50, 50, 140) : new Color(50, 200, 70, 140));
            FontUtil.drawString(banStr, badgeX + 4, badgeY + 2, 6, Color.WHITE, true);
            badgeX += banW + 4;

            // Name Change Badge
            if (alt.json.containsKey("nameChangeDate")) {
                String nameStr = nameChangeAllowed ? "§aRenameable" : "§cRename Cooldown";
                float nameW = FontUtil.getStringWidth(nameChangeAllowed ? "Renameable" : "Rename Cooldown", 6) + 8;
                RenderUtil.drawRoundedRect(badgeX, badgeY, nameW, 11, 2, new Color(30, 30, 38, 180));
                RenderUtil.drawRoundedRectOutline(badgeX, badgeY, nameW, 11, 2, 1, new Color(255, 255, 255, 20));
                FontUtil.drawString(nameStr, badgeX + 4, badgeY + 2, 6, Color.WHITE, true);
            }

            // UUID / Subtext
            float uuidY = cardY + 46;
            String uuidPreview = alt.uuid != null && alt.uuid.length() > 14 ? alt.uuid.substring(0, 14) + "..." : (alt.uuid == null ? "No UUID" : alt.uuid);
            FontUtil.drawString("§7UUID: " + uuidPreview, textStartX, uuidY, 6, new Color(140, 140, 150), true);

            // Action Buttons: Delete & Rename (Right side of card)
            float actionIconSize = 14;
            float binX = cardX + cardWidth - actionIconSize - 8;
            float binY = cardY + (cardHeight - actionIconSize) / 2f;
            boolean binHovered = ScreenUtil.isMouseOver(binX, binY - renderedScroll, actionIconSize, actionIconSize, mX, mY);

            if (binTexture != null) {
                RenderUtil.drawRoundedRect(binX - 2, binY - 2, actionIconSize + 4, actionIconSize + 4, 3, binHovered ? new Color(220, 50, 50, 120) : new Color(0, 0, 0, 60));
                RenderUtil.drawRectTextured(binX, binY, actionIconSize, actionIconSize, binHovered ? Color.WHITE : new Color(180, 180, 180), binTexture.getGlTextureId());
            }

            float pencilX = binX - actionIconSize - 6;
            float pencilY = binY;
            boolean pencilHovered = ScreenUtil.isMouseOver(pencilX, pencilY - renderedScroll, actionIconSize, actionIconSize, mX, mY);

            if (pencilTexture != null) {
                RenderUtil.drawRoundedRect(pencilX - 2, pencilY - 2, actionIconSize + 4, actionIconSize + 4, 3, pencilHovered ? new Color(0, 180, 216, 120) : new Color(0, 0, 0, 60));
                RenderUtil.drawRectTextured(pencilX, pencilY, actionIconSize, actionIconSize, pencilHovered ? Color.WHITE : new Color(180, 180, 180), pencilTexture.getGlTextureId());
            }

            // Click Handlers
            if (activeModal == InputModalType.NONE && mouseClickedThisFrame) {
                if (binHovered) {
                    deleteAlt(alt);
                    mouseClickedThisFrame = false;
                } else if (pencilHovered) {
                    if (nameChangeAllowed) {
                        targetAltForModal = alt;
                        openInputModal(InputModalType.RENAME, "Change In-Game Name for " + alt.name, "Enter new Minecraft username:");
                    } else {
                        setStatus("Cannot rename " + alt.name + " yet: name change cooldown is active!", true);
                    }
                    mouseClickedThisFrame = false;
                } else if (headHovered) {
                    // Change skin from clipboard
                    try {
                        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        String data = (String) clipboard.getData(DataFlavor.stringFlavor);
                        if (data != null && (data.startsWith("http://") || data.startsWith("https://"))) {
                            setStatus(SessionUtil.changeSkin(C.mc.getSession().getToken(), data), false);
                        } else {
                            setStatus("Clipboard doesn't contain a valid skin URL!", true);
                        }
                    } catch (Exception ex) {
                        setStatus("Invalid clipboard data for skin change", true);
                    }
                    mouseClickedThisFrame = false;
                } else if (cardHovered) {
                    loginAlt(alt);
                    mouseClickedThisFrame = false;
                }
            }

            headTexture.deleteGlTexture();
        }

        if (filteredAlts.isEmpty()) {
            float emptyY = startY + availableHeight / 2f - 20;
            FontUtil.drawCenteredString("No accounts found", startX + availableWidth / 2f, emptyY, 14, new Color(160, 160, 170), true);
            FontUtil.drawCenteredString("Add an account from the sidebar or drop cookie files", startX + availableWidth / 2f, emptyY + 16, 8, new Color(110, 110, 120), true);
        }

        GL11.glPopMatrix();
        RenderUtil.disableScissor();

        // Mouse wheel scrolling
        int dWheel = Mouse.getDWheel();
        if (dWheel != 0) {
            scrollAmount -= (dWheel / 120f) * 28f;
            scrollAmount = Math.max(0, Math.min(scrollAmount, maxScroll));
        }
    }

    private boolean drawModernButton(float x, float y, float w, float h, String text, String subtitle, Color accent, int mX, int mY) {
        boolean hovered = ScreenUtil.isMouseOver(x, y, w, h, mX, mY);

        Color bgColor = hovered ? new Color(38, 40, 52, 220) : new Color(26, 28, 36, 180);
        Color borderColor = hovered ? accent : new Color(255, 255, 255, 20);

        RenderUtil.drawBlurRect(x, y, w, h, 2);
        RenderUtil.drawRoundedRect(x, y, w, h, 4, bgColor);
        RenderUtil.drawRoundedRectOutline(x, y, w, h, 4, 1, borderColor);

        // Accent strip on the left
        RenderUtil.drawRoundedRect(x + 2, y + 3, 3, h - 6, 2, hovered ? accent : new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 120));

        FontUtil.drawString(text, x + 10, y + 4, 9, hovered ? Color.WHITE : new Color(220, 220, 230), true);
        if (subtitle != null && !subtitle.isEmpty()) {
            FontUtil.drawString(subtitle, x + 10, y + 14, 6, new Color(130, 130, 145), true);
        }

        if (hovered && mouseClickedThisFrame && activeModal == InputModalType.NONE) {
            mouseClickedThisFrame = false;
            return true;
        }

        return false;
    }

    private void drawToast(int screenWidth, int screenHeight) {
        if (statusMessage.isEmpty() || System.currentTimeMillis() - statusTimestamp > 4500) {
            return;
        }

        float alpha = 1f;
        long elapsed = System.currentTimeMillis() - statusTimestamp;
        if (elapsed > 3500) {
            alpha = 1f - (elapsed - 3500) / 1000f;
        }

        int alphaInt = (int) (Math.max(0, Math.min(1, alpha)) * 255);
        if (alphaInt <= 5) return;

        float toastW = Math.min(500, FontUtil.getStringWidth(statusMessage, 9) + 36);
        float toastH = 26;
        float toastX = screenWidth / 2f - toastW / 2f;
        float toastY = screenHeight - toastH - 12;

        Color bg = isStatusError ? new Color(180, 30, 30, (int) (alphaInt * 0.85f)) : new Color(30, 120, 60, (int) (alphaInt * 0.85f));
        Color border = isStatusError ? new Color(255, 100, 100, alphaInt) : new Color(100, 255, 150, alphaInt);

        RenderUtil.drawBlurRect(toastX, toastY, toastW, toastH, 6);
        RenderUtil.drawRoundedRect(toastX, toastY, toastW, toastH, 5, bg);
        RenderUtil.drawRoundedRectOutline(toastX, toastY, toastW, toastH, 5, 1, border);

        FontUtil.drawCenteredString(statusMessage, toastX + toastW / 2f, toastY + 8, 9, new Color(255, 255, 255, alphaInt), true);
    }

    private void openInputModal(InputModalType type, String title, String subtitle) {
        activeModal = type;
        modalTitle = title;
        modalSubtitle = subtitle;
        modalInput.setText("");
        modalInput.setFocused(true);
    }

    private String modalTitle = "";
    private String modalSubtitle = "";

    private void drawModal(int mX, int mY, int screenWidth, int screenHeight) {
        // Darken backdrop
        RenderUtil.drawRect(0, 0, screenWidth, screenHeight, new Color(0, 0, 0, 160));

        float modalW = Math.min(440, screenWidth - 40);
        float modalH = 160;
        float modalX = (screenWidth - modalW) / 2f;
        float modalY = (screenHeight - modalH) / 2f;

        RenderUtil.drawBlurRect(modalX, modalY, modalW, modalH, 10);
        RenderUtil.drawRoundedRect(modalX, modalY, modalW, modalH, 8, new Color(22, 22, 28, 240));
        RenderUtil.drawRoundedRectOutline(modalX, modalY, modalW, modalH, 8, 1, new Color(0, 180, 216, 180));

        // Header
        FontUtil.drawString(modalTitle, modalX + 16, modalY + 16, 12, Color.WHITE, true);
        FontUtil.drawString(modalSubtitle, modalX + 16, modalY + 32, 7, new Color(160, 160, 175), true);

        // Text Field Box
        modalInput.xPosition = (int) modalX + 16;
        modalInput.yPosition = (int) modalY + 54;
        modalInput.width = (int) modalW - 32;
        modalInput.height = 24;

        RenderUtil.drawRoundedRect(modalInput.xPosition - 2, modalInput.yPosition - 2, modalInput.width + 4, modalInput.height + 4, 4, new Color(12, 12, 16, 200));
        RenderUtil.drawRoundedRectOutline(modalInput.xPosition - 2, modalInput.yPosition - 2, modalInput.width + 4, modalInput.height + 4, 4, 1,
                modalInput.isFocused() ? new Color(0, 180, 216, 220) : new Color(255, 255, 255, 40));

        modalInput.drawTextBox();

        if (modalInput.getText().isEmpty() && !modalInput.isFocused()) {
            FontUtil.drawString("Type or paste here...", modalInput.xPosition + 6, modalInput.yPosition + 7, 8, new Color(100, 100, 110), true);
        }

        // Buttons: Confirm, Paste Clipboard, Cancel
        float btnW = 90;
        float btnH = 24;
        float btnY = modalY + modalH - btnH - 16;

        // Confirm
        float confirmX = modalX + modalW - btnW - 16;
        boolean confirmHover = ScreenUtil.isMouseOver(confirmX, btnY, btnW, btnH, mX, mY);
        RenderUtil.drawRoundedRect(confirmX, btnY, btnW, btnH, 4, confirmHover ? new Color(0, 180, 216, 220) : new Color(0, 150, 180, 180));
        RenderUtil.drawRoundedRectOutline(confirmX, btnY, btnW, btnH, 4, 1, new Color(255, 255, 255, 60));
        FontUtil.drawCenteredString("Submit (Enter)", confirmX + btnW / 2f, btnY + 7, 8, Color.WHITE, true);

        // Paste from Clipboard
        float pasteX = confirmX - btnW - 10;
        boolean pasteHover = ScreenUtil.isMouseOver(pasteX, btnY, btnW, btnH, mX, mY);
        RenderUtil.drawRoundedRect(pasteX, btnY, btnW, btnH, 4, pasteHover ? new Color(60, 65, 80, 220) : new Color(40, 45, 55, 180));
        RenderUtil.drawRoundedRectOutline(pasteX, btnY, btnW, btnH, 4, 1, new Color(255, 255, 255, 30));
        FontUtil.drawCenteredString("Paste Clipboard", pasteX + btnW / 2f, btnY + 7, 8, Color.WHITE, true);

        // Cancel
        float cancelX = modalX + 16;
        boolean cancelHover = ScreenUtil.isMouseOver(cancelX, btnY, btnW, btnH, mX, mY);
        RenderUtil.drawRoundedRect(cancelX, btnY, btnW, btnH, 4, cancelHover ? new Color(200, 60, 60, 200) : new Color(140, 40, 40, 160));
        RenderUtil.drawRoundedRectOutline(cancelX, btnY, btnW, btnH, 4, 1, new Color(255, 255, 255, 30));
        FontUtil.drawCenteredString("Cancel (Esc)", cancelX + btnW / 2f, btnY + 7, 8, Color.WHITE, true);

        if (mouseClickedThisFrame) {
            if (confirmHover) {
                submitModal();
                mouseClickedThisFrame = false;
            } else if (pasteHover) {
                pasteClipboardToModal();
                mouseClickedThisFrame = false;
            } else if (cancelHover) {
                closeModal();
                mouseClickedThisFrame = false;
            }
        }
    }

    private void pasteClipboardToModal() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                String data = (String) clipboard.getData(DataFlavor.stringFlavor);
                if (data != null) {
                    modalInput.setText(data.trim());
                }
            }
        } catch (Exception e) {
            setStatus("Could not read clipboard: " + e.getMessage(), true);
        }
    }

    private void submitModal() {
        String input = modalInput.getText().trim();
        if (input.isEmpty()) {
            setStatus("Input cannot be empty!", true);
            return;
        }

        switch (activeModal) {
            case REFRESH_TOKEN:
                Login.loginRefreshToken(input);
                setStatus("Authenticating Refresh Token...", false);
                break;
            case ACCESS_TOKEN:
                if (Login.loginSession(input)) {
                    setStatus("Successfully logged in with Access Token!", false);
                } else {
                    setStatus("Invalid Minecraft Access Token!", true);
                }
                break;
            case COOKIE_TEXT:
                Login.loginCookie(input);
                setStatus("Authenticating Cookie...", false);
                break;
            case CRACKED:
                Login.addCracked(input);
                setStatus("Set cracked account to: " + input, false);
                break;
            case RENAME:
                if (targetAltForModal != null && targetAltForModal.json.containsKey("session")) {
                    String result = SessionUtil.changeName(targetAltForModal.json.get("session"), input);
                    setStatus(result, result.contains("Cannot") || result.contains("failed") || result.contains("invalid"));
                    loadAlts();
                } else {
                    setStatus("Cannot rename account without valid session token!", true);
                }
                break;
        }

        closeModal();
    }

    private void closeModal() {
        activeModal = InputModalType.NONE;
        targetAltForModal = null;
        modalInput.setText("");
    }

    private void loginAlt(Login.Alt alt) {
        setStatus("Logging into " + alt.name + "...", false);

        if (alt.json.containsKey("session") && Login.loginSession(alt.json.get("session"))) {
            setStatus("Logged into " + alt.name + " using Session!", false);
            return;
        }

        switch (alt.type) {
            case Refresh_Token:
                if (alt.json.containsKey("refreshToken")) {
                    Login.loginRefreshToken(alt.json.get("refreshToken"));
                } else {
                    setStatus("No refresh token stored for " + alt.name, true);
                }
                break;
            case Microsoft:
                if (alt.json.containsKey("refreshToken")) {
                    Login.loginMicrosoft(alt.json.get("refreshToken"));
                } else {
                    setStatus("No OAuth token stored for " + alt.name, true);
                }
                break;
            case Cookie:
                if (alt.json.containsKey("cookie")) {
                    Login.loginCookie(alt.json.get("cookie"));
                } else {
                    setStatus("No cookie data stored for " + alt.name, true);
                }
                break;
            case Session:
                if (alt.json.containsKey("session")) {
                    Login.loginSession(alt.json.get("session"));
                }
                break;
            case Cracked:
                Login.addCracked(alt.name);
                break;
        }
    }

    private void deleteAlt(Login.Alt alt) {
        try {
            Files.deleteIfExists(Login.getAccountPath(alt.uuid));
            alts.remove(alt);
            Files.deleteIfExists(Paths.get(Login.altsPath + "/skins/" + alt.uuid + ".png"));
            setStatus("Removed account: " + alt.name, false);
        } catch (Exception e) {
            setStatus("Failed to delete account: " + e.getMessage(), true);
        }
    }

    private List<Login.Alt> getFilteredAlts() {
        String query = searchField != null ? searchField.getText().trim().toLowerCase() : "";
        if (query.isEmpty()) return alts;

        List<Login.Alt> list = new ArrayList<>();
        for (Login.Alt alt : alts) {
            if (alt.name.toLowerCase().contains(query) || (alt.uuid != null && alt.uuid.toLowerCase().contains(query))) {
                list.add(alt);
            }
        }
        return list;
    }

    private boolean isBanned(Login.Alt alt) {
        if (alt.json.containsKey("unbanDate")) {
            String unbanDate = alt.json.get("unbanDate");
            if (unbanDate.equals("now")) return false;
            if (unbanDate.equals("never")) return true;
            try {
                return Instant.parse(unbanDate).isAfter(Instant.now());
            } catch (Exception ignored) {}
        }
        return false;
    }

    private boolean canNameChange(Login.Alt alt) {
        if (alt.json.containsKey("nameChangeDate")) {
            String date = alt.json.get("nameChangeDate");
            if (date.equals("ALLOWED")) return true;
            if (date.isEmpty() || date.contains("path")) return true;
            try {
                return Instant.now().isAfter(Instant.parse(date).plusSeconds(2592000));
            } catch (Exception ignored) {}
        }
        return true;
    }

    private boolean mouseClickedThisFrame = false;

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0) {
            mouseClickedThisFrame = true;
        }

        if (activeModal != InputModalType.NONE) {
            modalInput.mouseClicked(mouseX, mouseY, mouseButton);
        } else {
            searchField.mouseClicked(mouseX, mouseY, mouseButton);
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        mouseClickedThisFrame = false;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (activeModal != InputModalType.NONE) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                closeModal();
                return;
            }
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                submitModal();
                return;
            }
            modalInput.textboxKeyTyped(typedChar, keyCode);
            return;
        }

        if (searchField.isFocused()) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                searchField.setFocused(false);
                return;
            }
            searchField.textboxKeyTyped(typedChar, keyCode);
            return;
        }

        if (keyCode == Keyboard.KEY_ESCAPE) {
            C.mc.displayGuiScreen(new MainMenuScreen());
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (searchField != null) searchField.updateCursorCounter();
        if (modalInput != null) modalInput.updateCursorCounter();
    }
}
