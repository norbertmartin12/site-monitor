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
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import org.site_monitor.BuildConfig;
import org.site_monitor.GA;
import org.site_monitor.GAHit;
import org.site_monitor.R;
import org.site_monitor.activity.MainActivity;
import org.site_monitor.model.adapter.SiteSettingsManager;
import org.site_monitor.model.bo.NetworkCallResult;
import org.site_monitor.model.bo.SiteCall;
import org.site_monitor.model.bo.SiteSettings;
import org.site_monitor.service.NetworkService;

import java.util.List;

/**
 * Implementation of App Widget functionality.
 */
public class SiteMonitorWidget extends AppWidgetProvider {
    public static final String COMA = ", ";
    public static final String BG_COLOR = "setBackgroundResource";
    private static final String TAG = "SiteMonitorWidget";

    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.site_monitor_widget);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.widgetView, pendingIntent);

        StringBuilder sb = new StringBuilder();
        List<SiteSettings> siteSettingsList = SiteSettingsManager.instance(context).getSiteSettingsUnmodifiableList();
        if (siteSettingsList.isEmpty()) {
            views.setTextViewText(R.id.widgetTextView, context.getString(R.string.widget_no_site));
            views.setInt(R.id.widgetBackgroundImage, BG_COLOR, R.color.state_unknown);
        } else {
            for (SiteSettings siteSettings : siteSettingsList) {
                SiteCall siteCall = siteSettings.getLastCall();
                if (siteCall != null && siteCall.getResult() == NetworkCallResult.FAIL) {
                    if (sb.length() > 0) {
                        sb.append(COMA);
                    }
                    sb.append(siteSettings.getName());
                }
            }
            if (sb.length() == 0) {
                views.setTextViewText(R.id.widgetTextView, context.getText(R.string.widget_all_ok));
                views.setInt(R.id.widgetBackgroundImage, BG_COLOR, R.color.state_success);
            } else {
                views.setTextViewText(R.id.widgetTextView, sb.toString());
                views.setInt(R.id.widgetBackgroundImage, BG_COLOR, R.color.state_fail);
            }
        }
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    public static void refresh(Context context) {
        Intent intent = new Intent(context, SiteMonitorWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int ids[] = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, SiteMonitorWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.sendBroadcast(intent);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "external refresh");
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onEnabled");
        }
        GA.tracker().send(GAHit.builder().event(R.string.c_widget, R.string.a_add).build());
        context.startService(new Intent(context, NetworkService.class));
    }

    @Override
    public void onDisabled(Context context) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onDisabled");
        }
        GA.tracker().send(GAHit.builder().event(R.string.c_widget, R.string.a_remove).build());
    }
}

