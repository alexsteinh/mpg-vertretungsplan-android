package com.stonedroid.mpgvertretungsplan;

import android.app.Activity;
import android.graphics.Color;

public abstract class SimpleCustomTheme extends CustomTheme
{
    public SimpleCustomTheme(Activity context)
    {
        super(context);
    }

    public abstract String getName();

    @Override
    public int getTextColor()
    {
        return isLight() ? Color.WHITE : Color.BLACK;
    }

    @Override
    public int getImportantTextColor()
    {
        return Color.RED;
    }

    @Override
    public int getTabTextColor()
    {
        return isLight() ? Color.BLACK : Color.WHITE;
    }

    @Override
    public int getTabRippleColor()
    {
        return (isLight() ? Color.BLACK : Color.WHITE) - (224 << 24);
    }

    @Override
    public int getCardColor()
    {
        return isLight() ? Color.BLACK : Color.WHITE;
    }

    @Override
    public int getLayoutColor()
    {
        return Utils.getThemePrimaryColor(context);
    }

    @Override
    public int getIndicatorColor()
    {
        return isLight() ? Color.BLACK : Color.WHITE;
    }

    public abstract int getResId();

    public abstract boolean isLight();
}
