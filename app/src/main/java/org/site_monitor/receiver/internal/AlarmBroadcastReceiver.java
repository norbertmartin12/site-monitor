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
import android.util.Log;

import org.site_monitor.BuildConfig;
import org.site_monitor.util.AlarmUtil;
import org.site_monitor.util.BroadcastUtil;

import java.util.Date;

/**
 * Created by Martin Norbert on 20/02/2016.
 */
public class AlarmBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = AlarmBroadcastReceiver.class.getSimpleName();
    private Listener listener;

    public AlarmBroadcastReceiver() {
    }

    public AlarmBroadcastReceiver(Listener listener) {
        this.listener = listener;
    }

    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            if (AlarmUtil.ACTION_NEXT_ALARM_SET.equals(intent.getAction())) {
                long nextAlarm = intent.getLongExtra(BroadcastUtil.EXTRA_ALARM, 0);
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "ACTION_NEXT_ALARM_SET: " + new Date(nextAlarm));
                }
                listener.onNextAlarmChange(nextAlarm);
            }
        }
    }

    public interface Listener {
        void onNextAlarmChange(long nextAlarm);
    }
}
