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
package org.opennms.netmgt.events.api.model;

import java.util.Objects;

/**
 * An immutable implementation of '{@link ISnmp}'.
 */
public final class ImmutableSnmp implements ISnmp {
    private final String id;
    private final String trapOID;
    private final String idText;
    private final String version;
    private final Integer specific;
    private final Integer generic;
    private final String community;
    private final Long timeStamp;

    private ImmutableSnmp(Builder builder) {
        id = builder.id;
        idText = builder.idText;
        version = builder.version;
        specific = builder.specific;
        generic = builder.generic;
        community = builder.community;
        timeStamp = builder.timeStamp;
        trapOID = builder.trapOID;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilderFrom(ISnmp snmp) {
        return new Builder(snmp);
    }

    public static ISnmp immutableCopy(ISnmp snmp) {
        if (snmp == null || snmp instanceof ImmutableSnmp) {
            return snmp;
        }
        return newBuilderFrom(snmp).build();
    }

    public static final class Builder {
        private String id;
        private String idText;
        private String version;
        private Integer specific;
        private Integer generic;
        private String community;
        private Long timeStamp;
        private String trapOID;

        private Builder() {
        }

        public Builder(ISnmp snmp) {
            id = snmp.getId();
            idText = snmp.getIdtext();
            version = snmp.getVersion();
            specific = snmp.getSpecific();
            generic = snmp.getGeneric();
            community = snmp.getCommunity();
            timeStamp = snmp.getTimeStamp();
        }

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setTrapOID(String trapOID) {
            this.trapOID = trapOID;
            return this;
        }

        public Builder setIdText(String idText) {
            this.idText = idText;
            return this;
        }

        public Builder setVersion(String version) {
            this.version = version;
            return this;
        }

        public Builder setSpecific(Integer specific) {
            this.specific = specific;
            return this;
        }

        public Builder setGeneric(Integer generic) {
            this.generic = generic;
            return this;
        }

        public Builder setCommunity(String community) {
            this.community = community;
            return this;
        }

        public Builder setTimeStamp(Long timeStamp) {
            this.timeStamp = timeStamp;
            return this;
        }

        public ImmutableSnmp build() {
            return new ImmutableSnmp(this);
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getTrapOID() {
        return trapOID;
    }

    @Override
    public String getIdtext() {
        return idText;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public Integer getSpecific() {
        return specific == null ? 0 : specific;
    }

    @Override
    public Integer getGeneric() {
        return generic == null ? 0 : generic;
    }

    @Override
    public String getCommunity() {
        return community;
    }

    @Override
    public Long getTimeStamp() {
        return timeStamp == null ? 0 : timeStamp;
    }

    @Override
    public boolean hasGeneric() {
        return generic != null;
    }

    @Override
    public boolean hasSpecific() {
        return specific != null;
    }

    @Override
    public boolean hasTimeStamp() {
        return timeStamp != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImmutableSnmp that = (ImmutableSnmp) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(idText, that.idText) &&
                Objects.equals(version, that.version) &&
                Objects.equals(specific, that.specific) &&
                Objects.equals(generic, that.generic) &&
                Objects.equals(community, that.community) &&
                Objects.equals(timeStamp, that.timeStamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, idText, version, specific, generic, community, timeStamp);
    }

    @Override
    public String toString() {
        return "ImmutableSnmp{" +
                "id='" + id + '\'' +
                ", idText='" + idText + '\'' +
                ", version='" + version + '\'' +
                ", specific=" + specific +
                ", generic=" + generic +
                ", community='" + community + '\'' +
                ", timeStamp=" + timeStamp +
                '}';
    }
}
