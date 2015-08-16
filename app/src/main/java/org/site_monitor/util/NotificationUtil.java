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

package org.site_monitor.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.site_monitor.BuildConfig;
import org.site_monitor.R;
import org.site_monitor.activity.PrefSettingsActivity;

/**
 * Created by norbert on 04/07/2015.
 */
public class NotificationUtil {

    public static int ID_NOT_REACHABLE = 1;


    /**
     * Sends notification. If BuildConfig.DEBUG && typeId < 0 ignore command.
     *
     * @param context
     * @param typeId
     * @param title
     * @param text
     * @param pendingIntent
     */
    public static void sendNotification(Context context, int typeId, String title, String text, PendingIntent pendingIntent) {
        if (BuildConfig.DEBUG && typeId < 0) {
            return;
        }
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean notificationEnable = preferences.getBoolean(PrefSettingsActivity.NOTIFICATION_ENABLE, false);
        if (typeId == ID_NOT_REACHABLE && !notificationEnable) {
            if (BuildConfig.DEBUG) {
                Log.d("NotificationUtil", "doesn't send notification (disabled)");
            }
            return;
        }
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);
        notificationBuilder.setSmallIcon(R.drawable.ic_app);
        notificationBuilder.setColor(context.getResources().getColor(R.color.primary));
        notificationBuilder.setCategory(Notification.CATEGORY_SERVICE);
        notificationBuilder.setContentTitle(title).setContentText(text);
        notificationBuilder.setAutoCancel(true);

        String ringtonePath = preferences.getString(PrefSettingsActivity.NOTIFICATIONS_RINGTONE, null);
        Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        if (ringtonePath != null && !ringtonePath.isEmpty()) {
            ringtoneUri = Uri.parse(ringtonePath);
        }

        notificationBuilder.setSound(ringtoneUri);
        String colorString = preferences.getString(PrefSettingsActivity.NOTIFICATION_LIGHT_COLOR, Color.WHITE + "");
        int color = Integer.valueOf(colorString);
        notificationBuilder.setLights(color, TimeUtil._1_SEC_INT * 2, TimeUtil._1_SEC_INT * 10);
        if (preferences.getBoolean(PrefSettingsActivity.NOTIFICATIONS_VIBRATE, false)) {
            long[] pattern = {NotificationCompat.DEFAULT_VIBRATE * TimeUtil.SEC_2_MILLISEC};
            notificationBuilder.setVibrate(pattern);
        }
        notificationBuilder.setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(typeId, notificationBuilder.build());

    }

    public static void removeNotification(Context context, int typeId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(typeId);
    }

}
