package com.github.cooldood.modules.impl.render;

import com.github.cooldood.modules.*;

@RegisterModule(
        name = "Chat Fixer",
        description = "Provides Chat Fixer functionality for the client.",
        category = Category.RENDER,
        enabledByDefault = true
)
public class ChatFixer extends Module {
    @RegisterSubModule(name = "No Clear", parent = "Mode", modeParentString = "Line", min = 1, max = 5)
    public static boolean noClearChat = true;

    @RegisterSubModule(name = "Limited Chat")
    public static boolean limitedChat = false;

    @RegisterSubModule(name = "Chat History Length", parent = "Limited Chat", min = 1, max = 5000)
    public static int chatHistoryLength = 100;

    public static int getMaxChatHistory() {
        return ModuleManager.isEnabled(ChatFixer.class) ? limitedChat ? chatHistoryLength : Integer.MAX_VALUE : 100;
    }

    public static boolean shouldClearChat() {
        return !ModuleManager.isEnabled(ChatFixer.class) || !noClearChat;
    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {

    }
}
