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

package org.site_monitor.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import org.site_monitor.BuildConfig;
import org.site_monitor.model.db.DBHelper;
import org.site_monitor.model.db.DBSiteCall;

import java.sql.SQLException;
import java.util.Calendar;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class PurgeDbService extends IntentService {

    private static final String ACTION_PURGE_CALLS = "org.site_monitor.service.action.PURGE_CALLS";
    private static final String TAG = PurgeDbService.class.getSimpleName();

    public PurgeDbService() {
        super(TAG);
    }

    public static Intent intent(Context context) {
        return new Intent(context, PurgeDbService.class).setAction(ACTION_PURGE_CALLS);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final String action = intent.getAction();
        if (action == null) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "onHandleIntent: no action");
            }
            return;
        }
        if (ACTION_PURGE_CALLS.equals(action)) {
            purgeCalls();
            WakefulBroadcastReceiver.completeWakefulIntent(intent);
        }
    }

    private void purgeCalls() {
        try {
            DBHelper dbHelper = DBHelper.getHelper(this);
            DBSiteCall dbSiteCall = dbHelper.getDBSiteCall();
            Calendar calendar = Calendar.getInstance();
            calendar.roll(Calendar.DAY_OF_MONTH, -5);
            dbSiteCall.removeCallsBefore(calendar.getTime());
        } catch (SQLException e) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "purgeCalls", e);
            }
        }
    }
}
