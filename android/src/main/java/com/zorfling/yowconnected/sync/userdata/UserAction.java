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
package com.zorfling.yowconnected.sync.userdata;

public class UserAction {
    public enum TYPE {
        ADD_STAR, REMOVE_STAR;
    };

    public UserAction() {
    }

    public UserAction(TYPE type, String sessionId) {
        this.type = type;
        this.sessionId = sessionId;
    }

    public TYPE type;
    public String sessionId;
    public String accountName;
    public boolean requiresSync;

}
