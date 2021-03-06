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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import org.site_monitor.R;

import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {

    private static final String MARKET_SITE_MONITOR = "market://details?id=org.site_monitor";
    private static final String TRELLO_SITE_MONITOR = "https://trello.com/b/G0rQVzo8/site-monitor";

    public static void start(Context context) {
        Intent intent = new Intent(context, AboutActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        TextView versionTextView = findViewById(R.id.version);

        try {
            versionTextView.setText(getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            versionTextView.setText("?");
        }
    }

    public void goTo(View v) {

        if (v.getId() == R.id.rateAppButton) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(MARKET_SITE_MONITOR));
            startActivity(intent);
        } else if (v.getId() == R.id.trelloButton) {

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(TRELLO_SITE_MONITOR));
            startActivity(intent);
        }
    }

}
