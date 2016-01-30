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

import com.j256.ormlite.dao.Dao;

import org.site_monitor.model.bo.SiteSettings;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by Martin Norbert on 31/01/2016.
 */
public class DBSiteSettings {

    private static final String F_HOST = "host";

    private Dao<SiteSettings, Long> dao;

    public DBSiteSettings(Dao<SiteSettings, Long> dao) {
        this.dao = dao;
    }

    public SiteSettings findForHost(String host) throws SQLException {
        return dao.queryBuilder().where().eq(DBSiteSettings.F_HOST, host).queryForFirst();
    }

    public int create(SiteSettings siteSettings) throws SQLException {
        return dao.create(siteSettings);
    }

    public int update(SiteSettings siteSettings) throws SQLException {
        return dao.update(siteSettings);
    }

    public int delete(SiteSettings siteSettings) throws SQLException {
        return dao.delete(siteSettings);
    }

    public List<SiteSettings> queryForAll() throws SQLException {
        return dao.queryForAll();
    }

    public long countOf() throws SQLException {
        return dao.countOf();
    }

}
