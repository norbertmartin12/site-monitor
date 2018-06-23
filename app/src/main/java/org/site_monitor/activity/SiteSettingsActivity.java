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

package org.site_monitor.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import org.site_monitor.R;
import org.site_monitor.activity.fragment.SiteSettingsActivityFragment;
import org.site_monitor.model.adapter.SiteSettingsBusiness;
import org.site_monitor.model.bo.SiteCall;
import org.site_monitor.model.bo.SiteSettings;
import org.site_monitor.model.db.DBHelper;
import org.site_monitor.model.db.DBSiteSettings;
import org.site_monitor.service.NetworkService;
import org.site_monitor.task.NetworkTask;
import org.site_monitor.task.TaskCallback;
import org.site_monitor.util.AlarmUtil;
import org.site_monitor.widget.WidgetManager;

import java.sql.SQLException;

public class SiteSettingsActivity extends AppCompatActivity implements SiteSettingsActivityFragment.Callback, TaskCallback<NetworkTask, Void, Pair<SiteSettings, SiteCall>> {

    private static final String TAG = SiteSettingsActivity.class.getSimpleName();
    private static final String P_SITE_SETTINGS = "org.site_monitor.activity.SiteSettingsActivity.site";
    private static final String TAG_TASK_FRAGMENT = "site_settings_activity_task_fragment";
    private static final String PARCEL_SITE = "site";
    private SiteSettingsBusiness siteSettings;
    private MenuItem syncMenuItem;
    private MenuItem addInternalIpMenuItem;
    private SiteSettingsActivityFragment siteSettingsFragment;
    private SiteSettingsActivity context;
    private DBHelper dbHelper;

    public static void start(Context context, String siteSettingsUrl) {
        Intent intent = new Intent(context, SiteSettingsActivity.class).putExtra(P_SITE_SETTINGS, siteSettingsUrl);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        String url = getIntent().getStringExtra(P_SITE_SETTINGS);
        if (url == null) {
            Toast.makeText(this, R.string.site_not_found, Toast.LENGTH_SHORT).show();
            finish();
        }
        dbHelper = DBHelper.getHelper(context);
        if (savedInstanceState == null || savedInstanceState.isEmpty()) {
            try {
                startService(NetworkService.intentToLoadFavicon(this, url));
                SiteSettings dbSiteSettings = dbHelper.getDBSiteSettings().findForHost(url);
                if (dbSiteSettings == null) {
                    Toast.makeText(this, R.string.site_not_found, Toast.LENGTH_SHORT).show();
                    finish();
                }
                siteSettings = new SiteSettingsBusiness(dbSiteSettings);
            } catch (SQLException e) {
                Log.e(TAG, "search for host", e);
                Toast.makeText(this, R.string.site_not_found, Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            siteSettings = savedInstanceState.getParcelable(PARCEL_SITE);
        }
        setTitle(siteSettings.getName());
        setContentView(R.layout.activity_site_settings);

        FragmentManager fragmentManager = getSupportFragmentManager();
        siteSettingsFragment = (SiteSettingsActivityFragment) fragmentManager.findFragmentByTag(TAG_TASK_FRAGMENT);
        if (siteSettingsFragment == null) {
            siteSettingsFragment = (SiteSettingsActivityFragment) fragmentManager.findFragmentById(R.id.fragment_site_settings);
        }
        siteSettingsFragment.setSiteSettings(siteSettings);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.release();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(PARCEL_SITE, siteSettings);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_site_settings, menu);
        syncMenuItem = menu.findItem(R.id.action_refresh);
        addInternalIpMenuItem = menu.findItem(R.id.action_add_internal_ip);
        addInternalIpMenuItem.setChecked(siteSettings.getInternalUrl() != null);
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
            new NetworkTask(this, siteSettingsFragment).execute(siteSettings.getSiteSettings());
            return true;
        }
        if (id == R.id.action_rename) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.action_rename);
            final EditText input = new EditText(context);
            input.setHint(R.string.hint_rename_site);
            input.setText(siteSettings.getName());
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            builder.setView(input);
            builder.setPositiveButton(R.string.action_rename, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        name = getString(R.string.no_name);
                    }
                    siteSettings.getSiteSettings().setName(name);
                    setTitle(siteSettings.getName());
                    try {
                        DBSiteSettings dbSiteSettings = dbHelper.getDBSiteSettings();
                        dbSiteSettings.update(siteSettings.getSiteSettings());
                        WidgetManager.refresh(context);
                    } catch (SQLException e) {
                        Log.e(TAG, "rename", e);
                    }

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
            builder.setMessage(R.string.remove_current_monitor);
            builder.setPositiveButton(R.string.action_delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        DBSiteSettings dbSiteSettings = dbHelper.getDBSiteSettings();
                        dbSiteSettings.delete(siteSettings.getSiteSettings());
                        AlarmUtil.instance().stopAlarmIfNeeded(context);
                    } catch (SQLException e) {
                        Log.e(TAG, "remove", e);
                    }
                    WidgetManager.refresh(context);
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
        if (id == R.id.action_add_internal_ip) {
            if (addInternalIpMenuItem.isChecked()) {
                try {
                    siteSettings.getSiteSettings().setInternalUrl(null);
                    DBSiteSettings dbSiteSettings = dbHelper.getDBSiteSettings();
                    dbSiteSettings.update(siteSettings.getSiteSettings());
                    addInternalIpMenuItem.setChecked(!addInternalIpMenuItem.isChecked());
                    Snackbar.make(this.getCurrentFocus(), R.string.internal_url_removed, Snackbar.LENGTH_SHORT).show();
                    siteSettingsFragment.setSiteSettings(siteSettings);
                } catch (SQLException e) {
                    Log.e(TAG, "update", e);
                }
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(R.string.add_internal_url);
                final EditText input = new EditText(context);
                input.setHint(R.string.hint_interval_site_url);
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_URI);
                builder.setView(input);
                builder.setPositiveButton(R.string.action_add, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String internalIp = input.getText().toString().trim();
                        if (internalIp.isEmpty()) {
                            return;
                        }
                        try {
                            siteSettings.getSiteSettings().setInternalUrl(internalIp);
                            DBSiteSettings dbSiteSettings = dbHelper.getDBSiteSettings();
                            dbSiteSettings.update(siteSettings.getSiteSettings());
                            addInternalIpMenuItem.setChecked(!addInternalIpMenuItem.isChecked());
                            Snackbar.make(context.getCurrentFocus(), R.string.internal_url_added, Snackbar.LENGTH_SHORT).show();
                            siteSettingsFragment.setSiteSettings(siteSettings);
                        } catch (SQLException e) {
                            Log.e(TAG, "update", e);
                        }
                    }
                });
                builder.setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                builder.show();
            }
            return true;
        }
        return false;
    }

    @Override
    public void hasChanged(SiteSettings siteSettings) {
        try {
            DBSiteSettings dbSiteSettings = dbHelper.getDBSiteSettings();
            dbSiteSettings.update(siteSettings);
        } catch (SQLException e) {
            Log.e(TAG, "hasChanged", e);
        }
    }

    @Override
    public void onPreExecute(NetworkTask task) {
    }

    @Override
    public void onProgressUpdate(NetworkTask task, Void... percent) {
    }

    @Override
    public void onPostExecute(NetworkTask task, Pair<SiteSettings, SiteCall> result) {
        if (this.siteSettings.getHost().equals(siteSettings.getHost())) {
            if (syncMenuItem != null) {
                syncMenuItem.setEnabled(true);
            }
        }
    }

    @Override
    public void onCancelled(NetworkTask task) {
    }

}
