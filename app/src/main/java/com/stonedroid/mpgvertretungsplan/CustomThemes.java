package com.stonedroid.mpgvertretungsplan;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.preference.PreferenceManager;

import java.lang.reflect.Constructor;

public final class CustomThemes
{
    private static CustomTheme currentTheme = null;

    public static CustomTheme changeTheme(Context context)
    {
        CustomTheme theme = _changeTheme(context);
        currentTheme = theme;
        theme.apply();
        return theme;
    }

    public static CustomTheme _changeTheme(Context context)
    {
        String name = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.saved_theme), "Orange");

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
            Constructor<?> ctor = theme.getConstructor(Context.class);
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
            String name = themes[names.length - i - 1].getSimpleName();
            names[i] = name.substring(0, name.indexOf("T"));
        }

        return names;
    }

    public static CustomTheme getCurrentTheme()
    {
        return currentTheme;
    }

    public static class OrangeTheme extends CustomTheme
    {
        public OrangeTheme(Context context)
        {
            super(context);
        }

        @Override
        public String getName()
        {
            return OrangeTheme.class.getSimpleName();
        }

        @Override
        public int getTextColor()
        {
            return Color.BLACK;
        }

        @Override
        public int getImportantTextColor()
        {
            return Color.RED;
        }

        @Override
        public int getTabTextColor()
        {
            return Color.WHITE;
        }

        @Override
        public int getTabRippleColor()
        {
            return Color.WHITE - (224 << 24);
        }

        @Override
        public int getCardColor()
        {
            return Color.WHITE;
        }

        @Override
        public int getLayoutColor()
        {
            return Utils.getThemePrimaryColor(context);
        }

        @Override
        public int getIndicatorColor()
        {
            return Color.WHITE;
        }

        @Override
        public int getResId()
        {
            return R.style.OrangeTheme;
        }
    }

    public static class DarkTheme extends CustomTheme
    {
        public DarkTheme(Context context)
        {
            super(context);
        }

        @Override
        public String getName()
        {
            return DarkTheme.class.getSimpleName();
        }

        @Override
        public int getTextColor()
        {
            return Color.WHITE;
        }

        @Override
        public int getImportantTextColor()
        {
            return Color.RED;
        }

        @Override
        public int getTabTextColor()
        {
            return Color.WHITE;
        }

        @Override
        public int getTabRippleColor()
        {
            return Color.WHITE - (224 << 24);
        }

        @Override
        public int getCardColor()
        {
            return Color.parseColor("#222222");
        }

        @Override
        public int getLayoutColor()
        {
            return Color.BLACK;
        }

        @Override
        public int getIndicatorColor()
        {
            return Color.WHITE;
        }

        @Override
        public int getResId()
        {
            return R.style.DarkTheme;
        }
    }

    public static class LightTheme extends CustomTheme
    {
        public LightTheme(Context context)
        {
            super(context);
        }

        @Override
        public String getName()
        {
            return LightTheme.class.getSimpleName();
        }

        @Override
        public int getTextColor()
        {
            return Color.BLACK;
        }

        @Override
        public int getImportantTextColor()
        {
            return Color.RED;
        }

        @Override
        public int getTabTextColor()
        {
            return Color.BLACK;
        }

        @Override
        public int getTabRippleColor()
        {
            return Color.BLACK - (224 << 24);
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
        public int getIndicatorColor()
        {
            return Color.BLACK;
        }

        @Override
        public int getResId()
        {
            return R.style.LightTheme;
        }
    }
}
