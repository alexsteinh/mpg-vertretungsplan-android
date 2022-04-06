package com.stonedroid.mpgvertretungsplan;

import android.app.NotificationManager;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.BlendModeColorFilterCompat;
import androidx.core.graphics.BlendModeCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.common.collect.ImmutableSet;
import com.stonedroid.mpgvertretungsplan.settings.SettingsActivity;
import com.stonedroid.mpgvertretungsplan.theme.CustomTheme;
import com.stonedroid.mpgvertretungsplan.theme.CustomThemes;
import de.stonedroid.vertretungsplan.*;

import java.util.*;

public class MainActivity extends AppCompatActivity {
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

    private CustomTheme theme;

    private boolean isDownloadingTables = false;

    private void log(String message) {
        Log.d(TAG + "(" + toString().split("@")[1] + ")", message);
    }

    private boolean shouldShowDeathMessage() {
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);
        return year > 2022 || (year == 2022 && month >= Calendar.AUGUST);
    }

    private void showDeathAlert(Context context) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.deathalert_title))
                .setMessage(context.getString(R.string.deathalert_message))
                .setPositiveButton("OK", null)
                .create();

        dialog.setOnDismissListener(sender -> finish());
        dialog.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Get VersionCode of this application
        int versionCode = Utils.getVersionCode(this);

        // Display changelog if user updated to a newer version
        boolean displayChangelog = false;
        int oldVersionCode = preferences.getInt(getString(R.string.saved_version_code), Integer.MAX_VALUE);
        if (versionCode > oldVersionCode) {
            // If were a here, the user made an update and not a new installation
            SharedPreferences.Editor editor = preferences.edit();

            displayChangelog = true;

            // - Convert "Aus" to "Alle anzeigen" and "Alle blockieren" to "Nichts anzeigen"
            //   for users coming from version 13 or below
            if (oldVersionCode <= 13) {
                Set<String> subjects = Subject.getAllSubjects().keySet();

                for (String subject : subjects) {
                    String pref = preferences.getString("filter_enabled_" + subject, null);
                    if (pref != null) {
                        if (pref.equals("Aus")) {
                            editor.putString("filter_enabled_" + subject, "Alle anzeigen");
                        } else if (pref.equals("Alle blockieren")) {
                            editor.putString("filter_enabled_" + subject, "Nichts anzeigen");
                        }
                    }
                }
            }

            // - Invalidate offline tables -> same replacement table is shown
            //   a little bit different in the new vertretungsplan-api v1.2
            //
            // - Change theme names
            if (oldVersionCode <= 15) {
                editor.putBoolean(getString(R.string.saved_offline_available), false);

                // Also convert "phil" to "phil1"
                String key = "filter_enabled_Philosophie";
                String oldValue = preferences.getString(key, null);
                if (oldValue != null && oldValue.equals("phil")) {
                    editor.putString(key, "phil1");
                }

                String theme = preferences.getString(getString(R.string.saved_theme), "Orange");
                if (!theme.equals("Orange")) {
                    if (theme.equals("Schwarz")) {
                        theme = "Dark";
                    } else if (theme.equals("Weiß")) {
                        theme = "Light";
                    }

                    editor.putString(getString(R.string.saved_theme), theme);
                }
            }

            // - Delete key "init_preferences"
            // - New vertretungsplan-api v1.2.2 -> Invalidate offline tables
            if (oldVersionCode <= 17) {
                editor.remove("init_preferences");
                editor.putBoolean(getString(R.string.saved_offline_available), false);
            }

            // - Delete notifications
            // - Clean up preferences
            if (oldVersionCode <= 21) {
                editor.apply();
                deleteNotificationChannel();

                Set<String> neededPreferences = ImmutableSet.of(
                        "saved_grade",
                        "saved_first_time",
                        "saved_offline_available",
                        "saved_theme",
                        "saved_version_code",
                        "saved_rounded_corners",
                        "saved_swipe_refresh_enabled"
                );
                Set<String> legacyPreferences = new HashSet<>(preferences.getAll().keySet());
                legacyPreferences.removeAll(neededPreferences);
                for (String legacyPreference : legacyPreferences) {
                    editor.remove(legacyPreference);
                }
            }

            editor.apply();
        }

        // Set theme (with customizations)
        theme = CustomThemes.changeTheme(this, false);

        if (shouldShowDeathMessage()) {
            showDeathAlert(this);
            return;
        }

        setContentView(R.layout.activity_main);

        if (displayChangelog) {
            Utils.createChangelog(this).show();
        }

        preferences.edit()
                .putInt(getString(R.string.saved_version_code), versionCode)
                .apply();

        // Get mainLayout
        mainLayout = findViewById(R.id.main_layout);
        mainLayout.setBackgroundColor(theme.getLayoutColor());

        Toolbar toolbar = findViewById(theme.isLight() ? R.id.toolbar_light : R.id.toolbar_dark);
        toolbar.setVisibility(View.VISIBLE);
        setSupportActionBar(toolbar);

        // Create a TableFragment foreach week
        fragments = new TableFragment[2];
        for (int i = 0; i < fragments.length; i++) {
            if (savedInstanceState == null) {
                // Create new fragments if app opens from drive
                fragments[i] = TableFragment.newInstance();
            } else {
                // Get recycled fragments back
                fragments[i] = (TableFragment) getSupportFragmentManager()
                        .findFragmentByTag(getFragmentTag(R.id.view_pager, i));

                if (fragments[i] == null) {
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
        tabLayout.setTabTextColors(theme.getTabTextColor() - ((theme.isLight() ? 138 : 76) << 24), theme.getTabTextColor());
        tabLayout.setSelectedTabIndicatorColor(theme.getIndicatorColor());
        tabLayout.setTabRippleColor(ColorStateList.valueOf(theme.getTabRippleColor()));

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // Add shadow below TabLayout for pre-Lollipop devices
            View shadow = new View(this);
            shadow.setBackgroundResource(R.drawable.shadow);
            shadow.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1)));

            LinearLayout layout = (LinearLayout) mainLayout.getChildAt(0);
            layout.addView(shadow, 1);
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
    private void onCreate2() {
        // Select correct tab automatically
        // (on weekends show the next table
        Calendar calendar = Calendar.getInstance();
        if ((calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY)
                || (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)) {
            viewPager.setCurrentItem(1);
        }

        for (TableFragment fragment : fragments) {
            SwipeRefreshLayout refreshLayout = fragment.getRefreshLayout();
            refreshLayout.setOnRefreshListener(() ->
            {
                if (!isDownloadingTables) {
                    downloadTablesAndShow(Grade.parse(preferences.getString(getString(R.string.saved_grade), null)), true, true);
                } else {
                    refreshLayout.setRefreshing(false);
                }
            });
            if (!preferences.getBoolean(getString(R.string.saved_swipe_refresh_enabled), true)) {
                refreshLayout.setEnabled(false);
            }
        }

        // Register listener to be noticed if user changes something in the settings
        preferenceChangeListener = (sharedPrefs, s) ->
        {
            if (registerPreferencesChanges) {
                // s = (String) key of the changed value
                log("Key of changed value: " + s);
                if (s.equals(getString(R.string.saved_grade))) {
                    // Load new table
                    downloadTablesAndShow(Grade.parse(sharedPrefs.getString(s, "")), true, false);
                } else if (s.contains("filter_enabled") || s.equals(getString(R.string.saved_rounded_corners))) {
                    // Always show the table from scratch if filter settings were altered
                    if (tables != null) {
                        showTables(tables);
                    }
                } else if (s.equals("theme")) {
                    // To change the theme, recreate the activity
                    preferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);

                    if (Build.VERSION.SDK_INT >= 28) {
                        releaseInstance();
                    } else {
                        recreate();
                    }
                } else if (s.equals(getString(R.string.saved_swipe_refresh_enabled))) {
                    boolean enabled = preferences.getBoolean(s, true);
                    if (enabled) {
                        enableSwipeRefresh();
                    } else {
                        disableSwipeRefresh();
                    }
                }
            }
        };
        preferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        boolean firstTime = preferences.getBoolean(getString(R.string.saved_first_time), true);
        if (firstTime) {
            log("First time!");
            createWelcomeDialog().show();
        } else {
            log("Second time!");
            // Use user defined grade and download the grade
            Grade grade = Grade.parse(preferences.getString(getString(R.string.saved_grade), null));
            if (grade != null) {
                tables = (ReplacementTable[]) getLastCustomNonConfigurationInstance();
                if (tables != null) {
                    // Load tables from cache
                    log("Load tables from cache");
                    showTables(tables);
                } else {
                    if (isNetworkAvailable()) {
                        // Download from internet
                        log("Download tables");
                        downloadTablesAndShow(grade, true, false);
                    } else {
                        // Is there an offline version available?
                        boolean canDoOffline = preferences.getBoolean(getString(R.string.saved_offline_available), false);
                        if (canDoOffline) {
                            // Load ReplacementTables from internal storage
                            loadTablesFromStorage(true);
                        } else {
                            // No internet and no offline data...
                            Toast.makeText(this, "Keine Vertretungen wurden heruntergeladen", Toast.LENGTH_LONG)
                                    .show();
                        }
                    }
                }
            } else {
                createGradeDialog().show();
            }
        }
    }

    private void deleteNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.deleteNotificationChannel(CHANNEL_ID);
        }
    }

    // Load tables async from storage
    private void loadTablesFromStorage(boolean showTables) {
        new Thread(() ->
        {
            try {
                String path = this.getFilesDir().getAbsolutePath();
                if (!path.endsWith("/")) {
                    path += "/";
                }

                log("Reading from " + path);
                // Load tables
                if (tables == null) {
                    tables = new ReplacementTable[2];
                }

                tables[0] = (ReplacementTable) Utils.loadObject(path + TABLE_1_BIN);
                tables[1] = (ReplacementTable) Utils.loadObject(path + TABLE_2_BIN);
                if (showTables) {
                    runOnUiThread(() ->
                    {
                        showTables(tables);
                        Snackbar.make(mainLayout, R.string.offline_version, Snackbar.LENGTH_SHORT)
                                .setActionTextColor(theme.getActionTextColor())
                                .show();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private AlertDialog createWelcomeDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.welcome)
                .setMessage(getString(R.string.welcome_message,
                        getString(R.string.changelog_message),
                        String.valueOf(Calendar.getInstance().get(Calendar.YEAR))))
                .setPositiveButton("OK", null)
                .setOnDismissListener(d -> createGradeDialog().show())
                .create();

        dialog.setOnShowListener(d ->
        {
            TextView text = dialog.findViewById(android.R.id.message);
            if (text != null) {
                Utils.toMonospacedFont(this, text);
            }
        });

        return dialog;
    }

    private AlertDialog createGradeDialog() {
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

    // Returns a nice readable string like (01.01 - 05.01)
    private String getCalendarSpan(int plusWeeks) {
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
    private String toDate(int date) {
        return date < 10 ? "0" + date : String.valueOf(date);
    }

    // Adds a zero if date.str() has length == 1
    private String toDate(String date) {
        return date.length() == 1 ? "0" + date : date;
    }

    // Returns the intern tag of a fragment
    private String getFragmentTag(int viewId, int position) {
        return String.format("android:switcher:%s:%s", viewId, position);
    }

    // Returns whether a network is available
    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT < 28) {
            NetworkInfo info = manager.getActiveNetworkInfo();
            return info != null && info.isConnected();
        }

        Network network = manager.getActiveNetwork();
        if (network == null) {
            return false;
        }

        return manager.getNetworkCapabilities(network).hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        // If the activity should be rebuild, the tables are cached and retrieved in the onCreate method
        return tables;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        int color;

        if (theme.isLight()) {
            color = Color.BLACK;
        } else {
            color = Color.WHITE;
        }

        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).getIcon().setColorFilter(BlendModeColorFilterCompat
                    .createBlendModeColorFilterCompat(color, BlendModeCompat.SRC_ATOP));
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                openSettings(false);
                break;
            case R.id.action_reload:
                if (!isDownloadingTables) {
                    Grade grade = Grade.parse(preferences.getString(getString(R.string.saved_grade), ""));
                    if (grade != null) {
                        downloadTablesAndShow(grade, true, false);
                    } else {
                        openSettings(true);
                    }
                }
        }

        return super.onOptionsItemSelected(item);
    }

    // Downloads both ReplacementTables (for this and the next week) async
    // and adds them after downloading to the MainLayout
    // without blocking the UI thread.
    private void downloadTablesAndShow(Grade grade, boolean saveTables, boolean swipeRefresh) {
        // Check if we have a valid network connection
        if (isNetworkAvailable()) {
            isDownloadingTables = true;

            showProgressBar(swipeRefresh);

            new Thread(() ->
            {
                tables = new ReplacementTable[2];

                Thread th1 = new Thread(() ->
                {
                    try {
                        tables[0] = ReplacementTable.downloadTable(grade, 0);
                    } catch (WebException e) {
                        log("Couldn't download ReplacementTable#1");
                    }
                });

                Thread th2 = new Thread(() ->
                {
                    try {
                        tables[1] = ReplacementTable.downloadTable(grade, 1);
                    } catch (WebException e) {
                        log("Couldn't download ReplacementTable#2");
                    }
                });

                th1.start();
                th2.start();

                try {
                    th1.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                try {
                    th2.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                runOnUiThread(() ->
                {
                    hideProgressBar();
                    showTables(tables);
                });

                if (saveTables) {
                    // Save tables to storage
                    String path = getFilesDir().getAbsolutePath();
                    if (!path.endsWith("/")) {
                        path += "/";
                    }

                    try {
                        Utils.saveObject(tables[0], path + TABLE_1_BIN);
                        Utils.saveObject(tables[1], path + TABLE_2_BIN);
                        preferences.edit()
                                .putBoolean(getString(R.string.saved_offline_available), true)
                                .apply();

                        log("Saved tables to path " + path);
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast
                                .makeText(this, getString(R.string.low_memory), Toast.LENGTH_LONG)
                                .show());
                    }
                }

                isDownloadingTables = false;
            }).start();
        } else {
            // Network is not available...
            hideProgressBar();

            Snackbar.make(mainLayout, R.string.no_internet, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.retry, v -> downloadTablesAndShow(grade, saveTables, false))
                    .setActionTextColor(theme.getActionTextColor())
                    .show();
        }
    }

    // Shows all replacements and messages of all ReplacementTables
    // onto the the main layout of this activity.
    private void showTables(ReplacementTable[] tables) {
        boolean alreadyRenamedAppbar = false;

        for (int t = 0; t < tables.length; t++) {
            // Setup layout
            LinearLayout layout = fragments[t].getLayout();
            layout.removeAllViews();

            ReplacementTable table = tables[t];
            if (table != null) {
                // Rename appbar title to possibly new grade
                if (!alreadyRenamedAppbar) {
                    ActionBar bar = getSupportActionBar();
                    if (bar != null) {
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
                ArrayList<Replacement> replacements = new ArrayList<>(table.getReplacements());
                ArrayList<Message> messages = new ArrayList<>(table.getMessages());
                String[] dates = table.getDates();
                String[] days = table.getDays();

                for (int i = 0; i < dates.length; i++) {
                    if (layout.getChildCount() != 0) {
                        layout.addView(createSpacer());
                    }

                    // Setup card view with layout
                    CardView card = createCard();
                    LinearLayout cardLayout = createCardLayout();

                    String date = dates[i];
                    String day = days[i];
                    // Add date view to card
                    cardLayout.addView(createDateView(date, day));
                    // Replacements and Messages are sorted by date, which means that the first replacement/message
                    // in the list is always the earliest replacement/message

                    // Add messages
                    boolean addedMessages = false;

                    while (!messages.isEmpty() && messages.get(0).getDate().equals(date)) {
                        cardLayout.addView(createMessageView(messages.remove(0)));
                        addedMessages = true;
                    }

                    // Add replacements
                    boolean addedCaption = false;

                    while (!replacements.isEmpty() && replacements.get(0).getDate().equals(date)) {
                        if (!addedCaption) {
                            cardLayout.addView(createCaptionView());
                            addedCaption = true;
                        }

                        cardLayout.addView(createLine());
                        cardLayout.addView(createReplacementView(replacements.remove(0)));
                    }

                    // If no replacements were added, but messages were added
                    // add a tiny spacer so the message's TextView doesn't almost
                    // touch the black line afterwards
                    if (!addedCaption && addedMessages) {
                        // Create small spacer between line and message to make it look better
                        View view = new View(this);
                        view.setBackgroundColor(Color.TRANSPARENT);
                        view.setMinimumHeight(dpToPx(8));
                        cardLayout.addView(view);
                    }

                    // If no replacements were added, add noReplacementsView
                    if (!addedCaption) {
                        cardLayout.addView(createLine());
                        cardLayout.addView(createNoReplacementsView());
                    }

                    // Add card to layout
                    card.addView(cardLayout);
                    layout.addView(card);
                }
            } else {
                layout.addView(createNoTableView());
            }
        }
    }

    private void enableSwipeRefresh() {
        for (TableFragment fragment : fragments) {
            fragment.getRefreshLayout().setEnabled(true);
        }
    }

    private void disableSwipeRefresh() {
        for (TableFragment fragment : fragments) {
            fragment.getRefreshLayout().setEnabled(false);
        }
    }

    private void showProgressBar(boolean swipeRefresh) {
        for (TableFragment fragment : fragments) {
            LinearLayout layout = fragment.getLayout();
            layout.removeAllViews();

            if (!swipeRefresh) {
                layout.addView(createProgressBar());
            }
        }
    }

    private void hideProgressBar() {
        for (TableFragment fragment : fragments) {
            fragment.getRefreshLayout().setRefreshing(false);
        }
    }

    private View createProgressBar() {
        ProgressBar bar = new ProgressBar(this);
        bar.getIndeterminateDrawable()
                .setColorFilter(BlendModeColorFilterCompat
                        .createBlendModeColorFilterCompat(theme.getIndicatorColor(), BlendModeCompat.SRC_IN));
        bar.setMinimumHeight(dpToPx(32));
        return bar;
    }

    private View createNoTableView() {
        TextView text = new TextView(this);
        text.setMinHeight(dpToPx(32));
        text.setGravity(Gravity.CENTER);
        text.setTextColor(theme.getTextColor());
        text.setText(R.string.no_table);
        return text;
    }

    // Creates a grey thin horizontal line
    private View createLine() {
        View view = new View(this);
        int width = getResources().getDisplayMetrics().widthPixels - dpToPx(32);
        view.setLayoutParams(new ViewGroup.LayoutParams(width, dpToPx(1)));
        view.setBackgroundColor(Color.GRAY);
        return view;
    }

    // Creates a view with table captions for the replacements
    private View createCaptionView() {
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

        for (int i = 0; i < data.length; i++) {
            // Give text component of replacement more space
            int textWidth = i != data.length - 1 ? maxWidth - dpToPx(8) : maxWidth + dpToPx(32);
            // Setup text
            TextView text = new TextView(this);
            text.setLayoutParams(new ViewGroup.LayoutParams(textWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
            text.setTypeface(Typeface.DEFAULT_BOLD);
            text.setTextColor(theme.getTextColor());
            text.setText(data[i]);
            text.setGravity(Gravity.CENTER);
            view.addView(text);
        }

        return view;
    }

    // Creates a view containing the message's text
    private View createMessageView(Message message) {
        TextView text = new TextView(this);
        text.setGravity(Gravity.CENTER);
        text.setTextColor(theme.getTextColor());
        text.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);

        String messageText = message.getText();
        // Mark text red if the message indicates that something was cancelled
        if (messageText.indexOf("fällt") < messageText.indexOf("aus")) {
            SpannableStringBuilder str = new SpannableStringBuilder(messageText);
            int color = theme.getImportantTextColor();

            List<Integer> f_indexes = Utils.indexesOf(messageText, "fällt");
            List<Integer> a_indexes = Utils.indexesOf(messageText, "aus");

            if (f_indexes.size() == a_indexes.size()) {
                int size = f_indexes.size();
                for (int i = 0; i < size; i++) {
                    int f_start = f_indexes.get(i);
                    int a_start = a_indexes.get(i);

                    if (f_start < a_start) {
                        int f_end = f_start + "fällt".length();
                        int a_end = a_start + "aus".length();

                        str.setSpan(new ForegroundColorSpan(color), f_start, f_end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        str.setSpan(new ForegroundColorSpan(color), a_start, a_end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }

            text.setText(str);
        } else {
            text.setText(messageText);
        }

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
    private View createReplacementView(Replacement replacement) {
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

        for (int i = 3; i < data.length; i++) {
            // Give text component of replacement more space
            int textWidth = i != data.length - 1 ? maxWidth - dpToPx(8) : maxWidth + dpToPx(32);
            // Setup text
            TextView text = new TextView(this);
            text.setLayoutParams(new ViewGroup.LayoutParams(textWidth, ViewGroup.LayoutParams.WRAP_CONTENT));

            if (i == ReplacementFilter.TEXT.ordinal()) {
                // Mark text red if the replacements indicates that something was cancelled
                if (data[i].contains("fällt aus")) {
                    SpannableStringBuilder str = new SpannableStringBuilder(data[i]);
                    ForegroundColorSpan color = new ForegroundColorSpan(theme.getImportantTextColor());
                    int start = data[i].indexOf("fällt aus");
                    int end = start + "fällt aus".length();
                    str.setSpan(color, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    text.setText(str);
                } else {
                    text.setText(data[i]);
                }
            } else {
                text.setText(data[i]);
            }

            text.setTextColor(theme.getTextColor());
            text.setGravity(Gravity.CENTER);
            view.addView(text);
        }

        return view;
    }

    // Copies the given text into the primary clipboard
    private void copyIntoClipboard(String text, Runnable onSuccess) {
        ClipboardManager manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData data = ClipData.newPlainText("Vertretungsplan", text);
        if (manager != null) {
            manager.setPrimaryClip(data);
            if (onSuccess != null) {
                runOnUiThread(onSuccess);
            }
        } else {
            Toast.makeText(this, getString(R.string.error_message), Toast.LENGTH_SHORT).show();
        }
    }

    // Creates a view which shows the user that there aren't any replacements.
    private View createNoReplacementsView() {
        TextView text = new TextView(this);
        text.setMinHeight(dpToPx(32));
        text.setGravity(Gravity.CENTER);
        text.setTextColor(theme.getTextColor());
        text.setText(R.string.no_replacements);
        return text;
    }

    // Creates a view containing the replacement's/message's date
    private View createDateView(String date, String day) {
        TextView text = new TextView(this);
        text.setMinHeight(dpToPx(32));
        text.setTextColor(theme.getTextColor());
        text.setGravity(Gravity.CENTER);

        // Make the day bold
        String str = String.format("%s - %s", day, date);
        SpannableStringBuilder txt = new SpannableStringBuilder(str);
        txt.setSpan(new StyleSpan(Typeface.BOLD), 0, str.indexOf(" "), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        text.setText(txt);
        return text;
    }

    // Creates a transparent view which is used to separate the cards
    private View createSpacer() {
        View view = new View(this);
        view.setMinimumHeight(dpToPx(10));
        view.setBackgroundColor(Color.TRANSPARENT);
        return view;
    }

    // Creates a beautiful CardView which functions as a container for all other views
    private CardView createCard() {
        CardView card = new CardView(this);
        //card.setMinimumHeight(dpToPx(32));
        card.setCardElevation(dpToPx(1));
        card.setRadius(dpToPx(preferences.getBoolean(getString(R.string.saved_rounded_corners), true) ? 8 : 0));
        card.getBackground().setColorFilter(BlendModeColorFilterCompat
                .createBlendModeColorFilterCompat(theme.getCardColor(), BlendModeCompat.SRC));
        return card;
    }

    private LinearLayout createCardLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        return layout;
    }

    // Opens the preference screen
    private void openSettings(boolean withDialog) {
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtra("with_dialog", withDialog);
        startActivity(intent);
    }

    // Convert density pixels to real pixels
    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}
