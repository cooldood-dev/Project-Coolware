package com.github.cooldood.events.impl;

import com.github.cooldood.events.Event;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class WindowResizeEvent extends Event {
    public final int width, height;
}
