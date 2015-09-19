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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import org.site_monitor.BuildConfig;
import org.site_monitor.model.adapter.SiteSettingsManager;

public class StartupBootReceiver extends BroadcastReceiver {

    public static final String TAG = "StartupBootReceiver";

    public static void setCanBeInitiatedBySystem(Context context, boolean enable) {
        ComponentName receiver = new ComponentName(context, StartupBootReceiver.class);
        PackageManager pm = context.getPackageManager();
        if (enable) {
            pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        } else {
            pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "setCanBeInitiatedBySystem: " + enable);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            SiteSettingsManager siteSettingsManager = SiteSettingsManager.instance(context);
            if (siteSettingsManager.size() > 0) {
                AlarmReceiver.startAlarm(context);
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "starts on boot: " + siteSettingsManager.size() + " sites");
                }
            } else {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "no start on boot: " + siteSettingsManager.size() + " site");
                }
            }
        }
    }

}

