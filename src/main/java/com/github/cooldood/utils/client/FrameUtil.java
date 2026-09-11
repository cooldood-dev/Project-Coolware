package com.github.cooldood.utils.client;

import com.github.cooldood.events.Bus;
import com.github.cooldood.events.impl.FileDroppedEvent;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.util.List;

public class FrameUtil {
    // stupid   G89HoergguiogghoHUG,
    // this probably has 99 problems and they are NOT all bitches. <- kid cudi reference, not jay z
    // but i want to drag and drop files.
    public static void createCookiesFrame() {
        FileChooserUtil.openFilePicker("Select Cookie File", file -> {
            if (file != null) {
                Bus.post(new FileDroppedEvent(java.util.Collections.singletonList(file)));
            }
        });
    }
}
