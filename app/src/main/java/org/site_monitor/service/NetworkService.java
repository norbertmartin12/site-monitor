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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;
import android.util.Pair;

import org.site_monitor.BuildConfig;
import org.site_monitor.GA;
import org.site_monitor.GAHit;
import org.site_monitor.R;
import org.site_monitor.activity.MainActivity;
import org.site_monitor.model.adapter.SiteSettingsManager;
import org.site_monitor.model.bo.NetworkCallResult;
import org.site_monitor.model.bo.SiteCall;
import org.site_monitor.model.bo.SiteSettings;
import org.site_monitor.util.ConnectivityUtil;
import org.site_monitor.util.NotificationUtil;
import org.site_monitor.util.TimeUtil;
import org.site_monitor.util.Timer;
import org.site_monitor.widget.WidgetManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class NetworkService extends IntentService {

    public static final String ACTION_SITE_UPDATED = "org.site_monitor.service.action.SITE_UPDATED";
    public static final String EXTRA_SITE = "org.site_monitor.service.extra.SITE";
    public static final String RECVFROM_FAILED_ECONNRESET = "recvfrom failed: ECONNRESET";
    private static final String BOT_AGENT = "bot-on-web-monitor";
    private static final String USER_AGENT = "User-Agent";
    private static final String TAG = "NetworkService";
    private static final String CLOSE = "close";
    private static final String CONNECTION = "Connection";
    private static final String HTTP = "http";
    private static final String ROOT_PROTOCOL = "://";
    private static final int TIMEOUT_10 = (int) (10 * TimeUtil.SEC_2_MILLISEC);
    private static final String METHOD_HEAD = "HEAD";
    private static final String FAVICON_SERVICE_URL = "http://www.google.com/s2/favicons?domain=";

    public NetworkService() {
        super(TAG);
    }

    /**
     * @param context
     * @return intent to call start service
     */
    public static Intent getIntent(Context context) {
        return new Intent(context, NetworkService.class);
    }

    /**
     * Builds and performs http request for given siteSettings
     *
     * @param context
     * @param siteSettings
     * @return SiteCall result
     */
    public static SiteCall buildHeadHttpConnectionThenDoCall(Context context, SiteSettings siteSettings) {
        SiteCall siteCall;
        if (ConnectivityUtil.isConnected(context)) {
            if (siteSettings.getFavicon() == null) {
                loadFaviconFor(siteSettings);
            }
            HttpURLConnection urlConnection = null;
            Timer timer = new Timer();
            try {
                urlConnection = buildHeadHttpConnection(siteSettings);
                siteCall = doCall(urlConnection, timer);
            } catch (IOException e) {
                siteCall = new SiteCall(timer.getReferenceDate(), NetworkCallResult.FAIL, timer.getElapsedTime(), e);
                if (e instanceof SocketException && e.getLocalizedMessage().startsWith(RECVFROM_FAILED_ECONNRESET)) {
                    try {
                        urlConnection = buildHeadHttpConnection(siteSettings);
                        siteCall = doCall(urlConnection, timer);
                    } catch (IOException e2) {
                        siteCall = new SiteCall(timer.getReferenceDate(), NetworkCallResult.FAIL, timer.getElapsedTime(), e);
                    }
                }
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

    /** Performs call represented by urlConnection
     * @param urlConnection
     * @param timer
     * @return call result
     * @throws IOException
     */
    private static SiteCall doCall(HttpURLConnection urlConnection, Timer timer) throws IOException {
        urlConnection.connect();
        int responseCode = urlConnection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            return new SiteCall(timer.getReferenceDate(), NetworkCallResult.FAIL, timer.getElapsedTime(), responseCode);
        }
        return new SiteCall(timer.getReferenceDate(), NetworkCallResult.SUCCESS, timer.getElapsedTime(), responseCode);
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
        urlConnection.setRequestProperty(USER_AGENT, BOT_AGENT);
        urlConnection.setUseCaches(false);
        urlConnection.setDefaultUseCaches(false);
        urlConnection.setDoInput(false);
        urlConnection.setDoOutput(false);
        urlConnection.setInstanceFollowRedirects(true);
        urlConnection.setConnectTimeout(TIMEOUT_10);
        urlConnection.setReadTimeout(TIMEOUT_10);
        return urlConnection;
    }

    /** Broadcasts siteSettings for given action as EXTRA_SITE
     * @param context
     * @param action
     * @param siteSettings
     */
    public static void broadcast(Context context, String action, SiteSettings siteSettings) {
        Intent localIntent = new Intent(action).putExtra(EXTRA_SITE, siteSettings);
        LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);
    }

    /** Retrieves and set favicon for given siteSettings
     * @param siteSettings
     */
    public static void loadFaviconFor(SiteSettings siteSettings) {
        InputStream is = null;
        try {
            is = (InputStream) new URL(FAVICON_SERVICE_URL + siteSettings.getHost()).getContent();
        } catch (IOException e) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "loadFaviconFor " + siteSettings.getName() + " fails: " + e, e);
            }
            return;
        }
        Bitmap favicon = BitmapFactory.decodeStream(is);
        if (favicon != null) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "favicon update for " + siteSettings + " " + favicon.getByteCount() + "bytes");
            }
            siteSettings.setFavicon(favicon);
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            SiteSettingsManager siteSettingsManager = SiteSettingsManager.instance(this);
            List<SiteSettings> siteSettingsList = siteSettingsManager.getSiteSettingsUnmodifiableList();
            List<Pair<SiteSettings, SiteCall>> failsPairs = new LinkedList<Pair<SiteSettings, SiteCall>>();
            for (SiteSettings siteSettings : siteSettingsList) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "call: " + siteSettings);
                }
                siteSettings.setIsChecking(true);
                broadcast(this, ACTION_SITE_UPDATED, siteSettings);
                SiteCall siteCall = buildHeadHttpConnectionThenDoCall(this, siteSettings);
                siteSettings.add(siteCall);
                siteSettings.setIsChecking(false);
                broadcast(this, ACTION_SITE_UPDATED, siteSettings);

                if (siteCall.getResult() == NetworkCallResult.FAIL) {
                    failsPairs.add(new Pair<SiteSettings, SiteCall>(siteSettings, siteCall));
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
                    NotificationCompat.Builder notificationBuilder = NotificationUtil.build(this, failsPairs.size() + " " + getString(R.string.state_unreachable), sb.toString(), pendingIntent);
                    if (NotificationUtil.send(this, NotificationUtil.ID_NOT_REACHABLE, notificationBuilder.build())) {
                        GA.tracker().send(GAHit.builder().event(R.string.c_notification, R.string.a_sent, new Long(failsPairs.size())).build());
                    }
                }
            }

            WidgetManager.refresh(this);
            WakefulBroadcastReceiver.completeWakefulIntent(intent);
        }
    }



}
