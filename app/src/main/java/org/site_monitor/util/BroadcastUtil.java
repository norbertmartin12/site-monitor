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

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.content.LocalBroadcastManager;

import org.site_monitor.model.bo.SiteCall;
import org.site_monitor.model.bo.SiteSettings;

/**
 * Created by Martin Norbert on 31/01/2016.
 */
public class BroadcastUtil {

    public static final String EXTRA_SITE = "org.site_monitor.service.extra.SITE";
    public static final String EXTRA_CALL = "org.site_monitor.service.extra.CALL";
    public static final String EXTRA_ALARM = "org.site_monitor.service.extra.ALARM";
    public static final String EXTRA_FAVICON = "org.site_monitor.service.extra.FAVICON";

    /**
     * Broadcasts siteSettings for given action as EXTRA_SITE
     *
     * @param context
     * @param action
     * @param siteSettings
     */
    public static void broadcast(Context context, String action, SiteSettings siteSettings) {
        Intent localIntent = new Intent(action).putExtra(EXTRA_SITE, siteSettings);
        LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);
    }

    /**
     * Broadcasts siteSettings for given action as EXTRA_SITE
     *
     * @param context
     * @param action
     * @param extraName
     * @param bitmap
     */
    public static void broadcast(Context context, String action, SiteSettings siteSettings, String extraName, Bitmap bitmap) {
        Intent localIntent = new Intent(action).putExtra(EXTRA_SITE, siteSettings).putExtra(extraName, bitmap);
        LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);
    }

    /**
     * Broadcasts long for given action as EXTRA_SITE
     *
     * @param context
     * @param action
     * @param extraName
     * @param value
     */
    public static void broadcast(Context context, String action, String extraName, long value) {
        Intent localIntent = new Intent(action).putExtra(extraName, value);
        LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);
    }

    /**
     * Broadcasts siteSettings for given action as EXTRA_SITE
     *
     * @param context
     * @param action
     * @param siteSettings
     */
    public static void broadcast(Context context, String action, SiteSettings siteSettings, SiteCall siteCall) {
        Intent localIntent = new Intent(action).putExtra(EXTRA_SITE, siteSettings).putExtra(EXTRA_CALL, siteCall);
        LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);
    }

}
