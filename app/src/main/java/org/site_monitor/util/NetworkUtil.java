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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.site_monitor.BuildConfig;
import org.site_monitor.model.bo.NetworkCallResult;
import org.site_monitor.model.bo.SiteCall;
import org.site_monitor.model.bo.SiteSettings;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by Martin Norbert on 31/01/2016.
 */
public class NetworkUtil {

    private static final String TAG = NetworkUtil.class.getSimpleName();

    private static final String RECVFROM_FAILED_ECONNRESET = "recvfrom failed: ECONNRESET";
    private static final String BOT_AGENT = "bot-site-monitor";
    private static final String USER_AGENT = "User-Agent";
    private static final String CLOSE = "close";
    private static final String CONNECTION = "Connection";
    private static final String HTTP = "http";
    private static final String ROOT_PROTOCOL = "://";
    private static final int TIMEOUT_10 = (int) (10 * TimeUtil.SEC_2_MILLISEC);
    private static final String METHOD_HEAD = "HEAD";
    private static final String FAVICON_SERVICE_URL = "http://www.google.com/s2/favicons?domain=";

    /**
     * Retrieves favicon for given url
     *
     * @param url
     */
    public static Bitmap loadFaviconFor(String url) {
        InputStream is = null;
        try {
            is = (InputStream) new URL(FAVICON_SERVICE_URL + url).getContent();
            if (BuildConfig.DEBUG) {
                Log.v(TAG, "loadFaviconFor " + url + " succeed");
            }
            return BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "loadFaviconFor " + url + " fails: " + e, e);
            }
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    if (BuildConfig.DEBUG) {
                        Log.w(TAG, "loadFaviconFor " + url + " fails: " + e, e);
                    }
                    return null;
                }
            }
        }
    }

    /**
     * Builds and performs http request for given siteSettings
     *
     * @param context
     * @param siteSettings
     * @return SiteCall result
     */
    public SiteCall buildHeadHttpConnectionThenDoCall(Context context, SiteSettings siteSettings) {
        SiteCall siteCall;
        if (ConnectivityUtil.isConnected(context)) {
            HttpURLConnection urlConnection = null;
            Timer timer = new Timer();
            try {
                urlConnection = buildHeadHttpConnection(siteSettings.getHost(), siteSettings.isForcedCertificate());
                siteCall = doCall(urlConnection, timer);
            } catch (IOException e) {
                disconnect(urlConnection);
                if (e.getLocalizedMessage() != null && e.getLocalizedMessage().startsWith(RECVFROM_FAILED_ECONNRESET)) {
                    Log.d(TAG, "RECVFROM_FAILED_ECONNRESET - retry once: " + siteSettings);
                    try {
                        timer = new Timer();
                        urlConnection = buildHeadHttpConnection(siteSettings.getHost(), siteSettings.isForcedCertificate());
                        siteCall = doCall(urlConnection, timer);
                    } catch (IOException eConnReset) {
                        disconnect(urlConnection);
                        siteCall = new SiteCall(timer.getReferenceDate(), NetworkCallResult.FAIL, timer.getElapsedTime(), e);
                    }
                } else {
                    try {
                        if (siteSettings.getInternalUrl() != null) {
                            Log.d(TAG, "retry with internal URL: " + siteSettings);
                            timer = new Timer();
                            urlConnection = buildHeadHttpConnection(siteSettings.getInternalUrl(), siteSettings.isForcedCertificate());
                            siteCall = doCall(urlConnection, timer);
                        } else {
                            siteCall = new SiteCall(timer.getReferenceDate(), NetworkCallResult.FAIL, timer.getElapsedTime(), e);
                        }
                    } catch (IOException eInternalIp) {
                        disconnect(urlConnection);
                        siteCall = new SiteCall(timer.getReferenceDate(), NetworkCallResult.FAIL, timer.getElapsedTime(), e);
                    }
                }
            } finally {
                disconnect(urlConnection);
            }
        } else {
            siteCall = new SiteCall(new Date(), NetworkCallResult.NO_CONNECTIVITY);
        }
        return siteCall;
    }

    public void disconnect(HttpURLConnection urlConnection) {
        if (urlConnection != null) {
            urlConnection.disconnect();
        }
    }

    /**
     * Performs call represented by urlConnection
     *
     * @param urlConnection
     * @param timer
     * @return call result
     * @throws IOException
     */
    private SiteCall doCall(HttpURLConnection urlConnection, Timer timer) throws IOException {
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
     * @param host
     * @param forceTrust
     * @return HttpURLConnection
     * @throws IOException
     */
    private HttpURLConnection buildHeadHttpConnection(String host, boolean forceTrust) throws IOException {
        URL url;
        if (host.toLowerCase().startsWith(HTTP)) {
            url = new URL(host);
        } else {
            url = new URL(HTTP + ROOT_PROTOCOL + host);
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
        if (forceTrust) {
            ((HttpsURLConnection) urlConnection).setSSLSocketFactory(CertificateTrustAllManager.sslSocketFactory());
        }
        return urlConnection;
    }

}
