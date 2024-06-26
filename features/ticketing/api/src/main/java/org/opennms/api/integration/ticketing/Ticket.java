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
package org.opennms.api.integration.ticketing;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenNMS Trouble Ticket Model class used to contain common ticket data
 * by implementations of <code>TicketerPlugin</code> API.
 *
 * @author <a href="mailto:brozow@opennms.org">Mathew Brozowski</a>
 * @author <a href="mailto:david@opennms.org">David Hustace</a>
 */
public class Ticket {
    
    /**
     * Enumeration for representation of a Ticket's state.
     * @author <a href="mailto:brozow@opennms.org">Mathew Brozowski</a>
     *
     */
    public enum State {
        /**
         * Ticket is considered to be in an Open State in the HelpDesk system.
         */
        OPEN,
        /**
         * Ticket is considered to be in a Canceled State in the HelpDesk system.
         */
        CANCELLED,
        /**
         * Ticket is considered to be in an Closed State in the HelpDesk system.
         */
        CLOSED
    }
    
    private String m_id;
    private Integer m_alarmId;
    private Integer m_nodeId;
    private InetAddress m_ipAddress;
    private State m_state = State.OPEN;
    private String m_summary;
    private String m_details;
    private String m_user;
    private String m_modificationTimestamp;
    private Map<String, String> m_attributes;
    private boolean m_isSituation = false;
    private List<RelatedAlarmSummary> m_relatedAlarms = new ArrayList<>();

    /**
     * <p>getAttributes</p>
     *
     * @return a Map of free from attributes of the ticket.  Typically,
     * from OnmsAlarm attributes.
     */
    public Map<String, String> getAttributes() {
        return m_attributes;
    }
    
    /**
     * Store a list of free form attributes in the Ticket.  Typically, from
     * the OnmsAlarm attributes.
     *
     * @param attributes a {@link java.util.Map} object.
     */
    public void setAttributes(Map<String, String> attributes) {
        m_attributes = attributes;
    }
    
    /**
     * Adds a single free form attribute to the Ticket.
     *
     * @param key a {@link java.lang.String} object.
     * @param value a {@link java.lang.String} object.
     */
    public void addAttribute(String key, String value) {
        if (m_attributes == null) {
            m_attributes = new HashMap<String, String>();
        }
        m_attributes.put(key, value);
    }
    
    /**
     * Gets a single free form attribute from a Ticket.
     *
     * @param key a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getAttribute(String key) {
        if (m_attributes == null) {
            return null;
        }
        return m_attributes.get(key);
    }
    
    /**
     * Returns a simple high level summary about the ticket that is generated
     * from the Alarm logmsg.
     *
     * @return A string containing the summary of the ticket.
     */
    public String getSummary() {
        return m_summary;
    }
    
    /**
     * Set a summary into the ticket.  Typically the alarm's logmsg.
     *
     * @param summary a {@link java.lang.String} object.
     */
    public void setSummary(String summary) {
        m_summary = summary;
    }
    
    /**
     * TODO: This should probably turn into a collection of comments.
     *
     * @return A string of details about the Ticket.
     */
    public String getDetails() {
        return m_details;
    }
    /**
     * TODO: This should probably turn into a collection of comments or some such thing.
     *
     * @param details a {@link java.lang.String} object.
     */
    public void setDetails(String details) {
        m_details = details;
    }
    
    /**
     * This should be the ticket ID assigned by the HelpDesk system.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getId() {
        return m_id;
    }

    public boolean hasId() {
        return m_id != null && !m_id.isEmpty();
    }

    public boolean hasAttributes() {
        return m_attributes != null && !m_attributes.isEmpty();
    }

    /**
     * The TicketerPlugin should set the ID.
     *
     * @param id a {@link java.lang.String} object.
     */
    public void setId(String id) {
        m_id = id;
    }
    
    /**
     * User name owning/createing the ticket.  Will be set initially to
     * the user name set in the parameter of the create ticket event.
     *
     * @return a String containing the user name that owns the ticket.
     */
    public String getUser() {
        return m_user;
    }
    
    /**
     * Set the user name owning the ticket.  Typically set by the TicketerServiceLayer
     * implemenation as the user name parameter from the create ticket event.
     *
     * @param user a {@link java.lang.String} object.
     */
    public void setUser(String user) {
        m_user = user;
    }
    
    /**
     * Returns the current <code>Ticket.State</code>
     *
     * @return the ticket state enum.
     */
    public State getState() {
        return m_state;
    }
    
    /**
     * Sets the Ticket state to one of the <code>Ticket.State</code> Enums.
     *
     * @param state a {@link org.opennms.api.integration.ticketing.Ticket.State} object.
     */
    public void setState(State state) {
        m_state = state;
    }

    /**
     * A timestamp to be used for optimistic locking with the trouble ticketing system
     *
     * @return a {@link java.lang.String} object.
     */
    public String getModificationTimestamp() {
        return m_modificationTimestamp;
    }

    /**
     * <p>setModificationTimestamp</p>
     *
     * @param modificationTimestamp a {@link java.lang.String} object.
     */
    public void setModificationTimestamp(String modificationTimestamp) {
        m_modificationTimestamp = modificationTimestamp;
    }

    /**
     * Returns the ID of the originator alarm
     *
     * @return the alarm ID.
     */
    public Integer getAlarmId() {
        return m_alarmId;
    }

    /**
     * Sets the ID of the originator alarm.
     *
     * @param alarmId a {@link java.lang.Integer} object.
     */
    public void setAlarmId(Integer alarmId) {
        this.m_alarmId = alarmId;
    }

    /**
     * Returns the ID of the originator node
     *
     * @return the node ID.
     */
    public Integer getNodeId() {
        return m_nodeId;
    }

    /**
     * Sets the ID of the originator node.
     *
     * @param nodeId a {@link java.lang.Integer} object.
     */
    public void setNodeId(Integer nodeId) {
        this.m_nodeId = nodeId;
    }

    /**
     * Returns the IP address of the originator alarm
     *
     * @return the IP address.
     */
    public InetAddress getIpAddress() {
        return m_ipAddress;
    }

    /**
     * Sets the IP address of the originator alarm.
     *
     * @param ipAddress a {@link java.net.InetAddress} object.
     */
    public void setIpAddress(InetAddress ipAddress) {
        this.m_ipAddress = ipAddress;
    }

    /**
     *
     * @return boolean depending on whether alarm is situation or not. default is false.
     */
    public boolean isSituation() {
        return m_isSituation;
    }

    /**
     *  Set as true if this alarm is a situation.
     *
     * @param situation  whether or not this alarm is a situation.
     */
    public void setSituation(boolean situation) {
        m_isSituation = situation;
    }

    /**
     *
     * @return a {@link List} of {@link RelatedAlarmSummary} containing related alarm details
     */
    public List<RelatedAlarmSummary> getRelatedAlarms() {
        return m_relatedAlarms;
    }

    /**
     * Set related alarms for the current alarm associated with ticket.
     *
     * @param relatedAlarms a {@link List} of {@link RelatedAlarmSummary}
     */
    public void setRelatedAlarms(List<RelatedAlarmSummary> relatedAlarms) {
        m_relatedAlarms = relatedAlarms;
    }

    @Override
    public String toString() {
        return String.format("Ticket[id=%s, alarmId=%d, nodeId=%d, ipAddress=%s, state=%s, "
                + "summary=%s, details=%s, user=%s, modificationTimestamp=%s, attributes=%s relatedAlarms=%s]",
                m_id, m_alarmId, m_nodeId, m_ipAddress, m_state, m_summary, m_details, m_user,
                m_modificationTimestamp, m_attributes, m_relatedAlarms);
    }
}
