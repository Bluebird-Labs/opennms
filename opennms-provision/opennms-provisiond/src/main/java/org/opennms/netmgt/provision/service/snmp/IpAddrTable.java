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
package org.opennms.netmgt.provision.service.snmp;

import java.net.InetAddress;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.model.OnmsIpInterface;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.model.OnmsSnmpInterface;
import org.opennms.netmgt.snmp.SnmpInstId;
import org.opennms.netmgt.snmp.SnmpObjId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <P>
 * IpAddrTable uses a SnmpSession to collect the ipAddrTable entries It
 * implements the SnmpHandler to receive notifications when a reply is
 * received/error occurs in the SnmpSession used to send requests /recieve
 * replies.
 * </P>
 *
 * @author <A HREF="mailto:brozow@opennms.org">Matt Brozowski</A>
 * @author <A HREF="mailto:jamesz@opennms.org">James Zuo </A>
 * @author <A HREF="mailto:sowmya@opennms.org">Sowmya </A>
 * @author <A HREF="mailto:weave@oculan.com">Weave </A>
 * @see <A HREF="http://www.ietf.org/rfc/rfc1213.txt">RFC1213 </A>
 */
public class IpAddrTable extends SnmpTable<IpAddrTableEntry> {
    private static final Logger LOG = LoggerFactory.getLogger(IpAddrTable.class);

    /**
     * <P>
     * Constructs an IpAddrTable object that is used to collect the address
     * elements from the remote agent. Once all the elements are collected, or
     * there is an error in the collection the signaler object is <EM>notified
     * </EM> to inform other threads.
     * </P>
     *
     * @param address TODO
     * @see IpAddrTableEntry
     */
    public IpAddrTable(InetAddress address) {
        super(address, "ipAddrTable", IpAddrTableEntry.ms_elemList);
    }

    /**
     * <p>Constructor for IpAddrTable.</p>
     *
     * @param address a {@link java.net.InetAddress} object.
     * @param ipAddrs a {@link java.util.Set} object.
     */
    public IpAddrTable(InetAddress address, Set<SnmpInstId> ipAddrs) {
        super(address, "ipAddrTable", IpAddrTableEntry.ms_elemList, ipAddrs);
    }

    /** {@inheritDoc} */
    @Override
    protected IpAddrTableEntry createTableEntry(SnmpObjId base, SnmpInstId inst, Object val) {
        return new IpAddrTableEntry();
    }
    
    /**
     * <p>getIfIndices</p>
     *
     * @return a {@link java.util.Set} object.
     */
    public Set<Integer> getIfIndices() {
        Set<Integer> ifIndices = new TreeSet<>();
        for(IpAddrTableEntry entry : getEntries()) {
            Integer ifIndex = entry.getIpAdEntIfIndex();
            if (ifIndex != null) {
                ifIndices.add(ifIndex);
            }
        }
        return ifIndices;
    }

    /**
     * <p>getIfAddress</p>
     *
     * @param ifIndex a int.
     * @return a {@link java.net.InetAddress} object.
     */
    public InetAddress getIfAddress(int ifIndex) {
        IpAddrTableEntry entry = getEntryByIfIndex(ifIndex);
        return entry == null ? null : entry.getIpAdEntAddr();
    }
    
    /**
     * <p>getNetMask</p>
     *
     * @param ifIndex a int.
     * @return a {@link java.net.InetAddress} object.
     */
    public InetAddress getNetMask(final int ifIndex) {
        IpAddrTableEntry entry = getEntryByIfIndex(ifIndex);
        return entry == null ? null : entry.getIpAdEntNetMask();
        
    }
    
    /**
     * <p>getNetMask</p>
     *
     * @param address a {@link java.net.InetAddress} object.
     * @return a {@link java.net.InetAddress} object.
     */
    public InetAddress getNetMask(final InetAddress address) {
        return getEntry(address) == null ? null : getEntry(address).getIpAdEntNetMask();
    }
    
    /**
     * <p>getIfIndex</p>
     *
     * @param address a {@link java.net.InetAddress} object.
     * @return a {@link java.lang.Integer} object.
     */
    public Integer getIfIndex(InetAddress address) {
        return getEntry(address) == null ? null : getEntry(address).getIpAdEntIfIndex();
    }


    /**
     * <p>getEntryByIfIndex</p>
     *
     * @param ifIndex a int.
     * @return a {@link org.opennms.netmgt.provision.service.snmp.IpAddrTableEntry} object.
     */
    public IpAddrTableEntry getEntryByIfIndex(int ifIndex) {
        if (getEntries() == null) {
            return null;
        }
        
        for(IpAddrTableEntry entry : getEntries()) {
            Integer ndx = entry.getIpAdEntIfIndex();
            if (ndx != null && ndx.intValue() == ifIndex) {
                return entry;
            }
        }
        return null;
    }
     
    /**
     * <p>getEntry</p>
     *
     * @param address a {@link java.net.InetAddress} object.
     * @return a {@link org.opennms.netmgt.provision.service.snmp.IpAddrTableEntry} object.
     */
    public IpAddrTableEntry getEntry(InetAddress address) {
        return getEntry(new SnmpInstId(InetAddressUtils.str(address)));
    }

    /**
     * <p>updateIpInterfaceData</p>
     *
     * @param node a {@link org.opennms.netmgt.model.OnmsNode} object.
     */
    public void updateIpInterfaceData(OnmsNode node) {
        for(IpAddrTableEntry entry : getEntries()) {
            updateIpInterfaceData(node, entry.getIpAdEntAddr());
        }
    }

    /**
     * <p>updateIpInterfaceData</p>
     *
     * @param node a {@link org.opennms.netmgt.model.OnmsNode} object.
     * @param ipAddr a {@link InetAddress} object.
     */
    public boolean updateIpInterfaceData(OnmsNode node, InetAddress ipAddr) {
        boolean newIpInterfaceCrated = false;
        OnmsIpInterface ipIf = node.getIpInterfaceByIpAddress(ipAddr);
        if (ipIf == null) {
            ipIf = new OnmsIpInterface(ipAddr, node);
            newIpInterfaceCrated = true;
        }

        InetAddress inetAddr = ipIf.getIpAddress();
        Integer ifIndex = getIfIndex(inetAddr);

        // if we've found an ifIndex for this interface
        if (ifIndex != null) {

            // first look to see if an snmpIf was created already
            OnmsSnmpInterface snmpIf = node.getSnmpInterfaceWithIfIndex(ifIndex);

            if (snmpIf == null) {
                // if not then create one
                snmpIf = new OnmsSnmpInterface(node, ifIndex);
            }

            final InetAddress mask = getNetMask(inetAddr);
            if (mask != null) {
                ipIf.setNetMask(mask);
            }

            snmpIf.setCollectionEnabled(true);
            
            ipIf.setSnmpInterface(snmpIf);

        }
        return newIpInterfaceCrated;
    }

    /**
     * <p>getIpAddresses</p>
     *
     * @return a {@link java.util.Set} object.
     */
    public Set<String> getIpAddresses() {
        Set<String> ipAddrs = new LinkedHashSet<>();
        for(SnmpInstId inst : getInstances()) {
            ipAddrs.add(inst.toString());
        }
        return ipAddrs;
        
    }

}
