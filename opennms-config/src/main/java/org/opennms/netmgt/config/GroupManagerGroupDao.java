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

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.opennms.core.utils.OwnedIntervalSequence;
import org.opennms.core.xml.MarshallingExceptionTranslator;
import org.opennms.netmgt.config.groups.Group;
import org.opennms.netmgt.config.groups.Role;
import org.opennms.netmgt.config.groups.Schedule;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.util.Assert;

/**
 * <p>GroupManagerGroupDao class.</p>
 *
 * @author <a href="mailto:dj@opennms.org">DJ Gregor</a>
 * @version $Id: $
 */
public class GroupManagerGroupDao implements GroupDao, InitializingBean {
    private static final GroupManagerConfigObjectExceptionTranslator CONFIG_OBJECT_EXCEPTION_TRANSLATOR = new GroupManagerConfigObjectExceptionTranslator();
    
    private GroupManager m_groupManager;
    
    /** {@inheritDoc} */
    @Override
    public void deleteGroup(String name) {
        try {
            m_groupManager.deleteGroup(name);
        } catch (Throwable e) {
            throw CONFIG_OBJECT_EXCEPTION_TRANSLATOR.translate("deleting group '" + name + "'", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void deleteRole(String name) {
        try {
            m_groupManager.deleteRole(name);
        } catch (Throwable e) {
            throw CONFIG_OBJECT_EXCEPTION_TRANSLATOR.translate("deleting role '" + name + "'", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void deleteUser(String name) {
        try {
            m_groupManager.deleteUser(name);
        } catch (Throwable e) {
            throw CONFIG_OBJECT_EXCEPTION_TRANSLATOR.translate("deleting user '" + name + "'", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<Group> findGroupsForUser(String user) {
        return m_groupManager.findGroupsForUser(user);
    }

    /** {@inheritDoc} */
    @Override
    public Group getGroup(String name) {
        try {
            return m_groupManager.getGroup(name);
        } catch (Throwable e) {
            throw CONFIG_OBJECT_EXCEPTION_TRANSLATOR.translate("getting group '" + name + "'", e);
        }
    }

    /**
     * <p>getGroupNames</p>
     *
     * @return a {@link java.util.List} object.
     */
    @Override
    public List<String> getGroupNames() {
        try {
            return m_groupManager.getGroupNames();
        } catch (Throwable e) {
            throw CONFIG_OBJECT_EXCEPTION_TRANSLATOR.translate("getting group names", e);
        }
    }

    /**
     * <p>getGroups</p>
     *
     * @return a {@link java.util.Map} object.
     */
    @Override
    public Map<String, Group> getGroups() {
        try {
            return m_groupManager.getGroups();
        } catch (Throwable e) {
            throw CONFIG_OBJECT_EXCEPTION_TRANSLATOR.translate("getting groups", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Role getRole(String name) {
        try {
            return m_groupManager.getRole(name);
        } catch (Throwable e) {
            throw CONFIG_OBJECT_EXCEPTION_TRANSLATOR.translate("getting role '" + name + "'", e);
        }
    }

    /**
     * <p>getRoleNames</p>
     *
     * @return an array of {@link java.lang.String} objects.
     */
    @Override
    public String[] getRoleNames() {
        return m_groupManager.getRoleNames();
    }

    /** {@inheritDoc} */
    @Override
    public OwnedIntervalSequence getRoleScheduleEntries(String role, Date start, Date end) {
        try {
            return m_groupManager.getRoleScheduleEntries(role, start, end);
        } catch (Throwable e) {
            throw CONFIG_OBJECT_EXCEPTION_TRANSLATOR.translate("getting scheduled entries for role '" + role + "' between " + start + " and " + end, e);
        }
    }

    /**
     * <p>getRoles</p>
     *
     * @return a {@link java.util.Collection} object.
     */
    @Override
    public Collection<Role> getRoles() {
        try {
            return m_groupManager.getRoles();
        } catch (Throwable e) {
            throw CONFIG_OBJECT_EXCEPTION_TRANSLATOR.translate("getting roles", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<Schedule> getSchedulesForRoleAt(String role, Date time) {
        try {
            return m_groupManager.getSchedulesForRoleAt(role, time);
        } catch (Throwable e) {
            throw CONFIG_OBJECT_EXCEPTION_TRANSLATOR.translate("getting schedules for role '" + role + "' at " + time, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<Schedule> getUserSchedulesForRole(String user, String role) {
        try {
            return m_groupManager.getUserSchedulesForRole(user, role);
        } catch (Throwable e) {
            throw CONFIG_OBJECT_EXCEPTION_TRANSLATOR.translate("getting user schedules for user '" + user + "' for role '" + role + "'", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public long groupNextOnDuty(String group, Calendar time) {
        try {
            return m_groupManager.groupNextOnDuty(group, time);
        } catch (Throwable e) {
            throw CONFIG_OBJECT_EXCEPTION_TRANSLATOR.translate("getting next on duty time for group '" + group + "' after " + time, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasGroup(String name) {
        try {
            return m_groupManager.hasGroup(name);
        } catch (Throwable e) {
            throw CONFIG_OBJECT_EXCEPTION_TRANSLATOR.translate("getting group '" + name + "'", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isGroupOnDuty(String group, Calendar time) {
        try {
            return m_groupManager.isGroupOnDuty(group, time);
        } catch (Throwable e) {
            throw CONFIG_OBJECT_EXCEPTION_TRANSLATOR.translate("getting group '" + group + "' to see if it is on duty at " + time, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isUserScheduledForRole(String user, String role, Date time) {
        try {
            return m_groupManager.isUserScheduledForRole(user, role, time);
        } catch (Throwable e) {
            throw CONFIG_OBJECT_EXCEPTION_TRANSLATOR.translate("checking to see if user '" + user + "' is schedule for role '" + role + "' at " + time, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void renameGroup(String oldName, String newName) {
        try {
            m_groupManager.renameGroup(oldName, newName);
        } catch (Throwable e) {
            throw CONFIG_OBJECT_EXCEPTION_TRANSLATOR.translate("renaming group from '" + oldName + "' to '" + newName + "'", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void renameUser(String oldName, String newName) {
        try {
            m_groupManager.renameUser(oldName, newName);
        } catch (Throwable e) {
            throw CONFIG_OBJECT_EXCEPTION_TRANSLATOR.translate("renaming user from '" + oldName + "' to '" + newName + "'", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void saveGroup(String name, Group details) {
        try {
            m_groupManager.saveGroup(name, details);
        } catch (Throwable e) {
            throw CONFIG_OBJECT_EXCEPTION_TRANSLATOR.translate("saving group '" + name + "' with details " + details, e);
        }
    }

    /**
     * <p>saveGroups</p>
     */
    @Override
    public void saveGroups() {
        try {
            m_groupManager.saveGroups();
        } catch (Throwable e) {
            throw CONFIG_OBJECT_EXCEPTION_TRANSLATOR.translate("saving groups", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void saveRole(Role name) {
        try {
            m_groupManager.saveRole(name);
        } catch (Throwable e) {
            throw CONFIG_OBJECT_EXCEPTION_TRANSLATOR.translate("saving role '" + name + "'", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setGroups(Map<String, Group> groups) {
        m_groupManager.setGroups(groups);
    }

    /** {@inheritDoc} */
    @Override
    public boolean userHasRole(String user, String role) {
        try {
            return m_groupManager.userHasRole(user, role);
        } catch (Throwable e) {
            throw CONFIG_OBJECT_EXCEPTION_TRANSLATOR.translate("checking to see if user '" + user + "' has role '" + role + "'", e);
        }
    }
    
    /**
     * <p>afterPropertiesSet</p>
     */
    @Override
    public void afterPropertiesSet() {
        Assert.state(m_groupManager != null, "groupManager property must be set and be non-null");
    }

    /**
     * <p>getGroupManager</p>
     *
     * @return a {@link org.opennms.netmgt.config.GroupManager} object.
     */
    public GroupManager getGroupManager() {
        return m_groupManager;
    }

    /**
     * <p>setGroupManager</p>
     *
     * @param groupManager a {@link org.opennms.netmgt.config.GroupManager} object.
     */
    public void setGroupManager(GroupManager groupManager) {
        m_groupManager = groupManager;
    }
    
    public static class GroupManagerConfigObjectExceptionTranslator extends MarshallingExceptionTranslator {
        public DataAccessException translate(String task, Throwable e) {
            return new ConfigObjectRetrievalFailureException("General error while " + task + ": " + e, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getDefaultMapForUser(String user) {
        for (Group group: findGroupsForUser(user)) {
            if (group.getDefaultMap().isPresent()) {
                return group.getDefaultMap().get();
            }
        }
        return null;
    }

}
