package com.stonedroid.mpgvertretungsplan;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.*;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import de.stonedroid.vertretungsplan.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity
{
    public static final String TAG = MainActivity.class.getSimpleName();

    private static final String TABLE_1_BIN = "table1.dat";
    private static final String TABLE_2_BIN = "table2.dat";

    private static final String CHANNEL_ID = "0";

    public static boolean registerPreferencesChanges = true;

    // It's important to store this listener in a global field, otherwise gc will delete it.
    // (For some reasons, the android developers thought it was a good idea to store listeners in
    // a WeakHashMap, where objects without any references to others are deleted.)
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;
    private SharedPreferences preferences;

    private ReplacementTable[] tables = null;
    private TableFragment[] fragments = null;
    private CoordinatorLayout mainLayout;
    private ViewPager viewPager;
    private TabLayout tabLayout;

    private int tabTextColor;
    private int textColor;
    private int importantTextColor;
    private int cardColor;
    private int layoutColor;
    private int indicatorColor;

    private String versionName;
    private int versionCode;

    private boolean isDownloadingTables = false;
    
    private void log(String message)
    {
        Log.d(TAG, message);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Set theme (with customizations)
        CustomTheme theme = CustomTheme.changeTheme(this);
        textColor = theme.getTextColor();
        importantTextColor = theme.getImportantTextColor();
        tabTextColor = theme.getTabTextColor();
        cardColor = theme.getCardColor();
        layoutColor = theme.getLayoutColor();
        indicatorColor = theme.getIndicatorColor();

        // Get VersionName/VersionCode of this application
        try
        {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = info.versionName;
            versionCode = info.versionCode;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
        }

        // Display changelog if user updated to a newer version
        int oldVersionCode = preferences.getInt(getString(R.string.saved_version_code), Integer.MAX_VALUE);
        if (versionCode > oldVersionCode)
        {
            // If were a here, the user made an update and not a new installation
            SharedPreferences.Editor editor = preferences.edit();

            createChangelog().show();

            // Convert "Aus" to "Alle anzeigen" and "Alle blockieren" to "Nichts anzeigen"
            // for users coming from version 13 or below
            if (oldVersionCode <= 13)
            {
                Set<String> subjects = Subject.getAllSubjects().keySet();

                for (String subject : subjects)
                {
                    String pref = preferences.getString("filter_enabled_" + subject, null);
                    if (pref != null)
                    {
                        if (pref.equals("Aus"))
                        {
                            editor.putString("filter_enabled_" + subject, "Alle anzeigen");
                        }
                        else if(pref.equals("Alle blockieren"))
                        {
                            editor.putString("filter_enabled_" + subject, "Nichts anzeigen");
                        }
                    }
                }
            }

            // Invalidate offline tables -> same replacement table is shown
            // a little bit different in the new vertretungsplan-api v1.2
            if (oldVersionCode <= 15)
            {
                editor.putBoolean(getString(R.string.saved_offline_available), false);

                // Also convert "phil" to "phil1"
                String key = "filter_enabled_Philosophie";
                String oldValue = preferences.getString(key, null);
                if (oldValue != null && oldValue.equals("phil"))
                {
                    editor.putString(key, "phil1");
                }
            }

            editor.apply();
        }

        setContentView(R.layout.activity_main);

        preferences.edit()
                .putInt(getString(R.string.saved_version_code), versionCode)
                .apply();

        // Get mainLayout
        mainLayout = findViewById(R.id.main_layout);
        mainLayout.setBackgroundColor(layoutColor);

        // Turn action bar elevation off and transfer elevation to TabLayout
        float elevation = 0;
        ActionBar bar = getSupportActionBar();
        if (bar != null)
        {
            elevation = bar.getElevation();
            bar.setElevation(0);
        }

        // Create a TableFragment foreach week
        fragments = new TableFragment[2];
        for (int i = 0; i < fragments.length; i++)
        {
            if (savedInstanceState == null)
            {
                // Create new fragments if app opens from drive
                fragments[i] = TableFragment.newInstance();
            }
            else
            {
                // Get recycled fragments back
                fragments[i] = (TableFragment) getSupportFragmentManager()
                        .findFragmentByTag(getFragmentTag(R.id.view_pager, i));

                if (fragments[i] == null)
                {
                    fragments[i] = TableFragment.newInstance();
                }
            }
        }

        // Setup ViewPager with TabLayout
        fragments[1].setOnFragmentCreatedListener(this::onCreate2);
        CharSequence[] titles = {getCalendarSpan(0), getCalendarSpan(1)};
        viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(new ViewPagerAdapter(getSupportFragmentManager(),
                fragments, titles));

        tabLayout = findViewById(R.id.tab_layout);
        tabLayout.setBackgroundColor(Utils.getThemePrimaryColor(this));
        tabLayout.setTabTextColors(tabTextColor - (70 << 24), tabTextColor);
        tabLayout.setSelectedTabIndicatorColor(indicatorColor);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            tabLayout.setElevation(elevation);
        }

        tabLayout.setupWithViewPager(viewPager);

        // Reset notifications
        NotificationManagerCompat.from(this).cancelAll();
        preferences.edit()
                .putInt(getString(R.string.saved_unseen_replacements), 0)
                .putInt(getString(R.string.saved_unseen_messages), 0)
                .apply();
    }

    // Is executed after fragments were instantiated
    private void onCreate2()
    {
        // Select correct tab automatically
        // (on weekends show the next table
        Calendar calendar = Calendar.getInstance();
        if ((calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY)
                || (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY))
        {
            viewPager.setCurrentItem(1);
        }

        for (TableFragment fragment : fragments)
        {
            SwipeRefreshLayout refreshLayout = fragment.getRefreshLayout();
            refreshLayout.setOnRefreshListener(() ->
            {
                downloadTablesAndShow(Grade.parse(preferences.getString(getString(R.string.saved_grade), null)), true, true);
            });
        }

        // Register listener to be noticed if user changes something in the settings
        preferenceChangeListener = (sharedPrefs, s) ->
        {
            if (registerPreferencesChanges && !preferences.getBoolean(getString(R.string.saved_init_preferences), false))
            {
                // s = (String) key of the changed value
                log("Key of changed value: " + s);
                if (s.equals(getString(R.string.saved_grade)))
                {
                    // Load new table
                    downloadTablesAndShow(Grade.parse(sharedPrefs.getString(s, "")), true, false);
                }
                else if (s.contains("filter_enabled"))
                {
                    // Always show the table from scratch if filter settings were altered
                    if (tables != null)
                    {
                        showTables(tables);
                    }
                }
                else if (s.equals("theme"))
                {
                    // To change the theme, recreate the activity
                    recreate();
                }
                else if (s.equals(getString(R.string.saved_notifications_enabled)))
                {
                    boolean enabled = preferences.getBoolean(s, false);
                    if (enabled)
                    {
                        enableNotifications();
                    }
                    else
                    {
                        disableNotifications();
                    }
                }
            }
        };
        preferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        boolean firstTime = preferences.getBoolean(getString(R.string.saved_first_time), true);
        if (firstTime)
        {
            log("First time!");
            createWelcomeDialog().show();
        }
        else
        {
            log("Second time!");
            // Use user defined grade and download the grade
            String grade = preferences.getString(getString(R.string.saved_grade), null);
            if (grade != null)
            {
                tables = (ReplacementTable[]) getLastCustomNonConfigurationInstance();
                if (tables != null)
                {
                    // Load tables from cache
                    log("Load tables from cache");
                    showTables(tables);
                }
                else
                {
                    if (isNetworkAvailable())
                    {
                        // Download from internet
                        log("Download tables");
                        downloadTablesAndShow(Grade.parse(grade), true, false);
                    }
                    else
                    {
                        // Is there an offline version available?
                        boolean canDoOffline = preferences.getBoolean(getString(R.string.saved_offline_available), false);
                        if (canDoOffline)
                        {
                            // Load ReplacementTables from internal storage
                            loadTablesFromStorage(true);
                        }
                        else
                        {
                            // No internet and no offline data...
                            Toast.makeText(this, "Keine Vertretungen wurden heruntergeladen", Toast.LENGTH_LONG)
                                    .show();
                        }
                    }
                }
            }
            else
            {
                createGradeDialog().show();
            }
        }
    }

    private void enableNotifications()
    {
        createNotificationChannel();

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest work = new PeriodicWorkRequest.Builder(NotificationWorker.class,
                30, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();

        preferences.edit()
                .putString(getString(R.string.saved_worker_id), work.getId().toString())
                .apply();

        WorkManager manager = WorkManager.getInstance();
        manager.enqueue(work);

        log(work.getId().toString());
    }

    private void disableNotifications()
    {
        /*String id = preferences.getString(getString(R.string.saved_worker_id), null);
        if (id != null)
        {
            WorkManager.getInstance().cancelWorkById(UUID.fromString(id));
        }*/

        WorkManager.getInstance().cancelAllWork();
    }

    private void createNotificationChannel()
    {
        if (Build.VERSION.SDK_INT >= 26)
        {
            String name = getString(R.string.notification_channel_name);
            String description = getString(R.string.notification_channel_description);

            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager  manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    // Load tables async from storage
    private void loadTablesFromStorage(boolean showTables)
    {
        new Thread(() ->
        {
            try
            {
                String path = this.getFilesDir().getAbsolutePath();
                if (!path.endsWith("/"))
                {
                    path += "/";
                }

                log("Reading from " + path);
                // Load tables
                if (tables == null)
                {
                    tables = new ReplacementTable[2];
                }

                tables[0] = (ReplacementTable) Utils.loadObject(path + TABLE_1_BIN);
                tables[1] = (ReplacementTable) Utils.loadObject(path + TABLE_2_BIN);
                if (showTables)
                {
                    runOnUiThread(() ->
                    {
                        showTables(tables);
                        Snackbar.make(mainLayout, R.string.offline_version, Snackbar.LENGTH_SHORT).show();
                    });
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }).start();
    }

    private AlertDialog createWelcomeDialog()
    {
        return new AlertDialog.Builder(this)
                .setTitle(R.string.welcome)
                .setMessage(R.string.welcome_message)
                .setPositiveButton("OK", null)
                .setOnDismissListener(dialog -> createGradeDialog().show())
                .create();
    }

    private AlertDialog createGradeDialog()
    {
        List<String> grades = Grade.getGradeNames();
        CharSequence[] cs_grades = new CharSequence[grades.size()];
        cs_grades = grades.toArray(cs_grades);

        return new AlertDialog.Builder(this)
                .setTitle(getString(R.string.grade_message))
                .setItems(cs_grades, (dialog, which) ->
                {
                    preferences.edit()
                            .putString(getString(R.string.saved_grade), grades.get(which))
                            .putBoolean(getString(R.string.saved_first_time), false)
                            .apply();
                })
                .setOnCancelListener(dialog -> createGradeDialog().show())
                .create();
    }

    private AlertDialog createChangelog()
    {
        return new AlertDialog.Builder(this)
                .setTitle(getString(R.string.changelog_title))
                .setMessage(getString(R.string.changelog_message))
                .setPositiveButton("OK", null)
                .create();
    }

    // Returns a nice readable string like (01.01 - 05.01)
    private String getCalendarSpan(int plusWeeks)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.WEEK_OF_YEAR, plusWeeks);
        // Set calendar to Monday
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        String part1 = String.format("%s.%s.", toDate(calendar.get(Calendar.DAY_OF_MONTH)),
                toDate(calendar.get(Calendar.MONTH) + 1));
        // Set calendar to Friday
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
        String part2 = String.format("%s.%s.", toDate(calendar.get(Calendar.DAY_OF_MONTH)),
                toDate(calendar.get(Calendar.MONTH) + 1));
        // Build formatted string
        return String.format("%s - %s", part1, part2);
    }

    // Adds a zero if date.str() has length == 1
    private String toDate(int date)
    {
        return date < 10 ? "0" + String.valueOf(date) : String.valueOf(date);
    }

    // Adds a zero if date.str() has length == 1
    private String toDate(String date)
    {
        return date.length() == 1 ? "0" + date : date;
    }

    // Returns the intern tag of a fragment
    private String getFragmentTag(int viewId, int position)
    {
        return String.format("android:switcher:%s:%s", viewId, position);
    }

    // Returns whether a network is available
    private boolean isNetworkAvailable()
    {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance()
    {
        // If the activity should be rebuild, the tables are cached and retrieved in the onCreate method
        return tables;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_settings:
                openSettings(false);
                break;
            case R.id.action_reload:
                if (!isDownloadingTables)
                {
                    Grade grade = Grade.parse(preferences.getString(getString(R.string.saved_grade), ""));
                    if (grade != null)
                    {
                        downloadTablesAndShow(grade, true, false);
                    }
                    else
                    {
                        openSettings(true);
                    }
                }
                break;
            case R.id.action_changelog:
                createChangelog().show();
                break;
            case R.id.action_info:
                createInfoDialog().show();

        }

        return super.onOptionsItemSelected(item);
    }

    private AlertDialog createInfoDialog()
    {
        String message = String.format("Version %s\n" +
                "Copyright © %s Alexander Steinhauer",
                versionName,
                Calendar.getInstance().get(Calendar.YEAR));
        return new AlertDialog.Builder(this)
                .setTitle(getString(R.string.action_info))
                .setMessage(message)
                .create();
    }

    // Downloads both ReplacementTables (for this and the next week) async
    // and adds them after downloading to the MainLayout
    // without blocking the UI thread.
    private void downloadTablesAndShow(Grade grade, boolean saveTables, boolean swipeRefresh)
    {
        // Check if we have a valid network connection
        if (isNetworkAvailable())
        {
            isDownloadingTables = true;

            showProgressBar(swipeRefresh);

            new Thread(() ->
            {
                tables = new ReplacementTable[2];

                Thread th1 = new Thread(() ->
                {
                    try
                    {
                        tables[0] = ReplacementTable.downloadTable(grade, 0);
                    }
                    catch (WebException e)
                    {
                        log("Couldn't download ReplacementTable#1");
                    }
                });

                Thread th2 = new Thread(() ->
                {
                    try
                    {
                        tables[1] = ReplacementTable.downloadTable(grade, 1);
                    }
                    catch (WebException e)
                    {
                        log("Couldn't download ReplacementTable#2");
                    }
                });

                th1.start();
                th2.start();

                try
                {
                    th1.join();
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }

                try
                {
                    th2.join();
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }

                runOnUiThread(() ->
                {
                    hideProgressBar();
                    showTables(tables);
                });
                
                if (saveTables)
                {
                    // Save tables to storage
                    String path = getFilesDir().getAbsolutePath();
                    if (!path.endsWith("/"))
                    {
                        path += "/";
                    }

                    try
                    {
                        Utils.saveObject(tables[0], path + TABLE_1_BIN);
                        Utils.saveObject(tables[1], path + TABLE_2_BIN);
                        preferences.edit()
                                .putBoolean(getString(R.string.saved_offline_available), true)
                                .apply();

                        log("Saved tables to path " + path);
                    }
                    catch (IOException e)
                    {
                        runOnUiThread(() -> Toast
                                .makeText(this, getString(R.string.low_memory), Toast.LENGTH_SHORT)
                                .show());
                    }
                }

                isDownloadingTables = false;
            }).start();
        }
        else
        {
            // Network is not available...
            Snackbar.make(mainLayout, R.string.no_internet, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.retry, v -> downloadTablesAndShow(grade, saveTables, false))
                    .show();
        }
    }

    // Show all replacements and messages of the ReplacementTable
    // onto the main layout of this activity.
    private void showTable(ReplacementTable table)
    {
        showTables(new ReplacementTable[] {table});
    }

    // Shows all replacements and messages of all ReplacementTables
    // onto the the main layout of this activity.
    private void showTables(ReplacementTable[] tables)
    {
        boolean alreadyRenamedAppbar = false;

        for (int t = 0; t < tables.length; t++)
        {
            // Setup layout
            LinearLayout layout = fragments[t].getLayout();
            layout.removeAllViews();

            ReplacementTable table = tables[t];
            if (table != null)
            {
                // Rename appbar title to possibly new grade
                if (!alreadyRenamedAppbar)
                {
                    ActionBar bar = getSupportActionBar();
                    if (bar != null)
                    {
                        bar.setTitle(String.format("Klasse %s", table.getGrade()));

                        Calendar date = table.getDownloadDate();
                        bar.setSubtitle(String.format("vom %s.%s.%s um %s:%s",
                                toDate(date.get(Calendar.DAY_OF_MONTH)),
                                toDate(date.get(Calendar.MONTH) + 1),
                                date.get(Calendar.YEAR),
                                toDate(date.get(Calendar.HOUR_OF_DAY)),
                                toDate(date.get(Calendar.MINUTE))));
                    }

                    alreadyRenamedAppbar = true;
                }

                // Get all necessary information
                ArrayList<Replacement> replacements = preferences
                        .getBoolean(getString(R.string.saved_filter_enabled), false)
                        ? new ArrayList<>(Utils.filterReplacements(this, table))
                        : new ArrayList<>(table.getReplacements());
                ArrayList<Message> messages = new ArrayList<>(table.getMessages());
                String[] dates = table.getDates();
                String[] days = table.getDays();

                for (int i = 0; i < dates.length; i++)
                {
                    if (layout.getChildCount() != 0)
                    {
                        layout.addView(createSpacer());
                    }

                    // Setup card view with layout
                    CardView card = createCard();
                    LinearLayout cardLayout = new LinearLayout(this);
                    cardLayout.setOrientation(LinearLayout.VERTICAL);
                    cardLayout.setGravity(Gravity.CENTER);
                    cardLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT));

                    String date = dates[i];
                    String day = days[i];
                    // Add date view to card
                    cardLayout.addView(createDateView(date, day));
                    // Replacements and Messages are sorted by date, which means that the first replacement/message
                    // in the list is always the earliest replacement/message

                    // Add messages
                    boolean addedMessages = false;

                    while (!messages.isEmpty() && messages.get(0).getDate().equals(date))
                    {
                        cardLayout.addView(createMessageView(messages.remove(0)));
                        addedMessages = true;
                    }

                    // Add replacements
                    boolean addedCaption = false;

                    while (!replacements.isEmpty() && replacements.get(0).getDate().equals(date))
                    {
                        if (!addedCaption)
                        {
                            cardLayout.addView(createCaptionView());
                            addedCaption = true;
                        }

                        cardLayout.addView(createLine());
                        cardLayout.addView(createReplacementView(replacements.remove(0)));
                    }

                    // If no replacements were added, but messages were added
                    // add a tiny spacer so the message's TextView doesn't almost
                    // touch the black line afterwards
                    if (!addedCaption && addedMessages)
                    {
                        // Create small spacer between line and message to make it look better
                        View view = new View(this);
                        view.setBackgroundColor(Color.TRANSPARENT);
                        view.setMinimumHeight(dpToPx(8));
                        cardLayout.addView(view);
                    }

                    // If no replacements were added, add noReplacementsView
                    if (!addedCaption)
                    {
                        cardLayout.addView(createLine());
                        cardLayout.addView(createNoReplacementsView());
                    }

                    // Add card to layout
                    card.addView(cardLayout);
                    layout.addView(card);
                }
            }
            else
            {
                layout.addView(createNoTableView());
            }
        }
    }

    private void showProgressBar(boolean swipeRefresh)
    {
        for (TableFragment fragment : fragments)
        {
            LinearLayout layout = fragment.getLayout();
            layout.removeAllViews();

            if (!swipeRefresh)
            {
                layout.addView(createProgressBar());
            }
        }
    }

    private void hideProgressBar()
    {
        for (TableFragment fragment : fragments)
        {
            fragment.getRefreshLayout().setRefreshing(false);
        }
    }

    private View createProgressBar()
    {
        ProgressBar bar = new ProgressBar(this);
        bar.getIndeterminateDrawable().setColorFilter(indicatorColor, PorterDuff.Mode.SRC_IN);
        bar.setMinimumHeight(dpToPx(32));
        return bar;
    }

    private View createNoTableView()
    {
        TextView text = new TextView(this);
        text.setMinHeight(dpToPx(32));
        text.setGravity(Gravity.CENTER);
        text.setTextColor(textColor);
        text.setText(R.string.no_table);
        return text;
    }

    // Creates a grey thin horizontal line
    private View createLine()
    {
        View view = new View(this);
        int width = getResources().getDisplayMetrics().widthPixels - dpToPx(32);
        view.setLayoutParams(new ViewGroup.LayoutParams(width, dpToPx(1)));
        view.setBackgroundColor(Color.GRAY);
        return view;
    }

    // Creates a view with table captions for the replacements
    private View createCaptionView()
    {
        // Setup container
        LinearLayout view = new LinearLayout(this);
        view.setGravity(Gravity.CENTER);
        view.setOrientation(LinearLayout.HORIZONTAL);
        view.setMinimumHeight(dpToPx(32));
        // Calculate width foreach text view
        int width = getResources().getDisplayMetrics().widthPixels - dpToPx(16);
        int maxWidth = width / 5;
        // Add replacement data pieces
        String[] data = {"Stunde", "Fach", "Raum", "statt Fach", "Text"};

        for (int i = 0; i < data.length; i++)
        {
            // Give text component of replacement more space
            int textWidth = i != data.length - 1 ? maxWidth - dpToPx(8) : maxWidth + dpToPx(32);
            // Setup text
            TextView text = new TextView(this);
            text.setLayoutParams(new ViewGroup.LayoutParams(textWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
            text.setTypeface(Typeface.DEFAULT_BOLD);
            text.setTextColor(textColor);
            text.setText(data[i]);
            text.setGravity(Gravity.CENTER);
            view.addView(text);
        }

        return view;
    }

    // Creates a view containing the message's text
    private View createMessageView(Message message)
    {
        TextView text = new TextView(this);
        text.setGravity(Gravity.CENTER);
        text.setTextColor(textColor);
        text.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
        text.setText(message.getText());

        // Clipboard functionality
        // When a message is clicked for a longer time,
        // the message is copied
        text.setOnLongClickListener(v ->
        {
            copyIntoClipboard(message.getText(),
                    () -> Toast.makeText(this, "Nachricht wurde kopiert", Toast.LENGTH_SHORT).show());
            return true;
        });
        return text;
    }

    // Creates a view containing all information of the replacement
    private View createReplacementView(Replacement replacement)
    {
        // Setup container
        LinearLayout view = new LinearLayout(this);
        view.setGravity(Gravity.CENTER);
        view.setOrientation(LinearLayout.HORIZONTAL);
        view.setMinimumHeight(dpToPx(32));

        // Clipboard functionality
        // When a replacement is clicked for a longer time,
        // a nice formatted String from the replacement's data is created
        // with formatting tags for WhatsApp
        view.setOnLongClickListener(v ->
        {
            /*String formattedData = String.format("*Datum*: %s\n*Tag*: %s\n*Klasse*: %s\n*Stunde*: %s\n" +
                    "*Fach*: %s\n*Raum*: %s\n*statt Fach*: %s\n*Text*: %s",
                    replacement.getDate(), replacement.getDay(), replacement.getGrade(),
                    replacement.getPeriod(), replacement.getSubject(), replacement.getRoom(),
                    replacement.getOldSubject(), replacement.getText());*/
            copyIntoClipboard(Utils.getReplacementSummary(replacement),
                    () -> Toast.makeText(this, "Vertretung wurde kopiert", Toast.LENGTH_SHORT).show());
            return true;
        });

        // Calculate width foreach text view
        int width = getResources().getDisplayMetrics().widthPixels - dpToPx(16);
        int maxWidth = width / 5;
        // Add replacement data pieces
        String[] data = replacement.getData();

        for (int i = 3; i < data.length; i++)
        {
            // Give text component of replacement more space
            int textWidth = i != data.length - 1 ? maxWidth - dpToPx(8) : maxWidth + dpToPx(32);
            // Setup text
            TextView text = new TextView(this);
            text.setLayoutParams(new ViewGroup.LayoutParams(textWidth, ViewGroup.LayoutParams.WRAP_CONTENT));

            if (i == ReplacementFilter.TEXT.ordinal())
            {
                // Mark text red if the replacements indicates that something was cancelled
                if (data[i].contains("fällt aus"))
                {
                    SpannableStringBuilder str = new SpannableStringBuilder(data[i]);
                    ForegroundColorSpan color = new ForegroundColorSpan(importantTextColor);
                    int start = data[i].indexOf("fällt aus");
                    int end = start + "fällt aus".length();
                    str.setSpan(color, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    text.setText(str);
                }
                else
                {
                    text.setText(data[i]);
                }
            }
            else
            {
                text.setText(data[i]);
            }

            text.setTextColor(textColor);
            text.setGravity(Gravity.CENTER);
            view.addView(text);
        }

        return view;
    }

    // Copies the given text into the primary clipboard
    private void copyIntoClipboard(String text, Runnable onSuccess)
    {
        ClipboardManager manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData data = ClipData.newPlainText("Vertretungsplan", text);
        if (manager != null)
        {
            manager.setPrimaryClip(data);
            if (onSuccess != null)
            {
                runOnUiThread(onSuccess);
            }
        }
        else
        {
            Toast.makeText(this, getString(R.string.error_message), Toast.LENGTH_SHORT).show();
        }
    }

    // Creates a view which shows the user that there aren't any replacements.
    private View createNoReplacementsView()
    {
        TextView text = new TextView(this);
        text.setMinHeight(dpToPx(32));
        text.setGravity(Gravity.CENTER);
        text.setTextColor(textColor);
        text.setText(R.string.no_replacements);
        return text;
    }

    // Creates a view containing the replacement's/message's date
    private View createDateView(String date, String day)
    {
        TextView text = new TextView(this);
        text.setMinHeight(dpToPx(32));
        text.setTextColor(textColor);
        text.setGravity(Gravity.CENTER);

        // Make the day bold
        String str = String.format("%s - %s", day, date);
        SpannableStringBuilder txt = new SpannableStringBuilder(str);
        txt.setSpan(new StyleSpan(Typeface.BOLD), 0, str.indexOf(" "), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        text.setText(txt);
        return text;
    }

    // Creates a transparent view which is used to separate the cards
    private View createSpacer()
    {
        View view = new View(this);
        view.setMinimumHeight(dpToPx(10));
        view.setBackgroundColor(Color.TRANSPARENT);
        return view;
    }

    // Creates a beautiful CardView which functions as a container for all other views
    private CardView createCard()
    {
        CardView card = new CardView(this);
        //card.setMinimumHeight(dpToPx(32));
        card.setCardElevation(dpToPx(1));
        card.setRadius(dpToPx(8));
        card.getBackground().setColorFilter(cardColor, PorterDuff.Mode.SRC);
        return card;
    }

    // Opens the preference screen
    private void openSettings(boolean withDialog)
    {
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtra("with_dialog", withDialog);
        startActivity(intent);
    }

    // Convert density pixels to real pixels
    private int dpToPx(float dp)
    {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}
