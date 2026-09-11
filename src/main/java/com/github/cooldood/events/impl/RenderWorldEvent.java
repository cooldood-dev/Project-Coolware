package com.github.cooldood.events.impl;

import com.github.cooldood.events.Event;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RenderWorldEvent extends Event {
    public float partialTicks;
}
