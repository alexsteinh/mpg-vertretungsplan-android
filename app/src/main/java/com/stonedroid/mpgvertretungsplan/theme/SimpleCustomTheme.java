package com.stonedroid.mpgvertretungsplan.theme;

import android.app.Activity;
import android.graphics.Color;
import com.stonedroid.mpgvertretungsplan.Utils;

public abstract class SimpleCustomTheme extends CustomTheme {
    public SimpleCustomTheme(Activity context) {
        super(context);
    }

    @Override
    public int getTextColor() {
        return isLight() ? Color.WHITE : Color.BLACK;
    }

    @Override
    public int getImportantTextColor() {
        return Color.RED;
    }

    @Override
    public int getTabTextColor() {
        return isLight() ? Color.BLACK : Color.WHITE;
    }

    @Override
    public int getTabRippleColor() {
        return (isLight() ? Color.BLACK : Color.WHITE) - (224 << 24);
    }

    @Override
    public int getCardColor() {
        return isLight() ? Color.BLACK : Color.WHITE;
    }

    @Override
    public int getLayoutColor() {
        return Utils.getThemePrimaryColor(context);
    }

    @Override
    public int getIndicatorColor() {
        return isLight() ? Color.BLACK : Color.WHITE;
    }

    @Override
    public int getActionTextColor() {
        return Utils.getThemeAccentColor(context);
    }
}
