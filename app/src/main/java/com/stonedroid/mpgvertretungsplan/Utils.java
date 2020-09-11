package com.stonedroid.mpgvertretungsplan;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import androidx.core.content.res.ResourcesCompat;
import androidx.appcompat.app.AlertDialog;
import android.util.TypedValue;
import android.widget.TextView;
import de.stonedroid.vertretungsplan.Grade;
import de.stonedroid.vertretungsplan.Replacement;
import de.stonedroid.vertretungsplan.ReplacementTable;

import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Utils {
    // Saves an object, which implements the Serializable interface.
    public static void saveObject(Object obj, String file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        // Save object
        oos.writeObject(obj);
        oos.close();
    }

    // Loads an object, which was serialized before.
    public static Object loadObject(String file) throws IOException, ClassNotFoundException {
        FileInputStream fis = new FileInputStream(file);
        ObjectInputStream ois = new ObjectInputStream(fis);
        // Load object
        Object obj = ois.readObject();
        ois.close();
        return obj;
    }


    public static boolean containsAny(List<? extends Comparable> list1, List<? extends Comparable> list2) {
        for (Comparable comp1 : list1) {
            for (Comparable comp2 : list2) {
                if (comp1.equals(comp2)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static String getReplacementSummary(Replacement replacement) {
        String summary;
        if (replacement.getText().contains("fällt aus")) {
            if (replacement.getPeriod().length() == 1) {
                summary = "fällt Fach %s in der %s. Stunde aus";
            } else {
                summary = "fällt Fach %s in den Stunden %s aus";
            }

            summary = String.format(summary, replacement.getOldSubject(),
                    replacement.getPeriod());
        } else if (replacement.getText().equals("Raumänderung")) {
            if (replacement.getPeriod().length() == 1) {
                summary = "Fach %s in der %s. Stunde im Raum %s";
            } else {
                summary = "Fach %s in den Stunden %s im Raum %s";
            }

            summary = String.format(summary, replacement.getSubject(),
                    replacement.getPeriod(), replacement.getRoom());
        } else {
            if (replacement.getPeriod().length() == 1) {
                summary = "Fach %s statt %s in der %s. Stunde im Raum %s, da %s";
            } else {
                summary = "Fach %s statt %s in den Stunden %s im Raum %s, da %s";
            }

            summary = String.format(summary, replacement.getSubject(),
                    replacement.getOldSubject(), replacement.getPeriod(), replacement.getRoom(),
                    replacement.getText());
        }

        summary = String.format("Am %s (%s) ", replacement.getDate(), replacement.getDay()).concat(summary);

        if ((!summary.endsWith(".")) && (!summary.endsWith("!"))) {
            if (summary.endsWith("?")) {
                return summary;
            }

            return summary.concat(".");
        }

        return summary;
    }

    private static int getAttributeData(Context context, int attribute) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attribute, value, true);
        return value.data;
    }

    public static int getThemePrimaryColor(Context context) {
        return getAttributeData(context, R.attr.colorPrimary);
    }

    public static int getThemePrimaryDarkColor(Context context) {
        return getAttributeData(context, R.attr.colorPrimaryDark);
    }

    public static int getThemeAccentColor(Context context) {
        return getAttributeData(context, R.attr.colorAccent);
    }

    public static String join(CharSequence delimiter, Iterable<? extends CharSequence> elements) {
        StringBuilder sb = new StringBuilder();
        Iterator<? extends CharSequence> iterator = elements.iterator();

        while (iterator.hasNext()) {
            sb.append(iterator.next());

            if (iterator.hasNext()) {
                sb.append(delimiter);
            }
        }

        return sb.toString();
    }

    public static String join(CharSequence delimiter, CharSequence[] elements) {
        StringBuilder sb = new StringBuilder();
        int size = elements.length;

        for (int i = 0; i < size; i++) {
            sb.append(elements[i]);

            if (i < (size - 1)) {
                sb.append(delimiter);
            }
        }

        return sb.toString();
    }

    public static <T extends Comparable<T>> T max(T[] array) {
        T max = null;

        for (T t : array) {
            if (max == null) {
                max = t;
            } else if (t.compareTo(max) > 0) {
                max = t;
            }
        }

        return max;
    }

    private static PackageInfo getPackageInfo(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String getVersionName(Context context) {
        return getPackageInfo(context).versionName;
    }

    public static int getVersionCode(Context context) {
        return getPackageInfo(context).versionCode;
    }

    public static AlertDialog createChangelog(Context context) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.changelog_title))
                .setMessage(context.getString(R.string.changelog_message))
                .setPositiveButton("OK", null)
                .create();

        dialog.setOnShowListener(d ->
        {
            TextView text = dialog.findViewById(android.R.id.message);
            if (text != null) {
                toMonospacedFont(context, text);
            }
        });

        return dialog;
    }

    public static void toMonospacedFont(Context context, TextView textView) {
        textView.setTextSize(14);
        Typeface face = ResourcesCompat.getFont(context, R.font.noto_mono_regular);
        textView.setTypeface(face);
    }

    public static String createInfoText(Context context) {
        return String.format("Version %s\n" +
                        "Copyright © %s Alexander Steinhauer",
                getVersionName(context),
                Calendar.getInstance().get(Calendar.YEAR));
    }

    public static List<Integer> indexesOf(String text, String word) {
        List<Integer> indexes = new ArrayList<>();
        int offset = 0;

        while (text.contains(word)) {
            int start = text.indexOf(word);
            int end = start + word.length();

            indexes.add(start + offset);
            text = text.substring(0, start).concat(text.substring(end));

            offset += word.length();
        }

        return indexes;
    }

    public static boolean hasFlag(int value, int flag) {
        return (value & flag) == flag;
    }

    public static int addFlag(int value, int flag) {
        return value | flag;
    }

    public static int removeFlag(int value, int flag) {
        return flag & ~value;
    }
}
