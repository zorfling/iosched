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
package com.zorfling.yowconnected.model;

import com.zorfling.yowconnected.provider.ScheduleContract;

public class ScheduleItem implements Cloneable, Comparable<ScheduleItem> {
    // types:
    public static final int FREE = 0;  // a free chunk of time
    public static final int SESSION = 1; // a session
    public static final int BREAK = 2; // a break (lunch, breaks, after-hours party)

    // item type
    public int type = FREE;

    // start and end time for this item
    public long startTime = 0;
    public long endTime = 0;

    // number of sessions available in this block (usually for free blocks)
    public int numOfSessions = 0;

    // session id
    public String sessionId = "";

    // title and subtitle
    public String title = "";
    public String subtitle = "";

    // has feedback been given on this session?
    public boolean hasGivenFeedback;

    // background image URL
    public String backgroundImageUrl = "";
    public int backgroundColor = 0;

    // flags
    public int flags = 0;
    public static final int FLAG_HAS_LIVESTREAM = 0x01;
    public static final int FLAG_NOT_REMOVABLE = 0x02;
    public static final int FLAG_CONFLICTS_WITH_PREVIOUS = 0x04;

    public void setTypeFromBlockType(String blockType) {
        if (!ScheduleContract.Blocks.isValidBlockType(blockType) ||
                ScheduleContract.Blocks.BLOCK_TYPE_FREE.equals(blockType)) {
            type = FREE;
        } else {
            type = BREAK;
        }
    }

    @Override
    public Object clone()  {
        try {
            return super.clone();
        } catch (CloneNotSupportedException unused) {
            // does not happen (since we implement Cloneable)
            return new ScheduleItem();
        }
    }

    @Override
    public int compareTo(ScheduleItem another) {
        return this.startTime < another.startTime ? -1 :
                ( this.startTime > another.startTime ? 1 : 0 );
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof ScheduleItem)) {
            return false;
        }
        ScheduleItem i = (ScheduleItem) o;
        return type == i.type &&
                sessionId.equals(i.sessionId) &&
                startTime == i.startTime &&
                endTime == i.endTime;
    }

    @Override
    public String toString() {
        return String.format("[item type=%d, startTime=%d, endTime=%d, title=%s, subtitle=%s, flags=%d]",
                type, startTime, endTime, title, subtitle, flags);
    }
}
