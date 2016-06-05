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

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Martin Norbert on 11/07/2015.
 */

@DatabaseTable
public class SiteSettings implements Parcelable {

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

    @DatabaseField(generatedId = true)
    private Long id;
    @Expose
    @DatabaseField
    private String name;
    @Expose
    @DatabaseField(canBeNull = false, uniqueIndex = true)
    private String host;
    @Expose
    @DatabaseField(canBeNull = true, uniqueIndex = true)
    private String internalUrl;
    @Expose
    @DatabaseField
    private boolean isNotificationEnabled = true;
    @Expose
    @DatabaseField
    private boolean forcedCertificate = false;
    @DatabaseField(dataType = DataType.BYTE_ARRAY)
    private byte[] favicon;
    @Expose
    private List<SiteCall> calls = new ArrayList<SiteCall>();
    @ForeignCollectionField(eager = true)
    private ForeignCollection<SiteCall> siteCalls;


    public SiteSettings() {
    }

    public SiteSettings(String host) {
        this.host = host;
        this.name = host;
    }

    public SiteSettings(Parcel in) {
        name = in.readString();
        host = in.readString();
        internalUrl = in.readString();
        isNotificationEnabled = in.readInt() == 1 ? true : false;
        in.readList(calls, SiteCall.class.getClassLoader());
        in.readByteArray(favicon);
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

    /**
     * @return
     * @deprecated only for json retreiving
     */
    public List<SiteCall> getCalls() {
        return calls;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ForeignCollection<SiteCall> getSiteCalls() {
        return siteCalls;
    }

    public void setSiteCalls(ForeignCollection<SiteCall> siteCalls) {
        this.siteCalls = siteCalls;
    }

    public boolean isForcedCertificate() {
        return forcedCertificate;
    }

    public void setForcedCertificate(boolean forcedCertificate) {
        this.forcedCertificate = forcedCertificate;
    }

    public byte[] getFavicon() {
        return favicon;
    }

    public void setFavicon(byte[] favicon) {
        this.favicon = favicon;
    }

    public String getInternalUrl() {
        return internalUrl;
    }

    public void setInternalUrl(String internalUrl) {
        this.internalUrl = internalUrl;
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
                ", internalUrl='" + internalUrl + '\'' +
                ", isNotificationEnabled=" + isNotificationEnabled +
                ", calls=" + calls.size() +
                '}';
    }

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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(host);
        dest.writeString(internalUrl);
        dest.writeInt(isNotificationEnabled ? 1 : 0);
        dest.writeList(calls);
        if (favicon != null) {
            dest.writeByteArray(favicon);
        }
    }

}
