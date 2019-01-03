package com.stonedroid.mpgvertretungsplan;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.preference.PreferenceManager;

public class CustomTheme
{
    private static final int DEFAULT_GRAY = Color.parseColor("#f3f3f3");
    private static final String[] THEME_NAMES = new String[] {"Orange", "Light", "Dark"};

    private int textColor;
    private int importantTextColor;
    private int tabTextColor;
    private int cardColor;
    private int layoutColor;
    private int indicatorColor;

    private static class Builder
    {
        private int textColor;
        private int importantTextColor;
        private int tabTextColor;
        private int cardColor;
        private int layoutColor;
        private int indicatorColor;

        public Builder setTextColor(int textColor)
        {
            this.textColor = textColor;
            return this;
        }

        public Builder setImportantTextColor(int importantTextColor)
        {
            this.importantTextColor = importantTextColor;
            return this;
        }

        public Builder setTabTextColor(int tabTextColor)
        {
            this.tabTextColor = tabTextColor;
            return this;
        }

        public Builder setCardColor(int cardColor)
        {
            this.cardColor = cardColor;
            return this;
        }

        public Builder setLayoutColor(int layoutColor)
        {
            this.layoutColor = layoutColor;
            return this;
        }

        public Builder setIndicatorColor(int indicatorColor)
        {
            this.indicatorColor = indicatorColor;
            return this;
        }

        public CustomTheme build()
        {
            CustomTheme theme = new CustomTheme();
            theme.textColor = textColor;
            theme.importantTextColor = importantTextColor;
            theme.tabTextColor = tabTextColor;
            theme.cardColor = cardColor;
            theme.layoutColor = layoutColor;
            theme.indicatorColor = indicatorColor;
            return theme;
        }
    }

    public static CustomTheme changeTheme(Context context)
    {
        String theme = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.saved_theme), "Orange");
        switch (theme)
        {
            case "Orange":
                context.setTheme(R.style.OrangeTheme);
                return new Builder()
                        .setTextColor(Color.BLACK)
                        .setImportantTextColor(Color.RED)
                        .setTabTextColor(Color.WHITE)
                        .setCardColor(Color.WHITE)
                        .setLayoutColor(Utils.getThemePrimaryColor(context))
                        .setIndicatorColor(Color.WHITE)
                        .build();
            case "Dark":
                context.setTheme(R.style.DarkTheme);
                return new Builder()
                        .setTextColor(Color.WHITE)
                        .setImportantTextColor(Color.RED)
                        .setTabTextColor(Color.WHITE)
                        .setCardColor(Color.parseColor("#222222"))
                        .setLayoutColor(Color.BLACK)
                        .setIndicatorColor(Color.WHITE)
                        .build();
            case "Light":
                context.setTheme(R.style.LightTheme);
                return new Builder()
                        .setTextColor(Color.BLACK)
                        .setImportantTextColor(Color.RED)
                        .setTabTextColor(Color.BLACK)
                        .setCardColor(Color.WHITE)
                        .setLayoutColor(DEFAULT_GRAY)
                        .setIndicatorColor(Color.BLACK)
                        .build();
            default:
                // Default orange theme
                context.setTheme(R.style.OrangeTheme);
                return new Builder()
                        .setTextColor(Color.BLACK)
                        .setImportantTextColor(Color.RED)
                        .setTabTextColor(Color.WHITE)
                        .setCardColor(Color.WHITE)
                        .setLayoutColor(Utils.getThemePrimaryColor(context))
                        .setIndicatorColor(Color.WHITE)
                        .build();
        }
    }

    public static String[] getThemeNames()
    {
        return THEME_NAMES;
    }

    public int getTextColor()
    {
        return textColor;
    }

    public int getImportantTextColor()
    {
        return importantTextColor;
    }

    public int getTabTextColor()
    {
        return tabTextColor;
    }

    public int getCardColor()
    {
        return cardColor;
    }

    public int getLayoutColor()
    {
        return layoutColor;
    }

    public int getIndicatorColor()
    {
        return indicatorColor;
    }
}
