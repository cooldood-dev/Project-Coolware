package com.github.cooldood.mixins.net.minecraft.block;

import com.google.common.base.Objects;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.statemap.BlockStateMapper;
import net.minecraft.client.renderer.block.statemap.DefaultStateMapper;
import net.minecraft.client.renderer.block.statemap.IStateMapper;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.init.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(BlockStateMapper.class)
public abstract class StateMapperBaseMixin {
    @Shadow
    private Map<Block, IStateMapper> blockStateMap;

    @Inject(method = "putAllStateModelLocations", at = @At("RETURN"))
    private void addBarrierTexture(CallbackInfoReturnable<Map<IBlockState, ModelResourceLocation>> cir) {
        cir.getReturnValue().putAll(Objects.firstNonNull(this.blockStateMap.get(Blocks.barrier), new DefaultStateMapper()).putStateModelLocations(Blocks.barrier));
    }

}
