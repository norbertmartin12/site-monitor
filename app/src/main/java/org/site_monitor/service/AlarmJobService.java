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

package org.site_monitor.service;

import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.util.Log;
import android.util.Pair;

import org.site_monitor.BuildConfig;
import org.site_monitor.R;
import org.site_monitor.activity.MainActivity;
import org.site_monitor.model.bo.NetworkCallResult;
import org.site_monitor.model.bo.SiteCall;
import org.site_monitor.model.bo.SiteSettings;
import org.site_monitor.model.db.DBHelper;
import org.site_monitor.model.db.DBSiteSettings;
import org.site_monitor.task.CallSiteTask;
import org.site_monitor.task.TaskCallbackDefault;
import org.site_monitor.util.AlarmUtil;
import org.site_monitor.util.NotificationUtil;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import androidx.core.app.NotificationCompat;

public class AlarmJobService extends JobService {

    private static final String TAG = AlarmJobService.class.getSimpleName();

    private AlarmUtil alarmUtil = AlarmUtil.instance();

    /**
     * Override this method with the callback logic for your job. Any such logic needs to be
     * performed on a separate thread, as this function is executed on your application's main
     * thread.
     *
     * @param params Parameters specifying info about this job, including the extras bundle you
     *               optionally provided at job-creation time.
     * @return True if your service needs to process the work (on a separate thread). False if
     * there's no more work to be done for this job.
     */
    @Override
    public boolean onStartJob(final JobParameters params) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onStartJob");
        }
        new Thread(() -> {
            DBHelper dbHelper = DBHelper.getHelper(getApplicationContext());
            try {
                DBSiteSettings siteSettingDao = dbHelper.getDBSiteSettings();
                new CallSiteTask(getApplicationContext(), () -> new TaskCallbackDefault() {
                    @Override
                    public void onPostExecute(Object o, Object o2) {
                        List<Pair<SiteSettings, SiteCall>> results = (List<Pair<SiteSettings, SiteCall>>) o2;
                        List<Pair<SiteSettings, SiteCall>> failsPairs = new LinkedList<>();
                        for (Pair<SiteSettings, SiteCall> siteResult : results) {
                            if (siteResult.second.getResult() == NetworkCallResult.FAIL) {
                                failsPairs.add(siteResult);
                            }
                        }
                        String notificationMessage = NetworkService.performNotifyMessage(failsPairs, getApplicationContext());
                        if (notificationMessage != null) {
                            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(getApplicationContext(), MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
                            NotificationCompat.Builder notificationBuilder = NotificationUtil.build(getApplicationContext(), failsPairs.size() + " " + getString(R.string.state_unreachable), notificationMessage, pendingIntent);
                            NotificationUtil.send(getApplicationContext(), NotificationUtil.ID_NOT_REACHABLE, notificationBuilder.build());
                        }
                        alarmUtil.updateNextAlarmDate(getApplicationContext());
                        jobFinished(params, true);
                    }
                }).execute(siteSettingDao.queryForAll().toArray(new SiteSettings[]{}));
            } catch (SQLException e) {
                Log.e(TAG, "onHandleWork", e);
            } finally {
                if (dbHelper != null) {
                    dbHelper.release();
                }
                PurgeDbService.enqueueWork(getApplicationContext(), PurgeDbService.class, JobEnum.PURGE_DB.ordinal(), PurgeDbService.intent((getApplicationContext())));
            }
        }).start();
        return true;
    }

    /**
     * This method is called if the system has determined that you must stop execution of your job
     * even before you've had a chance to call {@link #jobFinished(JobParameters, boolean)}.
     * <p>
     * <p>This will happen if the requirements specified at schedule time are no longer met. For
     * example you may have requested WiFi with
     * {@link JobInfo.Builder#setRequiredNetworkType(int)}, yet while your
     * job was executing the user toggled WiFi. Another example is if you had specified
     * {@link JobInfo.Builder#setRequiresDeviceIdle(boolean)}, and the phone left its
     * idle maintenance window. You are solely responsible for the behaviour of your application
     * upon receipt of this message; your app will likely start to misbehave if you ignore it. One
     * immediate repercussion is that the system will cease holding a wakelock for you.</p>
     *
     * @param params Parameters specifying info about this job.
     * @return True to indicate to the JobManager whether you'd like to reschedule this job based
     * on the retry criteria provided at job creation-time. False to drop the job. Regardless of
     * the value returned, your job must stop executing.
     */
    @Override
    public boolean onStopJob(JobParameters params) {
        alarmUtil.killAlarm(this.getApplicationContext(), params.getJobId());
        return true;
    }

}
