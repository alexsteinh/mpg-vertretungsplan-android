package com.stonedroid.mpgvertretungsplan;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;
import de.stonedroid.vertretungsplan.Grade;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SettingsFragment extends PreferenceFragmentCompat
{
    public static final String TAG = SettingsFragment.class.getSimpleName();

    private SharedPreferences preferences;
    private PreferenceScreen screen;

    @Override
    public void onCreatePreferences(Bundle bundle, String s)
    {
        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        preferences.edit()
                .putBoolean(getString(R.string.saved_init_preferences), true)
                .apply();

        // Load "pseudo" preferences (small hack to dynamically add preferences)
        addPreferencesFromResource(R.xml.preferences);

        screen = getPreferenceScreen();

        addGradePreference();
        addThemePreference();
        addNotificationPreference();
        addFilterPreference();

        preferences.edit()
                .putBoolean(getString(R.string.saved_init_preferences), false)
                .apply();
    }

    // Add the grade preference to the fragment
    private void addGradePreference()
    {
        // Add preference category
        PreferenceCategory category = new PreferenceCategory(getActivity());
        category.setTitle(R.string.general);
        screen.addPreference(category);

        // Create preference for choosing user's grade
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
        // Add listener
        gradePref.setOnPreferenceChangeListener((preference, newValue) ->
        {
            showValue(preference, newValue);

            CheckBoxPreference filterPref = (CheckBoxPreference) screen.getPreferenceManager()
                    .findPreference(getString(R.string.saved_filter_enabled));

            String strGrade = (String) newValue;
            if (filterPref != null)
            {
                filterPref.setEnabled(!(!strGrade.equals("11") && !strGrade.equals("12")));
            }

            return true;
        });
        // Add preference to fragment
        screen.addPreference(gradePref);
        showValue(gradePref);
    }

    private void addThemePreference()
    {
        // Add new preference category
        PreferenceCategory category = new PreferenceCategory(getActivity());
        category.setTitle(getString(R.string.pref_category_customizing));
        screen.addPreference(category);

        // Add Theme options
        ListPreference pref = new ListPreference(getActivity());
        CharSequence[] themeNames = {"Orange", "Weiß", "Schwarz"};
        pref.setTitle(getString(R.string.pref_title_theme));
        pref.setKey(getString(R.string.saved_theme));
        pref.setEntries(themeNames);
        pref.setEntryValues(themeNames);
        pref.setDefaultValue(themeNames[0]);
        pref.setOnPreferenceChangeListener((preference, newValue) ->
        {
            showValue(preference, newValue);
            getActivity().recreate();
            return true;
        });
        // Add preference to screen
        screen.addPreference(pref);
        showValue(pref);
    }

    private void addNotificationPreference()
    {
        PreferenceCategory category = new PreferenceCategory(getActivity());
        category.setTitle(getString(R.string.pref_category_notifications));
        screen.addPreference(category);

        CheckBoxPreference enabled = new CheckBoxPreference(getActivity());
        enabled.setKey(getString(R.string.saved_notifications_enabled));
        enabled.setTitle(getString(R.string.pref_title_notifications_enabled));
        screen.addPreference(enabled);
    }

    private void addFilterPreference()
    {
        // Add new preference category
        PreferenceCategory category = new PreferenceCategory(getActivity());
        category.setTitle(R.string.pref_category_filter);
        screen.addPreference(category);

        // Add quick explanation of the preference category
        Preference explanation = new Preference(getActivity());
        explanation.setSummary(getString(R.string.pref_category_summary_filter));
        explanation.setSelectable(false);
        screen.addPreference(explanation);

        // Add enable checkbox
        CheckBoxPreference enabled = new CheckBoxPreference(getActivity());
        enabled.setKey(getString(R.string.saved_filter_enabled));
        enabled.setTitle(R.string.pref_title_filter_enabled);
        String grade = preferences.getString(getString(R.string.saved_grade), null);
        enabled.setEnabled(!(grade != null && !grade.equals("11") && !grade.equals("12")));
        screen.addPreference(enabled);

        // Add subjects
        Map<String, List<String>> subjects = Subject.getAllSubjects();
        Set<String> keys = subjects.keySet();

        for (String key : keys)
        {
            ListPreference subjectPref = new ListPreference(getActivity());
            subjectPref.setKey(String.format("filter_enabled_%s", key));
            subjectPref.setTitle(key);
            // Set possible subject names
            ArrayList<String> names = new ArrayList<>(subjects.get(key));
            names.add(0, "Alle anzeigen");
            names.add(1, "Nichts anzeigen");
            // Convert to CharSequence
            CharSequence[] charNames = new CharSequence[names.size()];
            charNames = names.toArray(charNames);
            // Set entries/entry values
            subjectPref.setDefaultValue(charNames[0]);
            subjectPref.setEntries(charNames);
            subjectPref.setEntryValues(charNames);
            subjectPref.setOnPreferenceChangeListener((preference, newValue) ->
            {
                showValue(preference, newValue);
                return true;
            });
            screen.addPreference(subjectPref);
            showValue(subjectPref);
            subjectPref.setDependency(getString(R.string.saved_filter_enabled));
        }
    }

    private void showValue(ListPreference preference)
    {
        String value = preferences.getString(preference.getKey(), null);
        if (value != null)
        {
            preference.setSummary(value);
        }
    }

    private void showValue(Preference preference, Object value)
    {
        preference.setSummary((String) value);
    }
}
