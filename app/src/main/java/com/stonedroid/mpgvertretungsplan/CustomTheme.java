package com.stonedroid.mpgvertretungsplan;

import android.app.Activity;
import android.app.ActivityManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.view.View;

public abstract class CustomTheme
{
    protected static final int DEFAULT_GREY = Color.parseColor("#f3f3f3");

    protected Activity context;

    public CustomTheme(Activity context)
    {
        this.context = context;
    }

    public void apply()
    {
        context.setTheme(getResId());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                int flags = isLight()
                        ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        : 0;

                context.getWindow().getDecorView().setSystemUiVisibility(flags);
            }

            if (Build.VERSION.SDK_INT >= 28)
            {
                context.setTaskDescription(new ActivityManager.TaskDescription(null,
                        R.mipmap.ic_launcher_round,
                        Utils.getThemePrimaryColor(context)));
            }
            else
            {
                Bitmap bmp = BitmapFactory.decodeResource(context.getResources(),
                        Build.VERSION.SDK_INT >= 25 ? R.mipmap.ic_launcher_round : R.mipmap.ic_launcher);

                context.setTaskDescription(new ActivityManager.TaskDescription(null, bmp,
                        Utils.getThemePrimaryColor(context)));

                bmp.recycle();
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

    public abstract int getResId();

    public abstract boolean isLight();
}
