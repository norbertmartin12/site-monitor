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

import android.app.Activity;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import org.site_monitor.R;
import org.site_monitor.activity.adapter.SiteCallAdapter;
import org.site_monitor.model.adapter.SiteSettingsBusiness;
import org.site_monitor.model.bo.SiteCall;
import org.site_monitor.model.bo.SiteSettings;
import org.site_monitor.receiver.internal.NetworkBroadcastReceiver;
import org.site_monitor.service.NetworkService;

/**
 * A placeholder fragment containing a simple view.
 */
public class SiteSettingsActivityFragment extends TaskFragment implements NetworkBroadcastReceiver.Listener {

    private CompoundButton trustCertificateCheckable;
    private View trustCertificateView;
    private CompoundButton notificationCheckable;
    private TextView hostTextView;
    private TextView internalUrlTextView;
    private ViewGroup internalUrlViewGroup;
    private ListView callListView;
    private ProgressBar progressBar;
    private ImageView faviconView;
    private View view;

    private Callback callback;
    private SiteSettingsBusiness siteSettings;
    private SiteCallAdapter siteCallAdapter;

    private NetworkBroadcastReceiver networkBroadcastReceiver;

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
        notificationCheckable = (Switch) view.findViewById(R.id.notificationSwitch);
        trustCertificateCheckable = (Switch) view.findViewById(R.id.trustCertificateSwitch);
        trustCertificateView = view.findViewById(R.id.trustCertificateView);
        callListView = view.findViewById(R.id.callListView);
        hostTextView = view.findViewById(R.id.hostTextView);
        internalUrlViewGroup = view.findViewById(R.id.internalIpLayout);
        internalUrlTextView = view.findViewById(R.id.internalIpTextView);
        progressBar = view.findViewById(R.id.progressBar);
        faviconView = view.findViewById(R.id.faviconImage);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateView();
        notificationCheckable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                siteSettings.getSiteSettings().setNotificationEnabled(isChecked);
                callback.hasChanged(siteSettings.getSiteSettings());
            }
        });
        trustCertificateCheckable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                siteSettings.getSiteSettings().setForcedCertificate(isChecked);
                callback.hasChanged(siteSettings.getSiteSettings());
            }
        });

        if (networkBroadcastReceiver == null && siteCallAdapter != null) {
            networkBroadcastReceiver = new NetworkBroadcastReceiver(this);
        }
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(networkBroadcastReceiver, new IntentFilter(NetworkService.ACTION_SITE_START_REFRESH));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(networkBroadcastReceiver, new IntentFilter(NetworkService.ACTION_SITE_END_REFRESH));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(networkBroadcastReceiver, new IntentFilter(NetworkService.ACTION_FAVICON_UPDATED));
    }

    @Override
    public void onPause() {
        super.onPause();
        notificationCheckable.setOnCheckedChangeListener(null);
        trustCertificateCheckable.setOnCheckedChangeListener(null);
        if (networkBroadcastReceiver != null) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(networkBroadcastReceiver);
        }
    }

    void updateView() {
        if (siteSettings != null) {
            if (siteCallAdapter == null) {
                siteCallAdapter = new SiteCallAdapter(getActivity(), siteSettings);
            }
            callListView.setAdapter(siteCallAdapter);
            hostTextView.setText(siteSettings.getHost());
            if (siteSettings.getInternalUrl() != null) {
                internalUrlViewGroup.setVisibility(View.VISIBLE);
                internalUrlTextView.setText(siteSettings.getInternalUrl());
            } else {
                internalUrlViewGroup.setVisibility(View.GONE);
            }
            if (notificationCheckable.isChecked() != siteSettings.isNotificationEnabled()) {
                notificationCheckable.setChecked(siteSettings.isNotificationEnabled());
            }
            if (siteSettings.isForcedCertificate() || siteSettings.isLastCallCertError()) {
                trustCertificateView.setVisibility(View.VISIBLE);
                if (trustCertificateCheckable.isChecked() != siteSettings.isForcedCertificate()) {
                    trustCertificateCheckable.setChecked(siteSettings.isForcedCertificate());
                }
            } else {
                trustCertificateView.setVisibility(View.GONE);
            }
            // HACK: is empty is a hack to show progress on create action cause listener is register to late to catch start refresh
            if (siteSettings.isChecking() || siteSettings.getCalls().isEmpty()) {
                progressBar.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.INVISIBLE);
            }
            siteCallAdapter.notifyDataSetChanged();
            faviconView.setImageBitmap(siteSettings.getFavicon());
        }
    }

    public void setSiteSettings(SiteSettingsBusiness siteSettings) {
        this.siteSettings = siteSettings;
        updateView();
    }

    @Override
    public void onSiteStartRefresh(SiteSettings siteSettings) {
        if (this.siteSettings != null && siteSettings.equals(this.siteSettings.getSiteSettings())) {
            this.siteSettings.setIsChecking(true);
            updateView();
        }
    }

    @Override
    public void onSiteEndRefresh(SiteSettings siteSettings, SiteCall siteCall) {
        if (this.siteSettings != null && siteSettings.equals(this.siteSettings.getSiteSettings())) {
            this.siteSettings.setIsChecking(false);
            this.siteSettings.getCalls().add(siteCall);
            updateView();
        }
    }

    @Override
    public void onFaviconUpdated(SiteSettings siteSettings, Bitmap favicon) {
        if (this.siteSettings != null && siteSettings.equals(this.siteSettings.getSiteSettings())) {
            this.siteSettings.setFavicon(favicon);
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
