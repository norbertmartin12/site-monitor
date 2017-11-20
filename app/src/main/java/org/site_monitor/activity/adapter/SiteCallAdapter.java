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

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.support.design.widget.Snackbar;
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

    private static final String MS = "ms";
    private static final String UNKNOWN = "?";
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
    private static final String HTTP = "HTTP-";
    private static final String SPACE = " ";
    private static final String EMPTY = "";
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

    private void updateView(final ViewHandler viewHandler) {
        final String date = simpleDateFormat.format(viewHandler.siteCall.getDate());
        viewHandler.mainTextView.setText(date);

        if (viewHandler.siteCall.getException() != null) {
            viewHandler.secondCodeTextView.setText(viewHandler.siteCall.getException());
        } else if (viewHandler.siteCall.getResult() == NetworkCallResult.NO_CONNECTIVITY) {
            viewHandler.secondCodeTextView.setText(R.string.no_connectivity_available);
        } else if (viewHandler.siteCall.getResponseCode() != null) {
            viewHandler.secondCodeTextView.setText(HTTP + viewHandler.siteCall.getResponseCode().toString());
        } else {
            viewHandler.secondCodeTextView.setText(UNKNOWN);
        }
        final NetworkCallResult result = viewHandler.siteCall.getResult();
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
            viewHandler.responseTimeTextView.setText(EMPTY);
        }

        viewHandler.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String prefixText = date + " - " ;
                if (result == NetworkCallResult.SUCCESS) {
                    prefixText += getContext().getText(R.string.tip_all_ok);
                    Snackbar.make(v, prefixText, Snackbar.LENGTH_SHORT).show();
                } else if (result == NetworkCallResult.FAIL) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    if (SiteSettingsBusiness.isCallCertError(viewHandler.siteCall)) {
                        builder.setMessage(R.string.tip_fail_cert_error);
                    } else if (SiteSettingsBusiness.isCallFailToConnectError(viewHandler.siteCall)) {
                        builder.setMessage(R.string.tip_fail_to_connect);
                    } else {
                        prefixText +=  getContext().getText(R.string.tip_unknown_error_good_luck);
                        Snackbar.make(v, prefixText, Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    builder.show();
                } else {
                    prefixText += getContext().getText(R.string.tip_unknown_state);
                    Snackbar.make(v, prefixText, Snackbar.LENGTH_SHORT).show();
                }
            }
        });
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
