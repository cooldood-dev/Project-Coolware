package com.github.cooldood.events.impl;

import com.github.cooldood.events.Event;
import lombok.AllArgsConstructor;

import java.io.File;
import java.util.List;

/// isn't posted to if optifine is loaded.
@AllArgsConstructor
public class FileDroppedEvent extends Event {
    public List<File> droppedFiles;
}
