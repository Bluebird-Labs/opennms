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
package org.opennms.netmgt.trapd;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Closeable;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.jms.MessageProducer;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.util.KeyValueHolder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opennms.core.ipc.sink.api.MessageConsumerManager;
import org.opennms.core.ipc.sink.api.MessageDispatcherFactory;
import org.opennms.core.ipc.sink.mock.MockMessageConsumerManager;
import org.opennms.core.ipc.twin.api.TwinPublisher;
import org.opennms.core.ipc.twin.api.TwinSubscriber;
import org.opennms.core.ipc.twin.memory.MemoryTwinPublisher;
import org.opennms.core.ipc.twin.memory.MemoryTwinSubscriber;
import org.opennms.core.test.OpenNMSJUnit4ClassRunner;
import org.opennms.core.test.camel.CamelBlueprintTest;
import org.opennms.distributed.core.api.RestClient;
import org.opennms.netmgt.dao.api.DistPollerDao;
import org.opennms.netmgt.snmp.SnmpObjId;
import org.opennms.netmgt.snmp.SnmpTrapBuilder;
import org.opennms.netmgt.snmp.SnmpUtils;
import org.opennms.netmgt.snmp.TrapListenerConfig;
import org.opennms.netmgt.trapd.jmx.Trapd;
import org.opennms.test.JUnitConfigurationEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

/**
 * Verify that the TrapListener is actually receiving traps and is using the Sink pattern to "dispatch" messages.
 */
@RunWith(OpenNMSJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/META-INF/opennms/applicationContext-soa.xml",
        "classpath:/META-INF/opennms/applicationContext-mockDao.xml",
        "classpath:/META-INF/opennms/applicationContext-twin-memory.xml",
})
@JUnitConfigurationEnvironment
public class TrapdSinkPatternWiringIT extends CamelBlueprintTest {

    private AtomicInteger m_port = new AtomicInteger(1162);

    @Autowired
    private DistPollerDao distPollerDao;

    @Autowired
    private TwinPublisher twinPublisher;

    private final CountDownLatch messageProcessedLatch = new CountDownLatch(1);

    @Override
    protected String setConfigAdminInitialConfiguration(Properties props) {
        getAvailablePort(m_port, 2162);
        props.put("trapd.listen.port", String.valueOf(m_port.get()));
        return "org.opennms.netmgt.trapd";
    }

    @Override
    protected void addServicesOnStartup(Map<String, KeyValueHolder<Object, Dictionary>> services) {
        final MessageDispatcherFactory mockMessageDispatcherFactory = mock(MessageDispatcherFactory.class);
        when(mockMessageDispatcherFactory.createAsyncDispatcher(Mockito.any(TrapSinkModule.class)))
            .thenAnswer(invocation -> {
                messageProcessedLatch.countDown(); // register call
                return mock(MessageProducer.class);
            });

        // add mocked services to osgi mocked container (Felix Connect)
        services.put(MessageConsumerManager.class.getName(), asService(new MockMessageConsumerManager(), null, null));
        services.put(MessageDispatcherFactory.class.getName(), asService(mockMessageDispatcherFactory, null, null));
        services.put(DistPollerDao.class.getName(), asService(distPollerDao, null, null));

        final var context = new DefaultCamelContext(new SimpleRegistry());
        services.put(CamelContext.class.getName(), asService(context, null, null));
    }

    // The CamelBlueprintTest should have started the bundle and therefore also started
    // the TrapListener (see blueprint-trapd-listener.xml), which listens to traps.
    @Test(timeout=30000)
    public void testWiring() throws Exception {
        // Send empty listener config to get things started
        this.twinPublisher.register(TrapListenerConfig.TWIN_KEY, TrapListenerConfig.class)
                          .publish(new TrapListenerConfig());

        // No traps received or processed
        Assert.assertEquals(1, messageProcessedLatch.getCount());

        // At this point everything should be set up correctly
        while(messageProcessedLatch.getCount() != 0) {
            final SnmpTrapBuilder builder = SnmpUtils.getV2TrapBuilder();
            builder.addVarBind(SnmpObjId.get(".1.3.6.1.2.1.1.3.0"), SnmpUtils.getValueFactory().getTimeTicks(0));
            builder.addVarBind(SnmpObjId.get(".1.3.6.1.6.3.1.1.4.1.0"), SnmpUtils.getValueFactory().getObjectId(SnmpObjId.get(".1.3.6.1.6.3.1.1.5.2")));
            builder.addVarBind(SnmpObjId.get(".1.3.6.1.6.3.1.1.4.3.0"), SnmpUtils.getValueFactory().getObjectId(SnmpObjId.get(".1.3.6.1.4.1.5813")));
            builder.send("localhost", m_port.get(), "public");

            // Wait before continuing
            messageProcessedLatch.await(10, TimeUnit.SECONDS);
        }
    }

    @Override
    protected String getBlueprintDescriptor() {
        return "OSGI-INF/blueprint/blueprint-empty.xml";
    }
}
