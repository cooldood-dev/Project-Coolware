package com.github.cooldood.mixins.net.minecraft.entity;

import com.github.cooldood.modules.ModuleManager;
import com.github.cooldood.modules.impl.client.MovementFix;
import com.github.cooldood.modules.impl.movement.NoJumpDelay;
import com.github.cooldood.modules.impl.render.Animations;
import com.github.cooldood.modules.impl.render.NoRender;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.minecraft.PlayerUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityLivingBase.class)
public class EntityLivingBaseMixin extends Entity {
    @Shadow
    private int jumpTicks;

    public EntityLivingBaseMixin(World worldIn) {
        super(worldIn);
    }

    @Redirect(method = "jump", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;rotationYaw:F"))
    public float editYawOnJump(EntityLivingBase instance) {
        return MovementFix.shouldMoveFix(instance) ? PlayerUtil.currentRotation().yaw : instance.rotationYaw;
    }

    @Inject(method = "isPotionActive(Lnet/minecraft/potion/Potion;)Z", at = @At("HEAD"), cancellable = true)
    public void onIsPotionActive(Potion potionIn, CallbackInfoReturnable<Boolean> cir) {
        if (potionIn == Potion.confusion && NoRender.noNausea() && C.p() != null && this.getEntityId() == C.p().getEntityId()) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(method = "onLivingUpdate", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;jumpTicks:I", ordinal = 4))
    public void onLivingUpdate(EntityLivingBase instance, int value) {
        this.jumpTicks = NoJumpDelay.getDelay();
    }

    /**
     * @author animations (syuto), ported from OpenMyau
     * @reason Custom swing speed
     */
    @Overwrite
    private int getArmSwingAnimationEnd() {
        if (!ModuleManager.isEnabled(Animations.class)) return 6;
        int pct = Math.max(0, Math.min(Animations.swingSpeed, 100));
        return (int) (6.0D + (double) pct / 100.0D * 14.0D);
    }

    @Shadow
    protected void entityInit() {

    }

    @Shadow
    public void readEntityFromNBT(NBTTagCompound tagCompund) {

    }

    @Shadow
    public void writeEntityToNBT(NBTTagCompound tagCompound) {

    }
}