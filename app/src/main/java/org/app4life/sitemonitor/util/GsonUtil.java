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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Created by Martin Norbert on 13/07/2015.
 */
public class GsonUtil {

    public static String toJson(Object obj) {
        return getGson().toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> objClass) {
        return objClass.cast(getGson().fromJson(json, objClass));
    }

    public static <T> T fromJson(String json, TypeToken<T> typeToken) {
        return getGson().fromJson(json, typeToken.getType());
    }

    private static Gson getGson() {
        return new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    }
}
