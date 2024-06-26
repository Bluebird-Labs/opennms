/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.features.situationfeedback.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Enums;

/**
 * Expresses Feedback on the Correlation of an Alarm.
 */
@JsonDeserialize(builder = AlarmFeedback.Builder.class)
public final class AlarmFeedback {

    public enum FeedbackType {
        FALSE_POSITIVE, // Alarm does not belong in this Situation
        FALSE_NEGATIVE, // Alarm was missing from this Situation
        CREATE_SITUATION, // Alarm should be correlated in a new Situation
        CORRECT, // Alarm is correctly correlated
        UNKNOWN;

        public static FeedbackType valueOfOrUnknown(String type) {
            return Enums.getIfPresent(FeedbackType.class, type).or(UNKNOWN);
        }
    }

    // Situation ReductionKey
    private final String situationKey;

    // this may not be nesc but may prove helpful
    private final String situationFingerprint; // fingerprint of situation/alarms at time of feedback

    // Alarm ReductionKey
    private final String alarmKey;

    private final FeedbackType feedbackType;

    private final boolean rootCause;

    private final String reason;

    private final List<String> tags = new ArrayList<>();

    private final String user;

    private final long timestamp;

    private AlarmFeedback(Builder builder) {
        this.situationKey = builder.situationKey;
        this.situationFingerprint = builder.situationFingerprint;
        this.alarmKey = builder.alarmKey;
        this.feedbackType = builder.feedbackType;
        this.reason = builder.reason;
        this.rootCause = builder.rootCause != null && builder.rootCause;
        this.tags.addAll(builder.tags);
        this.user = builder.user;
        this.timestamp = builder.timestamp;
    }

    public static class Builder {
        private String situationKey;
        private String situationFingerprint;
        private String alarmKey;
        private AlarmFeedback.FeedbackType feedbackType;
        private String reason;
        @JsonProperty(value="rootCause")
        private Boolean rootCause;
        private List<String> tags = new ArrayList<>();
        private String user;
        private Long timestamp;

        // This constructor is public for the purposes of JSON deserialization
        public Builder() {
            timestamp = System.currentTimeMillis();
        }

        public Builder withSituationKey(String situationKey) {
            this.situationKey = situationKey;
            return this;
        }

        public Builder withSituationFingerprint(String situationFingerprint) {
            this.situationFingerprint = situationFingerprint;
            return this;
        }

        public Builder withAlarmKey(String alarmKey) {
            this.alarmKey = alarmKey;
            return this;
        }

        public Builder withFeedbackType(AlarmFeedback.FeedbackType feedbackType) {
            this.feedbackType = feedbackType;
            return this;
        }

        public Builder withRootCause(Boolean rootCause) {
            this.rootCause = rootCause;
            return this;
        }

        public Builder withReason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder withTags(List<String> tags) {
            this.tags.addAll(tags);
            return this;
        }

        public Builder withUser(String user) {
            this.user = user;
            return this;
        }

        public Builder withTimestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public AlarmFeedback build() {
            Objects.requireNonNull(situationKey, "The situation key cannot be null");
            Objects.requireNonNull(alarmKey, "The alarm key cannot be null");
            Objects.requireNonNull(feedbackType, "The feedback type cannot be null");
            Objects.requireNonNull(timestamp, "The timestamp cannot be null");

            return new AlarmFeedback(this);            
        }
    }
    
    public static Builder newBuilder() {
        return new Builder();
    }

    public String getSituationKey() {
        return situationKey;
    }

    public String getSituationFingerprint() {
        return situationFingerprint;
    }

    public String getAlarmKey() {
        return alarmKey;
    }

    public FeedbackType getFeedbackType() {
        return feedbackType;
    }

    public boolean getRootCause() {
        return rootCause;
    }

    public String getReason() {
        return reason;
    }

    public List<String> getTags() {
        return Collections.unmodifiableList(tags);
    }

    public String getUser() {
        return user;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlarmFeedback that = (AlarmFeedback) o;
        return timestamp == that.timestamp &&
                Objects.equals(situationKey, that.situationKey) &&
                Objects.equals(situationFingerprint, that.situationFingerprint) &&
                Objects.equals(alarmKey, that.alarmKey) &&
                feedbackType == that.feedbackType &&
                Objects.equals(reason, that.reason) &&
                Objects.equals(rootCause, that.rootCause) &&
                Objects.equals(tags, that.tags) &&
                Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(situationKey, situationFingerprint, alarmKey, feedbackType, reason, rootCause, tags, user, timestamp);
    }

    @Override
    public String toString() {
        return "AlarmFeedback{" +
                "situationKey='" + situationKey + '\'' +
                ", situationFingerprint='" + situationFingerprint + '\'' +
                ", alarmKey='" + alarmKey + '\'' +
                ", feedbackType=" + feedbackType +
                ", reason='" + reason + '\'' +
                ", rootCause='" + rootCause + '\'' +
                ", tags='" + tags + '\'' +
                ", user='" + user + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
