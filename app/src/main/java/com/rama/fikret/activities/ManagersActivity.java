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
import com.rama.fikret.helpers.FarmArt;
import com.rama.fikret.helpers.ViewUpdates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ManagersActivity extends ListScreenActivity {
    private static final int KIND_MANAGER = 0;
    private static final int KIND_GUIDE = 1;
    private static final int KIND_ACCOUNTANT = 2;
    private static final int KIND_DISCOUNT = 3;
    private static final int KIND_BUYER = 4;
    private static final int KIND_QUICK_BUY = 5;
    private static final int KIND_SUPER = 6;
    private static final int KIND_EFFICIENCY = 7;

    private final List<int[]> rows = new ArrayList<>();

    @Override
    protected String screenTitle() {
        return getString(R.string.menu_managers);
    }

    @Override
    protected String emptyText() {
        return getString(R.string.empty_managers);
    }

    protected boolean pointsScreen() {
        return false;
    }

    @Override
    protected String subtitle() {
        return Worlds.ALL[state.getCurrentWorld()].name;
    }

    private void rebuildRows() {
        rows.clear();
        if (pointsScreen()) {
            if (state.hasAccountantList(world)) {
                for (int g = 0; g < Worlds.GARDENS_PER_WORLD; g++) {
                    if (!state.hasAccountant(world, g)) {
                        rows.add(new int[]{KIND_ACCOUNTANT, g});
                    }
                }
            }
            for (int i = 0; i < Worlds.SUPER_UPGRADE_COUNT; i++) {
                if (!state.hasSuperUpgrade(world, i)) {
                    rows.add(new int[]{KIND_SUPER, i});
                }
            }
        } else {
            for (int g = 0; g < Worlds.GARDENS_PER_WORLD; g++) {
                if (!state.hasManager(world, g)) {
                    rows.add(new int[]{KIND_MANAGER, g});
                }
            }
            if (state.hasDiscountList(world)) {
                for (int g = 0; g < Worlds.GARDENS_PER_WORLD; g++) {
                    if (!state.hasDiscount(world, g)) {
                        rows.add(new int[]{KIND_DISCOUNT, g});
                    }
                }
            }
            if (!state.hasTravelManager(world)) {
                rows.add(new int[]{KIND_GUIDE, 0});
            }
            if (!state.hasBuyer(world)) {
                rows.add(new int[]{KIND_BUYER, 0});
            }
            if (!state.hasQuickBuy(world)) {
                rows.add(new int[]{KIND_QUICK_BUY, 0});
            }
            for (int i = 0; i < Worlds.EFFICIENCY_BIRD_COUNT; i++) {
                if (!state.hasEfficiencyBird(world, i)) {
                    rows.add(new int[]{KIND_EFFICIENCY, i});
                }
            }
        }
        Collections.sort(rows, new Comparator<int[]>() {
            @Override
            public int compare(int[] a, int[] b) {
                boolean aPoints = paidInPoints(a[0]);
                boolean bPoints = paidInPoints(b[0]);
                if (aPoints != bPoints) {
                    return aPoints ? 1 : -1;
                }
                boolean aAffordable = isAffordable(a[0], a[1]);
                boolean bAffordable = isAffordable(b[0], b[1]);
                if (aAffordable != bAffordable) {
                    return aAffordable ? -1 : 1;
                }
                return Double.compare(cost(a[0], a[1]), cost(b[0], b[1]));
            }
        });
    }

    /** Accountants and super upgrades are bought with farm points; everything else costs money. */
    private static boolean paidInPoints(int kind) {
        return kind == KIND_ACCOUNTANT || kind == KIND_SUPER;
    }

    private double cost(int kind, int g) {
        switch (kind) {
            case KIND_GUIDE:
                return state.travelManagerCost(world);
            case KIND_BUYER:
                return state.buyerCost(world);
            case KIND_QUICK_BUY:
                return state.quickBuyCost(world);
            case KIND_ACCOUNTANT:
                return state.accountantCost(world, g);
            case KIND_SUPER:
                return state.superUpgradeCost(g);
            case KIND_EFFICIENCY:
                return state.efficiencyBirdCost(world, g);
            case KIND_DISCOUNT:
                return state.discountCost(world, g);
            default:
                return state.managerCost(world, g);
        }
    }

    private boolean isAffordable(int kind, int g) {
        double cost = cost(kind, g);
        return paidInPoints(kind) ? state.getAngels(world) >= cost : state.getMoney(world) >= cost;
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
                    row = LayoutInflater.from(ManagersActivity.this).inflate(R.layout.list_item_manager, parent, false);
                    applyFont(row);
                    ((ImageView) row.findViewById(R.id.manager_picture))
                            .setImageDrawable(FarmArt.supervisor(ManagersActivity.this, world));
                    wireListener(row);
                }
                bindRow(row, position);
                return row;
            }
        };
    }

    private void wireListener(final View row) {
        row.findViewById(R.id.rescue_button).setOnClickListener(v -> {
            int position = (Integer) row.getTag();
            if (position >= rows.size()) {
                return;
            }
            int kind = rows.get(position)[0];
            int g = rows.get(position)[1];
            boolean hired;
            switch (kind) {
                case KIND_GUIDE:
                    hired = state.hireTravelManager(world);
                    break;
                case KIND_BUYER:
                    hired = state.hireBuyer(world);
                    break;
                case KIND_QUICK_BUY:
                    hired = state.hireQuickBuy(world);
                    break;
                case KIND_ACCOUNTANT:
                    hired = state.hireAccountant(world, g);
                    break;
                case KIND_SUPER:
                    hired = state.buySuperUpgrade(world, g);
                    break;
                case KIND_EFFICIENCY:
                    hired = state.hireEfficiencyBird(world, g);
                    break;
                case KIND_DISCOUNT:
                    hired = state.hireDiscountManager(world, g);
                    break;
                default:
                    hired = state.hireManager(world, g);
                    break;
            }
            if (hired) {
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
        int kind = rows.get(position)[0];
        int g = rows.get(position)[1];
        Worlds.WorldDef def = Worlds.ALL[world];

        String name;
        String description;
        String costLabel;
        boolean affordable;
        switch (kind) {
            case KIND_GUIDE: {
                double cost = state.travelManagerCost(world);
                name = getString(R.string.guide_bird_name);
                description = getString(R.string.guide_bird_desc);
                costLabel = NumberFormatter.money(world, cost);
                affordable = state.getMoney(world) >= cost;
                break;
            }
            case KIND_BUYER: {
                double cost = state.buyerCost(world);
                name = getString(R.string.buyer_bird_name);
                description = getString(R.string.buyer_bird_desc);
                costLabel = NumberFormatter.money(world, cost);
                affordable = state.getMoney(world) >= cost;
                break;
            }
            case KIND_QUICK_BUY: {
                double cost = state.quickBuyCost(world);
                name = getString(R.string.quick_buy_bird_name);
                description = getString(R.string.quick_buy_bird_desc);
                costLabel = NumberFormatter.money(world, cost);
                affordable = state.getMoney(world) >= cost;
                break;
            }
            case KIND_ACCOUNTANT: {
                double cost = state.accountantCost(world, g);
                String garden = def.gardens[g].name;
                name = getString(R.string.accountant_name, garden);
                description = getString(R.string.accountant_desc, garden);
                costLabel = getString(R.string.farm_points_cost, NumberFormatter.number(cost));
                affordable = state.getAngels(world) >= cost;
                break;
            }
            case KIND_EFFICIENCY: {
                double cost = state.efficiencyBirdCost(world, g);
                String pct = NumberFormatter.number(Worlds.efficiencyBirdValue(g) * 100);
                name = getString(R.string.bird_efficiency_name, pct);
                description = getString(R.string.super_efficiency_desc, pct);
                costLabel = NumberFormatter.money(world, cost);
                affordable = state.getMoney(world) >= cost;
                break;
            }
            case KIND_SUPER: {
                double cost = state.superUpgradeCost(g);
                double value = Worlds.superValue(g);
                if (Worlds.superKind(g) == Worlds.SUPER_PROFIT) {
                    String x = value == Math.rint(value) ? String.format(Locale.US, "%.0f", value) : String.format(Locale.US, "%.2f", value);
                    name = getString(R.string.super_profit_name, x);
                    description = getString(R.string.super_profit_desc, x);
                } else {
                    String pct = NumberFormatter.number(value * 100);
                    name = getString(R.string.super_efficiency_name, pct);
                    description = getString(R.string.super_efficiency_desc, pct);
                }
                costLabel = getString(R.string.farm_points_cost, NumberFormatter.number(cost));
                affordable = state.getAngels(world) >= cost;
                break;
            }
            case KIND_DISCOUNT: {
                double cost = state.discountCost(world, g);
                String garden = def.gardens[g].name;
                name = getString(R.string.discount_name, garden);
                description = getString(R.string.discount_desc, garden);
                costLabel = NumberFormatter.money(world, cost);
                affordable = state.getMoney(world) >= cost;
                break;
            }
            default: {
                double cost = state.managerCost(world, g);
                String garden = def.gardens[g].name;
                name = getString(R.string.supervisor_name, garden);
                description = getString(R.string.supervisor_desc, garden);
                costLabel = NumberFormatter.money(world, cost);
                affordable = state.getMoney(world) >= cost;
                break;
            }
        }

        ViewUpdates.setText((TextView) row.findViewById(R.id.manager_name), name);
        ViewUpdates.setText((TextView) row.findViewById(R.id.manager_description), description);
        ViewUpdates.setText((TextView) row.findViewById(R.id.rescue_label), getString(R.string.rescue));
        TextView costView = (TextView) row.findViewById(R.id.rescue_cost);
        ViewUpdates.setText(costView, costLabel);
        ViewUpdates.setBackgroundColor(row.findViewById(R.id.rescue_button),
                affordable ? ViewUpdates.themeColor(this, R.attr.accent) : ViewUpdates.color(this, R.color.disabled));
    }
}
