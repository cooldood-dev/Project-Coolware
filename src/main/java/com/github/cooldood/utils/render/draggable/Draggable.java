package com.github.cooldood.utils.render.draggable;

import java.util.concurrent.Callable;
import java.util.function.Predicate;

public class Draggable {
    public enum Anchor { LEFT, RIGHT }
    public Anchor anchor = Anchor.LEFT;
    public double width = 0, height = 0;

    public String id;
    public Callable<double[]> render;
    /// if module / mode is enabled
    public Predicate<?> canRender;
    /// conditions to render if enabled
    public Predicate<?> conditions;

    public double x = 0.1;
    public double y = 0.1;

    public Draggable(String id, Callable<double[]> render, Predicate<?> canRender, Predicate<?> conditions) {
        this.id = id;
        this.render = render;
        this.canRender = canRender;
        this.conditions = conditions;

        DraggableRenderer.draggables.add(this);
    }
}
