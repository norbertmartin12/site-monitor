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

package org.site_monitor.task;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.reflect.TypeToken;

import org.site_monitor.BuildConfig;
import org.site_monitor.model.bo.SiteSettings;
import org.site_monitor.util.GsonUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Martin Norbert on 19/09/2015.
 */
public class DataStoreTask extends AsyncTaskWithCallback<String, Void, List<SiteSettings>> {

    private static final String TAG = "DataStoreTask";
    private Context context;

    public DataStoreTask(Context context, TaskCallback.Provider callbackProvider) {
        super(callbackProvider, TAG);
        this.context = context;
    }

    @Override
    protected List<SiteSettings> doInBackground(String... params) {
        if (params.length != 1) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "too many params: " + params.length);
            }
            throw new UnsupportedOperationException("task supports only 1 param, params: " + params);
        }
        String key = params[0];
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "doInBackground key: " + key);
        }
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String jsonData = defaultSharedPreferences.getString(key, "");

        List<SiteSettings> retrievedList = null;
        if (!jsonData.isEmpty()) {
            retrievedList = GsonUtil.fromJson(jsonData, new TypeToken<List<SiteSettings>>() {
            });
        } else {
            retrievedList = new ArrayList<SiteSettings>();
        }
        return retrievedList;
    }
}
