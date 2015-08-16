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

package org.app4life.sitemonitor.util;

public class TimeUtil {

    public static final long SEC_2_MILLISEC = 1000;

    public static final long MINUTE_2_MILLISEC = 60 * SEC_2_MILLISEC;

    public static final long MINUTE_2_SEC = 60;

    public static final long HOUR_2_MILLISEC = 60 * MINUTE_2_MILLISEC;

    public static final long DAY_2_MILLISEC = 24 * HOUR_2_MILLISEC;

    public static final long _1_SEC = SEC_2_MILLISEC;

    public static final int _1_SEC_INT = (int) _1_SEC;
}
