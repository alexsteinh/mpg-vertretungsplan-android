package com.stonedroid.mpgvertretungsplan;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v14.preference.MultiSelectListPreference;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;
import de.stonedroid.vertretungsplan.Grade;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SettingsFragment extends PreferenceFragmentCompat
{
    public static final String TAG = SettingsFragment.class.getSimpleName();

    public static final String SHOW_ALL = "Alle anzeigen";
    public static final String SHOW_NOTHING = "Nichts anzeigen";

    private SharedPreferences preferences;
    private PreferenceScreen screen;
    private Context context;

    private Map<String, List<String>> subjectMap = Subject.getAllSubjects();
    private Set<String> subjectKeys = subjectMap.keySet();

    @Override
    public void onCreatePreferences(Bundle bundle, String s)
    {
        context = getActivity();

        preferences = PreferenceManager.getDefaultSharedPreferences(context);
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
        addAboutTheAppPreferences();

        preferences.edit()
                .putBoolean(getString(R.string.saved_init_preferences), false)
                .apply();
    }

    // Add the grade preference to the fragment
    private void addGradePreference()
    {
        // Add preference category
        PreferenceCategory category = new PreferenceCategory(context);
        category.setTitle(R.string.general);
        screen.addPreference(category);

        // Create preference for choosing user's grade
        ListPreference gradePref = new ListPreference(context);

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
        PreferenceCategory category = new PreferenceCategory(context);
        category.setTitle(getString(R.string.pref_category_customizing));
        screen.addPreference(category);

        // Add Theme options
        ListPreference pref = new ListPreference(context);
        CharSequence[] themeNames = CustomThemes.getSimpleNames();
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
        PreferenceCategory category = new PreferenceCategory(context);
        category.setTitle(getString(R.string.pref_category_notifications));
        screen.addPreference(category);

        CheckBoxPreference enabled = new CheckBoxPreference(context);
        enabled.setKey(getString(R.string.saved_notifications_enabled));
        enabled.setTitle(getString(R.string.pref_title_notifications_enabled));
        screen.addPreference(enabled);
    }

    private void addFilterPreference()
    {
        // Add new preference category
        PreferenceCategory category = new PreferenceCategory(context);
        category.setTitle(R.string.pref_category_filter);
        screen.addPreference(category);

        // Add quick explanation of the preference category
        Preference explanation = new Preference(context);
        explanation.setSummary(getString(R.string.pref_category_summary_filter));
        explanation.setSelectable(false);
        screen.addPreference(explanation);

        // Add enable checkbox
        CheckBoxPreference enabled = new CheckBoxPreference(context);
        enabled.setKey(getString(R.string.saved_filter_enabled));
        enabled.setTitle(R.string.pref_title_filter_enabled);
        String grade = preferences.getString(getString(R.string.saved_grade), null);
        enabled.setEnabled(!(grade != null && !grade.equals("11") && !grade.equals("12")));
        screen.addPreference(enabled);

        // Add multiple choice checkbox
        CheckBoxPreference multi_enabled = new CheckBoxPreference(context);
        multi_enabled.setKey(getString(R.string.saved_filter_multi_select_enabled));
        multi_enabled.setTitle(getString(R.string.pref_title_filter_multi_select_enabled));
        if (preferences.getBoolean(getString(R.string.saved_filter_multi_select_enabled), false))
        {
            multi_enabled.setSummary(getString(R.string.pref_summary_filter_multi_select_enabled));
        }
        multi_enabled.setOnPreferenceChangeListener((preference, newValue) ->
        {
            boolean multiEnabled = (boolean) newValue;
            deleteFilterPreferences();
            changeFilterSettings(multiEnabled);
            addFilterPreferences2(multiEnabled);
            if (multiEnabled)
            {
                multi_enabled.setSummary(getString(R.string.pref_summary_filter_multi_select_enabled));
            }
            else
            {
                multi_enabled.setSummary("");
            }

            return true;
        });
        screen.addPreference(multi_enabled);
        multi_enabled.setDependency(getString(R.string.saved_filter_enabled));
        addFilterPreferences2(preferences.getBoolean(getString(R.string.saved_filter_multi_select_enabled), false));
    }

    private void addFilterPreferences2(boolean multiEnabled)
    {
        if (!multiEnabled)
        {
            for (String subject : subjectKeys)
            {
                ListPreference preference = new ListPreference(context);
                preference.setKey(String.format("filter_enabled_%s", subject));
                preference.setTitle(subject);

                ArrayList<String> _options = new ArrayList<>(subjectMap.get(subject));
                _options.add(0, SHOW_ALL);
                _options.add(1, SHOW_NOTHING);
                String[] options = new String[_options.size()];
                options = _options.toArray(options);

                preference.setDefaultValue(options[0]);
                preference.setEntries(options);
                preference.setEntryValues(options);
                preference.setOnPreferenceChangeListener((pref, newValue) ->
                {
                    showValue(pref, newValue);
                    return true;
                });
                screen.addPreference(preference);
                showValue(preference);
                preference.setDependency(getString(R.string.saved_filter_enabled));
            }
        }
        else
        {
            for (String subject : subjectKeys)
            {
                MultiSelectListPreference preference = new MultiSelectListPreference(context);
                preference.setKey(String.format("filter_enabled_%s", subject));
                preference.setTitle(subject);

                List<String> _options = subjectMap.get(subject);
                String[] options = new String[_options.size()];
                options = _options.toArray(options);

                preference.setDefaultValue(options[0]);
                preference.setEntries(options);
                preference.setEntryValues(options);
                preference.setOnPreferenceChangeListener((pref, newValues) ->
                {
                    showValue(pref, newValues);
                    return true;
                });
                screen.addPreference(preference);
                showValue(preference);
                preference.setDependency(getString(R.string.saved_filter_enabled));
            }
        }
    }

    private void changeFilterSettings(boolean toMulti)
    {
        MainActivity.registerPreferencesChanges = false;
        SharedPreferences.Editor editor = preferences.edit();

        for (String subject : subjectKeys)
        {
            String key = String.format("filter_enabled_%s", subject);
            if (toMulti)
            {
                String oldValue = preferences.getString(key, null);
                HashSet<String> newValues = new HashSet<>();
                if (oldValue != null && !oldValue.equals(SHOW_NOTHING))
                {
                    if (oldValue.equals(SHOW_ALL))
                    {
                        newValues.addAll(subjectMap.get(subject));
                    }
                    else
                    {
                        newValues.add(oldValue);
                    }
                }

                editor.remove(key);
                editor.putStringSet(key, newValues);
            }
            else
            {
                Set<String> oldValues = preferences.getStringSet(key, null);
                String newValue = null;
                if (oldValues != null)
                {
                    if (oldValues.isEmpty())
                    {
                        newValue = SHOW_NOTHING;
                    }
                    else if (oldValues.size() == subjectMap.get(subject).size())
                    {
                        newValue = SHOW_ALL;
                    }
                    else
                    {
                        newValue = oldValues.iterator().next();
                    }
                }

                editor.remove(key);
                editor.putString(key, newValue);
            }
        }
        
        editor.apply();
        MainActivity.registerPreferencesChanges = true;
    }

    private void deleteFilterPreferences()
    {
        for (String key : subjectKeys)
        {
            Preference preference = screen.findPreference((String.format("filter_enabled_%s", key)));
            screen.removePreference(preference);
        }
    }

    private void showValue(ListPreference preference)
    {
        showValue(preference, preferences.getString(preference.getKey(), null));
    }

    private void showValue(MultiSelectListPreference preference)
    {
        showValue(preference, preferences.getStringSet(preference.getKey(), null));
    }

    private void showValue(Preference preference, Object value)
    {
        if (value != null)
        {
            if (preference instanceof ListPreference)
            {
                preference.setSummary((String) value);
            }
            else if (preference instanceof MultiSelectListPreference)
            {
                Set<String> _values = (Set<String>) value;
                String[] values = new String[_values.size()];
                values = _values.toArray(values);

                Arrays.sort(values);
                if (values.length == subjectMap.get(preference.getTitle()).size())
                {
                    preference.setSummary(SHOW_ALL);
                }
                else if (values.length == 0)
                {
                    preference.setSummary(SHOW_NOTHING);
                }
                else
                {
                    preference.setSummary(Utils.join(" | ", values));
                }
            }
        }
    }

    private void addAboutTheAppPreferences()
    {
        PreferenceCategory category = new PreferenceCategory(context);
        category.setTitle(R.string.pref_category_about_the_app);
        category.setOrder(0x1000);
        screen.addPreference(category);

        Preference changelog = new Preference(context);
        changelog.setTitle(R.string.pref_title_changelog);
        changelog.setOrder(0x1001);
        changelog.setOnPreferenceClickListener(preference ->
        {
            Utils.createChangelog(context).show();
            return true;
        });
        screen.addPreference(changelog);

        Preference info = new Preference(context);
        info.setSelectable(false);
        info.setTitle(R.string.pref_title_info);
        info.setSummary(Utils.createInfoText(context));
        info.setOrder(0x1002);
        screen.addPreference(info);
    }
}
