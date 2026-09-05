package com.github.cooldood.modules.impl.player;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.MotionEvent;
import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.modules.RegisterSubModule;

import java.util.Arrays;
import java.util.List;

/*
For those of you that don't know, there are a new pair of boots in the game called Great Spook Boots.

Hypixel, are you honestly not aware that "Spook" is a racist slur for a black person? You really couldn't call them "Spooky" or "Halloween" or "Pumpkin" or literally anything else?!

Don't believe me? Too lazy to google it? Here's the wiki page for the word. Scroll down to Slang uses.
Slang for someone involved in espionage
CIA members, in slang
MI5 members, in slang
A racial slur for a black person

A+ patch guys. Really impressed with how little you pay attention to what you put into the game.

https://hypixel.net/threads/new-great-spook-boots-contain-a-racial-slur-great-job-hypixel.4630582/
 */
@RegisterModule(
        name = "Disabler",
        description = "Disables server-side anticheats.",
        category = Category.PLAYER
)
public class Disabler extends Module {
    @RegisterSubModule(name = "Racism", description = "Disables racism.")
    public static boolean racismDisabler = true;
    @RegisterSubModule(name = "Omnisprint", description = "Disables sending sprint packets.")
    public static boolean omniSprintDisabler = false;


    // most important disabler, racism.
    public static List<String> racistWords = Arrays.asList(
            "nigger",
            "nigga",
            "spook", // cookclops
            "coon",
            "wigga",
            "faggot",
            "monkey",
            "racism",
            "clanker", // new wow

            "cheat", // none of this pls.
            "hack",

            "fuck",
            "shit",
            "bitch",
            "bastard",
            "rape",

            // truly disheartening words. (mushroom client is BANNED from this client.)
            "mushroom",
            "wurst",
            "scale",
            "swig",
            "batman",
            "sideload",
            "sunder"
    );


    // used in the fixText function in nickHider to censor evil racism
    public static String censorRacism(String racism) {
        return racism.replaceFirst("[aeiouAEIOU]", "*");
    }


    @SubscribeEvent
    public static void onPreMotion(MotionEvent event) {
        if (!omniSprintDisabler) return;

        event.sprinting = false;
    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {

    }
}