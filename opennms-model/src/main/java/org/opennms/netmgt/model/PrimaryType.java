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

import java.util.stream.Stream;

public enum PrimaryType {
    NOT_ELIGIBLE("N"),
    SECONDARY("S"),
    PRIMARY("P");

    private final String code;

    PrimaryType(final String type) {
        this.code = type;
    }

    public String getCode() {
        return code;
    }

    public char getCharCode() {
        return code.charAt(0);
    }

    public boolean isLessThan(final PrimaryType collType) {
        return compareTo(collType) < 0;
    }

    public boolean isGreaterThan(final PrimaryType collType) {
        return compareTo(collType) > 0;
    }

    public PrimaryType max(final PrimaryType collType) {
        return this.isLessThan(collType) ? collType : this;
    }

    public PrimaryType min(final PrimaryType collType) {
        return this.isLessThan(collType) ? this : collType;
    }

    public static PrimaryType get(final char code) {
        return Stream.of(values())
                .filter(it -> it.getCharCode() == code)
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Cannot create collType from code " + code));
    }

    public static PrimaryType get(final String type) {
        return Stream.of(values())
                .filter(it -> it.name().equalsIgnoreCase(type))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Cannot convert string '" + type + "' to a collType"));
    }

    /**
     * @deprecated Use dedicated get method instead of relying on generating from Object
     */
    @Deprecated
    public static PrimaryType get(final Object code) {
        if (code == null || code.toString().trim().isEmpty()) {
            return NOT_ELIGIBLE;
        } else if (code instanceof Character) {
            return get(((Character) code).charValue());
        } else {
            final String codeText = code.toString().trim();
            if (codeText.length() == 1) {
                return get(codeText.charAt(0));
            } else {
                return get(codeText.length());
            }
        }
    }

}
