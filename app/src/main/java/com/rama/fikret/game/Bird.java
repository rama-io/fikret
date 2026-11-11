package com.rama.fikret.game;

import android.graphics.Canvas;
import android.graphics.Rect;

import java.util.Random;

public class Bird {
    private static final long STEP_DURATION_MS = 200;
    private static final long WALK_FRAME_DURATION_MS = 80;

    private static final int FRAME_REST = 0;
    private static final int FRAME_IDLE = 1;
    private static final int FRAME_WALK_A = 2;
    private static final int FRAME_WALK_B = 3;

    private static final long PATROL_WAIT_MIN_MS = 3000;
    private static final long PATROL_WAIT_MAX_MS = 9000;

    private final SpriteSheet spriteSheet;
    private final Random random = new Random();

    private boolean patrolling = false;
    private int patrolRowA, patrolColA, patrolRowB, patrolColB;
    private boolean patrolToB = true;
    private long patrolWaitMs = 0;

    private int fromRow, fromCol;
    private int row, col;
    private float stepProgress = 1f;
    private boolean hasPendingTarget = false;
    private int pendingTargetRow, pendingTargetCol;

    private Direction facing = Direction.RIGHT;
    private boolean moving = false;
    private boolean following = false;
    private boolean swimming = false;
    private boolean walkToggle = false;
    private long walkAnimTimer = 0;

    public Bird(SpriteSheet spriteSheet, int startRow, int startCol) {
        this.spriteSheet = spriteSheet;
        this.row = this.fromRow = startRow;
        this.col = this.fromCol = startCol;
    }

    public boolean isFollowing() {
        return following;
    }

    public void setSwimming(boolean swimming) {
        this.swimming = swimming;
    }

    public void teleportTo(int newRow, int newCol) {
        row = fromRow = newRow;
        col = fromCol = newCol;
        stepProgress = 1f;
        moving = false;
        hasPendingTarget = false;
    }

    public void setPatrol(int rowB, int colB) {
        patrolRowA = row;
        patrolColA = col;
        patrolRowB = rowB;
        patrolColB = colB;
        patrolToB = true;
        patrolling = rowB != row || colB != col;
        patrolWaitMs = randomWait();
    }

    private long randomWait() {
        return PATROL_WAIT_MIN_MS + (long) (random.nextFloat() * (PATROL_WAIT_MAX_MS - PATROL_WAIT_MIN_MS));
    }

    private void updatePatrol(long deltaMs) {
        if (!patrolling || following || moving || hasPendingTarget) {
            return;
        }
        int targetRow = patrolToB ? patrolRowB : patrolRowA;
        int targetCol = patrolToB ? patrolColB : patrolColA;
        if (row == targetRow && col == targetCol) {
            patrolToB = !patrolToB;
            patrolWaitMs = randomWait();
            return;
        }
        if (patrolWaitMs > 0) {
            patrolWaitMs -= deltaMs;
            return;
        }
        pendingTargetRow = row + Integer.signum(targetRow - row);
        pendingTargetCol = col + Integer.signum(targetCol - col);
        hasPendingTarget = true;
    }

    public void startFollowing() {
        following = true;
    }

    public void moveTowards(int targetRow, int targetCol) {
        if (!following) {
            return;
        }
        pendingTargetRow = targetRow;
        pendingTargetCol = targetCol;
        hasPendingTarget = true;
    }

    public void update(long deltaMs) {
        updatePatrol(deltaMs);
        if (!moving && hasPendingTarget) {
            hasPendingTarget = false;
            int dRow = pendingTargetRow - row;
            int dCol = pendingTargetCol - col;

            if (dRow != 0 || dCol != 0) {
                if (dCol != 0) {
                    facing = dCol < 0 ? Direction.LEFT : Direction.RIGHT;
                }
                fromRow = row;
                fromCol = col;
                row = pendingTargetRow;
                col = pendingTargetCol;
                stepProgress = 0f;
                moving = true;
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

    public void draw(Canvas canvas, Rect dst) {
        int frame = swimming ? FRAME_REST : (moving ? (walkToggle ? FRAME_WALK_A : FRAME_WALK_B) : FRAME_IDLE);
        Rect src = spriteSheet.frameRect(facing.column, frame);
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
}
