package com.rama.fikret.game;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.rama.fikret.R;

public class Goose {
    private static final int ATLAS_ROWS = 5;
    private static final int TELEPORT_COLUMN_OFFSET = 2;
    private static final long STEP_DURATION_MS = 200;
    private static final long WALK_FRAME_DURATION_MS = 80;
    private static final int FRAME_SWIMMING_A = 0;
    private static final int FRAME_SWIMMING_B = 1;
    private static final int FRAME_IDLE = 2;
    private static final int FRAME_WALK_A = 3;
    private static final int FRAME_WALK_B = 4;
    private final SpriteSheet spriteSheet;
    private final boolean hasTeleportPose;
    private int fromRow, fromCol;
    private int row, col;
    private float stepProgress = 1f;
    private Direction facing = Direction.RIGHT;
    private boolean moving = false;
    private boolean swimming = false;
    private boolean teleporting = false;
    private boolean walkToggle = false;
    private long walkAnimTimer = 0;

    public Goose(Resources res, int startRow, int startCol) {
        this.spriteSheet = SpriteSheet.withSquareCells(res, R.drawable.goose, ATLAS_ROWS);
        this.hasTeleportPose = spriteSheet.getColumns() >= TELEPORT_COLUMN_OFFSET + 2;
        this.row = this.fromRow = startRow;
        this.col = this.fromCol = startCol;
    }

    public void update(long deltaMs, int dx, int dy, GameMap map) {
        if (!moving) {
            if (dx != 0 || dy != 0) {
                if (dx != 0) {
                    facing = dx < 0 ? Direction.LEFT : Direction.RIGHT;
                }

                int stepX = dx;
                int stepY = dy;
                if (!map.canStep(row, col, stepX, stepY) && dx != 0 && dy != 0) {
                    if (map.canStep(row, col, dx, 0)) {
                        stepY = 0;
                    } else if (map.canStep(row, col, 0, dy)) {
                        stepX = 0;
                    }
                }
                if (map.canStep(row, col, stepX, stepY)) {
                    fromRow = row;
                    fromCol = col;
                    row = row + stepY;
                    col = col + stepX;
                    stepProgress = 0f;
                    moving = true;
                }
            }
        }

        if (moving) {
            stepProgress += deltaMs / (float) STEP_DURATION_MS;
            if (stepProgress >= 1f) {
                stepProgress = 1f;
                moving = false;
            }

            walkAnimTimer += deltaMs;
            if (walkAnimTimer >= WALK_FRAME_DURATION_MS) {
                walkAnimTimer = 0;
                walkToggle = !walkToggle;
            }
        } else {
            walkAnimTimer = 0;
        }
    }

    public void teleportTo(int newRow, int newCol) {
        row = fromRow = newRow;
        col = fromCol = newCol;
        stepProgress = 1f;
        moving = false;
    }

    public void setSwimming(boolean swimming) {
        this.swimming = swimming;
    }

    public void setTeleporting(boolean teleporting) {
        this.teleporting = teleporting;
    }

    public boolean isTeleporting() {
        return teleporting;
    }

    public boolean isSwimming() {
        return swimming;
    }

    public void draw(Canvas canvas, Rect dst) {
        int frame = swimming
                ? (walkToggle ? FRAME_SWIMMING_A : FRAME_SWIMMING_B)
                : (moving ? (walkToggle ? FRAME_WALK_A : FRAME_WALK_B) : FRAME_IDLE);
        int column = facing.column + (teleporting && hasTeleportPose ? TELEPORT_COLUMN_OFFSET : 0);
        Rect src = spriteSheet.frameRect(column, frame);
        canvas.drawBitmap(spriteSheet.getBitmap(), src, dst, null);
    }

    public float getX() {
        return lerp(fromCol, col) * GameMap.TILE_SIZE;
    }

    public float getY() {
        return lerp(fromRow, row) * GameMap.TILE_SIZE;
    }

    private float lerp(int from, int to) {
        return from + (to - from) * stepProgress;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public boolean isMoving() {
        return moving;
    }
}
