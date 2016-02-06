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

package org.site_monitor.model.bo;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Martin Norbert on 11/07/2015.
 */
public class SiteSettings implements Comparable<SiteSettings>, Parcelable {

    public static final Creator<SiteSettings> CREATOR = new Creator<SiteSettings>() {
        @Override
        public SiteSettings createFromParcel(Parcel in) {
            return new SiteSettings(in);
        }

        @Override
        public SiteSettings[] newArray(int size) {
            return new SiteSettings[size];
        }
    };
    public static final String CERT_PATH_EXCEPTION = "java.security.cert.CertPathValidatorException";
    @Expose
    private String name;
    @Expose
    private String host;
    @Expose
    private boolean isNotificationEnabled = true;
    @Expose
    private boolean forcedCertificate = false;
    @Expose
    private List<SiteCall> calls = new ArrayList<SiteCall>();
    private Bitmap favicon;
    private boolean isChecking;

    public SiteSettings(String host, boolean isNotificationEnabled) {
        this.host = host;
        this.name = host;
        this.isNotificationEnabled = isNotificationEnabled;
    }

    public SiteSettings(Parcel in) {
        name = in.readString();
        host = in.readString();
        isNotificationEnabled = in.readInt() == 1 ? true : false;
        in.readList(calls, SiteCall.class.getClassLoader());
    }

    public String getHost() {
        return host;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isNotificationEnabled() {
        return isNotificationEnabled;
    }

    public void setNotificationEnabled(boolean notificationEnabled) {
        this.isNotificationEnabled = notificationEnabled;
    }

    public List<SiteCall> getUnmodifiableCalls() {
        return Collections.unmodifiableList(calls);
    }

    public void add(SiteCall siteCall) {
        calls.add(siteCall);
    }

    public boolean isChecking() {
        return isChecking;
    }

    public void setIsChecking(boolean isChecking) {
        this.isChecking = isChecking;
    }

    public Bitmap getFavicon() {
        return favicon;
    }

    public void setFavicon(Bitmap favicon) {
        this.favicon = favicon;
    }

    public boolean isForcedCertificate() {
        return forcedCertificate;
    }

    public void setForcedCertificate(boolean forcedCertificate) {
        this.forcedCertificate = forcedCertificate;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SiteSettings) {
            SiteSettings obj = (SiteSettings) o;
            return host.equals(obj.host);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return host.hashCode();
    }

    @Override
    public String toString() {
        return "SiteSettings{" +
                "name='" + name + '\'' +
                ", host='" + host + '\'' +
                ", isNotificationEnabled=" + isNotificationEnabled +
                ", calls=" + calls.size() +
                '}';
    }

    @Override
    public int compareTo(SiteSettings another) {
        if (calls.size() == 0 && another.calls.size() == 0) {
            return host.compareTo(another.host);
        }
        if (calls.size() == 0) {
            return -1;
        }
        if (another.calls.size() == 0) {
            return 1;
        }
        SiteCall siteCall = calls.get(calls.size() - 1);
        SiteCall anotherSiteCall = another.calls.get(another.calls.size() - 1);
        int callResultCompare = siteCall.getResult().compareTo(anotherSiteCall.getResult());
        if (callResultCompare == 0) {
            return name.compareToIgnoreCase(another.name);
        }
        return callResultCompare;
    }

    public boolean isInFail() {
        if (calls.size() == 0) {
            return false;
        }
        return calls.get(calls.size() - 1).getResult() == NetworkCallResult.FAIL;
    }

    /**
     * @return 2 site calls representing last fail period, null if none
     */
    public Pair<SiteCall, SiteCall> getLastFailPeriod() {
        SiteCall start = null;
        SiteCall last = null;
        // read last calls first
        for (int i = calls.size() - 1; i >= 0; i--) {
            SiteCall siteCall = calls.get(i);
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
                start = calls.get(i + 1);
                break;
            }
        }
        // no fail found
        if (last == null) {
            return null;
        }
        // first call is a fail
        if (start == null) {
            start = calls.get(0);
        }
        return new Pair<SiteCall, SiteCall>(start, last);
    }

    /**
     * @return null if empty or last call
     */
    public SiteCall getLastCall() {
        if (calls.isEmpty()) {
            return null;
        }
        return calls.get(calls.size() - 1);
    }

    public boolean isLastCallIsCertError() {
        SiteCall lastCall = getLastCall();
        if (lastCall == null || lastCall.getResult() != NetworkCallResult.FAIL || lastCall.getException() == null) {
            return false;
        }
        return lastCall.getException().startsWith(CERT_PATH_EXCEPTION);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(host);
        dest.writeInt(isNotificationEnabled ? 1 : 0);
        dest.writeList(calls);
    }
}
