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

package org.site_monitor.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.site_monitor.BuildConfig;
import org.site_monitor.activity.PrefSettingsActivity;
import org.site_monitor.util.GsonUtil;

import java.io.Serializable;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class DataStoreService extends IntentService {

    private static final String ACTION_SAVE_DATA = "org.site_monitor.service.action.SAVE_DATA";

    private static final String EXTRA_DATA = "org.site_monitor.service.extra.DATA";
    private static final String EXTRA_DATA_KEY = "org.site_monitor.service.extra.DATA_KEY";
    private static final String TAG = "DataStoreService";

    public DataStoreService() {
        super(TAG);
    }

    /**
     * @param context
     * @param key
     * @param data
     */
    public static void startActionSaveData(Context context, String key, Serializable data) {
        Intent intent = new Intent(context, DataStoreService.class);
        intent.setAction(ACTION_SAVE_DATA);
        intent.putExtra(EXTRA_DATA_KEY, key);
        intent.putExtra(EXTRA_DATA, data);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_SAVE_DATA.equals(action)) {
                final String key = intent.getStringExtra(EXTRA_DATA_KEY);
                final Serializable data = intent.getSerializableExtra(EXTRA_DATA);
                handleActionSaveData(key, GsonUtil.toJson(data));
            }
        }
    }

    private void handleActionSaveData(String key, String jsonData) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "handleActionSaveData key: " + key);
        }
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        defaultSharedPreferences.edit().putString(PrefSettingsActivity.JSON_SITE_SETTINGS, jsonData).commit();
    }

}
