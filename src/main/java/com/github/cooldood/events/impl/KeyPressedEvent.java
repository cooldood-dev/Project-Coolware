package com.github.cooldood.events.impl;

import com.github.cooldood.events.Event;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class KeyPressedEvent extends Event {
    public int keyCode;
    public boolean pressed;
}
