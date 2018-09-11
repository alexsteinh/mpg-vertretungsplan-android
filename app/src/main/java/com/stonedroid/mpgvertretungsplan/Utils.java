package com.stonedroid.mpgvertretungsplan;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.*;

public class Utils
{
    // Saves an object, which implements the Serializable interface.
    public static void saveObject(Object obj, String file) throws IOException
    {
        FileOutputStream fos = new FileOutputStream(file);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        // Save object
        oos.writeObject(obj);
        oos.close();
    }

    // Loads an object, which was serialized before.
    public static Object loadObject(String file) throws IOException, ClassNotFoundException
    {
        FileInputStream fis = new FileInputStream(file);
        ObjectInputStream ois = new ObjectInputStream(fis);
        // Load object
        Object obj = ois.readObject();
        ois.close();
        return obj;
    }

    public static String setCustomTheme(Context context)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String themeName = prefs.getString(context.getString(R.string.saved_theme), "Orange");
        switch (themeName)
        {
            case "Orange":
                context.setTheme(R.style.OrangeTheme);
                break;
            case "Light":
                context.setTheme(R.style.LightTheme);
        }

        return themeName;
    }
}
