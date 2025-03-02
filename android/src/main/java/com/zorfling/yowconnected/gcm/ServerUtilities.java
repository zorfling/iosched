/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zorfling.yowconnected.gcm;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;

import com.zorfling.yowconnected.Config;
import com.zorfling.yowconnected.util.AccountUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;

import static com.zorfling.yowconnected.util.LogUtils.*;

/**
 * Helper class used to communicate with the demo server.
 */
public final class ServerUtilities {
    private static final String TAG = makeLogTag("GCMs");

    private static final String PREFERENCES = "com.zorfling.yowconnected.gcm";
    private static final String PROPERTY_REGISTERED_TS = "registered_ts";
    private static final String PROPERTY_REG_ID = "reg_id";
    private static final String PROPERTY_GCM_KEY = "gcm_key";
    private static final int MAX_ATTEMPTS = 5;
    private static final int BACKOFF_MILLI_SECONDS = 2000;

    private static final Random sRandom = new Random();

    private static boolean checkGcmEnabled() {
        if (TextUtils.isEmpty(Config.GCM_SERVER_URL)) {
            LOGD(TAG, "GCM feature disabled (no URL configured)");
            return false;
        } else if (TextUtils.isEmpty(Config.GCM_API_KEY)) {
            LOGD(TAG, "GCM feature disabled (no API key configured)");
            return false;
        } else if (TextUtils.isEmpty(Config.GCM_SENDER_ID)) {
            LOGD(TAG, "GCM feature disabled (no sender ID configured)");
            return false;
        }
        return true;
    }

    /**
     * Register this account/device pair within the server.
     *
     * @param context Current context
     * @param gcmId   The GCM registration ID for this device
     * @param gcmKey  The GCM key with which to register.
     * @return whether the registration succeeded or not.
     */
    public static boolean register(final Context context, final String gcmId, final String gcmKey) {
        if (!checkGcmEnabled()) {
            return false;
        }

        LOGI(TAG, "registering device (gcm_id = " + gcmId + ")");
        String serverUrl = Config.GCM_SERVER_URL + "/register";
        LOGI(TAG, "registering on GCM with GCM key: " + AccountUtils.sanitizeGcmKey(gcmKey));

        Map<String, String> params = new HashMap<String, String>();
        params.put("gcm_id", gcmId);
        params.put("gcm_key", gcmKey);
        long backoff = BACKOFF_MILLI_SECONDS + sRandom.nextInt(1000);
        // Once GCM returns a registration id, we need to register it in the
        // demo server. As the server might be down, we will retry it a couple
        // times.
        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
            LOGV(TAG, "Attempt #" + i + " to register");
            try {
                post(serverUrl, params, Config.GCM_API_KEY);
                setRegisteredOnServer(context, true, gcmId, gcmKey);
                return true;
            } catch (IOException e) {
                // Here we are simplifying and retrying on any error; in a real
                // application, it should retry only on unrecoverable errors
                // (like HTTP error code 503).
                LOGE(TAG, "Failed to register on attempt " + i, e);
                if (i == MAX_ATTEMPTS) {
                    break;
                }
                try {
                    LOGV(TAG, "Sleeping for " + backoff + " ms before retry");
                    Thread.sleep(backoff);
                } catch (InterruptedException e1) {
                    // Activity finished before we complete - exit.
                    LOGD(TAG, "Thread interrupted: abort remaining retries!");
                    Thread.currentThread().interrupt();
                    return false;
                }
                // increase backoff exponentially
                backoff *= 2;
            }
        }
        return false;
    }

    /**
     * Unregister this account/device pair within the server.
     *
     * @param context Current context
     * @param gcmId   The GCM registration ID for this device
     */
    static void unregister(final Context context, final String gcmId) {
        if (!checkGcmEnabled()) {
            return;
        }

        LOGI(TAG, "unregistering device (gcmId = " + gcmId + ")");
        String serverUrl = Config.GCM_SERVER_URL + "/unregister";
        Map<String, String> params = new HashMap<String, String>();
        params.put("gcm_id", gcmId);
        try {
            post(serverUrl, params, Config.GCM_API_KEY);
            setRegisteredOnServer(context, false, gcmId, null);
        } catch (IOException e) {
            // At this point the device is unregistered from GCM, but still
            // registered in the server.
            // We could try to unregister again, but it is not necessary:
            // if the server tries to send a message to the device, it will get
            // a "NotRegistered" error message and should unregister the device.
            LOGD(TAG, "Unable to unregister from application server", e);
        } finally {
            // Regardless of server success, clear local preferences
            setRegisteredOnServer(context, false, null, null);
        }
    }

    /**
     * Request user data sync.
     *
     * @param context Current context
     */
    public static void notifyUserDataChanged(final Context context) {
        if (!checkGcmEnabled()) {
            return;
        }

        LOGI(TAG, "Notifying GCM that user data changed");
        String serverUrl = Config.GCM_SERVER_URL + "/send/self/sync_user";
        try {
            String gcmKey = AccountUtils.getGcmKey(context, AccountUtils.getActiveAccountName(context));
            if (gcmKey != null) {
                post(serverUrl, new HashMap<String, String>(), gcmKey);
            }
        } catch (IOException e) {
            LOGE(TAG, "Unable to notify GCM about user data change", e);
        }
    }

    /**
     * Sets whether the device was successfully registered in the server side.
     *
     * @param context Current context
     * @param flag    True if registration was successful, false otherwise
     * @param gcmId    True if registration was successful, false otherwise
     */
    private static void setRegisteredOnServer(Context context, boolean flag, String gcmId, String gcmKey) {
        final SharedPreferences prefs = context.getSharedPreferences(
                PREFERENCES, Context.MODE_PRIVATE);
        LOGD(TAG, "Setting registered on server status as: " + flag + ", gcmKey="
                + AccountUtils.sanitizeGcmKey(gcmKey));
        Editor editor = prefs.edit();
        if (flag) {
            editor.putLong(PROPERTY_REGISTERED_TS, new Date().getTime());
            editor.putString(PROPERTY_GCM_KEY, gcmKey == null ? "" : gcmKey);
            editor.putString(PROPERTY_REG_ID, gcmId);
        } else {
            editor.remove(PROPERTY_REG_ID);
        }
        editor.commit();
    }

    /**
     * Checks whether the device was successfully registered in the server side.
     *
     * @param context Current context
     * @return True if registration was successful, false otherwise
     */
    public static boolean isRegisteredOnServer(Context context, String gcmKey) {
        final SharedPreferences prefs = context.getSharedPreferences(
                PREFERENCES, Context.MODE_PRIVATE);
        // Find registration threshold
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        long yesterdayTS = cal.getTimeInMillis();
        long regTS = prefs.getLong(PROPERTY_REGISTERED_TS, 0);

        gcmKey = gcmKey == null ? "" : gcmKey;

        if (regTS > yesterdayTS) {
            LOGV(TAG, "GCM registration current. regTS=" + regTS + " yesterdayTS=" + yesterdayTS);

            final String registeredGcmKey = prefs.getString(PROPERTY_GCM_KEY, "");
            if (registeredGcmKey.equals(gcmKey)) {
                LOGD(TAG, "GCM registration is valid and for the correct gcm key: "
                        + AccountUtils.sanitizeGcmKey(registeredGcmKey));
                return true;
            }
            LOGD(TAG, "GCM registration is for DIFFERENT gcm key "
                    + AccountUtils.sanitizeGcmKey(registeredGcmKey) + ". We were expecting "
                    + AccountUtils.sanitizeGcmKey(gcmKey));
            return false;
        } else {
            LOGV(TAG, "GCM registration expired. regTS=" + regTS + " yesterdayTS=" + yesterdayTS);
            return false;
        }
    }

    public static String getGcmId(Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        return prefs.getString(PROPERTY_REG_ID, null);
    }

    /**
     *  Unregister the current GCM ID when we sign-out
     *
     * @param context Current context
     */
    public static void onSignOut(Context context) {
        String gcmId = getGcmId(context);
        if (gcmId != null) {
            unregister(context, gcmId);
        }
    }

    /**
     * Issue a POST request to the server.
     *
     * @param endpoint POST address.
     * @param params   request parameters.
     * @throws java.io.IOException propagated from POST.
     */
    private static void post(String endpoint, Map<String, String> params, String key)
            throws IOException {
        URL url;
        try {
            url = new URL(endpoint);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("invalid url: " + endpoint);
        }
        params.put("key", key);
        StringBuilder bodyBuilder = new StringBuilder();
        Iterator<Entry<String, String>> iterator = params.entrySet().iterator();
        // constructs the POST body using the parameters
        while (iterator.hasNext()) {
            Entry<String, String> param = iterator.next();
            bodyBuilder.append(param.getKey()).append('=')
                    .append(param.getValue());
            if (iterator.hasNext()) {
                bodyBuilder.append('&');
            }
        }
        String body = bodyBuilder.toString();
        LOGV(TAG, "Posting '" + body + "' to " + url);
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setChunkedStreamingMode(0);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded;charset=UTF-8");
            conn.setRequestProperty("Content-Length",
                    Integer.toString(body.length()));
            // post the request
            OutputStream out = conn.getOutputStream();
            out.write(body.getBytes());
            out.close();
            // handle the response
            int status = conn.getResponseCode();
            if (status != 200) {
                throw new IOException("Post failed with error code " + status);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
