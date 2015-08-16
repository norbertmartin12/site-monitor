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

package org.site_monitor.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

import org.site_monitor.BuildConfig;
import org.site_monitor.model.bo.SiteSettings;
import org.site_monitor.service.NetworkService;
import org.site_monitor.util.ConnectivityUtil;

public class NetworkServiceReceiver extends BroadcastReceiver {

    public static final String TAG = "NetworkServiceReceiver";
    private Listener listener;

    public NetworkServiceReceiver() {
    }

    public NetworkServiceReceiver(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(NetworkService.ACTION_SITE_UPDATED)) {
            SiteSettings siteSettings = (SiteSettings) intent.getSerializableExtra(NetworkService.EXTRA_SITE);
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "ACTION_SITE_UPDATED: " + siteSettings);
            }
            listener.onSiteUpdated(siteSettings);
        } else if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            boolean b = ConnectivityUtil.isConnectedOrConnecting(context);
            if (BuildConfig.DEBUG) {
                Log.v(TAG, "CONNECTIVITY_ACTION: " + b);
            }
            listener.onNetworkStateChanged(b);
        } else {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "not managed action: " + intent.getAction());
            }
        }
    }

    public interface Listener {
        void onSiteUpdated(SiteSettings siteSettings);

        void onNetworkStateChanged(boolean hasConnectivity);
    }
}
