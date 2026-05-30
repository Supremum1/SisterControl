package com.example.overlaydetector;

public class OverlayRect {
    private final int id;
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public OverlayRect(int id, int x, int y, int width, int height) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int getId() {
        return id;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
