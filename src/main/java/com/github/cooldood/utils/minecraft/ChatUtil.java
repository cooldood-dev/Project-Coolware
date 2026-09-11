package com.github.cooldood.utils.minecraft;

import com.github.cooldood.Main;
import com.github.cooldood.utils.client.C;
import net.minecraft.util.ChatComponentText;

public class ChatUtil {

    public static void chat(Object message) {
        // i dont have a "§" on my keyboard, easier to type & if i want a color code.
        if (C.isInGame()) C.p().addChatMessage(new ChatComponentText((""+message).replaceAll("&", "§")));
    }

    public static void prefixMessage(Object message) {
        if (C.isInGame()) C.p().addChatMessage(new ChatComponentText(("&c[&f"+Main.MOD_NAME+"&c] " + message).replaceAll("&", "§")));
    }
}