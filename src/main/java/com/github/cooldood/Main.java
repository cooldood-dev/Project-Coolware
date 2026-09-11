package com.github.cooldood;

import com.github.cooldood.commands.CommandManager;
import com.github.cooldood.modules.impl.client.AutoQueueHandler;
import com.github.cooldood.modules.ModuleManager;
import com.github.cooldood.utils.alts.microsoft.AuthServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.Display;

import java.io.File;

@Mod(modid = Main.MOD_ID, name = Main.MOD_NAME, version = Main.MOD_VERSION)
public class Main {
    public static final String MOD_ID = "@MOD_ID@";
    public static final String MOD_NAME = "@MOD_NAME@";
    public static final String MOD_VERSION = "@MOD_VERSION@";

    public static final String baseConfig = "base";

    public static final String baseFolderPath = "config/" + Main.MOD_ID + "/";
    public static final String configPath = baseFolderPath + "config/";
    public static final String configExtension = ".cfg";
    public static final String extraSavedFeaturesPath = baseFolderPath + "extras/";
    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    public static Configuration autoQueueConfig;

    @EventHandler
    public void onPreInit(FMLPreInitializationEvent event) {
        File autoQueueFile = new File(extraSavedFeaturesPath, "autoqueue.cfg");
        autoQueueConfig = new Configuration(autoQueueFile);
        autoQueueConfig.load();
        AutoQueueHandler.loadConfig(autoQueueConfig);
        if (autoQueueConfig.hasChanged()) {
            autoQueueConfig.save();
        }
    }

    @EventHandler
    public void onInit(FMLInitializationEvent event) {
        Display.setTitle(MOD_NAME + " " + MOD_VERSION);

        ModuleManager.init();
        CommandManager.init();
        MinecraftForge.EVENT_BUS.register(new AutoQueueHandler());

        new AuthServer();
    }
}
