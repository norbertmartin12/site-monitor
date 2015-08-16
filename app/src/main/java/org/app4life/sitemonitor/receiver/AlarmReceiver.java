/*
 * Copyright (c) 2015 Martin Norbert
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

package org.app4life.sitemonitor.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import org.app4life.sitemonitor.BuildConfig;
import org.app4life.sitemonitor.activity.PrefSettingsActivity;
import org.app4life.sitemonitor.service.NetworkService;
import org.app4life.sitemonitor.util.TimeUtil;

import java.util.Date;
import java.util.Random;

public class AlarmReceiver extends WakefulBroadcastReceiver {

    public static final String TAG = "AlarmReceiver";
    private static final Random RANDOM = new Random(new Date().getTime());

    private static PendingIntent pendingIntent;
    private static String currentInterval;

    public static PendingIntent startAlarm(Context context) {
        if (hasAlarm()) {
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "startAlarm: already set");
            }
            return pendingIntent;
        }
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "startAlarm");
        }
        String intervalString = PreferenceManager.getDefaultSharedPreferences(context).getString(PrefSettingsActivity.FREQUENCY, null);
        long interval = AlarmManager.INTERVAL_HOUR;
        if (intervalString != null) {
            interval = Long.parseLong(intervalString) * TimeUtil.MINUTE_2_MILLISEC;
        }

        return scheduleAlarm(context, interval);
    }

    public static void stopAlarm(Context context) {
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
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "stop alarm");
        }
    }

    public static void rescheduleAlarm(Context context, Long newFrequency) {
        if (hasAlarm()) {
            stopAlarm(context);
        } else {
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "rescheduleAlarm: none started");
            }
        }
        scheduleAlarm(context, newFrequency);
    }

    public static boolean hasAlarm() {
        return pendingIntent != null;
    }

    public static String getCurrentInterval() {
        return currentInterval;
    }

    private static PendingIntent scheduleAlarm(Context context, long interval) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent newPendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(context, AlarmReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, interval, interval, newPendingIntent);
        currentInterval = interval / TimeUtil.MINUTE_2_MILLISEC + "min";
        pendingIntent = newPendingIntent;
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "schedule alarm every: " + currentInterval + " (" + interval + ")");
        }
        return pendingIntent;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "onReceive");
            }
            Thread.sleep(RANDOM.nextInt(10) * 100);
            WakefulBroadcastReceiver.startWakefulService(context, NetworkService.getIntent(context));
        } catch (InterruptedException e) {
            if (BuildConfig.DEBUG) {
                Log.wtf(TAG, e);
            }
        }
    }

}
