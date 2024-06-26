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
package org.opennms.netmgt.provision.detector.web;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.spring.BeanUtils;
import org.opennms.core.test.MockLogAppender;
import org.opennms.netmgt.provision.server.SimpleServer;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * <p>JUnit Test class for WebDetector.</p>
 * <p>The WebDetector should work at least like the HttpDetector.</p>
 * <p>The JUnit tests are basically the same as the HttpDetector with some minor changes in order to let HttpClient works.</p>
 *
 * @author Alejandro Galue <agalue@opennms.org>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:/META-INF/opennms/detectors.xml"})
public class WebDetectorTest implements InitializingBean {

    @Autowired
    private WebDetectorFactory m_detectorFactory;

    private WebDetector m_detector;
    
    private SimpleServer m_server;

    private String headers = "HTTP/1.1 200 OK\r\n"
            + "Date: Tue, 28 Oct 2008 20:47:55 GMT\r\n"
            + "Server: Apache/2.0.54\r\n"
            + "Last-Modified: Fri, 16 Jun 2006 01:52:14 GMT\r\n"
            + "ETag: \"778216aa-2f-aa66cf80\"\r\n"
            + "Accept-Ranges: bytes\r\n"
            + "Vary: Accept-Encoding,User-Agent\r\n"
            + "Connection: close\r\n"
            + "Content-Type: text/html\r\n";

    private String serverContent = "<html>\r\n"
            + "<body>\r\n"
            + "<!-- default -->\r\n"
            + "</body>\r\n"
            + "</html>\r\n";

    private String serverOKResponse = headers + String.format("Content-Length: %s\r\n", serverContent.length()) + "\r\n" + serverContent;

    private String notFoundResponse = "HTTP/1.1 404 Not Found\r\n"
            + "Date: Tue, 28 Oct 2008 20:47:55 GMT\r\n"
            + "Server: Apache/2.0.54\r\n"
            + "Last-Modified: Fri, 16 Jun 2006 01:52:14 GMT\r\n"
            + "ETag: \"778216aa-2f-aa66cf80\"\r\n"
            + "Accept-Ranges: bytes\r\n"
            + "Content-Length: 52\r\n"
            + "Vary: Accept-Encoding,User-Agent\r\n"
            + "Connection: close\rn"
            + "Content-Type: text/html\r\n"
            + "\r\n"
            + "<html>\r\n"
            + "<body>\r\n"
            + "<!-- default -->\r\n"
            + "</body>\r\n"
            + "</html>";

    private String notAServerResponse = "NOT A SERVER";

    @Override
    public void afterPropertiesSet() throws Exception {
        BeanUtils.assertAutowiring(this);
    }

    @Before
    public void setUp() throws Exception {
        MockLogAppender.setupLogging();
        m_detector = m_detectorFactory.createDetector(new HashMap<>());
        m_detector.setPort(80);
        m_detector.setPath("/");
        m_detector.setResponseRange("100-399");
    }

    @After
    public void tearDown() throws IOException {
        if (m_server != null) {
            m_server.stopServer();
            m_server = null;
        }
        MockLogAppender.assertNoWarningsOrGreater();
    }

    @Test(timeout=20000)
    public void testRegexMatch() {
        System.err.println(notFoundResponse);
        String expectedTest = "~404 Not Found";
        System.err.println("EXPRESSION: " + expectedTest.substring(1));
        final Pattern p = Pattern.compile(expectedTest.substring(1), Pattern.MULTILINE);
        final Matcher m1 = p.matcher(notFoundResponse);
        assertTrue(m1.find());
        final Matcher m2 = p.matcher(serverOKResponse);
        assertFalse(m2.find());
    }

    @Test(timeout=20000)
    public void testDetectorFailNotAServerResponse() throws Exception {
        m_server = createServer(notAServerResponse);
        m_detector.setPort(m_server.getLocalPort());
        m_detector.init();

        assertFalse(m_detector.isServiceDetected(m_server.getInetAddress()));
    }

    @Test(timeout=20000)
    public void testDetectorFailNotFoundResponseMaxRetCode399() throws Exception {        
        m_server = createServer(notFoundResponse);
        m_detector.setPath("/blog");
        m_detector.setResponseRange("100-301");
        m_detector.setPort(m_server.getLocalPort());
        m_detector.init();

        assertFalse(m_detector.isServiceDetected(m_server.getInetAddress()));
    }

    @Test(timeout=20000)
    public void testDetectorSucessMaxRetCode399() throws Exception {
        m_server = createServer(getServerOKResponse());
        m_detector.setPath("/blog");
        m_detector.setResponseRange("100-399");
        m_detector.setPort(m_server.getLocalPort());
        m_detector.init();

        assertTrue(m_detector.isServiceDetected(m_server.getInetAddress()));
    }

    @Test(timeout=20000)
    public void testDetectorFailMaxRetCodeBelow200() throws Exception {
        m_server = createServer(getServerOKResponse());
        m_detector.setPath("/blog");
        m_detector.setResponseRange("100-199");
        m_detector.setPort(m_server.getLocalPort());
        m_detector.init();

        assertFalse(m_detector.isServiceDetected(m_server.getInetAddress()));
    }

    @Test(timeout=20000)
    public void testDetectorMaxRetCode600() throws Exception {
        m_server = createServer(getServerOKResponse());
        m_detector.setResponseRange("100-600");
        m_detector.setPort(m_server.getLocalPort());
        m_detector.init();

        assertTrue(m_detector.isServiceDetected(m_server.getInetAddress()));
    }

    @Test(timeout=20000)
    public void testDetectorSucessCheckCodeTrue() throws Exception {
        m_server = createServer(getServerOKResponse());
        m_detector.setPath("http://localhost/");
        m_detector.setPort(m_server.getLocalPort());
        m_detector.init();

        assertTrue(m_detector.isServiceDetected(m_server.getInetAddress()));
    }

    @Test(timeout=20000)
    public void testDetectorSuccessCheckCodeFalse() throws Exception {
        m_server = createServer(getServerOKResponse());
        m_detector.setPort(m_server.getLocalPort());
        m_detector.init();

        assertTrue(m_detector.isServiceDetected(m_server.getInetAddress()));
    }

    public void setServerOKResponse(String serverOKResponse) {
        this.serverOKResponse = serverOKResponse;
    }

    public String getServerOKResponse() {
        return serverOKResponse;
    }

    private static SimpleServer createServer(final String httpResponse) throws Exception {
        SimpleServer server = new SimpleServer() {
            @Override
            public void init() throws Exception {
                super.init();
                setServerSocket(new ServerSocket(0, 0, InetAddress.getLocalHost()));
                addResponseHandler(contains("GET"), shutdownServer(httpResponse));
            }
        };
        server.init();
        server.startServer();
        return server;
    }

}
