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
package org.opennms.netmgt.vmmgr;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opennms.core.utils.ConfigFileConstants;
import org.opennms.core.xml.JaxbUtils;
import org.opennms.netmgt.config.opennmsDataSources.DataSourceConfiguration;
import org.opennms.netmgt.config.opennmsDataSources.JdbcDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This is the singleton class used to load the OpenNMS database configuration
 * from the opennms-database.xml. This provides convenience methods to create
 * database connections to the database configured in this default xml
 * </p>
 *
 * <p>
 * <strong>Note: </strong>Users of this class should make sure the
 * <em>init()</em> is called before calling any other method to ensure the
 * config is loaded before accessing other convenience methods
 * </p>
 *
 * @author <a href="mailto:weave@oculan.com">Brian Weaver </a>
 */
public class DatabaseChecker {
	
	private static final Logger LOG = LoggerFactory.getLogger(DatabaseChecker.class);
	
    private static List<String> m_required = new ArrayList<>();
    private static List<String> m_optional = new ArrayList<>();
    private Map<String,JdbcDataSource> m_dataSources = new HashMap<String,JdbcDataSource>();

    static {
        m_required.add("opennms");
        m_optional.add("opennms-admin");
    }
    
    /**
     * Protected constructor
     *
     * @exception java.io.IOException
     *                Thrown if the specified config file cannot be read
     * @param configFile a {@link java.lang.String} object.
     * @throws java.io.IOException if any.
     * @throws java.lang.ClassNotFoundException if any.
     */
    protected DatabaseChecker(final String configFile) throws IOException, ClassNotFoundException {
        final DataSourceConfiguration database;
        try {
            database = JaxbUtils.unmarshal(DataSourceConfiguration.class, new File(configFile));
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to unmarshal: %s Cause: %s",
                    configFile, e.getMessage()), e);
        }

        for (final JdbcDataSource dataSource : database.getJdbcDataSourceCollection()) {
            m_dataSources.put(dataSource.getName(), dataSource);
        }
    }

    /**
     * Load the config from the default config file and create the singleton
     * instance of this factory.
     *
     * @exception java.io.IOException
     *                Thrown if the specified config file cannot be read
     * @throws java.io.IOException if any.
     * @throws java.lang.ClassNotFoundException if any.
     */
    protected DatabaseChecker() throws IOException, ClassNotFoundException {
    	this(ConfigFileConstants.getFile(ConfigFileConstants.OPENNMS_DATASOURCE_CONFIG_FILE_NAME).getPath());
    }

    /**
     * <p>Check whether the data sources in opennms-datasources.xml are valid.</p>
     *
     * @throws MissingDataSourceException A required data source was not found in opennms-datasources.xml.
     * @throws InvalidDataSourceException A required data source could not be connected to.
     */
    public void check() throws MissingDataSourceException, InvalidDataSourceException {

        // First, check to make sure the required datasources are there.
        boolean dataSourcesFound = true;
        for (final String dataSource : m_required) {
            if (!m_dataSources.containsKey(dataSource)) {
            	LOG.error("Required data source '{}' is missing from opennms-datasources.xml", dataSource);
                dataSourcesFound = false;
            }
        }
        if (!dataSourcesFound) {
            throw new MissingDataSourceException("OpenNMS is missing one or more data sources required for startup.");
        }

        // Then, check for the optional ones so we can warn about them going missing.
        for (final String dataSource : m_optional) {
            if (!m_dataSources.containsKey(dataSource)) {
            	LOG.info("Data source '{}' is missing from opennms-datasources.xml", dataSource);
            }
        }
        
        // Finally, try connecting to all data sources, and warn or error as appropriate.
        for (final JdbcDataSource dataSource : m_dataSources.values()) {
            Connection connection = null;

            try {
                final String name = dataSource.getName();
                if (!m_required.contains(name) && !m_optional.contains(name)) {
                    LOG.warn("Unknown datasource '{}' was found.", name);
                }
                try {
                    Class.forName(dataSource.getClassName());
                    connection = DriverManager.getConnection(dataSource.getUrl(), dataSource.getUserName(), dataSource.getPassword());
                } catch (final Throwable t) {
                    final String errorMessage = "Unable to connect to data source '{}' at URL '{}' with username '{}', check opennms-datasources.xml and your database permissions.";
                    if (m_required.contains(name)) {
                        LOG.error(errorMessage, name, dataSource.getUrl(), dataSource.getUserName());
                        throw new InvalidDataSourceException("Data source '" + name + "' failed.", t);
                    } else {
                        LOG.warn(errorMessage, name, dataSource.getUrl(), dataSource.getUserName());
                    }
                }
            } finally {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (final SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    /**
     * <p>main</p>
     *
     * @param argv an array of {@link java.lang.String} objects.
     * @throws java.lang.Exception if any.
     */
    public static void main(final String[] argv) throws Exception {
        final DatabaseChecker checker = new DatabaseChecker();
    	checker.check();
    }
}
