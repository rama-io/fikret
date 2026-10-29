package com.rama.fikret.game;

public class GameMap {
    public static final int TILE_SIZE = 64;

    private final MapCell[][] cells;
    private final int rows;
    private final int cols;

    public GameMap(int[][] rawData) {
        this.rows = rawData.length;
        this.cols = rawData[0].length;
        this.cells = new MapCell[rows][cols];
        for (int r = 0; r < rows; r++) {
            if (rawData[r].length != cols) {
                throw new IllegalArgumentException("Map row " + r + " has a different length than row 0 - all rows must be the same length.");
            }
            for (int c = 0; c < cols; c++) {
                cells[r][c] = new MapCell(rawData[r][c]);
            }
        }
    }

    public MapCell getCell(int row, int col) {
        if (!isInBounds(row, col)) {
            return null;
        }
        return cells[row][col];
    }

    public boolean isInBounds(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    public boolean isPassable(int row, int col) {
        if (!isInBounds(row, col)) {
            return false;
        }
        return !cells[row][col].item.blocksMovement;
    }

    public boolean isWalkable(int row, int col) {
        return isPassable(row, col);
    }

    public boolean canStep(int row, int col, int dx, int dy) {
        if (dx == 0 && dy == 0) {
            return false;
        }
        if (!isWalkable(row + dy, col + dx)) {
            return false;
        }
        if (dx != 0 && dy != 0) {
            boolean horizontalSideOpen = isWalkable(row, col + dx);
            boolean verticalSideOpen = isWalkable(row + dy, col);
            if (!horizontalSideOpen && !verticalSideOpen) {
                return false;
            }
        }
        return true;
    }

    public boolean isLiquidAt(float worldX, float worldY) {
        int col = (int) Math.floor(worldX / TILE_SIZE);
        int row = (int) Math.floor(worldY / TILE_SIZE);
        MapCell cell = getCell(row, col);
        return cell != null && cell.isLiquid();
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public int getWidthPx() {
        return cols * TILE_SIZE;
    }

    public int getHeightPx() {
        return rows * TILE_SIZE;
    }
}
