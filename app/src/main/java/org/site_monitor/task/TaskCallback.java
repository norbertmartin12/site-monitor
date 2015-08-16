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

import android.os.AsyncTask;

/**
 * Created by norbert on 19/07/2015.
 * TaskCallback interface is designed to be used easily with {@link AsyncTask}.
 *
 * @see TaskCallbackDefault
 */
public interface TaskCallback<Task, Progress, Result> {
    void onPreExecute(Task task);

    void onProgressUpdate(Task task, Progress... percent);

    void onPostExecute(Task task, Result result);

    void onCancelled(Task task);

    /**
     * Interface that offers to {@link AsyncTask} it's callback.
     *
     * @param <Task>
     * @param <Progress>
     * @param <Result>
     */
    interface Provider<Task, Progress, Result> {
        TaskCallback<Task, Progress, Result> getCallback();
    }
}

