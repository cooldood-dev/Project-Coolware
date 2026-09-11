package com.github.cooldood.modules.impl.render;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.RenderWorldEvent;
import com.github.cooldood.modules.*;
import com.github.cooldood.modules.impl.player.Fucker;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.minecraft.BlockTracker;
import com.github.cooldood.utils.minecraft.WorldUtil;
import com.github.cooldood.utils.render.Render3dUtil;
import com.github.cooldood.utils.render.RenderUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;

@RegisterModule(
        name = "Bed ESP",
        description = "Provides Bed ESP functionality for the client.",
        category = Category.RENDER
)
public class BedESP extends Module {
    @RegisterSubModule(name = "Plates")
    public static boolean plates = true;

    @RegisterSubModule(name = "Highlight")
    public static boolean highlight = false;
    @RegisterSubModule(name = "Colour", parent = "Highlight")
    public static Color colour = new Color(255, 167, 167, 50);

    @SubscribeEvent
    public static void drawBeds(RenderWorldEvent event) {
        ArrayList<BlockPos> beds = BlockTracker.getBlockPositions(Blocks.bed);

        if (beds == null) return;

        for (BlockPos blockPos : beds) {
            IBlockState state = C.w().getBlockState(blockPos);

            if (!state.getProperties().containsKey(BlockBed.PART) || !state.getProperties().containsKey(BlockBed.FACING)) continue;
            if (state.getValue(BlockBed.PART) != BlockBed.EnumPartType.FOOT) continue;

            if (highlight) {
                EnumFacing facing = state.getValue(BlockBed.FACING);
                Vec3 size = new Vec3(1.0F + Math.abs(facing.getFrontOffsetX()), 0.5625F, 1.0F + Math.abs(facing.getFrontOffsetZ()));
                Render3dUtil.draw3dBox(blockPos.getX() + Math.min(facing.getFrontOffsetX(), 0), blockPos.getY(), blockPos.getZ() + Math.min(facing.getFrontOffsetZ(), 0), size.xCoord, size.yCoord, size.zCoord, colour, event.partialTicks);
            }

            if (plates) {
                BlockPos bestSurrounding = WorldUtil.getBestBlockSurroundingBed(blockPos);

                if (bestSurrounding == null) continue;
                Block bestBlock = C.w().getBlockState(bestSurrounding).getBlock();
                if (bestBlock == Blocks.air) continue;
                IBlockState bestState = C.w().getBlockState(bestSurrounding);

                GL11.glPushMatrix();
                Vec3 relativeBlockPos = Render3dUtil.getRelativePos(new Vec3(blockPos.getX() + 0.5, blockPos.getY() + 1.5, blockPos.getZ() + 0.5), event.partialTicks);
                RenderUtil.glTranslate(relativeBlockPos);
                Render3dUtil.rotateToPlayer(true);

                RenderUtil.drawRoundedRect(-0.25f, -0.25f, 0.5f, 0.5f, 0.1f, new Color(22, 22, 22, 100));

                C.mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
                GL11.glScaled(0.5, 0.5, 0.5);

                RenderUtil.drawRectSprite(-0.25f, -0.25f, 0.5f, 0.5f, Color.WHITE, C.mc.getBlockRendererDispatcher().getBlockModelShapes().getModelForState(bestState).getParticleTexture());

                GL11.glPopMatrix();
            }
        }
    }

    @Override
    protected void onEnable() {
        if (!ModuleManager.isEnabled(Fucker.class)) BlockTracker.beginTracking(Blocks.bed);
    }

    @Override
    protected void onDisable() {
        if (!ModuleManager.isEnabled(Fucker.class)) BlockTracker.stopTracking(Blocks.bed);
    }
}
