package com.stonedroid.mpgvertretungsplan;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
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
import android.util.TypedValue;
import android.view.*;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import de.stonedroid.vertretungsplan.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity
{
    public static final String TAG = MainActivity.class.getSimpleName();
    public static final boolean DEBUG = true;

    private static final String TABLE_1_BIN = "table1.dat";
    private static final String TABLE_2_BIN = "table2.dat";

    // It's important to store this listener in a global field, otherwise gc will delete it.
    // (For some reasons, the android developers thought it was a good idea to store listeners in
    // a WeakHashMap, where objects without any connections to other objects are deleted.)
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;
    private SharedPreferences preferences;

    private ReplacementTable[] tables = null;
    private TableFragment[] fragments = null;
    private CoordinatorLayout mainLayout;

    private int tabTextColor;
    private int textColor;
    private int importantTextColor;
    private int cardColor;
    private int layoutColor;

    // Writes in the debug log if bool DEBUG is true
    private void log(String message)
    {
        if (DEBUG)
        {
            Log.d(TAG, message);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Set theme (with customizations)
        String themeName = Utils.setCustomTheme(this);
        switch (themeName)
        {
            case "Orange":
                textColor = Color.BLACK;
                importantTextColor = Color.RED;
                tabTextColor = Color.WHITE;
                cardColor = Color.parseColor("#fdecd9");
                layoutColor = Color.parseColor("#f3f3f3");
                break;
            case "Weiß":
                textColor = Color.BLACK;
                importantTextColor = Color.RED;
                tabTextColor = Color.BLACK;
                cardColor = Color.WHITE;
                layoutColor = Color.LTGRAY;
        }

        setContentView(R.layout.activity_main);

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
                fragments[i] = new TableFragment();
            }
            else
            {
                // Get recycled fragments back
                fragments[i] = (TableFragment) getSupportFragmentManager()
                        .findFragmentByTag(getFragmentTag(R.id.view_pager, i));
            }
        }

        fragments[1].setOnFragmentCreatedListener(this::onCreate2);
        // Setup ViewPager with TabLayout
        CharSequence[] titles = {getCalendarSpan(0), getCalendarSpan(1)};
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(new ViewPagerAdapter(getSupportFragmentManager(),
                fragments, titles));

        TabLayout tabLayout = findViewById(R.id.tab_layout);
        tabLayout.setBackgroundColor(getThemePrimaryColor());
        tabLayout.setTabTextColors(tabTextColor - (70 << 24), tabTextColor);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            tabLayout.setElevation(elevation);
        }

        tabLayout.setupWithViewPager(viewPager);
    }

    // Is executed after fragments were instantiated
    private void onCreate2()
    {
        // Register listener to be noticed if user changes something in the settings
        preferenceChangeListener = (sharedPrefs, s) ->
        {
            // s = (String) key of the changed value
            log("Key of changed value: " + s);
            if (s.equals(getString(R.string.saved_grade)))
            {
                // Load new table
                downloadTableAndShow(Grade.parse(sharedPrefs.getString(s, "")), true);
            }
            else if(s.contains("filter_enabled"))
            {
                // Always show the table from scratch if filter settings were altered
                if (tables != null)
                {
                    showTables(tables);
                }
            }
            else if(s.equals("theme"))
            {
                // To change the theme, recreate the activity
                recreate();
            }
        };
        preferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        boolean firstTime = preferences.getBoolean(getString(R.string.saved_first_time), true);
        if (firstTime)
        {
            // Now user opened the app one time
            // -> set boolean to false
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(getString(R.string.saved_first_time), false);
            editor.apply();

            // TODO: Rewrite welcome dialog
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
                        downloadTableAndShow(Grade.parse(grade), false);
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
                    }
                }
            }
            else
            {
                openSettings(true);
            }
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (tables != null)
        {
            new Thread(() ->
            {
                try
                {
                    String path = getFilesDir().getAbsolutePath();
                    if (!path.endsWith("/"))
                    {
                        path += "/";
                    }

                    log("Writing to " + path);
                    // Save tables
                    Utils.saveObject(tables[0], path + TABLE_1_BIN);
                    Utils.saveObject(tables[1], path + TABLE_2_BIN);
                    // Next time, we can open the offline view
                    preferences.edit()
                            .putBoolean(getString(R.string.saved_offline_available), true)
                            .apply();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    // Load tables async from storage
    private void loadTablesFromStorage(boolean showTables)
    {
        new Thread(() ->
        {
            try
            {
                String path = getFilesDir().getAbsolutePath();
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
                .setPositiveButton("OK", (dialog, which) -> openSettings(true))
                .create();
    }

    // Returns a nice readable string like (01.01 - 05.01)
    private String getCalendarSpan(int plusWeeks)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.WEEK_OF_YEAR, plusWeeks);
        // Set calendar to Monday
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        String part1 = String.format("%s.%s", calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.MONTH) + 1);
        // Set calendar to Friday
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
        String part2 = String.format("%s.%s", calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.MONTH) + 1);
        // Build formatted string
        return String.format("%s - %s", part1, part2);
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
                Grade grade = Grade.parse(preferences.getString(getString(R.string.saved_grade), ""));
                if (grade != null)
                {
                    downloadTableAndShow(grade, true);
                }
                else
                {
                    openSettings(true);
                }
        }

        return super.onOptionsItemSelected(item);
    }

    // Downloads both ReplacementTables (for this and the next week) async
    // and adds them after downloading to the MainLayout
    // without blocking the UI thread.
    private void downloadTableAndShow(Grade grade, boolean reload)
    {
        // Check if we have a valid network connection
        if (isNetworkAvailable())
        {
            new Thread(() ->
            {
                tables = new ReplacementTable[2];
                try
                {
                    tables[0] = ReplacementTable.downloadTable(grade, 0);
                }
                catch (WebException e)
                {
                    log("Couldn't download ReplacementTable#1");
                }

                try
                {
                    tables[1] = ReplacementTable.downloadTable(grade, 1);
                }
                catch (WebException e)
                {
                    log("Couldn't download ReplacementTable#2");
                }

                runOnUiThread(() ->
                {
                    showTables(tables);
                    if (reload)
                    {
                        Snackbar.make(mainLayout, getString(R.string.reloaded_table), Snackbar.LENGTH_SHORT)
                                .show();
                    }
                });
            }).start();
        }
        else
        {
            // Network is not available...
            Snackbar.make(mainLayout, R.string.no_internet, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.retry, v -> downloadTableAndShow(grade, reload))
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
                    }

                    alreadyRenamedAppbar = true;
                }

                // Get all necessary information
                ArrayList<Replacement> replacements = PreferenceManager.getDefaultSharedPreferences(this)
                        .getBoolean(getString(R.string.saved_filter_enabled), false)
                        ? filterReplacements(table.getReplacements())
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

    private ArrayList<Replacement> filterReplacements(List<Replacement> replacements)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Load all subject
        Map<String, List<String>> subjects = Subject.getAllSubjects();
        Set<String> subjectNames = subjects.keySet();

        ArrayList<Replacement> filteredReplacements = new ArrayList<>();

        for (Replacement replacement : replacements)
        {
            try
            {
                String subjectName = Subject.getSubjectName(replacement.getOldSubject());
                // Look at userPrefs what to do next
                String course = prefs.getString("filter_enabled_" + subjectName, null);
                if (course == null || course.equals("Ignorieren") || course.equals(replacement.getOldSubject()))
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
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        container.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        View view = new View(this);
        int width = getResources().getDisplayMetrics().widthPixels - dpToPx(32);
        view.setLayoutParams(new ViewGroup.LayoutParams(width, dpToPx(1)));
        view.setBackgroundColor(Color.GRAY);
        container.addView(view);
        return container;
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

        // TODO: Copy replacement to clipboard when onLongClick
        view.setOnLongClickListener(v ->
        {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Vertretungsplan", replacement.toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Vertretung wurde kopiert", Toast.LENGTH_SHORT).show();
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

            if (i == data.length - 1)
            {
                /*// Dirty fix of "fÃ¤llt aus" in replacement message (don't know why this happens)
                if (data[i].equals("fÃ¤llt aus"))
                {
                    data[i] = "fällt aus";
                }*/

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
        // Make date look nice
        if (date.endsWith("."))
        {
            date = date.substring(0, date.length() - 1);
        }

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
        card.setCardElevation(dpToPx(2));
        card.setRadius(dpToPx(8));
        Drawable background = card.getBackground();
        background.setColorFilter(cardColor, PorterDuff.Mode.SRC);
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

    public int getThemePrimaryColor()
    {
        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(R.attr.colorPrimary, value, true);
        return value.data;
    }
}
