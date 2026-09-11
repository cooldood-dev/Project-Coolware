package com.github.cooldood.events.impl;

import com.github.cooldood.events.Event;
import lombok.AllArgsConstructor;

public class ClickMouseEvent {
    @AllArgsConstructor
    public static class Left extends Event  {
    }

    @AllArgsConstructor
    public static class Right extends Event  {
    }
}
