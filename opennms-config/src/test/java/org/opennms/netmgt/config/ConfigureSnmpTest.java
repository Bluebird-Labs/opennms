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
package org.opennms.netmgt.config;

import java.net.UnknownHostException;

import junit.framework.TestCase;

import org.opennms.core.test.ConfigurationTestUtils;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.events.api.EventConstants;
import org.opennms.netmgt.events.api.model.ImmutableMapper;
import org.opennms.netmgt.model.events.EventBuilder;
import org.opennms.netmgt.snmp.SnmpAgentConfig;
import org.springframework.core.io.Resource;

/**
 * 
 * @author <a href="mailto:david@opennms.org">David Hustace</a>
 * @author <a href="mailto:dj@opennms.org">DJ Gregor</a>
 */
public class ConfigureSnmpTest extends TestCase {
    final private int m_startingDefCount = 5;

    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
    	super.setUp();
    	
        Resource rsrc = ConfigurationTestUtils.getSpringResourceForResource(this, "snmp-config-configureSnmpTest.xml");
    	SnmpPeerFactory.setInstance(new SnmpPeerFactory(rsrc));
    }

    /**
     * Tests creating a string representation of an IP address that is converted to an InetAddress and then
     * a long and back to an IP address.
     * 
     * @throws UnknownHostException 
     */
    public void testToIpAddrString() throws UnknownHostException {
        String addr = "192.168.1.1";
        assertEquals(addr, InetAddressUtils.toIpAddrString(InetAddressUtils.addr(addr).getAddress()));
    }

    /**
     * Test method for {@link org.opennms.netmgt.config.SnmpPeerFactory#createSnmpEventInfo(org.opennms.netmgt.events.api.model.IEvent)}.
     * Tests creating an SNMP config definition from a configureSNMP event.
     * 
     * @throws UnknownHostException 
     */
    public void testCreateSnmpEventInfo() throws UnknownHostException {
        EventBuilder bldr = createConfigureSnmpEventBuilder("192.168.1.1", null);
        addCommunityStringToEvent(bldr, "seemore");
        
        SnmpEventInfo info = new SnmpEventInfo(ImmutableMapper.fromMutableEvent(bldr.getEvent()));
        
        assertNotNull(info);
        assertEquals("192.168.1.1", info.getFirstIPAddress());
        assertNull(info.getLastIPAddress());
        assertTrue(info.isSpecific());
    }
    
    /**
     * Tests getting the correct SNMP Peer after a configureSNMP event and merge to the running config.
     * @throws UnknownHostException
     */
    public void testSnmpEventInfoClassWithSpecific() throws UnknownHostException {
        final String addr = "192.168.0.5";
        EventBuilder bldr = createConfigureSnmpEventBuilder(addr, null);
        addCommunityStringToEvent(bldr, "abc");
        SnmpEventInfo info = new SnmpEventInfo(ImmutableMapper.fromMutableEvent(bldr.getEvent()));
        
        SnmpPeerFactory.getInstance().define(info);

        SnmpAgentConfig agent = SnmpPeerFactory.getInstance().getAgentConfig(InetAddressUtils.addr(addr));
        assertEquals(InetAddressUtils.str(agent.getAddress()), addr);
        assertEquals("abc", agent.getReadCommunity());
    }
    
    /**
     * This test should remove the specific 192.168.0.5 from the first definition and
     * replace it with a range 192.168.0.5 - 192.168.0.7.
     * 
     * @throws UnknownHostException
     */
    public void testSnmpEventInfoClassWithRangeReplacingSpecific() throws UnknownHostException {
        final String addr1 = "192.168.0.5";
        final String addr2 = "192.168.0.7";
        
        SnmpAgentConfig agent = SnmpPeerFactory.getInstance().getAgentConfig(InetAddressUtils.addr(addr1));
        assertEquals(SnmpAgentConfig.VERSION2C, agent.getVersion());
        
        EventBuilder bldr = createConfigureSnmpEventBuilder(addr1, addr2);
        SnmpEventInfo info = new SnmpEventInfo(ImmutableMapper.fromMutableEvent(bldr.getEvent()));
        info.setVersion("v2c");
        
        SnmpPeerFactory.getInstance().define(info);
        
        agent = SnmpPeerFactory.getInstance().getAgentConfig(InetAddressUtils.addr(addr1));
        assertEquals(InetAddressUtils.str(agent.getAddress()), addr1);
        assertEquals(SnmpAgentConfig.VERSION2C, agent.getVersion());
        assertEquals(m_startingDefCount, SnmpPeerFactory.getInstance().getSnmpConfig().getDefinitions().size());
    }

    /**
     * Tests getting the correct SNMP Peer after merging a new range that super sets a current range.
     * 
     * @throws UnknownHostException
     */
    public void testSnmpEventInfoClassWithRangeSuperSettingDefRanges() throws UnknownHostException {
        final String addr1 = "192.168.99.1";
        final String addr2 = "192.168.108.254";
        
        SnmpAgentConfig agent = SnmpPeerFactory.getInstance().getAgentConfig(InetAddressUtils.addr(addr1));
        assertEquals(SnmpAgentConfig.VERSION1, agent.getVersion());
        
        EventBuilder bldr = createConfigureSnmpEventBuilder(addr1, addr2);
        SnmpEventInfo info = new SnmpEventInfo(ImmutableMapper.fromMutableEvent(bldr.getEvent()));
        info.setReadCommunityString("opennmsrules");
        
        SnmpPeerFactory.getInstance().define(info);
        
        agent = SnmpPeerFactory.getInstance().getAgentConfig(InetAddressUtils.addr(addr1));
        assertEquals(InetAddressUtils.str(agent.getAddress()), addr1);
        assertEquals(SnmpAgentConfig.VERSION1, agent.getVersion());
        assertEquals(m_startingDefCount, SnmpPeerFactory.getInstance().getSnmpConfig().getDefinitions().size());
    }

    /**
     * Tests getting the correct SNMP Peer after receiving a configureSNMP event that moves a
     * specific from one definition into another.
     * 
     * @throws UnknownHostException
     */
    public void testSplicingSpecificsIntoRanges() throws UnknownHostException {
        assertEquals(3, SnmpPeerFactory.getInstance().getSnmpConfig().getDefinitions().get(2).getRanges().size());
        assertEquals(6, SnmpPeerFactory.getInstance().getSnmpConfig().getDefinitions().get(2).getSpecifics().size());
        
        final String specificAddr = "10.1.1.7";
        final EventBuilder bldr = createConfigureSnmpEventBuilder(specificAddr, null);
        final SnmpEventInfo info = new SnmpEventInfo(ImmutableMapper.fromMutableEvent(bldr.getEvent()));
        info.setReadCommunityString("splice-test");
        info.setVersion("v2c");
        
        SnmpPeerFactory.getInstance().define(info);
        
        assertEquals(5, SnmpPeerFactory.getInstance().getSnmpConfig().getDefinitions().get(2).getRanges().size());
        
        assertEquals("10.1.1.10", SnmpPeerFactory.getInstance().getSnmpConfig().getDefinitions().get(2).getSpecifics().get(0));
        assertEquals(1, SnmpPeerFactory.getInstance().getSnmpConfig().getDefinitions().get(2).getSpecifics().size());
        assertEquals(m_startingDefCount, SnmpPeerFactory.getInstance().getSnmpConfig().getDefinitions().size());
    }
    
    /**
     * This test should show that a specific is added to the definition and the current
     * single definition should become the beginning address in the adjacent range.
     * 
     * @throws UnknownHostException
     */
    public void testSplice2() throws UnknownHostException {
        assertEquals(3, SnmpPeerFactory.getInstance().getSnmpConfig().getDefinitions().get(3).getRanges().size());
        assertEquals(1, SnmpPeerFactory.getInstance().getSnmpConfig().getDefinitions().get(3).getSpecifics().size());
        assertEquals("10.1.1.10", SnmpPeerFactory.getInstance().getSnmpConfig().getDefinitions().get(3).getSpecifics().get(0));
        assertEquals("10.1.1.11", SnmpPeerFactory.getInstance().getSnmpConfig().getDefinitions().get(3).getRanges().get(0).getBegin());
        
        final String specificAddr = "10.1.1.7";
        final EventBuilder bldr = createConfigureSnmpEventBuilder(specificAddr, null);
        final SnmpEventInfo info = new SnmpEventInfo(ImmutableMapper.fromMutableEvent(bldr.getEvent()));
        info.setReadCommunityString("splice2-test");

        SnmpPeerFactory.getInstance().define(info);
        
        assertEquals(3, SnmpPeerFactory.getInstance().getSnmpConfig().getDefinitions().get(3).getRanges().size());
        assertEquals(1, SnmpPeerFactory.getInstance().getSnmpConfig().getDefinitions().get(3).getSpecifics().size());
        assertEquals("10.1.1.7", SnmpPeerFactory.getInstance().getSnmpConfig().getDefinitions().get(3).getSpecifics().get(0));
        assertEquals("10.1.1.10", SnmpPeerFactory.getInstance().getSnmpConfig().getDefinitions().get(3).getRanges().get(0).getBegin());

        String marshalledConfig = SnmpPeerFactory.getInstance().getSnmpConfigAsString();
        assertNotNull(marshalledConfig);
        
    }

    private EventBuilder createConfigureSnmpEventBuilder(final String firstIp, final String lastIp) {
        
        EventBuilder bldr = new EventBuilder(EventConstants.CONFIGURE_SNMP_EVENT_UEI, "ConfigureSnmpTest");

        bldr.addParam(EventConstants.PARM_FIRST_IP_ADDRESS, firstIp);
        bldr.addParam(EventConstants.PARM_LAST_IP_ADDRESS, lastIp);
        
        return bldr;
    }

    private void addCommunityStringToEvent(final EventBuilder bldr, final String commStr) {
        bldr.addParam(EventConstants.PARM_SNMP_READ_COMMUNITY_STRING, commStr);
    }

}
