package com.rama.fikret.game;

public enum TileType {
    NONE(0, false),
    GRASS(1, false),
    WATER(2, true),
    SAND(3, false),
    SOIL(4, false),
    DEEP_GRASS(5, false),
    DEEP_WATER(6, true),
    SNOW(7, false),
    ICE(8, false),
    VOLCANIC_SOIL(9, false),
    LAVA(10, true),
    ACID_SOIL(11, false),
    ACID_LAKE(12, true),
    BUBBLEGUM(13, false),
    BUBBLEGUM_LAKE(14, true),
    SPACE_PURPLE_SOIL(15, false),
    SPACE_LAKE(16, true),
    NIGHTMARE_BLOOD_LAKE(17, true);

    public static final int BLOCK_SIZE = 3;
    public static final int TYPES_PER_COLUMN = 10;
    public static final int TYPE_COLUMNS = 2;

    public static final int SHEET_COLUMNS = TYPE_COLUMNS * BLOCK_SIZE;
    public static final int SHEET_ROWS = TYPES_PER_COLUMN * BLOCK_SIZE;

    public final int id;
    public final boolean liquid;
    public final int blockCol;
    public final int blockRow;

    TileType(int id, boolean liquid) {
        this.id = id;
        this.liquid = liquid;
        if (id <= 0) {
            this.blockCol = -1;
            this.blockRow = -1;
        } else {
            int index = id - 1;
            this.blockCol = (index / TYPES_PER_COLUMN) * BLOCK_SIZE;
            this.blockRow = (index % TYPES_PER_COLUMN) * BLOCK_SIZE;
        }
    }

    public static TileType fromId(int id) {
        for (TileType t : values()) {
            if (t.id == id) {
                return t;
            }
        }
        return NONE;
    }
}
