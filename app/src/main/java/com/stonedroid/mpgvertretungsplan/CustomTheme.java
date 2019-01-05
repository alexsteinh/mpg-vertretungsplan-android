package com.stonedroid.mpgvertretungsplan;

import android.content.Context;
import android.graphics.Color;

public abstract class CustomTheme
{
    protected static final int DEFAULT_GREY = Color.parseColor("#f3f3f3");

    protected Context context;

    public CustomTheme(Context context)
    {
        this.context = context;
    }

    public void apply()
    {
        context.setTheme(getResId());
    }

    public abstract String getName();

    public abstract int getTextColor();
    public abstract int getImportantTextColor();
    public abstract int getTabTextColor();
    public abstract int getTabRippleColor();
    public abstract int getCardColor();
    public abstract int getLayoutColor();
    public abstract int getIndicatorColor();

    public abstract int getResId();

    public abstract boolean isLight();
}
