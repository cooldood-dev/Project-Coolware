package com.github.cooldood.bridge.net.minecraft.item;

import net.minecraft.item.Item;

public interface ItemSwordBridge {
    static ItemSwordBridge from(Object instance) {
        return (ItemSwordBridge) instance;
    }

    Item.ToolMaterial bridge$getMaterial();
}
