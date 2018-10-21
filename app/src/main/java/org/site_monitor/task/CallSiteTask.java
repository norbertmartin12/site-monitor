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

package org.site_monitor.task;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import org.site_monitor.BuildConfig;
import org.site_monitor.model.bo.SiteCall;
import org.site_monitor.model.bo.SiteSettings;
import org.site_monitor.model.db.DBHelper;
import org.site_monitor.model.db.DBSiteCall;
import org.site_monitor.service.NetworkService;
import org.site_monitor.util.BroadcastUtil;
import org.site_monitor.util.NetworkUtil;
import org.site_monitor.widget.WidgetManager;

import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Runs a background task that send notifications and save result in database.
 */
public class CallSiteTask extends AsyncTaskWithCallback<SiteSettings, Void, List<Pair<SiteSettings, SiteCall>>> {
    private static final String TAG = "CallSiteTask";
    private NetworkUtil networkUtil = new NetworkUtil();
    private Context context;

    public CallSiteTask(Context context, TaskCallback.Provider callbackProvider) {
        super(callbackProvider, TAG);
        this.context = context;
    }

    @Override
    protected List<Pair<SiteSettings, SiteCall>> doInBackground(SiteSettings... params) {
        if (params.length == 0) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "doInBackground: none site in params");
            }
            return Collections.emptyList();
        }
        List<Pair<SiteSettings, SiteCall>> results = new LinkedList<Pair<SiteSettings, SiteCall>>();
        for (SiteSettings siteSettings : params) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "doInBackground: " + siteSettings);
            }
            BroadcastUtil.broadcast(context, NetworkService.ACTION_SITE_START_REFRESH, siteSettings);
            SiteCall siteCall = networkUtil.buildHeadHttpConnectionThenDoCall(context, siteSettings);
            siteCall.setSiteSettings(siteSettings);
            DBHelper dbHelper = DBHelper.getHelper(context);
            try {
                DBSiteCall dbSiteCall = dbHelper.getDBSiteCall();
                dbSiteCall.create(siteCall);
                WidgetManager.refresh(context);
            } catch (SQLException e) {
                Log.e(TAG, "doInBackground", e);
            }
            BroadcastUtil.broadcast(context, NetworkService.ACTION_SITE_END_REFRESH, siteSettings, siteCall);
            results.add(new Pair<SiteSettings, SiteCall>(siteSettings, siteCall));
        }
        return results;
    }

}
