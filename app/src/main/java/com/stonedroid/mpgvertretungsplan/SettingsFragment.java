package com.stonedroid.mpgvertretungsplan;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.*;
import de.stonedroid.vertretungsplan.Grade;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SettingsFragment extends PreferenceFragment
{
    private SharedPreferences preferences;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        // Load "pseudo" preferences (small hack to dynamically add preferences)
        addPreferencesFromResource(R.xml.preferences);

        addGradePreference();
        if (Build.VERSION.SDK_INT >= 23) addThemePreference(); // Only allow customization with Android 6.0 (SDK 23)
        addFilterPreference();
    }

    // Add the grade preference to the fragment
    private void addGradePreference()
    {
        PreferenceScreen screen = getPreferenceScreen();

        // Add preference category
        PreferenceCategory category = new PreferenceCategory(getActivity());
        category.setTitle(R.string.general);
        screen.addPreference(category);

        // Create preference for choosing user's grade
        XListPreference gradePref = new XListPreference(getActivity());
        // Add listener
        gradePref.setOnPreferenceChangeListener((preference, newValue) ->
        {
            getActivity().onBackPressed();
            return true;
        });

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
        gradePref.showValue();
        // Add preference to fragment
        screen.addPreference(gradePref);
    }

    private void addThemePreference()
    {
        PreferenceScreen screen = getPreferenceScreen();

        // Add new preference category
        PreferenceCategory category = new PreferenceCategory(getActivity());
        category.setTitle(getString(R.string.pref_category_customizing));
        screen.addPreference(category);

        // Add Theme options
        XListPreference pref = new XListPreference(getActivity());
        CharSequence[] themeNames = {"Orange", "Weiß"};
        pref.setTitle(getString(R.string.pref_title_theme));
        pref.setKey(getString(R.string.saved_theme));
        pref.setEntries(themeNames);
        pref.setEntryValues(themeNames);
        pref.setDefaultValue(themeNames[0]);
        pref.showValue();
        pref.setOnPreferenceChangeListener((preference, newValue) ->
        {
            getActivity().recreate();
            return true;
        });
        // Add preference to screen
        screen.addPreference(pref);
    }

    private void addFilterPreference()
    {
        PreferenceScreen screen = getPreferenceScreen();

        // Add new preference category
        PreferenceCategory category = new PreferenceCategory(getActivity());
        category.setTitle(R.string.pref_category_filter);
        screen.addPreference(category);

        // Add enable checkbox
        CheckBoxPreference enabled = new CheckBoxPreference(getActivity());
        enabled.setKey(getString(R.string.saved_filter_enabled));
        enabled.setTitle(R.string.pref_title_filter_enabled);
        screen.addPreference(enabled);

        // Add subjects
        Map<String, List<String>> subjects = Subject.getAllSubjects();
        Set<String> keys = subjects.keySet();

        for (String key : keys)
        {
            XListPreference subjectPref = new XListPreference(getActivity());
            subjectPref.setKey(String.format("filter_enabled_%s", key));
            subjectPref.setTitle(key);
            // Set possible subject names
            ArrayList<String> names = new ArrayList<>(subjects.get(key));
            names.add(0, "Ignorieren");
            names.add(1, "Alle blockieren");
            // Convert to CharSequence
            CharSequence[] charNames = new CharSequence[names.size()];
            charNames = names.toArray(charNames);
            // Set entries/entry values
            subjectPref.setDefaultValue(charNames[0]);
            subjectPref.setEntries(charNames);
            subjectPref.setEntryValues(charNames);
            subjectPref.showValue();
            screen.addPreference(subjectPref);
            subjectPref.setDependency(getString(R.string.saved_filter_enabled));
        }
    }
}
