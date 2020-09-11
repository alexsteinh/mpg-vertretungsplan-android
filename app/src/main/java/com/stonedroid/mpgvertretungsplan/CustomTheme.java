package com.stonedroid.mpgvertretungsplan;

import android.app.Activity;
import android.app.ActivityManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.view.View;

public abstract class CustomTheme {
    protected static final int DEFAULT_GREY = Color.parseColor("#f3f3f3");

    protected Activity context;

    public CustomTheme(Activity context) {
        this.context = context;
    }

    public void apply(boolean withActionBar) {
        context.setTheme(withActionBar ? getResId() : getResIdNoActionBar());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int flags = context.getWindow().getDecorView().getSystemUiVisibility();

            // Light status bar
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (isLight()) {
                    flags = Utils.addFlag(flags, View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                } else {
                    if (Utils.hasFlag(flags, View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)) {
                        flags = Utils.removeFlag(flags, View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                    }
                }
            }

            // Light navigation bar
            if (Build.VERSION.SDK_INT >= 26) {
                if (isLight()) {
                    flags = Utils.addFlag(flags, View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
                    context.getWindow().setNavigationBarColor(Color.WHITE);
                } else {
                    if (Utils.hasFlag(flags, View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)) {
                        flags = Utils.removeFlag(flags, View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
                    }

                    context.getWindow().setNavigationBarColor(Color.BLACK);
                }
            }

            // Task description in task manager
            if (Build.VERSION.SDK_INT >= 28) {
                context.setTaskDescription(new ActivityManager.TaskDescription(null,
                        R.mipmap.ic_launcher_round,
                        Utils.getThemePrimaryColor(context)));

                // Light navigation bar divider
                if (isLight()) {
                    context.getWindow().setNavigationBarDividerColor(Color.parseColor("#e0e0e0"));
                } else {
                    context.getWindow().setNavigationBarDividerColor(Color.BLACK);
                }
            } else {
                Bitmap bmp = BitmapFactory.decodeResource(context.getResources(),
                        Build.VERSION.SDK_INT >= 25 ? R.mipmap.ic_launcher_round : R.mipmap.ic_launcher);

                context.setTaskDescription(new ActivityManager.TaskDescription(null, bmp,
                        Utils.getThemePrimaryColor(context)));

                bmp.recycle();
            }

            context.getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }

    public abstract String getName();

    public abstract int getTextColor();

    public abstract int getImportantTextColor();

    public abstract int getTabTextColor();

    public abstract int getTabRippleColor();

    public abstract int getCardColor();

    public abstract int getLayoutColor();

    public abstract int getIndicatorColor();

    public abstract int getActionTextColor();

    public abstract int getResId();

    public abstract int getResIdNoActionBar();

    public abstract boolean isLight();
}
