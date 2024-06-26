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
package org.opennms.web.rest.v1;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import javax.servlet.ServletContext;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.eclipse.jetty.util.URIUtil;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.opennms.core.test.OpenNMSJUnit4ClassRunner;
import org.opennms.core.test.db.annotations.JUnitTemporaryDatabase;
import org.opennms.core.test.rest.AbstractSpringJerseyRestTestCase;
import org.opennms.features.distributed.kvstore.api.JsonStore;
import org.opennms.netmgt.dao.DatabasePopulator;
import org.opennms.netmgt.dao.support.FilesystemResourceStorageDao;
import org.opennms.netmgt.rrd.RrdStrategyFactory;
import org.opennms.test.JUnitConfigurationEnvironment;
import org.opennms.web.svclayer.support.DefaultGraphResultsService;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

@RunWith(OpenNMSJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations={
        "classpath:/META-INF/opennms/applicationContext-soa.xml",
        "classpath:/META-INF/opennms/applicationContext-commonConfigs.xml",
        "classpath:/META-INF/opennms/applicationContext-minimal-conf.xml",
        "classpath:/META-INF/opennms/applicationContext-dao.xml",
        "classpath:/META-INF/opennms/applicationContext-mockConfigManager.xml",
        "classpath*:/META-INF/opennms/component-service.xml",
        "classpath*:/META-INF/opennms/component-dao.xml",
        "classpath:/META-INF/opennms/applicationContext-databasePopulator.xml",
        "classpath:/META-INF/opennms/mockEventIpcManager.xml",
        "file:src/main/webapp/WEB-INF/applicationContext-svclayer.xml",
        "file:src/main/webapp/WEB-INF/applicationContext-cxf-common.xml",
        "classpath:/META-INF/opennms/applicationContext-postgresJsonStore.xml",
        "classpath:/applicationContext-rest-test.xml"
})
@JUnitConfigurationEnvironment
@JUnitTemporaryDatabase
@Transactional
public class ResourceRestServiceIT extends AbstractSpringJerseyRestTestCase {

    @Autowired
    private ServletContext m_servletContext;

    @Autowired
    private DatabasePopulator m_dbPopulator;

    @Autowired
    private FilesystemResourceStorageDao m_resourceStorageDao;

    @Autowired
    private RrdStrategyFactory m_rrdStrategyFactory;

    @Autowired
    private JsonStore jsonStore;

    @Rule
    public TemporaryFolder m_tempFolder = new TemporaryFolder();

    protected String m_extension;

    @Before
    public void setUp() throws Throwable {
        super.setUp();

        // Add some nodes
        m_dbPopulator.populateDatabase();

        // Point to our temporary directory
        m_resourceStorageDao.setRrdDirectory(m_tempFolder.getRoot());

        // Add some blank RRD files
        m_extension = m_rrdStrategyFactory.getStrategy().getDefaultFileExtension();
        File nodeSnmp1 = m_tempFolder.newFolder("snmp", "1");
        FileUtils.touch(new File(nodeSnmp1, "SwapIn" + m_extension));
        FileUtils.touch(new File(nodeSnmp1, "SwapOut" + m_extension));
    }

    @Test
    @JUnitTemporaryDatabase
    public void testResources() throws Exception {
        // Top level
        String url = "/resources";
        String xml = sendRequest(GET, url, 200);
        assertTrue(xml.contains("Node-level Performance Data"));
        System.err.println(xml);

        // By ID
        url = "/resources/" + URLEncoder.encode("node[1].nodeSnmp[]", StandardCharsets.UTF_8.name());
        xml = sendRequest(GET, url, 200);
        assertTrue(xml.contains("Node-level Performance Data"));

        // 404 on invalid resource
        url = "/resources/" + URLEncoder.encode("node[99].nodeSnmp[]", StandardCharsets.UTF_8.name());
        sendRequest(GET, url, 404);

        // By Node ID
        url = "/resources/fornode/1";
        xml = sendRequest(GET, url, 200);
        assertTrue(xml.contains("Node-level Performance Data"));

        // 404 on invalid Node ID
        url = "/resources/fornode/99";
        xml = sendRequest(GET, url, 404);
    }

    @Test
    @JUnitTemporaryDatabase
    public void selectResources() throws Exception {
        {
            var value = select("1,2", null);
            assertThat(value, hasSize(2));
            assertThat(value.get(0).get("id"), is("node[imported::1]"));
            assertThat(value.get(1).get("id"), is("node[imported::2]"));
            var resources = (ArrayList<Map<String, String>>) ((Map) value.get(0).get("children")).get("resource");
            assertThat(resources, hasSize(1));
            assertThat(resources.get(0).get("id"), is("node[imported::1].nodeSnmp[]"));
        }

        {
            var value = select("1", "nodeSnmp[]");
            assertThat(value, hasSize(1));
            assertThat(value.get(0).get("id"), is("node[imported::1]"));
            var resources = (ArrayList<Map<String, String>>) ((Map) value.get(0).get("children")).get("resource");
            assertThat(resources, hasSize(1));
            assertThat(resources.get(0).get("id"), is("node[imported::1].nodeSnmp[]"));
        }

        {
            var value = select("1", "unknown");
            assertThat(value, hasSize(1));
            assertThat(value.get(0).get("id"), is("node[imported::1]"));
            var resources = (ArrayList<Map<String, String>>) ((Map) value.get(0).get("children")).get("resource");
            assertThat(resources, hasSize(0));
        }

    }

    private ArrayList<Map<String, Object>> select(String nodes, String subresources) throws Exception {
        var url = "/resources/select?nodes=" + nodes;
        if (subresources != null) {
            url += "&nodeSubresources=" + URLEncoder.encode(subresources, StandardCharsets.UTF_8);
        }
        var req = createRequest(GET, url);
        req.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        var json = sendRequest(req, 200);
        var mapper = new ObjectMapper();
        return mapper.readValue(json, new TypeReference<ArrayList<Map<String, Object>>>() {});
    }

    @Test
    @JUnitTemporaryDatabase
    public void testResourcesJson() throws Exception {
        String url = "/resources";

        // GET all users
        MockHttpServletRequest jsonRequest = createRequest(m_servletContext, GET, url);
        jsonRequest.addHeader("Accept", MediaType.APPLICATION_JSON);
        String json = sendRequest(jsonRequest, 200);

        JSONObject restObject = new JSONObject(json);
        final String jsonString = IOUtils.toString(new FileInputStream("src/test/resources/v1/resources.json"));
        JSONObject expectedObject = new JSONObject(jsonString.replace(".jrb", m_extension));
        JSONAssert.assertEquals(expectedObject, restObject, true);
    }


    @Test
    @JUnitTemporaryDatabase
    public void generateIdFromResources() throws Exception {

        String url = "/resources/generateId";
        String[] resources = {"node[homeNetwork:1550956829009].interfaceSnmp[opennms-jvm]",
                "node[homeNetwork:1550956829009].interfaceSnmp[docker0-0242ddee5bdc]"};
        String jsonString = new Gson().toJson(resources);
        MockHttpServletResponse response = sendData(POST, MediaType.APPLICATION_JSON, url, jsonString, 200);
        String generatedId = new Gson().fromJson(response.getContentAsString(), String.class);
        Optional<String> resourceIds = jsonStore.get(generatedId, DefaultGraphResultsService.RESOURCE_IDS_CONTEXT);
        assertTrue(resourceIds.isPresent());
        String[] savedResources = new Gson().fromJson(resourceIds.get(), String[].class);
        assertArrayEquals(resources, savedResources);
        //Do another post with same resources, it should generate same Id.
        response = sendData(POST, MediaType.APPLICATION_JSON, url, jsonString, 200);
        String  regeneratedId = new Gson().fromJson(response.getContentAsString(), String.class);
        assertEquals(generatedId, regeneratedId);

    }
}
