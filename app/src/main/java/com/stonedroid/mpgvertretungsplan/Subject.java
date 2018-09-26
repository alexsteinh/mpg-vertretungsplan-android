package com.stonedroid.mpgvertretungsplan;

import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Subject
{
    private static final Map<String, List<String>> subjects = ImmutableMap.<String, List<String>>builder()
            .put("Basiskurs Mathematik", Arrays.asList("bm1", "bm2"))
            .put("Biologie", Arrays.asList("bio1", "bio2", "bio3", "Bio1", "Bio2"))
            .put("Chemie", Arrays.asList("ch1", "ch2", "ch3", "Ch1", "Ch2"))
            .put("Darstellende Geometrie", Arrays.asList("dg1"))
            .put("Deutsch", Arrays.asList("D1", "D2", "D3", "D4", "D5"))
            .put("Englisch", Arrays.asList("E1", "E2", "E3", "E4", "E5"))
            .put("Ethik", Arrays.asList("eth1"))
            .put("Französisch", Arrays.asList("F1"))
            .put("Gemeinschaftskunde", Arrays.asList("gk1", "gk2", "gk3", "gk4", "gk5", "Gk1"))
            .put("Geographie", Arrays.asList("geo1", "geo2", "geo3", "geo4", "Geo1"))
            .put("Geschichte", Arrays.asList("g1", "g2", "g3", "g4", "g5", "G1"))
            .put("Informatik", Arrays.asList("inf1", "inf2"))
            .put("Italienisch", Arrays.asList("Ital1"))
            .put("Kunst", Arrays.asList("bk1", "bk2", "bk3", "Bk1"))
            .put("Latein", Arrays.asList("L1"))
            .put("Mathematik", Arrays.asList("M1", "M2", "M3", "M4", "M5"))
            .put("Musik", Arrays.asList("mu1", "mu2", "Mu1"))
            .put("Philosophie", Arrays.asList("phil"))
            .put("Physik", Arrays.asList("ph1", "ph2", "Ph1"))
            .put("Psychologie", Arrays.asList("psy1", "psy2", "psy3"))
            .put("Religion", Arrays.asList("rev1", "rev2", "rev3", "rrk1"))
            .put("Sport", Arrays.asList("sp1", "sp2", "sp3", "sp4", "sp5", "Sp1"))
            .put("Vertiefungskurs Mathe", Arrays.asList("vma1", "vma2"))
            .put("Wirtschaft", Arrays.asList("Wi1", "Wi2"))
            .put("Wirtschaftsenglisch", Arrays.asList("we1"))
            .build();

    private static final Map<String, String> synonyms = ImmutableMap.<String, String>builder()
            .put("L", "L1")
            .put("mu", "mu1")
            .put("G", "G1")
            .put("GK", "GK1")
            .put("SP", "Sp1")
            .put("BK", "Bk1")
            .put("MU", "Mu1")
            .put("GEO", "Geo1")
            .put("F", "F1")
            .put("WI1", "Wi1")
            .put("WI2", "Wi2")
            .put("BIO1", "Bio1")
            .put("BIO2", "Bio2")
            .put("CH", "Ch1")
            .put("PH", "Ph1")
            .put("ITAL", "Ital1")
            .put("rrk", "rrk1")
            .put("eth", "eth1")
            .build();

    public static Map<String, List<String>> getAllSubjects()
    {
        return subjects;
    }

    public static Map<String, String> getSynonyms()
    {
        return synonyms;
    }

    public static List<String> getAllCourses()
    {
        ArrayList<String> coursesList = new ArrayList<>();
        Collection<List<String>> _coursesList = subjects.values();

        for (List<String> courses : _coursesList)
        {
            coursesList.addAll(courses);
        }

        return coursesList;
    }

    public static String getSubjectName(String course) throws Exception
    {
        Collection<List<String>> _courses = subjects.values();
        List<String>[] courses = new List[_courses.size()];
        courses = _courses.toArray(courses);

        Set<String> _names = subjects.keySet();
        String[] names = new String[_names.size()];
        names = _names.toArray(names);

        for (int i = 0; i < courses.length; i++)
        {
            if (courses[i].contains(course))
            {
                return names[i];
            }
        }

        throw new Exception("Course \"" + course + "\" does not exist");
    }
}
