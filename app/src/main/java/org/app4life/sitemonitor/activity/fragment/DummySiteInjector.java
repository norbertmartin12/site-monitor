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

package org.app4life.sitemonitor.activity.fragment;

import android.content.Context;
import android.widget.Toast;

import org.app4life.sitemonitor.model.adapter.SiteSettingsManager;
import org.app4life.sitemonitor.model.bo.SiteSettings;
import org.app4life.sitemonitor.task.NetworkTask;
import org.app4life.sitemonitor.task.TaskCallback;

/**
 * Created by norbert on 13/08/2015.
 */
public class DummySiteInjector {

    private static final String[] HOSTS = {"soanity.fr", "soanity.com", "192.168.1.10", "http://www.alittlemarket.com/boutique/soanity-709555.html", "https://m.facebook.com/soanity?refsrc=https%3A%2F%2Ffr-fr.facebook.com%2Fsoanity", "ghost.jbrieu.info", "home.jbrieu.info"};

    public static void inject(Context context, TaskCallback.Provider callbackProvider, SiteSettingsManager siteSettingsManager) {
        Toast.makeText(context, "inject dummy data", Toast.LENGTH_LONG).show();
        for (String host : HOSTS) {
            SiteSettings siteSettings = new SiteSettings(host, true);
            siteSettingsManager.add(context, siteSettings);
            new NetworkTask(context, callbackProvider).execute(siteSettings);
        }

    }
}
