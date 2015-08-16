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
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.site_monitor.R;
import org.site_monitor.activity.SiteSettingsActivity;
import org.site_monitor.model.bo.NetworkCallResult;
import org.site_monitor.model.bo.SiteCall;
import org.site_monitor.model.bo.SiteSettings;

import java.util.List;

/**
 * Created by norbert on 17/07/2015.
 */
public class SiteSettingsAdapter extends ArrayAdapter<SiteSettings> {

    private final LayoutInflater inflater;
    private final SiteSettingsManager siteSettingsManager;

    SiteSettingsAdapter(Context context, SiteSettingsManager siteSettingsManager) {
        super(context, R.layout.cell_site_settings, R.id.nameText, siteSettingsManager.getSiteSettingsSortedList());
        this.siteSettingsManager = siteSettingsManager;
        this.inflater = LayoutInflater.from(context);
    }

    private static void updateView(ViewHandler viewHandler) {
        viewHandler.nameTextView.setText(viewHandler.siteSettings.getName());
        List<SiteCall> unmodifiableCalls = viewHandler.siteSettings.getUnmodifiableCalls();
        int lastCall = unmodifiableCalls.size() - 1;
        if (lastCall >= 0) {
            viewHandler.progressBar.setVisibility(View.INVISIBLE);
            Resources resources = viewHandler.view.getResources();
            SiteCall siteCall = unmodifiableCalls.get(lastCall);
            if (siteCall.getResult() == NetworkCallResult.SUCCESS) {
                viewHandler.stateText.setText(R.string.state_success);
                viewHandler.stateText.setTextColor(resources.getColor(R.color.state_success));
                viewHandler.stateText.setVisibility(View.VISIBLE);
            } else if (siteCall.getResult() == NetworkCallResult.FAIL) {
                viewHandler.stateText.setText(R.string.state_unreachable);
                viewHandler.stateText.setTextColor(resources.getColor(R.color.state_fail));
                viewHandler.stateText.setVisibility(View.VISIBLE);
            } else {
                viewHandler.stateText.setText(R.string.state_unknown);
                viewHandler.stateText.setTextColor(resources.getColor(R.color.state_unknown));
                viewHandler.stateText.setVisibility(View.VISIBLE);
            }
        } else {
            viewHandler.stateText.setVisibility(View.INVISIBLE);
            viewHandler.progressBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.cell_site_settings, parent, false);
        }
        final ViewHandler viewHandler = new ViewHandler(position, convertView);
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (viewHandler.siteSettings.getUnmodifiableCalls().size() == 0) {
                    Toast.makeText(getContext(), R.string.connecting_site_1st_time, Toast.LENGTH_SHORT).show();
                    return;
                }
                SiteSettingsActivity.start(getContext(), position);
            }
        });
        updateView(viewHandler);
        return convertView;
    }

    private class ViewHandler {
        final View view;
        final SiteSettings siteSettings;
        final TextView nameTextView;
        final ProgressBar progressBar;
        final TextView stateText;

        ViewHandler(int position, View view) {
            this.siteSettings = getItem(position);
            this.view = view;
            this.nameTextView = (TextView) view.findViewById(R.id.nameText);
            this.progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
            this.stateText = (TextView) view.findViewById(R.id.stateText);
        }
    }
}
