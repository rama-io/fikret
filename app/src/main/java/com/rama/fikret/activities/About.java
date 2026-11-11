package com.rama.fikret.activities;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.rama.fikret.R;
import com.rama.fikret.economy.GameState;
import com.rama.fikret.helpers.FarmArt;
import com.rama.fikret.helpers.SystemBars;
import com.rama.fikret.managers.FontManager;

public class About extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FarmArt.applyTheme(this, GameState.get(this).getCurrentWorld());
        setContentView(R.layout.activity_about);
        View root = findViewById(R.id.root);
        SystemBars.applyInsets(root);
        FontManager.apply(root, FontManager.get(this));
        TextView appName = findViewById(R.id.name_version);
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            appName.setText(getString(R.string.app_version, getString(R.string.app_name), info.versionCode));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        findViewById(R.id.go_back).setOnClickListener(v -> finish());
    }
}
