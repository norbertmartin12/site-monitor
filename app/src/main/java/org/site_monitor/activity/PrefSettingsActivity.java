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

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.support.v4.app.NavUtils;
import android.text.TextUtils;
import android.view.MenuItem;

import org.site_monitor.GA;
import org.site_monitor.GAHit;
import org.site_monitor.R;
import org.site_monitor.receiver.StartupBootReceiver;
import org.site_monitor.util.AlarmUtil;
import org.site_monitor.util.TimeUtil;

import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class PrefSettingsActivity extends AppCompatPreferenceActivity {

    public static final String NOTIFICATIONS_VIBRATE = "notifications_vibrate";
    public static final String NOTIFICATIONS_RINGTONE = "notifications_ringtone";
    public static final String NOTIFICATION_ENABLE = "notifications_enable";
    public static final String NOTIFICATION_LIMIT_TO_NEW_FAIL = "notifications_limit_to_new_fail";

    public static final String NOTIFICATION_LIGHT_COLOR = "notification_light_color";
    public static final String BOOT_START = "boot_start";
    public static final String FREQUENCY = "frequency";
    public static final String ANALYTICS = "allow_analytics";
    /**
     * Determines whether to always show the simplified settings UI, where
     * settings are presented in a single list. When false, settings are shown
     * as a master/detail two-pane view on tablets. When true, a single pane is
     * shown on tablets.
     */
    private static final boolean ALWAYS_SIMPLE_PREFS = false;
    private static AlarmUtil alarmUtil = AlarmUtil.instance();
    /**
     * A preference value change listener that updates the preference's summary to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);
                preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);

                if (preference.getKey().equals(FREQUENCY)) {
                    String currentValue = preference.getSharedPreferences().getString(FREQUENCY, "");
                    if (!currentValue.equals(stringValue)) {
                        GA.tracker().send(GAHit.builder().event(R.string.c_settings, R.string.a_frequency_changed, Long.parseLong(stringValue)).build());
                        alarmUtil.rescheduleAlarm(preference.getContext(), Long.parseLong(stringValue) * TimeUtil.MINUTE_2_MILLISEC);
                    }
                }

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    preference.setSummary(R.string.pref_ringtone_silent);
                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(preference.getContext(), Uri.parse(stringValue));
                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            } else {
                // For all other preferences, set the summary to the value's simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    private static Preference.OnPreferenceChangeListener sPreferenceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            boolean state = (Boolean) value;
            if (preference.getKey().equals(BOOT_START)) {
                if (state) {
                    GA.tracker().send(GAHit.builder().event(R.string.c_settings, R.string.a_boot_start_changed, 1L).build());
                } else {
                    GA.tracker().send(GAHit.builder().event(R.string.c_settings, R.string.a_boot_start_changed, 0L).build());
                }
                StartupBootReceiver.setCanBeInitiatedBySystem(preference.getContext(), state);
                preference.setSummary("");
            } else if (preference.getKey().equals(NOTIFICATION_ENABLE)) {
                if (state) {
                    GA.tracker().send(GAHit.builder().event(R.string.c_settings, R.string.a_notification_changed, 1L).build());
                } else {
                    GA.tracker().send(GAHit.builder().event(R.string.c_settings, R.string.a_notification_changed, 0L).build());
                }
            } else if (preference.getKey().equals(NOTIFICATION_LIMIT_TO_NEW_FAIL)) {
                if (state) {
                    GA.tracker().send(GAHit.builder().event(R.string.c_settings, R.string.a_notification_limit_changed, 1L).build());
                } else {
                    GA.tracker().send(GAHit.builder().event(R.string.c_settings, R.string.a_notification_limit_changed, 0L).build());
                }
            } else if (preference.getKey().equals(ANALYTICS)) {
                if (state) {
                    GA.getInstance().startTracking();
                } else {
                    GA.getInstance().stopTracking();
                }
            }
            return true;
        }
    };


    /**
     * Helper method to determine if the device has an extra-large screen. For example, 10" tablets are extra-large.
     */

    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Determines whether the simplified settings UI should be shown. This is  true if this is forced via {@link #ALWAYS_SIMPLE_PREFS}, or the device
     * doesn't have newer APIs like {@link PreferenceFragment}, or the device doesn't have an extra-large screen. In these cases, a single-pane
     * "simplified" settings UI should be shown.
     */
    private static boolean isSimplePreferences(Context context) {
        return ALWAYS_SIMPLE_PREFS || Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB || !isXLargeTablet(context);
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the preference's value is changed, its summary (line of text below the
     * preference title) is dataChanged to reflect the value. The summary is also immediately dataChanged upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
    }

    public static void start(Context context) {
        context.startActivity(new Intent(context, PrefSettingsActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setupSimplePreferencesScreen();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this) && !isSimplePreferences(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        if (!isSimplePreferences(this)) {
            loadHeadersFromResource(R.xml.pref_headers, target);
        }
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar.
            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Shows the simplified settings UI if the device configuration if the
     * device configuration dictates that a simplified, single-pane UI should be shown.
     */
    private void setupSimplePreferencesScreen() {
        if (!isSimplePreferences(this)) {
            return;
        }

        // In the simplified UI, fragments are not used at all and we instead use the older PreferenceActivity APIs.
        addPreferencesFromResource(R.xml.pref_monitoring);
        addPreferencesFromResource(R.xml.pref_notification);
        addPreferencesFromResource(R.xml.pref_history);
        addPreferencesFromResource(R.xml.pref_analytics);

        // Bind the summaries of EditText/List/Dialog/Ringtone preferences to their values. When their values change,
        // their summaries are dataChanged to reflect the new value, per the Android Design guidelines.
        bindPreferenceSummaryToValue(findPreference(NOTIFICATIONS_RINGTONE));
        bindPreferenceSummaryToValue(findPreference(FREQUENCY));
        bindPreferenceSummaryToValue(findPreference(NOTIFICATION_LIGHT_COLOR));

        findPreference(BOOT_START).setOnPreferenceChangeListener(sPreferenceListener);
        findPreference(NOTIFICATION_LIMIT_TO_NEW_FAIL).setOnPreferenceChangeListener(sPreferenceListener);
        findPreference(NOTIFICATION_ENABLE).setOnPreferenceChangeListener(sPreferenceListener);
        findPreference(ANALYTICS).setOnPreferenceChangeListener(sPreferenceListener);
    }

    /**
     * This fragment shows notification preferences only. It is used when the activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class NotificationPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_notification);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences to their values. When their values change, their summaries are
            // dataChanged to reflect the new value, per the Android Design guidelines.
            bindPreferenceSummaryToValue(findPreference(NOTIFICATIONS_RINGTONE));
            bindPreferenceSummaryToValue(findPreference(NOTIFICATION_LIGHT_COLOR));

            findPreference(NOTIFICATION_ENABLE).setOnPreferenceChangeListener(sPreferenceListener);
            findPreference(NOTIFICATION_LIMIT_TO_NEW_FAIL).setOnPreferenceChangeListener(sPreferenceListener);
        }
    }

    /**
     * This fragment shows monitoring preferences only.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class MonitoringPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_monitoring);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences to their values. When their values change, their summaries are
            // dataChanged to reflect the new value, per the Android Design guidelines.
            bindPreferenceSummaryToValue(findPreference(FREQUENCY));

            findPreference(BOOT_START).setOnPreferenceChangeListener(sPreferenceListener);
        }
    }


    /**
     * This fragment shows history preferences only.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class HistoryPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_history);
        }
    }

    /**
     * This fragment shows analytics preferences only.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class AnalyticsPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_analytics);

            findPreference(ANALYTICS).setOnPreferenceChangeListener(sPreferenceListener);
        }
    }
}
