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

package org.site_monitor.model.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import org.site_monitor.model.bo.SiteCall;
import org.site_monitor.model.bo.SiteSettings;

import java.sql.SQLException;

/**
 * Created by Martin Norbert on 30/01/2016.
 */
public class DBHelper extends OrmLiteSqliteOpenHelper {

    private static final String DATABASE_NAME = "sitemonitor.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TAG = DBHelper.class.getSimpleName();

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Returns DBHelper. Each time you call this method, you must call #release
     *
     * @param context
     * @return DBHelper
     * @see #release()
     */
    public static DBHelper getHelper(Context context) {
        return OpenHelperManager.getHelper(context, DBHelper.class);
    }

    /**
     * This is called when the database is first created. Usually you should call createTable statements here to create
     * the tables that will store your data.
     */
    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
        try {
            TableUtils.createTable(connectionSource, SiteSettings.class);
            TableUtils.createTable(connectionSource, SiteCall.class);
            Log.i(DBHelper.class.getName(), "database created");
        } catch (SQLException e) {
            Log.e(TAG, "Can't create database", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * This is called when your application is upgraded and it has a higher version number. This allows you to adjust
     * the various data to match the new version number.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
    }

    /**
     * Close the database connections and clear any cached DAOs.
     */
    @Override
    public void close() {
        super.close();
    }

    public DBSiteSettings getDBSiteSettings() throws SQLException {
        return new DBSiteSettings(this.<Dao<SiteSettings, Long>, SiteSettings>getDao(SiteSettings.class));
    }

    public DBSiteCall getDBSiteCall() throws SQLException {
        return new DBSiteCall(this.<Dao<SiteCall, Long>, SiteCall>getDao(SiteCall.class));
    }


    /**
     * Must be call by each entities that has call #getHelper before when no more using it.
     */
    public void release() {
        OpenHelperManager.releaseHelper();
    }
}
