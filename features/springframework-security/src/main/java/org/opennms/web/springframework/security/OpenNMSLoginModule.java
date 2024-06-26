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
package org.opennms.web.springframework.security;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.opennms.netmgt.config.api.UserConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;

public class OpenNMSLoginModule implements LoginModule, LoginHandler, OpenNMSLoginHandler {
    private static final transient Logger LOG = LoggerFactory.getLogger(OpenNMSLoginModule.class);

    private static transient volatile UserConfig m_userConfig;
    private static transient volatile SpringSecurityUserDao m_springSecurityUserDao;

    protected Subject m_subject;
    protected CallbackHandler m_callbackHandler;
    protected Map<String, ?> m_sharedState;
    protected Map<String, ?> m_options;

    protected String m_user;
    protected Set<Principal> m_principals = new HashSet<>();

    @Override
    public void initialize(final Subject subject, final CallbackHandler callbackHandler, final Map<String, ?> sharedState, final Map<String, ?> options) {
        LOG.info("OpenNMS Login Module initializing.");
        m_subject = subject;
        m_callbackHandler = callbackHandler;
        m_sharedState = sharedState;
        m_options = options;
    }

    @Override
    public boolean login() throws LoginException {
        return LoginModuleUtils.doLogin(this, m_subject, m_sharedState, m_options);
    }

    @Override
    public boolean abort() throws LoginException {
        LOG.debug("Aborting {} login.", m_user);
        m_user = null;
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        LOG.debug("Logging out user {}.", m_user);
        m_subject.getPrincipals().removeAll(m_principals);
        m_principals.clear();
        return true;
    }

    public static synchronized UserConfig getUserConfig() {
        return m_userConfig;
    }

    public static synchronized void setUserConfig(final UserConfig userConfig) {
        m_userConfig = userConfig;
    }

    public static synchronized SpringSecurityUserDao getSpringSecurityUserDao() {
        return m_springSecurityUserDao;
    }

    public static synchronized void setSpringSecurityUserDao(final SpringSecurityUserDao userDao) {
        m_springSecurityUserDao = userDao;
    }

    @Override
    public boolean commit() throws LoginException {
        final Set<Principal> principals = principals();
        if (principals.isEmpty()) {
            return false;
        }
        m_subject.getPrincipals().addAll(principals);
        return true;
    }

    public CallbackHandler callbackHandler() {
        return m_callbackHandler;
    }

    @Override
    public UserConfig userConfig() {
        return m_userConfig;
    }

    @Override
    public SpringSecurityUserDao springSecurityUserDao() {
        return m_springSecurityUserDao;
    }

    @Override
    public String user() {
        return m_user;
    }

    @Override
    public void setUser(final String user) {
        m_user = user;
    }

    public Set<Principal> createPrincipals(final GrantedAuthority authority) {
        return Collections.singleton(new AuthorityPrincipal(authority));
    }

    @Override
    public Set<Principal> principals() {
        return m_principals;
    }

    @Override
    public void setPrincipals(final Set<Principal> principals) {
        m_principals = principals;
    }

    @Override
    public boolean requiresAdminRole() {
        // this LoginHandler is used for JMX access, allow JMX to handle checking roles for authority
        return false;
    }
}
