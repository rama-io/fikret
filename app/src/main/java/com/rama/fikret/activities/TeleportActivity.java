package com.rama.fikret.activities;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.rama.fikret.R;
import com.rama.fikret.economy.Worlds;
import com.rama.fikret.game.Maps;
import com.rama.fikret.game.Stage;
import com.rama.fikret.helpers.DeveloperMode;
import com.rama.fikret.managers.PrefsManager;

public class TeleportActivity extends ListScreenActivity {

    @Override
    protected String screenTitle() {
        return getString(R.string.menu_teleport);
    }

    @Override
    protected void bindRow(android.view.View row, int position) {
        // static rows, nothing to refresh
    }

    @Override
    protected BaseAdapter createAdapter() {
        return new BaseAdapter() {
            @Override
            public int getCount() {
                return DeveloperMode.availableFarms();
            }

            @Override
            public Object getItem(int position) {
                return Worlds.ALL_STAGE_IDS[position];
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView row = (TextView) convertView;
                if (row == null) {
                    row = (TextView) LayoutInflater.from(TeleportActivity.this).inflate(R.layout.list_item_teleport, parent, false);
                    applyFont(row);
                }
                final int stageId = Worlds.ALL_STAGE_IDS[position];
                row.setText(Worlds.stageName(stageId));
                row.setOnClickListener(v -> teleportTo(stageId));
                return row;
            }
        };
    }

    private void teleportTo(int stageId) {
        Stage stage = Maps.get(stageId);
        PrefsManager.getInstance(this).saveTeleportHome(stageId, stage.spawnRow, stage.spawnCol);
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra(GameActivity.EXTRA_TELEPORT_ARRIVAL, true);
        startActivity(intent);
        finish();
    }
}
