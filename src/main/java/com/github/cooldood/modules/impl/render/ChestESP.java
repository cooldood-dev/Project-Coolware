package com.github.cooldood.modules.impl.render;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.RenderWorldEvent;
import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.utils.client.C;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.renderer.RenderGlobal;
import com.github.cooldood.utils.tenacity.render.ColorUtil;
import java.awt.Color;

@RegisterModule(
        name = "Chest ESP",
        description = "Draws an outline around chests.",
        category = Category.RENDER
)
public class ChestESP extends Module {

    @SubscribeEvent
    public static void onRenderWorld(RenderWorldEvent event) {
        for (TileEntity entity : C.w().loadedTileEntityList) {
            if (entity instanceof TileEntityChest || entity instanceof TileEntityEnderChest) {
                double x = entity.getPos().getX() - C.mc.getRenderManager().viewerPosX;
                double y = entity.getPos().getY() - C.mc.getRenderManager().viewerPosY;
                double z = entity.getPos().getZ() - C.mc.getRenderManager().viewerPosZ;

                AxisAlignedBB bb = new AxisAlignedBB(x, y, z, x + 1.0, y + 1.0, z + 1.0);
                
                Color color = entity instanceof TileEntityEnderChest ? new Color(200, 0, 255) : new Color(255, 170, 0);

                GL11.glPushMatrix();
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glLineWidth(1.5f);
                
                com.github.cooldood.utils.tenacity.render.TRenderUtil.resetColor();
                GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 1f);
                RenderGlobal.drawSelectionBoundingBox(bb);
                
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glPopMatrix();
            }
        }
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {}
}
