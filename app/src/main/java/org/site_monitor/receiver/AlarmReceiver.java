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

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import org.site_monitor.BuildConfig;
import org.site_monitor.service.NetworkService;
import org.site_monitor.service.PurgeDbService;
import org.site_monitor.util.AlarmUtil;

import java.util.Date;
import java.util.Random;

public class AlarmReceiver extends WakefulBroadcastReceiver {

    private static final String TAG = AlarmReceiver.class.getSimpleName();
    private static final Random RANDOM = new Random(new Date().getTime());

    private AlarmUtil alarmUtil = AlarmUtil.instance();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            try {
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "onReceive");
                }
                Thread.sleep(RANDOM.nextInt(10) * 100);
                WakefulBroadcastReceiver.startWakefulService(context, NetworkService.intentToCheckSites(context));
                WakefulBroadcastReceiver.startWakefulService(context, PurgeDbService.intent(context));
                alarmUtil.updateNextAlarmDate(context);
            } catch (InterruptedException e) {
                if (BuildConfig.DEBUG) {
                    Log.wtf(TAG, e);
                }
            }
        }
    }
}
