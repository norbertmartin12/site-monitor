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
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import org.site_monitor.BuildConfig;
import org.site_monitor.activity.PrefSettingsActivity;
import org.site_monitor.model.db.DBHelper;
import org.site_monitor.service.AlarmJobService;
import org.site_monitor.service.JobEnum;
import org.site_monitor.service.SharedPreferencesService;

import java.sql.SQLException;
import java.util.Date;

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
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "startAlarmIfNeeded does nothing - already started");
            }
            return true;
        }
        if (countSites(context) > 0) {
            scheduleAlarm(context, getAlarmInterval(context));
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "startAlarmIfNeeded - started");
            }
            return true;
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "startAlarmIfNeeded does nothing - no sites");
        }
        return false;
    }

    private long getAlarmInterval(Context context) {
        String intervalString = PreferenceManager.getDefaultSharedPreferences(context).getString(PrefSettingsActivity.FREQUENCY, "60");
        // PATCH 0.14 - REMOVE 5 VALUE
        if ("5".equals(intervalString)) {
            PreferenceManager.getDefaultSharedPreferences(context).edit().putString(PrefSettingsActivity.FREQUENCY, "15").apply();
            return AlarmManager.INTERVAL_FIFTEEN_MINUTES;
        }
        // SECURITY IF NOT SET
        if (Long.parseLong(intervalString) <= 0) {
            PreferenceManager.getDefaultSharedPreferences(context).edit().putString(PrefSettingsActivity.FREQUENCY, "60").apply();
            return AlarmManager.INTERVAL_HOUR;
        }
        return Long.parseLong(intervalString) * TimeUtil.MINUTE_2_MILLISEC;
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
        killAlarm(context, JobEnum.CHECK_SITES.ordinal());
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

    private void scheduleAlarm(Context context, long interval) {
        ComponentName serviceComponent = new ComponentName(context, AlarmJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(JobEnum.CHECK_SITES_JS.ordinal(), serviceComponent);
        builder.setPeriodic(interval);
        builder.setPersisted(true);
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            if (jobScheduler.schedule(builder.build()) == JobScheduler.RESULT_FAILURE) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "failed to schedule alarm every: " + currentInterval + "min (" + interval + ")");
                }
                currentInterval = null;
            } else {
                currentInterval = interval / TimeUtil.MINUTE_2_MILLISEC;
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "scheduled alarm every: " + currentInterval + "min (" + interval + ")");
                }
            }
            updateNextAlarmDate(context);
        }
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
            Log.i(TAG, "updateNextAlarmDate: " + new Date(nextAlarm));
        }
        BroadcastUtil.broadcast(context, ACTION_NEXT_ALARM_SET, BroadcastUtil.EXTRA_ALARM, nextAlarm);
    }

    /**
     * Kills current alarm an notifies this update
     *
     * @param context
     * @param jobId
     */
    public void killAlarm(Context context, int jobId) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            jobScheduler.cancel(jobId);
            currentInterval = null;
            updateNextAlarmDate(context);
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "killAlarm jobSchedule: " + jobId);
            }
        }
    }

    /**
     * @return true if has current alarm set
     */
    private boolean hasAlarm() {
        return currentInterval != null;
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

    /**
     * @param context
     * @return next alarm timestamp
     */
    public long getNextAlarmTime(Context context) {
        return SharedPreferencesService.getLongNow(context, KEY_NEXT_ALARM);
    }

    /**
     * @param context
     * @return next alarm delay
     */
    public long getCountUntilNextAlarmTime(Context context) {
        return getNextAlarmTime(context) - System.currentTimeMillis();
    }

    /**
     * @return current alarm interval, or null if no alarm set
     */
    public Long getCurrentInterval() {
        return currentInterval;
    }

}
