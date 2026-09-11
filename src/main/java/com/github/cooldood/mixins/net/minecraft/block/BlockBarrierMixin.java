package com.github.cooldood.mixins.net.minecraft.block;

import com.github.cooldood.modules.ModuleManager;
import com.github.cooldood.modules.impl.render.RenderBarriers;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBarrier;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBarrier.class)
public class BlockBarrierMixin extends Block {
    public BlockBarrierMixin(Material blockMaterialIn, MapColor blockMapColorIn) {
        super(blockMaterialIn, blockMapColorIn);
    }

    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    public void onGetRenderType(CallbackInfoReturnable<Integer> cir) {
        // The type of render function called. 3 for standard block models, 2 for TESR's, 1 for liquids, -1 is no render
        if (ModuleManager.isEnabled(RenderBarriers.class)) cir.setReturnValue(3);
    }

    @Override
    public EnumWorldBlockLayer getBlockLayer() {
        // allows transparent/translucent textures (solid does not)
        return EnumWorldBlockLayer.TRANSLUCENT;
    }

    @Override
    public boolean shouldSideBeRendered(IBlockAccess worldIn, BlockPos pos, EnumFacing side) {
        return worldIn.getBlockState(pos.offset(side.getOpposite())) != worldIn.getBlockState(pos);
    }
}
