package com.github.cooldood.events.impl;

import com.github.cooldood.events.Event;
import com.github.cooldood.modules.Module;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ModuleStateChangeEvent extends Event {
    public final Module module;
    public final boolean state;
}
