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

package org.site_monitor.service;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;
import android.util.Pair;

import org.site_monitor.BuildConfig;
import org.site_monitor.R;
import org.site_monitor.activity.MainActivity;
import org.site_monitor.model.adapter.SiteSettingsManager;
import org.site_monitor.model.bo.NetworkCallResult;
import org.site_monitor.model.bo.SiteCall;
import org.site_monitor.model.bo.SiteSettings;
import org.site_monitor.util.ConnectivityUtil;
import org.site_monitor.util.NotificationUtil;
import org.site_monitor.util.TimeUtil;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class NetworkService extends IntentService {
    public static final String ACTION_SITE_UPDATED = "org.site_monitor.service.networkService.SITE_UPDATED";
    public static final String EXTRA_SITE = "org.site_monitor.service.networkService.SITE";

    private static final String TAG = "NetworkService";
    private static final String CLOSE = "close";
    private static final String CONNECTION = "Connection";
    private static final String HTTP = "http";
    private static final String ROOT_PROTOCOL = "://";
    private static final int TIMEOUT_10 = (int) (10 * TimeUtil.SEC_2_MILLISEC);
    private static final String METHOD_HEAD = "HEAD";

    public NetworkService() {
        super(TAG);
    }

    public static Intent getIntent(Context context) {
        return new Intent(context, NetworkService.class);
    }

    public static SiteCall buildHeadHttpConnectionThenDoCall(Context context, SiteSettings siteSettings) {
        SiteCall siteCall;
        if (ConnectivityUtil.isConnectedOrConnecting(context)) {
            HttpURLConnection urlConnection = null;
            try {
                urlConnection = buildHeadHttpConnection(siteSettings);
                siteCall = doCall(urlConnection);
            } catch (IOException e) {
                siteCall = new SiteCall(new Date(), NetworkCallResult.FAIL, e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        } else {
            siteCall = new SiteCall(new Date(), NetworkCallResult.NO_CONNECTIVITY);
        }
        return siteCall;
    }

    private static SiteCall doCall(HttpURLConnection urlConnection) throws IOException {
        urlConnection.connect();
        if (urlConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            return new SiteCall(new Date(), NetworkCallResult.FAIL, urlConnection.getResponseCode());
        }
        return new SiteCall(new Date(), NetworkCallResult.SUCCESS, urlConnection.getResponseCode());
    }

    /**
     * Builds HttpURLConnection (requestMethod = head, property = connection/close, no cache, follow redirects, connection/read timeout 10sec
     *
     * @param siteSettings
     * @return HttpURLConnection
     * @throws IOException
     */
    private static HttpURLConnection buildHeadHttpConnection(SiteSettings siteSettings) throws IOException {
        URL url;
        if (siteSettings.getHost().toLowerCase().startsWith(HTTP)) {
            url = new URL(siteSettings.getHost());
        } else {
            url = new URL(HTTP + ROOT_PROTOCOL + siteSettings.getHost());
        }
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod(METHOD_HEAD);
        urlConnection.setRequestProperty(CONNECTION, CLOSE);
        urlConnection.setUseCaches(false);
        urlConnection.setInstanceFollowRedirects(true);
        urlConnection.setConnectTimeout(TIMEOUT_10);
        urlConnection.setReadTimeout(TIMEOUT_10);
        return urlConnection;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SiteSettingsManager siteSettingsManager = SiteSettingsManager.instance(this);
        List<SiteSettings> siteSettingsList = siteSettingsManager.getSiteSettingsUnmodifiableList();
        List<Pair<SiteSettings, SiteCall>> failsPairs = new LinkedList<>();
        for (SiteSettings siteSettings : siteSettingsList) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "call: " + siteSettings);
            }
            SiteCall siteCall = buildHeadHttpConnectionThenDoCall(this, siteSettings);

            siteSettings.add(siteCall);

            broadcastUpdate(siteSettings);
            if (siteCall.getResult() == NetworkCallResult.FAIL) {
                failsPairs.add(new Pair<>(siteSettings, siteCall));
            }
        }
        siteSettingsManager.saveSiteSettings(this);

        if (BuildConfig.DEBUG && siteSettingsList.isEmpty()) {
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
                NotificationUtil.sendNotification(this, NotificationUtil.ID_NOT_REACHABLE, failsPairs.size() + " " + getString(R.string.state_unreachable), sb.toString(), pendingIntent);
            }
        }

        WakefulBroadcastReceiver.completeWakefulIntent(intent);
    }

    public void broadcastUpdate(SiteSettings siteSettings) {
        Intent localIntent = new Intent(ACTION_SITE_UPDATED).putExtra(EXTRA_SITE, siteSettings);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

}
