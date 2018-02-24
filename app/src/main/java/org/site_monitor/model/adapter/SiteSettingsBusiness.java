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

package org.site_monitor.model.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;

import org.site_monitor.model.bo.NetworkCallResult;
import org.site_monitor.model.bo.SiteCall;
import org.site_monitor.model.bo.SiteSettings;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Martin Norbert on 06/02/2016.
 */
public class SiteSettingsBusiness implements Parcelable {

    public static final String CERT_PATH_EXCEPTION = "java.security.cert.CertPathValidatorException";

    public static final String FAIL_TO_CONNECT_TO = "failed to connect to";

    public static final Creator<SiteSettingsBusiness> CREATOR = new Creator<SiteSettingsBusiness>() {
        @Override
        public SiteSettingsBusiness createFromParcel(Parcel in) {
            return new SiteSettingsBusiness(in);
        }

        @Override
        public SiteSettingsBusiness[] newArray(int size) {
            return new SiteSettingsBusiness[size];
        }
    };
    public static final Comparator<SiteSettingsBusiness> NAME_COMPARATOR = new Comparator<SiteSettingsBusiness>() {
        @Override
        public int compare(SiteSettingsBusiness lhs, SiteSettingsBusiness rhs) {
            return lhs.getName().compareToIgnoreCase(rhs.getName());
        }
    };
    private final SiteSettings siteSettings;
    private boolean isChecking;
    private List<SiteCall> siteCalls;
    private Bitmap faviconCache;

    public SiteSettingsBusiness(SiteSettings siteSettings) {
        this.siteSettings = siteSettings;
        if (siteSettings.getSiteCalls() == null || siteSettings.getSiteCalls().isEmpty()) {
            this.siteCalls = new ArrayList<SiteCall>();
        } else {
            this.siteCalls = new ArrayList<SiteCall>(siteSettings.getSiteCalls());
            Collections.sort(this.siteCalls, SiteCall.ASC_DATE);

        }
    }

    public SiteSettingsBusiness(Parcel in) {
        siteSettings = in.readParcelable(SiteSettings.class.getClassLoader());
        isChecking = in.readByte() != 0;
        siteCalls = in.readArrayList(SiteCall.class.getClassLoader());
    }

    public static boolean isCallCertError(SiteCall siteCall) {
        if (siteCall == null || siteCall.getResult() != NetworkCallResult.FAIL || siteCall.getException() == null) {
            return false;
        }
        return siteCall.getException().startsWith(CERT_PATH_EXCEPTION);
    }

    public static boolean isCallFailToConnectError(SiteCall lastCall) {
        if (lastCall == null || lastCall.getResult() != NetworkCallResult.FAIL || lastCall.getException() == null) {
            return false;
        }
        return lastCall.getException().startsWith(FAIL_TO_CONNECT_TO);
    }

    /**
     * @return 2 site calls representing last fail period, null if none
     */
    public Pair<SiteCall, SiteCall> getLastFailPeriod() {
        SiteCall start = null;
        SiteCall last = null;
        // read last calls first
        for (int i = siteCalls.size() - 1; i >= 0; i--) {
            SiteCall siteCall = siteCalls.get(i);
            if (last == null && siteCall.getResult() == NetworkCallResult.FAIL) {
                last = siteCall;
                // last call in fail is first call
                if (i == 0) {
                    start = last;
                    break;
                }
                continue;
            }
            // when find non fail call get previous it's period start
            if (start == null && siteCall.getResult() != NetworkCallResult.FAIL) {
                start = siteCalls.get(i + 1);
                break;
            }
        }
        // no fail found
        if (last == null) {
            return null;
        }
        // first call is a fail
        if (start == null) {
            start = siteCalls.get(0);
        }
        return new Pair<SiteCall, SiteCall>(start, last);
    }

    public boolean isLastCallCertError() {
        return isCallCertError(getLastCall());
    }

    public boolean isLastCalFailToConnectSError() {
        return isCallFailToConnectError(getLastCall());
    }

    public List<SiteCall> getCalls() {
        return siteCalls;
    }

    public String getHost() {
        return siteSettings.getHost();
    }

    public String getInternalUrl() {
        return siteSettings.getInternalUrl();
    }

    public Long getId() {
        return siteSettings.getId();
    }

    public String getName() {
        return siteSettings.getName();
    }

    public boolean isChecking() {
        return isChecking;
    }

    public void setIsChecking(boolean isChecking) {
        this.isChecking = isChecking;
    }

    public boolean isForcedCertificate() {
        return siteSettings.isForcedCertificate();
    }

    public boolean isNotificationEnabled() {
        return siteSettings.isNotificationEnabled();
    }

    public Bitmap getFavicon() {
        if (faviconCache != null) {
            return faviconCache;
        }
        if (siteSettings.getFavicon() == null) {
            return null;
        }
        faviconCache = BitmapFactory.decodeByteArray(siteSettings.getFavicon(), 0, siteSettings.getFavicon().length);
        return faviconCache;
    }

    public void setFavicon(Bitmap favicon) {
        faviconCache = favicon;
        ByteBuffer buffer = ByteBuffer.allocate(favicon.getByteCount());
        favicon.copyPixelsToBuffer(buffer);
        siteSettings.setFavicon(buffer.array());
    }

    /**
     * @return null if empty or last call
     */
    public SiteCall getLastCall() {
        if (siteCalls.isEmpty()) {
            return null;
        }
        return siteCalls.get(siteCalls.size() - 1);
    }

    public boolean isInFail() {
        SiteCall lastCall = getLastCall();
        if (lastCall == null) {
            return false;
        }
        return lastCall.getResult() == NetworkCallResult.FAIL;
    }

    public SiteSettings getSiteSettings() {
        return siteSettings;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SiteSettingsBusiness) {
            return siteSettings.equals(((SiteSettingsBusiness) o).getSiteSettings());
        }
        return false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(siteSettings, flags);
        dest.writeByte(isChecking ? (byte) 1 : (byte) 0);
        dest.writeList(siteCalls);
    }
}
