package com.stonedroid.mpgvertretungsplan.settings;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.stonedroid.mpgvertretungsplan.theme.CustomThemes;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CustomThemes.changeTheme(this, true);

        // Push SettingsFragment into the the foreground
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}
