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
package org.opennms.netmgt.provision.persist;

import java.io.File;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opennms.netmgt.provision.persist.foreignsource.ForeignSource;
import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * <p>
 * The fused foreign source repository always returns data from the deployed foreign source
 * repository.  When updating or deleting data, it always updates the deployed foreign source
 * repository, and deletes from the pending.
 * </p>
 * <p>
 * One thing to note -- if you are importing/saving a requisition to the fused foreign
 * source repository, any pending changes to the foreign source will be promoted to the
 * deployed repository as well.
 * </p>
 */
public class FusedForeignSourceRepository extends AbstractForeignSourceRepository implements ForeignSourceRepository, InitializingBean {
    private ForeignSourceRepository m_pendingForeignSourceRepository;
    private ForeignSourceRepository m_deployedForeignSourceRepository;

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(m_pendingForeignSourceRepository, "Pending foreign source repository must not be null.");
        Assert.notNull(m_deployedForeignSourceRepository, "Deployed foreign source repository must not be null.");
    }

    public ForeignSourceRepository getPendingForeignSourceRepository() {
        return m_pendingForeignSourceRepository;
    }
    
    public void setPendingForeignSourceRepository(final ForeignSourceRepository fsr) {
        m_pendingForeignSourceRepository = fsr;
    }

    public ForeignSourceRepository getDeployedForeignSourceRepository() {
        return m_deployedForeignSourceRepository;
    }
    
    public void setDeployedForeignSourceRepository(final ForeignSourceRepository fsr) {
        m_deployedForeignSourceRepository = fsr;
    }

    /**
     * <p>getActiveForeignSourceNames</p>
     *
     * @return a {@link java.util.Set} object.
     */
    @Override
    public synchronized Set<String> getActiveForeignSourceNames() {
        final Set<String> fsNames = new HashSet<String>(m_pendingForeignSourceRepository.getActiveForeignSourceNames());
        fsNames.addAll(m_deployedForeignSourceRepository.getActiveForeignSourceNames());
        return fsNames;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized Requisition importResourceRequisition(final Resource resource) throws ForeignSourceRepositoryException {
        final Requisition requisition = m_deployedForeignSourceRepository.importResourceRequisition(resource);
        final String foreignSource = requisition.getForeignSource();

        cleanUpDeployedForeignSources(foreignSource);
        cleanUpSnapshots(requisition);

        return requisition;
    }
    
    private synchronized void cleanUpDeployedForeignSources(String foreignSourceName) {
        ForeignSource deployed = m_deployedForeignSourceRepository.getForeignSource(foreignSourceName);
        ForeignSource pending = m_pendingForeignSourceRepository.getForeignSource(foreignSourceName);

        if (pending.isDefault()) {
            // if pending is default, assume deployed is valid, be it default or otherwise
            m_pendingForeignSourceRepository.delete(pending);
        } else {
            if (deployed.isDefault()) {
                // if pending is not default, and deployed is, assume pending should override deployed
                m_deployedForeignSourceRepository.save(pending);
            } else {
                // otherwise, compare dates, pending updates deployed if it's timestamp is newer
                Date pendingDate = pending.getDateStampAsDate();
                Date deployedDate = deployed.getDateStampAsDate();
                if (!deployedDate.after(pendingDate)) {
                    m_deployedForeignSourceRepository.save(pending);
                }
            }
        }
        m_pendingForeignSourceRepository.delete(pending);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void delete(ForeignSource foreignSource) throws ForeignSourceRepositoryException {
        m_pendingForeignSourceRepository.delete(foreignSource);
        m_deployedForeignSourceRepository.delete(foreignSource);
    }

    /**
     * <p>delete</p>
     *
     * @param requisition a {@link org.opennms.netmgt.provision.persist.requisition.Requisition} object.
     * @throws org.opennms.netmgt.provision.persist.ForeignSourceRepositoryException if any.
     */
    @Override
    public synchronized void delete(Requisition requisition) throws ForeignSourceRepositoryException {
        m_pendingForeignSourceRepository.delete(requisition);
        m_deployedForeignSourceRepository.delete(requisition);
    }

    /** {@inheritDoc} */
    @Override
    public ForeignSource getForeignSource(String foreignSourceName) throws ForeignSourceRepositoryException {
        return m_deployedForeignSourceRepository.getForeignSource(foreignSourceName);
    }

    /**
     * <p>getForeignSourceCount</p>
     *
     * @return a int.
     * @throws org.opennms.netmgt.provision.persist.ForeignSourceRepositoryException if any.
     */
    @Override
    public int getForeignSourceCount() throws ForeignSourceRepositoryException {
        return m_deployedForeignSourceRepository.getForeignSourceCount();
    }

    /**
     * <p>getForeignSources</p>
     *
     * @return a {@link java.util.Set} object.
     * @throws org.opennms.netmgt.provision.persist.ForeignSourceRepositoryException if any.
     */
    @Override
    public Set<ForeignSource> getForeignSources() throws ForeignSourceRepositoryException {
        return m_deployedForeignSourceRepository.getForeignSources();
    }

    /** {@inheritDoc} */
    @Override
    public Requisition getRequisition(String foreignSourceName) throws ForeignSourceRepositoryException {
        return m_deployedForeignSourceRepository.getRequisition(foreignSourceName);
    }

    /**
     * <p>getRequisition</p>
     *
     * @param foreignSource a {@link org.opennms.netmgt.provision.persist.foreignsource.ForeignSource} object.
     * @return a {@link org.opennms.netmgt.provision.persist.requisition.Requisition} object.
     * @throws org.opennms.netmgt.provision.persist.ForeignSourceRepositoryException if any.
     */
    @Override
    public Requisition getRequisition(ForeignSource foreignSource) throws ForeignSourceRepositoryException {
        return m_deployedForeignSourceRepository.getRequisition(foreignSource);
    }

    /** {@inheritDoc} */
    @Override
    public Date getRequisitionDate(String foreignSource) {
        return m_deployedForeignSourceRepository.getRequisitionDate(foreignSource);
    }

    /** {@inheritDoc} */
    @Override
    public URL getRequisitionURL(String foreignSource) {
        return m_deployedForeignSourceRepository.getRequisitionURL(foreignSource);
    }

    /**
     * <p>getRequisitions</p>
     *
     * @return a {@link java.util.Set} object.
     * @throws org.opennms.netmgt.provision.persist.ForeignSourceRepositoryException if any.
     */
    @Override
    public Set<Requisition> getRequisitions() throws ForeignSourceRepositoryException {
        return m_deployedForeignSourceRepository.getRequisitions();
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void save(ForeignSource foreignSource) throws ForeignSourceRepositoryException {
        m_deployedForeignSourceRepository.validate(foreignSource);
        m_pendingForeignSourceRepository.delete(foreignSource);
        m_deployedForeignSourceRepository.save(foreignSource);
    }

    /**
     * <p>save</p>
     *
     * @param requisition a {@link org.opennms.netmgt.provision.persist.requisition.Requisition} object.
     * @throws org.opennms.netmgt.provision.persist.ForeignSourceRepositoryException if any.
     */
    @Override
    public synchronized void save(final Requisition requisition) throws ForeignSourceRepositoryException {
        m_deployedForeignSourceRepository.validate(requisition);
        m_deployedForeignSourceRepository.save(requisition);
        cleanUpSnapshots(requisition);
    }

    private void cleanUpSnapshots(final Requisition requisition) {
        final String foreignSource = requisition.getForeignSource();
        final Date pendingDate = m_pendingForeignSourceRepository.getRequisitionDate(foreignSource);

        final List<File> pendingSnapshots = RequisitionFileUtils.findSnapshots(m_pendingForeignSourceRepository, foreignSource);

        if (pendingDate != null) {
            /* determine whether to delete the pending requisition */
            boolean deletePendingRequisition = true;
            if (pendingSnapshots.size() > 0) {
                for (final File pendingSnapshotFile : pendingSnapshots) {
                    if (isNewer(pendingSnapshotFile, pendingDate)) {
                        // the pending file is newer than an in-process snapshot, don't delete it
                        deletePendingRequisition = false;
                        break;
                    }
                }
            }
            if (deletePendingRequisition) {
                m_pendingForeignSourceRepository.delete(requisition);
            }
        }

        /* determine whether this requisition was imported from a snapshot, and if so, delete its snapshot file */
        RequisitionFileUtils.deleteResourceIfSnapshot(requisition);

        final Date deployedDate = m_deployedForeignSourceRepository.getRequisitionDate(foreignSource);
        if (deployedDate != null) {
            RequisitionFileUtils.deleteSnapshotsOlderThan(getPendingForeignSourceRepository(), foreignSource, deployedDate);
        }
    }

    private boolean isNewer(final File snap, final Date date) {
        return RequisitionFileUtils.isNewer(snap, date);
    }

    @Override
    public void flush() throws ForeignSourceRepositoryException {
        // Unnecessary, there is no caching/delayed writes in FusedForeignSourceRepository
        m_pendingForeignSourceRepository.flush();
        m_deployedForeignSourceRepository.flush();
    }

    @Override
    public void clear() throws ForeignSourceRepositoryException {
        m_pendingForeignSourceRepository.clear();
        m_deployedForeignSourceRepository.clear();
        super.clear();
    }
}
