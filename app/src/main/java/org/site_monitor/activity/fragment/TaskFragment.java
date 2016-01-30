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

package org.site_monitor.activity.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import org.site_monitor.task.TaskCallback;

/**
 * Created by norbert on 19/07/2015.
 * Util fragment that allow to any activity to manage a call back for async task even if screen orientation change and current activity is delete and re create.<nr/>
 * Link with callback activity is done on #onAttach fragment with the activity.
 */
public class TaskFragment extends Fragment implements TaskCallback.Provider {

    private TaskCallback callback;

    @Override
    public TaskCallback getCallback() {
        return callback;
    }

    /**
     * Hold a reference to the parent context so we can report the
     * task's current progress and results. The Android framework
     * will pass us a reference to the newly created Activity after
     * each configuration change.
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.callback = (TaskCallback) context;
    }

    /**
     * This method will only be called once when the retained
     * Fragment is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    /**
     * Set the callback to null so we don't accidentally leak the
     * Activity instance.
     */
    @Override
    public void onDetach() {
        super.onDetach();
        callback = null;
    }
}
