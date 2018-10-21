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

package org.site_monitor.receiver.internal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.util.Log;

import org.site_monitor.BuildConfig;
import org.site_monitor.model.bo.SiteCall;
import org.site_monitor.model.bo.SiteSettings;
import org.site_monitor.service.FavIconService;
import org.site_monitor.service.NetworkService;
import org.site_monitor.util.BroadcastUtil;
import org.site_monitor.util.ConnectivityUtil;

public class NetworkBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = NetworkBroadcastReceiver.class.getSimpleName();
    private Listener listener;

    public NetworkBroadcastReceiver() {
    }

    public NetworkBroadcastReceiver(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            if (NetworkService.ACTION_SITE_START_REFRESH.equals(intent.getAction())) {
                SiteSettings siteSettings = intent.getParcelableExtra(BroadcastUtil.EXTRA_SITE);
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "ACTION_SITE_START_REFRESH: " + siteSettings);
                }
                listener.onSiteStartRefresh(siteSettings);
            } else if (NetworkService.ACTION_SITE_END_REFRESH.equals(intent.getAction())) {
                SiteSettings siteSettings = intent.getParcelableExtra(BroadcastUtil.EXTRA_SITE);
                SiteCall siteCall = intent.getParcelableExtra(BroadcastUtil.EXTRA_CALL);
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "ACTION_SITE_END_REFRESH: " + siteSettings + " " + siteCall);
                }
                listener.onSiteEndRefresh(siteSettings, siteCall);
            } else if (FavIconService.ACTION_FAVICON_UPDATED.equals(intent.getAction())) {
                SiteSettings siteSettings = intent.getParcelableExtra(BroadcastUtil.EXTRA_SITE);
                Bitmap favicon = intent.getParcelableExtra(BroadcastUtil.EXTRA_FAVICON);
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "ACTION_FAVICON_UPDATED: " + siteSettings);
                }
                listener.onFaviconUpdated(siteSettings, favicon);
            } else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                boolean isConnected = ConnectivityUtil.isConnected(context);
                if (BuildConfig.DEBUG) {
                    Log.v(TAG, "CONNECTIVITY_ACTION: " + isConnected);
                }
                listener.onNetworkStateChanged(isConnected);
            } else {
                if (BuildConfig.DEBUG) {
                    Log.w(TAG, "not managed action: " + intent.getAction());
                }
            }
        }
    }

    public interface Listener {
        void onSiteStartRefresh(SiteSettings siteSettings);
        void onSiteEndRefresh(SiteSettings siteSettings, SiteCall siteCall);

        void onFaviconUpdated(SiteSettings siteSettings, Bitmap favicon);
        void onNetworkStateChanged(boolean hasConnectivity);
    }
}
