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

package org.site_monitor.util;

import java.util.Date;

/**
 * Created by Martin Norbert on 10/09/2015.
 */
public class Timer {

    private final Date referenceDate;

    public Timer() {
        this(new Date());
    }

    public Timer(Date referenceDate) {
        this.referenceDate = referenceDate;
    }

    public long getElapsedTime() {
        return new Date().getTime() - referenceDate.getTime();
    }

    public Date getReferenceDate() {
        return new Date(referenceDate.getTime());
    }
}
