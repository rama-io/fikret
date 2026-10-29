package com.rama.fikret.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.rama.fikret.R;
import com.rama.fikret.economy.GameState;
import com.rama.fikret.economy.Worlds;
import com.rama.fikret.helpers.DeveloperMode;
import com.rama.fikret.managers.PrefsManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private GameThread thread;
    private GameMap map;
    private Goose goose;
    private Stage stage;
    private int stageId;
    private final List<Bird> managerBirds = new ArrayList<>();
    private final List<Bird> guideBirds = new ArrayList<>();
    private final List<int[]> guideChase = new ArrayList<>();
    private int worldIndex = -1;
    private int[] startPosition;
    private int holeGooseRow, holeGooseCol;
    private SpriteSheet tileSheet;
    private SpriteSheet itemSheet;
    private final Paint backgroundPaint = new Paint();

    private static final long TELEPORT_PHASE_MS = 200;
    private static final long ARRIVAL_POSE_MS = 500;
    private static final long CAMERA_GLIDE_MIN_MS = 250;
    private static final long CAMERA_GLIDE_MAX_MS = 600;
    private static final float CAMERA_GLIDE_SPEED = 4f;

    private enum TeleportKind {HOME, ARRIVE}

    private boolean pendingArrival;
    private TeleportKind teleportKind;
    private boolean teleportArrived;
    private long teleportTimerMs;

    private final Rect reusableSrc = new Rect();
    private final Rect reusableDst = new Rect();
    private final SwipeJoystick joystick;
    private final Blizzard blizzard;
    private int cameraX, cameraY;
    private boolean glidePending;
    private boolean cameraGliding;
    private int glideFromX, glideFromY;
    private long glideElapsedMs, glideDurationMs;
    private static final float MIN_ZOOM = 1f;
    private static final float MAX_ZOOM = 4f;
    private float zoomRaw = 2f;
    private float zoom = 2f;
    private PinchZoomDetector pinchZoomDetector;
    private float panOffsetX, panOffsetY;
    private float panAnchorX, panAnchorY;
    private boolean wasMoving;
    private static final long PAN_RESET_DURATION_MS = 200;
    private boolean panResetting;
    private long panResetElapsedMs;
    private float panResetStartX, panResetStartY;
    private volatile boolean keyLeft, keyRight, keyUp, keyDown;
    private volatile int activeNumpadKeyCode = 0;
    private volatile int numpadDx, numpadDy;
    private volatile int stickDx, stickDy;
    private boolean pinching;

    public GameView(Context context) {
        this(context, Maps.BEACH);
    }

    public GameView(Context context, int stageId) {
        this(context, stageId, null);
    }

    public GameView(Context context, int stageId, int[] startPosition) {
        super(context);
        getHolder().addCallback(this);
        setFocusable(true);
        setFocusableInTouchMode(true);

        backgroundPaint.setColor(Color.BLACK);
        float density = getResources().getDisplayMetrics().density;
        joystick = new SwipeJoystick(density);
        blizzard = new Blizzard();

        if (Build.VERSION.SDK_INT >= 8) {
            pinchZoomDetector = new PinchZoomDetector(context, new PinchZoomDetector.Listener() {
                @Override
                public void onZoom(float scaleFactor, float focusX, float focusY) {
                    setZoom(zoomRaw * scaleFactor);
                }
            });
        }

        this.startPosition = startPosition;
        try {
            stage = Maps.get(stageId);
        } catch (IllegalArgumentException unknownStage) {
            stageId = Maps.BEACH;
            stage = Maps.get(stageId);
            this.startPosition = null;
        }
        this.stageId = stageId;
        map = new GameMap(stage.tiles);
        worldIndex = Worlds.indexOfStage(stageId);
        GameState.get(context).onStageEntered(stageId);
        loadTileSheets();
        loadItemSheet();
    }

    private void loadTileSheets() {
        if (tileSheet == null) {
            tileSheet = new SpriteSheet(getResources(), R.drawable.tiles, TileType.SHEET_COLUMNS, TileType.SHEET_ROWS);
        }
    }

    private void loadItemSheet() {
        if (itemSheet == null) {
            itemSheet = ItemSheet.get(getResources());
        }
    }

    private void setZoom(float newZoom) {
        zoomRaw = Math.max(MIN_ZOOM, Math.min(newZoom, MAX_ZOOM));
        zoom = Math.round(zoomRaw);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (goose == null) {
            int row = stage.spawnRow;
            int col = stage.spawnCol;
            if (startPosition != null && map.isWalkable(startPosition[0], startPosition[1])) {
                row = startPosition[0];
                col = startPosition[1];
            }
            goose = new Goose(getResources(), row, col);
            placeBirds();
            if (pendingArrival) {
                pendingArrival = false;
                startArrival();
            }
        }
        holeGooseRow = goose.getRow();
        holeGooseCol = goose.getCol();
        thread = new GameThread(holder, this);
        thread.setRunning(true);
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        clearInput();
        if (thread == null) {
            return;
        }
        thread.setRunning(false);
        boolean retry = true;
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (pinchZoomDetector != null) {
            pinchZoomDetector.onTouchEvent(event);
        }

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                pinching = false;
                joystick.begin(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                pinching = true;
                panResetting = false;
                joystick.end();
                panAnchorX = averagePointerX(event);
                panAnchorY = averagePointerY(event);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() > 1) {
                    pinching = true;
                    joystick.end();
                    float avgX = averagePointerX(event);
                    float avgY = averagePointerY(event);
                    panOffsetX -= (avgX - panAnchorX) / zoom;
                    panOffsetY -= (avgY - panAnchorY) / zoom;
                    panAnchorX = avgX;
                    panAnchorY = avgY;
                } else if (!pinching) {
                    joystick.move(event.getX(), event.getY());
                }
                return true;
            case MotionEvent.ACTION_POINTER_UP:
                return true;
            case MotionEvent.ACTION_UP:
                pinching = false;
                joystick.end();
                return true;
            case MotionEvent.ACTION_CANCEL:
                pinching = false;
                joystick.end();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private static float averagePointerX(MotionEvent event) {
        float sum = 0f;
        int count = event.getPointerCount();
        for (int i = 0; i < count; i++) {
            sum += event.getX(i);
        }
        return sum / count;
    }

    private static float averagePointerY(MotionEvent event) {
        float sum = 0f;
        int count = event.getPointerCount();
        for (int i = 0; i < count; i++) {
            sum += event.getY(i);
        }
        return sum / count;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_A:
                keyLeft = true;
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_D:
                keyRight = true;
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_W:
                keyUp = true;
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_S:
                keyDown = true;
                return true;
            default:
                if (keyCode >= KeyEvent.KEYCODE_NUMPAD_1 && keyCode <= KeyEvent.KEYCODE_NUMPAD_9) {
                    int numpadPosition = keyCode - KeyEvent.KEYCODE_NUMPAD_0;
                    activeNumpadKeyCode = keyCode;
                    numpadDx = TilePosition.col(numpadPosition) - 1;
                    numpadDy = TilePosition.row(numpadPosition) - 1;
                    return true;
                }
                return super.onKeyDown(keyCode, event);
        }
    }

    public boolean handleKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            return onKeyDown(event.getKeyCode(), event);
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            return onKeyUp(event.getKeyCode(), event);
        }
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_A:
                keyLeft = false;
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_D:
                keyRight = false;
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_W:
                keyUp = false;
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_S:
                keyDown = false;
                return true;
            default:
                if (keyCode == activeNumpadKeyCode) {
                    activeNumpadKeyCode = 0;
                    numpadDx = 0;
                    numpadDy = 0;
                    return true;
                }
                return super.onKeyUp(keyCode, event);
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (Build.VERSION.SDK_INT >= 12 && GamepadAxes.isJoystickMove(event)) {
            stickDx = GamepadAxes.digitalX(event);
            stickDy = GamepadAxes.digitalY(event);
            return true;
        }
        return false;
    }

    private void clearInput() {
        keyLeft = false;
        keyRight = false;
        keyUp = false;
        keyDown = false;
        activeNumpadKeyCode = 0;
        numpadDx = 0;
        numpadDy = 0;
        stickDx = 0;
        stickDy = 0;
        pinching = false;
        panOffsetX = 0f;
        panOffsetY = 0f;
        panResetting = false;
        wasMoving = false;
        joystick.end();
    }

    private int resolvedDx() {
        return sign(rawDx());
    }

    private int resolvedDy() {
        return sign(rawDy());
    }

    private int rawDx() {
        return (keyRight ? 1 : 0) - (keyLeft ? 1 : 0) + numpadDx + stickDx + joystick.getDx();
    }

    private int rawDy() {
        return (keyDown ? 1 : 0) - (keyUp ? 1 : 0) + numpadDy + stickDy + joystick.getDy();
    }

    private static int sign(int v) {
        return v > 0 ? 1 : (v < 0 ? -1 : 0);
    }

    public void update(long deltaMs) {
        if (goose == null) {
            return;
        }
        int dx = isTeleporting() ? 0 : resolvedDx();
        int dy = isTeleporting() ? 0 : resolvedDy();

        boolean nowMoving = dx != 0 || dy != 0;
        if (nowMoving && !wasMoving) {
            startPanReset();
        }
        wasMoving = nowMoving;

        goose.update(deltaMs, dx, dy, map);
        goose.setSwimming(isOverLiquid(goose.getX(), goose.getY()));
        checkHoleTransition();
        updateTeleport(deltaMs);
        updateGuideBirds(deltaMs);
        updatePanReset(deltaMs);
        updateCamera(deltaMs);
        if (stage.hasBlizzard) {
            blizzard.update(deltaMs);
        }
    }

    private void startPanReset() {
        if (panOffsetX == 0f && panOffsetY == 0f) {
            return;
        }
        panResetting = true;
        panResetElapsedMs = 0;
        panResetStartX = panOffsetX;
        panResetStartY = panOffsetY;
    }

    private void updatePanReset(long deltaMs) {
        if (!panResetting) {
            return;
        }
        panResetElapsedMs += deltaMs;
        float t = Math.min(1f, panResetElapsedMs / (float) PAN_RESET_DURATION_MS);
        float eased = 1f - (1f - t) * (1f - t);
        panOffsetX = panResetStartX * (1f - eased);
        panOffsetY = panResetStartY * (1f - eased);
        if (t >= 1f) {
            panResetting = false;
        }
    }

    private void checkHoleTransition() {
        if (goose.getRow() == holeGooseRow && goose.getCol() == holeGooseCol) {
            return;
        }
        holeGooseRow = goose.getRow();
        holeGooseCol = goose.getCol();

        MapCell cell = map.getCell(holeGooseRow, holeGooseCol);
        if (cell == null) {
            return;
        }
        if (cell.item == ItemType.HOLE_DOWN) {
            if (isHoleDownUnlocked() && DeveloperMode.isStageAvailable(stageId + 1)) {
                changeStage(stageId + 1);
            }
        } else if (cell.item == ItemType.HOLE_UP) {
            changeStage(stageId - 1);
        }
    }

    private void changeStage(int newStageId) {
        changeStage(newStageId, false);
    }

    private void changeStage(int newStageId, boolean resetWorld) {
        Stage newStage;
        try {
            newStage = Maps.get(newStageId);
        } catch (IllegalArgumentException noSuchStage) {
            return;
        }

        PrefsManager prefs = PrefsManager.getInstance(getContext());
        int leftStageId = stageId;
        int leftRow = goose.getRow();
        int leftCol = goose.getCol();

        stageId = newStageId;
        stage = newStage;
        map = new GameMap(stage.tiles);
        worldIndex = Worlds.indexOfStage(stageId);
        GameState.get(getContext()).onStageEntered(stageId);
        cancelCameraGlide();

        int spawnRow = stage.spawnRow;
        int spawnCol = stage.spawnCol;
        if (!resetWorld) {
            int[] savedPos = prefs.getStagePosition(newStageId);
            if (savedPos != null && map.isWalkable(savedPos[0], savedPos[1])) {
                spawnRow = savedPos[0];
                spawnCol = savedPos[1];
            }
        }

        if (resetWorld) {
            prefs.saveTeleportHome(newStageId, spawnRow, spawnCol);
        } else {
            prefs.saveStageEntry(leftStageId, leftRow, leftCol, newStageId, spawnRow, spawnCol);
        }

        goose = new Goose(getResources(), spawnRow, spawnCol);
        placeBirds();

        holeGooseRow = goose.getRow();
        holeGooseCol = goose.getCol();
        resetPan();
    }

    private void resetPan() {
        panOffsetX = 0f;
        panOffsetY = 0f;
        panResetting = false;
    }

    private void moveGooseTo(int row, int col) {
        goose.teleportTo(row, col);
        for (int i = 0; i < guideBirds.size(); i++) {
            guideBirds.get(i).teleportTo(row, col);
            guideChase.get(i)[0] = row;
            guideChase.get(i)[1] = col;
        }
        holeGooseRow = row;
        holeGooseCol = col;
        resetPan();
    }

    public void playArrivalAnimation() {
        pendingArrival = true;
    }

    private void startArrival() {
        teleportKind = TeleportKind.ARRIVE;
        teleportArrived = true;
        teleportTimerMs = 0;
        goose.setTeleporting(true);
    }

    private boolean isTeleporting() {
        return teleportKind != null;
    }

    private void updateTeleport(long deltaMs) {
        if (!isTeleporting()) {
            return;
        }
        teleportTimerMs += deltaMs;
        if (!teleportArrived) {
            if (teleportTimerMs < TELEPORT_PHASE_MS) {
                return;
            }
            performTeleport();
            teleportArrived = true;
            teleportTimerMs = 0;
            goose.setTeleporting(true);
            return;
        }
        long poseMs = teleportKind == TeleportKind.ARRIVE ? ARRIVAL_POSE_MS : TELEPORT_PHASE_MS;
        if (teleportTimerMs >= poseMs) {
            goose.setTeleporting(false);
            teleportKind = null;
        }
    }

    private void performTeleport() {
        if (stageId == Maps.BEACH) {
            beginCameraGlide();
            moveGooseTo(stage.spawnRow, stage.spawnCol);
        } else {
            changeStage(Maps.BEACH, true);
        }
    }

    private boolean isOverLiquid(float spriteX, float spriteY) {
        return map.isLiquidAt(spriteX + GameMap.TILE_SIZE / 2f, spriteY + GameMap.TILE_SIZE / 2f);
    }

    private boolean isHoleDownUnlocked() {
        if (worldIndex < 0) {
            return true;
        }
        return GameState.get(getContext()).hasTravelManager(worldIndex);
    }

    private void placeBirds() {
        managerBirds.clear();
        guideBirds.clear();
        guideChase.clear();
        if (goose == null) {
            return;
        }
        GameState state = GameState.get(getContext());

        if (worldIndex >= 0) {
            List<int[]> spots = candidateSpots(true);
            if (spots.isEmpty()) {
                spots = candidateSpots(false);
            }
            Random random = new Random();
            for (int g = 0; g < Worlds.GARDENS_PER_WORLD && !spots.isEmpty(); g++) {
                if (!state.hasManager(worldIndex, g)) {
                    continue;
                }
                int[] spot = spots.remove(random.nextInt(spots.size()));
                Bird bird = new Bird(BirdSheets.forWorld(getContext(), worldIndex), spot[0], spot[1]);
                bird.setSwimming(map.getCell(spot[0], spot[1]).isLiquid());
                managerBirds.add(bird);
            }
        }

        for (int w = 0; w < DeveloperMode.availableFarms(); w++) {
            if (!state.hasTravelManager(w)) {
                continue;
            }
            Bird bird = new Bird(BirdSheets.forWorld(getContext(), w), goose.getRow(), goose.getCol());
            bird.startFollowing();
            guideBirds.add(bird);
            guideChase.add(new int[]{goose.getRow(), goose.getCol()});
        }
    }

    private List<int[]> candidateSpots(boolean liquidOnly) {
        List<int[]> result = new ArrayList<>();
        for (int r = 0; r < map.getRows(); r++) {
            for (int c = 0; c < map.getCols(); c++) {
                MapCell cell = map.getCell(r, c);
                if (cell.hasItem() || (liquidOnly && !cell.isLiquid())) {
                    continue;
                }
                if (r == goose.getRow() && c == goose.getCol()) {
                    continue;
                }
                result.add(new int[]{r, c});
            }
        }
        return result;
    }

    private int[] findItem(ItemType item) {
        for (int r = 0; r < map.getRows(); r++) {
            for (int c = 0; c < map.getCols(); c++) {
                if (map.getCell(r, c).item == item) {
                    return new int[]{r, c};
                }
            }
        }
        return null;
    }

    private void updateGuideBirds(long deltaMs) {
        int leaderRow = goose.getRow();
        int leaderCol = goose.getCol();
        for (int i = 0; i < guideBirds.size(); i++) {
            Bird bird = guideBirds.get(i);
            int[] chase = guideChase.get(i);
            if (bird.isFollowing()) {
                if (leaderRow != chase[0] || leaderCol != chase[1]) {
                    bird.moveTowards(chase[0], chase[1]);
                }
                chase[0] = leaderRow;
                chase[1] = leaderCol;
            }
            bird.update(deltaMs);
            bird.setSwimming(isOverLiquid(bird.getX(), bird.getY()));
            leaderRow = bird.getRow();
            leaderCol = bird.getCol();
        }
    }

    public int[] getGoosePosition() {
        Goose g = goose;
        return g == null ? null : new int[]{g.getRow(), g.getCol()};
    }

    private void updateCamera(long deltaMs) {
        int viewW = getWidth();
        int viewH = getHeight();
        if (viewW == 0 || viewH == 0) {
            return;
        }
        float visibleW = viewW / zoom;
        float visibleH = viewH / zoom;
        cameraX = Math.round(goose.getX() + panOffsetX) + GameMap.TILE_SIZE / 2 - (int) (visibleW / 2f);
        cameraY = Math.round(goose.getY() + panOffsetY) + GameMap.TILE_SIZE / 2 - (int) (visibleH / 2f);

        int maxCamX = Math.max(0, (int) (map.getWidthPx() - visibleW));
        int maxCamY = Math.max(0, (int) (map.getHeightPx() - visibleH));
        cameraX = clamp(cameraX, 0, maxCamX);
        cameraY = clamp(cameraY, 0, maxCamY);

        applyCameraGlide(deltaMs);
    }

    private void applyCameraGlide(long deltaMs) {
        if (glidePending) {
            glidePending = false;
            float dx = cameraX - glideFromX;
            float dy = cameraY - glideFromY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            if (distance < 1f) {
                return;
            }
            glideDurationMs = (long) Math.max(CAMERA_GLIDE_MIN_MS,
                    Math.min(CAMERA_GLIDE_MAX_MS, distance / CAMERA_GLIDE_SPEED));
            glideElapsedMs = 0;
            cameraGliding = true;
            cameraX = glideFromX;
            cameraY = glideFromY;
            return;
        }
        if (!cameraGliding) {
            return;
        }
        glideElapsedMs += deltaMs;
        float t = Math.min(1f, glideElapsedMs / (float) glideDurationMs);
        float eased = t * t * (3f - 2f * t); // smoothstep
        cameraX = glideFromX + Math.round((cameraX - glideFromX) * eased);
        cameraY = glideFromY + Math.round((cameraY - glideFromY) * eased);
        if (t >= 1f) {
            cameraGliding = false;
        }
    }

    private void beginCameraGlide() {
        glidePending = true;
        glideFromX = cameraX;
        glideFromY = cameraY;
    }

    private void cancelCameraGlide() {
        glidePending = false;
        cameraGliding = false;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    public void render(Canvas canvas) {
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), backgroundPaint);
        if (map == null) {
            return;
        }

        canvas.save();
        canvas.scale(zoom, zoom);

        int tileSize = GameMap.TILE_SIZE;
        float visibleW = canvas.getWidth() / zoom;
        float visibleH = canvas.getHeight() / zoom;
        int firstCol = Math.max(0, cameraX / tileSize);
        int firstRow = Math.max(0, cameraY / tileSize);
        int lastCol = Math.min(map.getCols() - 1, (int) ((cameraX + visibleW) / tileSize) + 1);
        int lastRow = Math.min(map.getRows() - 1, (int) ((cameraY + visibleH) / tileSize) + 1);

        for (int r = firstRow; r <= lastRow; r++) {
            for (int c = firstCol; c <= lastCol; c++) {
                drawTile(canvas, r, c, tileSize);
            }
        }

        drawBirds(canvas, managerBirds, tileSize);
        drawBirds(canvas, guideBirds, tileSize);

        if (goose != null) {
            int screenX = Math.round(goose.getX()) - cameraX;
            int screenY = Math.round(goose.getY()) - cameraY;
            reusableDst.set(screenX, screenY, screenX + tileSize, screenY + tileSize);
            goose.draw(canvas, reusableDst);
        }

        canvas.restore();

        if (stage.hasBlizzard) {
            blizzard.draw(canvas, canvas.getWidth(), canvas.getHeight());
        }
        joystick.draw(canvas);
    }

    private void drawBirds(Canvas canvas, List<Bird> birds, int tileSize) {
        for (int i = 0; i < birds.size(); i++) {
            Bird bird = birds.get(i);
            int bx = Math.round(bird.getX()) - cameraX;
            int by = Math.round(bird.getY()) - cameraY;
            reusableDst.set(bx, by, bx + tileSize, by + tileSize);
            bird.draw(canvas, reusableDst);
        }
    }

    private void drawTile(Canvas canvas, int row, int col, int tileSize) {
        MapCell cell = map.getCell(row, col);
        if (cell == null) {
            return;
        }

        if (cell.hasBackground()) {
            drawTileLayer(canvas, cell.backgroundTile, cell.backgroundPosition, row, col, tileSize);
        }
        drawTileLayer(canvas, cell.tile, cell.position, row, col, tileSize);

        boolean hiddenHole = cell.item == ItemType.HOLE_DOWN && !isHoleDownUnlocked();
        if (cell.hasItem() && !hiddenHole && cell.item.hasSprite() && itemSheet != null) {
            int screenX = col * tileSize - cameraX;
            int screenY = row * tileSize - cameraY;
            reusableSrc.set(itemSheet.frameRect(cell.item.spriteCol, cell.item.spriteRow));
            reusableDst.set(screenX, screenY, screenX + tileSize, screenY + tileSize);
            canvas.drawBitmap(itemSheet.getBitmap(), reusableSrc, reusableDst, null);
        }
    }

    private void drawTileLayer(Canvas canvas, TileType type, int position, int row, int col, int tileSize) {
        if (type == TileType.NONE) {
            return;
        }
        if (tileSheet == null) {
            return;
        }

        reusableSrc.set(tileSheet.frameRect(
                type.blockCol + TilePosition.col(position),
                type.blockRow + TilePosition.row(position)));
        int screenX = col * tileSize - cameraX;
        int screenY = row * tileSize - cameraY;
        reusableDst.set(screenX, screenY, screenX + tileSize, screenY + tileSize);
        canvas.drawBitmap(tileSheet.getBitmap(), reusableSrc, reusableDst, null);
    }
}
