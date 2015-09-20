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

package org.site_monitor.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.site_monitor.BuildConfig;
import org.site_monitor.GA;
import org.site_monitor.GAHit;
import org.site_monitor.R;
import org.site_monitor.activity.MainActivity;
import org.site_monitor.service.NetworkService;

/**
 * Created by Martin Norbert on 20/09/2015.
 */
public class WidgetManager {

    private static final String TAG = "WidgetManager";

    /**
     * Sends broadcast to existing widgets for refresh.
     *
     * @param context
     */
    public static void refresh(Context context) {
        sendRefreshTo(context, LineWidget.class);
        sendRefreshTo(context, SquareWidget.class);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "refresh sent");
        }
    }

    private static void sendRefreshTo(Context context, Class clazz) {
        Intent intent = new Intent(context, clazz);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int ids[] = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, clazz));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.sendBroadcast(intent);
    }

    static PendingIntent onClickIntent(Context context) {
        return PendingIntent.getActivity(context, 1, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static void onEnabled(Context context) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onEnabled");
        }
        GA.tracker().send(GAHit.builder().event(R.string.c_widget, R.string.a_add).build());
        context.startService(new Intent(context, NetworkService.class));
    }

    public static void onDisabled(Context context) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onDisabled");
        }
        GA.tracker().send(GAHit.builder().event(R.string.c_widget, R.string.a_remove).build());
    }

    public static void onUpdate(Context context, SiteMonitorWidget provider, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            provider.updateAppWidget(context, appWidgetManager, appWidgetIds[i]);
        }
    }
}
