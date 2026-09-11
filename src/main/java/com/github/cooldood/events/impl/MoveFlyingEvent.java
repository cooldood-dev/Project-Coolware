package com.github.cooldood.events.impl;

import com.github.cooldood.events.Event;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MoveFlyingEvent extends Event {
    public float friction;
}
