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
package org.opennms.netmgt.collection.core;

import java.net.InetAddress;
import java.util.Objects;
import java.util.Properties;

import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.collection.api.CollectionAgent;
import org.opennms.netmgt.collection.api.CollectionAgentService;
import org.opennms.netmgt.dao.api.IpInterfaceDao;
import org.opennms.netmgt.model.OnmsIpInterface;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.model.ResourcePath;
import org.opennms.netmgt.model.ResourceTypeUtils;
import org.opennms.netmgt.model.monitoringLocations.OnmsMonitoringLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.TransactionProxyFactoryBean;

/**
 * Represents a remote SNMP agent on a specific IPv4 interface.
 *
 * @author ranger
 * @version $Id: $
 */
// Eventually, we should be constructing these instances in the context and using
// annotation-based transaction processing.
//@Transactional(propagation=Propagation.REQUIRED)
public class DefaultCollectionAgentService implements CollectionAgentService {
    
    private static final Logger LOG = LoggerFactory.getLogger(DefaultCollectionAgentService.class);
    
    /**
     * <p>create</p>
     *
     * @param ifaceId a {@link java.lang.Integer} object.
     * @param ifaceDao a {@link org.opennms.netmgt.dao.api.IpInterfaceDao} object.
     * @param transMgr a {@link org.springframework.transaction.PlatformTransactionManager} object.
     * @return a {@link org.opennms.netmgt.collection.api.CollectionAgentService} object.
     */
    public static CollectionAgentService create(Integer ifaceId, final IpInterfaceDao ifaceDao, final PlatformTransactionManager transMgr) {
        CollectionAgentService agent = new DefaultCollectionAgentService(ifaceId, ifaceDao);
        
        TransactionProxyFactoryBean bean = new TransactionProxyFactoryBean();
        bean.setTransactionManager(transMgr);
        bean.setTarget(agent);
        
        Properties props = new Properties();
        props.put("*", "PROPAGATION_REQUIRED");
        
        bean.setTransactionAttributes(props);
        
        bean.afterPropertiesSet();
        
        return (CollectionAgentService) bean.getObject();
    }

    // the interface of the Agent
    private final Integer m_ifaceId;
    private final IpInterfaceDao m_ifaceDao;

    protected DefaultCollectionAgentService(Integer ifaceId, IpInterfaceDao ifaceDao) {
        // we pass in null since we override calls to getAddress and getInetAddress
        m_ifaceId = ifaceId;
        m_ifaceDao = ifaceDao;
    }

    protected final OnmsIpInterface getIpInterface() {
        return m_ifaceDao.load(m_ifaceId);
    }

    /**
     * <p>getHostAddress</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Override
    public final String getHostAddress() {
        return InetAddressUtils.str(getInetAddress());
    }

    /**
     * <p>isStoreByForeignSource</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    private static Boolean isStoreByForeignSource() {
        return ResourceTypeUtils.isStoreByForeignSource();
    }

    /**
     * <p>getNodeId</p>
     *
     * @return a int.
     */
    @Override
    public final int getNodeId() {
        final OnmsNode node = getIpInterface().getNode();
        return node.getId() == null ? -1 : node.getId().intValue();
    }

    /**
     * <p>getNodeLabel</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Override
    public final String getNodeLabel() {
        return getIpInterface().getNode().getLabel();
    }

    /**
     * <p>getForeignSource</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Override
    public final String getForeignSource() {
       return getIpInterface().getNode().getForeignSource();
    }

    /**
     * <p>getForeignId</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Override
    public final String getForeignId() {
       return getIpInterface().getNode().getForeignId();
    }

    @Override
    public final String getLocationName() {
        final OnmsMonitoringLocation location = getIpInterface().getNode().getLocation();
        if (location != null) {
            return location.getLocationName();
        }
        return null;
    }

    /**
     * <p>getStorageDir</p>
     *
     * @return a {@link java.io.File} object.
     */
    @Override
    public final ResourcePath getStorageResourcePath() {
        final String foreignSource = getForeignSource();
        final String foreignId = getForeignId();
        final ResourcePath dir = createStorageResourcePath(foreignSource, foreignId, getNodeId());
        LOG.debug("getStorageDir: isStoreByForeignSource = {}, foreignSource = {}, foreignId = {}, dir = {}", isStoreByForeignSource(), foreignSource, foreignId, dir);
        return dir;
    }

    /**
     * <p>toString</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Override
    public String toString() {
        return "Agent[nodeid = "+getNodeId()+" ipaddr= "+getHostAddress()+']';
    }

    /**
     * <p>getInetAddress</p>
     *
     * @return a {@link java.net.InetAddress} object.
     */
    @Override
    public final InetAddress getInetAddress() {
        return getIpInterface().getIpAddress();
    }

    protected static ResourcePath createStorageResourcePath(CollectionAgent agent) {
        Objects.requireNonNull(agent);
        return createStorageResourcePath(agent.getForeignSource(), agent.getForeignId(), agent.getNodeId());
    }

    private static ResourcePath createStorageResourcePath(String foreignSource, String foreignId, int nodeId) {
        final ResourcePath dir;
        if(isStoreByForeignSource() && foreignSource != null && foreignId != null) {
            dir = ResourcePath.get(ResourceTypeUtils.FOREIGN_SOURCE_DIRECTORY,
                    foreignSource,
                    foreignId);
        } else {
            dir = ResourcePath.get(String.valueOf(nodeId));
        }
        return dir;
    }
}
