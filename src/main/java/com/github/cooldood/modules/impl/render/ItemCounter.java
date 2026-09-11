package com.github.cooldood.modules.impl.render;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.RenderWorldEvent;
import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.utils.client.C;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RegisterModule(
        name = "ItemCounter",
        description = "Shows the count of held items on screen.",
        category = Category.RENDER
)
public class ItemCounter extends Module {

    @Override
    protected void onEnable() {}
    @Override
    protected void onDisable() {}

    @SubscribeEvent
    public static void onRenderWorld(RenderWorldEvent event) {
        if (C.mc.getRenderManager() == null || C.mc.getRenderManager().options == null) return;

        List<Entity> loadedEntities = C.w().loadedEntityList;
        List<EntityItem> items = new ArrayList<>();

        for (Entity e : loadedEntities) {
            if (e instanceof EntityItem && e.isEntityAlive()) {
                items.add((EntityItem) e);
            }
        }

        Map<EntityItem, List<EntityItem>> clusters = new HashMap<>();
        List<EntityItem> handled = new ArrayList<>();

        for (EntityItem item : items) {
            if (handled.contains(item)) continue;

            List<EntityItem> cluster = new ArrayList<>();
            cluster.add(item);
            handled.add(item);

            ItemStack itemStack = item.getEntityItem();
            if (itemStack == null || itemStack.getItem() == null) continue;

            for (EntityItem other : items) {
                if (handled.contains(other)) continue;
                
                ItemStack otherStack = other.getEntityItem();
                if (otherStack == null || otherStack.getItem() == null) continue;

                if (itemStack.getItem() == otherStack.getItem() && itemStack.getMetadata() == otherStack.getMetadata()) {
                    if (item.getDistanceToEntity(other) < 2.0f) {
                        cluster.add(other);
                        handled.add(other);
                    }
                }
            }
            clusters.put(item, cluster);
        }

        double renderPosX = C.mc.getRenderManager().viewerPosX;
        double renderPosY = C.mc.getRenderManager().viewerPosY;
        double renderPosZ = C.mc.getRenderManager().viewerPosZ;

        for (Map.Entry<EntityItem, List<EntityItem>> entry : clusters.entrySet()) {
            EntityItem base = entry.getKey();
            List<EntityItem> cluster = entry.getValue();

            int totalCount = 0;
            double avgX = 0, avgY = 0, avgZ = 0;

            for (EntityItem i : cluster) {
                totalCount += i.getEntityItem().stackSize;
                avgX += i.posX;
                avgY += i.posY;
                avgZ += i.posZ;
            }

            avgX /= cluster.size();
            avgY /= cluster.size();
            avgZ /= cluster.size();

            ItemStack stack = base.getEntityItem();
            String itemName = stack.getDisplayName();
            String text = totalCount + "x " + itemName;

            double x = avgX - renderPosX;
            double y = avgY - renderPosY + 1.0; 
            double z = avgZ - renderPosZ;

            renderTag(text, x, y, z);
        }
    }

    private static void renderTag(String text, double x, double y, double z) {
        float viewerYaw = C.mc.getRenderManager().playerViewY;
        float viewerPitch = C.mc.getRenderManager().playerViewX;
        boolean isThirdPersonFrontal = C.mc.getRenderManager().options.thirdPersonView == 2;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(-viewerYaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate((float)(isThirdPersonFrontal ? -1 : 1) * viewerPitch, 1.0F, 0.0F, 0.0F);
        
        float scale = 0.016666668F * 1.6F;
        GlStateManager.scale(-scale, -scale, scale);

        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        FontRenderer fontrenderer = C.mc.fontRendererObj;
        int textWidth = fontrenderer.getStringWidth(text);
        int halfWidth = textWidth / 2;

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.5F);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2d(-halfWidth - 2, -2);
        GL11.glVertex2d(-halfWidth - 2, 10);
        GL11.glVertex2d(halfWidth + 2, 10);
        GL11.glVertex2d(halfWidth + 2, -2);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        fontrenderer.drawString(text, -halfWidth, 0, -1);

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }
}
