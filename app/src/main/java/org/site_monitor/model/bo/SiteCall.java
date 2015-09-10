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
import java.util.Date;

/**
 * Created by Martin Norbert on 13/07/2015.
 */
public class SiteCall implements Serializable {
    @Expose
    private Date date;
    @Expose
    private NetworkCallResult result;
    @Expose
    private Integer responseCode;
    @Expose
    private Long responseTime;
    @Expose
    private String exception;


    public SiteCall(Date date, NetworkCallResult callResult) {
        this(date, callResult, null, null);
    }

    public SiteCall(Date date, NetworkCallResult callResult, Long responseTime, int responseCode) {
        this(date, callResult, responseCode, responseTime, null);
    }

    public SiteCall(Date date, NetworkCallResult callResult, Long responseTime, Exception e) {
        this(date, callResult, null, responseTime, e);
    }

    public SiteCall(Date date, NetworkCallResult callResult, Integer responseCode, Long responseTime, Exception e) {
        this.date = date;
        this.result = callResult;
        this.responseCode = responseCode;
        if (e != null) {
            this.exception = e.getLocalizedMessage();
        }
        this.responseTime = responseTime;
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

}
