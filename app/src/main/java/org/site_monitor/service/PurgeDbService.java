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
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
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
public class PurgeDbService extends JobIntentService {

    private static final String ACTION_PURGE_CALLS = "org.site_monitor.service.action.PURGE_CALLS";
    private static final String TAG = PurgeDbService.class.getSimpleName();

    public static Intent intent(Context context) {
        return new Intent(context, PurgeDbService.class).setAction(ACTION_PURGE_CALLS);
    }

    /**
     * Called serially for each work dispatched to and processed by the service.  This
     * method is called on a background thread, so you can do long blocking operations
     * here.  Upon returning, that work will be considered complete and either the next
     * pending work dispatched here or the overall service destroyed now that it has
     * nothing else to do.
     * <p>
     * <p>Be aware that when running as a job, you are limited by the maximum job execution
     * time and any single or total sequential items of work that exceeds that limit will
     * cause the service to be stopped while in progress and later restarted with the
     * last unfinished work.  (There is currently no limit on execution duration when
     * running as a pre-O plain Service.)</p>
     *
     * @param intent The intent describing the work to now be processed.
     */
    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        final String action = intent.getAction();
        if (action == null) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "onHandleIntent: no action");
            }
            return;
        }
        if (ACTION_PURGE_CALLS.equals(action)) {
            purgeCalls();
        }
    }

    private void purgeCalls() {
        try {
            DBHelper dbHelper = DBHelper.getHelper(this);
            DBSiteCall dbSiteCall = dbHelper.getDBSiteCall();
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MONTH, -1);
            int nbDeleted = dbSiteCall.removeCallsBefore(calendar.getTime());
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "purgeCalls: " + nbDeleted + " before: " + calendar.getTime());
            }
        } catch (SQLException e) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "purgeCalls", e);
            }
        }
    }
}
