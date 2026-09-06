package com.github.cooldood.utils.client;

import javazoom.jl.player.Player;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.BufferedInputStream;
import java.io.InputStream;

public class SoundUtil {

    public static void playSound(String resourcePath) {
        new Thread(() -> {
            try {
                InputStream is = SoundUtil.class.getResourceAsStream(resourcePath);
                if (is == null) {
                    is = SoundUtil.class.getClassLoader().getResourceAsStream(resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath);
                }
                if (is == null) {
                    is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath);
                }
                if (is == null) {
                    System.err.println("[CoolWare Sound] Sound resource not found: " + resourcePath);
                    return;
                }

                if (resourcePath.toLowerCase().endsWith(".mp3")) {
                    try (BufferedInputStream bis = new BufferedInputStream(is)) {
                        Player player = new Player(bis);
                        player.play();
                    }
                } else {
                    InputStream bufferedIn = new BufferedInputStream(is);
                    AudioInputStream audioIn = AudioSystem.getAudioInputStream(bufferedIn);
                    Clip clip = AudioSystem.getClip();
                    clip.open(audioIn);
                    clip.start();
                    clip.addLineListener(event -> {
                        if (event.getType() == javax.sound.sampled.LineEvent.Type.STOP) {
                            clip.close();
                        }
                    });
                }
            } catch (Exception e) {
                System.err.println("[CoolWare Sound] Failed to play sound: " + resourcePath);
                e.printStackTrace();
            }
        }, "CoolWare-SoundThread").start();
    }
}
