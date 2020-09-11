package com.stonedroid.mpgvertretungsplan;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.util.Log;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import de.stonedroid.vertretungsplan.Grade;
import de.stonedroid.vertretungsplan.Message;
import de.stonedroid.vertretungsplan.Replacement;
import de.stonedroid.vertretungsplan.ReplacementTable;
import de.stonedroid.vertretungsplan.WebException;

import java.util.ArrayList;

// Handles fetching the newest ReplacementTables
// and push notifications if there changes while comparing to the old one
public class NotificationWorker extends Worker
{
    public static final String TAG = NotificationWorker.class.getSimpleName();

    private static final String TABLE_1_BIN = "table1.dat";
    private static final String TABLE_2_BIN = "table2.dat";

    private static final String CHANNEL_ID = "0";

    private Context context;
    private SharedPreferences preferences;

    private ReplacementTable[] tables = new ReplacementTable[2];
    private ReplacementTable[] oldTables = new ReplacementTable[2];

    public NotificationWorker(Context context, WorkerParameters workerParams)
    {
        super(context, workerParams);
    }

    @Override
    public Result doWork()
    {
        Log.d(TAG,"Doing some work right now!" + " (ID = " + getId().toString() + ")");

        context = getApplicationContext();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean canDoOffline = preferences.getBoolean(context.getString(R.string.saved_offline_available), false);

        // Only work if all tables are ready and from the same grade
        if (canDoOffline && loadTablesFromStorage() && downloadTables())
        {
            // Get all replacements and messages
            ArrayList<Replacement> newReplacements = new ArrayList<>(), oldReplacements = new ArrayList<>();
            ArrayList<Message> newMessages = new ArrayList<>(), oldMessages = new ArrayList<>();

            for (int i = 0; i < tables.length; i++)
            {
                if (tables[i] != null)
                {
                    newReplacements.addAll(Utils.filterReplacements(context, tables[i]));
                    newMessages.addAll(tables[i].getMessages());
                }

                if (oldTables[i] != null)
                {
                    oldReplacements.addAll(Utils.filterReplacements(context, oldTables[i]));
                    oldMessages.addAll(oldTables[i].getMessages());
                }
            }

            int rCount = preferences.getInt(context.getString(R.string.saved_unseen_replacements), 0);
            int mCount = preferences.getInt(context.getString(R.string.saved_unseen_messages), 0);

            // Count all new replacements
            for (Replacement r1 : newReplacements)
            {
                if (!oldReplacements.contains(r1))
                {
                    // The replacement is new... add one to the new replacement counter
                    rCount++;
                }
            }

            // Count all new messages
            for (Message m1 : newMessages)
            {
                if (!oldMessages.contains(m1))
                {
                    // The message is new... add one to the new message counter
                    mCount++;
                }
            }

            String title = null;
            String message = null;

            if (rCount == 0 && mCount > 0)
            {
                // Only messages arrived
                title = mCount == 1 ? "Neue Nachricht" : "Neue Nachrichten";
                message = String.format("Es gibt %s neue %s", mCount,
                        mCount == 1 ? "Nachricht" : "Nachrichten");
            }
            else if (rCount > 0 && mCount == 0)
            {
                // Only replacements arrived
                title = rCount == 1 ? "Neue Vertretung" : "Neue Vertretungen";
                message = String.format("Es gibt %s neue %s", rCount,
                        rCount == 1 ? "Vertretung" : "Vertretungen");
            }
            else if (rCount > 0 && mCount > 0)
            {
                // Replacements and messages arrived
                title = rCount == 1 ? "Neue Vertretung" : "Neue Vertretungen";
                title += " und ";
                title += mCount == 1 ? "neue Nachricht" : "neue Nachrichten";
                message = String.format("Es gibt %s neue %s und %s neue %s", rCount,
                        rCount == 1 ? "Vertretung" : "Vertretungen", mCount,
                        mCount == 1 ? "Nachricht" : "Nachrichten");
            }

            preferences.edit()
                    .putInt(context.getString(R.string.saved_unseen_replacements), rCount)
                    .putInt(context.getString(R.string.saved_unseen_messages), mCount)
                    .apply();

            if (title != null && message != null)
            {
                sendNotification(title, message);
            }

            // If we already downloaded new tables, we can also save them
            // for future comparisons and user friendliness
            saveTablesToStorage();
        }

        return Result.success();
    }

    private void sendNotification(String title, String message)
    {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_new)
                .setColor(Color.parseColor("#FF8000"))
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pIntent)
                .setAutoCancel(true);

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        manager.notify(0, builder.build());
    }

    // Loads the current (old) replacements from the devices storage
    private boolean loadTablesFromStorage()
    {
        try
        {
            String path = context.getFilesDir().getAbsolutePath();
            if (!path.endsWith("/"))
            {
                path += "/";
            }

            // Load tables
            oldTables[0] = (ReplacementTable) Utils.loadObject(path + TABLE_1_BIN);
            oldTables[1] = (ReplacementTable) Utils.loadObject(path + TABLE_2_BIN);
        }
        catch (Exception e)
        {
            return false;
        }

        return true;
    }

    private boolean saveTablesToStorage()
    {
        try
        {
            String path = context.getFilesDir().getAbsolutePath();
            if (!path.endsWith("/"))
            {
                path += "/";
            }

            // Save tables
            if (tables != null)
            {
                Utils.saveObject(tables[0], path + TABLE_1_BIN);
                Utils.saveObject(tables[1], path + TABLE_2_BIN);
            }
        }
        catch (Exception e)
        {
            return false;
        }

        return true;
    }

    // Returns
    //     true if download was successful
    //     false if download failed
    private boolean downloadTables()
    {
        String str_grade = preferences.getString(context.getString(R.string.saved_grade), null);
        if (str_grade == null)
        {
            return false;
        }

        Grade grade = Grade.parse(str_grade);
        int errors = 0;

        for (int i = 0; i < tables.length; i++)
        {
            try
            {
                tables[i] = ReplacementTable.downloadTable(grade, i);
            }
            catch (WebException e)
            {
                errors++;
            }
        }

        return errors != 2;
    }
}
