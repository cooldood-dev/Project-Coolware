package com.github.cooldood.mixins.net;

import com.github.cooldood.bridge.net.minecraft.DataWatchedBridge;
import net.minecraft.entity.DataWatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DataWatcher.class)
public abstract class DataWatchableObjectMixin implements DataWatchedBridge {
    @Shadow
    protected abstract DataWatcher.WatchableObject getWatchedObject(int id);

    @Override
    public DataWatcher.WatchableObject bridge$getWatchedObject(int index) {
        return this.getWatchedObject(index);
    }
}
