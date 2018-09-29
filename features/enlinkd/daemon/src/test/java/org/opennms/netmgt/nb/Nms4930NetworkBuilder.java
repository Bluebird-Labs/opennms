/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2013-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.nb;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.opennms.netmgt.dao.api.NodeDao;
import org.opennms.netmgt.enlinkd.model.IpNetToMedia;
import org.opennms.netmgt.enlinkd.model.IpNetToMedia.IpNetToMediaType;
import org.opennms.netmgt.enlinkd.persistence.api.IpNetToMediaDao;
import org.opennms.netmgt.model.NetworkBuilder;
import org.opennms.netmgt.model.OnmsNode.NodeType;

public class Nms4930NetworkBuilder extends NmsNetworkBuilder {

	NodeDao m_nodeDao;
	IpNetToMediaDao m_ipNetToMediaDao;

        public void addMacNodeWithSnmpInterface(String mac, String ip, Integer ifindex) {
            NetworkBuilder nb = getNetworkBuilder();
            nb.addNode(ip).setForeignSource("linkd").setForeignId(ip).setType(NodeType.ACTIVE);
            nb.addInterface(ip).setIsSnmpPrimary("N").setIsManaged("M")
            .addSnmpInterface(ifindex).setIfName("eth0").setIfType(6).setPhysAddr(mac).setIfDescr("eth0");
            m_nodeDao.save(nb.getCurrentNode());
            m_nodeDao.flush();

            IpNetToMedia at0 = new IpNetToMedia();
            at0.setSourceIfIndex(100);
            at0.setPhysAddress(mac);
            at0.setLastPollTime(at0.getCreateTime());
            at0.setSourceNode(m_nodeDao.findByForeignId("linkd", ip));
            try {
                at0.setNetAddress(InetAddress.getByName(ip));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            at0.setIpNetToMediaType(IpNetToMediaType.IPNETTOMEDIA_TYPE_DYNAMIC);
            m_ipNetToMediaDao.saveOrUpdate(at0);
            m_ipNetToMediaDao.flush();

        }
        
        public void addMacNode(String mac, String ip) {
            NetworkBuilder nb = getNetworkBuilder();
            nb.addNode(ip).setForeignSource("linkd").setForeignId(ip).setType(NodeType.ACTIVE);
            nb.addInterface(ip).setIsSnmpPrimary("N").setIsManaged("M");
            m_nodeDao.save(nb.getCurrentNode());
            m_nodeDao.flush();

            IpNetToMedia at0 = new IpNetToMedia();
            at0.setSourceIfIndex(100);
            at0.setPhysAddress(mac);
            at0.setLastPollTime(at0.getCreateTime());
            at0.setSourceNode(m_nodeDao.findByForeignId("linkd", ip));
            try {
                at0.setNetAddress(InetAddress.getByName(ip));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            at0.setIpNetToMediaType(IpNetToMediaType.IPNETTOMEDIA_TYPE_DYNAMIC);
            m_ipNetToMediaDao.saveOrUpdate(at0);
            m_ipNetToMediaDao.flush();

        }
	
    @SuppressWarnings("deprecation")
	public void buildNetwork4930() {
        NetworkBuilder nb = getNetworkBuilder();
        
        nb.addNode(DLINK1_NAME).setForeignSource("linkd").setForeignId(DLINK1_NAME).setSysObjectId(".1.3.6.1.4.1.9.1.122").setType(NodeType.ACTIVE);
        nb.addInterface(DLINK1_IP).setIsSnmpPrimary("P").setIsManaged("M")
        .addSnmpInterface(3).setIfType(6).setCollectionEnabled(true).setIfSpeed(100000000).setPhysAddr("c2007db90010");
        nb.addInterface("10.1.2.1").setIsSnmpPrimary("S").setIsManaged("M")
        .addSnmpInterface(1).setIfType(6).setCollectionEnabled(true).setIfSpeed(100000000).setPhysAddr("c2007db90000");
        nb.addInterface("10.1.3.1").setIsSnmpPrimary("S").setIsManaged("M")
        .addSnmpInterface(2).setIfType(6).setCollectionEnabled(true).setIfSpeed(100000000).setPhysAddr("c2007db90001");
        nb.addSnmpInterface(24).setIfType(6).setIfName("Fa0/24").setIfSpeed(100000000);
        m_nodeDao.save(nb.getCurrentNode());

        nb.addNode(DLINK2_NAME).setForeignSource("linkd").setForeignId(DLINK2_NAME).setSysObjectId(".1.3.6.1.4.1.9.1.122").setType(NodeType.ACTIVE);
        nb.addInterface(DLINK2_IP).setIsSnmpPrimary("P").setIsManaged("M")
        .addSnmpInterface(1).setIfType(6).setCollectionEnabled(true).setIfSpeed(100000000).setPhysAddr("c2017db90000");
        nb.addInterface("10.1.5.1").setIsSnmpPrimary("S").setIsManaged("M")
        .addSnmpInterface(2).setIfType(6).setCollectionEnabled(true).setIfSpeed(100000000).setPhysAddr("c2017db90001");
        nb.addSnmpInterface(10).setIfType(6).setIfName("FastEthernet0/10").setIfSpeed(100000000);
        m_nodeDao.save(nb.getCurrentNode());
        m_nodeDao.flush();
    }
    

    
	public NodeDao getNodeDao() {
		return m_nodeDao;
	}

	public void setNodeDao(NodeDao nodeDao) {
		m_nodeDao = nodeDao;
	}
	
       public IpNetToMediaDao getIpNetToMediaDao() {
	           return m_ipNetToMediaDao;
	       }

	       public void setIpNetToMediaDao(IpNetToMediaDao ipNetToMediaDao) {
	           m_ipNetToMediaDao = ipNetToMediaDao;
	       }

	
}
