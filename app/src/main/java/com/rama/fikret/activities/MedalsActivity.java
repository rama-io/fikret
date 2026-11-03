package com.rama.fikret.activities;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.rama.fikret.R;
import com.rama.fikret.economy.NumberFormatter;
import com.rama.fikret.economy.Worlds;
import com.rama.fikret.helpers.ViewUpdates;

import java.util.ArrayList;
import java.util.List;

public class MedalsActivity extends ListScreenActivity {
    private final List<Integer> rows = new ArrayList<>();

    @Override
    protected String screenTitle() {
        return getString(R.string.menu_medals);
    }

    @Override
    protected String emptyText() {
        return getString(R.string.empty_medals);
    }

    @Override
    protected String subtitle() {
        return Worlds.ALL[state.getCurrentWorld()].name;
    }

    private void rebuildRows() {
        rows.clear();
        for (int i = 0; i < Worlds.PROFIT_UPGRADE_COUNT; i++) {
            if (!state.hasProfitUpgrade(world, i)) {
                rows.add(i);
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
                    row = LayoutInflater.from(MedalsActivity.this).inflate(R.layout.list_item_upgrade, parent, false);
                    applyFont(row);
                    ((ImageView) row.findViewById(R.id.garden_picture)).setImageResource(R.drawable.medal);
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
            if (position < 0 || position >= rows.size()) {
                return;
            }
            if (state.buyProfitUpgrade(world, rows.get(position))) {
                rebuildRows();
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    protected void bindRow(View row, int position) {
        row.setTag(position);
        if (position < 0 || position >= rows.size()) {
            return;
        }
        int index = rows.get(position);
        String x = NumberFormatter.multiplier(Worlds.profitUpgradeValue(index));
        double cost = state.profitUpgradeCost(world, index);
        ViewUpdates.setText((TextView) row.findViewById(R.id.garden_name), getString(R.string.upgrade_profit_name, x));
        ViewUpdates.setText((TextView) row.findViewById(R.id.upgrade_description), getString(R.string.upgrade_profit_desc, x));
        ViewUpdates.setText((TextView) row.findViewById(R.id.activate_cost), NumberFormatter.money(world, cost));
        ViewUpdates.setBackgroundColor(row.findViewById(R.id.activate_button),
                state.getMoney(world) >= cost ? ViewUpdates.themeColor(this, R.attr.accent) : ViewUpdates.color(this, R.color.disabled));
    }
}
