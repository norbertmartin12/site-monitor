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
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.site_monitor.BuildConfig;
import org.site_monitor.R;
import org.site_monitor.activity.fragment.DummySiteInjector;
import org.site_monitor.activity.fragment.TaskFragment;
import org.site_monitor.activity.fragment.floatingButton.FloatingButtonFragment;
import org.site_monitor.model.adapter.SiteSettingsManager;
import org.site_monitor.model.bo.SiteSettings;
import org.site_monitor.receiver.AlarmReceiver;
import org.site_monitor.receiver.BatteryLevelReceiver;
import org.site_monitor.receiver.NetworkServiceReceiver;
import org.site_monitor.receiver.StartupBootReceiver;
import org.site_monitor.service.NetworkService;
import org.site_monitor.task.NetworkTask;
import org.site_monitor.task.TaskCallback;
import org.site_monitor.util.ConnectivityUtil;


/**
 * Allow user to add and monitor site settings.
 */
public class MainActivity extends FragmentActivity implements TaskCallback<NetworkTask, Void, SiteSettings>, NetworkServiceReceiver.Listener {
    private static final String TAG_TASK_FRAGMENT = "main_activity_task_fragment";
    private static final String TAG = "MainActivity";

    private MainActivity context = this;
    private FloatingButtonFragment floatingButtonFragment;
    private ListView listView;
    private TextView connectivityAlertView;
    private TaskFragment taskFragment;

    private NetworkServiceReceiver networkServiceReceiver;
    private SiteSettingsManager siteSettingsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = (ListView) this.findViewById(R.id.listView);
        connectivityAlertView = (TextView) this.findViewById(R.id.connectivityAlert);
        FragmentManager fragmentManager = getSupportFragmentManager();
        taskFragment = (TaskFragment) fragmentManager.findFragmentByTag(TAG_TASK_FRAGMENT);
        if (taskFragment == null) {
            taskFragment = new TaskFragment();
            fragmentManager.beginTransaction().add(taskFragment, TAG_TASK_FRAGMENT).commit();
        }
        floatingButtonFragment = new FloatingButtonFragment();
        floatingButtonFragment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(R.string.add_monitor);
                final EditText input = new EditText(context);
                input.setHint(R.string.hint_site_url);
                input.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                builder.setView(input);
                builder.setPositiveButton(R.string.action_add, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String host = input.getText().toString().trim();
                        if (host.isEmpty()) {
                            return;
                        }
                        SiteSettings siteSettings = new SiteSettings(host, true);
                        if (siteSettingsManager.contains(siteSettings)) {
                            Toast.makeText(context, host + getString(R.string.already_exists), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        siteSettingsManager.add(context, siteSettings);
                        new NetworkTask(context, taskFragment).execute(siteSettings);
                        SiteSettingsActivity.start(context, siteSettings.getHost());
                    }
                });
                builder.setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                builder.show();
            }
        });
        fragmentManager.beginTransaction().replace(R.id.sample_content_fragment, floatingButtonFragment).commit();

        siteSettingsManager = SiteSettingsManager.instance(context);
        listView.setAdapter(siteSettingsManager.getArrayAdapter(context));

        siteSettingsManager.startAlarmIfNeeded(context);
    }

    @Override
    protected void onResume() {
        super.onResume();
        siteSettingsManager.refreshData();
        if (networkServiceReceiver == null && listView.getAdapter() != null) {
            networkServiceReceiver = new NetworkServiceReceiver(this);
        }
        onNetworkStateChanged(ConnectivityUtil.isConnectedOrConnecting(this));

        LocalBroadcastManager.getInstance(this).registerReceiver(networkServiceReceiver, new IntentFilter(NetworkService.ACTION_SITE_UPDATED));
        registerReceiver(networkServiceReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (networkServiceReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(networkServiceReceiver);
            unregisterReceiver(networkServiceReceiver);
        }
        siteSettingsManager.saveSiteSettings(context);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        if (BuildConfig.DEBUG) {
            MenuItem debugItem = menu.findItem(R.id.action_debug);
            debugItem.setVisible(true);
            MenuItem injectItem = menu.findItem(R.id.action_inject);
            injectItem.setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "global refresh requested");
            }
            startService(new Intent(context, NetworkService.class));
        }
        if (id == R.id.action_settings) {
            PrefSettingsActivity.start(context);
            return true;
        }
        if (id == R.id.action_about) {
            AboutActivity.start(context);
            return true;
        }
        if (id == R.id.action_inject) {
            DummySiteInjector.inject(this, taskFragment, siteSettingsManager);
            return true;
        }
        if (id == R.id.action_debug) {
            StringBuilder sb = new StringBuilder("you're on alpha version").append("\n");
            sb.append("Connectivity: ").append(ConnectivityUtil.isConnectedOrConnecting(context)).append("\n");

            int startupBootState = context.getPackageManager().getComponentEnabledSetting(new ComponentName(context, StartupBootReceiver.class));
            boolean startupBootEnable = (startupBootState == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT || startupBootState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
            sb.append("StartupBoot state: ").append(startupBootEnable).append("\n");
            sb.append("Alarm set: ").append(AlarmReceiver.hasAlarm()).append(" ").append(AlarmReceiver.getCurrentInterval()).append("\n");
            sb.append("Battery: ").append(BatteryLevelReceiver.getLastAction()).append("\n");
            Toast.makeText(this, sb.toString(), Toast.LENGTH_LONG).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPreExecute(NetworkTask task) {
    }

    @Override
    public void onProgressUpdate(NetworkTask task, Void... percent) {

    }

    @Override
    public void onPostExecute(NetworkTask task, SiteSettings siteSettings) {
        siteSettingsManager.refreshData();
    }

    @Override
    public void onCancelled(NetworkTask task) {

    }

    @Override
    public void onSiteUpdated(SiteSettings siteSettings) {
        siteSettingsManager.refreshData();
    }

    @Override
    public void onNetworkStateChanged(boolean hasConnectivity) {
        if (hasConnectivity && connectivityAlertView.getVisibility() != View.GONE) {
            connectivityAlertView.setVisibility(View.GONE);
        } else if (!hasConnectivity && connectivityAlertView.getVisibility() != View.VISIBLE) {
            connectivityAlertView.setVisibility(View.VISIBLE);
        }
    }
}
