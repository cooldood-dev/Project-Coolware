package com.github.cooldood.utils.client;

import com.github.cooldood.Main;

import java.awt.*;
import java.io.File;
import java.net.URI;

/**
 * Cross-platform utility for opening files and URLs.
 * Tries java.awt.Desktop first, then falls back to OS-specific commands
 * for Linux (xdg-open), macOS (open), and Windows (cmd /c start).
 */
public class PlatformUtil {

    public enum OS {
        WINDOWS, MACOS, LINUX, UNKNOWN
    }

    private static OS detectedOS;

    /**
     * Detects the current operating system.
     */
    public static OS getOS() {
        if (detectedOS == null) {
            String osName = System.getProperty("os.name", "").toLowerCase();
            if (osName.contains("win")) {
                detectedOS = OS.WINDOWS;
            } else if (osName.contains("mac") || osName.contains("darwin")) {
                detectedOS = OS.MACOS;
            } else if (osName.contains("nux") || osName.contains("nix") || osName.contains("aix")) {
                detectedOS = OS.LINUX;
            } else {
                detectedOS = OS.UNKNOWN;
            }
        }
        return detectedOS;
    }

    /**
     * Opens a file with the system's default application.
     * Falls back to OS-specific commands if Desktop API is unsupported.
     */
    public static void openFile(File file) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.OPEN)) {
                    desktop.open(file);
                    return;
                }
            }
        } catch (Throwable ignored) {}

        // Fallback to OS-specific commands
        try {
            String path = file.getAbsolutePath();
            switch (getOS()) {
                case LINUX:
                    Runtime.getRuntime().exec(new String[]{"xdg-open", path});
                    break;
                case MACOS:
                    Runtime.getRuntime().exec(new String[]{"open", path});
                    break;
                case WINDOWS:
                    Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", "", path});
                    break;
                default:
                    Main.LOGGER.warn("Cannot open file: unsupported OS");
                    break;
            }
        } catch (Throwable t) {
            Main.LOGGER.error("Failed to open file: {}", file.getAbsolutePath(), t);
        }
    }

    /**
     * Opens a URL in the system's default browser.
     * Falls back to OS-specific commands if Desktop API is unsupported.
     */
    public static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(new URI(url));
                    return;
                }
            }
        } catch (Throwable ignored) {}

        // Fallback to OS-specific commands
        try {
            switch (getOS()) {
                case LINUX:
                    Runtime.getRuntime().exec(new String[]{"xdg-open", url});
                    break;
                case MACOS:
                    Runtime.getRuntime().exec(new String[]{"open", url});
                    break;
                case WINDOWS:
                    Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", "", url});
                    break;
                default:
                    Main.LOGGER.warn("Cannot open URL: unsupported OS");
                    break;
            }
        } catch (Throwable t) {
            Main.LOGGER.error("Failed to open URL: {}", url, t);
        }
    }
}
