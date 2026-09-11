package com.github.cooldood.modules.impl.render;

import com.github.cooldood.bridge.net.minecraft.client.model.ModelBoxBridge;
import com.github.cooldood.bridge.net.minecraft.client.model.ModelRendererBridge;
import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.RenderWorldEvent;
import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.modules.RegisterSubModule;
import com.github.cooldood.modules.SubCategory;
import com.github.cooldood.modules.impl.client.ThemeModule;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.minecraft.TargetUtil;
import com.github.cooldood.utils.render.Render3dUtil;
import com.github.cooldood.utils.render.RenderUtil;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.PositionTextureVertex;
import net.minecraft.client.model.TexturedQuad;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.awt.*;

@RegisterModule(
        name = "ESP",
        description = "Highlights entities through walls.",
        category = Category.RENDER
)
public class ESP extends Module {

    // ─── Mode ──────────────────────────────────────────────────────────────────
    @RegisterSubModule(name = "Mode")
    public static ESPMode mode = ESPMode.Box;
    public enum ESPMode {
        Box, WireFrame, Outline, Box2D
    }

    // ─── Colors ────────────────────────────────────────────────────────────────
    @RegisterSubModule(name = "ESP Color", description = "Color used to highlight entities")
    public static Color espColor = ThemeModule.primaryColor;

    @RegisterSubModule(name = "Use Theme Color", description = "Use the global theme color for ESP")
    public static boolean useThemeColor = true;

    @RegisterSubModule(name = "Friend Color", description = "Color used for teammates")
    public static Color friendColor = new Color(0, 120, 255);

    @RegisterSubModule(name = "Hurt Color", description = "Color when entity is hurt")
    public static Color hurtColor = new Color(255, 60, 60);

    // ─── Box settings ──────────────────────────────────────────────────────────
    @RegisterSubModule(name = "Box Settings")
    public static SubCategory boxCategory = new SubCategory();

    @RegisterSubModule(name = "Fill", description = "Fill the box with a translucent color", parent = "Box Settings")
    public static boolean boxFill = true;

    @RegisterSubModule(name = "Fill Alpha", description = "Opacity of box fill (0-255)", min = 0, max = 255, increment = 1, parent = "Box Settings")
    public static int fillAlpha = 26;

    @RegisterSubModule(name = "Outline Alpha", description = "Opacity of box outline (0-255)", min = 0, max = 255, increment = 1, parent = "Box Settings")
    public static int outlineAlpha = 180;

    // ─── WireFrame settings ────────────────────────────────────────────────────
    @RegisterSubModule(name = "WireFrame Settings")
    public static SubCategory wireCategory = new SubCategory();

    @RegisterSubModule(name = "WireFrame Width", min = 0.5, max = 5, increment = 0.5, parent = "WireFrame Settings")
    public static float wireframeWidth = 2.0f;

    // ─── Outline (model-level) settings ────────────────────────────────────────
    @RegisterSubModule(name = "Outline Settings")
    public static SubCategory outlineCategory = new SubCategory();

    @RegisterSubModule(name = "Outline Mode", description = "Solid fills quads; Line draws edges", parent = "Outline Settings")
    public static OutlineRenderMode outlineMode = OutlineRenderMode.Solid;
    public enum OutlineRenderMode {
        Solid, Line
    }
    @RegisterSubModule(name = "Outline Size", min = 0.75, max = 1.25, increment = 0.01, parent = "Outline Settings")
    public static float outlineSize = 1.1f;
    @RegisterSubModule(name = "Outline Width", min = 1, max = 10, increment = 1, parent = "Outline Settings")
    public static int outlineWidth = 3;

    // ─── Health bar ────────────────────────────────────────────────────────────
    @RegisterSubModule(name = "Health Bar", description = "Show a health bar next to entities")
    public static boolean healthBar = true;

    // ─── Chams ─────────────────────────────────────────────────────────────────
    @RegisterSubModule(name = "Chams", description = "Make entity models visible through walls")
    public static boolean chams = false;

    // ─── Render ────────────────────────────────────────────────────────────────
    @SubscribeEvent
    public static void onRenderWorldEvent(RenderWorldEvent event) {
        for (EntityLivingBase entity : TargetUtil.getAllValidTargets(true)) {
            Color color = getColor(entity);

            switch (mode) {
                case Box:
                    renderBox(entity, color, event.partialTicks);
                    break;
                case WireFrame:
                    renderWireframe(entity, color, event.partialTicks);
                    break;
                case Box2D:
                    renderBox2D(entity, color, event.partialTicks);
                    break;
                // Outline is drawn via renderOutline() called from entity renderer (chams path)
                default:
                    renderBox(entity, color, event.partialTicks);
                    break;
            }

            if (healthBar) renderHealthBar(entity, event.partialTicks);
        }
    }

    // ─── Color helper ──────────────────────────────────────────────────────────
    public static Color getColor(EntityLivingBase entity) {
        if (entity.hurtTime > 0) return hurtColor;
        return useThemeColor ? ThemeModule.primaryColor : espColor;
    }

    // ─── Box (filled + outline) ────────────────────────────────────────────────
    private static void renderBox(EntityLivingBase entity, Color color, float partialTicks) {
        Vec3 pos = Render3dUtil.getRelativeEntityPos(entity, partialTicks);

        double x = pos.xCoord;
        double y = pos.yCoord;
        double z = pos.zCoord;

        double hw = entity.width / 2.0 + 0.05;
        double h  = entity.height + 0.15;

        AxisAlignedBB bb = new AxisAlignedBB(
                x - hw, y, z - hw,
                x + hw, y + h, z + hw
        );

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GlStateManager.depthMask(false);

        if (boxFill) {
            GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, fillAlpha / 255f);
            drawFilledBox(bb);
        }

        GL11.glLineWidth(1.0f);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, outlineAlpha / 255f);
        RenderGlobal.drawSelectionBoundingBox(bb);

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.depthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glPopMatrix();
    }

    // ─── WireFrame ─────────────────────────────────────────────────────────────
    private static void renderWireframe(EntityLivingBase entity, Color color, float partialTicks) {
        Vec3 pos = Render3dUtil.getRelativeEntityPos(entity, partialTicks);

        double x = pos.xCoord;
        double y = pos.yCoord;
        double z = pos.zCoord;

        double hw = entity.width / 2.0 + 0.05;
        double h  = entity.height + 0.15;

        AxisAlignedBB bb = new AxisAlignedBB(
                x - hw, y, z - hw,
                x + hw, y + h, z + hw
        );

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GlStateManager.depthMask(false);

        GL11.glLineWidth(wireframeWidth);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, outlineAlpha / 255f);
        RenderGlobal.drawSelectionBoundingBox(bb);

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(1f);
        GlStateManager.depthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glPopMatrix();
    }

    // ─── 2D Billboard Bracket ──────────────────────────────────────────────────
    private static void renderBox2D(EntityLivingBase entity, Color color, float partialTicks) {
        GL11.glPushMatrix();
        RenderUtil.glTranslate(Render3dUtil.getRelativeEntityPos(entity, partialTicks));
        Render3dUtil.rotateToPlayer(false);
        RenderUtil.drawRectOutline(-entity.width / 2f, 0, entity.width, entity.height, 1, color);
        GL11.glPopMatrix();
    }

    // ─── Outline (model-level, called from chams renderer) ─────────────────────
    public static void renderOutline(ModelRenderer modelRenderer, float scale) {
        if (modelRenderer.isHidden || !modelRenderer.showModel) return;
        if (mode != ESPMode.Outline) return;

        ModelRendererBridge modelRendererBridge = ModelRendererBridge.from(modelRenderer);
        if (!modelRendererBridge.bridge$compiled()) {
            modelRendererBridge.bridge$compileDisplayList(scale);
        }

        Color color = useThemeColor ? ThemeModule.primaryColor : espColor;

        RenderUtil.beginRender();
        GlStateManager.disableLighting();
        GL11.glLineWidth(outlineWidth);

        GlStateManager.pushMatrix();
        GlStateManager.translate(modelRenderer.offsetX, modelRenderer.offsetY, modelRenderer.offsetZ);
        GlStateManager.translate(modelRenderer.rotationPointX * scale, modelRenderer.rotationPointY * 2 * scale, modelRenderer.rotationPointZ * scale);

        if (outlineMode == OutlineRenderMode.Solid) scale *= outlineSize;
        GlStateManager.translate(0, -modelRenderer.rotationPointY * scale, 0);

        GlStateManager.rotate(modelRenderer.rotateAngleZ * (180F / (float) Math.PI), 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(modelRenderer.rotateAngleY * (180F / (float) Math.PI), 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(modelRenderer.rotateAngleX * (180F / (float) Math.PI), 1.0F, 0.0F, 0.0F);

        WorldRenderer renderer = Tessellator.getInstance().getWorldRenderer();
        for (ModelBox box : modelRenderer.cubeList) {
            for (TexturedQuad quad : ModelBoxBridge.from(box).bridge$quadList()) {
                renderer.begin(outlineMode == OutlineRenderMode.Line ? GL11.GL_LINE_LOOP : GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
                for (int i = 0; i < 4; ++i) {
                    PositionTextureVertex v = quad.vertexPositions[i];
                    Render3dUtil.add3DVertexColor(
                            v.vector3D.xCoord * scale,
                            v.vector3D.yCoord * scale,
                            v.vector3D.zCoord * scale,
                            color
                    );
                }
                RenderUtil.getTessalator().draw();
            }
        }

        GlStateManager.popMatrix();
        RenderUtil.resetRender();
        GlStateManager.enableLighting();
        GL11.glLineWidth(1);
    }

    // ─── Filled AABB helper ────────────────────────────────────────────────────
    private static void drawFilledBox(AxisAlignedBB bb) {
        WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);

        double x1 = bb.minX, y1 = bb.minY, z1 = bb.minZ;
        double x2 = bb.maxX, y2 = bb.maxY, z2 = bb.maxZ;

        // Bottom
        wr.pos(x1, y1, z1).endVertex(); wr.pos(x2, y1, z1).endVertex();
        wr.pos(x2, y1, z2).endVertex(); wr.pos(x1, y1, z2).endVertex();
        // Top
        wr.pos(x1, y2, z2).endVertex(); wr.pos(x2, y2, z2).endVertex();
        wr.pos(x2, y2, z1).endVertex(); wr.pos(x1, y2, z1).endVertex();
        // West
        wr.pos(x1, y1, z2).endVertex(); wr.pos(x1, y2, z2).endVertex();
        wr.pos(x1, y2, z1).endVertex(); wr.pos(x1, y1, z1).endVertex();
        // East
        wr.pos(x2, y1, z1).endVertex(); wr.pos(x2, y2, z1).endVertex();
        wr.pos(x2, y2, z2).endVertex(); wr.pos(x2, y1, z2).endVertex();
        // North
        wr.pos(x1, y2, z1).endVertex(); wr.pos(x2, y2, z1).endVertex();
        wr.pos(x2, y1, z1).endVertex(); wr.pos(x1, y1, z1).endVertex();
        // South
        wr.pos(x1, y1, z2).endVertex(); wr.pos(x2, y1, z2).endVertex();
        wr.pos(x2, y2, z2).endVertex(); wr.pos(x1, y2, z2).endVertex();

        Tessellator.getInstance().draw();
    }

    // ─── Health bar (preserved from original) ──────────────────────────────────
    private static final Color BG_COLOUR         = new Color(22, 22, 22);
    private static final Color ABSORPTION_COLOUR = new Color(255, 255, 0);

    private static void renderHealthBar(EntityLivingBase entity, float partialTicks) {
        GL11.glPushMatrix();
        RenderUtil.glTranslate(Render3dUtil.getRelativeEntityPos(entity, partialTicks));
        Render3dUtil.rotateToPlayer(false);

        float healthPercent      = Math.min(entity.getHealth() / entity.getMaxHealth(), 1);
        float extraHealthPercent = TargetUtil.getAbsorption(entity) / entity.getMaxHealth();
        Color healthBarColour    = RenderUtil.getProgressColour(healthPercent);

        float bgW    = 0.1f;
        float bgH    = entity.height;
        float indent = 0.01f;
        float barW   = bgW - indent * 2;
        float barH   = (bgH - indent * 2) * healthPercent;
        float absH   = (bgH - indent * 2) * Math.min(extraHealthPercent, 1);

        RenderUtil.drawRect(entity.width, 0,           bgW,  bgH,  BG_COLOUR);
        RenderUtil.drawRect(entity.width + indent, indent, barW, barH, healthBarColour);
        RenderUtil.drawRect(entity.width + indent, bgH - indent - absH, barW, absH, ABSORPTION_COLOUR);

        GL11.glPopMatrix();
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {}
}
