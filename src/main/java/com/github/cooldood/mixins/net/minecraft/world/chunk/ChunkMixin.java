package com.github.cooldood.mixins.net.minecraft.world.chunk;

import com.github.cooldood.utils.minecraft.BlockTracker;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Chunk.class)
public class ChunkMixin {
    @Shadow @Final public int xPosition;
    @Shadow @Final public int zPosition;

    @Inject(method = "fillChunk", at = @At("RETURN"))
    public void fillChunk(CallbackInfo ci) {
        BlockTracker.addLoadedChunk(this.xPosition, this.zPosition);
    }

    @Inject(method = "onChunkUnload", at = @At("TAIL"))
    public void onChunkUnload(CallbackInfo ci) {
        BlockTracker.removeLoadedChunk(this.xPosition, this.zPosition);
    }

    @Inject(method = "setBlockState", at = @At("HEAD"))
    public void onSetBlockState(BlockPos pos, IBlockState state, CallbackInfoReturnable<IBlockState> cir) {
        BlockTracker.updateBlock(pos, state.getBlock());
    }
}
