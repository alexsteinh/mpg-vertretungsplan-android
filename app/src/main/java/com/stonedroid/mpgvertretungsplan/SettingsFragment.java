package com.stonedroid.mpgvertretungsplan;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.MenuItem;
import de.stonedroid.vertretungsplan.Grade;

import java.util.List;

public class SettingsFragment extends PreferenceFragment
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Load "pseudo" preferences (small hack to dynamically add preferences)
        addPreferencesFromResource(R.xml.preferences);
        // Add grade preference
        addGradePreference();
    }

    // Add the grade preference to the fragment
    private void addGradePreference()
    {
        PreferenceScreen screen = getPreferenceScreen();
        ListPreference gradePref = new ListPreference(getActivity());
        gradePref.setKey(getString(R.string.saved_grade));
        gradePref.setTitle(R.string.pref_title_grade);
        // Get all grade names for preference entries
        List<String> names = Grade.getGradeNames();
        // Convert to CharSequences because preference only support CharSequences
        CharSequence[] charNames =  new CharSequence[names.size()];
        charNames = names.toArray(charNames);
        // Set entries/entry values
        gradePref.setEntries(charNames);
        gradePref.setEntryValues(charNames);
        // Add preference to fragment
        screen.addPreference(gradePref);
    }
}
