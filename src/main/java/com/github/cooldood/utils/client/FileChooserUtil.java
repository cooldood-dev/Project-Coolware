package com.github.cooldood.utils.client;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

public class FileChooserUtil {

    /**
     * Opens a native file dialog asynchronously without creating visible empty frames
     * and without blocking the Minecraft rendering thread.
     *
     * @param title      Dialog title
     * @param onSelected Callback executed when a file is selected (null if cancelled)
     */
    public static void openFilePicker(String title, Consumer<File> onSelected) {
        ForkJoinPool.commonPool().execute(() -> {
            try {
                // Use FileDialog with a dummy null/headless parent dialog to prevent blank window artifact
                FileDialog dialog = new FileDialog((Frame) null, title, FileDialog.LOAD);
                dialog.setMode(FileDialog.LOAD);
                dialog.setAlwaysOnTop(true);
                dialog.toFront();
                dialog.requestFocus();
                dialog.setVisible(true);

                String directory = dialog.getDirectory();
                String file = dialog.getFile();
                dialog.dispose();

                if (directory != null && file != null) {
                    File selectedFile = new File(directory, file);
                    if (selectedFile.exists() && selectedFile.isFile()) {
                        if (onSelected != null) onSelected.accept(selectedFile);
                        return;
                    }
                }
            } catch (Throwable t) {
                // Fallback to Swing JFileChooser with native Look & Feel if AWT FileDialog encounters issues
                try {
                    try {
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    } catch (Exception ignored) {}

                    JFileChooser chooser = new JFileChooser();
                    chooser.setDialogTitle(title);
                    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    chooser.setMultiSelectionEnabled(false);
                    chooser.addChoosableFileFilter(new FileNameExtensionFilter("Cookie & Text Files (*.txt, *.json)", "txt", "json", "cookies"));
                    chooser.setAcceptAllFileFilterUsed(true);

                    int result = chooser.showOpenDialog(null);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        File selectedFile = chooser.getSelectedFile();
                        if (selectedFile != null && selectedFile.exists() && selectedFile.isFile()) {
                            if (onSelected != null) onSelected.accept(selectedFile);
                            return;
                        }
                    }
                } catch (Throwable fallbackEx) {
                    fallbackEx.printStackTrace();
                }
            }

            if (onSelected != null) onSelected.accept(null);
        });
    }
}
