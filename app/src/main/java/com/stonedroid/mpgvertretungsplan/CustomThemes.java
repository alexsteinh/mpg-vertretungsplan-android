package com.stonedroid.mpgvertretungsplan;

import android.app.Activity;
import android.graphics.Color;
import androidx.preference.PreferenceManager;

import java.lang.reflect.Constructor;

public final class CustomThemes
{
    private static CustomTheme currentTheme = null;

    public static CustomTheme changeTheme(Activity context, boolean withActionBar)
    {
        CustomTheme theme = _changeTheme(context);
        currentTheme = theme;
        theme.apply(withActionBar);
        return theme;
    }

    public static CustomTheme _changeTheme(Activity context)
    {
        String name = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.saved_theme), "Orange")
                .replaceAll(" ", "_");

        Class<?> theme = null;
        Class<?>[] themes = CustomThemes.class.getClasses();

        for (Class<?> possible : themes)
        {
            if (possible.getSimpleName().equals(name.concat("Theme")))
            {
                theme = possible;
                break;
            }
        }

        if (theme == null)
        {
            return new OrangeTheme(context);
        }

        try
        {
            Constructor<?> ctor = theme.getConstructor(Activity.class);
            return (CustomTheme) ctor.newInstance(context);
        }
        catch (Exception e)
        {
            // Should NEVER happen
            return new OrangeTheme(context);
        }
    }

    public static String[] getSimpleNames()
    {
        Class<?>[] themes = CustomThemes.class.getClasses();
        String[] names = new String[themes.length];

        for (int i = 0; i < names.length; i++)
        {
            String name = themes[names.length - i - 1].getSimpleName().replaceAll("_", " ");
            names[i] = name.substring(0, name.lastIndexOf("T"));
        }

        return names;
    }

    public static CustomTheme getCurrentTheme()
    {
        return currentTheme;
    }

    public static class OrangeTheme extends SimpleCustomTheme
    {
        public OrangeTheme(Activity context)
        {
            super(context);
        }

        @Override
        public String getName()
        {
            return OrangeTheme.class.getSimpleName();
        }

        @Override
        public int getResId()
        {
            return R.style.OrangeTheme;
        }

        @Override
        public int getResIdNoActionBar()
        {
            return R.style.OrangeTheme_no_action_bar;
        }

        @Override
        public boolean isLight()
        {
            return false;
        }
    }

    public static class DarkTheme extends SimpleCustomTheme
    {
        public DarkTheme(Activity context)
        {
            super(context);
        }

        @Override
        public String getName()
        {
            return DarkTheme.class.getSimpleName();
        }

        @Override
        public int getCardColor()
        {
            return Color.parseColor("#222222");
        }

        @Override
        public int getTextColor()
        {
            return Color.WHITE;
        }

        @Override
        public int getActionTextColor()
        {
            return Color.WHITE;
        }

        @Override
        public int getResId()
        {
            return R.style.DarkTheme;
        }

        @Override
        public int getResIdNoActionBar()
        {
            return R.style.DarkTheme_no_action_bar;
        }

        @Override
        public boolean isLight()
        {
            return false;
        }
    }

    public static class LightTheme extends SimpleCustomTheme
    {
        public LightTheme(Activity context)
        {
            super(context);
        }

        @Override
        public String getName()
        {
            return LightTheme.class.getSimpleName();
        }

        @Override
        public int getCardColor()
        {
            return Color.WHITE;
        }

        @Override
        public int getLayoutColor()
        {
            return DEFAULT_GREY;
        }

        @Override
        public int getTextColor()
        {
            return Color.BLACK;
        }

        @Override
        public int getActionTextColor()
        {
            return Color.WHITE;
        }

        @Override
        public int getResId()
        {
            return R.style.LightTheme;
        }

        @Override
        public int getResIdNoActionBar()
        {
            return R.style.LightTheme_no_action_bar;
        }

        @Override
        public boolean isLight()
        {
            return true;
        }
    }

    public static class BlueTheme extends SimpleCustomTheme
    {
        public BlueTheme(Activity context)
        {
            super(context);
        }

        @Override
        public String getName()
        {
            return BlueTheme.class.getSimpleName();
        }

        @Override
        public int getResId()
        {
            return R.style.BlueTheme;
        }

        @Override
        public int getResIdNoActionBar()
        {
            return R.style.BlueTheme_no_action_bar;
        }

        @Override
        public boolean isLight()
        {
            return false;
        }
    }

    public static class GreenTheme extends SimpleCustomTheme
    {
        public GreenTheme(Activity context)
        {
            super(context);
        }

        @Override
        public String getName()
        {
            return GreenTheme.class.getSimpleName();
        }

        @Override
        public int getResId()
        {
            return R.style.GreenTheme;
        }

        @Override
        public int getResIdNoActionBar()
        {
            return R.style.GreenTheme_no_action_bar;
        }

        @Override
        public boolean isLight()
        {
            return false;
        }
    }

    public static class PinkTheme extends SimpleCustomTheme
    {
        public PinkTheme(Activity context)
        {
            super(context);
        }

        @Override
        public int getCardColor()
        {
            return Color.WHITE;
        }

        @Override
        public int getTextColor()
        {
            return Color.BLACK;
        }

        @Override
        public String getName()
        {
            return PinkTheme.class.getSimpleName();
        }

        @Override
        public int getResId()
        {
            return R.style.PinkTheme;
        }

        @Override
        public int getResIdNoActionBar()
        {
            return R.style.PinkTheme_no_action_bar;
        }

        @Override
        public boolean isLight()
        {
            return true;
        }
    }
}
