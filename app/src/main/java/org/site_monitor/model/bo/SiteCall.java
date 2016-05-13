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
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.Date;

/**
 * Created by Martin Norbert on 13/07/2015.
 */

@DatabaseTable
public class SiteCall implements Parcelable {
    public static final Creator<SiteCall> CREATOR = new Creator<SiteCall>() {
        @Override
        public SiteCall createFromParcel(Parcel in) {
            return new SiteCall(in);
        }

        @Override
        public SiteCall[] newArray(int size) {
            return new SiteCall[size];
        }
    };
    @DatabaseField(generatedId = true)
    private Long id;
    @Expose
    @DatabaseField(canBeNull = false)
    private Date date;
    @Expose
    @DatabaseField(canBeNull = false)
    private NetworkCallResult result;
    @Expose
    @DatabaseField
    private Integer responseCode;
    @Expose
    @DatabaseField(canBeNull = false)
    private Long responseTime;
    @Expose
    @DatabaseField
    private String exception;
    @DatabaseField(foreign = true, canBeNull = false)
    private SiteSettings siteSettings;

    public SiteCall() {
    }

    public SiteCall(Date date, NetworkCallResult callResult) {
        this(date, callResult, 0L, null);
    }

    public SiteCall(Date date, NetworkCallResult callResult, Long responseTime, int responseCode) {
        this(date, callResult, responseCode, responseTime, null);
    }

    public SiteCall(Date date, NetworkCallResult callResult, Long responseTime, Exception e) {
        this(date, callResult, HttpURLConnection.HTTP_NOT_FOUND, responseTime, e);
    }

    public SiteCall(Date date, NetworkCallResult callResult, Integer responseCode, Long responseTime, Exception e) {
        this.date = date;
        this.result = callResult;
        this.responseCode = responseCode;
        if (e != null) {
            this.exception = e.getLocalizedMessage();
            if (exception == null && e instanceof SocketTimeoutException) {
                this.exception = "timeout";
            }
        }
        this.responseTime = responseTime;
    }

    public SiteCall(Parcel in) {
        id = in.readLong();
        date = new Date(in.readLong());
        result = NetworkCallResult.values()[in.readInt()];
        responseCode = (Integer) in.readValue(Integer.class.getClassLoader());
        exception = in.readString();
    }

    public Integer getResponseCode() {
        return responseCode;
    }

    public String getException() {
        return exception;
    }

    public Date getDate() {
        return date;
    }

    public NetworkCallResult getResult() {
        return result;
    }

    public Long getResponseTime() {
        return responseTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SiteSettings getSiteSettings() {
        return siteSettings;
    }

    public void setSiteSettings(SiteSettings siteSettings) {
        this.siteSettings = siteSettings;
    }

    @Override
    public String toString() {
        return "SiteCall{" +
                "date=" + date +
                ", result=" + result +
                ", responseCode=" + responseCode +
                ", responseTime=" + responseTime +
                ", exception='" + exception + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeLong(date.getTime());
        dest.writeInt(result.ordinal());
        // write int doesn't accept null
        dest.writeValue(responseCode);
        dest.writeString(exception);
    }
}
