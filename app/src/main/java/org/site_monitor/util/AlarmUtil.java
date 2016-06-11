/*
 * Copyright (c) 2016 Martin Norbert
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.site_monitor.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

import org.site_monitor.BuildConfig;
import org.site_monitor.activity.PrefSettingsActivity;
import org.site_monitor.model.db.DBHelper;
import org.site_monitor.receiver.AlarmReceiver;
import org.site_monitor.service.SharedPreferencesService;

import java.sql.SQLException;

/**
 * Created by Martin Norbert on 31/01/2016.
 */
public class AlarmUtil {

    public static final String ACTION_NEXT_ALARM_SET = "org.site_monitor.service.action.NEXT_ALARM_SET";
    private static final String TAG = AlarmUtil.class.getSimpleName();
    private static final String KEY_NEXT_ALARM = "org.site_monitor.nextAlarm";
    private static final AlarmUtil ALARM_SERVICE;

    static {
        ALARM_SERVICE = new AlarmUtil();
    }

    private PendingIntent pendingIntent;
    private Long currentInterval;

    private AlarmUtil() {
    }

    public static AlarmUtil instance() {
        return ALARM_SERVICE;
    }

    /**
     * Stops alarm if no site settings in database.
     *
     * @param context
     * @return true if stopped or all ready stopped, otherwise false
     */
    public boolean stopAlarmIfNeeded(Context context) {
        if (!hasAlarm()) {
            return true;
        }
        if (countSites(context) == 0) {
            stopAlarm(context);
            return true;
        }
        return false;
    }

    /**
     * Starts alarm if at least one site settings is in database.
     *
     * @param context
     * @return true if started or all ready started, otherwise false
     */
    public boolean startAlarmIfNeeded(Context context) {
        if (hasAlarm()) {
            return true;
        }
        if (countSites(context) > 0) {
            startAlarm(context);
            return true;
        }
        return false;
    }

    /**
     * Starts alarm if none set.
     *
     * @param context
     * @return
     */
    public PendingIntent startAlarm(Context context) {
        if (hasAlarm()) {
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "startAlarm: already set");
            }
            return pendingIntent;
        }

        String intervalString = PreferenceManager.getDefaultSharedPreferences(context).getString(PrefSettingsActivity.FREQUENCY, "60");
        if (intervalString == null || intervalString.isEmpty()) {
            return null;
        }
        long interval = AlarmManager.INTERVAL_HOUR;
        if (intervalString != null && !intervalString.isEmpty()) {
            interval = Long.parseLong(intervalString) * TimeUtil.MINUTE_2_MILLISEC;
        } else {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "intervalString is null, default interval: " + interval);
            }
        }
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "startAlarm");
        }
        return scheduleAlarm(context, interval);
    }

    /**
     * Stops alarm if exists
     *
     * @param context
     */
    public void stopAlarm(Context context) {
        if (!hasAlarm()) {
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "stopAlarm: already stopped");
            }
            return;
        }
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        pendingIntent = null;
        currentInterval = null;
        updateNextAlarmDate(context);
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "stop alarm");
        }
    }

    /**
     * Stops existing alarm if any and schedule next for given newFrequency
     *
     * @param context
     * @param newFrequency
     */
    public void rescheduleAlarm(Context context, Long newFrequency) {
        if (hasAlarm()) {
            stopAlarm(context);
        } else {
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "rescheduleAlarm: none started");
            }
        }
        scheduleAlarm(context, newFrequency);
    }

    /**
     * @return true if has current alarm set
     */
    public boolean hasAlarm() {
        return pendingIntent != null;
    }

    /**
     * @return current alarm interval, or null if no alarm set
     */
    public Long getCurrentInterval() {
        return currentInterval;
    }

    private PendingIntent scheduleAlarm(Context context, long interval) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent newPendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(context, AlarmReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, interval, interval, newPendingIntent);
        currentInterval = interval / TimeUtil.MINUTE_2_MILLISEC;
        pendingIntent = newPendingIntent;
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "schedule alarm every: " + currentInterval + " (" + interval + ")");
        }
        updateNextAlarmDate(context);
        return pendingIntent;
    }

    public void updateNextAlarmDate(Context context) {
        long nextAlarm = 0;
        if (currentInterval != null) {
            long intervalMilli = currentInterval * TimeUtil.MINUTE_2_MILLISEC;
            nextAlarm = System.currentTimeMillis() + intervalMilli;
            SharedPreferencesService.saveNow(context, KEY_NEXT_ALARM, nextAlarm);
        } else {
            // no next alarm
            SharedPreferencesService.saveNow(context, KEY_NEXT_ALARM, 0);
        }
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "updateNextAlarmDate: " + nextAlarm);
        }
        BroadcastUtil.broadcast(context, ACTION_NEXT_ALARM_SET, BroadcastUtil.EXTRA_ALARM, nextAlarm);
    }

    /**
     * Returns count of site settings in database.
     *
     * @param context
     * @return count or -1 if exception occurs
     */
    private long countSites(Context context) {
        DBHelper dbHelper = DBHelper.getHelper(context);
        try {
            return dbHelper.getDBSiteSettings().countOf();
        } catch (SQLException e) {
            Log.e(TAG, "countSites", e);
            return -1;
        } finally {
            dbHelper.release();
        }
    }

    public long getNextAlarmTime(Context context) {
        return SharedPreferencesService.getLongNow(context, KEY_NEXT_ALARM);
    }

    public long getCountUntilNextAlarmTime(Context context) {
        return getNextAlarmTime(context) - System.currentTimeMillis();
    }
}
