package com.stonedroid.mpgvertretungsplan.settings;

import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.*;
import com.stonedroid.mpgvertretungsplan.MainActivity;
import com.stonedroid.mpgvertretungsplan.R;
import com.stonedroid.mpgvertretungsplan.Utils;
import com.stonedroid.mpgvertretungsplan.theme.CustomThemes;
import de.stonedroid.vertretungsplan.Grade;

import java.util.List;

public class SettingsFragment extends PreferenceFragmentCompat {
    public static final String TAG = SettingsFragment.class.getSimpleName();

    private PreferenceScreen screen;
    private Context context;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        MainActivity.registerPreferencesChanges = false;

        context = getActivity();

        // Load "pseudo" preferences (small hack to dynamically add preferences)
        addPreferencesFromResource(R.xml.preferences);

        screen = getPreferenceScreen();

        addGradePreference();
        addCustomizationPreferences();
        addAboutTheAppPreferences();

        MainActivity.registerPreferencesChanges = true;
    }

    // Add the grade preference to the fragment
    private void addGradePreference() {
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
        CharSequence[] charNames = new CharSequence[names.size()];
        charNames = names.toArray(charNames);
        // Set entries/entry values
        gradePref.setEntries(charNames);
        gradePref.setEntryValues(charNames);
        // Add listener
        gradePref.setOnPreferenceChangeListener((preference, newValue) ->
        {
            showValue((ListPreference) preference, (String) newValue);
            return true;
        });
        // Add preference to fragment
        screen.addPreference(gradePref);
        showValue(gradePref);
    }

    private void addCustomizationPreferences() {
        // Add new preference category
        PreferenceCategory category = new PreferenceCategory(context);
        category.setTitle(getString(R.string.pref_category_customizing));
        screen.addPreference(category);

        // Add Theme options
        {
            ListPreference pref = new ListPreference(context);
            CharSequence[] themeNames = CustomThemes.getSimpleNames();
            pref.setTitle(getString(R.string.pref_title_theme));
            pref.setKey(getString(R.string.saved_theme));
            pref.setEntries(themeNames);
            pref.setEntryValues(themeNames);
            pref.setDefaultValue(themeNames[0]);
            pref.setOnPreferenceChangeListener((preference, newValue) ->
            {
                showValue((ListPreference) preference, (String) newValue);
                FragmentActivity parent = getActivity();
                if (parent != null) {
                    parent.recreate();
                }
                return true;
            });
            // Add preference to screen
            screen.addPreference(pref);
            showValue(pref);
        }

        // Add round corners option
        {
            CheckBoxPreference pref = new CheckBoxPreference(context);
            pref.setTitle(getString(R.string.pref_title_rounded_corners));
            pref.setKey(getString(R.string.saved_rounded_corners));
            pref.setDefaultValue(true);
            screen.addPreference(pref);
        }

        // Add swipe refresh option
        {
            CheckBoxPreference pref = new CheckBoxPreference(context);
            pref.setTitle(getString(R.string.pref_title_swipe_refresh_enabled));
            pref.setKey(getString(R.string.saved_swipe_refresh_enabled));
            pref.setDefaultValue(true);
            screen.addPreference(pref);
        }
    }

    private void deleteFilterPreferences() {
        int count = screen.getPreferenceCount();

        for (int i = 0; i < count; i++) {
            Preference preference = screen.getPreference(i);
            if (preference.getKey().startsWith("filter_enabled_")) {
                screen.removePreference(preference);
            }
        }
    }

    private void showValue(ListPreference preference) {
        showValue(preference, preference.getValue());
    }

    private void showValue(ListPreference preference, String value) {
        preference.setSummary(value);
    }

    private void addAboutTheAppPreferences() {
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
