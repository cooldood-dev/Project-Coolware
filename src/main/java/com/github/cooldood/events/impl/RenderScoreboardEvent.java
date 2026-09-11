package com.github.cooldood.events.impl;

import com.github.cooldood.events.Event;
import lombok.AllArgsConstructor;
import net.minecraft.scoreboard.ScoreObjective;

@AllArgsConstructor
public class RenderScoreboardEvent extends Event {
    public ScoreObjective scoreObjective;
}
