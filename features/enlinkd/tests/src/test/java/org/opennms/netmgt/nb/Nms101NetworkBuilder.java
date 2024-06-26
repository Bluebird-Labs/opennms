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
package org.opennms.netmgt.nb;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.model.OnmsNode;

public class Nms101NetworkBuilder extends NmsNetworkBuilder {

	//Nms101

	public static final String LAPTOP_IP = "10.1.1.2";
	public static final String LAPTOP_NAME = "laptop";
	public static final String LAPTOP_SYSOID = ".1.3.6.1.4.1.8072.3.2.255";

	public static final Map<InetAddress,Integer> LAPTOP_IP_IF_MAP =  new HashMap<>();
	public static final Map<Integer,String> LAPTOP_IF_IFNAME_MAP = new HashMap<>();
	public static final Map<Integer,String> LAPTOP_IF_IFDESCR_MAP = new HashMap<>();
	public static final Map<Integer,String> LAPTOP_IF_MAC_MAP = new HashMap<>();
	public static final Map<Integer,String> LAPTOP_IF_IFALIAS_MAP = new HashMap<>();
	public static final Map<InetAddress,InetAddress> LAPTOP_IP_MK_MAP = new HashMap<>();

	public static final String CISCO7200A_IP = "10.1.1.1";
	public static final String CISCO7200A_NAME = "cisco7200ATM.befunk.com";
	public static final String CISCO7200A_SYSOID = ".1.3.6.1.4.1.9.1.222";

	public static final Map<InetAddress,Integer> CISCO7200A_IP_IF_MAP =  new HashMap<>();
	public static final Map<Integer,String> CISCO7200A_IF_IFNAME_MAP = new HashMap<>();
	public static final Map<Integer,String> CISCO7200A_IF_IFDESCR_MAP = new HashMap<>();
	public static final Map<Integer,String> CISCO7200A_IF_MAC_MAP = new HashMap<>();
	public static final Map<Integer,String> CISCO7200A_IF_IFALIAS_MAP = new HashMap<>();
	public static final Map<InetAddress,InetAddress> CISCO7200A_IP_MK_MAP = new HashMap<>();

	public static final String CISCO7200B_IP = "10.1.2.2";
	public static final String CISCO7200B_NAME = "cisco7200";
	public static final String CISCO7200B_SYSOID = ".1.3.6.1.4.1.9.1.222";

	public static final Map<InetAddress,Integer> CISCO7200B_IP_IF_MAP =  new HashMap<>();
	public static final Map<Integer,String> CISCO7200B_IF_IFNAME_MAP = new HashMap<>();
	public static final Map<Integer,String> CISCO7200B_IF_IFDESCR_MAP = new HashMap<>();
	public static final Map<Integer,String> CISCO7200B_IF_MAC_MAP = new HashMap<>();
	public static final Map<Integer,String> CISCO7200B_IF_IFALIAS_MAP = new HashMap<>();
	public static final Map<InetAddress,InetAddress> CISCO7200B_IP_MK_MAP = new HashMap<>();

	public static final String CISCO3700_IP = "10.1.3.2";
	public static final String CISCO3700_NAME = "cisco3700";
	public static final String CISCO3700_SYSOID = ".1.3.6.1.4.1.9.1.122";

	public static final Map<InetAddress,Integer> CISCO3700_IP_IF_MAP =  new HashMap<>();
	public static final Map<Integer,String> CISCO3700_IF_IFNAME_MAP = new HashMap<>();
	public static final Map<Integer,String> CISCO3700_IF_IFDESCR_MAP = new HashMap<>();
	public static final Map<Integer,String> CISCO3700_IF_MAC_MAP = new HashMap<>();
	public static final Map<Integer,String> CISCO3700_IF_IFALIAS_MAP = new HashMap<>();
	public static final Map<InetAddress,InetAddress> CISCO3700_IP_MK_MAP = new HashMap<>();

	public static final String CISCO2691_IP = "10.1.4.2";
	public static final String CISCO2691_NAME = "cisco2691";
	public static final String CISCO2691_SYSOID = ".1.3.6.1.4.1.9.1.122";

	public static final Map<InetAddress,Integer> CISCO2691_IP_IF_MAP =  new HashMap<>();
	public static final Map<Integer,String> CISCO2691_IF_IFNAME_MAP = new HashMap<>();
	public static final Map<Integer,String> CISCO2691_IF_IFDESCR_MAP = new HashMap<>();
	public static final Map<Integer,String> CISCO2691_IF_MAC_MAP = new HashMap<>();
	public static final Map<Integer,String> CISCO2691_IF_IFALIAS_MAP = new HashMap<>();
	public static final Map<InetAddress,InetAddress> CISCO2691_IP_MK_MAP = new HashMap<>();

	public static final String CISCO1700B_IP = "10.1.5.1";
	public static final String CISCO1700B_NAME = "cisco1700b";
	public static final String CISCO1700B_SYSOID = ".1.3.6.1.4.1.9.1.200";

	public static final Map<InetAddress,Integer> CISCO1700B_IP_IF_MAP =  new HashMap<>();
	public static final Map<Integer,String> CISCO1700B_IF_IFNAME_MAP = new HashMap<>();
	public static final Map<Integer,String> CISCO1700B_IF_IFDESCR_MAP = new HashMap<>();
	public static final Map<Integer,String> CISCO1700B_IF_MAC_MAP = new HashMap<>();
	public static final Map<Integer,String> CISCO1700B_IF_IFALIAS_MAP = new HashMap<>();
	public static final Map<InetAddress,InetAddress> CISCO1700B_IP_MK_MAP = new HashMap<>();

	public static final String CISCO3600_IP = "10.1.6.2";
	public static final String CISCO3600_NAME = "cisco3600";
	public static final String CISCO3600_SYSOID = ".1.3.6.1.4.1.9.1.122";

	public static final Map<InetAddress,Integer> CISCO3600_IP_IF_MAP =  new HashMap<>();
	public static final Map<Integer,String> CISCO3600_IF_IFNAME_MAP = new HashMap<>();
	public static final Map<Integer,String> CISCO3600_IF_IFDESCR_MAP = new HashMap<>();
	public static final Map<Integer,String> CISCO3600_IF_MAC_MAP = new HashMap<>();
	public static final Map<Integer,String> CISCO3600_IF_IFALIAS_MAP = new HashMap<>();
	public static final Map<InetAddress,InetAddress> CISCO3600_IP_MK_MAP = new HashMap<>();

	public static final String CISCO1700_IP = "10.1.5.2";
	public static final String CISCO1700_NAME = "cisco1700";
	public static final String CISCO1700_SYSOID = ".1.3.6.1.4.1.9.1.200";

	public static final Map<InetAddress,Integer> CISCO1700_IP_IF_MAP =  new HashMap<>();
	public static final Map<Integer,String> CISCO1700_IF_IFNAME_MAP = new HashMap<>();
	public static final Map<Integer,String> CISCO1700_IF_IFDESCR_MAP = new HashMap<>();
	public static final Map<Integer,String> CISCO1700_IF_MAC_MAP = new HashMap<>();
	public static final Map<Integer,String> CISCO1700_IF_IFALIAS_MAP = new HashMap<>();
	public static final Map<InetAddress,InetAddress> CISCO1700_IP_MK_MAP = new HashMap<>();

	public static final String LAPTOP_SNMP_RESOURCE = "classpath:/linkd/nms101/laptop.properties";
	public static final String CISCO7200A_SNMP_RESOURCE = "classpath:/linkd/nms101/cisco7200a.properties";
	public static final String CISCO7200B_SNMP_RESOURCE = "classpath:/linkd/nms101/cisco7200b.properties";
	public static final String CISCO3700_SNMP_RESOURCE = "classpath:/linkd/nms101/cisco3700.properties";
	public static final String CISCO2691_SNMP_RESOURCE = "classpath:/linkd/nms101/cisco2691.properties";
	public static final String CISCO1700B_SNMP_RESOURCE = "classpath:/linkd/nms101/cisco1700b.properties";
	public static final String CISCO3600_SNMP_RESOURCE = "classpath:/linkd/nms101/cisco3600.properties";
	public static final String CISCO1700_SNMP_RESOURCE = "classpath:/linkd/nms101/cisco1700.properties";

	public static final String EXAMPLECOM_IP = "192.168.1.10";
	public static final String EXAMPLECOM_NAME = "test.example.com";
	public static final String EXAMPLECOM_SYSOID = ".1.3.6.1.4.1.1724.81";

	public static final Map<InetAddress,Integer> EXAMPLECOM_IP_IF_MAP =  new HashMap<>();
	public static final Map<Integer,String> EXAMPLECOM_IF_IFNAME_MAP = new HashMap<>();
	public static final Map<Integer,String> EXAMPLECOM_IF_IFDESCR_MAP = new HashMap<>();
	public static final Map<Integer,String> EXAMPLECOM_IF_MAC_MAP = new HashMap<>();
	public static final Map<Integer,String> EXAMPLECOM_IF_IFALIAS_MAP = new HashMap<>();
	public static final Map<InetAddress,InetAddress> EXAMPLECOM_IP_MK_MAP = new HashMap<>();
    static {
		EXAMPLECOM_IP_IF_MAP.put(InetAddressUtils.addr(EXAMPLECOM_IP),-1);
		LAPTOP_IF_IFNAME_MAP.put(1,"lo0");
		LAPTOP_IF_IFDESCR_MAP.put(1,"lo0");
		LAPTOP_IP_IF_MAP.put(InetAddressUtils.addr("127.0.0.1"), 1);
    	LAPTOP_IP_IF_MAP.put(InetAddressUtils.addr("172.16.8.1"), 8);
    	LAPTOP_IP_IF_MAP.put(InetAddressUtils.addr("172.16.190.1"), 9);
    	LAPTOP_IP_IF_MAP.put(InetAddressUtils.addr("10.1.1.2"), 10);
    	LAPTOP_IP_IF_MAP.put(InetAddressUtils.addr("192.0.2.182"), 6);

		LAPTOP_IP_MK_MAP.put(InetAddressUtils.addr("10.1.1.2"), InetAddressUtils.addr("255.255.255.0"));
		LAPTOP_IP_MK_MAP.put(InetAddressUtils.addr("127.0.0.1"), InetAddressUtils.addr("255.0.0.0"));
		LAPTOP_IP_MK_MAP.put(InetAddressUtils.addr("172.16.8.1"), InetAddressUtils.addr("255.255.255.0"));
		LAPTOP_IP_MK_MAP.put(InetAddressUtils.addr("172.16.190.1"), InetAddressUtils.addr("255.255.255.0"));
		LAPTOP_IP_MK_MAP.put(InetAddressUtils.addr("192.0.2.182"), InetAddressUtils.addr("255.255.255.0"));
    	LAPTOP_IF_IFNAME_MAP.put(5, "fw0");
    	LAPTOP_IF_IFDESCR_MAP.put(5, "fw0");
    	LAPTOP_IF_IFNAME_MAP.put(7, "utun0");
    	LAPTOP_IF_IFDESCR_MAP.put(7, "utun0");
    	LAPTOP_IF_IFNAME_MAP.put(9, "vmnet8");
    	LAPTOP_IF_IFDESCR_MAP.put(9, "vmnet8");
    	LAPTOP_IF_MAC_MAP.put(9, "005056c00008");
    	LAPTOP_IF_IFNAME_MAP.put(8, "vmnet1");
    	LAPTOP_IF_IFDESCR_MAP.put(8, "vmnet1");
    	LAPTOP_IF_MAC_MAP.put(8, "005056c00001");
    	LAPTOP_IF_IFNAME_MAP.put(4, "en0");
    	LAPTOP_IF_IFDESCR_MAP.put(4, "en0");
    	LAPTOP_IF_MAC_MAP.put(4, "00b035eececd");
    	LAPTOP_IF_IFNAME_MAP.put(6, "en1");
    	LAPTOP_IF_IFDESCR_MAP.put(6, "en1");
    	LAPTOP_IF_MAC_MAP.put(6, "00b0356a91c7");
    	LAPTOP_IF_IFNAME_MAP.put(10, "tap0");
    	LAPTOP_IF_IFDESCR_MAP.put(10, "tap0");
    	LAPTOP_IF_MAC_MAP.put(10, "005568ae696c");
    	LAPTOP_IF_IFNAME_MAP.put(2, "gif0");
    	LAPTOP_IF_IFDESCR_MAP.put(2, "gif0");
    	LAPTOP_IF_IFNAME_MAP.put(3, "stf0");
    	LAPTOP_IF_IFDESCR_MAP.put(3, "stf0");

    	CISCO7200A_IP_IF_MAP.put(InetAddressUtils.addr("10.1.1.1"), 3);
    	CISCO7200A_IP_IF_MAP.put(InetAddressUtils.addr("10.1.2.1"), 2);
		CISCO7200A_IP_MK_MAP.put(InetAddressUtils.addr("10.1.1.1"), InetAddressUtils.addr("255.255.255.0"));
		CISCO7200A_IP_MK_MAP.put(InetAddressUtils.addr("10.1.2.1"), InetAddressUtils.addr("255.255.255.0"));
		CISCO7200A_IF_IFNAME_MAP.put(1, "Fa0/0");
    	CISCO7200A_IF_IFDESCR_MAP.put(1, "FastEthernet0/0");
    	CISCO7200A_IF_MAC_MAP.put(1, "ca0497a80000");
    	CISCO7200A_IF_IFNAME_MAP.put(8, "Nu0");
    	CISCO7200A_IF_IFDESCR_MAP.put(8, "Null0");
    	CISCO7200A_IF_IFNAME_MAP.put(2, "Gi1/0");
    	CISCO7200A_IF_IFDESCR_MAP.put(2, "GigabitEthernet1/0");
    	CISCO7200A_IF_MAC_MAP.put(2, "ca0497a8001c");
    	CISCO7200A_IF_IFNAME_MAP.put(4, "Gi3/0");
    	CISCO7200A_IF_IFDESCR_MAP.put(4, "GigabitEthernet3/0");
    	CISCO7200A_IF_MAC_MAP.put(4, "ca0497a80054");
    	CISCO7200A_IF_IFNAME_MAP.put(3, "Gi2/0");
    	CISCO7200A_IF_IFDESCR_MAP.put(3, "GigabitEthernet2/0");
    	CISCO7200A_IF_MAC_MAP.put(3, "ca0497a80038");
    	CISCO7200A_IF_IFNAME_MAP.put(7, "SS0");
    	CISCO7200A_IF_IFDESCR_MAP.put(7, "SSLVPN-VIF0");

    	CISCO7200B_IP_IF_MAP.put(InetAddressUtils.addr("10.1.3.1"), 2);
    	CISCO7200B_IP_IF_MAP.put(InetAddressUtils.addr("10.1.2.2"), 4);
    	CISCO7200B_IP_IF_MAP.put(InetAddressUtils.addr("10.1.4.1"), 1);
		CISCO7200B_IP_MK_MAP.put(InetAddressUtils.addr("10.1.2.2"), InetAddressUtils.addr("255.255.255.0"));
		CISCO7200B_IP_MK_MAP.put(InetAddressUtils.addr("10.1.3.1"), InetAddressUtils.addr("255.255.255.0"));
		CISCO7200B_IP_MK_MAP.put(InetAddressUtils.addr("10.1.4.1"), InetAddressUtils.addr("255.255.255.0"));

    	CISCO7200B_IF_IFNAME_MAP.put(7, "Se3/2");
    	CISCO7200B_IF_IFDESCR_MAP.put(7, "Serial3/2");
    	CISCO7200B_IF_IFNAME_MAP.put(10, "Et4/1");
    	CISCO7200B_IF_IFDESCR_MAP.put(10, "Ethernet4/1");
    	CISCO7200B_IF_MAC_MAP.put(10, "ca0597a80071");
    	CISCO7200B_IF_IFNAME_MAP.put(16, "Nu0");
    	CISCO7200B_IF_IFDESCR_MAP.put(16, "Null0");
    	CISCO7200B_IF_IFNAME_MAP.put(2, "Fa1/0");
    	CISCO7200B_IF_IFDESCR_MAP.put(2, "FastEthernet1/0");
    	CISCO7200B_IF_MAC_MAP.put(2, "ca0597a8001c");
    	CISCO7200B_IF_IFNAME_MAP.put(8, "Se3/3");
    	CISCO7200B_IF_IFDESCR_MAP.put(8, "Serial3/3");
    	CISCO7200B_IF_IFNAME_MAP.put(3, "Fa1/1");
    	CISCO7200B_IF_IFDESCR_MAP.put(3, "FastEthernet1/1");
    	CISCO7200B_IF_MAC_MAP.put(3, "ca0597a8001d");
    	CISCO7200B_IF_IFNAME_MAP.put(5, "Se3/0");
    	CISCO7200B_IF_IFDESCR_MAP.put(5, "Serial3/0");
    	CISCO7200B_IF_IFNAME_MAP.put(12, "Et4/3");
    	CISCO7200B_IF_IFDESCR_MAP.put(12, "Ethernet4/3");
    	CISCO7200B_IF_MAC_MAP.put(12, "ca0597a80073");
    	CISCO7200B_IF_IFNAME_MAP.put(11, "Et4/2");
    	CISCO7200B_IF_IFDESCR_MAP.put(11, "Ethernet4/2");
    	CISCO7200B_IF_MAC_MAP.put(11, "ca0597a80072");
    	CISCO7200B_IF_IFNAME_MAP.put(9, "Et4/0");
    	CISCO7200B_IF_IFDESCR_MAP.put(9, "Ethernet4/0");
    	CISCO7200B_IF_MAC_MAP.put(9, "ca0597a80070");
    	CISCO7200B_IF_IFNAME_MAP.put(4, "Gi2/0");
    	CISCO7200B_IF_IFDESCR_MAP.put(4, "GigabitEthernet2/0");
    	CISCO7200B_IF_MAC_MAP.put(4, "ca0597a80038");
    	CISCO7200B_IF_IFNAME_MAP.put(15, "SS0");
    	CISCO7200B_IF_IFDESCR_MAP.put(15, "SSLVPN-VIF0");
    	CISCO7200B_IF_IFNAME_MAP.put(6, "Se3/1");
    	CISCO7200B_IF_IFDESCR_MAP.put(6, "Serial3/1");
    	CISCO7200B_IF_IFNAME_MAP.put(1, "Fa0/0");
    	CISCO7200B_IF_IFDESCR_MAP.put(1, "FastEthernet0/0");
    	CISCO7200B_IF_MAC_MAP.put(1, "ca0597a80000");

    	CISCO3700_IP_IF_MAP.put(InetAddressUtils.addr("10.1.3.2"), 1);
    	CISCO3700_IP_IF_MAP.put(InetAddressUtils.addr("10.1.6.1"), 3);
		CISCO3700_IP_MK_MAP.put(InetAddressUtils.addr("10.1.3.2"), InetAddressUtils.addr("255.255.255.0"));
		CISCO3700_IP_MK_MAP.put(InetAddressUtils.addr("10.1.6.1"), InetAddressUtils.addr("255.255.255.0"));
		CISCO3700_IF_IFNAME_MAP.put(2, "Se0/0");
    	CISCO3700_IF_IFDESCR_MAP.put(2, "Serial0/0");
    	CISCO3700_IF_IFNAME_MAP.put(3, "Fa0/1");
    	CISCO3700_IF_IFDESCR_MAP.put(3, "FastEthernet0/1");
    	CISCO3700_IF_MAC_MAP.put(3, "c20197a50001");
    	CISCO3700_IF_IFNAME_MAP.put(1, "Fa0/0");
    	CISCO3700_IF_IFDESCR_MAP.put(1, "FastEthernet0/0");
    	CISCO3700_IF_MAC_MAP.put(1, "c20197a50000");
    	CISCO3700_IF_IFNAME_MAP.put(5, "Se0/2");
    	CISCO3700_IF_IFDESCR_MAP.put(5, "Serial0/2");
    	CISCO3700_IF_IFNAME_MAP.put(8, "Nu0");
    	CISCO3700_IF_IFDESCR_MAP.put(8, "Null0");
    	CISCO3700_IF_IFNAME_MAP.put(4, "Se0/1");
    	CISCO3700_IF_IFDESCR_MAP.put(4, "Serial0/1");
    	CISCO3700_IF_IFNAME_MAP.put(6, "Se0/3");
    	CISCO3700_IF_IFDESCR_MAP.put(6, "Serial0/3");

    	CISCO2691_IP_IF_MAP.put(InetAddressUtils.addr("10.1.7.1"), 1);
    	CISCO2691_IP_IF_MAP.put(InetAddressUtils.addr("10.1.5.1"), 2);
    	CISCO2691_IP_IF_MAP.put(InetAddressUtils.addr("10.1.4.2"), 4);
		CISCO2691_IP_MK_MAP.put(InetAddressUtils.addr("10.1.4.2"), InetAddressUtils.addr("255.255.255.0"));
		CISCO2691_IP_MK_MAP.put(InetAddressUtils.addr("10.1.5.1"), InetAddressUtils.addr("255.255.255.0"));
		CISCO2691_IP_MK_MAP.put(InetAddressUtils.addr("10.1.7.1"), InetAddressUtils.addr("255.255.255.0"));
		CISCO2691_IF_IFNAME_MAP.put(2, "Fa0/0");
    	CISCO2691_IF_IFDESCR_MAP.put(2, "FastEthernet0/0");
    	CISCO2691_IF_MAC_MAP.put(2, "c00397a70000");
    	CISCO2691_IF_IFNAME_MAP.put(3, "Se0/0");
    	CISCO2691_IF_IFDESCR_MAP.put(3, "Serial0/0");
    	CISCO2691_IF_IFNAME_MAP.put(1, "Fa1/0");
    	CISCO2691_IF_IFDESCR_MAP.put(1, "FastEthernet1/0");
    	CISCO2691_IF_MAC_MAP.put(1, "c00397a70010");
    	CISCO2691_IF_IFNAME_MAP.put(6, "Se0/2");
    	CISCO2691_IF_IFDESCR_MAP.put(6, "Serial0/2");
    	CISCO2691_IF_IFNAME_MAP.put(5, "Se0/1");
    	CISCO2691_IF_IFDESCR_MAP.put(5, "Serial0/1");
    	CISCO2691_IF_IFNAME_MAP.put(9, "Nu0");
    	CISCO2691_IF_IFDESCR_MAP.put(9, "Null0");
    	CISCO2691_IF_IFNAME_MAP.put(7, "Se0/3");
    	CISCO2691_IF_IFDESCR_MAP.put(7, "Serial0/3");
    	CISCO2691_IF_IFNAME_MAP.put(4, "Fa0/1");
    	CISCO2691_IF_IFDESCR_MAP.put(4, "FastEthernet0/1");
    	CISCO2691_IF_MAC_MAP.put(4, "c00397a70001");

    	CISCO1700_IP_IF_MAP.put(InetAddressUtils.addr("10.1.5.2"), 2);
		CISCO1700_IP_MK_MAP.put(InetAddressUtils.addr("10.1.5.2"), InetAddressUtils.addr("255.255.255.0"));
		CISCO1700_IF_IFNAME_MAP.put(2, "Fa0");
    	CISCO1700_IF_IFDESCR_MAP.put(2, "FastEthernet0");
    	CISCO1700_IF_MAC_MAP.put(2, "d00297a60000");
    	CISCO1700_IF_IFNAME_MAP.put(1, "Et0");
    	CISCO1700_IF_IFDESCR_MAP.put(1, "Ethernet0");
    	CISCO1700_IF_MAC_MAP.put(1, "d00297a60001");
    	CISCO1700_IF_IFNAME_MAP.put(4, "Nu0");
    	CISCO1700_IF_IFDESCR_MAP.put(4, "Null0");

		CISCO3600_IP_IF_MAP.put(InetAddressUtils.addr("10.1.6.2"), 1);
    	CISCO3600_IP_IF_MAP.put(InetAddressUtils.addr("10.1.7.2"), 2);
		CISCO3600_IP_MK_MAP.put(InetAddressUtils.addr("10.1.6.2"), InetAddressUtils.addr("255.255.255.0"));
		CISCO3600_IP_MK_MAP.put(InetAddressUtils.addr("10.1.7.2"), InetAddressUtils.addr("255.255.255.0"));
    	CISCO3600_IF_IFNAME_MAP.put(1, "Fa0/0");
    	CISCO3600_IF_IFDESCR_MAP.put(1, "FastEthernet0/0");
    	CISCO3600_IF_MAC_MAP.put(1, "cc0097a30000");
    	CISCO3600_IF_IFNAME_MAP.put(2, "Fa1/0");
    	CISCO3600_IF_IFDESCR_MAP.put(2, "FastEthernet1/0");
    	CISCO3600_IF_MAC_MAP.put(2, "cc0097a30010");
    	CISCO3600_IF_IFNAME_MAP.put(4, "Nu0");
    	CISCO3600_IF_IFDESCR_MAP.put(4, "Null0");

		CISCO1700B_IP_IF_MAP.put(InetAddressUtils.addr("10.1.5.1"), 2);
		CISCO1700B_IP_MK_MAP.put(InetAddressUtils.addr("10.1.5.1"), InetAddressUtils.addr("255.255.255.0"));
		CISCO1700B_IF_IFNAME_MAP.put(2, "Fa0");
    	CISCO1700B_IF_IFDESCR_MAP.put(2, "FastEthernet0");
    	CISCO1700B_IF_MAC_MAP.put(2, "d00297a60000");
    	CISCO1700B_IF_IFNAME_MAP.put(4, "Nu0");
    	CISCO1700B_IF_IFDESCR_MAP.put(4, "Null0");
    	CISCO1700B_IF_IFNAME_MAP.put(1, "Et0");
    	CISCO1700B_IF_IFDESCR_MAP.put(1, "Ethernet0");
    	CISCO1700B_IF_MAC_MAP.put(1, "d00297a60001");
    }
    
    public OnmsNode getLaptop() {
        return getNode(LAPTOP_NAME,LAPTOP_SYSOID,LAPTOP_IP,LAPTOP_IP_IF_MAP,LAPTOP_IF_IFNAME_MAP,LAPTOP_IF_MAC_MAP,LAPTOP_IF_IFDESCR_MAP,LAPTOP_IF_IFALIAS_MAP,LAPTOP_IP_MK_MAP);        
    }

    public OnmsNode getCisco7200a() {
        return getNode(CISCO7200A_NAME,CISCO7200A_SYSOID,CISCO7200A_IP,CISCO7200A_IP_IF_MAP,CISCO7200A_IF_IFNAME_MAP,CISCO7200A_IF_MAC_MAP,CISCO7200A_IF_IFDESCR_MAP,CISCO7200A_IF_IFALIAS_MAP,CISCO7200A_IP_MK_MAP);        
    }

    public OnmsNode getCisco7200b() {
        return getNode(CISCO7200B_NAME,CISCO7200B_SYSOID,CISCO7200B_IP,CISCO7200B_IP_IF_MAP,CISCO7200B_IF_IFNAME_MAP,CISCO7200B_IF_MAC_MAP,CISCO7200B_IF_IFDESCR_MAP,CISCO7200B_IF_IFALIAS_MAP,CISCO7200B_IP_MK_MAP);        
    }

    public OnmsNode getCisco3700() {
        return getNode(CISCO3700_NAME,CISCO3700_SYSOID,CISCO3700_IP,CISCO3700_IP_IF_MAP,CISCO3700_IF_IFNAME_MAP,CISCO3700_IF_MAC_MAP,CISCO3700_IF_IFDESCR_MAP,CISCO3700_IF_IFALIAS_MAP,CISCO3700_IP_MK_MAP);        
    }

    public OnmsNode getCisco2691() {
        return getNode(CISCO2691_NAME,CISCO2691_SYSOID,CISCO2691_IP,CISCO2691_IP_IF_MAP,CISCO2691_IF_IFNAME_MAP,CISCO2691_IF_MAC_MAP,CISCO2691_IF_IFDESCR_MAP,CISCO2691_IF_IFALIAS_MAP,CISCO2691_IP_MK_MAP);        
    }

    public OnmsNode getCisco1700() {
        return getNode(CISCO1700_NAME,CISCO1700_SYSOID,CISCO1700_IP,CISCO1700_IP_IF_MAP,CISCO1700_IF_IFNAME_MAP,CISCO1700_IF_MAC_MAP,CISCO1700_IF_IFDESCR_MAP,CISCO1700_IF_IFALIAS_MAP,CISCO1700_IP_MK_MAP);        
    }

    public OnmsNode getCisco3600() {
        return getNode(CISCO3600_NAME,CISCO3600_SYSOID,CISCO3600_IP,CISCO3600_IP_IF_MAP,CISCO3600_IF_IFNAME_MAP,CISCO3600_IF_MAC_MAP,CISCO3600_IF_IFDESCR_MAP,CISCO3600_IF_IFALIAS_MAP,CISCO3600_IP_MK_MAP);        
    }

    public OnmsNode getCisco1700b() {
        return getNode(CISCO1700B_NAME,CISCO1700B_SYSOID,CISCO1700B_IP,CISCO1700B_IP_IF_MAP,CISCO1700B_IF_IFNAME_MAP,CISCO1700B_IF_MAC_MAP,CISCO1700B_IF_IFDESCR_MAP,CISCO1700B_IF_IFALIAS_MAP,CISCO1700B_IP_MK_MAP);        
    }

    public OnmsNode getExampleCom() {
        return getNode(EXAMPLECOM_NAME,EXAMPLECOM_SYSOID,EXAMPLECOM_IP,EXAMPLECOM_IP_IF_MAP,EXAMPLECOM_IF_IFNAME_MAP,EXAMPLECOM_IF_MAC_MAP,EXAMPLECOM_IF_IFDESCR_MAP,EXAMPLECOM_IF_IFALIAS_MAP,EXAMPLECOM_IP_MK_MAP);        
    }
}
