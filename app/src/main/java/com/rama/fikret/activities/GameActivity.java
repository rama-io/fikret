package com.rama.fikret.activities;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.rama.fikret.economy.GameState;
import com.rama.fikret.game.GameView;
import com.rama.fikret.game.ItemSheet;
import com.rama.fikret.game.ItemType;
import com.rama.fikret.game.SpriteSheet;
import com.rama.fikret.game.Maps;
import com.rama.fikret.helpers.SystemBars;
import com.rama.fikret.managers.PrefsManager;

public class GameActivity extends Activity {
    public static final String EXTRA_STAGE = "stage";
    public static final String EXTRA_TELEPORT_ARRIVAL = "teleport_arrival";

    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        SystemBars.applyFullscreen(this);

        int stageId;
        int[] startPosition = null;
        if (getIntent().hasExtra(EXTRA_STAGE)) {
            stageId = getIntent().getIntExtra(EXTRA_STAGE, Maps.BEACH);
        } else {
            PrefsManager prefs = PrefsManager.getInstance(this);
            stageId = prefs.getCurrentStage(Maps.BEACH);
            startPosition = prefs.getArrivalPosition();
        }
        gameView = new GameView(this, stageId, startPosition);

        if (getIntent().getBooleanExtra(EXTRA_TELEPORT_ARRIVAL, false)) {
            gameView.playArrivalAnimation();
        }

        FrameLayout container = new FrameLayout(this);
        container.addView(gameView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        ImageButton farmButton = createFarmButton();
        container.addView(farmButton, farmButtonParams());
        setContentView(container);
        SystemBars.applyInsetsAsMargins(farmButton);
        gameView.requestFocus();
    }

    private ImageButton createFarmButton() {
        SpriteSheet items = ItemSheet.get(getResources());
        Rect src = items.frameRect(ItemType.FARM_BUTTON.spriteCol, ItemType.FARM_BUTTON.spriteRow);
        Bitmap icon = Bitmap.createBitmap(items.getBitmap(), src.left, src.top, src.width(), src.height());
        BitmapDrawable drawable = new BitmapDrawable(getResources(), icon);
        drawable.setFilterBitmap(false);

        float density = getResources().getDisplayMetrics().density;
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setColor(0x99ffebb3);
        background.setStroke(4, 0xFFe6a89f);

        ImageButton button = new ImageButton(this);
        button.setImageDrawable(drawable);
        button.setBackgroundDrawable(background);
        button.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int pad = Math.round(8 * density);
        button.setPadding(pad, pad, pad, pad);
        button.setContentDescription("Back to farm");
        button.setOnClickListener(v -> finish());
        return button;
    }

    private FrameLayout.LayoutParams farmButtonParams() {
        float density = getResources().getDisplayMetrics().density;
        int size = Math.round(56 * density);
        int margin = Math.round(16 * density);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size, size, Gravity.BOTTOM | Gravity.END);
        lp.setMargins(margin, margin, margin, margin);
        return lp;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (gameView != null && gameView.handleKeyEvent(event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        int[] position = gameView.getGoosePosition();
        if (position != null) {
            PrefsManager.getInstance(this).setArrivalPosition(position[0], position[1]);
        }
        GameState.get(this).save();
    }
}
