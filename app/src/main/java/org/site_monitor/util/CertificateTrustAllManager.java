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

package org.site_monitor.util;

import android.util.Log;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Bad implementation : must implements like http://developer.android.com/training/articles/security-ssl.html
 * No risk for user cause no information are provided (credential, position or other sensible data)
 * Created by Martin Norbert on 30/01/2016.
 */
public class CertificateTrustAllManager implements X509TrustManager {

    public static final String TLS = "TLS";
    private static final X509TrustManager INSTANCE = new CertificateTrustAllManager();
    private static final String TAG = CertificateTrustAllManager.class.getSimpleName();

    public static SSLSocketFactory sslSocketFactory() {
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            trustManagerFactory.init(keyStore);
            TrustManager[] trustAllCertManagers = {INSTANCE};
            SSLContext sslContext = SSLContext.getInstance(TLS);
            sslContext.init(null, trustAllCertManagers, new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (CertificateException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException | IOException e) {
            Log.e(TAG, "sslSocketFactory", e);
            return null;
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) {
        //Bad implementation - do nothing
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) {
        //Bad implementation - do nothing
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }

}
