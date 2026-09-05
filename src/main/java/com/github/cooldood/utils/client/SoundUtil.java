package com.github.cooldood.utils.client;

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
                if (is == null) return;
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
