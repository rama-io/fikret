package com.rama.fikret.activities;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.rama.fikret.R;
import com.rama.fikret.economy.FarmFinance;
import com.rama.fikret.economy.GameState;
import com.rama.fikret.economy.NumberFormatter;
import com.rama.fikret.economy.Worlds;
import com.rama.fikret.helpers.DeveloperMode;
import com.rama.fikret.helpers.ViewUpdates;
import com.rama.fikret.widgets.WdCollapsibleSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FinanceActivity extends ListScreenActivity {
    private static final int[] SOURCE_NAMES = {R.string.source_base, R.string.source_upgrades, R.string.source_milestones, R.string.source_points};

    private final List<FarmFinance> farms = new ArrayList<>();

    private static final class Bar {
        View root;
        View gap;
        TextView label;
        TextView value;
        TextView percent;
        ProgressBar fill;
    }

    private static final class FarmHolder {
        WdCollapsibleSection section;
        int world = -1;
        final Bar[] sources = new Bar[FarmFinance.SOURCES];
        final Bar[] gardens = new Bar[Worlds.GARDENS_PER_WORLD];
        TextView points;
        TextView pointsProfit;
        TextView pointsGainOnAscend;
        View ascendButton;
        View bestGroup;
        TextView bestName;
        int bestKind = FarmFinance.BEST_NONE;
        int bestGarden;
        TextView afford;
        TextView paysBack;
        TextView gainSecond;
        TextView gainMinute;
        TextView gainHour;
    }

    @Override
    protected String screenTitle() {
        return getString(R.string.menu_finance);
    }

    @Override
    protected String emptyText() {
        return getString(R.string.empty_finance);
    }

    private boolean refreshFarms() {
        int before = farms.size();
        int[] worldsBefore = new int[before];
        for (int i = 0; i < before; i++) {
            worldsBefore[i] = farms.get(i).world;
        }
        farms.clear();
        for (int w = 0; w < DeveloperMode.availableFarms(); w++) {
            FarmFinance finance = FarmFinance.of(state, w);
            if (finance.income > 0) {
                farms.add(finance);
            }
        }
        if (farms.size() != before) {
            return true;
        }
        for (int i = 0; i < before; i++) {
            if (farms.get(i).world != worldsBefore[i]) {
                return true;
            }
        }
        return false;
    }

    private FarmFinance farmAt(int position) {
        return position >= 0 && position < farms.size() ? farms.get(position) : null;
    }

    @Override
    protected void onTick() {
        if (refreshFarms()) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected BaseAdapter createAdapter() {
        refreshFarms();
        return new BaseAdapter() {
            @Override
            public int getCount() {
                return farms.size();
            }

            @Override
            public Object getItem(int position) {
                return farmAt(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View row = convertView;
                if (row == null) {
                    row = inflateFarm(parent);
                }
                bindRow(row, position);
                return row;
            }
        };
    }

    private View inflateFarm(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View row = inflater.inflate(R.layout.list_item_finance_farm, parent, false);
        FarmHolder h = new FarmHolder();
        h.section = (WdCollapsibleSection) row;
        h.points = row.findViewById(R.id.farm_points);
        h.pointsProfit = row.findViewById(R.id.farm_points_profit);
        h.pointsGainOnAscend = row.findViewById(R.id.farm_points_gain_on_ascend);
        h.ascendButton = row.findViewById(R.id.ascend_confirm);
        h.ascendButton.setOnClickListener(v -> {
            if (h.world >= 0 && state.ascend(h.world)) {
                adapter.notifyDataSetChanged();
            }
        });
        h.bestGroup = row.findViewById(R.id.farm_best_group);
        h.bestName = row.findViewById(R.id.best_buy_name);
        h.bestName.setOnClickListener(v -> {
            if (h.world >= 0 && buyBest(h)) {
                adapter.notifyDataSetChanged();
            }
        });
        h.afford = row.findViewById(R.id.afford_timer);
        h.paysBack = row.findViewById(R.id.pays_back);
        h.gainSecond = row.findViewById(R.id.gain_second);
        h.gainMinute = row.findViewById(R.id.gain_minute);
        h.gainHour = row.findViewById(R.id.gain_hour);
        fillBars(inflater, (LinearLayout) row.findViewById(R.id.farm_sources), h.sources);
        fillBars(inflater, (LinearLayout) row.findViewById(R.id.farm_gardens), h.gardens);
        applyFont(row);
        row.setTag(h);
        return row;
    }

    private void fillBars(LayoutInflater inflater, LinearLayout parent, Bar[] bars) {
        for (int i = 0; i < bars.length; i++) {
            Bar bar = new Bar();
            View root = inflater.inflate(R.layout.list_item_finance_bar, parent, false);
            bar.gap = root.findViewById(R.id.bar_gap);
            bar.label = root.findViewById(R.id.bar_label);
            bar.value = root.findViewById(R.id.bar_value);
            bar.percent = root.findViewById(R.id.bar_percent);
            bar.fill = root.findViewById(R.id.bar_fill);
            bar.gap.setVisibility(i == 0 ? View.GONE : View.VISIBLE);
            bar.root = root;
            parent.addView(root);
            bars[i] = bar;
        }
    }

    @Override
    protected void bindRow(View row, int position) {
        FarmFinance f = farmAt(position);
        if (f == null || !(row.getTag() instanceof FarmHolder)) {
            return;
        }
        bindFarm((FarmHolder) row.getTag(), f);
    }

    private void bindFarm(FarmHolder h, FarmFinance f) {
        int w = f.world;
        if (h.world != w) {
            h.world = w;
            h.section.setHeader(Worlds.ALL[w].name);
            h.section.setKey("finance:section:" + w, true);
        }

        for (int i = 0; i < h.sources.length; i++) {
            double value = f.bySource[i];
            boolean show = i == FarmFinance.SOURCE_BASE || value > 0;
            h.sources[i].root.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show) {
                bindBar(h.sources[i], getString(SOURCE_NAMES[i]), w, value, f.income);
            }
        }

        int[] order = f.gardensByIncome();
        for (int i = 0; i < h.gardens.length; i++) {
            int g = order[i];
            bindBar(h.gardens[i], Worlds.ALL[w].gardens[g].name, w, f.byGarden[g], f.income);
        }

        ViewUpdates.setText(h.points, NumberFormatter.number(f.points));
        ViewUpdates.setText(h.pointsProfit, getString(R.string.finance_profit_bonus, NumberFormatter.number(f.pointsBonusPercent)));
        ViewUpdates.setText(h.pointsGainOnAscend, getString(R.string.finance_points_gain, NumberFormatter.number(f.pointsGain)));
        ViewUpdates.setBackgroundColor(h.ascendButton, f.pointsGain > 0 ? ViewUpdates.themeColor(this, R.attr.accent) : ViewUpdates.color(this, R.color.disabled));

        boolean hasBest = f.bestBuyKind != FarmFinance.BEST_NONE;
        h.bestGroup.setVisibility(hasBest ? View.VISIBLE : View.GONE);
        if (hasBest) {
            h.bestKind = f.bestBuyKind;
            h.bestGarden = f.bestBuyGarden;
            ViewUpdates.setText(h.bestName, bestBuyName(f, w));
            ViewUpdates.setBackgroundColor(h.bestName, f.bestBuyWait <= 0
                    ? ViewUpdates.themeColor(this, R.attr.accent) : ViewUpdates.color(this, R.color.disabled));
            ViewUpdates.setText(h.afford, f.bestBuyWait <= 0 ? getString(R.string.finance_afford_now) : longDuration(f.bestBuyWait));
            ViewUpdates.setText(h.paysBack, longDuration(f.bestBuyPayback));
        }

        ViewUpdates.setText(h.gainSecond, NumberFormatter.money(w, f.income));
        ViewUpdates.setText(h.gainMinute, NumberFormatter.money(w, f.income * 60));
        ViewUpdates.setText(h.gainHour, NumberFormatter.money(w, f.income * 3600));
    }

    private boolean buyBest(FarmHolder h) {
        int w = h.world;
        int g = h.bestGarden;
        switch (h.bestKind) {
            case FarmFinance.BEST_SUPERVISOR:
                return state.hireManager(w, g);
            case FarmFinance.BEST_SPEED:
                return state.buyUpgrade(w, g, GameState.UPGRADE_SPEED, state.upgradeLevel(w, g, GameState.UPGRADE_SPEED));
            case FarmFinance.BEST_VALUE:
                return state.buyUpgrade(w, g, GameState.UPGRADE_VALUE, state.upgradeLevel(w, g, GameState.UPGRADE_VALUE));
            case FarmFinance.BEST_MILESTONE:
                return state.buy(w, g, GameState.BUY_NEXT);
            default:
                return false;
        }
    }

    private String bestBuyName(FarmFinance f, int w) {
        String garden = Worlds.ALL[w].gardens[f.bestBuyGarden].name;
        switch (f.bestBuyKind) {
            case FarmFinance.BEST_SPEED:
                return getString(R.string.upgrade_speed_name, garden);
            case FarmFinance.BEST_VALUE:
                return getString(R.string.upgrade_value_name, garden);
            case FarmFinance.BEST_MILESTONE:
                return getString(R.string.milestone_name, garden, f.bestBuyTarget);
            default:
                return getString(R.string.supervisor_name, garden);
        }
    }

    private void bindBar(Bar bar, String label, int w, double value, double total) {
        double share = total > 0 ? value / total : 0;
        ViewUpdates.setText(bar.label, label);
        ViewUpdates.setText(bar.value, NumberFormatter.money(w, value));
        ViewUpdates.setText(bar.percent, percent(share));
        int progress = (int) Math.round(Math.min(1, share) * 1000);
        if (bar.fill.getProgress() != progress) {
            bar.fill.setProgress(progress);
        }
    }

    private static String percent(double share) {
        double pct = share * 100;
        return String.format(Locale.US, pct < 10 ? "%.1f%%" : "%.0f%%", pct);
    }

    private String longDuration(double seconds) {
        if (Double.isNaN(seconds) || Double.isInfinite(seconds)) {
            return getString(R.string.duration_never);
        }
        if (seconds >= 2 * 86400) {
            return getString(R.string.duration_days, NumberFormatter.number(seconds / 86400));
        }
        return NumberFormatter.duration(seconds);
    }
}
