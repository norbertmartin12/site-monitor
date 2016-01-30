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
import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;
import android.util.Pair;

import org.site_monitor.BuildConfig;
import org.site_monitor.GA;
import org.site_monitor.GAHit;
import org.site_monitor.R;
import org.site_monitor.activity.MainActivity;
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

import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class NetworkService extends IntentService {

    public static final String ACTION_SITE_START_REFRESH = "org.site_monitor.service.action.SITE_START_REFRESH";
    public static final String ACTION_SITE_END_REFRESH = "org.site_monitor.service.action.SITE_END_REFRESH";
    public static final String ACTION_FAVICON_UPDATED = "org.site_monitor.service.action.FAVICON_UPDATED";
    public static final String REQUEST_REFRESH_SITES = "refreshSites";
    public static final String REQUEST_REFRESH_FAVICON = "loadFavicon";
    public static final String P_URL = "url";
    private static final String TAG = NetworkService.class.getSimpleName();
    private NetworkUtil networkUtil = new NetworkUtil();

    public NetworkService() {
        super(TAG);
    }

    /**
     * @param context
     * @return intent to call start service
     */
    public static Intent intentToCheckSites(Context context) {
        return new Intent(context, NetworkService.class).setAction(REQUEST_REFRESH_SITES);
    }

    /**
     * @param context
     * @return intent to call start service
     */
    public static Intent intentToLoadFavicon(Context context, String url) {
        return new Intent(context, NetworkService.class).setAction(REQUEST_REFRESH_FAVICON).putExtra(P_URL, url);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
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
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "action: " + REQUEST_REFRESH_SITES);
                }
                DBSiteCall dbSiteCall = dbHelper.getDBSiteCall();
                refreshSites(siteSettingDao, dbSiteCall);
            } else if (intent.getAction().equals(REQUEST_REFRESH_FAVICON)) {
                String url = intent.getStringExtra(P_URL);
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "action: " + REQUEST_REFRESH_FAVICON + " " + url);
                }
                refreshFavicon(siteSettingDao, url);
            }
        } catch (SQLException e) {
            Log.e(TAG, "onHandleIntent", e);
        } finally {
            if (dbHelper != null) {
                dbHelper.release();
            }
            WakefulBroadcastReceiver.completeWakefulIntent(intent);
        }
    }

    private void refreshFavicon(DBSiteSettings siteSettingDao, String url) throws SQLException {
        SiteSettings siteSettings = siteSettingDao.findForHost(url);
        if (siteSettings == null) {
            Log.w(TAG, "refreshFavicon, not site for: " + url);
            return;
        }
        Log.d(TAG, "favicon: " + siteSettings);
        Bitmap favicon = NetworkUtil.loadFaviconFor(siteSettings.getHost());
        ByteBuffer buffer = ByteBuffer.allocate(favicon.getByteCount());
        favicon.copyPixelsToBuffer(buffer);
        siteSettings.setFavicon(buffer.array());
        siteSettingDao.update(siteSettings);
        BroadcastUtil.broadcast(this, ACTION_FAVICON_UPDATED, siteSettings, BroadcastUtil.EXTRA_FAVICON, favicon);
    }

    private void refreshSites(DBSiteSettings siteSettingDao, DBSiteCall dbSiteCall) throws SQLException {
        List<SiteSettings> siteSettingList = siteSettingDao.queryForAll();
        List<Pair<SiteSettings, SiteCall>> failsPairs = new LinkedList<Pair<SiteSettings, SiteCall>>();
        for (SiteSettings siteSettings : siteSettingList) {
            if (BuildConfig.DEBUG) {
                Log.v(TAG, "call: " + siteSettings);
            }
            BroadcastUtil.broadcast(this, ACTION_SITE_START_REFRESH, siteSettings);
            SiteCall siteCall = networkUtil.buildHeadHttpConnectionThenDoCall(this, siteSettings);
            siteCall.setSiteSettings(siteSettings);
            BroadcastUtil.broadcast(this, ACTION_SITE_END_REFRESH, siteSettings, siteCall);

            if (siteCall.getResult() == NetworkCallResult.FAIL) {
                failsPairs.add(new Pair<SiteSettings, SiteCall>(siteSettings, siteCall));
            }
            dbSiteCall.create(siteCall);
        }

        if (BuildConfig.DEBUG && siteSettingList.isEmpty()) {
            Log.w(TAG, "called but nothing to run");
        }

        boolean atLeastOneToNotify = false;
        if (!failsPairs.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Pair<SiteSettings, SiteCall> pair : failsPairs) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                if (pair.first.isNotificationEnabled()) {
                    atLeastOneToNotify = true;
                }
                sb.append(pair.first.getName());
            }
            if (atLeastOneToNotify) {
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
                NotificationCompat.Builder notificationBuilder = NotificationUtil.build(this, failsPairs.size() + " " + getString(R.string.state_unreachable), sb.toString(), pendingIntent);
                if (NotificationUtil.send(this, NotificationUtil.ID_NOT_REACHABLE, notificationBuilder.build())) {
                    GA.tracker().send(GAHit.builder().event(R.string.c_notification, R.string.a_sent, new Long(failsPairs.size())).build());
                }
            }
        }
        WidgetManager.refresh(this);
    }
}
