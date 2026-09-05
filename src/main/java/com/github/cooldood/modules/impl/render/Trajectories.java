package com.github.cooldood.modules.impl.render;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.RenderWorldEvent;
import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.render.RenderUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemSnowball;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemExpBottle;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;
import java.awt.Color;
import net.minecraft.client.renderer.RenderGlobal;

@RegisterModule(
        name = "Trajectories",
        description = "Provides Trajectories functionality for the client.",
        category = Category.RENDER
)
public class Trajectories extends Module {
    @Override
    protected void onEnable() {}
    @Override
    protected void onDisable() {}

    @SubscribeEvent
    public static void onRenderWorld(RenderWorldEvent event) {
        EntityPlayer player = C.p();
        if (player == null || player.getCurrentEquippedItem() == null) return;
        
        ItemStack stack = player.getCurrentEquippedItem();
        Item item = stack.getItem();
        
        float gravity = 0.0f;
        float drag = 0.99f;
        float velocity = 0.0f;
        Color color = Color.WHITE;
        
        if (item instanceof ItemBow) {
            int useCount = player.getItemInUseCount();
            if (useCount == 0) return; // Only show when drawing
            
            int maxDuration = stack.getMaxItemUseDuration();
            int ticksInUse = maxDuration - useCount;
            float power = (float) ticksInUse / 20.0f;
            power = (power * power + power * 2.0f) / 3.0f;
            if (power > 1.0f) power = 1.0f;
            if (power < 0.1f) return;
            
            velocity = power * 3.0f;
            gravity = 0.05f;
            color = new Color(255, 255, 255, 200);
        } else if (item instanceof ItemSnowball || item instanceof ItemEgg) {
            gravity = 0.03f;
            velocity = 1.5f;
            color = new Color(180, 220, 255, 200);
        } else if (item instanceof ItemEnderPearl) {
            gravity = 0.03f;
            velocity = 1.5f;
            color = new Color(150, 80, 255, 200);
        } else if (item instanceof ItemPotion) {
            if (!ItemPotion.isSplash(stack.getMetadata())) return;
            gravity = 0.05f;
            velocity = 0.5f;
            color = new Color(100, 255, 150, 200);
        } else if (item instanceof ItemExpBottle) {
            gravity = 0.07f;
            velocity = 0.7f;
            color = new Color(255, 220, 80, 200);
        } else if (item instanceof ItemFishingRod) {
            gravity = 0.04f;
            drag = 0.992f;
            velocity = 0.4f;
            color = new Color(200, 160, 100, 200);
        } else {
            return;
        }

        double renderPosX = C.mc.getRenderManager().viewerPosX;
        double renderPosY = C.mc.getRenderManager().viewerPosY;
        double renderPosZ = C.mc.getRenderManager().viewerPosZ;

        float yaw = player.rotationYaw;
        float pitch = player.rotationPitch;
        
        double posX = renderPosX - (double)(Math.cos(yaw / 180.0F * (float)Math.PI) * 0.16F);
        double posY = renderPosY + (double)player.getEyeHeight() - 0.10000000149011612D;
        double posZ = renderPosZ - (double)(Math.sin(yaw / 180.0F * (float)Math.PI) * 0.16F);
        
        double motionX = (double)(-Math.sin(yaw / 180.0F * (float)Math.PI) * Math.cos(pitch / 180.0F * (float)Math.PI)) * velocity;
        double motionZ = (double)(Math.cos(yaw / 180.0F * (float)Math.PI) * Math.cos(pitch / 180.0F * (float)Math.PI)) * velocity;
        double motionY = (double)(-Math.sin(pitch / 180.0F * (float)Math.PI)) * velocity;
        
        drawTrajectory(posX, posY, posZ, motionX, motionY, motionZ, gravity, drag, color, renderPosX, renderPosY, renderPosZ);
    }

    private static void drawTrajectory(double posX, double posY, double posZ, double motionX, double motionY, double motionZ, float gravity, float drag, Color color, double renderPosX, double renderPosY, double renderPosZ) {
        boolean hasCollided = false;
        MovingObjectPosition hitPos = null;
        
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glLineWidth(2.0f);
        
        RenderUtil.glColor(color);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        
        for (int i = 0; i < 300; i++) {
            GL11.glVertex3d(posX - renderPosX, posY - renderPosY, posZ - renderPosZ);
            
            Vec3 currentVec = new Vec3(posX, posY, posZ);
            Vec3 nextVec = new Vec3(posX + motionX, posY + motionY, posZ + motionZ);
            hitPos = C.w().rayTraceBlocks(currentVec, nextVec, false, true, false);
            
            if (hitPos != null) {
                hasCollided = true;
                break;
            }
            
            posX += motionX;
            posY += motionY;
            posZ += motionZ;
            
            motionX *= drag;
            motionY *= drag;
            motionZ *= drag;
            motionY -= gravity;
        }
        
        GL11.glEnd();
        
        if (hasCollided && hitPos != null) {
            double hx = hitPos.hitVec.xCoord - renderPosX;
            double hy = hitPos.hitVec.yCoord - renderPosY;
            double hz = hitPos.hitVec.zCoord - renderPosZ;
            
            AxisAlignedBB bb = new AxisAlignedBB(hx - 0.1, hy - 0.1, hz - 0.1, hx + 0.1, hy + 0.1, hz + 0.1);
            RenderGlobal.drawSelectionBoundingBox(bb);
        }
        
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glPopMatrix();
    }
}
