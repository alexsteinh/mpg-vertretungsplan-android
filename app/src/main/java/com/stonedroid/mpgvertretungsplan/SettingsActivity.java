package com.stonedroid.mpgvertretungsplan;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Show grade dialog, if user is forced to pick a class
        Bundle extras = getIntent().getExtras();
        if (extras.getBoolean("with_dialog"))
        {
            createGradeDialog().show();
        }

        // Push SettingsFragment into the the foreground
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    private AlertDialog createGradeDialog()
    {
        return new AlertDialog.Builder(this)
                .setMessage(R.string.grade_message)
                .setPositiveButton("OK", null)
                .create();
    }
}
