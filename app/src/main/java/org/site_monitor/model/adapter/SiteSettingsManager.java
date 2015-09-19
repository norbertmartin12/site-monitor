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

package org.site_monitor.model.adapter;

import android.content.Context;

import org.site_monitor.model.bo.SiteSettings;
import org.site_monitor.receiver.AlarmReceiver;
import org.site_monitor.service.DataStoreService;
import org.site_monitor.task.DataStoreTask;
import org.site_monitor.task.TaskCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by norbert on 19/07/2015.
 */
public class SiteSettingsManager implements TaskCallback<DataStoreTask, Void, List<SiteSettings>>, TaskCallback.Provider {
    private static final String TAG = "SiteSettingsManager";
    private static SiteSettingsManager instance;
    private final List<SiteSettings> siteSettingsList = new ArrayList<SiteSettings>();
    private SiteSettingsAdapter siteSettingsAdapter;

    public static SiteSettingsManager instance(Context context) {
        if (instance == null) {
            instance = new SiteSettingsManager();
            instance.loadSiteSettings(context);
        }
        return instance;
    }

    public void stopAlarmIfNeeded(Context context) {
        if (siteSettingsList.size() == 0) {
            AlarmReceiver.stopAlarm(context);
        }
    }

    public void startAlarmIfNeeded(Context context) {
        if (siteSettingsList.size() > 0) {
            AlarmReceiver.startAlarm(context);
        }
    }

    public List<SiteSettings> getSiteSettingsUnmodifiableList() {
        return Collections.unmodifiableList(new ArrayList<SiteSettings>(siteSettingsList));
    }

    public int size() {
        return siteSettingsList.size();
    }

    public SiteSettingsAdapter getArrayAdapter(Context context) {
        if (siteSettingsAdapter == null) {
            siteSettingsAdapter = new SiteSettingsAdapter(context, this);
        }
        return siteSettingsAdapter;
    }

    public void add(Context context, SiteSettings siteSettings) {
        siteSettingsList.add(siteSettings);
        Collections.sort(siteSettingsList);
        if (siteSettingsAdapter != null) {
            siteSettingsAdapter.notifyDataSetChanged();
        }
        startAlarmIfNeeded(context);
    }

    public void remove(Context context, SiteSettings siteSettings) {
        siteSettingsList.remove(siteSettings);
        Collections.sort(siteSettingsList);
        if (siteSettingsAdapter != null) {
            siteSettingsAdapter.notifyDataSetChanged();
        }
        stopAlarmIfNeeded(context);
    }

    public synchronized void saveSiteSettings(Context context) {
        DataStoreService.startActionSaveData(context, DataStoreService.KEY_JSON_SITE_SETTINGS, new ArrayList<SiteSettings>(siteSettingsList));
    }

    private synchronized void loadSiteSettings(final Context context) {
        new DataStoreTask(context, this).execute(DataStoreService.KEY_JSON_SITE_SETTINGS);
    }

    List<SiteSettings> getSiteSettingsList() {
        return siteSettingsList;
    }

    public boolean contains(SiteSettings siteSettings) {
        return siteSettingsList.contains(siteSettings);
    }

    public void refreshData() {
        Collections.sort(siteSettingsList);
        if (this.siteSettingsAdapter != null) {
            this.siteSettingsAdapter.notifyDataSetChanged();
        }
    }

    /**
     * @param host
     * @return found object or null
     */
    public SiteSettings getBy(String host) {
        for (SiteSettings siteSettings : siteSettingsList) {
            if (siteSettings.getHost().equals(host)) {
                return siteSettings;
            }
        }
        return null;
    }

    @Override
    public void onPreExecute(DataStoreTask task) {
    }

    @Override
    public void onProgressUpdate(DataStoreTask task, Void... percent) {
    }

    @Override
    public void onPostExecute(DataStoreTask task, List<SiteSettings> siteSettings) {
        siteSettingsList.clear();
        siteSettingsList.addAll(siteSettings);
        refreshData();
        startAlarmIfNeeded(task.getContext());
    }

    @Override
    public void onCancelled(DataStoreTask task) {
    }

    @Override
    public TaskCallback<DataStoreTask, Void, List<SiteSettings>> getCallback() {
        return this;
    }
}
