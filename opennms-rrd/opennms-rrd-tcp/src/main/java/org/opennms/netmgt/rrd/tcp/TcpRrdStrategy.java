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
package org.opennms.netmgt.rrd.tcp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.opennms.netmgt.rrd.RrdDataSource;
import org.opennms.netmgt.rrd.RrdGraphDetails;
import org.opennms.netmgt.rrd.RrdStrategy;

/**
 * Provides a TCP socket-based implementation of RrdStrategy that pushes update
 * commands out in a simple serialized format.
 * <p>
 * The receiver of this strategy is not defined in any way. This is just a fire
 * and forget strategy. There is no way to read data back into opennms.
 * </p>
 * 
 * @author ranger
 * @version $Id: $
 */
public class TcpRrdStrategy implements RrdStrategy<TcpRrdStrategy.RrdDefinition,TcpRrdStrategy.RrdOutputSocketWithFilename> {
	
    public static class RrdDefinition {
        private final String m_directory, m_rrdName;
        public RrdDefinition(
                String directory,
                String rrdName
        ) {
            m_directory = directory;
            m_rrdName = rrdName;
        }

        public String getPath() {
            return m_directory + File.separator + m_rrdName;
        };
    }

    public static class RrdOutputSocketWithFilename {
        private final RrdOutputSocket m_socket;
        private final String m_filename;

        public RrdOutputSocketWithFilename(RrdOutputSocket socket, String filename) {
            m_socket = socket;
            m_filename = filename;
        }

        public RrdOutputSocket getSocket() {
            return m_socket;
        }

        public String getFilename() {
            return m_filename;
        }
    }

    private String m_host = null;

    /**
     * <p>getHost</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getHost() {
        return m_host;
    }

    /** {@inheritDoc} */
    @Override
    public void setConfigurationProperties(Properties configurationParameters) {
        // Do nothing
    }

    /**
     * <p>setHost</p>
     *
     * @param host a {@link java.lang.String} object.
     */
    public void setHost(String host) {
        this.m_host = host;
    }

    private int m_port = 0;

    /**
     * <p>getPort</p>
     *
     * @return a int.
     */
    public int getPort() {
        return m_port;
    }

    /**
     * <p>setPort</p>
     *
     * @param port a int.
     */
    public void setPort(int port) {
        this.m_port = port;
    }

    /**
     * <p>getDefaultFileExtension</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Override
    public String getDefaultFileExtension() {
        return "";
    }

    /** {@inheritDoc} */
    @Override
    public RrdDefinition createDefinition(String creator, String directory, String rrdName, int step, List<RrdDataSource> dataSources, List<String> rraList) throws Exception {
        return new RrdDefinition(directory, rrdName);
    }

    /**
     * <p>createFile</p>
     *
     * @param rrdDef a {@link RrdDefinition} object.
     * @throws java.lang.Exception if any.
     */
    @Override
	public void createFile(RrdDefinition rrdDef) throws Exception {
        // Do nothing
    }

    /** {@inheritDoc} */
    @Override
    public RrdOutputSocketWithFilename openFile(String fileName) throws Exception {
        return new RrdOutputSocketWithFilename(new RrdOutputSocket(m_host, m_port), fileName);
    }

    /** {@inheritDoc} */
    @Override
    public void updateFile(RrdOutputSocketWithFilename rrd, String owner, String data) throws Exception {
        rrd.getSocket().addData(rrd.getFilename(), owner, data);
    }

    /**
     * <p>closeFile</p>
     *
     * @param rrd a {@link org.opennms.netmgt.rrd.tcp.TcpRrdStrategy.RrdOutputSocketWithFilename} object.
     * @throws java.lang.Exception if any.
     */
    @Override
    public void closeFile(RrdOutputSocketWithFilename rrd) throws Exception {
        rrd.getSocket().writeData();
    }

    /** {@inheritDoc} */
    @Override
    public Double fetchLastValue(String rrdFile, String ds, int interval) throws NumberFormatException {
        return Double.NaN;
    }

    /** {@inheritDoc} */
    @Override
    public Double fetchLastValue(String rrdFile, String ds, String consolidationFunction, int interval) throws NumberFormatException {
        return Double.NaN;
    }

    /** {@inheritDoc} */
    @Override
    public Double fetchLastValueInRange(String rrdFile, String ds, int interval, int range) throws NumberFormatException {
        return Double.NaN;
    }

    /** {@inheritDoc} */
    @Override
    public InputStream createGraph(String command, File workDir) throws IOException {
        throw new UnsupportedOperationException(this.getClass().getName() + " does not support graphing.");
    }

    /** {@inheritDoc} */
    @Override
    public RrdGraphDetails createGraphReturnDetails(String command, File workDir) throws IOException {
        throw new UnsupportedOperationException(this.getClass().getName() + " does not support graphing.");
    }

    /**
     * <p>getGraphLeftOffset</p>
     *
     * @return a int.
     */
    @Override
    public int getGraphLeftOffset() {
        throw new UnsupportedOperationException(this.getClass().getName() + " does not support graphing.");
    }

    /**
     * <p>getGraphRightOffset</p>
     *
     * @return a int.
     */
    @Override
    public int getGraphRightOffset() {
        throw new UnsupportedOperationException(this.getClass().getName() + " does not support graphing.");
    }

    /**
     * <p>getGraphTopOffsetWithText</p>
     *
     * @return a int.
     */
    @Override
    public int getGraphTopOffsetWithText() {
        throw new UnsupportedOperationException(this.getClass().getName() + " does not support graphing.");
    }

    /**
     * <p>getStats</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Override
    public String getStats() {
        throw new UnsupportedOperationException(this.getClass().getName() + " does not support graphing.");
    }

    /** {@inheritDoc} */
    @Override
    public void promoteEnqueuedFiles(Collection<String> rrdFiles) {
        // Do nothing; this implementation simply sends data to an external source and has not control
        // over when data is persisted to disk.
    }
}
