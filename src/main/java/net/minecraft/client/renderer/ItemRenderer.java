package net.minecraft.client.renderer;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.src.Config;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.MapData;
import net.optifine.DynamicLights;
import net.optifine.reflect.Reflector;
import net.optifine.shaders.Shaders;
import org.lwjgl.opengl.GL11;

public class ItemRenderer
{
    private static final ResourceLocation RES_MAP_BACKGROUND = new ResourceLocation("textures/map/map_background.png");
    private static final ResourceLocation RES_UNDERWATER_OVERLAY = new ResourceLocation("textures/misc/underwater.png");

    /** A reference to the Minecraft object. */
    private final Minecraft mc;
    private ItemStack itemToRender;

    /**
     * How far the current item has been equipped (0 disequipped and 1 fully up)
     */
    private float equippedProgress;
    private float prevEquippedProgress;
    private final RenderManager renderManager;
    private final RenderItem itemRenderer;

    /** The index of the currently held item (0-8, or -1 if not yet updated) */
    private int equippedItemSlot = -1;

    public ItemRenderer(Minecraft mcIn)
    {
        this.mc = mcIn;
        this.renderManager = mcIn.getRenderManager();
        this.itemRenderer = mcIn.getRenderItem();
    }

    public void renderItem(EntityLivingBase entityIn, ItemStack heldStack, ItemCameraTransforms.TransformType transform)
    {
        if (heldStack != null)
        {
            Item item = heldStack.getItem();
            Block block = Block.getBlockFromItem(item);
            GlStateManager.pushMatrix();

            if (this.itemRenderer.shouldRenderItemIn3D(heldStack))
            {
                GlStateManager.scale(2.0F, 2.0F, 2.0F);

                if (this.isBlockTranslucent(block) && (!Config.isShaders() || !Shaders.renderItemKeepDepthMask))
                {
                    GlStateManager.depthMask(false);
                }
            }

            this.itemRenderer.renderItemModelForEntity(heldStack, entityIn, transform);

            if (this.isBlockTranslucent(block))
            {
                GlStateManager.depthMask(true);
            }

            GlStateManager.popMatrix();
        }
    }

    /**
     * Returns true if given block is translucent
     */
    private boolean isBlockTranslucent(Block blockIn)
    {
        return blockIn != null && blockIn.getBlockLayer() == EnumWorldBlockLayer.TRANSLUCENT;
    }

    /**
     * Rotate the render around X and Y
     *  
     * @param angleY The angle for the rotation arround Y
     */
    private void rotateArroundXAndY(float angle, float angleY)
    {
        GlStateManager.pushMatrix();
        GlStateManager.rotate(angle, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(angleY, 0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.popMatrix();
    }

    /**
     * Set the OpenGL LightMapTextureCoords based on the AbstractClientPlayer
     */
    private void setLightMapFromPlayer(AbstractClientPlayer clientPlayer)
    {
        int i = this.mc.theWorld.getCombinedLight(new BlockPos(clientPlayer.posX, clientPlayer.posY + (double)clientPlayer.getEyeHeight(), clientPlayer.posZ), 0);

        if (Config.isDynamicLights())
        {
            i = DynamicLights.getCombinedLight(this.mc.getRenderViewEntity(), i);
        }

        float f = (float)(i & 65535);
        float f1 = (float)(i >> 16);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, f, f1);
    }

    /**
     * Rotate the render according to the player's yaw and pitch
     */
    private void rotateWithPlayerRotations(EntityPlayerSP entityplayerspIn, float partialTicks)
    {
        float f = entityplayerspIn.prevRenderArmPitch + (entityplayerspIn.renderArmPitch - entityplayerspIn.prevRenderArmPitch) * partialTicks;
        float f1 = entityplayerspIn.prevRenderArmYaw + (entityplayerspIn.renderArmYaw - entityplayerspIn.prevRenderArmYaw) * partialTicks;
        GlStateManager.rotate((entityplayerspIn.rotationPitch - f) * 0.1F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate((entityplayerspIn.rotationYaw - f1) * 0.1F, 0.0F, 1.0F, 0.0F);
    }

    /**
     * Return the angle to render the Map
     *  
     * @param pitch The player's pitch
     */
    private float getMapAngleFromPitch(float pitch)
    {
        float f = 1.0F - pitch / 45.0F + 0.1F;
        f = MathHelper.clamp_float(f, 0.0F, 1.0F);
        f = -MathHelper.cos(f * (float)Math.PI) * 0.5F + 0.5F;
        return f;
    }

    private void renderRightArm(RenderPlayer renderPlayerIn)
    {
        GlStateManager.pushMatrix();
        GlStateManager.rotate(54.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(64.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(-62.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.translate(0.25F, -0.85F, 0.75F);
        renderPlayerIn.renderRightArm(this.mc.thePlayer);
        GlStateManager.popMatrix();
    }

    private void renderLeftArm(RenderPlayer renderPlayerIn)
    {
        GlStateManager.pushMatrix();
        GlStateManager.rotate(92.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(45.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(41.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.translate(-0.3F, -1.1F, 0.45F);
        renderPlayerIn.renderLeftArm(this.mc.thePlayer);
        GlStateManager.popMatrix();
    }

    private void renderPlayerArms(AbstractClientPlayer clientPlayer)
    {
        this.mc.getTextureManager().bindTexture(clientPlayer.getLocationSkin());
        Render<AbstractClientPlayer> render = this.renderManager.<AbstractClientPlayer>getEntityRenderObject(this.mc.thePlayer);
        RenderPlayer renderplayer = (RenderPlayer)render;

        if (!clientPlayer.isInvisible())
        {
            GlStateManager.disableCull();
            this.renderRightArm(renderplayer);
            this.renderLeftArm(renderplayer);
            GlStateManager.enableCull();
        }
    }

    private void renderItemMap(AbstractClientPlayer clientPlayer, float pitch, float equipmentProgress, float swingProgress)
    {
        float f = -0.4F * MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float)Math.PI);
        float f1 = 0.2F * MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float)Math.PI * 2.0F);
        float f2 = -0.2F * MathHelper.sin(swingProgress * (float)Math.PI);
        GlStateManager.translate(f, f1, f2);
        float f3 = this.getMapAngleFromPitch(pitch);
        GlStateManager.translate(0.0F, 0.04F, -0.72F);
        GlStateManager.translate(0.0F, equipmentProgress * -1.2F, 0.0F);
        GlStateManager.translate(0.0F, f3 * -0.5F, 0.0F);
        GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(f3 * -85.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(0.0F, 1.0F, 0.0F, 0.0F);
        this.renderPlayerArms(clientPlayer);
        float f4 = MathHelper.sin(swingProgress * swingProgress * (float)Math.PI);
        float f5 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float)Math.PI);
        GlStateManager.rotate(f4 * -20.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(f5 * -20.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(f5 * -80.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(0.38F, 0.38F, 0.38F);
        GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(0.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.translate(-1.0F, -1.0F, 0.0F);
        GlStateManager.scale(0.015625F, 0.015625F, 0.015625F);
        this.mc.getTextureManager().bindTexture(RES_MAP_BACKGROUND);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        GL11.glNormal3f(0.0F, 0.0F, -1.0F);
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos(-7.0D, 135.0D, 0.0D).tex(0.0D, 1.0D).endVertex();
        worldrenderer.pos(135.0D, 135.0D, 0.0D).tex(1.0D, 1.0D).endVertex();
        worldrenderer.pos(135.0D, -7.0D, 0.0D).tex(1.0D, 0.0D).endVertex();
        worldrenderer.pos(-7.0D, -7.0D, 0.0D).tex(0.0D, 0.0D).endVertex();
        tessellator.draw();
        MapData mapdata = Items.filled_map.getMapData(this.itemToRender, this.mc.theWorld);

        if (mapdata != null)
        {
            this.mc.entityRenderer.getMapItemRenderer().renderMap(mapdata, false);
        }
    }

    /**
     * Render the player's arm
     *  
     * @param equipProgress The progress of equiping the item
     * @param swingProgress The swing movement progression
     */
    private void renderPlayerArm(AbstractClientPlayer clientPlayer, float equipProgress, float swingProgress)
    {
        float f = -0.3F * MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float)Math.PI);
        float f1 = 0.4F * MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float)Math.PI * 2.0F);
        float f2 = -0.4F * MathHelper.sin(swingProgress * (float)Math.PI);
        GlStateManager.translate(f, f1, f2);
        GlStateManager.translate(0.64000005F, -0.6F, -0.71999997F);
        GlStateManager.translate(0.0F, equipProgress * -0.6F, 0.0F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float f3 = MathHelper.sin(swingProgress * swingProgress * (float)Math.PI);
        float f4 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float)Math.PI);
        GlStateManager.rotate(f4 * 70.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(f3 * -20.0F, 0.0F, 0.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(clientPlayer.getLocationSkin());
        GlStateManager.translate(-1.0F, 3.6F, 3.5F);
        GlStateManager.rotate(120.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(200.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.scale(1.0F, 1.0F, 1.0F);
        GlStateManager.translate(5.6F, 0.0F, 0.0F);
        Render<AbstractClientPlayer> render = this.renderManager.<AbstractClientPlayer>getEntityRenderObject(this.mc.thePlayer);
        GlStateManager.disableCull();
        RenderPlayer renderplayer = (RenderPlayer)render;
        renderplayer.renderRightArm(this.mc.thePlayer);
        GlStateManager.enableCull();
    }

    /**
     * Rotate and translate render to show item consumption
     *  
     * @param swingProgress The swing movement progress
     */
    private void doItemUsedTransformations(float swingProgress)
    {
        float f = -0.4F * MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float)Math.PI);
        float f1 = 0.2F * MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float)Math.PI * 2.0F);
        float f2 = -0.2F * MathHelper.sin(swingProgress * (float)Math.PI);
        GlStateManager.translate(f, f1, f2);
    }

    /**
     * Perform the drinking animation movement
     *  
     * @param partialTicks Partials ticks
     */
    private void performDrinking(AbstractClientPlayer clientPlayer, float partialTicks)
    {
        float f = (float)clientPlayer.getItemInUseCount() - partialTicks + 1.0F;
        float f1 = f / (float)this.itemToRender.getMaxItemUseDuration();
        float f2 = MathHelper.abs(MathHelper.cos(f / 4.0F * (float)Math.PI) * 0.1F);

        if (f1 >= 0.8F)
        {
            f2 = 0.0F;
        }

        GlStateManager.translate(0.0F, f2, 0.0F);
        float f3 = 1.0F - (float)Math.pow((double)f1, 27.0D);
        GlStateManager.translate(f3 * 0.6F, f3 * -0.5F, f3 * 0.0F);
        GlStateManager.rotate(f3 * 90.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(f3 * 10.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(f3 * 30.0F, 0.0F, 0.0F, 1.0F);
    }

    /**
     * Performs transformations prior to the rendering of a held item in first person.
     */
    private void transformFirstPersonItem(float equipProgress, float swingProgress)
    {
        GlStateManager.translate(0.56F, -0.52F, -0.71999997F);
        GlStateManager.translate(0.0F, equipProgress * -0.6F, 0.0F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float f = MathHelper.sin(swingProgress * swingProgress * (float)Math.PI);
        float f1 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float)Math.PI);
        GlStateManager.rotate(f * -20.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(f1 * -20.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(f1 * -80.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(0.4F, 0.4F, 0.4F);
    }

    /**
     * Translate and rotate the render to look like holding a bow
     *  
     * @param partialTicks Partial ticks
     */
    private void doBowTransformations(float partialTicks, AbstractClientPlayer clientPlayer)
    {
        GlStateManager.rotate(-18.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(-12.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-8.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.translate(-0.9F, 0.2F, 0.0F);
        float f = (float)this.itemToRender.getMaxItemUseDuration() - ((float)clientPlayer.getItemInUseCount() - partialTicks + 1.0F);
        float f1 = f / 20.0F;
        f1 = (f1 * f1 + f1 * 2.0F) / 3.0F;

        if (f1 > 1.0F)
        {
            f1 = 1.0F;
        }

        if (f1 > 0.1F)
        {
            float f2 = MathHelper.sin((f - 0.1F) * 1.3F);
            float f3 = f1 - 0.1F;
            float f4 = f2 * f3;
            GlStateManager.translate(f4 * 0.0F, f4 * 0.01F, f4 * 0.0F);
        }

        GlStateManager.translate(f1 * 0.0F, f1 * 0.0F, f1 * 0.1F);
        GlStateManager.scale(1.0F, 1.0F, 1.0F + f1 * 0.2F);
    }

    /**
     * Translate and rotate the render for holding a block
     */
    private void doBlockTransformations()
    {
        GlStateManager.translate(-0.5F, 0.2F, 0.0F);
        GlStateManager.rotate(30.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-80.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(60.0F, 0.0F, 1.0F, 0.0F);
    }

    /**
     * Renders the active item in the player's hand when in first person mode. Args: partialTickTime
     */
    public void renderItemInFirstPerson(float partialTicks)
    {
        if (!Config.isShaders() || !Shaders.isSkipRenderHand())
        {
            float f = 1.0F - (this.prevEquippedProgress + (this.equippedProgress - this.prevEquippedProgress) * partialTicks);
            AbstractClientPlayer abstractclientplayer = this.mc.thePlayer;
            float f1 = abstractclientplayer.getSwingProgress(partialTicks);
            float f2 = abstractclientplayer.prevRotationPitch + (abstractclientplayer.rotationPitch - abstractclientplayer.prevRotationPitch) * partialTicks;
            float f3 = abstractclientplayer.prevRotationYaw + (abstractclientplayer.rotationYaw - abstractclientplayer.prevRotationYaw) * partialTicks;
            this.rotateArroundXAndY(f2, f3);
            this.setLightMapFromPlayer(abstractclientplayer);
            this.rotateWithPlayerRotations((EntityPlayerSP)abstractclientplayer, partialTicks);
            GlStateManager.enableRescaleNormal();
            GlStateManager.pushMatrix();

            if (this.itemToRender != null)
            {
                if (this.itemToRender.getItem() instanceof ItemMap)
                {
                    this.renderItemMap(abstractclientplayer, f2, f, f1);
                }
                else if (abstractclientplayer.getItemInUseCount() > 0 || coolware.module.combat.KillAura.isBlocking())
                {
                    EnumAction enumaction = this.itemToRender.getItemUseAction();

                    switch (enumaction)
                    {
                        case NONE:
                            this.transformFirstPersonItem(f, 0.0F);
                            break;

                        case EAT:
                        case DRINK:
                            this.performDrinking(abstractclientplayer, partialTicks);
                            this.transformFirstPersonItem(f, 0.0F);
                            break;

                        case BLOCK:
                            if (coolware.config.AnimationConfig.isEnabled())
                            {
                                this.applyCustomBlockAnimation(f, f1, partialTicks);
                            }
                            else
                            {
                                this.transformFirstPersonItem(f, 0.0F);
                                this.doBlockTransformations();
                            }
                            break;

                        case BOW:
                            this.transformFirstPersonItem(f, 0.0F);
                            this.doBowTransformations(partialTicks, abstractclientplayer);
                    }
                }
                else
                {
                    if (coolware.config.AnimationConfig.isEnabled() && coolware.config.AnimationConfig.getRenderMode() == 1)
                    {
                        this.applyCustomBlockAnimation(f, f1, partialTicks);
                    }
                    else
                    {
                        this.doItemUsedTransformations(f1);
                        this.transformFirstPersonItem(f, f1);
                    }
                }

                if (coolware.config.AnimationConfig.isEnabled())
                {
                    double s = (double) coolware.config.AnimationConfig.getScale() / 100.0D * (1.0D + (double) coolware.config.AnimationConfig.getItemSize());
                    GL11.glScaled(s, s, s);
                }

                this.renderItem(abstractclientplayer, this.itemToRender, ItemCameraTransforms.TransformType.FIRST_PERSON);
            }
            else if (!abstractclientplayer.isInvisible())
            {
                this.renderPlayerArm(abstractclientplayer, f, f1);
            }

            GlStateManager.popMatrix();
            GlStateManager.disableRescaleNormal();
            RenderHelper.disableStandardItemLighting();
        }
    }

    /**
     * Renders all the overlays that are in first person mode. Args: partialTickTime
     */
    public void renderOverlays(float partialTicks)
    {
        GlStateManager.disableAlpha();

        if (this.mc.thePlayer.isEntityInsideOpaqueBlock())
        {
            IBlockState iblockstate = this.mc.theWorld.getBlockState(new BlockPos(this.mc.thePlayer));
            BlockPos blockpos = new BlockPos(this.mc.thePlayer);
            EntityPlayer entityplayer = this.mc.thePlayer;

            for (int i = 0; i < 8; ++i)
            {
                double d0 = entityplayer.posX + (double)(((float)((i >> 0) % 2) - 0.5F) * entityplayer.width * 0.8F);
                double d1 = entityplayer.posY + (double)(((float)((i >> 1) % 2) - 0.5F) * 0.1F);
                double d2 = entityplayer.posZ + (double)(((float)((i >> 2) % 2) - 0.5F) * entityplayer.width * 0.8F);
                BlockPos blockpos1 = new BlockPos(d0, d1 + (double)entityplayer.getEyeHeight(), d2);
                IBlockState iblockstate1 = this.mc.theWorld.getBlockState(blockpos1);

                if (iblockstate1.getBlock().isVisuallyOpaque())
                {
                    iblockstate = iblockstate1;
                    blockpos = blockpos1;
                }
            }

            if (iblockstate.getBlock().getRenderType() != -1)
            {
                Object object = Reflector.getFieldValue(Reflector.RenderBlockOverlayEvent_OverlayType_BLOCK);

                if (!Reflector.callBoolean(Reflector.ForgeEventFactory_renderBlockOverlay, new Object[] {this.mc.thePlayer, Float.valueOf(partialTicks), object, iblockstate, blockpos}))
                {
                    this.renderBlockInHand(partialTicks, this.mc.getBlockRendererDispatcher().getBlockModelShapes().getTexture(iblockstate));
                }
            }
        }

        if (!this.mc.thePlayer.isSpectator())
        {
            if (this.mc.thePlayer.isInsideOfMaterial(Material.water) && !Reflector.callBoolean(Reflector.ForgeEventFactory_renderWaterOverlay, new Object[] {this.mc.thePlayer, Float.valueOf(partialTicks)}))
            {
                this.renderWaterOverlayTexture(partialTicks);
            }

            if (this.mc.thePlayer.isBurning() && !Reflector.callBoolean(Reflector.ForgeEventFactory_renderFireOverlay, new Object[] {this.mc.thePlayer, Float.valueOf(partialTicks)}))
            {
                this.renderFireInFirstPerson(partialTicks);
            }
        }

        GlStateManager.enableAlpha();
    }

    /**
     * Render the block in the player's hand
     *  
     * @param partialTicks Partial ticks
     * @param atlas The TextureAtlasSprite to render
     */
    private void renderBlockInHand(float partialTicks, TextureAtlasSprite atlas)
    {
        this.mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        float f = 0.1F;
        GlStateManager.color(0.1F, 0.1F, 0.1F, 0.5F);
        GlStateManager.pushMatrix();
        float f1 = -1.0F;
        float f2 = 1.0F;
        float f3 = -1.0F;
        float f4 = 1.0F;
        float f5 = -0.5F;
        float f6 = atlas.getMinU();
        float f7 = atlas.getMaxU();
        float f8 = atlas.getMinV();
        float f9 = atlas.getMaxV();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos(-1.0D, -1.0D, -0.5D).tex((double)f7, (double)f9).endVertex();
        worldrenderer.pos(1.0D, -1.0D, -0.5D).tex((double)f6, (double)f9).endVertex();
        worldrenderer.pos(1.0D, 1.0D, -0.5D).tex((double)f6, (double)f8).endVertex();
        worldrenderer.pos(-1.0D, 1.0D, -0.5D).tex((double)f7, (double)f8).endVertex();
        tessellator.draw();
        GlStateManager.popMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Renders a texture that warps around based on the direction the player is looking. Texture needs to be bound
     * before being called. Used for the water overlay. Args: parialTickTime
     *  
     * @param partialTicks Partial ticks
     */
    private void renderWaterOverlayTexture(float partialTicks)
    {
        if (!Config.isShaders() || Shaders.isUnderwaterOverlay())
        {
            this.mc.getTextureManager().bindTexture(RES_UNDERWATER_OVERLAY);
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldrenderer = tessellator.getWorldRenderer();
            float f = this.mc.thePlayer.getBrightness(partialTicks);
            GlStateManager.color(f, f, f, 0.5F);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.pushMatrix();
            float f1 = 4.0F;
            float f2 = -1.0F;
            float f3 = 1.0F;
            float f4 = -1.0F;
            float f5 = 1.0F;
            float f6 = -0.5F;
            float f7 = -this.mc.thePlayer.rotationYaw / 64.0F;
            float f8 = this.mc.thePlayer.rotationPitch / 64.0F;
            worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
            worldrenderer.pos(-1.0D, -1.0D, -0.5D).tex((double)(4.0F + f7), (double)(4.0F + f8)).endVertex();
            worldrenderer.pos(1.0D, -1.0D, -0.5D).tex((double)(0.0F + f7), (double)(4.0F + f8)).endVertex();
            worldrenderer.pos(1.0D, 1.0D, -0.5D).tex((double)(0.0F + f7), (double)(0.0F + f8)).endVertex();
            worldrenderer.pos(-1.0D, 1.0D, -0.5D).tex((double)(4.0F + f7), (double)(0.0F + f8)).endVertex();
            tessellator.draw();
            GlStateManager.popMatrix();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.disableBlend();
        }
    }

    /**
     * Renders the fire on the screen for first person mode. Arg: partialTickTime
     *  
     * @param partialTicks Partial ticks
     */
    private void renderFireInFirstPerson(float partialTicks)
    {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 0.9F);
        GlStateManager.depthFunc(519);
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        float f = 1.0F;

        for (int i = 0; i < 2; ++i)
        {
            GlStateManager.pushMatrix();
            TextureAtlasSprite textureatlassprite = this.mc.getTextureMapBlocks().getAtlasSprite("minecraft:blocks/fire_layer_1");
            this.mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
            float f1 = textureatlassprite.getMinU();
            float f2 = textureatlassprite.getMaxU();
            float f3 = textureatlassprite.getMinV();
            float f4 = textureatlassprite.getMaxV();
            float f5 = (0.0F - f) / 2.0F;
            float f6 = f5 + f;
            float f7 = 0.0F - f / 2.0F;
            float f8 = f7 + f;
            float f9 = -0.5F;
            GlStateManager.translate((float)(-(i * 2 - 1)) * 0.24F, -0.3F, 0.0F);
            GlStateManager.rotate((float)(i * 2 - 1) * 10.0F, 0.0F, 1.0F, 0.0F);
            worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
            worldrenderer.setSprite(textureatlassprite);
            worldrenderer.pos((double)f5, (double)f7, (double)f9).tex((double)f2, (double)f4).endVertex();
            worldrenderer.pos((double)f6, (double)f7, (double)f9).tex((double)f1, (double)f4).endVertex();
            worldrenderer.pos((double)f6, (double)f8, (double)f9).tex((double)f1, (double)f3).endVertex();
            worldrenderer.pos((double)f5, (double)f8, (double)f9).tex((double)f2, (double)f3).endVertex();
            tessellator.draw();
            GlStateManager.popMatrix();
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
        GlStateManager.depthFunc(515);
    }

    public void updateEquippedItem()
    {
        this.prevEquippedProgress = this.equippedProgress;
        EntityPlayer entityplayer = this.mc.thePlayer;
        boolean spoofing = coolware.module.movement.Scaffold.isItemSpoofing();
        ItemStack itemstack = spoofing ? coolware.module.movement.Scaffold.getVisualHeldItem() : entityplayer.inventory.getCurrentItem();
        int activeSlot = spoofing ? coolware.module.movement.Scaffold.getVisualSlot() : entityplayer.inventory.currentItem;
        boolean flag = false;

        if (this.itemToRender != null && itemstack != null)
        {
            if (!this.itemToRender.getIsItemStackEqual(itemstack))
            {
                if (Reflector.ForgeItem_shouldCauseReequipAnimation.exists())
                {
                    boolean flag1 = Reflector.callBoolean(this.itemToRender.getItem(), Reflector.ForgeItem_shouldCauseReequipAnimation, new Object[] {this.itemToRender, itemstack, Boolean.valueOf(this.equippedItemSlot != activeSlot)});

                    if (!flag1)
                    {
                        this.itemToRender = itemstack;
                        this.equippedItemSlot = activeSlot;
                        return;
                    }
                }

                flag = true;
            }
        }
        else if (this.itemToRender == null && itemstack == null)
        {
            flag = false;
        }
        else
        {
            flag = true;
        }

        float f2 = 0.4F;
        float f = flag ? 0.0F : 1.0F;
        float f1 = MathHelper.clamp_float(f - this.equippedProgress, -f2, f2);
        this.equippedProgress += f1;

        if (this.equippedProgress < 0.1F)
        {
            this.itemToRender = itemstack;
            this.equippedItemSlot = activeSlot;

            if (Config.isShaders())
            {
                Shaders.setItemToRenderMain(itemstack);
            }
        }
    }

    /**
     * Resets equippedProgress
     */
    public void resetEquippedProgress()
    {
        this.equippedProgress = 0.0F;
    }

    /**
     * Resets equippedProgress
     */
    public void resetEquippedProgress2()
    {
        this.equippedProgress = 0.0F;
    }

    private float customAnimSpin = 0.0F;
    private float customAnimDelay = 0.0F;
    private long customAnimLastUpdate = System.currentTimeMillis();

    private void applyCustomBlockAnimation(float equipProgress, float swingProgress, float partialTicks)
    {
        GL11.glTranslated(
                coolware.config.AnimationConfig.getBlockPosX(),
                coolware.config.AnimationConfig.getBlockPosY(),
                coolware.config.AnimationConfig.getBlockPosZ()
        );

        float sine = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        float sqrtSwing = MathHelper.sqrt_float(swingProgress);
        float sine1 = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);

        coolware.config.AnimationMode m = coolware.config.AnimationConfig.getMode();
        if (m == coolware.config.AnimationMode.EXHIBITION) {
            GL11.glTranslated(0.0D, -0.1D, 0.0D);
            this.transformFirstPersonItem(equipProgress / 2.0F, 0.0F);
            GL11.glTranslatef(0.1F, 0.4F, -0.1F);
            GL11.glRotated(-sine * 30.0F, sine / 2.0F, 0.0D, 9.0D);
            GL11.glRotated(-sine * 50.0F, 0.8D, sine / 2.0F, 0.0D);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.SIGMA) {
            this.transformFirstPersonItem(equipProgress * 0.5F, 0.0F);
            GL11.glRotated(-sine * 27.5F, -8.0D, 0.0D, 9.0D);
            GL11.glRotated(-sine * 45.0F, 1.0D, sine / 2.0F, 0.0D);
            GL11.glTranslated(-0.1D, 0.3D, 0.1D);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.VANILLA) {
            GL11.glTranslated(0.0D, 0.05D, -0.1D);
            this.transformFirstPersonItem(equipProgress, swingProgress);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.PLAIN) {
            GL11.glTranslated(0.0D, 0.05D, 0.0D);
            this.transformFirstPersonItem(equipProgress, 0.0F);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.SPIN) {
            GL11.glRotated(customAnimSpin, 0.0D, 0.0D, -0.1D);
            this.transformFirstPersonItem(equipProgress, 0.0F);
            customAnimSpin = -(System.currentTimeMillis() / 2L % 360L);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.ETB) {
            GL11.glTranslated(0.0D, -0.1D, 0.0D);
            this.transformFirstPersonItem(equipProgress, 0.0F);
            GL11.glTranslatef(0.1F, 0.4F, -0.1F);
            GL11.glRotated(-sine * 35.0F, -8.0D, 0.0D, 9.0D);
            GL11.glRotated(-sine * 70.0F, 1.5D, -0.4D, 0.0D);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.DORTWARE) {
            float alt = MathHelper.sin(sqrtSwing * 3.1415927F - 3.0F);
            this.transformFirstPersonItem(equipProgress, 0.0F);
            GL11.glRotated(-sine * 10.0F, 0.0D, 15.0D, 200.0D);
            GL11.glRotated(-sine * 10.0F, 300.0D, sine / 2.0F, 1.0D);
            GL11.glTranslated(3.4D, 0.3D, -0.4D);
            GL11.glTranslatef(-2.1F, -0.2F, 0.1F);
            GL11.glRotated(alt * 13.0F, -10.0D, -1.4D, -10.0D);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.AVATAR) {
            GL11.glTranslatef(0.56F, -0.52F, -0.72F);
            GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(sine1 * -20.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(sine * -20.0F, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef(sine * -40.0F, 1.0F, 0.0F, 0.0F);
            GL11.glScalef(0.4F, 0.4F, 0.4F);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.SWONG) {
            this.transformFirstPersonItem(equipProgress / 2.0F, 0.0F);
            GL11.glRotated(-sine * 20.0F, sine / 2.0F, 0.0D, 9.0D);
            GL11.glRotated(-sine * 30.0F, 1.0D, sine / 2.0F, 0.0D);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.SWANG) {
            this.transformFirstPersonItem(equipProgress / 2.0F, swingProgress);
            GL11.glRotated(sine * 15.0F, -sine, 0.0D, 9.0D);
            GL11.glRotated(sine * 40.0F, 1.0D, -sine / 2.0F, 0.0D);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.SWANK) {
            this.transformFirstPersonItem(equipProgress / 2.0F, swingProgress);
            GL11.glRotated(sine * 30.0F, -sine, 0.0D, 9.0D);
            GL11.glRotated(sine * 40.0F, 1.0D, -sine, 0.0D);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.STYLES) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            GL11.glTranslatef(-0.05F, 0.2F, 0.0F);
            GL11.glRotated(-sine * 35.0F, -8.0D, 0.0D, 9.0D);
            GL11.glRotated(-sine * 70.0F, 1.0D, -0.4D, 0.0D);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.NUDGE) {
            GL11.glTranslated(-0.1D, 0.09D, 0.0D);
            GL11.glRotated(0.0D, -320.0D, 320.0D, 0.0D);
            this.transformFirstPersonItem(0.0F, 1.0F);
            float ns1 = MathHelper.sin(sqrtSwing * 3.0F);
            float ns2 = MathHelper.sin(sqrtSwing * 4.9415927F);
            GL11.glRotated(-ns1 * 60.0F, -90.0D, -ns2, 10.0D);
            GL11.glRotated(-ns1 * 110.0F, 15.0D, ns2, 0.0D);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.PUNCH) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            GL11.glTranslatef(0.1F, 0.2F, 0.3F);
            GL11.glRotated(-sine * 30.0F, -5.0D, 0.0D, 9.0D);
            GL11.glRotated(-sine * 10.0F, 1.0D, -0.4D, -0.5D);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.SLIDE) {
            GL11.glTranslated(-0.1D, 0.15D, 0.0D);
            this.transformFirstPersonItem(0.0F, 0.0F);
            float ss = MathHelper.sin(sqrtSwing * 2.9415927F);
            GL11.glTranslatef(-0.05F, 0.0F, 0.35F);
            GL11.glRotated(-ss * 30.0F, -15.0D, ss, 10.0D);
            GL11.glRotated(-ss * 70.0D, 5.0D, -ss, 0.0D);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.JIGSAW) {
            GL11.glTranslatef(0.56F, -0.42F, -0.72F);
            GL11.glTranslatef(0.1F * sine, 0.0F, -0.22F * sine);
            GL11.glTranslatef(0.0F, sine1 * -0.15F, 0.0F);
            GL11.glRotated(sine1 * 45.0F, 0.0D, 1.0D, 0.0D);
            GL11.glRotated(sine1 * -20.0F, 0.0D, 1.0D, 0.0D);
            GL11.glRotated(sine * -20.0F, 0.0D, 0.0D, 1.0D);
            GL11.glRotated(sine * -80.0F, 1.0D, 0.0D, 0.0D);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.SWING) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.OLD) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applyOldAnimation();
        } else if (m == coolware.config.AnimationMode.PUSH) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applyPushAnimation(swingProgress);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.DASH) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applyDashAnimation(swingProgress);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.SLASH) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applySlashAnimation(swingProgress);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.SCALE) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applyScaleAnimation(swingProgress);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.SWONK) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applySwonkAnimation(swingProgress);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.STELLA) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applyStellaAnimation();
        } else if (m == coolware.config.AnimationMode.SMALL) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applySmallAnimation();
        } else if (m == coolware.config.AnimationMode.EDIT) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applyEditAnimation(swingProgress);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.RHYS) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applyRhysAnimation(swingProgress);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.STAB) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applyStabAnimation(swingProgress);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.FLOAT) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applyFloatAnimation(swingProgress);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.REMIX) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applyRemixAnimation(swingProgress);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.XIV) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applyXivAnimation(swingProgress);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.WINTER) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applyWinterAnimation();
        } else if (m == coolware.config.AnimationMode.YAMATO) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applyYamatoAnimation(swingProgress);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.SLIDE_SWING) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applySlideSwingAnimation(swingProgress);
        } else if (m == coolware.config.AnimationMode.SMALL_PUSH) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applySmallPushAnimation(swingProgress);
        } else if (m == coolware.config.AnimationMode.REVERSE) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applyReverseAnimation();
        } else if (m == coolware.config.AnimationMode.INVENT) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applyInventAnimation(swingProgress);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.LEAKED) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applyLeakedAnimation(swingProgress);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.AQUA) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applyAquaAnimation(swingProgress);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.ASTRO) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applyAstroAnimation(swingProgress);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.FADEAWAY) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applyFadeawayAnimation(swingProgress);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.ASTOLFO) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applyAstolfoAnimation(swingProgress);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.ASTOLFO_SPIN) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applyAstolfoSpinAnimation();
        } else if (m == coolware.config.AnimationMode.MOON) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applyMoonAnimation(swingProgress);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.MOON_PUSH) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applyMoonPushAnimation(swingProgress);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.SMOOTH) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applySmoothAnimation(swingProgress);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.TAP1) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applyTap1Animation(swingProgress);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.TAP2) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applyTap2Animation(swingProgress);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.SIGMA3) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applySigma3Animation(swingProgress);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.SIGMA4) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applySigma4Animation(swingProgress);
        } else if (m == coolware.config.AnimationMode.MYAU_1_8) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.MYAU_SLIDE) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applyMyauSlideAnimation(swingProgress);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.MYAU_SWANK) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applyMyauSwankAnimation(swingProgress);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.MYAU_SWANG) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applyMyauSwangAnimation(swingProgress);
            this.doBlockTransformations();
        } else if (m == coolware.config.AnimationMode.MYAU_AVATAR) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applyMyauAvatarAnimation(swingProgress);
        } else if (m == coolware.config.AnimationMode.MYAU_JIGSAW) {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            applyMyauJigsawAnimation();
        } else {
            this.transformFirstPersonItem(equipProgress, 0.0F);
            this.doBlockTransformations();
        }
    }

    private void applyOldAnimation() {
        GL11.glTranslated(0.08D, -0.14D, -0.05D);
        GlStateManager.translate(-0.35F, 0.2F, 0.0F);
        this.doBlockTransformations();
    }

    private void applyPushAnimation(float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(-swing * 20.0F, swing / 2.0F, 1.0F, 4.0F);
        GlStateManager.rotate(-swing * 30.0F, 1.0F, swing / 3.0F, -0.0F);
    }

    private void applyDashAnimation(float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GL11.glRotated(-swing * 22.0F, swing / 2.0F, 0.0F, 9.0F);
        GL11.glRotated(-swing * 50.0F, 0.8F, swing / 2.0F, 0.0F);
    }

    private void applySlashAnimation(float swingProgress) {
        GL11.glTranslated(0.08D, 0.08D, 0.0D);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(-swing * 70.0F, 5.0F, 13.0F, 50.0F);
    }

    private void applyScaleAnimation(float swingProgress) {
        GL11.glTranslated(0.84D, -0.77D, -1.1D);
        GlStateManager.translate(0.56F, -0.52F, -0.71999997F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float sine1 = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
        float sine = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(sine1 * -27.0F, 0.0F, 0.0F, 0.0F);
        GlStateManager.rotate(sine * -27.0F, 0.0F, 0.0F, 0.0F);
        GlStateManager.rotate(sine * -27.0F, 0.0F, 0.0F, 0.0F);
    }

    private void applyMyauSlideAnimation(float swingProgress) {
        GL11.glTranslated(0.08D, -0.11D, -0.07D);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.translate(-0.4F, 0.28F, 0.0F);
        GlStateManager.rotate(-swing * 35.0F, -8.0F, -0.0F, 9.0F);
        GlStateManager.rotate(-swing * 70.0F, 1.0F, -0.4F, -0.0F);
    }

    private void applyMyauSwankAnimation(float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GL11.glRotatef(swing * 15.0F, -swing, -0.0F, 9.0F);
        GL11.glRotatef(swing * 40.0F, 1.0F, -swing / 2.0F, -0.0F);
    }

    private void applyMyauSwangAnimation(float swingProgress) {
        GL11.glTranslated(0.0D, 0.03D, 0.0D);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(-swing * 37.0F, swing / 2.0F, 1.0F, 4.0F);
        GlStateManager.rotate(-swing * 52.0F, 1.0F, swing / 3.0F, -0.0F);
    }

    private void applyMyauAvatarAnimation(float swingProgress) {
        GlStateManager.translate(0.56F, -0.52F, -0.72F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float sine1 = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
        float sine = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(sine1 * -20.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(sine * -20.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(sine * -40.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(0.4F, 0.4F, 0.4F);
        this.doBlockTransformations();
    }

    private void applyMyauJigsawAnimation() {
        GL11.glTranslated(0.0D, -0.18D, -0.1D);
        GlStateManager.translate(-0.5D, 0.0D, 0.0D);
        this.doBlockTransformations();
    }

    private void applySwonkAnimation(float swingProgress) {
        GL11.glTranslated(0.0D, 0.03D, 0.0D);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GL11.glRotated(-swing * -15.0F, swing / 2.0F, 1.0F, 4.0F);
        GL11.glRotated(-swing * 7.5F, 1.0F, swing / 3.0F, -0.0F);
    }

    private void applyStellaAnimation() {
        GlStateManager.translate(-0.5F, 0.3F, -0.2F);
        GlStateManager.rotate(32.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-70.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(40.0F, 0.0F, 1.0F, 0.0F);
        this.doBlockTransformations();
    }

    private void applySmallAnimation() {
        GL11.glTranslated(-0.01D, 0.03D, -0.24D);
        this.doBlockTransformations();
    }

    private void applyEditAnimation(float swingProgress) {
        GL11.glTranslated(-0.04D, 0.06D, 0.0D);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(swing * 8.0F, -swing, -0.0F, 2.0F);
        GlStateManager.rotate(swing * 22.0F, 1.0F, -swing / 3.0F, -0.0F);
    }

    private void applyRhysAnimation(float swingProgress) {
        GL11.glTranslated(0.0D, 0.19D, 0.0D);
        GlStateManager.translate(0.41F, -0.25F, -0.5555557F);
        GlStateManager.rotate(35.0F, 0.0F, 1.5F, 0.0F);
        float slowSwing = MathHelper.sin(swingProgress * swingProgress / 64.0F * 3.1415927F);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(slowSwing * -5.0F, 0.0F, 0.0F, 0.0F);
        GlStateManager.rotate(swing * -12.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(swing * -65.0F, 1.0F, 0.0F, 0.0F);
    }

    private void applyStabAnimation(float swingProgress) {
        GL11.glTranslated(-0.25D, 0.45D, 0.8D);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.translate(0.6F, 0.3F, -0.6F + -swing * 0.7F);
        GlStateManager.rotate(6090.0F, 0.0F, 0.0F, 0.1F);
        GlStateManager.rotate(6085.0F, 0.0F, 0.1F, 0.0F);
        GlStateManager.rotate(6110.0F, 0.1F, 0.0F, 0.0F);
    }

    private void applyFloatAnimation(float swingProgress) {
        float swing = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
        GlStateManager.rotate(-swing * 20.0F, swing / 2.0F, -0.0F, 9.0F);
        GlStateManager.rotate(-swing * 30.0F, 1.0F, swing / 2.0F, -0.0F);
    }

    private void applyRemixAnimation(float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(0.0F, -2.0F, 0.0F, 10.0F);
        GlStateManager.rotate(-swing * 25.0F, 0.5F, 0.0F, 1.0F);
    }

    private void applyXivAnimation(float swingProgress) {
        float sine = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        float sine1 = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
        GlStateManager.rotate(-sine1 * 20.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-sine * 20.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(-sine * 80.0F, 1.0F, 0.0F, 0.0F);
    }

    private void applyWinterAnimation() {
        GL11.glTranslated(0.0D, -0.16D, 0.0D);
        GL11.glTranslatef(-0.35F, 0.1F, 0.0F);
        GL11.glTranslatef(-0.05F, -0.1F, 0.1F);
        this.doBlockTransformations();
    }

    private void applyYamatoAnimation(float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GL11.glRotatef(-swing * 100.0F, -9.0F, 5.0F, 9.0F);
    }

    private void applySlideSwingAnimation(float swingProgress) {
        GlStateManager.translate(0.56F, -0.52F, -0.72F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(swing * -80.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(0.4F, 0.4F, 0.4F);
        this.doBlockTransformations();
    }

    private void applySmallPushAnimation(float swingProgress) {
        GlStateManager.translate(0.56F, -0.52F, -0.72F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float sine1 = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
        float sine = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(sine1 * -10.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.rotate(sine * -10.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.rotate(sine * -10.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.scale(0.4F, 0.4F, 0.4F);
        this.doBlockTransformations();
    }

    private void applyReverseAnimation() {
        GL11.glTranslated(0.0D, 0.1D, -0.12D);
        GL11.glTranslated(0.08D, -0.1D, -0.3D);
        this.doBlockTransformations();
    }

    private void applyInventAnimation(float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(-swing * 30.0F, -8.0F, -0.2F, 9.0F);
    }

    private void applyLeakedAnimation(float swingProgress) {
        GL11.glTranslated(0.08D, 0.02D, 0.0D);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(-swing * 41.0F, 1.1F, 0.8F, -0.3F);
    }

    private void applyAquaAnimation(float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(-swing * 8.5F, swing / 2.0F, 1.0F, 4.0F);
        GlStateManager.rotate(-swing * 6.0F, 1.0F, swing / 3.0F, -0.0F);
    }

    private void applyAstroAnimation(float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(swing * 50.0F / 9.0F, -swing, -0.0F, 90.0F);
        GlStateManager.rotate(swing * 50.0F, 200.0F, -swing / 2.0F, -0.0F);
    }

    private void applyFadeawayAnimation(float swingProgress) {
        float sine1 = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
        GlStateManager.rotate(-sine1 * 45.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(0.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(0.0F, 1.5F, 0.0F, 0.0F);
    }

    private void applyAstolfoAnimation(float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(-swing * 29.0F, swing / 2.0F, 1.0F, 0.5F);
        GlStateManager.rotate(-swing * 43.0F, 1.0F, swing / 3.0F, -0.0F);
    }

    private void applyAstolfoSpinAnimation() {
        GlStateManager.rotate(this.customAnimDelay, 0.0F, 0.0F, -0.1F);
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - customAnimLastUpdate;
        this.customAnimDelay += elapsedTime * 360.0F / 850.0F;
        customAnimLastUpdate = currentTime;
        if (this.customAnimDelay > 360.0F) {
            this.customAnimDelay = 0.0F;
        }
        this.doBlockTransformations();
    }

    private void applyMoonAnimation(float swingProgress) {
        GL11.glTranslated(-0.08D, 0.12D, 0.0D);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(-swing * 32.5F, swing / 2.0F, 1.0F, 4.0F);
        GlStateManager.rotate(-swing * 60.0F, 1.0F, swing / 3.0F, -0.0F);
    }

    private void applyMoonPushAnimation(float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.translate(-0.2F, 0.45F, 0.25F);
        GlStateManager.rotate(-swing * 20.0F, -5.0F, -5.0F, 9.0F);
    }

    private void applySmoothAnimation(float swingProgress) {
        GL11.glTranslated(0.14D, -0.1D, -0.24D);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.translate(-0.36F, 0.25F, -0.06F);
        GlStateManager.rotate(-swing * 35.0F, -8.0F, -0.0F, 9.0F);
        GlStateManager.rotate(-swing * 70.0F, 1.0F, 0.4F, -0.0F);
    }

    private void applyTap1Animation(float swingProgress) {
        GlStateManager.translate(0.56F, -0.52F, -0.71999997F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate((swingProgress * 0.8F - swingProgress * swingProgress * 0.8F) * -90.0F,
                0.0F, 1.0F, 0.0F);
        GlStateManager.scale(0.37F, 0.37F, 0.37F);
    }

    private void applyTap2Animation(float swingProgress) {
        GL11.glTranslated(0.0D, -0.1D, 0.0D);
        GlStateManager.translate(0.56F, -0.42F, -0.71999997F);
        GlStateManager.rotate(30.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F) * -30.0F,
                0.0F, 1.0F, 0.0F);
        GlStateManager.scale(0.4F, 0.4F, 0.4F);
    }

    private void applySigma3Animation(float swingProgress) {
        GL11.glTranslated(0.02D, 0.02D, 0.0D);
        GL11.glTranslated(0.4D, -0.06D, -0.46D);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(swing * 12.5F, -swing, -0.0F, 9.0F);
        GlStateManager.rotate(swing * 15.0F, 1.0F, -swing / 2.0F, -0.0F);
    }

    private void applySigma4Animation(float swingProgress) {
        GL11.glTranslated(-0.6D, 0.2D, 0.11D);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(-swing * 27.5F, -8.0F, -0.0F, 9.0F);
        GlStateManager.rotate(-swing * 45.0F, 1.0F, swing / 2.0F, 0.0F);
        this.doBlockTransformations();
        GL11.glTranslated(-0.08D, -1.25D, 1.25D);
    }
}
