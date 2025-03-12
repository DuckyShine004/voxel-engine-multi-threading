package com.duckyshine.app.model;

public enum BlockType {
    GRASS("grass", 0, false),
    DIRT("dirt", 1, false),
    STONE("stone", 2, false),
    OAK_LOG("oak_log", 3, false),
    OAK_LEAVES("oak_leaves", 4, true),
    WATER("water", 5, true);

    private int index;

    private String type;

    private boolean isTransparent;

    private BlockType(String type, int index, boolean isTransparent) {
        this.type = type;

        this.index = index;

        this.isTransparent = isTransparent;
    }

    public int getIndex() {
        return this.index;
    }

    public String getType() {
        return this.type;
    }

    public boolean isTransparent() {
        return this.isTransparent;
    }
}
