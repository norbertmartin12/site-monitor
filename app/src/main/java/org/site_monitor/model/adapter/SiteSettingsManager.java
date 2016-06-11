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

package org.site_monitor.model.adapter;

import android.content.Context;
import android.util.Log;

import com.google.gson.reflect.TypeToken;

import org.site_monitor.BuildConfig;
import org.site_monitor.model.bo.SiteCall;
import org.site_monitor.model.bo.SiteSettings;
import org.site_monitor.model.db.DBHelper;
import org.site_monitor.model.db.DBSiteCall;
import org.site_monitor.model.db.DBSiteSettings;
import org.site_monitor.service.SharedPreferencesService;
import org.site_monitor.util.GsonUtil;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by norbert on 19/07/2015.
 */
public class SiteSettingsManager {

    private static final String TAG = SiteSettingsManager.class.getSimpleName();

    public synchronized static void migrateDataFromJsonToDatabase(Context context) {
        String jsonData = SharedPreferencesService.getStringNow(context, SharedPreferencesService.KEY_JSON_SITE_SETTINGS);
        if (jsonData != null && !jsonData.isEmpty()) {
            List<SiteSettings> jsonList = GsonUtil.fromJson(jsonData, new TypeToken<List<SiteSettings>>() {
            });
            if (jsonList != null && !jsonList.isEmpty()) {
                // store in db
                DBHelper dbHelper = DBHelper.getHelper(context);
                try {
                    DBSiteSettings dbSiteSettings = dbHelper.getDBSiteSettings();
                    DBSiteCall dbSiteCall = dbHelper.getDBSiteCall();

                    for (SiteSettings siteSettings : jsonList) {
                        dbSiteSettings.create(siteSettings);
                        for (SiteCall siteCall : siteSettings.getCalls()) {
                            siteCall.setSiteSettings(siteSettings);
                            dbSiteCall.create(siteCall);
                        }
                    }
                    if (BuildConfig.DEBUG) {
                        Log.i(TAG, "migrateDataFromJsonToDatabase - copy in database SUCCESS");
                    }
                    SharedPreferencesService.saveNow(context, SharedPreferencesService.KEY_JSON_SITE_SETTINGS, null);
                } catch (SQLException e) {
                    Log.e(TAG, "migrateDataFromJsonToDatabase - copy in database", e);
                } finally {
                    dbHelper.release();
                }
            }
        }
    }
}
