package com.rama.fikret.activities;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.rama.fikret.R;
import com.rama.fikret.economy.GameState;
import com.rama.fikret.economy.NumberFormatter;
import com.rama.fikret.helpers.FarmArt;
import com.rama.fikret.helpers.SystemBars;
import com.rama.fikret.helpers.ViewUpdates;
import com.rama.fikret.managers.FontManager;

public abstract class ListScreenActivity extends Activity {
    private int appliedStyle;
    private static final long TICK_MS = 100;

    protected GameState state;
    protected ListView list;
    protected BaseAdapter adapter;
    protected int world;
    private TextView moneyText;
    private final Handler handler = new Handler();

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            state.sync();
            ViewUpdates.setText(moneyText, NumberFormatter.money(world, state.getMoney(world)));
            onTick();
            refreshRows();
            handler.postDelayed(this, TICK_MS);
        }
    };

    protected abstract String screenTitle();

    protected abstract BaseAdapter createAdapter();

    protected abstract void bindRow(View row, int position);

    protected String subtitle() {
        return "";
    }

    /** Text shown when the list has no rows. */
    protected String emptyText() {
        return "";
    }

    /** Called every tick before the visible rows are rebound; screens whose row set changes on its own hook in here. */
    protected void onTick() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        state = GameState.get(this);
        appliedStyle = FarmArt.applyTheme(this, state.getCurrentWorld());
        setContentView(R.layout.activity_list);
        View root = findViewById(R.id.root);
        SystemBars.applyInsets(root);
        FontManager.apply(root, FontManager.getJersey25(this));

        world = state.getCurrentWorld();
        moneyText = findViewById(R.id.money);
        list = findViewById(R.id.list);

        TextView title = findViewById(R.id.title);
        title.setText(screenTitle());

        findViewById(R.id.back).setOnClickListener(v -> finish());

        adapter = createAdapter();
        list.setAdapter(adapter);
        TextView empty = findViewById(R.id.empty);
        empty.setText(emptyText());
        list.setEmptyView(empty);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (FarmArt.styleOf(state.getCurrentWorld()) != appliedStyle) {
            recreate();
            return;
        }
        handler.post(ticker);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
        state.save();
    }

    private void refreshRows() {
        int first = list.getFirstVisiblePosition();
        for (int i = 0; i < list.getChildCount(); i++) {
            bindRow(list.getChildAt(i), first + i);
        }
    }

    protected void applyFont(View row) {
        FontManager.apply(row, FontManager.getJersey25(this));
    }
}
