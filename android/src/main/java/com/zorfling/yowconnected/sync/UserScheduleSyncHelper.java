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

package com.zorfling.yowconnected.sync;

import android.content.Context;

import java.io.IOException;
import java.util.ArrayList;

import static com.zorfling.yowconnected.util.LogUtils.LOGI;
import static com.zorfling.yowconnected.util.LogUtils.makeLogTag;


public class UserScheduleSyncHelper {
    private Context mContext;
    private static final String TAG = makeLogTag(SyncHelper.class);

    public UserScheduleSyncHelper(Context ctx) {
        mContext = ctx;
    }

    public void updateUserSchedule(Context context, ArrayList<String> sessionsToAdd,
                ArrayList<String> sessionsToRemove) throws IOException {
        LOGI(TAG, "Updating session on user schedule: add "+sessionsToAdd.size()+
                " and remove "+sessionsToRemove.size()+" sessions");
        /*
        Googledevelopers conferenceAPI = getConferenceAPIClient();
        try {
            sendScheduleUpdate(conferenceAPI, context, sessionId, inSchedule);
        } catch (GoogleJsonResponseException e) {
            if (e.getDetails().getCode() == 401) {
                LOGI(TAG, "Unauthorized; getting a new auth token.", e);
                AccountUtils.refreshAuthToken(mContext);
                // Try request one more time with new credentials before giving up
                conferenceAPI = getConferenceAPIClient();
                sendScheduleUpdate(conferenceAPI, context, sessionId, inSchedule);
            }
        }
        */
    }
}
