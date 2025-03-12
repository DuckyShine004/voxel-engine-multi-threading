package com.duckyshine.app.model;

public enum BlockType {
    GRASS(0, false),
    DIRT(1, false),
    STONE(2, false),
    OAK_LOG(3, false),
    OAK_LEAVES(4, true),
    WATER(5, true);

    private int index;

    private boolean isTransparent;

    private BlockType(int index, boolean isTransparent) {
        this.index = index;

        this.isTransparent = isTransparent;
    }

    public int getIndex() {
        return this.index;
    }

    public String getName() {
        return this.name().toLowerCase();
    }

    public boolean isTransparent() {
        return this.isTransparent;
    }
}
