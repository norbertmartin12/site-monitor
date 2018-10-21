package org.site_monitor.service;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.util.Log;

import org.site_monitor.BuildConfig;
import org.site_monitor.model.bo.SiteSettings;
import org.site_monitor.model.db.DBHelper;
import org.site_monitor.model.db.DBSiteSettings;
import org.site_monitor.util.BroadcastUtil;
import org.site_monitor.util.NetworkUtil;

import java.io.ByteArrayOutputStream;
import java.sql.SQLException;

public class FavIconService extends JobIntentService {

    private static final String TAG = FavIconService.class.getSimpleName();
    private static final String P_URL = "url";
    public static final String ACTION_FAVICON_UPDATED = "org.site_monitor.service.action.FAVICON_UPDATED";
    public static final String REQUEST_REFRESH_FAVICON = "loadFavicon";

    public static void enqueueLoadFavIcoWork(@NonNull Context context, String url) {
        FavIconService.enqueueWork(context, FavIconService.class, JobEnum.LOAD_FAVICO.ordinal(), intentToLoadFavicon(context, url));
    }

    public static Intent intentToLoadFavicon(Context context, String url) {
        return new Intent(context, FavIconService.class).setAction(REQUEST_REFRESH_FAVICON).putExtra(P_URL, url);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        if (intent.getAction() == null) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "onHandleIntent: no action");
            }
            return;
        }
        DBHelper dbHelper = DBHelper.getHelper(this);
        try {
            if (intent.getAction().equals(REQUEST_REFRESH_FAVICON)) {
                DBSiteSettings siteSettingDao = dbHelper.getDBSiteSettings();
                String url = intent.getStringExtra(P_URL);
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "action: " + REQUEST_REFRESH_FAVICON + " " + url);
                }
                refreshFavicon(siteSettingDao, url);
            }
        } catch (SQLException e) {
            Log.e(TAG, "onHandleWork", e);
        } finally {
            if (dbHelper != null) {
                dbHelper.release();
            }
        }
    }

    private void refreshFavicon(DBSiteSettings siteSettingDao, String url) throws SQLException {
        SiteSettings siteSettings = siteSettingDao.findForHost(url);
        if (siteSettings == null) {
            Log.w(TAG, "refreshFavicon, no site for: " + url);
            return;
        }
        Log.d(TAG, "favicon: " + siteSettings);
        Bitmap favicon = NetworkUtil.loadFaviconFor(siteSettings.getHost());
        if (favicon == null) {
            Log.w(TAG, "refreshFavicon, no favicon for: " + url);
            return;
        }
        ByteArrayOutputStream blob = new ByteArrayOutputStream();
        favicon.compress(Bitmap.CompressFormat.PNG, 0 /* Ignored for PNGs */, blob);
        siteSettings.setFavicon(blob.toByteArray());
        siteSettingDao.update(siteSettings);
        BroadcastUtil.broadcast(this, ACTION_FAVICON_UPDATED, siteSettings, BroadcastUtil.EXTRA_FAVICON, favicon);
    }
}
