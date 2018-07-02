package com.stonedroid.mpgvertretungsplan;

import android.app.AlarmManager;
import android.app.PendingIntent;
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
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.*;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.stonedroid.vertretungsplan.*;

import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity
{
    public static final String TAG = MainActivity.class.getSimpleName();
    public static final boolean DEBUG = true;

    // It's important to store this listener in a global field, otherwise gc will delete it.
    // (For some reasons, the android developers thought it was a good idea to store listeners in
    // a WeakHashMap, where objects without any connections to other objects are deleted.)
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;

    private ReplacementTable[] tables;
    private TableFragment[] fragments;
    private CoordinatorLayout mainLayout;

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
        setContentView(R.layout.activity_main);

        /*Intent intent = new Intent(this, NotificationService.class);
        PendingIntent sender = PendingIntent.getService(this, 0, intent, 0);

        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, 1000, sender);*/

        // Get mainLayout
        mainLayout = findViewById(R.id.main_layout);

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
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferenceChangeListener = (sharedPrefs, s) ->
        {
            // s = (String) key of the changed value
            log("Key of changed value: " + s);
            if (s.equals(getString(R.string.saved_grade)))
            {
                downloadTableAndShow(Grade.parse(sharedPrefs.getString(s, "")));
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

            // TODO: Write welcome logic
            log("First time!");
            openSettings();
        }
        else
        {
            // TODO: Write normal logic
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
                    // Download from internet
                    log("Download tables");
                    downloadTableAndShow(Grade.parse(grade));
                }
            }
            else
            {
                openSettings();
            }
        }
    }

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

    private boolean isNetworkAvailable()
    {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance()
    {
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
                openSettings();
        }

        return super.onOptionsItemSelected(item);
    }

    private void downloadTableAndShow(Grade grade)
    {
        // Check if we have a valid network connection
        if (isNetworkAvailable())
        {
            // TODO: Rework for two fragments
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

                runOnUiThread(() -> showTables(tables));
            }).start();
        }
        else
        {
            // Network is not available...
            Snackbar.make(mainLayout, R.string.no_internet, BaseTransientBottomBar.LENGTH_INDEFINITE)
                    .setAction(R.string.retry, (view) -> downloadTableAndShow(grade))
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
        // TODO: Rework for two fragments
        // Set AppBar title
        ActionBar bar = getSupportActionBar();
        if (bar != null)
        {
            bar.setTitle(String.format("Klasse %s", tables[0].getGrade().toString()));
        }

        for (int t = 0; t < tables.length; t++)
        {
            // Setup layout
            LinearLayout layout = fragments[t].getLayout();
            layout.removeAllViews();

            ReplacementTable table = tables[t];
            if (table != null)
            {
                // Get all necessary information
                ArrayList<Replacement> replacements = new ArrayList<>(table.getReplacements());
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
                    cardLayout.addView(createDate(date, day));
                    // Replacements and Messages are sorted by date, which means that the first replacement/message
                    // in the list is always the earliest replacement/message

                    // Add messages
                    while (!messages.isEmpty() && messages.get(0).getDate().equals(date))
                    {
                        cardLayout.addView(createMessageView(messages.remove(0)));
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
                // Show that the ReplacementTable is not available
                /*CardView card = createCard();
                LinearLayout cardLayout = new LinearLayout(this);
                cardLayout.setOrientation(LinearLayout.VERTICAL);
                cardLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                cardLayout.addView(createNoTableView());
                card.addView(cardLayout);
                layout.addView(card);*/
                layout.addView(createNoTableView());
            }
        }
    }

    private View createNoTableView()
    {
        TextView text = new TextView(this);
        text.setMinHeight(dpToPx(32));
        text.setGravity(Gravity.CENTER);
        text.setTextColor(Color.BLACK);
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

    // Creates a view with captions for the replacements
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
            text.setTextColor(Color.BLACK);
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
        text.setMinHeight(dpToPx(32));
        text.setGravity(Gravity.CENTER);
        text.setTextColor(Color.BLACK);
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
            if (i != data.length - 1)
            {
                text.setTextColor(Color.BLACK);
            }
            else
            {
                // Mark text red if the replacements indicates that something was cancelled
                if (data[i].equals("fällt aus"))
                {
                    text.setTextColor(Color.RED);
                }
                else
                {
                    text.setTextColor(Color.BLACK);
                }
            }
            text.setText(data[i]);
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
        text.setTextColor(Color.BLACK);
        text.setText(R.string.no_replacements);
        return text;
    }

    // Creates a view containing the replacement's/message's date
    private View createDate(String date, String day)
    {
        // Make date look nice
        if (date.endsWith("."))
        {
            date = date.substring(0, date.length() - 1);
        }

        TextView text = new TextView(this);
        text.setMinHeight(dpToPx(32));
        text.setTextColor(Color.BLACK);
        text.setText(String.format("%s - %s", day, date));
        text.setGravity(Gravity.CENTER);
        return text;
    }

    // Creates a transparent view which is used to separate to cards
    private View createSpacer()
    {
        View view = new View(this);
        view.setMinimumHeight(dpToPx(16));
        view.setBackgroundColor(Color.TRANSPARENT);
        return view;
    }

    // Creates a beautiful CardView which functions as a container for all other views
    private CardView createCard()
    {
        CardView card = new CardView(this);
        //card.setMinimumHeight(dpToPx(32));
        card.setCardElevation(dpToPx(2));
        Drawable background = card.getBackground();
        background.setColorFilter(Color.parseColor("#fdecd9"), PorterDuff.Mode.SRC);
        return card;
    }

    // Opens the preference screen
    private void openSettings()
    {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    // Convert density pixels to real pixels
    private int dpToPx(float dp)
    {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}
