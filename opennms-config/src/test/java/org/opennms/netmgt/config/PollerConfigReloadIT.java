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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.config.poller.Package;
import org.opennms.netmgt.filter.FilterDaoFactory;
import org.opennms.netmgt.filter.api.FilterDao;
import org.opennms.netmgt.filter.api.FilterParseException;

public class PollerConfigReloadIT {

    private PollerConfigManager pollerConfigManager;

    private File includeUrlFile;


    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setup() throws IOException {
        includeUrlFile = tempFolder.newFile("poller-config-include-url.txt");
        fillInitialData(includeUrlFile);
        InputStream configStream = setIncludeUrlFileInConfig(PollerConfigReloadIT.class.getResource("/poller-configuration.xml"));;
        FilterDao mockFilterDao = mock(FilterDao.class);
        List<InetAddress> inetAddressList = new ArrayList<>();
        inetAddressList.add(InetAddressUtils.addr("127.0.0.5"));
        inetAddressList.add(InetAddressUtils.addr("128.0.1.10"));
        inetAddressList.add(InetAddressUtils.addr("128.0.1.9"));
        inetAddressList.add(InetAddressUtils.addr("128.0.1.12"));
        when(mockFilterDao.getActiveIPAddressList(Mockito.anyString())).thenReturn(inetAddressList);
        FilterDaoFactory.setInstance(mockFilterDao);
        pollerConfigManager = new TestPollerConfigFactory(configStream);
    }

    private static class TestPollerConfigFactory extends PollerConfigManager {

        private TestPollerConfigFactory(InputStream stream) {
            super(stream);
        }

        @Override
        protected void saveXml(String xml) throws IOException {
            //pass
        }
    }


    @Test
    public void testPollerConfigReloadForIncludeUrls() throws IOException {
        Package aPackage = pollerConfigManager.getPackage("example1");
        //Verify that ipAddress from url file exists in package.
        assertTrue(pollerConfigManager.isInterfaceInPackage("128.0.1.10", aPackage));
        assertFalse(pollerConfigManager.isInterfaceInPackage("128.0.1.12", aPackage));
        // Include 128.0.1.12 in url file
        updateIpAddressesInUrlFile(includeUrlFile);
        //Call update. Reload Event calls update on the config.
        pollerConfigManager.update();
        assertTrue(pollerConfigManager.isInterfaceInPackage("128.0.1.12", aPackage));
    }


    private void fillInitialData(File includeUrlFile) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(includeUrlFile.toPath())) {
            writer.write("127.0.0.5");
            writer.newLine();
            writer.write("128.0.1.10");
            writer.newLine();
        }
    }


    private void updateIpAddressesInUrlFile(File includeUrlFile) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(includeUrlFile.toPath())) {
            writer.write("128.0.1.12");
            writer.newLine();
            writer.write("128.0.1.9");
            writer.newLine();
        }
    }

    private InputStream setIncludeUrlFileInConfig(URL configUrl) throws IOException {
        String marshalledString = IOUtils.toString(configUrl, Charset.defaultCharset());
        String modifiedString = marshalledString.replace("${INCLUDE_URL_FILE}", includeUrlFile.getAbsolutePath());
        return IOUtils.toInputStream(modifiedString, Charset.defaultCharset());
    }

    @Test
    public void testPollerConfigReloadFail() throws Exception {
        final File temporaryFile = File.createTempFile("poller-configuration-", ".xml", new File(PollerConfigReloadIT.class.getResource("/etc").getFile()));
        PollerConfigFactory.setPollerConfigFile(temporaryFile);

        final AtomicBoolean invalid = new AtomicBoolean(false);
        FilterDao filterDao = mock(FilterDao.class);
        doAnswer(invocation -> {
            if (invalid.get()) {
                throw new FilterParseException("Something fishy");
            }
            return null;
        }).when(filterDao).validateRule(any(String.class));
        FilterDaoFactory.setInstance(filterDao);

        IOUtils.copy(new FileInputStream(PollerConfigReloadIT.class.getResource("/poller-configuration-valid1.xml").getFile()), new FileOutputStream(temporaryFile));
        long lastModified = temporaryFile.lastModified();

        PollerConfigFactory.init();

        assertEquals("IPADDR IPLIKE 1.*.*.*", PollerConfigFactory.getInstance().getPackage("example1").getFilter().getContent());

        IOUtils.copy(new FileInputStream(PollerConfigReloadIT.class.getResource("/poller-configuration-valid2.xml").getFile()), new FileOutputStream(temporaryFile));
        temporaryFile.setLastModified(lastModified + 1000);

        invalid.set(true);
        try {
            PollerConfigFactory.getInstance().update();
        } catch (FilterParseException e) {
            // we expect this
        }

        assertEquals("IPADDR IPLIKE 1.*.*.*", PollerConfigFactory.getInstance().getPackage("example1").getFilter().getContent());

        IOUtils.copy(new FileInputStream(PollerConfigReloadIT.class.getResource("/poller-configuration-valid2.xml").getFile()), new FileOutputStream(temporaryFile));
        temporaryFile.setLastModified(lastModified + 2000);

        invalid.set(false);
        PollerConfigFactory.getInstance().update();

        assertEquals("IPADDR IPLIKE 2.*.*.*", PollerConfigFactory.getInstance().getPackage("example1").getFilter().getContent());
    }
}
