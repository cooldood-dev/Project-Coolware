package coolware.module.visuals.esp;

import coolware.Coolware;
import coolware.util.RenderUtil;
import coolware.util.shader.ShaderQueueType;
import java.awt.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.player.EntityPlayer;

public class PlayerGlow {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final ESPColor espColor;

    public PlayerGlow(ESPColor espColor) {
        this.espColor = espColor;
    }

    public void render3D(final float partialTicks) {
        if (mc.theWorld == null || Coolware.getInstance().getShaderRenderManager() == null) return;

        Coolware.getInstance().getShaderRenderManager().getQueue(ShaderQueueType.BLOOM).add(new Runnable() {
            @Override
            public void run() {
                if (mc.theWorld == null || mc.thePlayer == null || mc.getRenderManager() == null) return;
                if (mc.getRenderManager().livingPlayer == null) {
                    mc.getRenderManager().livingPlayer = mc.getRenderViewEntity() != null ? mc.getRenderViewEntity() : mc.thePlayer;
                }
                for (EntityPlayer entityPlayer : mc.theWorld.playerEntities) {
                    if (entityPlayer == null) continue;
                    Render<EntityPlayer> render = mc.getRenderManager().getEntityRenderObject(entityPlayer);
                    if (mc.getRenderManager() != null && render != null) {
                        if (!RenderUtil.isInViewFrustrum(entityPlayer)) {
                            continue;
                        }
                        Color color = entityPlayer.hurtTime > 0 ? Color.RED : PlayerGlow.this.getColor(entityPlayer);
                        if (color.getAlpha() <= 0) {
                            continue;
                        }
                        double x = entityPlayer.prevPosX + (entityPlayer.posX - entityPlayer.prevPosX) * partialTicks;
                        double y = entityPlayer.prevPosY + (entityPlayer.posY - entityPlayer.prevPosY) * partialTicks;
                        double z = entityPlayer.prevPosZ + (entityPlayer.posZ - entityPlayer.prevPosZ) * partialTicks;
                        float yaw = entityPlayer.prevRotationYaw + (entityPlayer.rotationYaw - entityPlayer.prevRotationYaw) * partialTicks;
                        boolean wasInvisible = entityPlayer.isInvisible();
                        entityPlayer.setInvisible(false);
                        RendererLivingEntity.setShaderBrightness(color);
                        double renderX = x - mc.getRenderManager().renderPosX;
                        double renderY = y - mc.getRenderManager().renderPosY;
                        double renderZ = z - mc.getRenderManager().renderPosZ;
                        render.doRender(entityPlayer, renderX, renderY, renderZ, yaw, partialTicks);
                        RendererLivingEntity.No();
                        entityPlayer.setInvisible(wasInvisible);
                    }
                }
                RenderHelper.disableStandardItemLighting();
                mc.entityRenderer.disableLightmap();
            }
        });
    }

    public Color getColor(EntityPlayer player) {
        if (player == null) return this.espColor.getColor();
        if (player.hurtTime > 0) return this.espColor.getHurtColor();
        return this.espColor.getColor();
    }
}
