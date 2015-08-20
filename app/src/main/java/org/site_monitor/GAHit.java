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

package org.site_monitor;

import android.content.Context;

import com.google.android.gms.analytics.HitBuilders;

/**
 * Created by Martin Norbert on 20/08/2015.
 */
public class GAHit {

    private static GAHit instance;
    private Context mContext;

    /** Don't instantiate directly - use {@link #builder()}  instead. */
    private GAHit(Context context) {
        mContext = context.getApplicationContext();
    }

    public static synchronized void initialize(Context context) {
        if (instance != null) {
            throw new IllegalStateException("Extra call to initialize analytics trackers");
        }
        instance = new GAHit(context);
    }

    public static synchronized GAHit builder() {
        if (instance == null) {
            throw new IllegalStateException("Call initialize() before builder()");
        }
        return instance;
    }

    public HitBuilders.EventBuilder event(int category, int action) {
        return new HitBuilders.EventBuilder(mContext.getString(category), mContext.getString(action));
    }

    public HitBuilders.EventBuilder event(int category, int action, int label) {
        return event(category, action).setLabel(mContext.getString(label));
    }

    public HitBuilders.EventBuilder event(int category, int action, long value) {
        return event(category, action).setValue(value);
    }

}
