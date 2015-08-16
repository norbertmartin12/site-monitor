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

import org.site_monitor.model.adapter.SiteSettingsManager;

public class BatteryLevelReceiver extends BroadcastReceiver {
    private static String lastAction;

    public BatteryLevelReceiver() {
    }

    public static String getLastAction() {
        return lastAction;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BATTERY_LOW)) {
            AlarmReceiver.stopAlarm(context);
            lastAction = "ACTION_BATTERY_LOW";
        } else if (intent.getAction().equals(Intent.ACTION_BATTERY_OKAY)) {
            SiteSettingsManager.instance(context).startAlarmIfNeeded(context);
            lastAction = "ACTION_BATTERY_OKAY";
        }
    }
}
