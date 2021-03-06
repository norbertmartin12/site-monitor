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

package org.site_monitor.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import org.site_monitor.App;
import org.site_monitor.BuildConfig;
import org.site_monitor.R;
import org.site_monitor.activity.PrefSettingsActivity;

import androidx.core.app.NotificationCompat;

/**
 * Created by norbert on 04/07/2015.
 */
public class NotificationUtil {

    private static final String TAG = NotificationUtil.class.getSimpleName();
    private static final String CHANNEL_ID_ALERTS = "ALERTS";
    public static int ID_NOT_REACHABLE = 1;

    public static void createNotificationChannel(Context context) {
        // Create the NotificationChannel, but only on API 26+ because the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_ALERTS, context.getString(R.string.channel_title), NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(context.getString(R.string.channel_description));
            // Register the channel with the system; you can't change the importance or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Pre builds notification.
     */
    public static NotificationCompat.Builder build(Context context, String title, String text, PendingIntent pendingIntent) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID_ALERTS);
        notificationBuilder.setSmallIcon(R.drawable.ic_app);
        if (!BuildConfig.DEBUG) {
            notificationBuilder.setColor(context.getResources().getColor(R.color.primary));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setCategory(Notification.CATEGORY_SERVICE);
        }
        notificationBuilder.setContentTitle(title).setContentText(text);
        notificationBuilder.setAutoCancel(true);

        String ringtonePath = preferences.getString(PrefSettingsActivity.NOTIFICATIONS_RINGTONE, null);
        Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        if (ringtonePath != null && !ringtonePath.isEmpty()) {
            ringtoneUri = Uri.parse(ringtonePath);
        }

        notificationBuilder.setSound(ringtoneUri);
        String colorString = preferences.getString(PrefSettingsActivity.NOTIFICATION_LIGHT_COLOR, Color.WHITE + "");
        int color = Integer.parseInt(colorString);
        notificationBuilder.setLights(color, TimeUtil._1_SEC_INT * 2, TimeUtil._1_SEC_INT * 10);
        if (preferences.getBoolean(PrefSettingsActivity.NOTIFICATIONS_VIBRATE, false)) {
            long[] pattern = {NotificationCompat.DEFAULT_VIBRATE * TimeUtil.SEC_2_MILLISEC};
            notificationBuilder.setVibrate(pattern);
        }
        notificationBuilder.setContentIntent(pendingIntent);
        return notificationBuilder;
    }

    /**
     * @return false if not sent {@see #shouldNotify}
     */
    public static boolean send(Context context, int typeId, Notification notification) {
        //if (!shouldNotify(context)) {
        //     return false;
        // }
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(typeId, notification);
        return true;
    }

    /**
     * @return false if app foreground or notification disabled
     */
    public static boolean shouldNotify(Context context) {
        if (App.isForeground()) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "doesn't send notification (app foreground)");
            }
            return false;
        }
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean notificationEnable = preferences.getBoolean(PrefSettingsActivity.NOTIFICATION_ENABLE, false);
        if (!notificationEnable) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "doesn't send notification (disabled)");
            }
            return false;
        }
        return true;
    }
}
