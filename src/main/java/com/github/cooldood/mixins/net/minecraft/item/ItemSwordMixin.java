package com.github.cooldood.mixins.net.minecraft.item;

import com.github.cooldood.bridge.net.minecraft.item.ItemSwordBridge;
import net.minecraft.item.Item;
import net.minecraft.item.ItemSword;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ItemSword.class)
public abstract class ItemSwordMixin implements ItemSwordBridge {
    @Shadow
    private Item.ToolMaterial material;

    @Override
    public Item.ToolMaterial bridge$getMaterial() {
        return this.material;
    }
}
