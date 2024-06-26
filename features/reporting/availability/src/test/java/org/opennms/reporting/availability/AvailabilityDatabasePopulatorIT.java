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
package org.opennms.reporting.availability;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.criteria.CriteriaBuilder;
import org.opennms.core.spring.BeanUtils;
import org.opennms.core.test.OpenNMSJUnit4ClassRunner;
import org.opennms.core.test.db.annotations.JUnitTemporaryDatabase;
import org.opennms.netmgt.dao.api.IpInterfaceDao;
import org.opennms.netmgt.dao.api.NodeDao;
import org.opennms.netmgt.dao.api.OutageDao;
import org.opennms.netmgt.dao.api.ServiceTypeDao;
import org.opennms.netmgt.model.OnmsIpInterface;
import org.opennms.netmgt.model.OnmsMonitoredService;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.model.OnmsOutage;
import org.opennms.test.JUnitConfigurationEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

@RunWith(OpenNMSJUnit4ClassRunner.class)
@ContextConfiguration(locations={
        "classpath:/META-INF/opennms/applicationContext-soa.xml",
        "classpath:/META-INF/opennms/applicationContext-dao.xml",
		"classpath:/META-INF/opennms/applicationContext-mockConfigManager.xml",
        "classpath*:/META-INF/opennms/component-dao.xml",
        "classpath:/META-INF/opennms/applicationContext-availabilityDatabasePopulator.xml",
        "classpath:/META-INF/opennms/applicationContext-minimal-conf.xml",
        "classpath:/META-INF/opennms/applicationContext-commonConfigs.xml",
        "classpath:/META-INF/opennms/applicationContext-minimal-conf.xml"
})
@JUnitConfigurationEnvironment
@JUnitTemporaryDatabase
public class AvailabilityDatabasePopulatorIT implements InitializingBean {
    private static final Logger LOG = LoggerFactory.getLogger(AvailabilityDatabasePopulatorIT.class);

	@Autowired
	AvailabilityDatabasePopulator m_dbPopulator;

	@Autowired
	NodeDao m_nodeDao;

	@Autowired
	ServiceTypeDao m_serviceTypeDao;

	@Autowired
	IpInterfaceDao m_ipInterfaceDao;

	@Autowired
	OutageDao m_outageDao;

	@Autowired
	JdbcTemplate m_template;

    @Override
    public void afterPropertiesSet() throws Exception {
        BeanUtils.assertAutowiring(this);
    }

	@Before
	public void setUp() throws Exception {
		m_dbPopulator.populateDatabase();
	}

	/**
	 * @throws Exception
	 */
	@Test
	@Transactional
	public void testAvailabilityDatabase() throws Exception {

		List<OnmsNode> nodes = m_nodeDao.findAll();
		for (OnmsNode node : nodes) {
			m_nodeDao.initialize(node);
			// TODO: Is this necessary?
			m_nodeDao.initialize(node.getLocation());
		}
		for (OnmsNode node : nodes) {
			System.err.println("NODE "+ node.toString());
		}
		List<OnmsIpInterface> ifs = m_ipInterfaceDao.findAll();
		for (OnmsIpInterface iface : ifs) {
			System.err.println("INTERFACE "+ iface.toString());
		}
		Assert.assertEquals("node DB count", 2, m_nodeDao.countAll());
		Assert.assertEquals("service DB count", 3, m_serviceTypeDao.countAll());
		Assert.assertEquals("IP interface DB count", 3, m_ipInterfaceDao.countAll());
		Assert.assertEquals("outages DB Count",6 ,m_outageDao.countMatching(new CriteriaBuilder(OnmsOutage.class).isNull("perspective").toCriteria()));

		final OnmsIpInterface oneHundredDotOne = m_ipInterfaceDao.findByNodeIdAndIpAddress(1, "192.168.100.1");

		try {
			List<OnmsMonitoredService> stmt = m_template.query(
					"SELECT ifServices.serviceid, service.servicename FROM ifServices, ipInterface, node, service WHERE ifServices.ipInterfaceId = ipInterface.id AND ipInterface.nodeId = node.nodeId " + 
					"AND ipinterface.ipaddr = '192.168.100.1' AND ipinterface.isManaged ='M' AND " + 
					"ifServices.serviceid = service.serviceid AND ifservices.status = 'A' AND node.nodeid = 1 AND node.nodetype = 'A'",
					new RowMapper<OnmsMonitoredService>() {
                                                @Override
						public OnmsMonitoredService mapRow(ResultSet rs, int rowNum) throws SQLException {
							OnmsMonitoredService retval = new OnmsMonitoredService(oneHundredDotOne, m_serviceTypeDao.findByName(rs.getString("servicename")));
							return retval;
						}
					}
			);
			// ResultSet srs = stmt.executeQuery("SELECT ipInterface.ipaddr, ipInterface.nodeid FROM ipInterface WHERE ipInterface.ipaddr = '192.168.100.1'" );
			Assert.assertTrue("interface results for 192.168.100.2", stmt.size() > 0);
			Assert.assertEquals(new Integer(1) ,stmt.get(0).getServiceId());
		} catch (Exception e) {
			LOG.error("unable to execute SQL", e);
			throw e;
		}

		/*
		Assert.assertEquals("node DB count", 2, m_db.countRows("select * from node"));
		Assert.assertEquals("service DB count", 3,
				m_db.countRows("select * from service"));
		Assert.assertEquals("ipinterface DB count", 3,
				m_db.countRows("select * from ipinterface"));
		Assert.assertEquals("interface services DB count", 3,
				m_db.countRows("select * from ifservices"));
		// Assert.assertEquals("outages DB count", 3, m_db.countRows("select * from
		// outages"));
		Assert.assertEquals(
				"ip interface DB count where ipaddr = 192.168.100.1",
				1,
				m_db.countRows("select * from ipinterface where ipaddr = '192.168.100.1'"));
		Assert.assertEquals(
				"number of interfaces returned from IPLIKE",
				3,
				m_db.countRows("select * from ipinterface where iplike(ipaddr,'192.168.100.*')"));
		 */
	}
}
