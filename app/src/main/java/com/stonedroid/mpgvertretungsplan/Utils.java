package com.stonedroid.mpgvertretungsplan;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import de.stonedroid.vertretungsplan.Grade;
import de.stonedroid.vertretungsplan.Replacement;
import de.stonedroid.vertretungsplan.ReplacementTable;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
            case "Weiß":
                context.setTheme(R.style.WhiteTheme);
                break;
            case "Schwarz":
                context.setTheme(R.style.BlackTheme);
        }

        return themeName;
    }

    public static List<Replacement> filterReplacements(Context context, ReplacementTable table)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        List<Replacement> replacements = table.getReplacements();
        Grade grade = table.getGrade();

        if (grade == null || (!grade.toString().equals("11") && !grade.toString().equals("12")))
        {
            return replacements;
        }

        // Load all subject and synonyms
        Map<String, String> synonyms = Subject.getSynonyms();

        ArrayList<Replacement> filteredReplacements = new ArrayList<>();

        for (Replacement replacement : replacements)
        {
            try
            {
                String oldSubject = replacement.getOldSubject();
                // Is it a synonym? Some subjects from grade 11 have other names then in 12
                // Subject::getSynonyms contains all of them, so we can replace the synonyms with
                // the "real" subject names, which the filter can process
                String o_oldSubject = oldSubject;
                oldSubject = synonyms.get(oldSubject);
                if (oldSubject == null)
                {
                    // Subject is not a synonym
                    oldSubject = o_oldSubject;
                }

                String subjectName = Subject.getSubjectName(oldSubject);

                // Look at user preferences what to do next
                String course = prefs.getString("filter_enabled_" + subjectName, null);

                if (course == null || course.equals("Alle anzeigen") || course.contains(oldSubject))
                {
                    // Let it through the filter
                    filteredReplacements.add(replacement);
                }
            }
            catch (Exception e)
            {
                // Replacement's subject is not listed -> let it through the filter
                filteredReplacements.add(replacement);
            }
        }

        return filteredReplacements;
    }

    public static <T> boolean containsAny(List<T> list1, List<T> list2)
    {
        for (T t1 : list1)
        {
            for (T t2 : list2)
            {
                if (t1.equals(t2))
                {
                    return true;
                }
            }
        }

        return false;
    }
}
