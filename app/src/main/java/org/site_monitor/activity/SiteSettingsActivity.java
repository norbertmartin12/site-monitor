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

package org.site_monitor.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import org.site_monitor.R;
import org.site_monitor.activity.fragment.SiteSettingsActivityFragment;
import org.site_monitor.model.adapter.SiteSettingsManager;
import org.site_monitor.model.bo.SiteSettings;
import org.site_monitor.task.NetworkTask;
import org.site_monitor.task.TaskCallback;

public class SiteSettingsActivity extends FragmentActivity implements SiteSettingsActivityFragment.Callback, TaskCallback<NetworkTask, Void, SiteSettings> {

    private static final String P_SITE_SETTINGS = "org.site_monitor.activity.SiteSettingsActivity.site";
    private static final String TAG_TASK_FRAGMENT = "site_settings_activity_task_fragment";
    private SiteSettings siteSettings;
    private MenuItem syncMenuItem;
    private SiteSettingsActivityFragment siteSettingsFragment;
    private Context context;
    private boolean hasBeenModified = false;

    public static void start(Context context, String siteSettingsUrl) {
        Intent intent = new Intent(context, SiteSettingsActivity.class).putExtra(P_SITE_SETTINGS, siteSettingsUrl);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = this;
        setContentView(R.layout.activity_site_settings);
        String url = getIntent().getStringExtra(P_SITE_SETTINGS);
        if (url == null) {
            Toast.makeText(this, R.string.site_not_found, Toast.LENGTH_SHORT).show();
            finish();
        }
        siteSettings = SiteSettingsManager.instance(this).getBy(url);
        FragmentManager fragmentManager = getSupportFragmentManager();
        siteSettingsFragment = (SiteSettingsActivityFragment) fragmentManager.findFragmentByTag(TAG_TASK_FRAGMENT);
        if (siteSettingsFragment == null) {
            siteSettingsFragment = (SiteSettingsActivityFragment) fragmentManager.findFragmentById(R.id.fragment_site_settings);
        }
        siteSettingsFragment.setSiteSettings(siteSettings);
        setTitle(siteSettings.getName());
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (hasBeenModified) {
            SiteSettingsManager.instance(this).saveSiteSettings(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hasBeenModified = false;
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
            if (siteSettings.isChecking()) {
                return true;
            }
            syncMenuItem.setEnabled(false);
            new NetworkTask(this, siteSettingsFragment).execute(siteSettings);
            return true;
        }
        if (id == R.id.action_rename) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.action_rename);
            final EditText input = new EditText(context);
            input.setHint(R.string.hint_rename_site);
            input.setText(siteSettings.getName());
            input.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            builder.setView(input);
            builder.setPositiveButton(R.string.action_rename, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        name = getString(R.string.no_name);
                    }
                    siteSettings.setName(name);
                    setTitle(siteSettings.getName());
                    hasBeenModified = true;
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
        if (id == R.id.action_delete) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.delete_current_monitor);
            builder.setPositiveButton(R.string.action_delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    SiteSettingsManager.instance(context).remove(context, siteSettings);
                    hasBeenModified = true;
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
        hasBeenModified = true;
    }

    @Override
    public void onPreExecute(NetworkTask task) {
    }

    @Override
    public void onProgressUpdate(NetworkTask task, Void... percent) {
    }

    @Override
    public void onPostExecute(NetworkTask task, SiteSettings siteSettings) {
        if (this.siteSettings.getHost().equals(siteSettings.getHost())) {
            if (syncMenuItem != null) {
                syncMenuItem.setEnabled(true);
            }
            hasBeenModified = true;
        }
    }

    @Override
    public void onCancelled(NetworkTask task) {
    }

}
