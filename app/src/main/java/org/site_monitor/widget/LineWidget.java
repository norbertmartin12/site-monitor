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
import android.content.Context;
import android.util.Log;
import android.widget.RemoteViews;

import org.site_monitor.R;
import org.site_monitor.model.adapter.SiteSettingsBusiness;
import org.site_monitor.model.bo.NetworkCallResult;
import org.site_monitor.model.bo.SiteCall;
import org.site_monitor.model.bo.SiteSettings;
import org.site_monitor.model.db.DBHelper;
import org.site_monitor.model.db.DBSiteSettings;

import java.sql.SQLException;
import java.util.List;

/**
 * Implementation of App Widget functionality.
 */
public class LineWidget extends SiteMonitorWidget {
    private static final String COMA = ", ";
    private static final String TAG = LineWidget.class.getSimpleName();

    @Override
    public void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.line_widget);
        views.setOnClickPendingIntent(R.id.widgetView, onClickIntent(context));

        StringBuilder sb = new StringBuilder();
        DBHelper dbHelper = DBHelper.getHelper(context);
        try {
            DBSiteSettings siteSettingDao = dbHelper.getDBSiteSettings();
            List<SiteSettings> siteSettingsList = siteSettingDao.queryForAll();
            if (siteSettingsList.isEmpty()) {
                views.setTextViewText(R.id.widgetTextView, context.getString(R.string.widget_no_site));
                views.setInt(R.id.widgetBackgroundImage, BG_COLOR, R.color.state_unknown);
            } else {
                for (SiteSettings siteSettings : siteSettingsList) {
                    SiteCall siteCall = new SiteSettingsBusiness(siteSettings).getLastCall();
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
        } catch (SQLException e) {
            Log.e(TAG, "updateAppWidget", e);
        } finally {
            dbHelper.release();
        }
    }
}

