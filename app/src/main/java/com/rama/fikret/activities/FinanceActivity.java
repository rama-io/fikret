package com.rama.fikret.activities;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.rama.fikret.R;
import com.rama.fikret.economy.FarmFinance;
import com.rama.fikret.economy.GameState;
import com.rama.fikret.economy.NumberFormatter;
import com.rama.fikret.economy.Worlds;
import com.rama.fikret.helpers.GardenIcons;
import com.rama.fikret.helpers.DeveloperMode;
import com.rama.fikret.helpers.ViewUpdates;
import com.rama.fikret.widgets.WdCollapsibleSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FinanceActivity extends ListScreenActivity {
    private static final int[] SOURCE_NAMES = {R.string.source_base, R.string.source_upgrades, R.string.source_milestones, R.string.source_points};
    private int BADGES_PER_ROW;

    private final List<FarmFinance> farms = new ArrayList<>();

    private static final class Bar {
        View root;
        View gap;
        TextView label;
        TextView value;
        TextView percent;
        ProgressBar fill;
    }

    private static final class NextCell {
        Tile tile;
        TextView timer;
        View buy;
    }

    private static final class FarmHolder {
        WdCollapsibleSection section;
        int world = -1;
        final Bar[] sources = new Bar[FarmFinance.SOURCES];
        final Bar[] gardens = new Bar[Worlds.GARDENS_PER_WORLD];
        LinearLayout medals;
        View achievements;
        String medalsKey = "";
        final List<NextCell> nextCells = new ArrayList<>();
        TextView points;
        TextView pointsMultiplier;
        TextView pointsProfit;
        TextView pointsGainOnAscend;
        View ascendButton;
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
            if (state.isFarmUnlocked(w)) {
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
        h.medals = row.findViewById(R.id.farm_medals);
        h.achievements = row.findViewById(R.id.farm_achievements);
        h.pointsMultiplier = row.findViewById(R.id.farm_points_multiplier);
        h.pointsProfit = row.findViewById(R.id.farm_points_profit);
        h.pointsGainOnAscend = row.findViewById(R.id.farm_points_gain_on_ascend);
        h.ascendButton = row.findViewById(R.id.ascend_confirm);
        h.ascendButton.setOnClickListener(v -> {
            if (h.world >= 0 && state.pendingAngels(h.world) > 0) {
                confirmAscend(h.world);
            }
        });
        h.gainSecond = row.findViewById(R.id.gain_second);
        h.gainMinute = row.findViewById(R.id.gain_minute);
        h.gainHour = row.findViewById(R.id.gain_hour);
        applyFont(row);
        fillBars(inflater, (LinearLayout) row.findViewById(R.id.farm_sources), h.sources, false);
        fillBars(inflater, (LinearLayout) row.findViewById(R.id.farm_gardens), h.gardens, true);
        row.setTag(h);
        return row;
    }

    private void confirmAscend(int w) {
        Dialog dialog = new Dialog(this, R.style.AppDialog);
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_confirm, null);
        ((TextView) content.findViewById(R.id.confirm_title)).setText(getString(R.string.ascend_dialog_title, Worlds.ALL[w].name));
        ((TextView) content.findViewById(R.id.confirm_message)).setText(getString(R.string.ascend_dialog_message,
                getString(R.string.ascend_warning), NumberFormatter.number(state.pendingAngels(w))));
        TextView yes = content.findViewById(R.id.confirm_yes);
        yes.setText(R.string.ascend_confirm);
        yes.setOnClickListener(v -> {
            dialog.dismiss();
            if (state.ascend(w)) {
                adapter.notifyDataSetChanged();
            }
        });
        content.findViewById(R.id.confirm_cancel).setOnClickListener(v -> dialog.dismiss());
        applyFont(content);
        dialog.setContentView(content);
        dialog.show();
    }

    private void fillBars(LayoutInflater inflater, LinearLayout parent, Bar[] bars, Boolean isGarden) {
        for (int i = 0; i < bars.length; i++) {
            Bar bar = new Bar();
            View root = inflater.inflate(R.layout.list_item_finance_bar, parent, false);
            bar.gap = root.findViewById(R.id.bar_gap);
            bar.label = root.findViewById(R.id.bar_label);
            bar.value = root.findViewById(R.id.bar_value);
            bar.percent = root.findViewById(R.id.bar_percent);
            bar.fill = root.findViewById(R.id.bar_fill);

            if (isGarden) {
                applyUnicodeFont(bar.label);
                applyFont(bar.percent);
                applyFont(bar.value);
            } else {
                applyFont(root);
            }

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

        for (int g = 0; g < h.gardens.length; g++) {
            bindBar(h.gardens[g], Worlds.ALL[w].gardens[g].name, w, f.byGarden[g], f.income);
        }

        ViewUpdates.setText(h.points, NumberFormatter.number(f.points));
        bindMedals(h, w, f.income);
        ViewUpdates.setText(h.pointsMultiplier, getString(R.string.finance_points_efficiency, NumberFormatter.number(state.pointsEfficiency(w) * 100)));
        ViewUpdates.setText(h.pointsProfit, getString(R.string.finance_profit_bonus, NumberFormatter.number(f.pointsBonusPercent)));
        ViewUpdates.setText(h.pointsGainOnAscend, getString(R.string.finance_points_gain, NumberFormatter.number(f.pointsGain)));
        ViewUpdates.setBackgroundColor(h.ascendButton, f.pointsGain > 0 ? ViewUpdates.themeColor(this, R.attr.accent) : ViewUpdates.color(this, R.color.disabled));

        ViewUpdates.setText(h.gainSecond, NumberFormatter.money(w, f.income));
        ViewUpdates.setText(h.gainMinute, NumberFormatter.money(w, f.income * 60));
        ViewUpdates.setText(h.gainHour, NumberFormatter.money(w, f.income * 3600));
    }

    private static final int NEXT_NONE = 0;
    private static final int NEXT_MILESTONE = 1;
    private static final int NEXT_MEDAL = 2;

    private static final class Tile {
        final int icon;
        final String caption;
        final String value;
        final String nextCaption;
        final String nextValue;
        final int nextKind;
        final int garden;
        final double medalThreshold;

        Tile(int icon, String caption, String value, String nextCaption, String nextValue,
             int nextKind, int garden, double medalThreshold) {
            this.icon = icon;
            this.caption = caption;
            this.value = value;
            this.nextCaption = nextCaption;
            this.nextValue = nextValue;
            this.nextKind = nextKind;
            this.garden = garden;
            this.medalThreshold = medalThreshold;
        }

        boolean hasNext() {
            return nextCaption != null;
        }
    }

    private String tileValue(double multiplier) {
        return getString(R.string.medal_multiplier, NumberFormatter.multiplier(multiplier));
    }

    private List<Tile> achievementTiles(int w) {
        List<Tile> tiles = new ArrayList<>();
        double medals = state.medalMultiplier(w);
        for (int g = 0; g < Worlds.GARDENS_PER_WORLD; g++) {
            int icon = GardenIcons.forGarden(w, g);
            int earned = state.milestones(w, g);
            {
                double speed = Math.pow(2, earned);
                tiles.add(new Tile(icon, getString(R.string.tile_speed), tileValue(speed),
                        Worlds.ALL[w].gardens[g].name,
                        String.valueOf(state.nextUnearnedMilestone(w, g)),
                        NEXT_MILESTONE, g, 0));
            }
        }
        {
            int nextMedal = state.nextMedalIndex(w);
            double threshold = nextMedal >= 0 ? state.medalThreshold(w, nextMedal) : 0;
            tiles.add(new Tile(R.drawable.medal, getString(R.string.tile_profit), tileValue(medals),
                    getString(R.string.tile_profit),
                    nextMedal >= 0 ? NumberFormatter.money(w, threshold) : getString(R.string.tile_max),
                    nextMedal >= 0 ? NEXT_MEDAL : NEXT_NONE, -1, threshold));
        }
        return tiles;
    }

    private void bindMedals(FarmHolder h, int w, double income) {
        List<Tile> tiles = achievementTiles(w);
        StringBuilder key = new StringBuilder();
        for (Tile tile : tiles) {
            key.append(tile.icon).append(':').append(tile.caption).append(':').append(tile.value)
                    .append(':').append(tile.nextCaption).append(':').append(tile.nextValue).append(';');
        }
        String signature = key.toString();
        h.achievements.setVisibility(tiles.isEmpty() ? View.GONE : View.VISIBLE);
        if (!signature.equals(h.medalsKey)) {
            h.medalsKey = signature;
            rebuildMedals(h, w, tiles);
        }
        updateNextCells(h, w, income);
    }

    private void rebuildMedals(FarmHolder h, int w, List<Tile> tiles) {
        h.medals.removeAllViews();
        h.nextCells.clear();
        LayoutInflater inflater = LayoutInflater.from(this);
        LinearLayout rowLayout = null;
        for (int i = 0; i < tiles.size(); i++) {
            Tile tile = tiles.get(i);
            if (i % BADGES_PER_ROW == 0) {
                rowLayout = newMedalRow(h.medals);
            }

            View view = inflater.inflate(R.layout.list_item_medal, rowLayout, false);
            ((ImageView) view.findViewById(R.id.medal_icon)).setImageResource(tile.icon);
            ((TextView) view.findViewById(R.id.medal_caption)).setText(tile.caption);
            ((TextView) view.findViewById(R.id.medal_value)).setText(tile.value);
            applyFont(view);
            addMedalCell(rowLayout, view);

            View next;
            if (tile.hasNext()) {
                next = inflater.inflate(R.layout.list_item_medal_next, rowLayout, false);
                ((TextView) next.findViewById(R.id.medal_caption)).setText(tile.nextCaption);
                ((TextView) next.findViewById(R.id.medal_value)).setText(tile.nextValue);
                NextCell cell = new NextCell();
                cell.tile = tile;
                cell.timer = next.findViewById(R.id.time_counter);
                cell.buy = next.findViewById(R.id.buy_next);
                ((TextView) cell.buy).setText(R.string.tile_buy);
                if (tile.nextKind == NEXT_MILESTONE) {
                    int garden = tile.garden;
                    cell.buy.setOnClickListener(v -> {
                        if (h.world >= 0 && state.buy(h.world, garden, GameState.BUY_NEXT)) {
                            adapter.notifyDataSetChanged();
                        }
                    });
                }
                h.nextCells.add(cell);
                applyFont(next);
                applyUnicodeFont(next.findViewById(R.id.medal_caption));
            } else {
                next = new View(this);
            }
            addMedalCell(rowLayout, next);
        }
        int remainder = tiles.size() % BADGES_PER_ROW;
        if (rowLayout != null && remainder != 0) {
            for (int i = remainder; i < BADGES_PER_ROW; i++) {
                addMedalCell(rowLayout, new View(this));
                addMedalCell(rowLayout, new View(this));
            }
        }
    }

    private void updateNextCells(FarmHolder h, int w, double income) {
        double money = state.getMoney(w);
        for (NextCell cell : h.nextCells) {
            Tile tile = cell.tile;
            double cost;
            boolean canBuy = false;
            if (tile.nextKind == NEXT_MILESTONE) {
                GameState.Quote quote = state.quote(w, tile.garden, GameState.BUY_NEXT);
                cost = quote.cost;
                canBuy = quote.amount > 0;
            } else if (tile.nextKind == NEXT_MEDAL) {
                cost = tile.medalThreshold;
            } else {
                cell.timer.setVisibility(View.GONE);
                cell.buy.setVisibility(View.GONE);
                continue;
            }
            double wait = money >= cost ? 0 : income > 0 ? (cost - money) / income : Double.POSITIVE_INFINITY;
            boolean showBuy = tile.nextKind == NEXT_MILESTONE && canBuy && wait <= 0;
            cell.buy.setVisibility(showBuy ? View.VISIBLE : View.GONE);
            cell.timer.setVisibility(showBuy ? View.GONE : View.VISIBLE);
            if (!showBuy) {
                ViewUpdates.setText(cell.timer, wait <= 0 ? getString(R.string.finance_afford_now) : longDuration(wait));
            }
        }
    }

    private LinearLayout newMedalRow(LinearLayout parent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        parent.addView(row, params);
        return row;
    }

    private void addMedalCell(LinearLayout row, View cell) {
        int gap = getResources().getDimensionPixelSize(R.dimen.space_between_elements) / 2;
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        params.setMargins(gap, gap, gap, gap);
        row.addView(cell, params);
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

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BADGES_PER_ROW = getResources().getInteger(
                R.integer.achievements_badge_columns
        ) / 2;
    }
}
