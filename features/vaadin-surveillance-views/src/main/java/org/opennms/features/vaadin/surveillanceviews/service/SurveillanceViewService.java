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
package org.opennms.features.vaadin.surveillanceviews.service;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opennms.netmgt.config.surveillanceViews.Category;
import org.opennms.netmgt.config.surveillanceViews.View;
import org.opennms.netmgt.model.OnmsAlarm;
import org.opennms.netmgt.model.OnmsCategory;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.model.OnmsNotification;
import org.opennms.netmgt.model.OnmsResource;
import org.opennms.netmgt.model.OnmsResourceType;
import org.opennms.netmgt.model.ResourceId;
import org.opennms.netmgt.model.SurveillanceStatus;

import com.google.common.util.concurrent.ListeningExecutorService;

/**
 * Interface for the surveillance view service.
 *
 * @author Christian Pape
 */
public interface SurveillanceViewService {
    /**
     * Returns a list of the existing OpenNMS categories.
     *
     * @return the list of categories
     */
    List<OnmsCategory> getOnmsCategories();

    /**
     * Computes and returns the cell status used for displaying the surveillance view.
     *
     * @param view the view to use
     * @return the array of {@link SurveillanceStatus} instances
     */
    SurveillanceStatus[][] calculateCellStatus(final View view);

    /**
     * Returns a list of OpenNMS categories for a given collection of view categories
     *
     * @param viewCats the categories to search for
     * @return the set of OpenNMS categories
     */
    Set<OnmsCategory> getOnmsCategoriesFromViewCategories(final Collection<Category> viewCats);

    /**
     * Returns the list of OpenNMS alarm instances for a given set of row and column categories.
     *
     * @param rowCategories the row categories
     * @param colCategories the column categories
     * @return the list of alarms found
     */
    List<OnmsAlarm> getAlarmsForCategories(Set<OnmsCategory> rowCategories, Set<OnmsCategory> colCategories);

    /**
     * Returns the list og OpenNMS notification instacnes for a given set of row and column categories.
     *
     * @param rowCategories  the row categories
     * @param colCategories  the column categories
     * @param customSeverity the custom severity to be used
     * @return a list of notifications found
     */
    List<OnmsNotification> getNotificationsForCategories(Set<OnmsCategory> rowCategories, Set<OnmsCategory> colCategories, Map<OnmsNotification, String> customSeverity);

    /**
     * Returns the list of OpenNMS node instances for a given set of row and column categories.
     *
     * @param rowCategories the row categories
     * @param colCategories the column categories
     * @return the list of nodes found
     */
    List<OnmsNode> getNodesForCategories(Set<OnmsCategory> rowCategories, Set<OnmsCategory> colCategories);

    /**
     * Returns the list of RTC calculations for a given set of row and column categories.
     *
     * @param rowCategories the row categories
     * @param colCategories the column categories
     * @return the list of {@link org.opennms.features.vaadin.surveillanceviews.service.SurveillanceViewService.NodeRtc}
     */
    List<NodeRtc> getNodeRtcsForCategories(Set<OnmsCategory> rowCategories, Set<OnmsCategory> colCategories);

    /**
     * Returns a map of OpenNMS resource type/resource mappings for a given node id.
     *
     * @param nodeId the node id to search resources for
     * @return the mappings of resource types and resources
     */
    Map<OnmsResourceType, List<OnmsResource>> getResourceTypeMapForNodeId(final String nodeId);

    /**
     * Returns a map of graphs for a given resource id.
     *
     * @param resourceId the resource id to search graphs for
     * @return the map of graphs
     */
    Map<String, String> getGraphResultsForResourceId(final ResourceId resourceId);

    /**
     * Returns a map of OpenNMS resource type/resource mappings for a given node id.
     *
     * @param nodeId the node id to search resources for
     * @return the mappings of resource types and resources
     */
    Map<OnmsResourceType, List<OnmsResource>> getResourceTypeMapForNodeId(int nodeId);

    /**
     * Returns the default view for a given username.
     * <p/>
     * 1. If a view which name equals the username exists it will be returned
     * 2. If a view which name equals the user's group exists it will be returned
     * 3. the default view defined by the default-view attribute will be returned
     *
     * @param username the username to be used
     * @return the default view for this user
     */
    View selectDefaultViewForUsername(String username);

    /**
     * Returns the executor pool.
     *
     * @return the executor service pool
     */
    ListeningExecutorService getExecutorService();

    /**
     * Returns a node for a given id.
     *
     * @param id the node id
     * @return the node
     */
    OnmsNode getNodeForId(int id);

    /**
     * The class for storing node RTC calculations
     */
    class NodeRtc {
        /**
         * the format to be used for the availibility
         */
        private static final DecimalFormat AVAILABILITY_FORMAT = new DecimalFormat("0.000%");

        static {
            AVAILABILITY_FORMAT.setMultiplier(100);
        }

        /**
         * the node
         */
        private OnmsNode m_node;
        /**
         * the service count
         */
        private int m_serviceCount;
        /**
         * the count of services that are down
         */
        private int m_downServiceCount;
        /**
         * overall availability
         */
        private double m_availability;

        /**
         * Constructor for instantiating instances.
         *
         * @param node             the node to be used
         * @param serviceCount     the service count
         * @param downServiceCount the service down count
         * @param availability     the overall availability
         */
        public NodeRtc(OnmsNode node, int serviceCount, int downServiceCount, double availability) {
            m_node = node;
            m_serviceCount = serviceCount;
            m_downServiceCount = downServiceCount;
            m_availability = availability;
        }

        /**
         * Returns the availability as a double value.
         *
         * @return the availability
         */
        public double getAvailability() {
            return m_availability;
        }

        /**
         * Returns the availability as a formatted string.
         *
         * @return the formatted availability string
         */
        public String getAvailabilityAsString() {
            return AVAILABILITY_FORMAT.format(m_availability);
        }

        /**
         * Returns the down services count.
         *
         * @return the down services count.
         */
        public int getDownServiceCount() {
            return m_downServiceCount;
        }

        /**
         * Returns the node instance associated with this RTC calculation.
         *
         * @return the node
         */
        public OnmsNode getNode() {
            return m_node;
        }

        /**
         * Returns the service count.
         *
         * @return the service count.
         */
        public int getServiceCount() {
            return m_serviceCount;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return m_node.getLabel() + ": " + m_downServiceCount + " of " + m_serviceCount + ": " + getAvailabilityAsString();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            NodeRtc nodeRtc = (NodeRtc) o;

            if (Double.compare(nodeRtc.m_availability, m_availability) != 0) {
                return false;
            }
            if (m_downServiceCount != nodeRtc.m_downServiceCount) {
                return false;
            }
            if (m_serviceCount != nodeRtc.m_serviceCount) {
                return false;
            }
            if (m_node != null ? !m_node.equals(nodeRtc.m_node) : nodeRtc.m_node != null) {
                return false;
            }

            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            int result;
            long temp;
            result = m_node != null ? m_node.hashCode() : 0;
            result = 31 * result + m_serviceCount;
            result = 31 * result + m_downServiceCount;
            temp = Double.doubleToLongBits(m_availability);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }
    }
}
