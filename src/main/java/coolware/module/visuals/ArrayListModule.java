package coolware.module.visuals;

import coolware.Coolware;
import coolware.font.moonlight.FontRenderer;
import coolware.font.moonlight.Fonts;
import coolware.module.Category;
import coolware.module.Module;
import coolware.setting.BooleanSetting;
import coolware.setting.ModeSetting;
import coolware.setting.NumberSetting;
import coolware.setting.Setting;
import coolware.util.RenderUtil;
import coolware.util.RoundedUtils;
import coolware.util.vector.Vector2d;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ArrayListModule extends Module {

    // Main Style/Mode (Rise: Modern, Classic, Creida, Wurst)
    private final ModeSetting style = new ModeSetting("Style", this, "Modern", "Modern", "Classic", "Creida", "Wurst");

    // Modules to Show (Rise: Exclude render, All, Only bound)
    private final ModeSetting modulesToShow = new ModeSetting("Modules to Show", this, "Exclude render", "Exclude render", "All", "Only bound");

    // Colors (Rise: Fade, Rainbow, Breathe, Static, Category)
    private final ModeSetting colorMode = new ModeSetting("Color Mode", this, "Fade", "Fade", "Rainbow", "Breathe", "Static", "Category");

    // Font selection (Rise: Apple UI, Minecraft, Product Sans, Inter)
    private final ModeSetting fontMode = new ModeSetting("Font", this, "Apple UI", "Apple UI", "Minecraft", "Product Sans", "Inter");

    // Alignment (Right, Left)
    private final ModeSetting alignment = new ModeSetting("Alignment", this, "Right", "Right", "Left");

    // Background (Rise: Normal, Off)
    private final ModeSetting background = new ModeSetting("Background", this, "Normal", "Normal", "Off");

    // Scale
    private final NumberSetting scale = new NumberSetting("Scale", this, 1.0, 0.5, 2.0, 0.05);

    // Visual options
    private final BooleanSetting sidebar = new BooleanSetting("Sidebar", this, true);
    private final BooleanSetting dropShadow = new BooleanSetting("Drop Shadow", this, true);
    private final BooleanSetting suffix = new BooleanSetting("Suffix", this, true);
    private final BooleanSetting lowercase = new BooleanSetting("Lowercase", this, false);
    private final BooleanSetting removeSpaces = new BooleanSetting("Remove Spaces", this, false);

    // Number offsets and alpha
    private final NumberSetting backgroundAlpha = new NumberSetting("Background Alpha", this, 110, 0, 255, 5);
    private final NumberSetting moduleSpacingSetting = new NumberSetting("Spacing", this, 11.0, 8.0, 18.0, 0.5);
    private final NumberSetting edgeOffsetX = new NumberSetting("Edge Offset X", this, 4.0, 0.0, 50.0, 1.0);
    private final NumberSetting edgeOffsetY = new NumberSetting("Edge Offset Y", this, 4.0, 0.0, 100.0, 1.0);

    // Internal state
    private final List<ArrayListEntry> allEntries = new ArrayList<ArrayListEntry>();
    private final List<ArrayListEntry> activeEntries = new ArrayList<ArrayListEntry>();
    private long lastAnimTime = System.currentTimeMillis();

    public ArrayListModule() {
        super("ArrayList", "Displays enabled modules with smooth animations and custom Rise styles", Category.VISUALS, Keyboard.KEY_NONE);
        addSetting(style);
        addSetting(modulesToShow);
        addSetting(colorMode);
        addSetting(fontMode);
        addSetting(alignment);
        addSetting(background);
        addSetting(scale);
        addSetting(sidebar);
        addSetting(dropShadow);
        addSetting(suffix);
        addSetting(lowercase);
        addSetting(removeSpaces);
        addSetting(backgroundAlpha);
        addSetting(moduleSpacingSetting);
        addSetting(edgeOffsetX);
        addSetting(edgeOffsetY);

        setToggled(true);
        rebuildEntries();
    }

    public String getModulesToShow() {
        return modulesToShow.getMode();
    }

    @Override
    public void onEnable() {
        rebuildEntries();
    }

    public void rebuildEntries() {
        allEntries.clear();
        if (Coolware.getInstance().getModuleManager() == null) return;
        List<Module> mods = Coolware.getInstance().getModuleManager().getModules();
        for (Module m : mods) {
            allEntries.add(new ArrayListEntry(m));
        }
        updateEntryNames();
    }

    private void updateEntryNames() {
        for (ArrayListEntry entry : allEntries) {
            entry.setTranslatedName(entry.getModule().getName());
        }
    }

    private FontRenderer getSelectedFontRenderer() {
        String f = fontMode.getMode();
        if ("Product Sans".equalsIgnoreCase(f)) {
            return Fonts.psRegular.get(16);
        } else if ("Inter".equalsIgnoreCase(f)) {
            return Fonts.interMedium.get(15);
        } else {
            // Default Apple UI / SF UI
            return Fonts.sfui.get(16);
        }
    }

    private boolean isMinecraftFont() {
        return "Minecraft".equalsIgnoreCase(fontMode.getMode());
    }

    private float getTagGap() {
        return isMinecraftFont() ? 4.5F : 3.0F;
    }

    private float getStringWidth(String text) {
        if (isMinecraftFont() || mc.fontRendererObj == null) {
            return mc.fontRendererObj != null ? mc.fontRendererObj.getStringWidth(text) : text.length() * 6;
        }
        FontRenderer fr = getSelectedFontRenderer();
        return fr != null ? fr.getStringWidth(text) : mc.fontRendererObj.getStringWidth(text);
    }

    private float getFontHeight() {
        if (isMinecraftFont() || mc.fontRendererObj == null) {
            return mc.fontRendererObj != null ? mc.fontRendererObj.FONT_HEIGHT : 9.0F;
        }
        FontRenderer fr = getSelectedFontRenderer();
        return fr != null ? fr.getHeight() : 9.0F;
    }

    private void drawString(String text, double x, double y, int color, boolean shadow) {
        if (isMinecraftFont() || mc.fontRendererObj == null) {
            if (shadow) {
                mc.fontRendererObj.drawStringWithShadow(text, (float) x, (float) y, color);
            } else {
                mc.fontRendererObj.drawString(text, (int) x, (int) y, color);
            }
            return;
        }
        FontRenderer fr = getSelectedFontRenderer();
        if (fr != null) {
            if (shadow) {
                fr.drawStringWithShadow(text, (float) x, (float) y, color);
            } else {
                fr.drawString(text, (float) x, (float) y, color);
            }
        }
    }

    @Override
    public void onUpdate() {
        // Update tags and active list
        if (allEntries.size() != Coolware.getInstance().getModuleManager().getModules().size()) {
            rebuildEntries();
        }

        for (ArrayListEntry entry : allEntries) {
            if (entry.getModule() != null) {
                // Find tag if any
                String tag = "";
                for (Setting s : entry.getModule().getSettings()) {
                    if (s instanceof ModeSetting) {
                        tag = ((ModeSetting) s).getMode();
                        break;
                    }
                }
                entry.setTag(tag);

                boolean hasTag = suffix.isEnabled() && !tag.isEmpty();
                entry.setHasTag(hasTag);

                String displayName = entry.getTranslatedName();
                String displayTag = tag;

                if (lowercase.isEnabled()) {
                    displayName = displayName.toLowerCase();
                    displayTag = displayTag.toLowerCase();
                }

                if (removeSpaces.isEnabled()) {
                    displayName = displayName.replace(" ", "");
                    displayTag = displayTag.replace(" ", "");
                }

                entry.setDisplayName(displayName);
                entry.setDisplayTag(displayTag);

                entry.setNameWidth(getStringWidth(displayName));
                entry.setTagWidth(hasTag ? getStringWidth(displayTag) + getTagGap() : 0.0F);
            }
        }

        // Sort active entries by total width descending (Rise standard)
        activeEntries.clear();
        for (ArrayListEntry entry : allEntries) {
            if (entry.shouldShow(this)) {
                activeEntries.add(entry);
            }
        }
        activeEntries.sort(new Comparator<ArrayListEntry>() {
            @Override
            public int compare(ArrayListEntry o1, ArrayListEntry o2) {
                return Float.compare(o2.getTotalWidth(), o1.getTotalWidth());
            }
        });
    }

    @Override
    public void onRender2D() {
        if (mc.gameSettings.showDebugInfo || mc.theWorld == null || mc.thePlayer == null) {
            return;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        float screenWidth = sr.getScaledWidth();
        float screenHeight = sr.getScaledHeight();

        long now = System.currentTimeMillis();
        long elapsed = Math.min(100L, Math.max(1L, now - lastAnimTime));
        lastAnimTime = now;

        float animDivisor = "Classic".equalsIgnoreCase(style.getMode()) ? 10.0F : 100.0F;

        // 1. Advance animation time for entries
        for (ArrayListEntry entry : allEntries) {
            if (entry.getModule().isToggled()) {
                entry.animationTime = Math.min(entry.animationTime + (float) elapsed / animDivisor, 10.0F);
            } else {
                entry.animationTime = Math.max(entry.animationTime - (float) elapsed / animDivisor, 0.0F);
            }
        }

        float moduleSpacing = (float) moduleSpacingSetting.getValue();
        float edgeX = (float) edgeOffsetX.getValue();
        float edgeY = (float) edgeOffsetY.getValue();
        boolean isLeft = "Left".equalsIgnoreCase(alignment.getMode());

        float scaleVal = (float) scale.getValue();
        float effectiveScreenWidth = screenWidth / scaleVal;

        // 2. Compute targets & positions
        float currentY = edgeY;
        for (ArrayListEntry entry : activeEntries) {
            if (entry.animationTime > 0.001F) {
                float totalWidth = entry.getTotalWidth();
                double targetX;

                if (isLeft) {
                    if (entry.getModule().isToggled() || entry.animationTime >= 9.9F) {
                        targetX = edgeX;
                    } else {
                        targetX = -totalWidth - 10.0;
                    }
                } else {
                    if (entry.getModule().isToggled() || entry.animationTime >= 9.9F) {
                        targetX = effectiveScreenWidth - totalWidth - edgeX;
                    } else {
                        targetX = effectiveScreenWidth + 10.0;
                    }
                }

                entry.targetPosition = new Vector2d(targetX, currentY);

                if (entry.getModule().isToggled() || entry.animationTime >= 10.0F) {
                    currentY += moduleSpacing;
                }

                // Smooth interpolate position (Rise math: 0.015 * elapsed)
                float lerpFactor = (float) Math.min(1.0, 0.015F * elapsed);
                if (Math.abs(entry.position.x - entry.targetPosition.x) <= 0.5 && Math.abs(entry.position.y - entry.targetPosition.y) <= 0.5) {
                    entry.position.x = entry.targetPosition.x;
                    entry.position.y = entry.targetPosition.y;
                } else {
                    entry.position.x = entry.position.x + lerpFactor * (entry.targetPosition.x - entry.position.x);
                    entry.position.y = entry.position.y + lerpFactor * (entry.targetPosition.y - entry.position.y);
                }
            }
        }

        // 3. Render entries according to chosen style with GL scaling
        GlStateManager.pushMatrix();
        if (scaleVal != 1.0F) {
            GlStateManager.scale(scaleVal, scaleVal, 1.0F);
        }

        String currentStyle = style.getMode();
        if ("Classic".equalsIgnoreCase(currentStyle)) {
            renderClassic(isLeft, moduleSpacing);
        } else if ("Creida".equalsIgnoreCase(currentStyle)) {
            renderCreida(isLeft, moduleSpacing);
        } else if ("Wurst".equalsIgnoreCase(currentStyle)) {
            renderWurst(isLeft, moduleSpacing);
        } else {
            renderModern(isLeft, moduleSpacing);
        }

        GlStateManager.popMatrix();
    }

    private Color computeEntryColor(ArrayListEntry entry, int index) {
        String cm = colorMode.getMode();
        Theme theme = Theme.getInstance();

        if ("Rainbow".equalsIgnoreCase(cm)) {
            return new Color(getRainbow(500 * index / 6));
        } else if ("Fade".equalsIgnoreCase(cm)) {
            if (theme != null) {
                int col = theme.getColor(index * 25);
                return new Color(col, true);
            }
            return Color.WHITE;
        } else if ("Breathe".equalsIgnoreCase(cm)) {
            if (theme != null) {
                Color c1 = new Color(theme.getPrimaryColor());
                Color c2 = new Color(theme.getSecondaryColor());
                double blend = Math.sin(System.currentTimeMillis() / 600.0) * 0.5 + 0.5;
                return blendColor(c1, c2, blend);
            }
            return Color.CYAN;
        } else if ("Category".equalsIgnoreCase(cm)) {
            Category cat = entry.getModule().getCategory();
            if (cat == Category.COMBAT) return new Color(255, 80, 80);
            if (cat == Category.MOVEMENT) return new Color(80, 200, 255);
            if (cat == Category.PLAYER) return new Color(255, 200, 80);
            if (cat == Category.VISUALS) return new Color(180, 100, 255);
            if (cat == Category.GHOST) return new Color(100, 255, 150);
            return new Color(200, 200, 200);
        } else {
            // Static
            if (theme != null) {
                return new Color(theme.getPrimaryColor());
            }
            return Color.WHITE;
        }
    }

    private static Color blendColor(Color c1, Color c2, double factor) {
        factor = Math.max(0.0, Math.min(1.0, factor));
        int r = (int) (c1.getRed() + factor * (c2.getRed() - c1.getRed()));
        int g = (int) (c1.getGreen() + factor * (c2.getGreen() - c1.getGreen()));
        int b = (int) (c1.getBlue() + factor * (c2.getBlue() - c1.getBlue()));
        return new Color(r, g, b);
    }

    private static int getRainbow(int delay) {
        return Color.getHSBColor((float) ((Math.ceil((System.currentTimeMillis() + delay) / 10.0) % 360.0) / 360.0), 0.6F, 1.0F).getRGB();
    }

    // ==========================================
    // MODERN STYLE (Rise Modern Interface)
    // ==========================================
    private void renderModern(boolean isLeft, float moduleSpacing) {
        int index = 0;
        int bgAlpha = (int) backgroundAlpha.getValue();
        boolean hasBg = "Normal".equalsIgnoreCase(background.getMode());
        boolean hasShadow = dropShadow.isEnabled();
        boolean hasBar = sidebar.isEnabled();
        boolean mcFont = isMinecraftFont();
        float padLeft = mcFont ? 3.5F : 2.0F;
        float padRight = mcFont ? 3.5F : 2.0F;
        float boxOffsetY = 1.5F;
        float tagGap = getTagGap();

        for (ArrayListEntry entry : activeEntries) {
            if (entry.animationTime <= 0.001F) continue;

            double x = entry.getPosition().getX();
            double y = entry.getPosition().getY();
            float totalWidth = entry.getTotalWidth();
            Color color = computeEntryColor(entry, index);
            entry.setColor(color);

            float textY = (float) (mcFont ? (y - boxOffsetY) + (moduleSpacing - 9.0F) / 2.0F : y);

            // Draw Background
            if (hasBg) {
                int bgCol = new Color(0, 0, 0, bgAlpha).getRGB();
                RenderUtil.drawRectWH((float) (x - padLeft), (float) (y - boxOffsetY), totalWidth + padLeft + padRight, moduleSpacing, bgCol);
            }

            // Draw Sidebar
            if (hasBar) {
                if (isLeft) {
                    RenderUtil.drawRectWH((float) (x - padLeft - (hasBg ? 1.5F : 2.5F)), (float) (y - boxOffsetY), 1.5F, moduleSpacing, color.getRGB());
                } else {
                    RenderUtil.drawRectWH((float) (x + totalWidth + padRight + (mcFont ? 0.5F : 0.0F)), (float) (y - boxOffsetY), 1.5F, moduleSpacing, color.getRGB());
                }
            }

            // Draw Module Text
            drawString(entry.getDisplayName(), x, textY, color.getRGB(), hasShadow);

            // Draw Tag Text
            if (entry.isHasTag()) {
                drawString(entry.getDisplayTag(), x + entry.getNameWidth() + tagGap, textY, 0xFF999999, hasShadow);
            }

            index++;
        }
    }

    // ==========================================
    // CLASSIC STYLE (Rise Classic Interface)
    // ==========================================
    private void renderClassic(boolean isLeft, float moduleSpacing) {
        int index = 0;
        int bgAlpha = (int) backgroundAlpha.getValue();
        boolean hasBg = "Normal".equalsIgnoreCase(background.getMode());
        boolean hasBar = sidebar.isEnabled();
        boolean mcFont = isMinecraftFont();
        float padLeft = mcFont ? 3.5F : 2.0F;
        float padRight = mcFont ? 3.5F : 2.0F;
        float boxOffsetY = mcFont ? 1.5F : 2.0F;
        float tagGap = getTagGap();

        for (ArrayListEntry entry : activeEntries) {
            if (entry.animationTime <= 0.001F) continue;

            double x = entry.getPosition().getX();
            double y = entry.getPosition().getY();
            float totalWidth = entry.getTotalWidth();
            Color color = computeEntryColor(entry, index);
            entry.setColor(color);

            float textY = (float) (mcFont ? (y - boxOffsetY) + (moduleSpacing - 9.0F) / 2.0F : y);

            if (hasBg) {
                int bgCol = new Color(0, 0, 0, bgAlpha).getRGB();
                RenderUtil.drawRectWH((float) (x - padLeft), (float) (y - boxOffsetY), totalWidth + padLeft + padRight, moduleSpacing, bgCol);
            }

            if (hasBar) {
                if (isLeft) {
                    RenderUtil.drawRectWH((float) (x - padLeft - (hasBg ? 1.0F : 2.0F)), (float) (y - boxOffsetY), 1.0F, moduleSpacing, color.getRGB());
                } else {
                    RenderUtil.drawRectWH((float) (x + totalWidth + padRight + (mcFont ? 0.5F : 0.0F)), (float) (y - boxOffsetY), 1.0F, moduleSpacing, color.getRGB());
                }
            }

            drawString(entry.getDisplayName(), x, textY, color.getRGB(), true);

            if (entry.isHasTag()) {
                drawString(entry.getDisplayTag(), x + entry.getNameWidth() + tagGap, textY, 0xFFCCCCCC, true);
            }

            index++;
        }
    }

    // ==========================================
    // CREIDA STYLE (Rise Creida Interface)
    // ==========================================
    private void renderCreida(boolean isLeft, float moduleSpacing) {
        int index = 0;
        boolean mcFont = isMinecraftFont();
        float padLeft = mcFont ? 3.5F : 3.0F;
        float padRight = mcFont ? 3.5F : 2.0F;
        float boxOffsetY = mcFont ? 1.5F : 1.0F;
        float tagGap = getTagGap();

        for (ArrayListEntry entry : activeEntries) {
            if (entry.animationTime <= 0.001F) continue;

            double x = entry.getPosition().getX();
            double y = entry.getPosition().getY();
            float totalWidth = entry.getTotalWidth();
            Color color = computeEntryColor(entry, index);
            entry.setColor(color);

            float textY = (float) (mcFont ? (y - boxOffsetY) + (moduleSpacing - 9.0F) / 2.0F : y + 1.0);

            // Creida rectangular background + accent right line
            RenderUtil.drawRectWH((float) (x - padLeft), (float) (y - boxOffsetY), totalWidth + padLeft + padRight, moduleSpacing, new Color(0, 0, 0, 110).getRGB());
            RenderUtil.drawRectWH((float) (x + totalWidth + padRight), (float) (y - boxOffsetY), 1.0F, moduleSpacing, color.getRGB());

            drawString(entry.getDisplayName(), x - 1.0, textY, color.getRGB(), false);
            if (entry.isHasTag()) {
                drawString(entry.getDisplayTag(), x + entry.getNameWidth() + tagGap - 1.0, textY, 0xFFCCCCCC, false);
            }

            index++;
        }
    }

    // ==========================================
    // WURST STYLE (Rise Wurst Interface)
    // ==========================================
    private void renderWurst(boolean isLeft, float moduleSpacing) {
        boolean mcFont = isMinecraftFont();
        for (ArrayListEntry entry : activeEntries) {
            if (entry.animationTime <= 0.001F) continue;

            double x = entry.getPosition().getX();
            double y = entry.getPosition().getY();
            float textY = (float) (mcFont ? y + (moduleSpacing - 9.0F) / 2.0F - 0.5F : y);

            // Pure Wurst clean white text
            drawString(entry.getTranslatedName(), x, textY, 0xFFFFFFFF, true);
        }
    }
}
