package com.stonedroid.mpgvertretungsplan;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.PreferenceManager;

// eXtendedListPreference
// Shows the current value automatically below the title of the preference
public class XListPreference extends ListPreference
{
    private Context context;
    private OnPreferenceChangeListener userOnPreferenceChangeListener = null;
    private OnPreferenceChangeListener internOnPreferenceChangeListener = null;

    public XListPreference(Context context)
    {
        super(context);
        this.context = context;
    }

    @Override
    public void setOnPreferenceChangeListener(OnPreferenceChangeListener onPreferenceChangeListener)
    {
        if (internOnPreferenceChangeListener == null)
        {
            userOnPreferenceChangeListener = onPreferenceChangeListener;
            super.setOnPreferenceChangeListener(userOnPreferenceChangeListener);
        }
        else
        {
            if (!onPreferenceChangeListener.equals(internOnPreferenceChangeListener))
            {
                userOnPreferenceChangeListener = onPreferenceChangeListener;
            }

            super.setOnPreferenceChangeListener(internOnPreferenceChangeListener);
        }
    }

    public void showValue()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String currentValue = prefs.getString(getKey(), null);
        if (currentValue != null)
        {
            setSummary(currentValue);
        }

        internOnPreferenceChangeListener = (preference, newValue) ->
        {
            if (userOnPreferenceChangeListener != null)
            {
                userOnPreferenceChangeListener.onPreferenceChange(preference, newValue);
            }

            setSummary((String) newValue);
            return true;
        };

        setOnPreferenceChangeListener(internOnPreferenceChangeListener);
    }
}
