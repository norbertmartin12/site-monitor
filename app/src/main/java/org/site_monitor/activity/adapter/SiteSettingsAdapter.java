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

package org.site_monitor.activity.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.text.format.DateUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.site_monitor.R;
import org.site_monitor.model.adapter.SiteSettingsBusiness;
import org.site_monitor.model.bo.NetworkCallResult;
import org.site_monitor.model.bo.SiteCall;

import java.util.List;

/**
 * Created by norbert on 17/07/2015.
 */
public class SiteSettingsAdapter extends ArrayAdapter<SiteSettingsBusiness> {

    public static final String EMPTY = "";
    private final LayoutInflater inflater;
    private final Handler handler;

    public SiteSettingsAdapter(Context context, Handler handler, List<SiteSettingsBusiness> siteSettingsList) {
        super(context, R.layout.cell_site_settings, R.id.nameText, siteSettingsList);
        this.inflater = LayoutInflater.from(context);
        this.handler = handler;
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
                handler.touched(viewHandler.siteSettings);
            }
        });
        updateView(viewHandler);
        return convertView;
    }

    private void updateView(ViewHandler viewHandler) {
        viewHandler.nameTextView.setText(viewHandler.siteSettings.getName());
        List<SiteCall> calls = viewHandler.siteSettings.getCalls();
        Resources resources = viewHandler.view.getResources();
        if (!calls.isEmpty()) {
            int lastCall = calls.size() - 1;
            SiteCall siteCall = calls.get(lastCall);
            if (siteCall.getResult() == NetworkCallResult.SUCCESS) {
                viewHandler.stateImage.getBackground().setColorFilter(resources.getColor(R.color.state_success), PorterDuff.Mode.SRC);
            } else if (siteCall.getResult() == NetworkCallResult.FAIL) {
                viewHandler.stateImage.getBackground().setColorFilter(resources.getColor(R.color.state_fail), PorterDuff.Mode.SRC);
            } else {
                viewHandler.stateImage.getBackground().setColorFilter(resources.getColor(R.color.state_unknown), PorterDuff.Mode.SRC);
            }
        } else {
            viewHandler.stateImage.getBackground().setColorFilter(resources.getColor(R.color.state_unknown), PorterDuff.Mode.SRC);
        }
        // HACK: is empty is a hack to show progress on create action cause listener is register to late to catch start refresh
        if (viewHandler.siteSettings.isChecking() || viewHandler.siteSettings.getCalls().isEmpty()) {
            viewHandler.progressBar.setVisibility(View.VISIBLE);
        } else {
            viewHandler.progressBar.setVisibility(View.INVISIBLE);
        }

        if (viewHandler.siteSettings.isNotificationEnabled()) {
            viewHandler.notificationImage.setVisibility(View.INVISIBLE);
        } else {
            viewHandler.notificationImage.setVisibility(View.VISIBLE);
        }
        viewHandler.faviconImage.setImageBitmap(viewHandler.siteSettings.getFavicon());

        if (viewHandler.siteSettings.isInFail()) {
            Pair<SiteCall, SiteCall> lastFailPeriod = viewHandler.siteSettings.getLastFailPeriod();
            CharSequence lastFailText = DateUtils.getRelativeTimeSpanString(lastFailPeriod.first.getDate().getTime());
            viewHandler.lastFailText.setText(lastFailText);
            viewHandler.lastFailText.setVisibility(View.VISIBLE);
        } else {
            viewHandler.lastFailText.setVisibility(View.GONE);
            viewHandler.lastFailText.setText(EMPTY);
        }
    }

    public interface Handler {
        void touched(SiteSettingsBusiness siteSettings);
    }

    private class ViewHandler {
        final View view;
        final SiteSettingsBusiness siteSettings;
        final TextView nameTextView;
        final ProgressBar progressBar;
        final TextView lastFailText;
        final ImageView faviconImage;
        final ImageView notificationImage;
        final ImageView stateImage;

        ViewHandler(int position, View view) {
            this.siteSettings = getItem(position);
            this.view = view;
            this.nameTextView = view.findViewById(R.id.nameText);
            this.progressBar = view.findViewById(R.id.progressBar);
            this.faviconImage = view.findViewById(R.id.faviconImage);
            this.notificationImage = view.findViewById(R.id.notificationImage);
            this.lastFailText = view.findViewById(R.id.lastFailText);
            this.stateImage = view.findViewById(R.id.stateImage);
        }
    }
}
