package com.game;

public class Item {

    public double x, y;
    public ItemType type;
    public boolean collected = false;

    public double bob = 0;

    public Item(double x, double y, ItemType type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public void update() {
        bob += 0.08;
    }
}
