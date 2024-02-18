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

package org.site_monitor.widget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.site_monitor.BuildConfig;

/**
 * Created by Martin Norbert on 20/09/2015.
 */
public class WidgetManager {

    private static final String TAG = "WidgetManager";

    /**
     * Sends broadcast to existing widgets for refresh.
     *
     * @param context to use to build new intent
     */
    public static void refresh(Context context) {
        sendRefreshTo(context, LineWidget.class);
        sendRefreshTo(context, SquareWidget.class);
    }

    private static void sendRefreshTo(Context context, Class<?> clazz) {
        Intent intent = new Intent(context, clazz);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, clazz));
        if (ids.length > 0) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "refresh: " + clazz.getSimpleName());
            }
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            context.sendBroadcast(intent);
        }
    }

}
