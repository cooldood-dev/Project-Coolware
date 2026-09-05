package com.github.cooldood.bridge.net.minecraft;

import net.minecraft.entity.DataWatcher;

public interface DataWatchedBridge {
    static DataWatchedBridge from(Object instance) {
        return (DataWatchedBridge) instance;
    }

    DataWatcher.WatchableObject bridge$getWatchedObject(int index);
}