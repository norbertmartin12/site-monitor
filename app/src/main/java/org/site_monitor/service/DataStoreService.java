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
import org.site_monitor.util.GsonUtil;

import java.io.Serializable;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class DataStoreService extends IntentService {


    public static final String KEY_JSON_SITE_SETTINGS = "org.site_monitor.activity.settings.json.siteSettings";
    public static final String KEY_NEXT_ALARM = "org.site_monitor.nextAlarm";


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

    public static long getLongNow(Context context, String key) {
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return defaultSharedPreferences.getLong(key, 0);
    }

    public static void saveNow(Context context, String key, String value) {
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        defaultSharedPreferences.edit().putString(key, value).commit();
    }

    public static void saveNow(Context context, String key, long value) {
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        defaultSharedPreferences.edit().putLong(key, value).commit();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_SAVE_DATA.equals(action)) {
                final String key = intent.getStringExtra(EXTRA_DATA_KEY);
                final Serializable data = intent.getSerializableExtra(EXTRA_DATA);
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "handleActionSaveData key: " + key);
                }
                saveNow(this, key, GsonUtil.toJson(data));
            }
        }
    }
}
