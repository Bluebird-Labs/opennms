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
package org.opennms.netmgt.model;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Enum for severities.
 *
 * @author <a href="mailto:dj@opennms.org">DJ Gregor</a>
 */
public enum OnmsSeverity implements Serializable {
    // Keep this ordered by ID so we can use the internal enum compareTo
    INDETERMINATE(1, "Indeterminate", "lightblue"),
    CLEARED(2, "Cleared", "white"),
    NORMAL(3, "Normal", "green"),
    WARNING(4, "Warning", "cyan"),
    MINOR(5, "Minor", "yellow"),
    MAJOR(6, "Major", "orange"),
    CRITICAL(7, "Critical", "red");
    
    private final int id;
    private final String label;
    private final String color;

    OnmsSeverity(final int id, final String label, final String color) {
        this.id = id;
        this.label = label;
        this.color = color;
    }

    public int getId() {
        return id;
    }
    
    public String getLabel() {
        return label;
    }

    public String getColor() {
        return color;
    }

    public boolean isLessThan(final OnmsSeverity other) {
        return compareTo(other) < 0;
    }

    public boolean isLessThanOrEqual(final OnmsSeverity other) {
        return compareTo(other) <= 0;
    }

    public boolean isGreaterThan(final OnmsSeverity other) {
        return compareTo(other) > 0;
    }
    
    public boolean isGreaterThanOrEqual(final OnmsSeverity other) {
        return compareTo(other) >= 0;
    }
    
    public static OnmsSeverity get(final int id) {
        return Stream.of(values())
                .filter(it -> it.getId() == id)
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Cannot create OnmsSeverity from unknown ID " + id));
    }

    public static OnmsSeverity get(final String label) {
        return Stream.of(values())
                .filter(it -> it.getLabel().equalsIgnoreCase(label))
                .findAny().orElse(OnmsSeverity.INDETERMINATE);
    }

    /**
     * Temporary added for easier migration, but this is no longer needed.
     * @deprecated Use instance directly instead of wrapping in get method
     */
    @Deprecated(forRemoval = true)
    public static OnmsSeverity get(OnmsSeverity eventSeverity) {
        return eventSeverity;
    }

    public static OnmsSeverity escalate(final OnmsSeverity sev) {
        if (sev.isLessThan(OnmsSeverity.CRITICAL)) {
            return OnmsSeverity.get(sev.getId()+1);
        } else {
            return OnmsSeverity.get(sev.getId());
        }
    }

    public static List<String> names() {
        return Stream.of(values()).map(Enum::toString)
                .distinct()
                .collect(Collectors.toList());
    }
}
