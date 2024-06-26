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
package org.opennms.netmgt.provision.service.lifecycle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.opennms.core.tasks.BatchTask;
import org.opennms.core.tasks.SequenceTask;
import org.opennms.core.tasks.TaskCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DefaultLifeCycleInstance extends SequenceTask implements LifeCycleInstance {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultLifeCycleInstance.class);
    
    /*
     * Complications... 
     * separation between 'phase list... lifecycle definition', 'provider set', 'phase running'
     * 
     * also
     * confusion.. about definitions and factories
     * 
     * other notes:
     * phase runners can and should be built at lifecycle creation time rather than 'on the fly'
     * only strange case is 'fan-out' of lifecycles
     * 
     * 
     *  
     */
    
    private final BatchTask m_containingPhase;
    private final LifeCycleRepository m_repository;
    private final TaskCoordinator m_coordinator;
    private final String m_name;
    private final Phase[] m_phases;
    private final Object[] m_providers;
    private final Map<String, Object> m_attributes = new HashMap<String, Object>();
    
    /**
     * <p>Constructor for DefaultLifeCycleInstance.</p>
     *
     * @param containingPhase a {@link org.opennms.core.tasks.BatchTask} object.
     * @param repository a {@link org.opennms.netmgt.provision.service.lifecycle.LifeCycleRepository} object.
     * @param coordinator a {@link org.opennms.core.tasks.TaskCoordinator} object.
     * @param lifeCycleName a {@link java.lang.String} object.
     * @param phaseNames an array of {@link java.lang.String} objects.
     * @param providers an array of {@link java.lang.Object} objects.
     */
    public DefaultLifeCycleInstance(BatchTask containingPhase, LifeCycleRepository repository,
            TaskCoordinator coordinator, String lifeCycleName, String[] phaseNames, Object[] providers) {

        super(coordinator, containingPhase);
        m_containingPhase = containingPhase;
        m_repository = repository;
        m_coordinator = coordinator;
        m_name = lifeCycleName;
        m_providers = providers;

        LOG.debug("Set up default lifecycle instance");

        m_phases = new Phase[phaseNames.length];
        for(int i = 0; i < phaseNames.length; i++) {
            m_phases[i] = new Phase(this, this, phaseNames[i], m_providers);
            LOG.debug("Adding phase {} to lifecycle", m_phases[i].getName());
            add(m_phases[i]);
        }
        
        setAttribute("lifeCycleInstance", this);
    }


    /**
     * <p>Constructor for DefaultLifeCycleInstance.</p>
     *
     * @param repository a {@link org.opennms.netmgt.provision.service.lifecycle.LifeCycleRepository} object.
     * @param coordinator a {@link org.opennms.core.tasks.TaskCoordinator} object.
     * @param lifeCycleName a {@link java.lang.String} object.
     * @param phaseNames an array of {@link java.lang.String} objects.
     * @param providers an array of {@link java.lang.Object} objects.
     */
    public DefaultLifeCycleInstance(LifeCycleRepository repository, TaskCoordinator coordinator, String lifeCycleName, String[] phaseNames, Object[] providers) {
        this(null, repository, coordinator, lifeCycleName, Arrays.copyOf(phaseNames, phaseNames.length), Arrays.copyOf(providers, providers.length));
    }

    /**
     * <p>getPhaseNames</p>
     *
     * @return a {@link java.util.List} object.
     */
    @Override
    public List<String> getPhaseNames() {
        List<String> phaseNames = new ArrayList<String>(m_phases.length);
        for(Phase phase : m_phases) {
            phaseNames.add(phase.getName());
        }
        return phaseNames;
    }

    /**
     * <p>getName</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Override
    public String getName() {
        return m_name;
    }

    /** {@inheritDoc} */
    @Override
    public Object getAttribute(String key) {
        return m_attributes.get(key);
    }
    
    /**
     * <p>getAttribute</p>
     *
     * @param key a {@link java.lang.String} object.
     * @param defaultValue a T object.
     * @param <T> a T object.
     * @return a T object.
     */
    @Override
    public <T> T getAttribute(String key, T defaultValue) {
        if (getAttribute(key) == null) {
            return defaultValue;
        } else {
            return getAttribute(key, getClass(defaultValue));
        }
        
    }
    
    /**
     * <p>getClass</p>
     *
     * @param t a T object.
     * @param <T> a T object.
     * @return a {@link java.lang.Class} object.
     */
    @SuppressWarnings("unchecked")
    public <T> Class<T> getClass(T t) {
        return (Class<T>) t.getClass();
    }

    /** {@inheritDoc} */
    @Override
    public <T> T getAttribute(String key, Class<T> type) {
        return type.cast(getAttribute(key));
    }

    /** {@inheritDoc} */
    @Override
    public LifeCycleInstance setAttribute(String key, Object value) {
        m_attributes.put(key, value);
        return this;
    }
    
    /** {@inheritDoc} */
    @Override
    public <T> T findAttributeByType(Class<T> clazz) {
        T result = null;
        for(Entry<String, Object> entry : m_attributes.entrySet()) {
            if (clazz.isInstance(entry.getValue())) {
                if (result != null) {
                    throw new IllegalStateException("More than one attribute of type "+clazz+" in lifecycle "+this);
                } else {
                    result = clazz.cast(entry.getValue());
                }
            }
        }
        return result;
    }

    
    /** {@inheritDoc} */
    @Override
    public LifeCycleInstance createNestedLifeCycle(BatchTask containingPhase, String lifeCycleName) {
        return m_repository.createNestedLifeCycleInstance(containingPhase, lifeCycleName, m_providers);
    }

    /**
     * <p>trigger</p>
     */
    @Override
    public void trigger() {
        if (m_containingPhase != null) {
            m_containingPhase.add(this);
        } else {
            this.schedule();
        }
    }

    /**
     * <p>toString</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("name", m_name)
            .append("containing phase", m_containingPhase)
            .append("repository", m_repository)
            .append("coordinator", m_coordinator)
            .toString();
   }
}
