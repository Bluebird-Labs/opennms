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
package org.opennms.netmgt.enlinkd.model;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.EnumType;
import org.hibernate.type.IntegerType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

public class ImportAsExternUserType extends EnumType {


    private static final int[] SQL_TYPES = new int[] { java.sql.Types.INTEGER };

    private static final long serialVersionUID = -3640986963000561149L;

    /**
     * A public default constructor is required by Hibernate.
     */
    public ImportAsExternUserType() {}

    @Override
    public int hashCode(final Object x) throws HibernateException {
        return x == null ? 0 : x.hashCode();
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {
        final Integer c = (Integer) IntegerType.INSTANCE.nullSafeGet(rs, names[0], session, owner);
        if (c == null) {
            return null;
        }
        for (OspfArea.ImportAsExtern type : OspfArea.ImportAsExtern.values()) {
            if (type.getValue().intValue() == c.intValue()) {
                return type;
            }
        }
        throw new HibernateException("Invalid value for ImportAsExtern: " + c);
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
        if (value == null) {
            IntegerType.INSTANCE.nullSafeSet(st, null, index, session);
        } else if (value instanceof OspfArea.ImportAsExtern){
            IntegerType.INSTANCE.nullSafeSet(st, ((OspfArea.ImportAsExtern)value).getValue(), index, session);
        }
    }

    @Override
    public Class<OspfArea.ImportAsExtern> returnedClass() {
        return OspfArea.ImportAsExtern.class;
    }

    @Override
    public int[] sqlTypes() {
        return SQL_TYPES;
    }

    @Override
    public void setParameterValues(Properties parameters) {
    }
}
