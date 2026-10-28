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
import com.rama.fikret.economy.NumberFormatter;
import com.rama.fikret.economy.Worlds;
import com.rama.fikret.helpers.DeveloperMode;
import com.rama.fikret.helpers.ViewUpdates;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * What each active farm earns per second and where it comes from. A farm is active when it is
 * available and at least one of its gardens has a supervisor.
 */
public class FinanceActivity extends ListScreenActivity {
    private static final int TYPE_FARM = 0;
    private static final int TYPE_SUMMARY = 1;

    private static final String[] SOURCE_NAMES = {"Base", "Upgrades", "Milestones", "Points"};

    private final List<FarmFinance> farms = new ArrayList<>();

    private static final class Bar {
        View root;
        TextView label;
        TextView value;
        TextView percent;
        ProgressBar fill;
    }

    private static final class FarmHolder {
        TextView name;
        TextView income;
        TextView rates;
        TextView points;
        TextView ascend;
        TextView idle;
        TextView best;
        TextView lifetime;
        final Bar[] sources = new Bar[FarmFinance.SOURCES];
        final Bar[] gardens = new Bar[Worlds.GARDENS_PER_WORLD];
    }

    @Override
    protected String screenTitle() {
        return getString(R.string.menu_finance);
    }

    @Override
    protected String emptyText() {
        return getString(R.string.empty_finance);
    }

    /** Returns true when the set of active farms changed. */
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

    private boolean hasSummary() {
        return farms.size() >= 2;
    }

    private FarmFinance farmAt(int position) {
        int index = hasSummary() ? position - 1 : position;
        return index >= 0 && index < farms.size() ? farms.get(index) : null;
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
                return farms.size() + (hasSummary() ? 1 : 0);
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
            public int getViewTypeCount() {
                return 2;
            }

            @Override
            public int getItemViewType(int position) {
                return hasSummary() && position == 0 ? TYPE_SUMMARY : TYPE_FARM;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View row = convertView;
                if (row == null) {
                    row = getItemViewType(position) == TYPE_SUMMARY ? inflateSummary(parent) : inflateFarm(parent);
                }
                bindRow(row, position);
                return row;
            }
        };
    }

    private View inflateSummary(ViewGroup parent) {
        View row = LayoutInflater.from(this).inflate(R.layout.list_item_finance_summary, parent, false);
        applyFont(row);
        return row;
    }

    private View inflateFarm(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View row = inflater.inflate(R.layout.list_item_finance_farm, parent, false);
        FarmHolder h = new FarmHolder();
        h.name = row.findViewById(R.id.farm_name);
        h.income = row.findViewById(R.id.farm_income);
        h.rates = row.findViewById(R.id.farm_rates);
        h.points = row.findViewById(R.id.farm_points);
        h.ascend = row.findViewById(R.id.farm_ascend);
        h.idle = row.findViewById(R.id.farm_idle);
        h.best = row.findViewById(R.id.farm_best);
        h.lifetime = row.findViewById(R.id.farm_lifetime);
        LinearLayout sources = row.findViewById(R.id.farm_sources);
        for (int i = 0; i < h.sources.length; i++) {
            h.sources[i] = inflateBar(inflater, sources);
        }
        LinearLayout gardens = row.findViewById(R.id.farm_gardens);
        for (int i = 0; i < h.gardens.length; i++) {
            h.gardens[i] = inflateBar(inflater, gardens);
        }
        applyFont(row);
        row.setTag(h);
        return row;
    }

    private Bar inflateBar(LayoutInflater inflater, LinearLayout parent) {
        Bar bar = new Bar();
        bar.root = inflater.inflate(R.layout.list_item_finance_bar, parent, false);
        bar.label = bar.root.findViewById(R.id.bar_label);
        bar.value = bar.root.findViewById(R.id.bar_value);
        bar.percent = bar.root.findViewById(R.id.bar_percent);
        bar.fill = bar.root.findViewById(R.id.bar_fill);
        parent.addView(bar.root);
        return bar;
    }

    @Override
    protected void bindRow(View row, int position) {
        boolean summary = hasSummary() && position == 0;
        if (summary) {
            View income = row.findViewById(R.id.summary_income);
            if (income != null) {
                double lake = 0;
                for (FarmFinance f : farms) {
                    lake += f.lakeIncome;
                }
                ViewUpdates.setText((TextView) income,
                        NumberFormatter.money(Worlds.ANGEL_WORLD, lake) + "/s");
            }
            return;
        }
        FarmFinance f = farmAt(position);
        if (f == null || !(row.getTag() instanceof FarmHolder)) {
            return;
        }
        bindFarm((FarmHolder) row.getTag(), f);
    }

    private void bindFarm(FarmHolder h, FarmFinance f) {
        int w = f.world;
        ViewUpdates.setText(h.name, Worlds.ALL[w].name);
        ViewUpdates.setText(h.income, NumberFormatter.money(w, f.income) + "/s");
        ViewUpdates.setText(h.rates, NumberFormatter.number(f.income * 60) + "/min  ·  "
                + NumberFormatter.number(f.income * 3600) + "/h");

        for (int i = 0; i < h.sources.length; i++) {
            double value = f.bySource[i];
            // Base is always shown; the others only once they add something.
            boolean show = i == FarmFinance.SOURCE_BASE || value > 0;
            h.sources[i].root.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show) {
                bindBar(h.sources[i], SOURCE_NAMES[i], w, value, f.income);
            }
        }

        int[] order = f.gardensByIncome();
        for (int i = 0; i < h.gardens.length; i++) {
            boolean show = i < order.length;
            h.gardens[i].root.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show) {
                int g = order[i];
                bindBar(h.gardens[i], Worlds.ALL[w].gardens[g].name, w, f.byGarden[g], f.income);
            }
        }

        ViewUpdates.setText(h.points, "Points: " + NumberFormatter.number(f.points)
                + "  (+" + NumberFormatter.number(f.pointsBonusPercent) + "% profit)");
        ViewUpdates.setText(h.ascend, f.pendingPoints > 0
                ? "On ascend: +" + NumberFormatter.number(f.pendingPoints) + " points, profit x"
                + NumberFormatter.number(f.ascendFactor)
                : "On ascend: nothing to gain yet");

        h.idle.setVisibility(f.idleIncome > 0 ? View.VISIBLE : View.GONE);
        ViewUpdates.setText(h.idle, "Idle gardens: +" + NumberFormatter.money(w, f.idleIncome)
                + "/s with a supervisor");

        if (f.bestBuy == null) {
            h.best.setVisibility(View.GONE);
        } else {
            h.best.setVisibility(View.VISIBLE);
            ViewUpdates.setText(h.best, "Best buy: " + f.bestBuy + ", pays back in "
                    + longDuration(f.bestBuyPayback) + ". "
                    + (f.bestBuyWait <= 0 ? "You can afford it now."
                    : "Affordable in " + longDuration(f.bestBuyWait) + "."));
        }

        ViewUpdates.setText(h.lifetime, "Earned in total: " + NumberFormatter.money(w, f.lifetime));
    }

    private void bindBar(Bar bar, String label, int w, double value, double total) {
        double share = total > 0 ? value / total : 0;
        ViewUpdates.setText(bar.label, label);
        ViewUpdates.setText(bar.value, NumberFormatter.money(w, value) + "/s");
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

    private static String longDuration(double seconds) {
        if (Double.isNaN(seconds) || Double.isInfinite(seconds)) {
            return "never";
        }
        if (seconds >= 2 * 86400) {
            return NumberFormatter.number(seconds / 86400) + " days";
        }
        return NumberFormatter.duration(seconds);
    }
}
