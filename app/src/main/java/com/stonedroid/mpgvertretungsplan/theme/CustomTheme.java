package com.stonedroid.mpgvertretungsplan.theme;

import android.app.Activity;
import android.app.ActivityManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.view.WindowInsetsController;
import com.stonedroid.mpgvertretungsplan.R;
import com.stonedroid.mpgvertretungsplan.Utils;

public abstract class CustomTheme {
    protected static final int DEFAULT_GREY = Color.parseColor("#f3f3f3");

    protected Activity context;

    public CustomTheme(Activity context) {
        this.context = context;
    }

    public void apply(boolean withActionBar) {
        context.setTheme(withActionBar ? getResId() : getResIdNoActionBar());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            WindowInsetsController controller = context.getWindow().getDecorView().getWindowInsetsController();
            int flags = controller.getSystemBarsAppearance();

            // Light status bar
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (isLight()) {
                    controller.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                } else {
                    if (Utils.hasFlag(flags, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)) {
                        controller.setSystemBarsAppearance(0,
                                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                    }
                }
            }

            // Light navigation bar
            if (Build.VERSION.SDK_INT >= 26) {
                if (isLight()) {
                    controller.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                            WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
                    context.getWindow().setNavigationBarColor(Color.WHITE);
                } else {
                    if (Utils.hasFlag(flags, WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)) {
                        controller.setSystemBarsAppearance(0,
                                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
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
                int bmpRes = Build.VERSION.SDK_INT >= 25 ? R.mipmap.ic_launcher_round : R.mipmap.ic_launcher;
                context.setTaskDescription(new ActivityManager.TaskDescription(null, bmpRes,
                        Utils.getThemePrimaryColor(context)));
            }
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
