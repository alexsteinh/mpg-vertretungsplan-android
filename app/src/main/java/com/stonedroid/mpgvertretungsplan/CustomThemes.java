package com.stonedroid.mpgvertretungsplan;

import android.app.Activity;
import android.graphics.Color;
import android.support.v7.preference.PreferenceManager;

import java.lang.reflect.Constructor;

public final class CustomThemes
{
    private static CustomTheme currentTheme = null;

    public static CustomTheme changeTheme(Activity context)
    {
        CustomTheme theme = _changeTheme(context);
        currentTheme = theme;
        theme.apply();
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
        public int getResId()
        {
            return R.style.DarkTheme;
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
        public int getResId()
        {
            return R.style.LightTheme;
        }

        @Override
        public boolean isLight()
        {
            return true;
        }
    }

    public static class Royal_BlueTheme extends SimpleCustomTheme
    {
        public Royal_BlueTheme(Activity context)
        {
            super(context);
        }

        @Override
        public String getName()
        {
            return Royal_BlueTheme.class.getSimpleName();
        }

        @Override
        public int getResId()
        {
            return R.style.Royal_BlueTheme;
        }

        @Override
        public boolean isLight()
        {
            return false;
        }
    }

    public static class Calm_GreenTheme extends SimpleCustomTheme
    {
        public Calm_GreenTheme(Activity context)
        {
            super(context);
        }

        @Override
        public String getName()
        {
            return Calm_GreenTheme.class.getSimpleName();
        }

        @Override
        public int getResId()
        {
            return R.style.Calm_GreenTheme;
        }

        @Override
        public boolean isLight()
        {
            return false;
        }
    }
}
