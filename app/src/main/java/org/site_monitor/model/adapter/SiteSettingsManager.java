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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.reflect.TypeToken;

import org.site_monitor.BuildConfig;
import org.site_monitor.activity.PrefSettingsActivity;
import org.site_monitor.model.bo.SiteSettings;
import org.site_monitor.receiver.AlarmReceiver;
import org.site_monitor.util.GsonUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by norbert on 19/07/2015.
 */
public class SiteSettingsManager implements Serializable {
    private static final String TAG = "SiteSettingsManager";
    private static SiteSettingsManager instance;
    private final List<SiteSettings> siteSettingsList;
    private SiteSettingsAdapter siteSettingsAdapter;

    private SiteSettingsManager(Context context) {
        this.siteSettingsList = new ArrayList<SiteSettings>();
        retrieveSiteSettings(context);
    }

    public static SiteSettingsManager instance(Context context) {
        if (instance == null) {
            instance = new SiteSettingsManager(context);
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
        String json = GsonUtil.toJson(siteSettingsList);
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        defaultSharedPreferences.edit().putString(PrefSettingsActivity.JSON_SITE_SETTINGS, json).commit();
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "saveSiteSettings");
        }
    }

    private synchronized void retrieveSiteSettings(Context context) {
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String json = defaultSharedPreferences.getString(PrefSettingsActivity.JSON_SITE_SETTINGS, "");
        List<SiteSettings> retrievedList = null;
        if (!json.isEmpty()) {
            retrievedList = GsonUtil.fromJson(json, new TypeToken<List<SiteSettings>>() {
            });
        } else {
            retrievedList = new ArrayList<SiteSettings>();
        }
        siteSettingsList.clear();
        siteSettingsList.addAll(retrievedList);
        Collections.sort(siteSettingsList);
        if (this.siteSettingsAdapter != null) {
            this.siteSettingsAdapter.notifyDataSetChanged();
        }
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
}
