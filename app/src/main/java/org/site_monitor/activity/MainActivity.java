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
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Parcelable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputType;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.GoogleAnalytics;

import org.site_monitor.BuildConfig;
import org.site_monitor.GA;
import org.site_monitor.GAHit;
import org.site_monitor.R;
import org.site_monitor.activity.adapter.SiteSettingsAdapter;
import org.site_monitor.activity.fragment.DummySiteInjector;
import org.site_monitor.activity.fragment.TaskFragment;
import org.site_monitor.model.adapter.SiteSettingsBusiness;
import org.site_monitor.model.adapter.SiteSettingsManager;
import org.site_monitor.model.bo.SiteCall;
import org.site_monitor.model.bo.SiteSettings;
import org.site_monitor.model.db.DBHelper;
import org.site_monitor.model.db.DBSiteSettings;
import org.site_monitor.receiver.BatteryLevelReceiver;
import org.site_monitor.receiver.StartupBootReceiver;
import org.site_monitor.receiver.internal.AlarmBroadcastReceiver;
import org.site_monitor.receiver.internal.NetworkBroadcastReceiver;
import org.site_monitor.service.NetworkService;
import org.site_monitor.task.NetworkTask;
import org.site_monitor.task.TaskCallback;
import org.site_monitor.util.AlarmUtil;
import org.site_monitor.util.ConnectivityUtil;
import org.site_monitor.util.TimeUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Allow user to add and monitor site settings.
 */
public class MainActivity extends FragmentActivity implements SiteSettingsAdapter.Handler, TaskCallback<NetworkTask, Void, Pair<SiteSettings, SiteCall>>, NetworkBroadcastReceiver.Listener, AlarmBroadcastReceiver.Listener {
    private static final String TAG_TASK_FRAGMENT = "main_activity_task_fragment";
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String PARCEL_SITE_LIST = "SITE_LIST";

    private MainActivity context = this;
    private ListView listView;
    private TextView connectivityAlertView;
    private TaskFragment taskFragment;
    private Chronometer chronometer;
    private CountDownTimer countDownTimer;
    private NetworkBroadcastReceiver networkBroadcastReceiver;
    private AlarmBroadcastReceiver alarmBroadcastReceiver;
    private View timerBannerView;
    private DBHelper dbHelper;
    private AlarmUtil alarmUtil = AlarmUtil.instance();
    private SiteSettingsAdapter siteSettingsAdapter;
    private List<SiteSettingsBusiness> siteSettingsList;
    private boolean loadDataFromDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SiteSettingsManager.migrateDataFromJsonToDatabase(this);
        dbHelper = DBHelper.getHelper(this);
        listView = (ListView) this.findViewById(R.id.listView);
        connectivityAlertView = (TextView) this.findViewById(R.id.connectivityAlert);
        chronometer = (Chronometer) this.findViewById(R.id.chronometer);
        timerBannerView = this.findViewById(R.id.timerBanner);

        FragmentManager fragmentManager = getSupportFragmentManager();
        taskFragment = (TaskFragment) fragmentManager.findFragmentByTag(TAG_TASK_FRAGMENT);
        if (taskFragment == null) {
            taskFragment = new TaskFragment();
            fragmentManager.beginTransaction().add(taskFragment, TAG_TASK_FRAGMENT).commit();
        }

        if (savedInstanceState == null || savedInstanceState.isEmpty()) {
            siteSettingsList = new ArrayList<SiteSettingsBusiness>();
            loadDataFromDb = true;
        } else {
            siteSettingsList = savedInstanceState.getParcelableArrayList(PARCEL_SITE_LIST);
        }
        siteSettingsAdapter = new SiteSettingsAdapter(context, this, siteSettingsList);
        listView.setAdapter(siteSettingsAdapter);
        if (networkBroadcastReceiver == null) {
            networkBroadcastReceiver = new NetworkBroadcastReceiver(this);
        }
        if (alarmBroadcastReceiver == null) {
            alarmBroadcastReceiver = new AlarmBroadcastReceiver(this);
        }
        // start alarm on 1st install or if not started on system boot
        alarmUtil.startAlarmIfNeeded(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.release();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (loadDataFromDb) {
            loadSiteSettingsBusinesses();
        } else {
            loadDataFromDb = false;
        }
        scheduleTimer();
        onNetworkStateChanged(ConnectivityUtil.isConnected(this));

        LocalBroadcastManager.getInstance(this).registerReceiver(alarmBroadcastReceiver, new IntentFilter(AlarmUtil.ACTION_NEXT_ALARM_SET));
        LocalBroadcastManager.getInstance(this).registerReceiver(networkBroadcastReceiver, new IntentFilter(NetworkService.ACTION_SITE_START_REFRESH));
        LocalBroadcastManager.getInstance(this).registerReceiver(networkBroadcastReceiver, new IntentFilter(NetworkService.ACTION_SITE_END_REFRESH));
        LocalBroadcastManager.getInstance(this).registerReceiver(networkBroadcastReceiver, new IntentFilter(NetworkService.ACTION_FAVICON_UPDATED));
        registerReceiver(networkBroadcastReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(PARCEL_SITE_LIST, new ArrayList<Parcelable>(siteSettingsList));
    }

    @Override
    protected void onStop() {
        super.onStop();
        loadDataFromDb = true;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(alarmBroadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(networkBroadcastReceiver);
        unregisterReceiver(networkBroadcastReceiver);
    }

    private void loadSiteSettingsBusinesses() {
        try {
            List<SiteSettings> list = dbHelper.getDBSiteSettings().queryForAll();
            siteSettingsList.clear();
            for (SiteSettings siteSettings : list) {
                siteSettingsList.add(new SiteSettingsBusiness(siteSettings));
            }
        } catch (SQLException e) {
            Log.e(TAG, "queryForAll", e);
        }

        Collections.sort(siteSettingsList, SiteSettingsBusiness.NAME_COMPARATOR);
        siteSettingsAdapter.notifyDataSetChanged();
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
            GA.tracker().send(GAHit.builder().event(R.string.c_refresh, R.string.a_global_refresh).build());
            startService(NetworkService.intentToCheckSites(this));
            return true;
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
            try {
                DummySiteInjector.inject(this.getCurrentFocus(), taskFragment, dbHelper.getDBSiteSettings());
                loadSiteSettingsBusinesses();
                alarmUtil.startAlarmIfNeeded(context);
            } catch (SQLException e) {
                Log.e(TAG, "dummyinject", e);
            }
            return true;
        }
        if (id == R.id.action_debug) {
            StringBuilder sb = new StringBuilder("you're on alpha version").append("\n");
            sb.append("Connectivity: ").append(ConnectivityUtil.isConnected(context)).append("\n");

            int startupBootState = context.getPackageManager().getComponentEnabledSetting(new ComponentName(context, StartupBootReceiver.class));
            boolean startupBootEnable = (startupBootState == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT || startupBootState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
            sb.append("StartupBoot state: ").append(startupBootEnable).append("\n");
            sb.append("Alarm set: ").append(alarmUtil.hasAlarm()).append(" ").append(alarmUtil.getCurrentInterval()).append("\n");
            sb.append("Battery: ").append(BatteryLevelReceiver.getLastAction()).append("\n");
            sb.append("Analytics: ").append(!GoogleAnalytics.getInstance(this).getAppOptOut()).append("\n");
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
    public void onPostExecute(NetworkTask task, Pair<SiteSettings, SiteCall> result) {
    }


    @Override
    public void onCancelled(NetworkTask task) {
    }

    @Override
    public void onSiteStartRefresh(SiteSettings siteSettings) {
        int position = siteSettingsAdapter.getPosition(new SiteSettingsBusiness(siteSettings));
        if (position != -1) {
            siteSettingsAdapter.getItem(position).setIsChecking(true);
        }
        siteSettingsAdapter.notifyDataSetChanged();
    }

    @Override
    public void onSiteEndRefresh(SiteSettings siteSettings, SiteCall siteCall) {
        int position = siteSettingsAdapter.getPosition(new SiteSettingsBusiness(siteSettings));
        if (position != -1) {
            SiteSettingsBusiness siteSettingsView = siteSettingsAdapter.getItem(position);
            siteSettingsView.setIsChecking(false);
            siteSettingsView.getCalls().add(siteCall);
            siteSettingsAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onFaviconUpdated(SiteSettings siteSettings, Bitmap favicon) {
        int position = siteSettingsAdapter.getPosition(new SiteSettingsBusiness(siteSettings));
        if (position != -1) {
            SiteSettingsBusiness siteSettingsView = siteSettingsAdapter.getItem(position);
            siteSettingsView.setFavicon(favicon);
            siteSettingsAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onNetworkStateChanged(boolean hasConnectivity) {
        if (hasConnectivity && connectivityAlertView.getVisibility() != View.GONE) {
            connectivityAlertView.setVisibility(View.GONE);
        } else if (!hasConnectivity && connectivityAlertView.getVisibility() != View.VISIBLE) {
            connectivityAlertView.setVisibility(View.VISIBLE);
        }
    }

    private void scheduleTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        final long nextAlarmTime = alarmUtil.getNextAlarmTime(this);
        final long nextAlarmInterval = alarmUtil.getCountUntilNextAlarmTime(this);
        if (nextAlarmInterval > 0) {
            timerBannerView.setVisibility(View.VISIBLE);
            countDownTimer = new CountDownTimer(nextAlarmInterval, TimeUtil._1_SEC * 5) {
                public void onTick(long millisUntilFinished) {
                    chronometer.setText(DateUtils.getRelativeTimeSpanString(nextAlarmTime));
                    chronometer.setText(DateUtils.getRelativeTimeSpanString(nextAlarmTime, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
                }

                public void onFinish() {
                    chronometer.setText(R.string.imminent);
                }
            }.start();
        } else {
            timerBannerView.setVisibility(View.GONE);
        }
    }

    public void floatingAddSite(final View v) {
        GA.tracker().send(GAHit.builder().event(R.string.c_monitor, R.string.a_add, R.string.l_touched).build());
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.add_monitor);
        final EditText input = new EditText(context);
        input.setHint(R.string.hint_site_url);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_URI);
        builder.setView(input);
        builder.setPositiveButton(R.string.action_add, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String host = input.getText().toString().trim();
                if (host.isEmpty()) {
                    return;
                }
                try {
                    DBSiteSettings dbSiteSettings = dbHelper.getDBSiteSettings();
                    if (dbSiteSettings.findForHost(host) != null) {
                        Snackbar.make(v, host + getString(R.string.already_exists), Snackbar.LENGTH_SHORT).show();
                        GA.tracker().send(GAHit.builder().event(R.string.c_monitor, R.string.a_add, R.string.l_already_exists).build());
                        return;
                    }
                    SiteSettings siteSettings = new SiteSettings(host);
                    dbSiteSettings.create(siteSettings);
                    GA.tracker().send(GAHit.builder().event(R.string.c_monitor, R.string.a_add).build());
                    alarmUtil.startAlarmIfNeeded(context);
                    new NetworkTask(context, taskFragment).execute(siteSettings);
                    SiteSettingsActivity.start(context, siteSettings.getHost());
                } catch (SQLException e) {
                    Log.e(TAG, "create", e);
                }
            }
        });
        builder.setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                GA.tracker().send(GAHit.builder().event(R.string.c_monitor, R.string.a_add, R.string.l_cancel).build());
            }
        });
        builder.show();
    }

    @Override
    public void touched(SiteSettingsBusiness siteSettings) {
        SiteSettingsActivity.start(context, siteSettings.getHost());
    }

    @Override
    public void onNextAlarmChange(long nextAlarm) {
        scheduleTimer();
    }
}
