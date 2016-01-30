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

import android.os.AsyncTask;
import android.util.Log;

import org.site_monitor.BuildConfig;

import java.util.Arrays;

/**
 * Created by norbert on 19/07/2015.<br/>
 * Default implementation that does nothing except working with {@link TaskCallback} and {@link TaskCallback.Provider}
 */
public abstract class AsyncTaskWithCallback<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {
    protected final TaskCallback.Provider callbackProvider;
    private final String tag;

    public AsyncTaskWithCallback(TaskCallback.Provider callbackProvider, String tag) {
        this.callbackProvider = callbackProvider;
        this.tag = tag;
    }

    @Override
    public void onPreExecute() {
        if (BuildConfig.DEBUG) {
            Log.v(tag, "onPreExecute");
        }
        if (callbackProvider.getCallback() != null) {
            callbackProvider.getCallback().onPreExecute(this);
        }
    }

    @Override
    public void onPostExecute(Result result) {
        if (BuildConfig.DEBUG) {
            Log.v(tag, "onPostExecute: " + result);
        }
        if (callbackProvider.getCallback() != null) {
            callbackProvider.getCallback().onPostExecute(this, result);
        }
    }

    @Override
    public void onProgressUpdate(Progress... values) {
        if (BuildConfig.DEBUG) {
            Log.v(tag, "onProgressUpdate: " + Arrays.toString(values));
        }
        if (callbackProvider.getCallback() != null) {
            callbackProvider.getCallback().onProgressUpdate(this, values);
        }
    }

    @Override
    public void onCancelled() {
        if (BuildConfig.DEBUG) {
            Log.v(tag, "onCancelled");
        }
        if (callbackProvider.getCallback() != null) {
            callbackProvider.getCallback().onCancelled(this);
        }
    }

}
