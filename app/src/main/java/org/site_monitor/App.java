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

package org.site_monitor;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.analytics.Tracker;

/**
 * Created by Martin Norbert on 16/08/2015.
 */
public class App extends Application implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "App";
    private static boolean isForeground;

    public static boolean isForeground() {
        return isForeground;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
        GA.initialize(this);
        GAHit.initialize(this);
        Tracker tracker = GA.tracker();
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityResumed(Activity activity) {
        isForeground = true;
        if (BuildConfig.DEBUG) {
            Log.v(TAG, "isForeground: " + isForeground);
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        isForeground = false;
        if (BuildConfig.DEBUG) {
            Log.v(TAG, "isForeground: " + isForeground);
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }
}
