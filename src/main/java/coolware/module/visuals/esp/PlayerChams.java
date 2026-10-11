package coolware.module.visuals.esp;

import coolware.util.RenderUtil;
import coolware.util.render.ColorUtil;
import java.awt.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.player.EntityPlayer;

public class PlayerChams {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final ESPColor espColor;

    public PlayerChams(ESPColor espColor) {
        this.espColor = espColor;
    }

    public void render3D(float partialTicks, boolean staticColor) {
        if (mc.theWorld == null || mc.thePlayer == null || mc.getRenderManager() == null) return;
        if (mc.getRenderManager().livingPlayer == null) {
            mc.getRenderManager().livingPlayer = mc.getRenderViewEntity() != null ? mc.getRenderViewEntity() : mc.thePlayer;
        }

        for (EntityPlayer entityplayer : mc.theWorld.playerEntities) {
            if (entityplayer == null) continue;
            Render<EntityPlayer> render = mc.getRenderManager().getEntityRenderObject(entityplayer);
            if (render != null
                && (entityplayer != mc.thePlayer || mc.gameSettings.thirdPersonView != 0)
                && RenderUtil.isInViewFrustrum(entityplayer)
                && !entityplayer.isDead) {

                Color color = entityplayer.hurtTime > 0 ? Color.RED : ColorUtil.mix(this.getColor(entityplayer), Color.WHITE, 0.4);
                if (color.getAlpha() > 0) {
                    double d0 = entityplayer.prevPosX + (entityplayer.posX - entityplayer.prevPosX) * partialTicks;
                    double d1 = entityplayer.prevPosY + (entityplayer.posY - entityplayer.prevPosY) * partialTicks;
                    double d2 = entityplayer.prevPosZ + (entityplayer.posZ - entityplayer.prevPosZ) * partialTicks;
                    float f1 = entityplayer.prevRotationYaw + (entityplayer.rotationYaw - entityplayer.prevRotationYaw) * partialTicks;
                    if (staticColor) {
                        RendererLivingEntity.setShaderBrightness(color);
                    }

                    double d3 = d0 - mc.getRenderManager().renderPosX;
                    double d4 = d1 - mc.getRenderManager().renderPosY;
                    double d5 = d2 - mc.getRenderManager().renderPosZ;
                    render.doRender(entityplayer, d3, d4, d5, f1, partialTicks);
                    if (staticColor) {
                        RendererLivingEntity.No();
                    }
                }
            }
        }

        RenderHelper.disableStandardItemLighting();
        mc.entityRenderer.disableLightmap();
    }

    public Color getColor(EntityPlayer player) {
        if (player == null) return this.espColor.getColor();
        if (player.hurtTime > 0) return this.espColor.getHurtColor();
        return this.espColor.getColor();
    }
}
