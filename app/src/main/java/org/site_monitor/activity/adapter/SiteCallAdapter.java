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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.site_monitor.R;
import org.site_monitor.model.adapter.SiteSettingsBusiness;
import org.site_monitor.model.bo.NetworkCallResult;
import org.site_monitor.model.bo.SiteCall;

import java.text.SimpleDateFormat;

/**
 * Created by norbert on 26/07/2015.
 */
public class SiteCallAdapter extends ArrayAdapter<SiteCall> {

    public static final String MS = "ms";
    public static final String UNKNOWN = "?";
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private final LayoutInflater inflater;

    public SiteCallAdapter(Context context, SiteSettingsBusiness siteSettings) {
        super(context, R.layout.cell_site_call, R.id.mainTextView, siteSettings.getCalls());
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.cell_site_call, parent, false);
        }
        // order desc
        position = getCount() - position - 1;
        final ViewHandler viewHandler = new ViewHandler(convertView, position);
        updateView(viewHandler);
        return convertView;
    }

    private void updateView(ViewHandler viewHandler) {
        String date = simpleDateFormat.format(viewHandler.siteCall.getDate());
        viewHandler.mainTextView.setText(date);

        if (viewHandler.siteCall.getResponseCode() != null) {
            viewHandler.secondCodeTextView.setText(viewHandler.siteCall.getResponseCode().toString());
        } else if (viewHandler.siteCall.getException() != null) {
            viewHandler.secondCodeTextView.setText(viewHandler.siteCall.getException());
        } else if (viewHandler.siteCall.getResult() == NetworkCallResult.NO_CONNECTIVITY) {
            viewHandler.secondCodeTextView.setText(R.string.no_connectivity_available);
        } else {
            viewHandler.secondCodeTextView.setText(UNKNOWN);
        }
        NetworkCallResult result = viewHandler.siteCall.getResult();
        Resources resources = viewHandler.view.getResources();
        if (result == NetworkCallResult.SUCCESS) {
            viewHandler.view.setBackgroundColor(resources.getColor(R.color.state_success));
        } else if (result == NetworkCallResult.FAIL) {
            viewHandler.view.setBackgroundColor(resources.getColor(R.color.state_fail));
        } else {
            viewHandler.view.setBackgroundColor(resources.getColor(R.color.state_unknown));
        }

        if (viewHandler.siteCall.getResponseTime() != null) {
            viewHandler.responseTimeTextView.setText(viewHandler.siteCall.getResponseTime() + MS);
        } else {
            viewHandler.responseTimeTextView.setText("");
        }
    }

    private class ViewHandler {

        final TextView mainTextView;
        final TextView secondCodeTextView;
        final TextView responseTimeTextView;
        final SiteCall siteCall;
        final View view;

        ViewHandler(View view, int position) {
            this.view = view;
            this.siteCall = getItem(position);
            this.mainTextView = (TextView) view.findViewById(R.id.mainTextView);
            this.secondCodeTextView = (TextView) view.findViewById(R.id.secondTextView);
            this.responseTimeTextView = (TextView) view.findViewById(R.id.responseTimeTextView);
        }
    }
}
