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
package org.opennms.netmgt.alarmd;

import org.opennms.core.sysprops.SystemProperties;
import org.opennms.netmgt.alarmd.drools.DroolsAlarmContext;
import org.opennms.netmgt.daemon.AbstractServiceDaemon;
import org.opennms.netmgt.daemon.DaemonTools;
import org.opennms.netmgt.events.api.ThreadAwareEventListener;
import org.opennms.netmgt.events.api.annotations.EventHandler;
import org.opennms.netmgt.events.api.annotations.EventListener;
import org.opennms.netmgt.events.api.model.IEvent;
import org.opennms.netmgt.xml.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Alarm management Daemon
 *
 * @author jwhite
 * @author <a href="mailto:david@opennms.org">David Hustace</a>
 */
@EventListener(name=Alarmd.NAME, logPrefix="alarmd")
public class Alarmd extends AbstractServiceDaemon implements ThreadAwareEventListener {
    private static final Logger LOG = LoggerFactory.getLogger(Alarmd.class);

    /** Constant <code>NAME="alarmd"</code> */
    public static final String NAME = "alarmd";

    protected static final Integer THREADS = SystemProperties.getInteger("org.opennms.alarmd.threads", 4);

    private AlarmPersister m_persister;

    @Autowired
    private AlarmLifecycleListenerManager m_alm;

    @Autowired
    private DroolsAlarmContext m_droolsAlarmContext;

    @Autowired
    private NorthbounderManager m_northbounderManager;

    public Alarmd() {
        super(NAME);
    }

    /**
     * Listens for all events.
     *
     * This method is thread-safe.
     *
     * @param e a {@link org.opennms.netmgt.events.api.model.IEvent} object.
     */
    @EventHandler(uei = EventHandler.ALL_UEIS)
    public void onEvent(IEvent e) {
    	if (e.getUei().equals("uei.opennms.org/internal/reloadDaemonConfig")) {
           handleReloadEvent(e);
           return;
    	}
    	m_persister.persist(Event.copyFrom(e));
    }

    private synchronized void handleReloadEvent(IEvent e) {
        m_northbounderManager.handleReloadEvent(e);
        DaemonTools.handleReloadEvent(e, Alarmd.NAME, (event) -> onAlarmReload());
    }

    private void onAlarmReload() {
        m_droolsAlarmContext.reload();
    }

	/**
     * <p>setPersister</p>
     *
     * @param persister a {@link org.opennms.netmgt.alarmd.AlarmPersister} object.
     */
    public void setPersister(AlarmPersister persister) {
        this.m_persister = persister;
    }

    /**
     * <p>getPersister</p>
     *
     * @return a {@link org.opennms.netmgt.alarmd.AlarmPersister} object.
     */
    public AlarmPersister getPersister() {
        return m_persister;
    }

    @Override
    protected synchronized void onInit() {
        // pass
    }

    @Override
    public synchronized void onStart() {
        // Start the Drools context
        m_droolsAlarmContext.start();
    }

    @Override
    public synchronized void onStop() {
        // Stop the northbound interfaces
        m_northbounderManager.stop();
        // Stop the Drools context
        m_droolsAlarmContext.stop();
    }

    @Override
    public int getNumThreads() {
        return THREADS;
    }

}
