/*    
    This file is part of Gmail Notifier.
    Copyright 2010 Jonas Larsson, jonas@hallerud.se

    Gmail Notifier is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gmail Notifier is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Gmail Notifier. If not, see <http://www.gnu.org/licenses/>.

 */

package com.jonasl.gmailnotifier2;

import java.util.Calendar;
import com.jonasl.gmailnotifier2.R;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceClickListener;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.TimePicker;

public class PrefsActivity extends PreferenceActivity implements OnPreferenceClickListener {
    final static String TEST_ACCOUNT = "test@gmail.com";

    final static int VERSION = 2;

    final static int DISABLE_RINGTONE = 1;

    final static int DISABLE_VIBRATION = 2;

    final static int DISABLE_LED = 4;

    private final static int DEFAULT_SCHEDULE_START_HOUR = 22;

    private final static int DEFAULT_SCHEDULE_START_MINUTE = 0;

    private final static int DEFAULT_SCHEDULE_STOP_HOUR = 8;

    private final static int DEFAULT_SCHEDULE_STOP_MINUTE = 0;

    // Activity started and is shown to user
    static boolean sStarted = false;

    private String mAccount = null;

    private int mShownDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = this.getIntent();
        if (intent.hasExtra("account")) {
            mAccount = intent.getStringExtra("account").toLowerCase();
            upgradePreferences(getSharedPreferences(mAccount, 0));
            setTitle(mAccount);
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("hasaccount",
                    true).commit();
        } else {
            mAccount = TEST_ACCOUNT;
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("hasaccount",
                    false).commit();
        }

        getPreferenceManager().setSharedPreferencesName(mAccount);

        // Load preference elements from XML
        addPreferencesFromResource(R.xml.prefs);

        // Add listeners to the schedule start/stop screens
        PreferenceScreen ps = (PreferenceScreen) findPreference("schedulestart");
        ps.setOnPreferenceClickListener(this);
        ps = (PreferenceScreen) findPreference("schedulestop");
        ps.setOnPreferenceClickListener(this);
        updateScheduleSummaries();

        if (!supportsPriorityInbox(this)) {
            getPreferenceScreen().removePreference(findPreference("cat_priority"));
        }
    }

    public static SharedPreferences getAccountPreferences(Context context, String account) {
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("hasaccount", true)) {
            return context.getSharedPreferences(account.toLowerCase(), 0);
        } else {
            return context.getSharedPreferences(TEST_ACCOUNT, 0);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().equals("schedulestart")) {
            showDialog(0);
        } else if (preference.getKey().equals("schedulestop")) {
            showDialog(1);
        } else if (preference.getKey().equals("priority")) {
            // Reset unread count when switching mailboxes. They might differ.
            getPreferenceManager().getSharedPreferences().edit().putInt("unreadcount", 0).commit();
        }
        return true;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case 0:
            case 1:
                return new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        switch (mShownDialog) {
                            case 0:
                                getPreferenceManager().getSharedPreferences().edit().putInt(
                                        "schedulestarth", hourOfDay).putInt("schedulestartm",
                                        minute).commit();
                                break;
                            case 1:
                                getPreferenceManager().getSharedPreferences().edit().putInt(
                                        "schedulestoph", hourOfDay).putInt("schedulestopm", minute)
                                        .commit();
                        }
                        updateScheduleSummaries();
                    }
                }, 0, 0, DateFormat.is24HourFormat(this));
        }
        return null;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        TimePickerDialog tpd = (TimePickerDialog) dialog;
        int h = 0, m = 0;
        switch (id) {
            case 0:
                h = getScheduleStartHour();
                m = getScheduleStartMinute();
                break;
            case 1:
                h = getScheduleStopHour();
                m = getScheduleStopMinute();
                break;
        }
        tpd.updateTime(h, m);
        mShownDialog = id;
    }

    @Override
    public void onStart() {
        super.onStart();
        sStarted = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        sStarted = false;
        GmailReceiver.exitAsNeeded();
    }

    private void updateScheduleSummaries() {
        Calendar c = Calendar.getInstance();
        PreferenceScreen ps = (PreferenceScreen) findPreference("schedulestart");
        c.set(Calendar.HOUR_OF_DAY, getScheduleStartHour());
        c.set(Calendar.MINUTE, getScheduleStartMinute());
        ps.setSummary(DateFormat.getTimeFormat(this).format(c.getTime()));
        ps = (PreferenceScreen) findPreference("schedulestop");
        c.set(Calendar.HOUR_OF_DAY, getScheduleStopHour());
        c.set(Calendar.MINUTE, getScheduleStopMinute());
        ps.setSummary(DateFormat.getTimeFormat(this).format(c.getTime()));
    }

    private int getScheduleStartHour() {
        return getPreferenceManager().getSharedPreferences().getInt("schedulestarth",
                DEFAULT_SCHEDULE_START_HOUR);
    }

    private int getScheduleStartMinute() {
        return getPreferenceManager().getSharedPreferences().getInt("schedulestartm",
                DEFAULT_SCHEDULE_START_MINUTE);
    }

    private int getScheduleStopHour() {
        return getPreferenceManager().getSharedPreferences().getInt("schedulestoph",
                DEFAULT_SCHEDULE_STOP_HOUR);
    }

    private int getScheduleStopMinute() {
        return getPreferenceManager().getSharedPreferences().getInt("schedulestopm",
                DEFAULT_SCHEDULE_STOP_MINUTE);
    }

    static void upgradePreferences(SharedPreferences prefs) {
        if (prefs == null) {
            return;
        }

        int prefsVersion = prefs.getInt("version", 1);

        if (prefsVersion < VERSION) {
            Log.d(GmailReceiver.TAG, "Settings version " + prefsVersion + " Expected version "
                    + VERSION);
            String intervalString = prefs.getString("interval", "once");
            int interval = -1;
            if (intervalString.equals("once"))
                interval = 0;
            else if (intervalString.equals("10s"))
                interval = 10 * 1000;
            else if (intervalString.equals("30s"))
                interval = 30 * 1000;
            else if (intervalString.equals("1m"))
                interval = 1 * 60 * 1000;
            else if (intervalString.equals("2m"))
                interval = 2 * 60 * 1000;
            else if (intervalString.equals("3m"))
                interval = 3 * 60 * 1000;
            else if (intervalString.equals("4m"))
                interval = 4 * 60 * 1000;
            else if (intervalString.equals("5m"))
                interval = 5 * 60 * 1000;
            else if (intervalString.equals("10m"))
                interval = 10 * 60 * 1000;
            else if (intervalString.equals("30m"))
                interval = 30 * 60 * 1000;

            if (interval >= 0) {
                Log.d(GmailReceiver.TAG, "Changing interval to " + interval);
                prefs.edit().putString("interval", String.valueOf(interval)).commit();
            }

            if (interval > 0) {
                Log.d(GmailReceiver.TAG, "Setting repeats to 100");
                prefs.edit().putString("repeats", "100").commit();
            }

            prefs.edit().putInt("version", 2).commit();
        }
    }

    public static int getScheduledProperties(SharedPreferences prefs) {
        int res = 0;
        if (prefs.getBoolean("schedule", false)) {
            // Construct Calendars to determine if should disable
            Calendar now = Calendar.getInstance();
            Calendar start = Calendar.getInstance();
            Calendar stop = Calendar.getInstance();
            start.set(Calendar.HOUR_OF_DAY, prefs.getInt("schedulestarth",
                    DEFAULT_SCHEDULE_START_HOUR));
            start.set(Calendar.MINUTE, prefs
                    .getInt("schedulestartm", DEFAULT_SCHEDULE_START_MINUTE));
            stop.set(Calendar.HOUR_OF_DAY, prefs
                    .getInt("schedulestoph", DEFAULT_SCHEDULE_STOP_HOUR));
            stop.set(Calendar.MINUTE, prefs.getInt("schedulestopm", DEFAULT_SCHEDULE_STOP_MINUTE));
            // If stop occurs before start if should be the next day
            if (stop.before(start)) {
                // Before or after midnight?
                if (start.before(now)) {
                    stop.add(Calendar.HOUR_OF_DAY, 24);
                } else {
                    start.add(Calendar.HOUR_OF_DAY, -24);
                }
            }
            if (now.after(start) && now.before(stop)) {
                // We are in the scheduled disable period
                if (prefs.getBoolean("schedulering", true)) {
                    res |= DISABLE_RINGTONE;
                }
                if (prefs.getBoolean("schedulevibration", true)) {
                    res |= DISABLE_VIBRATION;
                }
                if (prefs.getBoolean("scheduleled", true)) {
                    res |= DISABLE_LED;
                }
            }
        }
        return res;
    }

    public static boolean supportsPriorityInbox(Context context) {
        int version = 0;
        try {
            version = context.getPackageManager().getPackageInfo("com.google.android.gm", 0).versionCode;
        } catch (Exception e) {
            // Ignore
        }
        return version >= 156;
    }
}
