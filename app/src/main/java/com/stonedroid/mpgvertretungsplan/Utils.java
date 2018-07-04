package com.stonedroid.mpgvertretungsplan;

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
}
