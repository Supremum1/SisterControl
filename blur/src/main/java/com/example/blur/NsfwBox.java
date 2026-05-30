package com.example.overlaydetector;

public class NsfwBox {
    private final String label;
    private final float score;
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public NsfwBox(String label, float score, int x, int y, int width, int height) {
        this.label = label;
        this.score = score;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public String getLabel() {
        return label;
    }

    public float getScore() {
        return score;
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
