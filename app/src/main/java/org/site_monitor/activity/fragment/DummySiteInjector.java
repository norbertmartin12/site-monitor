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

import android.view.View;

import com.google.android.material.snackbar.Snackbar;

import org.site_monitor.model.bo.SiteSettings;
import org.site_monitor.model.db.DBSiteSettings;
import org.site_monitor.task.CallSiteTask;
import org.site_monitor.task.TaskCallback;

import java.sql.SQLException;

/**
 * Created by norbert on 13/08/2015.
 */
public class DummySiteInjector {

    private static final String[] HOSTS = {"192.168.1.9", "https://www.alittlemarket.com/boutique/soanity-709555.html", "home.jbrieu.info"};

    public static void inject(View view, TaskCallback.Provider callbackProvider, DBSiteSettings dbSiteSettings) throws SQLException {
        Snackbar.make(view, "inject dummy data", Snackbar.LENGTH_LONG).show();
        for (String host : HOSTS) {
            SiteSettings siteSettings = new SiteSettings(host);
            dbSiteSettings.create(siteSettings);
            new CallSiteTask(view.getContext(), callbackProvider).execute(siteSettings);
        }

    }
}
