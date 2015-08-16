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

package org.app4life.sitemonitor.task;

/**
 * Created by norbert on 25/07/2015.
 * Util implementation: default do nothing.
 */
public class TaskCallbackDefault<Task, Progress, Result> implements TaskCallback<Task, Progress, Result> {
    @Override
    public void onPreExecute(Task task) {

    }

    @Override
    public void onProgressUpdate(Task task, Progress... percent) {

    }

    @Override
    public void onPostExecute(Task task, Result result) {

    }

    @Override
    public void onCancelled(Task task) {

    }
}
