package coolware.gui.altmanager;

import coolware.alts.Alt;
import coolware.alts.AltManager;
import coolware.alts.AltType;
import coolware.font.FontManager;
import coolware.gui.menu.ParticleSystem;
import coolware.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiAltManager extends GuiScreen {

    private final GuiScreen parentScreen;
    private AltManager altManager;
    private final ParticleSystem particleSystem = new ParticleSystem();

    // Search Bar
    private GuiTextField searchField;

    // Scrolling state
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private boolean isDraggingScrollbar = false;
    private int dragScrollStartY = 0;
    private int initialScrollOffset = 0;

    // Selected Alt
    private Alt selectedAlt = null;

    // Active Modal Sub-view
    public enum ModalType {
        NONE,
        CRACKED,
        SESSION_TOKEN,
        REFRESH_TOKEN,
        COOKIE
    }
    private ModalType activeModal = ModalType.NONE;
    private GuiTextField modalField;
    private String modalError = null;

    // Premium Color Palette
    private static final int C_BG_START    = 0xFF070509;
    private static final int C_BG_END      = 0xFF10070B;
    private static final int C_PANEL_BG    = 0xD90D0911;
    private static final int C_ROW_BG      = 0xFF131018;
    private static final int C_ROW_HOVER   = 0xFF1A1522;
    private static final int C_ROW_SEL     = 0xFF24141E;
    private static final int C_BORDER      = 0xFF221E26;
    private static final int C_BORDER_HOV  = 0xFF3D3744;
    private static final int C_RED_PRIM    = 0xFFE52B3F;
    private static final int C_RED_DARK    = 0xFF65151F;
    private static final int C_RED_BRIGHT  = 0xFFFF3D52;
    private static final int C_NORM_TEXT   = 0xFFD8D4DA;
    private static final int C_MUTED_TEXT  = 0xFFA8A2AA;
    private static final int C_BRIGHT_TXT  = 0xFFF2EFF3;

    public GuiAltManager(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.altManager = AltManager.getInstance();
        this.buttonList.clear();

        int panelWidth = Math.min(680, this.width - 40);
        int startTabsX = (this.width - panelWidth) / 2 + 10;
        int tabsY = 62;
        int tabHeight = 30;
        int tabSpacing = 8;
        int btnWidth = (panelWidth - 20 - (tabSpacing * 4)) / 5;

        // 1. Five Login Method Tabs with specific SVG vector icons (user, clipboard-user, refresh, microsoft, cookie-bite)
        ModernAltButton btnCracked = new ModernAltButton(1, startTabsX, tabsY, btnWidth, tabHeight, ModernAltButton.IconType.USER, "Cracked", ModernAltButton.Style.TAB);
        ModernAltButton btnSession = new ModernAltButton(2, startTabsX + (btnWidth + tabSpacing), tabsY, btnWidth, tabHeight, ModernAltButton.IconType.CLIPBOARD_USER, "Session ID", ModernAltButton.Style.TAB);
        ModernAltButton btnRefresh = new ModernAltButton(3, startTabsX + (btnWidth + tabSpacing) * 2, tabsY, btnWidth, tabHeight, ModernAltButton.IconType.REFRESH, "Refresh Token", ModernAltButton.Style.TAB);
        ModernAltButton btnMicrosoft = new ModernAltButton(4, startTabsX + (btnWidth + tabSpacing) * 3, tabsY, btnWidth, tabHeight, ModernAltButton.IconType.MICROSOFT, "Microsoft", ModernAltButton.Style.TAB);
        ModernAltButton btnCookie = new ModernAltButton(5, startTabsX + (btnWidth + tabSpacing) * 4, tabsY, btnWidth, tabHeight, ModernAltButton.IconType.COOKIE_BITE, "Cookie Login", ModernAltButton.Style.TAB);

        btnCracked.setActive(activeModal == ModalType.CRACKED);
        btnSession.setActive(activeModal == ModalType.SESSION_TOKEN);
        btnRefresh.setActive(activeModal == ModalType.REFRESH_TOKEN);
        btnCookie.setActive(activeModal == ModalType.COOKIE);

        this.buttonList.add(btnCracked);
        this.buttonList.add(btnSession);
        this.buttonList.add(btnRefresh);
        this.buttonList.add(btnMicrosoft);
        this.buttonList.add(btnCookie);

        // 2. Search Bar below Tabs
        int searchY = tabsY + tabHeight + 12;
        int searchWidth = Math.min(460, panelWidth - 40);
        int searchHeight = 34;
        int searchX = (this.width - searchWidth) / 2;

        String prevSearchText = this.searchField != null ? this.searchField.getText() : "";
        boolean prevSearchFocus = this.searchField != null && this.searchField.isFocused();

        this.searchField = new GuiTextField(100, this.fontRendererObj, searchX + 34, searchY + 11, searchWidth - 46, 14);
        this.searchField.setMaxStringLength(48);
        this.searchField.setCanLoseFocus(true);
        this.searchField.setEnableBackgroundDrawing(false);
        this.searchField.setTextColor(0xFFE0E0E0);
        this.searchField.setText(prevSearchText);
        this.searchField.setFocused(prevSearchFocus);

        // 3. Bottom Action Bar Buttons
        int bottomBtnW = 120;
        int bottomBtnH = 32;
        int bottomSpacing = 12;
        int totalBottomW = bottomBtnW * 4 + bottomSpacing * 3;
        int bottomStartX = (this.width - totalBottomW) / 2;
        int bottomY = this.height - 44;

        this.buttonList.add(new ModernAltButton(10, bottomStartX, bottomY, bottomBtnW, bottomBtnH, ModernAltButton.IconType.USER, "Use Selected", ModernAltButton.Style.NORMAL));
        this.buttonList.add(new ModernAltButton(11, bottomStartX + bottomBtnW + bottomSpacing, bottomY, bottomBtnW, bottomBtnH, ModernAltButton.IconType.DELETE, "Delete", ModernAltButton.Style.DESTRUCTIVE));
        this.buttonList.add(new ModernAltButton(12, bottomStartX + (bottomBtnW + bottomSpacing) * 2, bottomY, bottomBtnW, bottomBtnH, ModernAltButton.IconType.NONE, "Credits", ModernAltButton.Style.NORMAL));
        this.buttonList.add(new ModernAltButton(0, bottomStartX + (bottomBtnW + bottomSpacing) * 3, bottomY, bottomBtnW, bottomBtnH, ModernAltButton.IconType.NONE, "Back", ModernAltButton.Style.NORMAL));

        // 4. Modal Input Field Setup
        int modalW = Math.min(420, this.width - 40);
        int modalFieldW = modalW - 40;
        int modalX = (this.width - modalW) / 2;
        int modalY = (this.height - 160) / 2;
        int fieldX = modalX + 20;
        int fieldY = modalY + 58;

        String prevModalText = this.modalField != null ? this.modalField.getText() : "";
        boolean prevModalFocus = this.modalField != null ? this.modalField.isFocused() : (activeModal != ModalType.NONE);

        this.modalField = new GuiTextField(101, this.fontRendererObj, fieldX + 8, fieldY + 10, modalFieldW - 16, 14);
        this.modalField.setMaxStringLength(4096);
        this.modalField.setCanLoseFocus(false);
        this.modalField.setEnableBackgroundDrawing(false);
        this.modalField.setTextColor(0xFFE0E0E0);
        this.modalField.setText(prevModalText);
        if (activeModal != ModalType.NONE) {
            this.modalField.setFocused(true);
        } else {
            this.modalField.setFocused(prevModalFocus);
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        Keyboard.enableRepeatEvents(true);
        if (this.searchField != null) {
            this.searchField.updateCursorCounter();
        }
        if (this.activeModal != ModalType.NONE && this.modalField != null) {
            this.modalField.updateCursorCounter();
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (activeModal != ModalType.NONE && button.id != 200 && button.id != 201) {
            return;
        }

        switch (button.id) {
            case 0:
                // Back to Main Menu
                this.mc.displayGuiScreen(this.parentScreen);
                break;
            case 1:
                // Open Cracked Login Modal
                openModal(ModalType.CRACKED);
                break;
            case 2:
                // Open Session Token Login Modal
                openModal(ModalType.SESSION_TOKEN);
                break;
            case 3:
                // Open Refresh Token Login Modal
                openModal(ModalType.REFRESH_TOKEN);
                break;
            case 4:
                // Direct Microsoft OAuth Browser/Device Flow
                if (altManager != null) {
                    altManager.loginMicrosoft();
                }
                break;
            case 5:
                // Open Cookie Login Modal
                openModal(ModalType.COOKIE);
                break;
            case 10:
                // Use Selected Alt
                if (selectedAlt != null && altManager != null) {
                    altManager.loginAlt(selectedAlt);
                }
                break;
            case 11:
                // Delete Selected Alt
                if (selectedAlt != null && altManager != null) {
                    altManager.getAlts().remove(selectedAlt);
                    altManager.saveAlts();
                    selectedAlt = null;
                }
                break;
            case 12:
                // Open Credits Screen
                this.mc.displayGuiScreen(new GuiCredits(this));
                break;
            case 200:
                // Modal Confirm / Submit
                handleModalSubmit();
                break;
            case 201:
                // Modal Cancel
                closeModal();
                break;
        }
    }

    private void openModal(ModalType type) {
        this.activeModal = type;
        this.modalError = null;
        initGui();  // recreates modalField
        // Set focus AFTER initGui so the freshly-created field gets it
        if (this.modalField != null) {
            this.modalField.setText("");
            this.modalField.setFocused(true);
        }
    }

    private void closeModal() {
        this.activeModal = ModalType.NONE;
        this.modalError = null;
        initGui();
    }

    private void handleModalSubmit() {
        if (modalField == null) return;
        String val = modalField.getText().trim();
        if (val.isEmpty()) {
            modalError = "Input cannot be empty!";
            return;
        }

        if (altManager == null) {
            modalError = "AltManager is not initialized.";
            return;
        }

        switch (activeModal) {
            case CRACKED:
                altManager.loginCracked(val);
                closeModal();
                break;
            case SESSION_TOKEN:
                altManager.loginSessionToken(val);
                closeModal();
                break;
            case REFRESH_TOKEN:
                altManager.loginRefreshToken(val);
                closeModal();
                break;
            case COOKIE:
                altManager.loginCookie(val);
                closeModal();
                break;
            default:
                closeModal();
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // 1. Full Screen Deep Dark Gradient Background
        RenderUtil.drawHorizontalGradient(0, 0, this.width, this.height, C_BG_START, C_BG_END);
        RenderUtil.drawGradientRect(0, 0, this.width, this.height, 0x33000000, 0x88000000);

        // 2. Animated Particle Background Nebula
        this.particleSystem.updateAndRender(this.width, this.height, mouseX, mouseY);

        // 3. Header Title & Brand
        renderHeader();

        // 4. Render Search Bar
        renderSearchBar(mouseX, mouseY);

        // 5. Render Account Cards List View Container
        int panelWidth = Math.min(680, this.width - 40);
        int panelX = (this.width - panelWidth) / 2;
        int listY = 62 + 30 + 12 + 34 + 12;
        int listHeight = this.height - listY - 54;

        renderAccountList(panelX, listY, panelWidth, listHeight, mouseX, mouseY);

        // 6. Draw Standard Buttons (Tabs, Bottom Action Bar)
        super.drawScreen(mouseX, mouseY, partialTicks);

        // 7. Active Account Status Pill (Bottom Left)
        renderActiveAccountPill();

        // 8. Dynamic Status Message / Auth Feedback Pill (Bottom Right)
        renderStatusMessage();

        // 9. Render Modal Overlay if Active
        if (activeModal != ModalType.NONE) {
            renderModal(mouseX, mouseY);
        }
    }

    private void renderHeader() {
        int panelWidth = Math.min(680, this.width - 40);
        float titleX = (this.width - panelWidth) / 2.0F + 10.0F;
        float titleY = 18.0F;

        // Title text "ALT MANAGER" in Bold HD
        if (FontManager.sfBold28 != null) {
            FontManager.sfBold28.drawStringWithShadow("Alt Manager", titleX, titleY, C_BRIGHT_TXT);
        } else if (FontManager.sfBold22 != null) {
            FontManager.sfBold22.drawStringWithShadow("Alt Manager", titleX, titleY, C_BRIGHT_TXT);
        } else {
            this.fontRendererObj.drawStringWithShadow("\u00a7lAccount Manager", (int) titleX, (int) titleY, C_BRIGHT_TXT);
        }

        // Subtitle with account count in Bold SemiBold HD
        int totalAccounts = (altManager != null && altManager.getAlts() != null) ? altManager.getAlts().size() : 0;
        String sub = "Manage and authenticate accounts \u00a78\u00b7 \u00a77" + totalAccounts + " accounts loaded";
        float subY = titleY + 22.0F;

        if (FontManager.sfSemiBold14 != null) {
            FontManager.sfSemiBold14.drawStringWithShadow(sub, titleX, subY, C_MUTED_TEXT);
        } else if (FontManager.sfRegular14 != null) {
            FontManager.sfRegular14.drawStringWithShadow(sub, titleX, subY, C_MUTED_TEXT);
        } else {
            this.fontRendererObj.drawStringWithShadow(sub, (int) titleX, (int) subY, C_MUTED_TEXT);
        }
    }

    private void renderSearchBar(int mouseX, int mouseY) {
        int panelWidth = Math.min(680, this.width - 40);
        int searchWidth = Math.min(460, panelWidth - 40);
        int searchHeight = 34;
        int searchX = (this.width - searchWidth) / 2;
        int searchY = 62 + 30 + 12;

        boolean isFocused = searchField != null && searchField.isFocused();
        int bgCol = isFocused ? 0xFF18151D : 0xFF121015;
        int borderCol = isFocused ? C_RED_PRIM : C_BORDER;

        RenderUtil.drawRoundedRect(searchX, searchY, searchWidth, searchHeight, 6.0F, bgCol);
        RenderUtil.drawRoundedOutline(searchX, searchY, searchWidth, searchHeight, 6.0F, 1.0F, borderCol);

        // Vector Search Icon (Crisp & Bold)
        RenderUtil.drawCircleOutline(searchX + 18.0F, searchY + 16.0F, 4.5F, 1.8F, isFocused ? C_RED_PRIM : C_MUTED_TEXT);
        RenderUtil.drawSmoothLine(searchX + 21.0F, searchY + 19.0F, searchX + 25.0F, searchY + 23.0F, 2.0F, isFocused ? C_RED_PRIM : C_MUTED_TEXT);

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();

        if (searchField != null) {
            if (searchField.getText().isEmpty() && !searchField.isFocused()) {
                if (FontManager.sfSemiBold14 != null) {
                    FontManager.sfSemiBold14.drawString("Search accounts by username...", searchX + 34.0F, searchY + 10.0F, 0xFF6D6775);
                } else if (FontManager.sfRegular14 != null) {
                    FontManager.sfRegular14.drawString("Search accounts by username...", searchX + 34.0F, searchY + 10.0F, 0xFF6D6775);
                } else {
                    this.fontRendererObj.drawString("Search accounts...", searchX + 34, searchY + 11, 0xFF6D6775);
                }
            }
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            searchField.drawTextBox();
        }
    }

    private void renderAccountList(int x, int y, int w, int h, int mouseX, int mouseY) {
        // 1. Background Panel Container
        RenderUtil.drawRoundedRect(x, y, w, h, 8.0F, C_PANEL_BG);
        RenderUtil.drawRoundedOutline(x, y, w, h, 8.0F, 1.0F, C_BORDER);

        // 2. Filter Alts based on Search Query
        List<Alt> allAlts = (altManager != null && altManager.getAlts() != null) ? altManager.getAlts() : new ArrayList<Alt>();
        String query = (searchField != null && searchField.getText() != null) ? searchField.getText().trim().toLowerCase() : "";

        List<Alt> filtered = new ArrayList<Alt>();
        for (Alt alt : allAlts) {
            if (query.isEmpty() || (alt.getUsername() != null && alt.getUsername().toLowerCase().contains(query))) {
                filtered.add(alt);
            }
        }

        // 3. Empty State Handling
        if (filtered.isEmpty()) {
            String emptyMsg = query.isEmpty() ? "No accounts added yet. Use the tabs above to add one!" : "No accounts match '" + query + "'";
            if (FontManager.sfSemiBold16 != null) {
                FontManager.sfSemiBold16.drawCenteredString(emptyMsg, x + w / 2.0F, y + h / 2.0F - 6.0F, C_MUTED_TEXT);
            } else if (FontManager.sfMedium16 != null) {
                FontManager.sfMedium16.drawCenteredString(emptyMsg, x + w / 2.0F, y + h / 2.0F - 6.0F, C_MUTED_TEXT);
            } else {
                this.fontRendererObj.drawString(emptyMsg, (int) (x + (w - this.fontRendererObj.getStringWidth(emptyMsg)) / 2), (int) (y + h / 2 - 4), C_MUTED_TEXT);
            }
            return;
        }

        int rowHeight = 44;
        int rowSpacing = 6;
        int totalContentHeight = filtered.size() * (rowHeight + rowSpacing) + 12;
        maxScroll = Math.max(0, totalContentHeight - h);
        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
        }

        // 4. Scissored Account List Rendering
        RenderUtil.prepareScissor(x + 2, y + 2, w - 4, h - 4);

        int rowX = x + 10;
        int rowWidth = w - 20;
        int startRowY = y + 8 - scrollOffset;

        for (int i = 0; i < filtered.size(); i++) {
            Alt alt = filtered.get(i);
            int rowY = startRowY + i * (rowHeight + rowSpacing);

            // Culling check
            if (rowY + rowHeight < y || rowY > y + h) {
                continue;
            }

            boolean isHovered = mouseX >= rowX && mouseX <= rowX + rowWidth && mouseY >= rowY && mouseY <= rowY + rowHeight && mouseY >= y && mouseY <= y + h;
            boolean isSelected = (selectedAlt == alt);

            renderAccountRow(rowX, rowY, rowWidth, rowHeight, alt, isHovered, isSelected, mouseX, mouseY);
        }

        RenderUtil.endScissor();

        // 5. Scrollbar Indicator
        if (maxScroll > 0) {
            float scrollbarTrackH = h - 20;
            float thumbH = Math.max(20.0F, (h / (float) totalContentHeight) * scrollbarTrackH);
            float thumbY = y + 10 + (scrollOffset / (float) maxScroll) * (scrollbarTrackH - thumbH);
            float thumbX = x + w - 7.0F;

            RenderUtil.drawRoundedRect(thumbX, y + 10, 4.0F, scrollbarTrackH, 2.0F, 0xFF18151D);
            RenderUtil.drawRoundedRect(thumbX, thumbY, 4.0F, thumbH, 2.0F, isDraggingScrollbar ? C_RED_PRIM : 0xFF3D3744);
        }
    }

    private void renderAccountRow(int rx, int ry, int rw, int rh, Alt alt, boolean hovered, boolean selected, int mouseX, int mouseY) {
        // 1. Row Background
        int rowBg = selected ? C_ROW_SEL : (hovered ? C_ROW_HOVER : C_ROW_BG);
        RenderUtil.drawRoundedRect(rx, ry, rw, rh, 6.0F, rowBg);

        // 2. Row Border
        int rowBorder = selected ? C_RED_PRIM : (hovered ? C_BORDER_HOV : C_BORDER);
        RenderUtil.drawRoundedOutline(rx, ry, rw, rh, 6.0F, 1.0F, rowBorder);

        if (selected) {
            RenderUtil.drawRoundedOutline(rx - 1.0F, ry - 1.0F, rw + 2.0F, rh + 2.0F, 7.0F, 1.0F, 0x33E52B3F);
        }

        // 3. Avatar Badge Circle with Profile silhouette
        float avatarSize = 32.0F;
        float avatarX = rx + 12.0F;
        float avatarY = ry + (rh - avatarSize) / 2.0F;

        int avatarBg = selected ? 0x44E52B3F : 0xFF1C1820;
        RenderUtil.drawCircle(avatarX + avatarSize / 2.0F, avatarY + avatarSize / 2.0F, avatarSize / 2.0F, avatarBg);
        RenderUtil.drawCircleOutline(avatarX + avatarSize / 2.0F, avatarY + avatarSize / 2.0F, avatarSize / 2.0F, 1.0F, selected ? C_RED_PRIM : C_BORDER);

        int avatarIconColor = selected ? C_RED_PRIM : C_NORM_TEXT;
        RenderUtil.drawProfileIcon(avatarX + avatarSize / 2.0F, avatarY + avatarSize / 2.0F, 13.0F, avatarIconColor);

        // 4. Account Username in Bold HD
        float textStartX = avatarX + avatarSize + 12.0F;
        float nameY = ry + 8.0F;

        String username = alt.getUsername();
        if (FontManager.sfBold16 != null) {
            FontManager.sfBold16.drawStringWithShadow(username, textStartX, nameY, C_BRIGHT_TXT);
        } else if (FontManager.sfSemiBold16 != null) {
            FontManager.sfSemiBold16.drawStringWithShadow(username, textStartX, nameY, C_BRIGHT_TXT);
        } else {
            this.drawString(this.fontRendererObj, "\u00a7l" + username, (int) textStartX, (int) nameY, C_BRIGHT_TXT);
        }

        // 5. Account Type Badge with specific SVG Vector Icons
        float badgeY = nameY + 16.0F;
        String typeName = alt.getType().getDisplayName();
        int badgeTextW = (FontManager.sfSemiBold14 != null) ? FontManager.sfSemiBold14.getStringWidth(typeName) : this.fontRendererObj.getStringWidth(typeName);
        float badgeW = 14.0F + badgeTextW + 12.0F;
        float badgeH = 15.0F;

        RenderUtil.drawRoundedRect(textStartX, badgeY, badgeW, badgeH, 4.0F, 0x33E52B3F);
        RenderUtil.drawRoundedOutline(textStartX, badgeY, badgeW, badgeH, 4.0F, 1.0F, 0x88E52B3F);

        float iconCX = textStartX + 10.0F;
        float iconCY = badgeY + badgeH / 2.0F;

        if (alt.getType() == AltType.MICROSOFT) {
            RenderUtil.drawMicrosoftIcon(iconCX, iconCY, 8.5F, C_RED_PRIM);
        } else if (alt.getType() == AltType.CRACKED) {
            RenderUtil.drawUserSolidIcon(iconCX, iconCY, 8.5F, C_RED_PRIM);
        } else if (alt.getType() == AltType.SESSION) {
            RenderUtil.drawClipboardUserIcon(iconCX, iconCY, 8.5F, C_RED_PRIM);
        } else if (alt.getType() == AltType.REFRESH_TOKEN) {
            RenderUtil.drawRefreshIcon(iconCX, iconCY, 8.5F, C_RED_PRIM);
        } else if (alt.getType() == AltType.COOKIE) {
            RenderUtil.drawCookieBiteIcon(iconCX, iconCY, 8.5F, C_RED_PRIM);
        } else {
            RenderUtil.drawDirectKeyIcon(iconCX, iconCY, 8.5F, C_RED_PRIM);
        }

        float textX = textStartX + 18.0F;
        if (FontManager.sfSemiBold14 != null) {
            FontManager.sfSemiBold14.drawString(typeName, textX, badgeY + 2.0F, C_RED_PRIM);
        } else {
            this.fontRendererObj.drawString(typeName, (int) textX, (int) (badgeY + 3.5F), C_RED_PRIM);
        }
    }

    private void renderActiveAccountPill() {
        String activeUser = (this.mc.getSession() != null && this.mc.getSession().getUsername() != null) ? this.mc.getSession().getUsername() : "Player";
        String label = "Logged in: " + activeUser;

        int textW = (FontManager.sfSemiBold14 != null) ? FontManager.sfSemiBold14.getStringWidth(label) : this.fontRendererObj.getStringWidth(label);
        float pillW = textW + 36.0F;
        float pillH = 26.0F;
        float pillX = 20.0F;
        float pillY = this.height - 41.0F;

        RenderUtil.drawRoundedRect(pillX, pillY, pillW, pillH, 13.0F, 0xEE141018);
        RenderUtil.drawRoundedOutline(pillX, pillY, pillW, pillH, 13.0F, 1.0F, 0x44FFFFFF);

        RenderUtil.drawCircle(pillX + 12.0F, pillY + 13.0F, 3.5F, 0xFF00E676);

        if (FontManager.sfSemiBold14 != null) {
            FontManager.sfSemiBold14.drawStringWithShadow(label, pillX + 22.0F, pillY + 6.5F, C_NORM_TEXT);
        } else {
            this.fontRendererObj.drawStringWithShadow(label, (int) (pillX + 22), (int) (pillY + 8), C_NORM_TEXT);
        }
    }

    private void renderStatusMessage() {
        if (altManager == null) return;
        String status = altManager.getStatusMessage();
        if (status == null || status.isEmpty() || status.contains("Idle")) return;

        int color = altManager.isError() ? C_RED_PRIM : 0xFF00E676;

        int textW = (FontManager.sfSemiBold14 != null) ? FontManager.sfSemiBold14.getStringWidth(status) : this.fontRendererObj.getStringWidth(status);
        float pillW = textW + 24.0F;
        float pillH = 26.0F;
        float pillX = this.width - pillW - 20.0F;
        float pillY = this.height - 41.0F;

        RenderUtil.drawRoundedRect(pillX, pillY, pillW, pillH, 13.0F, 0xEE141018);
        RenderUtil.drawRoundedOutline(pillX, pillY, pillW, pillH, 13.0F, 1.0F, (0x66 << 24) | (color & 0xFFFFFF));

        if (FontManager.sfSemiBold14 != null) {
            FontManager.sfSemiBold14.drawStringWithShadow(status, pillX + 12.0F, pillY + 6.5F, color);
        } else {
            this.fontRendererObj.drawStringWithShadow(status, (int) (pillX + 12), (int) (pillY + 8), color);
        }
    }

    private void renderModal(int mouseX, int mouseY) {
        // Modal Backdrop Blur / Dim
        RenderUtil.drawRect(0, 0, this.width, this.height, 0xAA000000);

        int modalW = Math.min(420, this.width - 40);
        int modalH = 160;
        int modalX = (this.width - modalW) / 2;
        int modalY = (this.height - modalH) / 2;

        // Modal Glass Box
        RenderUtil.drawRoundedRect(modalX, modalY, modalW, modalH, 10.0F, 0xF8110D15);
        RenderUtil.drawRoundedOutline(modalX, modalY, modalW, modalH, 10.0F, 1.0F, C_BORDER_HOV);
        RenderUtil.drawRoundedOutline(modalX - 1.0F, modalY - 1.0F, modalW + 2.0F, modalH + 2.0F, 11.0F, 1.0F, 0x33E52B3F);

        // Modal Title
        String modalTitle = getModalTitle();
        if (FontManager.sfBold18 != null) {
            FontManager.sfBold18.drawStringWithShadow(modalTitle, modalX + 20.0F, modalY + 18.0F, C_BRIGHT_TXT);
        } else {
            this.fontRendererObj.drawStringWithShadow("\u00a7l" + modalTitle, modalX + 20, modalY + 18, C_BRIGHT_TXT);
        }

        String prompt = getModalPrompt();
        if (FontManager.sfSemiBold14 != null) {
            FontManager.sfSemiBold14.drawString(prompt, modalX + 20.0F, modalY + 40.0F, C_MUTED_TEXT);
        } else if (FontManager.sfRegular14 != null) {
            FontManager.sfRegular14.drawString(prompt, modalX + 20.0F, modalY + 40.0F, C_MUTED_TEXT);
        } else {
            this.fontRendererObj.drawString(prompt, modalX + 20, modalY + 40, C_MUTED_TEXT);
        }

        // Modal Input Field Box
        int fieldW = modalW - 40;
        int fieldH = 32;
        int fieldX = modalX + 20;
        int fieldY = modalY + 58;

        boolean fieldFocused = modalField != null && modalField.isFocused();
        RenderUtil.drawRoundedRect(fieldX, fieldY, fieldW, fieldH, 6.0F, fieldFocused ? 0xFF18151D : 0xFF141118);
        RenderUtil.drawRoundedOutline(fieldX, fieldY, fieldW, fieldH, 6.0F, 1.0F, fieldFocused ? C_RED_PRIM : C_BORDER);

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();

        if (modalField != null) {
            modalField.drawTextBox();
        }

        // Error message if any
        if (modalError != null) {
            if (FontManager.sfSemiBold14 != null) {
                FontManager.sfSemiBold14.drawString(modalError, modalX + 20.0F, modalY + 96.0F, C_RED_PRIM);
            } else {
                this.fontRendererObj.drawString(modalError, modalX + 20, modalY + 96, C_RED_PRIM);
            }
        }

        // Modal Action Buttons (Login & Cancel)
        int btnW = (modalW - 50) / 2;
        int btnH = 30;
        int btnY = modalY + modalH - 42;

        boolean loginHover = mouseX >= modalX + 20 && mouseX <= modalX + 20 + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        boolean cancelHover = mouseX >= modalX + 30 + btnW && mouseX <= modalX + 30 + btnW * 2 && mouseY >= btnY && mouseY <= btnY + btnH;

        RenderUtil.drawRoundedRect(modalX + 20, btnY, btnW, btnH, 6.0F, loginHover ? 0xFFE52B3F : 0xFFB31F2F);
        RenderUtil.drawRoundedRect(modalX + 30 + btnW, btnY, btnW, btnH, 6.0F, cancelHover ? 0xFF241F28 : 0xFF18151D);
        RenderUtil.drawRoundedOutline(modalX + 30 + btnW, btnY, btnW, btnH, 6.0F, 1.0F, cancelHover ? C_BORDER_HOV : C_BORDER);

        if (FontManager.sfSemiBold14 != null) {
            FontManager.sfSemiBold14.drawCenteredString("Login", modalX + 20 + btnW / 2.0F, btnY + 9.0F, 0xFFFFFFFF);
            FontManager.sfSemiBold14.drawCenteredString("Cancel", modalX + 30 + btnW + btnW / 2.0F, btnY + 9.0F, C_NORM_TEXT);
        } else {
            this.drawCenteredString(this.fontRendererObj, "Login", modalX + 20 + btnW / 2, btnY + 11, 0xFFFFFFFF);
            this.drawCenteredString(this.fontRendererObj, "Cancel", modalX + 30 + btnW + btnW / 2, btnY + 11, C_NORM_TEXT);
        }
    }

    private String getModalTitle() {
        switch (activeModal) {
            case CRACKED: return "Cracked / Offline Login";
            case SESSION_TOKEN: return "Session ID / Token Login";
            case REFRESH_TOKEN: return "Refresh Token Login";
            case COOKIE: return "Netscape Cookie Login";
            default: return "Account Login";
        }
    }

    private String getModalPrompt() {
        switch (activeModal) {
            case CRACKED: return "Enter desired Minecraft username:";
            case SESSION_TOKEN: return "Paste your valid Minecraft Bearer session token:";
            case REFRESH_TOKEN: return "Paste your OAuth refresh token:";
            case COOKIE: return "Paste your Netscape / JSON cookie string:";
            default: return "Enter credentials:";
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (activeModal != ModalType.NONE) {
            int modalW = Math.min(420, this.width - 40);
            int modalH = 160;
            int modalX = (this.width - modalW) / 2;
            int modalY = (this.height - modalH) / 2;

            int btnW = (modalW - 50) / 2;
            int btnH = 30;
            int btnY = modalY + modalH - 42;

            if (mouseX >= modalX + 20 && mouseX <= modalX + 20 + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                handleModalSubmit();
                return;
            }

            if (mouseX >= modalX + 30 + btnW && mouseX <= modalX + 30 + btnW * 2 && mouseY >= btnY && mouseY <= btnY + btnH) {
                closeModal();
                return;
            }

            int fieldW = modalW - 40;
            int fieldH = 32;
            int fieldX = modalX + 20;
            int fieldY = modalY + 58;

            if (modalField != null) {
                if (mouseX >= fieldX && mouseX <= fieldX + fieldW && mouseY >= fieldY && mouseY <= fieldY + fieldH) {
                    modalField.setFocused(true);
                }
                modalField.mouseClicked(mouseX, mouseY, mouseButton);
                modalField.setFocused(true); // modal field should never lose focus while modal is open
            }
            return;
        }

        int panelWidth = Math.min(680, this.width - 40);
        int searchWidth = Math.min(460, panelWidth - 40);
        int searchHeight = 34;
        int searchX = (this.width - searchWidth) / 2;
        int searchY = 62 + 30 + 12;

        if (searchField != null) {
            if (mouseX >= searchX && mouseX <= searchX + searchWidth && mouseY >= searchY && mouseY <= searchY + searchHeight) {
                searchField.setFocused(true);
            } else {
                searchField.setFocused(false);
            }
            searchField.mouseClicked(mouseX, mouseY, mouseButton);
        }

        // Account List Row Selection
        int panelX = (this.width - panelWidth) / 2;
        int listY = 62 + 30 + 12 + 34 + 12;
        int listHeight = this.height - listY - 54;

        if (mouseX >= panelX && mouseX <= panelX + panelWidth && mouseY >= listY && mouseY <= listY + listHeight) {
            List<Alt> allAlts = (altManager != null && altManager.getAlts() != null) ? altManager.getAlts() : new ArrayList<Alt>();
            String query = (searchField != null && searchField.getText() != null) ? searchField.getText().trim().toLowerCase() : "";

            List<Alt> filtered = new ArrayList<Alt>();
            for (Alt alt : allAlts) {
                if (query.isEmpty() || (alt.getUsername() != null && alt.getUsername().toLowerCase().contains(query))) {
                    filtered.add(alt);
                }
            }

            int rowHeight = 44;
            int rowSpacing = 6;
            int rowX = panelX + 10;
            int rowWidth = panelWidth - 20;
            int startRowY = listY + 8 - scrollOffset;

            for (int i = 0; i < filtered.size(); i++) {
                int rowY = startRowY + i * (rowHeight + rowSpacing);
                if (mouseX >= rowX && mouseX <= rowX + rowWidth && mouseY >= rowY && mouseY <= rowY + rowHeight) {
                    Alt clicked = filtered.get(i);
                    if (selectedAlt == clicked) {
                        // Double-click to login immediately
                        if (altManager != null) {
                            altManager.loginAlt(clicked);
                        }
                    } else {
                        selectedAlt = clicked;
                    }
                    break;
                }
            }

            // Check scrollbar drag
            if (maxScroll > 0 && mouseX >= panelX + panelWidth - 12 && mouseX <= panelX + panelWidth) {
                isDraggingScrollbar = true;
                dragScrollStartY = mouseY;
                initialScrollOffset = scrollOffset;
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        isDraggingScrollbar = false;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (isDraggingScrollbar && maxScroll > 0) {
            int panelWidth = Math.min(680, this.width - 40);
            int listY = 62 + 30 + 12 + 34 + 12;
            int listHeight = this.height - listY - 54;
            float scrollbarTrackH = listHeight - 20;

            float deltaY = mouseY - dragScrollStartY;
            scrollOffset = (int) (initialScrollOffset + (deltaY / scrollbarTrackH) * maxScroll);
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0 && maxScroll > 0 && activeModal == ModalType.NONE) {
            if (dWheel > 0) {
                scrollOffset -= 28;
            } else {
                scrollOffset += 28;
            }
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (activeModal != ModalType.NONE) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                closeModal();
                return;
            }
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                handleModalSubmit();
                return;
            }
            // Route ALL key events to modalField regardless of focus state
            if (modalField != null) {
                modalField.setFocused(true);  // ensure field stays focused
                modalField.textboxKeyTyped(typedChar, keyCode);
            }
            return;
        }

        if (searchField != null && searchField.isFocused()) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                searchField.setFocused(false);
                return;
            }
            searchField.textboxKeyTyped(typedChar, keyCode);
            return;
        }

        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen(this.parentScreen);
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }
}
