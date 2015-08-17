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

package org.site_monitor.model.bo;

import com.google.gson.annotations.Expose;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Martin Norbert on 11/07/2015.
 */
public class SiteSettings implements Serializable, Comparable<SiteSettings> {

    @Expose
    private String name;
    @Expose
    private String host;
    @Expose
    private boolean isNotificationEnabled = true;
    @Expose
    private List<SiteCall> calls = new ArrayList<>();

    private boolean isChecking;

    public SiteSettings(String host, boolean isNotificationEnabled) {
        this.host = host;
        this.name = host;
        this.isNotificationEnabled = isNotificationEnabled;
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
}
