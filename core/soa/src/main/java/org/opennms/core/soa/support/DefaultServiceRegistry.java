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
package org.opennms.core.soa.support;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.opennms.core.soa.Filter;
import org.opennms.core.soa.Registration;
import org.opennms.core.soa.RegistrationHook;
import org.opennms.core.soa.RegistrationListener;
import org.opennms.core.soa.ServiceRegistry;
import org.opennms.core.soa.filter.FilterParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DefaultServiceRegistry
 *
 * @author brozow
 * @version $Id: $
 */
public class DefaultServiceRegistry implements ServiceRegistry {

    private static Logger LOG = LoggerFactory.getLogger(DefaultServiceRegistry.class);

    /**
     * AnyFilter
     *
     * @author brozow
     */
    public static class AnyFilter implements Filter {

        @Override
        public boolean match(Map<String, String> properties) {
            return true;
        }

    }

    /** Constant <code>INSTANCE</code> */
    public static final DefaultServiceRegistry INSTANCE = new DefaultServiceRegistry();
    
    private class ServiceRegistration implements Registration {

        private boolean m_unregistered = false;
        private Object m_provider;
        private Map<String, String> m_properties;
        private Class<?>[] m_serviceInterfaces;
        
        public ServiceRegistration(Object provider, Map<String, String> properties, Class<?>[] serviceInterfaces) {
            m_provider = provider;
            m_properties = properties;
            m_serviceInterfaces = Arrays.copyOf(serviceInterfaces, serviceInterfaces.length);
        }
        

        @Override
        public Map<String, String> getProperties() {
            return m_properties == null ? null : Collections.unmodifiableMap(m_properties);
        }

        @Override
        public Class<?>[] getProvidedInterfaces() {
            return m_serviceInterfaces;
        }

        @Override
        public <T> T getProvider(Class<T> serviceInterface) {

            if (serviceInterface == null) throw new NullPointerException("serviceInterface may not be null");

            for( Class<?> cl : m_serviceInterfaces ) {
                if ( serviceInterface.equals( cl ) ) {
                    return serviceInterface.cast( m_provider );
                }
            }
            
            throw new IllegalArgumentException("Provider not registered with interface " + serviceInterface);
        }
        
        @Override
        public Object getProvider() {
        	return m_provider;
        }

        @Override
        public ServiceRegistry getRegistry() {
            return DefaultServiceRegistry.this;
        }

        @Override
        public boolean isUnregistered() {
            return m_unregistered;
        }
        
        @Override
        public void unregister() {
            m_unregistered = true;
            DefaultServiceRegistry.this.unregister(this);
            m_provider = null;
        }
        
    }
    
    private MultivaluedMap<Class<?>, ServiceRegistration> m_registrationMap = MultivaluedMapImpl.synchronizedMultivaluedMap();
    private MultivaluedMap<Class<?>, RegistrationListener<?>> m_listenerMap = MultivaluedMapImpl.synchronizedMultivaluedMap();
    private List<RegistrationHook> m_hooks = new CopyOnWriteArrayList<>();
    
    /** {@inheritDoc} */
    @Override
    public <T> T findProvider(Class<T> serviceInterface) {
        return findProvider(serviceInterface, null);
    }
    
    /** {@inheritDoc} */
    @Override
    public <T> T findProvider(Class<T> serviceInterface, String filter) {
        Collection<T> providers = findProviders(serviceInterface, filter);
        for(T provider : providers) {
            return provider;
        }
        return null;
    }
    
    /** {@inheritDoc} */
    @Override
    public <T> Collection<T> findProviders(Class<T> serviceInterface) {
        return findProviders(serviceInterface, null);
    }

    /** {@inheritDoc} */
    @Override
    public <T> Collection<T> findProviders(Class<T> serviceInterface, String filter) {
        
        Filter f = filter == null ? new AnyFilter() : new FilterParser().parse(filter);

        Set<ServiceRegistration> registrations = getRegistrations(serviceInterface);
        Set<T> providers = new LinkedHashSet<T>(registrations.size());
        for(ServiceRegistration registration : registrations) {
            if (f.match(registration.getProperties())) {
                providers.add(registration.getProvider(serviceInterface));
            }
        }
        return providers;
    }

    /**
     * <p>register</p>
     *
     * @param serviceProvider a {@link java.lang.Object} object.
     * @param services a {@link java.lang.Class} object.
     * @return a {@link org.opennms.core.soa.Registration} object.
     */
    @Override
    public Registration register(Object serviceProvider, Class<?>... services) {
        return register(serviceProvider, (Map<String, String>)null, services);
    }

    /**
     * <p>register</p>
     *
     * @param serviceProvider a {@link java.lang.Object} object.
     * @param properties a {@link java.util.Map} object.
     * @param services a {@link java.lang.Class} object.
     * @return a {@link org.opennms.core.soa.Registration} object.
     */
    @Override
    public Registration register(Object serviceProvider, Map<String, String> properties, Class<?>... services) {
        
        ServiceRegistration registration = new ServiceRegistration(serviceProvider, properties, services);
        
        for(Class<?> serviceInterface : services) {
            m_registrationMap.add(serviceInterface, registration);
        }
        
        fireRegistrationAdded(registration);
        
        for(Class<?> serviceInterface : services) {
            fireProviderRegistered(serviceInterface, registration);
        }

        LOG.debug("Registered {} as instance of {}", serviceProvider, services);

        return registration;

    }
    
    private void fireRegistrationAdded(ServiceRegistration registration) {
    	for(RegistrationHook hook : m_hooks) {
    		hook.registrationAdded(registration);
    	}
	}

    private void fireRegistrationRemoved(ServiceRegistration registration) {
    	for(RegistrationHook hook : m_hooks) {
    		hook.registrationRemoved(registration);
    	}
	}
	private <T> Set<ServiceRegistration> getRegistrations(Class<T> serviceInterface) {
        Set<ServiceRegistration> copy = m_registrationMap.getCopy(serviceInterface);
        return (copy == null ? Collections.<ServiceRegistration>emptySet() : copy);
    }

    private void unregister(ServiceRegistration registration) {
        
        for(Class<?> serviceInterface : registration.getProvidedInterfaces()) {
            m_registrationMap.remove(serviceInterface, registration);
        }
        
        fireRegistrationRemoved(registration);
        
        for(Class<?> serviceInterface : registration.getProvidedInterfaces()) {
            fireProviderUnregistered(serviceInterface, registration);
        }

    }

    /** {@inheritDoc} */
    @Override
    public <T> void addListener(Class<T> service,  RegistrationListener<T> listener) {
        m_listenerMap.add(service, listener);
    }

    /** {@inheritDoc} */
    @Override
    public <T> void addListener(Class<T> service,  RegistrationListener<T> listener, boolean notifyForExistingProviders) {

        if (notifyForExistingProviders) {
            
            Set<ServiceRegistration> registrations = null;
            
            synchronized (m_registrationMap) {
                m_listenerMap.add(service, listener);
                registrations = getRegistrations(service);
            }
            
            for(ServiceRegistration registration : registrations) {
                listener.providerRegistered(registration, registration.getProvider(service));
            }
            
        } else {
            
            m_listenerMap.add(service, listener);
            
        }
    }

    /** {@inheritDoc} */
    @Override
    public <T> void removeListener(Class<T> service, RegistrationListener<T> listener) {
        m_listenerMap.remove(service, listener);
    }
    
    private <T> void fireProviderRegistered(Class<T> serviceInterface, Registration registration) {
        Set<RegistrationListener<T>> listeners = getListeners(serviceInterface);
        
        for(RegistrationListener<T> listener : listeners) {
            listener.providerRegistered(registration, registration.getProvider(serviceInterface));
        }
    }
    
    private <T> void fireProviderUnregistered(Class<T> serviceInterface, Registration registration) {
        Set<RegistrationListener<T>> listeners = getListeners(serviceInterface);
        
        for(RegistrationListener<T> listener : listeners) {
            listener.providerUnregistered(registration, registration.getProvider(serviceInterface));
        }
        
    }
    
    @SuppressWarnings("unchecked")
    private <T> Set<RegistrationListener<T>> getListeners(Class<T> serviceInterface) {
        Set<RegistrationListener<?>> listeners = m_listenerMap.getCopy(serviceInterface);
        return (Set<RegistrationListener<T>>) (listeners == null ? Collections.emptySet() : listeners);
    }

	@Override
	public void addRegistrationHook(RegistrationHook hook, boolean notifyForExistingProviders) {
        if (notifyForExistingProviders) {
            
            Set<ServiceRegistration> registrations = null;
            
            synchronized (m_registrationMap) {
            	m_hooks.add(hook);
                registrations = getAllRegistrations();
            }
            
            for(ServiceRegistration registration : registrations) {
            	hook.registrationAdded(registration);
            }
            
        } else {
            m_hooks.add(hook);
        }
	}

	@Override
	public void removeRegistrationHook(RegistrationHook hook) {
		m_hooks.remove(hook);
	}

	@Override
	public void unregisterAll(Class<?> clazz) {
		for (ServiceRegistration registration : getRegistrations(clazz)) {
			unregister(registration);
		}
	}

	private Set<ServiceRegistration> getAllRegistrations() {
		Set<ServiceRegistration> registrations = new LinkedHashSet<>();
		
		for(Set<ServiceRegistration> registrationSet: m_registrationMap.values()) {
			registrations.addAll(registrationSet);
		}
		
		return registrations;
		
	}

}
