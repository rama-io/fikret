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
                return rows.size();
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
                    row = LayoutInflater.from(UpgradesActivity.this).inflate(R.layout.list_item_upgrade, parent, false);
                    applyFont(row);
                    wireListener(row);
                }
                bindRow(row, position);
                return row;
            }
        };
    }

    private void wireListener(final View row) {
        row.findViewById(R.id.activate_button).setOnClickListener(v -> {
            int position = (Integer) row.getTag();
            if (position >= rows.size()) {
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
        if (position >= rows.size()) {
            return;
        }
        int tier = rows.get(position)[0];
        int type = rows.get(position)[1];
        int g = rows.get(position)[2];

        String garden = Worlds.ALL[world].gardens[g].name;
        ViewUpdates.setImageResource((ImageView) row.findViewById(R.id.garden_picture), GardenIcons.forGarden(world, g));
        boolean speed = type == GameState.UPGRADE_SPEED;
        ViewUpdates.setText((TextView) row.findViewById(R.id.garden_name), garden + (speed ? " Speed x2" : " Value x2"));
        int skipped = tier - state.upgradeLevel(world, g, type);
        ViewUpdates.setText((TextView) row.findViewById(R.id.upgrade_description), (speed ? "Doubles how fast " + garden + " harvests." : "Doubles how much " + garden + " earns.") + (skipped > 0 ? " Includes " + skipped + " cheaper tier" + (skipped > 1 ? "s" : "") + "." : ""));

        double cost = state.upgradeCostAt(world, g, type, tier);
        ViewUpdates.setText((TextView) row.findViewById(R.id.activate_cost), NumberFormatter.money(world, cost));

        boolean available = state.upgradeLevel(world, g, type) <= tier && state.getCount(world, g) > 0;
        boolean affordable = available && state.getMoney(world) >= cost;
        ViewUpdates.setBackgroundColor(row.findViewById(R.id.activate_button), affordable ? ViewUpdates.themeColor(this, R.attr.accent) : ViewUpdates.color(this, R.color.disabled));
    }
}
