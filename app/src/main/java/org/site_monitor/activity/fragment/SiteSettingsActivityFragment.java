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

package org.site_monitor.activity.fragment;

import android.app.Activity;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.site_monitor.GA;
import org.site_monitor.GAHit;
import org.site_monitor.R;
import org.site_monitor.model.adapter.SiteCallAdapter;
import org.site_monitor.model.bo.SiteSettings;
import org.site_monitor.receiver.NetworkServiceReceiver;
import org.site_monitor.service.NetworkService;

/**
 * A placeholder fragment containing a simple view.
 */
public class SiteSettingsActivityFragment extends TaskFragment implements NetworkServiceReceiver.Listener {

    private CheckBox notificationCheckbox;
    private TextView hostTextView;
    private ListView callListView;
    private ProgressBar progressBar;
    private ImageView faviconView;
    private View view;

    private Callback callback;
    private SiteSettings siteSettings;
    private SiteCallAdapter siteCallAdapter;

    private NetworkServiceReceiver networkServiceReceiver;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            this.callback = (Callback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("activity must implements SiteSettingsActivityFragment.Callback");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_site_settings, container, false);
        notificationCheckbox = (CheckBox) view.findViewById(R.id.notificationCheckbox);
        callListView = (ListView) view.findViewById(R.id.callListView);
        hostTextView = (TextView) view.findViewById(R.id.hostTextView);
        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        faviconView = (ImageView) view.findViewById(R.id.faviconView);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateView();
        notificationCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    GA.tracker().send(GAHit.builder().event(R.string.c_monitor, R.string.a_notification_changed, 1L).build());
                } else {
                    GA.tracker().send(GAHit.builder().event(R.string.c_monitor, R.string.a_notification_changed, 0L).build());
                }
                siteSettings.setNotificationEnabled(isChecked);
                callback.hasChanged(siteSettings);
            }
        });

        if (networkServiceReceiver == null && siteCallAdapter != null) {
            networkServiceReceiver = new NetworkServiceReceiver(this);
        }
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(networkServiceReceiver, new IntentFilter(NetworkService.ACTION_SITE_UPDATED));
    }

    @Override
    public void onPause() {
        super.onPause();
        notificationCheckbox.setOnCheckedChangeListener(null);
        if (networkServiceReceiver != null) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(networkServiceReceiver);
        }
    }

    private void updateView() {
        if (siteSettings != null) {
            if (siteCallAdapter == null) {
                siteCallAdapter = new SiteCallAdapter(getActivity(), siteSettings);
                callListView.setAdapter(siteCallAdapter);
            }
            hostTextView.setText(siteSettings.getHost());
            if (notificationCheckbox.isChecked() != siteSettings.isNotificationEnabled()) {
                notificationCheckbox.setChecked(siteSettings.isNotificationEnabled());
            }
            if (siteSettings.isChecking()) {
                progressBar.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.INVISIBLE);
            }
            siteCallAdapter.notifyDataSetChanged();
            faviconView.setImageBitmap(siteSettings.getFavicon());
        }
    }

    public void setSiteSettings(SiteSettings siteSettings) {
        this.siteSettings = siteSettings;
        updateView();
    }

    @Override
    public void onSiteUpdated(SiteSettings siteSettings) {
        if (this.siteSettings != null && siteSettings.equals(this.siteSettings)) {
            updateView();
        }
    }

    @Override
    public void onNetworkStateChanged(boolean hasConnectivity) {
    }

    public interface Callback {
        void hasChanged(SiteSettings siteSettings);
    }

}
