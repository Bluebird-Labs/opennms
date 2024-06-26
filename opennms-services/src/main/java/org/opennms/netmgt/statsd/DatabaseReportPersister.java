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
package org.opennms.netmgt.statsd;

import java.util.Date;

import org.opennms.netmgt.dao.api.ResourceReferenceDao;
import org.opennms.netmgt.dao.api.StatisticsReportDao;
import org.opennms.netmgt.model.AttributeStatistic;
import org.opennms.netmgt.model.ResourceReference;
import org.opennms.netmgt.model.StatisticsReport;
import org.opennms.netmgt.model.StatisticsReportData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * <p>DatabaseReportPersister class.</p>
 *
 * @author <a href="mailto:dj@opennms.org">DJ Gregor</a>
 * @version $Id: $
 */
public class DatabaseReportPersister implements ReportPersister, InitializingBean {
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseReportPersister.class);
    private StatisticsReportDao m_statisticsReportDao;
    private ResourceReferenceDao m_resourceReferenceDao;

    /** {@inheritDoc} */
    @Override
    public void persist(ReportInstance report) {
        StatisticsReport dbReport = new StatisticsReport();
        dbReport.setName(report.getName());
        dbReport.setDescription(report.getDescription());
        dbReport.setStartDate(new Date(report.getStartTime()));
        dbReport.setEndDate(new Date(report.getEndTime()));
        dbReport.setJobStartedDate(report.getJobStartedDate());
        dbReport.setJobCompletedDate(report.getJobCompletedDate());
        dbReport.setPurgeDate(new Date(report.getJobCompletedDate().getTime() + report.getRetainInterval()));

        for (AttributeStatistic stat : report.getResults()) {
            ResourceReference resource = getResourceReference(stat.getAttribute().getResource().getId().toString());

            StatisticsReportData data = new StatisticsReportData();
            data.setResource(resource);
            data.setReport(dbReport);
            data.setValue(stat.getStatistic());
            dbReport.addData(data);
            LOG.debug("Adding {}", data);
        }
        
        if (dbReport.getData().isEmpty()) {
            LOG.warn("Cannot store {} because it doesn't contain data. Probably all the metrics are NaN for the report period.", report);
        } else {
            m_statisticsReportDao.save(dbReport);
        }
    }

    private ResourceReference getResourceReference(String id) {
        ResourceReference resource = m_resourceReferenceDao.getByResourceId(id);
        if (resource != null) {
            return resource;
        }
      
        resource = new ResourceReference();
        resource.setResourceId(id);
        m_resourceReferenceDao.save(resource);

        return resource;
    }

    /**
     * <p>afterPropertiesSet</p>
     */
    @Override
    public void afterPropertiesSet() {
        Assert.state(m_statisticsReportDao != null, "property statisticsReportDao must be set to a non-null value");
        Assert.state(m_resourceReferenceDao != null, "property resourceReferenceDao must be set to a non-null value");
    }
    
    /**
     * <p>getStatisticsReportDao</p>
     *
     * @return a {@link org.opennms.netmgt.dao.api.StatisticsReportDao} object.
     */
    public StatisticsReportDao getStatisticsReportDao() {
        return m_statisticsReportDao;
    }

    /**
     * <p>setStatisticsReportDao</p>
     *
     * @param statisticsReportDao a {@link org.opennms.netmgt.dao.api.StatisticsReportDao} object.
     */
    public void setStatisticsReportDao(StatisticsReportDao statisticsReportDao) {
        m_statisticsReportDao = statisticsReportDao;
    }

    /**
     * <p>getResourceReferenceDao</p>
     *
     * @return a {@link org.opennms.netmgt.dao.api.ResourceReferenceDao} object.
     */
    public ResourceReferenceDao getResourceReferenceDao() {
        return m_resourceReferenceDao;
    }

    /**
     * <p>setResourceReferenceDao</p>
     *
     * @param resourceReferenceDao a {@link org.opennms.netmgt.dao.api.ResourceReferenceDao} object.
     */
    public void setResourceReferenceDao(ResourceReferenceDao resourceReferenceDao) {
        m_resourceReferenceDao = resourceReferenceDao;
    }
}
