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

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Pair;

import org.site_monitor.BuildConfig;
import org.site_monitor.R;
import org.site_monitor.activity.MainActivity;
import org.site_monitor.activity.PrefSettingsActivity;
import org.site_monitor.model.bo.NetworkCallResult;
import org.site_monitor.model.bo.SiteCall;
import org.site_monitor.model.bo.SiteSettings;
import org.site_monitor.model.db.DBHelper;
import org.site_monitor.model.db.DBSiteCall;
import org.site_monitor.model.db.DBSiteSettings;
import org.site_monitor.util.BroadcastUtil;
import org.site_monitor.util.NetworkUtil;
import org.site_monitor.util.NotificationUtil;
import org.site_monitor.widget.WidgetManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class NetworkService extends JobIntentService {

    public static final String ACTION_SITE_START_REFRESH = "org.site_monitor.service.action.SITE_START_REFRESH";
    public static final String ACTION_SITE_END_REFRESH = "org.site_monitor.service.action.SITE_END_REFRESH";
    public static final String REQUEST_REFRESH_SITES = "refreshSites";
    public static final String COMA = ",";
    private static final String TAG = NetworkService.class.getSimpleName();
    private final NetworkUtil networkUtil = new NetworkUtil();

    /**
     * @param context to pass in the intent
     * @return intent to call start service
     */
    public static Intent intentToCheckSites(Context context) {
        return new Intent(context, NetworkService.class).setAction(REQUEST_REFRESH_SITES);
    }

    public static void enqueueCheckSitesWork(@NonNull Context context) {
        NetworkService.enqueueWork(context, NetworkService.class, JobEnum.CHECK_SITES.ordinal(), intentToCheckSites(context));
    }

    /**
     * Called serially for each work dispatched to and processed by the service.  This
     * method is called on a background thread, so you can do long blocking operations
     * here.  Upon returning, that work will be considered complete and either the next
     * pending work dispatched here or the overall service destroyed now that it has
     * nothing else to do.
     * <p>
     * <p>Be aware that when running as a job, you are limited by the maximum job execution
     * time and any single or total sequential items of work that exceeds that limit will
     * cause the service to be stopped while in progress and later restarted with the
     * last unfinished work.  (There is currently no limit on execution duration when
     * running as a pre-O plain Service.)</p>
     *
     * @param intent The intent describing the work to now be processed.
     */
    @Override
    public void onHandleWork(@NonNull Intent intent) {
        if (intent.getAction() == null) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "onHandleIntent: no action");
            }
            return;
        }
        DBHelper dbHelper = DBHelper.getHelper(this);
        try {
            DBSiteSettings siteSettingDao = dbHelper.getDBSiteSettings();
            if (intent.getAction().equals(REQUEST_REFRESH_SITES)) {
                DBSiteCall dbSiteCall = dbHelper.getDBSiteCall();
                refreshSites(siteSettingDao, dbSiteCall);
            }
        } catch (SQLException e) {
            Log.e(TAG, "onHandleWork", e);
        } finally {
            if (dbHelper != null) {
                dbHelper.release();
            }
        }
    }

    public void refreshSites(DBSiteSettings siteSettingDao, DBSiteCall dbSiteCall) throws SQLException {
        List<SiteSettings> siteSettingList = siteSettingDao.queryForAll();
        List<Pair<SiteSettings, SiteCall>> failsPairs = new LinkedList<>();
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "action: " + REQUEST_REFRESH_SITES + " nbSites: " + siteSettingList.size());
        }
        for (SiteSettings siteSettings : siteSettingList) {
            if (BuildConfig.DEBUG) {
                Log.v(TAG, "call: " + siteSettings);
            }
            BroadcastUtil.broadcast(this, ACTION_SITE_START_REFRESH, siteSettings);
            SiteCall siteCall = networkUtil.buildHeadHttpConnectionThenDoCall(this, siteSettings);
            siteCall.setSiteSettings(siteSettings);
            BroadcastUtil.broadcast(this, ACTION_SITE_END_REFRESH, siteSettings, siteCall);

            if (siteCall.getResult() == NetworkCallResult.FAIL) {
                failsPairs.add(new Pair<>(siteSettings, siteCall));
            }
            dbSiteCall.create(siteCall);
        }
        String notificationMessage = performNotifyMessage(failsPairs, this);
        if (notificationMessage != null) {
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
            NotificationCompat.Builder notificationBuilder = NotificationUtil.build(this, failsPairs.size() + " " + getString(R.string.state_unreachable), notificationMessage, pendingIntent);
            NotificationUtil.send(this, NotificationUtil.ID_NOT_REACHABLE, notificationBuilder.build());
        }
        WidgetManager.refresh(this);
    }

    public static String performNotifyMessage(List<Pair<SiteSettings, SiteCall>> failsPairs, Context context) {
        boolean atLeastOneToNotify = false;
        if (failsPairs.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean limitToNewFail = preferences.getBoolean(PrefSettingsActivity.NOTIFICATION_LIMIT_TO_NEW_FAIL, true);
        for (Pair<SiteSettings, SiteCall> pair : failsPairs) {
            // never break to get all sites in fail
            if (sb.length() > 0) {
                sb.append(COMA);
            }
            sb.append(pair.first.getName());
            if (pair.first.isNotificationEnabled()) {
                if (limitToNewFail) {
                    if (pair.first.getSiteCalls().isEmpty()) {
                        atLeastOneToNotify = true;
                    } else {
                        List<SiteCall> siteCalls = new ArrayList<>(pair.first.getSiteCalls());
                        Collections.sort(siteCalls, SiteCall.DESC_DATE);
                        SiteCall previousCall = siteCalls.get(0);
                        if (previousCall.getResult() == NetworkCallResult.SUCCESS) {
                            atLeastOneToNotify = true;
                        }
                        if (previousCall.getResult() == NetworkCallResult.FAIL) {
                            if (!DateUtils.isToday(previousCall.getDate().getTime())) {
                                atLeastOneToNotify = true;
                            }
                        }
                    }
                } else {
                    atLeastOneToNotify = true;
                }
            }
        }
        if (atLeastOneToNotify) {
            return sb.toString();
        }
        return null;
    }
}
