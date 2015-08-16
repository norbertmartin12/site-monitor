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
import android.util.Log;

import org.site_monitor.BuildConfig;
import org.site_monitor.model.bo.SiteCall;
import org.site_monitor.model.bo.SiteSettings;
import org.site_monitor.service.NetworkService;

/**
 * Created by norbert on 16/07/2015.
 */
public class NetworkTask extends AsyncTaskWithCallback<SiteSettings, Void, SiteSettings> {
    private static final String TAG = "NetworkTask";
    private Context context;

    public NetworkTask(Context context, TaskCallback.Provider callbackProvider) {
        super(callbackProvider, TAG);
        this.context = context;
    }

    @Override
    protected SiteSettings doInBackground(SiteSettings... params) {
        if (params.length != 1) {
            Log.w(TAG, "too many params: " + params.length);
            throw new UnsupportedOperationException("task supports only 1 params, params: " + params);
        }
        SiteSettings siteSettings = params[0];
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "doInBackground start: " + siteSettings);
        }
        SiteCall siteCall = NetworkService.buildHeadHttpConnectionThenDoCall(context, siteSettings);
        siteSettings.add(siteCall);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "doInBackground end: " + siteSettings);
        }
        return siteSettings;
    }

}
