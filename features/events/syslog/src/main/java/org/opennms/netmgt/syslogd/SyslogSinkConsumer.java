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
package org.opennms.netmgt.syslogd;

import static org.opennms.core.utils.InetAddressUtils.addr;

import java.util.List;
import java.util.Set;

import com.codahale.metrics.Gauge;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.opennms.core.ipc.sink.api.MessageConsumer;
import org.opennms.core.ipc.sink.api.MessageConsumerManager;
import org.opennms.core.logging.Logging;
import org.opennms.core.logging.Logging.MDCCloseable;
import org.opennms.core.sysprops.SystemProperties;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.config.SyslogdConfig;
import org.opennms.netmgt.dao.api.DistPollerDao;
import org.opennms.netmgt.events.api.EventConstants;
import org.opennms.netmgt.events.api.EventForwarder;
import org.opennms.netmgt.model.events.EventBuilder;
import org.opennms.netmgt.provision.LocationAwareDnsLookupClient;
import org.opennms.netmgt.syslogd.api.SyslogConnection;
import org.opennms.netmgt.syslogd.api.SyslogMessageDTO;
import org.opennms.netmgt.syslogd.api.SyslogMessageLogDTO;
import org.opennms.netmgt.xml.event.Event;
import org.opennms.netmgt.xml.event.Events;
import org.opennms.netmgt.xml.event.Log;
import org.opennms.netmgt.xml.event.Parm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.google.common.base.Strings;

public class SyslogSinkConsumer implements MessageConsumer<SyslogConnection, SyslogMessageLogDTO>, InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(SyslogSinkConsumer.class);

    private static final String defaultCacheConfig = "maximumSize=1000,expireAfterWrite=8h";
    private static final String dnsCacheConfigProperty = "org.opennms.netmgt.syslogd.dnscache.config";
    @Autowired
    private MessageConsumerManager messageConsumerManager;

    @Autowired
    private SyslogdConfig syslogdConfig;

    @Autowired
    private DistPollerDao distPollerDao;

    @Autowired
    private EventForwarder eventForwarder;

    @Autowired
    private LocationAwareDnsLookupClient m_locationAwareDnsLookupClient;

    private Cache<HostNameWithLocationKey, String> dnsCache;

    private final String localAddr;
    private final Timer consumerTimer;
    private final Timer toEventTimer;
    private final Timer broadcastTimer;

    public SyslogSinkConsumer(MetricRegistry registry) {
        consumerTimer = registry.timer("consumer");
        toEventTimer = registry.timer("consumer.toevent");
        broadcastTimer = registry.timer("consumer.broadcast");
        String cacheConfig = System.getProperty(dnsCacheConfigProperty, defaultCacheConfig);
        dnsCache = CacheBuilder.from(cacheConfig).recordStats().build();
        registry.register("dnsCacheSize", (Gauge<Long>) () -> dnsCache.size());
        registry.register("dnsCacheHitRate", (Gauge<Double>) () -> dnsCache.stats().hitRate());
        localAddr = InetAddressUtils.getLocalHostName();
    }

    @Override
    public SyslogSinkModule getModule() {
        return new SyslogSinkModule(syslogdConfig, distPollerDao);
    }

    @Override
    public void handleMessage(SyslogMessageLogDTO syslogDTO) {
        try (Context consumerCtx = consumerTimer.time()) {
            try (MDCCloseable mdc = Logging.withPrefixCloseable(Syslogd.LOG4J_CATEGORY)) {
                // Convert the Syslog UDP messages to Events
                final Log eventLog;
                try (Context toEventCtx = toEventTimer.time()) {
                    eventLog = toEventLog(syslogDTO);
                }
                // Broadcast the Events to the event bus
                try (Context broadCastCtx = broadcastTimer.time()) {
                    broadcast(eventLog);
                }
            }
        }
    }

    public Log toEventLog(SyslogMessageLogDTO messageLog) {
        final Log elog = new Log();
        final Events events = new Events();
        elog.setEvents(events);
        for (SyslogMessageDTO message : messageLog.getMessages()) {
            try {
                LOG.debug("Converting syslog message into event.");
                ConvertToEvent re = new ConvertToEvent(
                        messageLog.getSystemId(),
                        messageLog.getLocation(),
                        messageLog.getSourceAddress(),
                        messageLog.getSourcePort(),
                        message.getBytes(),
                        message.getTimestamp(),
                        syslogdConfig,
                        m_locationAwareDnsLookupClient,
                        dnsCache);
                events.addEvent(re.getEvent());
            } catch (final MessageDiscardedException e) {
                LOG.info("Message discarded, returning without enqueueing event.", e);
            } catch (final Throwable e) {
                LOG.error("Unexpected exception while processing SyslogConnection", e);
            }
        }
        return elog;
    }

    private void broadcast(Log eventLog)  {
        if (LOG.isTraceEnabled())  {
            for (Event event : eventLog.getEvents().getEventCollection()) {
                LOG.trace("Processing a syslog to event dispatch", event.toString());
                String uuid = event.getUuid();
                LOG.trace("Event {");
                LOG.trace("  uuid  = {}", (uuid != null && uuid.length() > 0 ? uuid : "<not-set>"));
                LOG.trace("  uei   = {}", event.getUei());
                LOG.trace("  src   = {}", event.getSource());
                LOG.trace("  iface = {}", event.getInterface());
                LOG.trace("  time  = {}", event.getTime());
                LOG.trace("  Msg   = {}", event.getLogmsg().getContent());
                LOG.trace("  Dst   = {}", event.getLogmsg().getDest());
                List<Parm> parms = (event.getParmCollection() == null ? null : event.getParmCollection());
                if (parms != null) {
                    LOG.trace("  parms {");
                    for (Parm parm : parms) {
                        if ((parm.getParmName() != null)
                                && (parm.getValue().getContent() != null)) {
                            LOG.trace("    ({}, {})", parm.getParmName().trim(), parm.getValue().getContent().trim());
                        }
                    }
                    LOG.trace("  }");
                }
                LOG.trace("}");
            }
        }
        eventForwarder.sendNowSync(eventLog);

        if (syslogdConfig.getNewSuspectOnMessage()) {
            eventLog.getEvents().getEventCollection().stream()
                .filter(e -> !e.hasNodeid())
                .filter(e -> !Strings.isNullOrEmpty(e.getInterface()))
                .forEach(e -> {
                    LOG.trace("Syslogd: Found a new suspect {}", e.getInterface());
                    sendNewSuspectEvent(localAddr, e.getInterface(), e.getDistPoller());
                });
        }
    }

    private void sendNewSuspectEvent(String localAddr, String trapInterface, String distPoller) {
        EventBuilder bldr = new EventBuilder(EventConstants.NEW_SUSPECT_INTERFACE_EVENT_UEI, "syslogd");
        bldr.setInterface(addr(trapInterface));
        bldr.setHost(localAddr);
        bldr.setDistPoller(distPoller);
        eventForwarder.sendNow(bldr.getEvent());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // Automatically register the consumer on initialization
        messageConsumerManager.registerConsumer(this);
    }

    public void setEventForwarder(EventForwarder eventForwarder) {
        this.eventForwarder = eventForwarder;
    }

    public void setMessageConsumerManager(MessageConsumerManager messageConsumerManager) {
        this.messageConsumerManager = messageConsumerManager;
    }

    public void setSyslogdConfig(SyslogdConfig syslogdConfig) {
        this.syslogdConfig = syslogdConfig;
    }

    public void setDistPollerDao(DistPollerDao distPollerDao) {
        this.distPollerDao = distPollerDao;
    }

    public void setLocationAwareDnsLookupClient(LocationAwareDnsLookupClient locationAwareDnsLookupClient) {
        this.m_locationAwareDnsLookupClient = locationAwareDnsLookupClient;
    }

}
