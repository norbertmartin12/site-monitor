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

package org.app4life.sitemonitor.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.app4life.sitemonitor.R;
import org.app4life.sitemonitor.activity.fragment.SiteSettingsActivityFragment;
import org.app4life.sitemonitor.model.adapter.SiteSettingsManager;
import org.app4life.sitemonitor.model.bo.SiteSettings;
import org.app4life.sitemonitor.task.NetworkTask;
import org.app4life.sitemonitor.task.TaskCallback;

public class SiteSettingsActivity extends FragmentActivity implements SiteSettingsActivityFragment.Callback, TaskCallback<NetworkTask, Void, SiteSettings> {

    private static final String P_SITE_SETTINGS_ID = "org.app4life.sitemonitor.activity.SiteSettingsActivity.site";
    private static final String TAG_TASK_FRAGMENT = "site_settings_activity_task_fragment";
    private SiteSettings siteSetting;
    private MenuItem syncMenuItem;
    private SiteSettingsActivityFragment siteSettingsFragment;
    private Context context;

    public static void start(Context context, int siteSettingsIndex) {
        Intent intent = new Intent(context, SiteSettingsActivity.class).putExtra(P_SITE_SETTINGS_ID, siteSettingsIndex);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = this;
        setContentView(R.layout.activity_site_settings);
        int index = getIntent().getIntExtra(P_SITE_SETTINGS_ID, -1);
        if (index == -1) {
            Toast.makeText(this, R.string.site_not_found, Toast.LENGTH_SHORT).show();
            finish();
        }
        siteSetting = SiteSettingsManager.instance(this).getSiteSettingsUnmodifiableList().get(index);
        FragmentManager fragmentManager = getSupportFragmentManager();
        siteSettingsFragment = (SiteSettingsActivityFragment) fragmentManager.findFragmentByTag(TAG_TASK_FRAGMENT);
        if (siteSettingsFragment == null) {
            siteSettingsFragment = (SiteSettingsActivityFragment) fragmentManager.findFragmentById(R.id.fragment_site_settings);
        }
        siteSettingsFragment.setSiteSettings(siteSetting);
        setTitle(siteSetting.getHost());

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_site_settings, menu);
        syncMenuItem = menu.findItem(R.id.action_refresh);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            new NetworkTask(this, siteSettingsFragment).execute(siteSetting);
            item.setEnabled(false);
            return true;
        }
        if (id == R.id.action_delete) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.delete_current_monitor);
            builder.setPositiveButton(R.string.action_delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    SiteSettingsManager.instance(context).remove(context, siteSetting);
                    SiteSettingsManager.instance(context).saveSiteSettings(context);
                    finish();
                }
            });

            builder.setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            builder.show();
            return true;
        }
        return false;
    }

    @Override
    public void hasChanged(SiteSettings siteSettings) {
        SiteSettingsManager.instance(this).saveSiteSettings(this);
    }

    @Override
    public void onPreExecute(NetworkTask task) {

    }

    @Override
    public void onProgressUpdate(NetworkTask task, Void... percent) {

    }

    @Override
    public void onPostExecute(NetworkTask task, SiteSettings siteSettings) {
        if (siteSetting.getHost().equals(siteSettings.getHost())) {
            siteSettingsFragment.refresh();
            SiteSettingsManager.instance(this).saveSiteSettings(this);
            if (syncMenuItem != null) {
                syncMenuItem.setEnabled(true);
            }
        }
    }

    @Override
    public void onCancelled(NetworkTask task) {

    }


}
