package com.rama.fikret.activities;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.rama.fikret.R;
import com.rama.fikret.economy.GameState;
import com.rama.fikret.economy.NumberFormatter;
import com.rama.fikret.economy.Worlds;
import com.rama.fikret.helpers.GardenIcons;
import com.rama.fikret.helpers.ViewUpdates;

import java.util.ArrayList;
import java.util.List;

public class UpgradesActivity extends ListScreenActivity {
    private static final int[] TYPE_ORDER = {GameState.UPGRADE_VALUE, GameState.UPGRADE_SPEED};
    private final List<int[]> rows = new ArrayList<>();

    @Override
    protected String screenTitle() {
        return getString(R.string.menu_upgrades);
    }

    @Override
    protected String emptyText() {
        return getString(R.string.empty_upgrades);
    }

    @Override
    protected String subtitle() {
        return Worlds.ALL[state.getCurrentWorld()].name;
    }

    private static final int VIEW_UPGRADE = 0;
    private static final int VIEW_QUICK_BUY = 1;

    private boolean hasHeader() {
        return state.hasQuickBuy(world) && !rows.isEmpty();
    }

    private int headerCount() {
        return hasHeader() ? 1 : 0;
    }

    private boolean isHeader(int position) {
        return position == 0 && hasHeader();
    }

    private boolean anyAffordable() {
        for (int[] r : rows) {
            if (state.getCount(world, r[2]) > 0 && r[0] == state.upgradeLevel(world, r[2], r[1])
                    && state.getMoney(world) >= state.upgradeCostAt(world, r[2], r[1], r[0])) {
                return true;
            }
        }
        return false;
    }

    private void rebuildRows() {
        rows.clear();
        for (int tier = 0; tier < Worlds.UPGRADE_MAX_LEVEL; tier++) {
            for (int type : TYPE_ORDER) {
                for (int g = 0; g < Worlds.GARDENS_PER_WORLD; g++) {
                    if (tier >= state.upgradeLevel(world, g, type)) {
                        rows.add(new int[]{tier, type, g});
                    }
                }
            }
        }
    }

    @Override
    protected BaseAdapter createAdapter() {
        rebuildRows();
        return new BaseAdapter() {
            @Override
            public int getCount() {
                return rows.size() + headerCount();
            }

            @Override
            public int getViewTypeCount() {
                return 2;
            }

            @Override
            public int getItemViewType(int position) {
                return isHeader(position) ? VIEW_QUICK_BUY : VIEW_UPGRADE;
            }

            @Override
            public Object getItem(int position) {
                return rows.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View row = convertView;
                if (row == null) {
                    if (isHeader(position)) {
                        row = LayoutInflater.from(UpgradesActivity.this).inflate(R.layout.list_item_quick_buy, parent, false);
                        applyFont(row);
                        row.findViewById(R.id.quick_buy_button).setOnClickListener(v -> {
                            if (state.buyAllUpgrades(world) > 0) {
                                rebuildRows();
                                adapter.notifyDataSetChanged();
                            }
                        });
                    } else {
                        row = LayoutInflater.from(UpgradesActivity.this).inflate(R.layout.list_item_upgrade, parent, false);
                        applyFont(row);
                        wireListener(row);
                    }
                }
                bindRow(row, position);
                return row;
            }
        };
    }

    private void wireListener(final View row) {
        row.findViewById(R.id.activate_button).setOnClickListener(v -> {
            int position = (Integer) row.getTag() - headerCount();
            if (position < 0 || position >= rows.size()) {
                return;
            }
            int[] upgrade = rows.get(position);
            if (state.getCount(world, upgrade[2]) > 0 && state.buyUpgrade(world, upgrade[2], upgrade[1], upgrade[0])) {
                rebuildRows();
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    protected void bindRow(View row, int position) {
        row.setTag(position);
        if (isHeader(position)) {
            ViewUpdates.setBackgroundColor(row.findViewById(R.id.quick_buy_button),
                    anyAffordable() ? ViewUpdates.themeColor(this, R.attr.accent) : ViewUpdates.color(this, R.color.disabled));
            return;
        }
        position -= headerCount();
        if (position < 0 || position >= rows.size()) {
            return;
        }
        int tier = rows.get(position)[0];
        int type = rows.get(position)[1];
        int g = rows.get(position)[2];

        String garden = Worlds.ALL[world].gardens[g].name;
        ViewUpdates.setImageResource((ImageView) row.findViewById(R.id.garden_picture), GardenIcons.forGarden(world, g));
        boolean speed = type == GameState.UPGRADE_SPEED;
        ViewUpdates.setText((TextView) row.findViewById(R.id.garden_name), getString(speed ? R.string.upgrade_speed_name : R.string.upgrade_value_name, garden));
        int skipped = tier - state.upgradeLevel(world, g, type);
        ViewUpdates.setText((TextView) row.findViewById(R.id.upgrade_description), getString(speed ? R.string.upgrade_speed_desc : R.string.upgrade_value_desc, garden) + (skipped > 0 ? " " + getResources().getQuantityString(R.plurals.upgrade_includes_tiers, skipped, skipped) : ""));

        double cost = state.upgradeCostAt(world, g, type, tier);
        ViewUpdates.setText((TextView) row.findViewById(R.id.activate_cost), NumberFormatter.money(world, cost));

        boolean available = state.upgradeLevel(world, g, type) <= tier && state.getCount(world, g) > 0;
        boolean affordable = available && state.getMoney(world) >= cost;
        ViewUpdates.setBackgroundColor(row.findViewById(R.id.activate_button), affordable ? ViewUpdates.themeColor(this, R.attr.accent) : ViewUpdates.color(this, R.color.disabled));
    }
}
