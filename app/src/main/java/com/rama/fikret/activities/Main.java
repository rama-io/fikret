package com.rama.fikret.activities;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.rama.fikret.R;
import com.rama.fikret.economy.GameState;
import com.rama.fikret.economy.NumberFormatter;
import com.rama.fikret.economy.Worlds;
import com.rama.fikret.helpers.DeveloperMode;
import com.rama.fikret.helpers.FarmArt;
import com.rama.fikret.helpers.GardenIcons;
import com.rama.fikret.helpers.SystemBars;
import com.rama.fikret.helpers.ViewUpdates;
import com.rama.fikret.managers.FontManager;
import com.rama.fikret.managers.PrefsManager;

public class Main extends Activity {
    private static final long TICK_MS = 100;
    private static final int[] MULTIPLIERS = {1, GameState.BUY_NEXT, GameState.BUY_MAX};

    private static final String PREF_MULTIPLIER = "main:multiplier";

    private int multiplierIndex;

    private GameState state;
    private TextView moneyText;
    private Button multiplierButton;
    private BaseAdapter adapter;
    private ListView gardenList;
    private int world = -1;
    private int appliedStyle;
    private final Handler handler = new Handler();

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            state.sync();
            refresh();
            handler.postDelayed(this, TICK_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        state = GameState.get(this);
        appliedStyle = FarmArt.applyTheme(this, state.getCurrentWorld());
        setContentView(R.layout.activity_main);
        View root = findViewById(R.id.root);
        SystemBars.applyInsets(root);
        FontManager.apply(root, FontManager.getJersey25(this));

        moneyText = findViewById(R.id.money);
        multiplierButton = findViewById(R.id.multiplier);
        int saved = PrefsManager.getInstance(this).getInt(PREF_MULTIPLIER, 0);
        multiplierIndex = saved >= 0 && saved < MULTIPLIERS.length ? saved : 0;

        findViewById(R.id.menu).setOnClickListener(v -> showMenu());
        multiplierButton.setOnClickListener(v -> {
            multiplierIndex = (multiplierIndex + 1) % MULTIPLIERS.length;
            PrefsManager.getInstance(this).setInt(PREF_MULTIPLIER, multiplierIndex);
            refresh();
        });

        adapter = new GardenAdapter();
        gardenList = findViewById(R.id.garden_list);
        gardenList.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (FarmArt.styleOf(state.getCurrentWorld()) != appliedStyle) {
            recreate();
            return;
        }
        state.sync();
        handler.post(ticker);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
        state.save();
    }

    private void refresh() {
        world = state.getCurrentWorld();
        Worlds.WorldDef def = Worlds.ALL[world];
        moneyText.setText(NumberFormatter.money(world, state.getMoney(world)));
        multiplierButton.setText(multiplierLabel());
        refreshRows();
        updateAscend();
    }

    private String multiplierLabel() {
        int m = MULTIPLIERS[multiplierIndex];
        return m == GameState.BUY_MAX ? getString(R.string.buy_max) : m == GameState.BUY_NEXT ? getString(R.string.multiplier_next) : getString(R.string.multiplier_x, m);
    }

    private void showMenu() {
        final Dialog dialog = new Dialog(this, R.style.AppDialog);
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_menu, null);
        FontManager.apply(content, FontManager.getJersey25(this));
        dialog.setContentView(content);

        bindMenuItem(dialog, content, R.id.menu_map, new Intent(this, GameActivity.class));
        bindMenuItem(dialog, content, R.id.menu_managers, new Intent(this, ManagersActivity.class));
        bindMenuItem(dialog, content, R.id.menu_upgrades, new Intent(this, UpgradesActivity.class));
        bindMenuItem(dialog, content, R.id.menu_finance, new Intent(this, FinanceActivity.class));
        content.findViewById(R.id.menu_ascend).setOnClickListener(v -> {
            dialog.dismiss();
            showAscend();
        });
        bindMenuItem(dialog, content, R.id.menu_teleport, new Intent(this, TeleportActivity.class));
        bindMenuItem(dialog, content, R.id.menu_about, new Intent(this, About.class));

        View cancelButton = content.findViewById(R.id.cancel);
        if (cancelButton != null) {
            cancelButton.setOnClickListener(v -> dialog.dismiss());
        }

        boolean teleportAvailable = DeveloperMode.availableFarms() > 1;
        int visibility = teleportAvailable ? View.VISIBLE : View.GONE;
        content.findViewById(R.id.menu_teleport).setVisibility(visibility);
        content.findViewById(R.id.menu_teleport_gap).setVisibility(visibility);

        dialog.show();
    }

    private Dialog ascendDialog;

    private void showAscend() {
        final Dialog dialog = new Dialog(this, R.style.AppDialog);
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_ascend, null);
        FontManager.apply(content, FontManager.getJersey25(this));
        dialog.setContentView(content);

        content.findViewById(R.id.ascend_cancel).setOnClickListener(v -> dialog.dismiss());
        content.findViewById(R.id.ascend_confirm).setOnClickListener(v -> {
            if (state.ascend(state.getCurrentWorld())) {
                dialog.dismiss();
                refresh();
            }
        });
        ((TextView) content.findViewById(R.id.ascend_have_label)).setText(Worlds.ALL[state.getCurrentWorld()].name + " points");
        int coinSize = Math.round(((TextView) content.findViewById(R.id.ascend_have)).getTextSize());
        for (int id : new int[]{R.id.ascend_have, R.id.ascend_gain}) {
            TextView value = content.findViewById(id);
            value.setCompoundDrawables(FarmArt.coin(this, state.getCurrentWorld(), coinSize), null, null, null);
            value.setCompoundDrawablePadding(coinSize / 4);
        }
        dialog.setOnDismissListener(d -> ascendDialog = null);
        ascendDialog = dialog;
        dialog.show();
        updateAscend();
    }

    private void updateAscend() {
        if (ascendDialog == null) {
            return;
        }
        state.sync();
        double have = state.getAngels(state.getCurrentWorld());
        double gain = state.pendingAngels(state.getCurrentWorld());
        ViewUpdates.setText((TextView) ascendDialog.findViewById(R.id.ascend_have), NumberFormatter.number(have));
        ViewUpdates.setText((TextView) ascendDialog.findViewById(R.id.ascend_bonus), "+" + NumberFormatter.number(have * Worlds.ANGEL_BONUS * 100) + "%");
        ViewUpdates.setText((TextView) ascendDialog.findViewById(R.id.ascend_gain), "+" + NumberFormatter.number(gain));
        ViewUpdates.setBackgroundColor(ascendDialog.findViewById(R.id.ascend_confirm), gain > 0 ? ViewUpdates.themeColor(this, R.attr.accent) : ViewUpdates.color(this, R.color.disabled));
    }

    private void bindMenuItem(final Dialog dialog, View content, int id, final Intent target) {
        content.findViewById(id).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(target);
        });
    }

    private void refreshRows() {
        int first = gardenList.getFirstVisiblePosition();
        for (int i = 0; i < gardenList.getChildCount(); i++) {
            bind(gardenList.getChildAt(i), first + i);
        }
    }

    private void bind(View row, int g) {
        row.setTag(g);
        final int w = world < 0 ? state.getCurrentWorld() : world;
        int count = state.getCount(w, g);

        ViewUpdates.setImageResource((ImageView) row.findViewById(R.id.garden_icon), GardenIcons.forGarden(w, g));
        ViewUpdates.setText((TextView) row.findViewById(R.id.garden_count), getString(R.string.garden_count, count, GameState.nextMilestone(count)));

        ProgressBar bar = row.findViewById(R.id.progress_bar);
        int progress = (int) Math.round(state.progress(w, g) * 1000);
        if (bar.getProgress() != progress) {
            bar.setProgress(progress);
        }
        ViewUpdates.setText((TextView) row.findViewById(R.id.product_value), count > 0 ? NumberFormatter.money(w, state.revenuePerCycle(w, g)) : getString(R.string.not_owned));
        ViewUpdates.setText((TextView) row.findViewById(R.id.duration), state.isRunning(w, g) && state.cycleSeconds(w, g) >= 0.25 ? NumberFormatter.duration(state.timeLeft(w, g)) : NumberFormatter.duration(state.cycleSeconds(w, g)));

        int multiplier = MULTIPLIERS[multiplierIndex];
        GameState.Quote quote = state.quote(w, g, multiplier);
        boolean affordable = quote.amount > 0 && quote.cost <= state.getMoney(w);
        String label;
        if (multiplier == GameState.BUY_MAX) {
            label = quote.amount > 0 ? getString(R.string.buy_max_amount, quote.amount) : getString(R.string.buy_max_label);
        } else if (multiplier == GameState.BUY_NEXT) {
            label = getString(R.string.buy_next_amount, quote.amount);
        } else {
            label = getString(R.string.buy_x, multiplier);
        }
        ViewUpdates.setText((TextView) row.findViewById(R.id.buy_label), label);
        ViewUpdates.setText((TextView) row.findViewById(R.id.buy_cost), NumberFormatter.money(w, quote.cost));
        ViewUpdates.setBackgroundColor(row.findViewById(R.id.buy_button), affordable ? ViewUpdates.themeColor(this, R.attr.btnBg) : ViewUpdates.color(this, R.color.disabled));
    }

    private final class GardenAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return Worlds.GARDENS_PER_WORLD;
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                row = LayoutInflater.from(Main.this).inflate(R.layout.list_item_garden, parent, false);
                FontManager.apply(row, FontManager.getJersey25(Main.this));
                wireListeners(row);
            }
            bind(row, position);
            return row;
        }
    }

    private void wireListeners(final View row) {
        row.findViewById(R.id.buy_button).setOnClickListener(v -> {
            int g = (Integer) row.getTag();
            if (state.buy(state.getCurrentWorld(), g, MULTIPLIERS[multiplierIndex])) {
                refresh();
            }
        });
        row.findViewById(R.id.harvest_btn).setOnClickListener(v -> {
            int g = (Integer) row.getTag();
            if (state.harvest(state.getCurrentWorld(), g)) {
                refresh();
            }
        });
    }
}
