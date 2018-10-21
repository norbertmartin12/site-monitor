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

package org.site_monitor.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.site_monitor.BuildConfig;
import org.site_monitor.util.AlarmUtil;

/**
 * Receives battery level event and stop or start AlarmJobService to optimize battery life and monitoring.
 */
public class BatteryLevelReceiver extends BroadcastReceiver {
    private static final String TAG = BatteryLevelReceiver.class.getSimpleName();
    private static String lastAction;
    private boolean batteryOk = true;
    private AlarmUtil alarmUtil = AlarmUtil.instance();

    public static String getLastAction() {
        return lastAction;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            if (Intent.ACTION_BATTERY_LOW.equals(intent.getAction())) {
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "ACTION_BATTERY_LOW");
                }
                batteryOk = false;
                alarmUtil.stopAlarm(context);
                lastAction = "ACTION_BATTERY_LOW";
            } else if (Intent.ACTION_BATTERY_OKAY.equals(intent.getAction())) {
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "ACTION_BATTERY_OKAY");
                }
                batteryOk = true;
                alarmUtil.startAlarmIfNeeded(context);
                lastAction = "ACTION_BATTERY_OKAY";
            } else if (Intent.ACTION_POWER_CONNECTED.equals(intent.getAction())) {
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "ACTION_POWER_CONNECTED");
                }
                alarmUtil.startAlarmIfNeeded(context);
                lastAction = "ACTION_POWER_CONNECTED";
            } else if (Intent.ACTION_POWER_DISCONNECTED.equals(intent.getAction())) {
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "ACTION_POWER_DISCONNECTED");
                }
                if (!batteryOk) {
                    alarmUtil.stopAlarm(context);
                }
                lastAction = "ACTION_POWER_DISCONNECTED";
            } else {
                Log.d(TAG, "unmanaged action: " + intent.getAction());
            }
        }
    }
}
