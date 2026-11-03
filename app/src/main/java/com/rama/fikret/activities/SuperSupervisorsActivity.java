package com.rama.fikret.activities;

import com.rama.fikret.R;
import com.rama.fikret.economy.NumberFormatter;

public class SuperSupervisorsActivity extends ManagersActivity {
    @Override
    protected String screenTitle() {
        return getString(R.string.menu_super_managers);
    }

    @Override
    protected String emptyText() {
        return getString(R.string.empty_super_managers);
    }

    @Override
    protected String balanceText() {
        return getString(R.string.farm_points_cost, NumberFormatter.number(state.getAngels(world)));
    }

    @Override
    protected boolean pointsScreen() {
        return true;
    }
}
