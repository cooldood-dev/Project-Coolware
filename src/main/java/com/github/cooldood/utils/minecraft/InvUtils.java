package com.github.cooldood.utils.minecraft;

import com.github.cooldood.utils.client.C;
import net.minecraft.item.ItemSword;

public class InvUtils {
    public static boolean isHoldingSword() {
        return C.p() != null && C.p().getCurrentEquippedItem() != null && C.p().getCurrentEquippedItem().getItem() instanceof ItemSword;
    }
}
