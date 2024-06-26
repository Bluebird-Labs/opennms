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

import java.net.URL;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.opennms.netmgt.provision.persist.foreignsource.ForeignSource;
import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

public class QueueingForeignSourceRepository implements ForeignSourceRepository, InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(QueueingForeignSourceRepository.class);

    private final ConcurrentMap<String,Requisition> m_pendingRequisitions     = new ConcurrentHashMap<String,Requisition>();
    private final ConcurrentMap<String,ForeignSource> m_pendingForeignSources = new ConcurrentHashMap<String,ForeignSource>();
    ForeignSourceRepository m_repository = null;
    private ExecutorService m_executor = Executors.newSingleThreadExecutor();

    public QueueingForeignSourceRepository() {
        super();
    }

    @Override
    public void flush() throws ForeignSourceRepositoryException {
        LOG.debug("flushing queue");
        // wait for everything currently in the queue to complete

        final CountDownLatch latch = new CountDownLatch(1);
        m_executor.execute(new Runnable() {
            @Override
            public void run() {
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (final InterruptedException e) {
            LOG.debug("Interrupted while waiting for ForeignSourceRepository flush.  Returning.", e);
            return;
        }
        LOG.debug("finished flushing queue");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(m_repository, "No foreign source repository was set!");
    }

    public ForeignSourceRepository getForeignSourceRepository() {
        return m_repository;
    }

    public void setForeignSourceRepository(final ForeignSourceRepository fsr) {
        m_repository = fsr;
    }

    @Override
    public Set<String> getActiveForeignSourceNames() {
        return m_repository.getActiveForeignSourceNames();
    }

    @Override
    public int getForeignSourceCount() throws ForeignSourceRepositoryException {
        return getActiveForeignSourceNames().size();
    }

    @Override
    public Set<ForeignSource> getForeignSources() throws ForeignSourceRepositoryException {
        return m_repository.getForeignSources();
    }

    @Override
    public ForeignSource getForeignSource(final String foreignSourceName) throws ForeignSourceRepositoryException {
        return m_repository.getForeignSource(foreignSourceName);
    }

    @Override
    public void save(final ForeignSource foreignSource) throws ForeignSourceRepositoryException {
        LOG.debug("Queueing save of foreign source {}", foreignSource.getName());
        validate(foreignSource);
        m_pendingForeignSources.put(foreignSource.getName(), foreignSource);
        m_executor.execute(new QueuePersistRunnable());
    }

    @Override
    public void delete(final ForeignSource foreignSource) throws ForeignSourceRepositoryException {
        LOG.debug("Queueing delete of foreign source {}", foreignSource.getName());
        m_pendingForeignSources.put(foreignSource.getName(), new DeletedForeignSource(foreignSource));
        m_executor.execute(new QueuePersistRunnable());
    }

    @Override
    public Set<Requisition> getRequisitions() throws ForeignSourceRepositoryException {
        return m_repository.getRequisitions();
    }

    @Override
    public Requisition getRequisition(final String foreignSourceName) throws ForeignSourceRepositoryException {
        return m_repository.getRequisition(foreignSourceName);
    }

    @Override
    public Requisition getRequisition(final ForeignSource foreignSource) throws ForeignSourceRepositoryException {
        return m_repository.getRequisition(foreignSource);
    }

    @Override
    public Date getRequisitionDate(final String foreignSource) {
        if (m_pendingRequisitions.containsKey(foreignSource)) {
            return m_pendingRequisitions.get(foreignSource).getDate();
        }
        return m_repository.getRequisitionDate(foreignSource);
    }

    @Override
    public URL getRequisitionURL(final String foreignSource) {
        return m_repository.getRequisitionURL(foreignSource);
    }

    @Override
    public void save(final Requisition requisition) throws ForeignSourceRepositoryException {
        LOG.debug("Queueing save of requisition {} (containing {} nodes)", requisition.getForeignSource(), requisition.getNodeCount());
        validate(requisition);
        m_pendingRequisitions.put(requisition.getForeignSource(), requisition);
        m_executor.execute(new QueuePersistRunnable());
    }

    @Override
    public void delete(final Requisition requisition) throws ForeignSourceRepositoryException {
        LOG.debug("Queueing delete of requistion {}", requisition.getForeignSource());
        m_pendingRequisitions.put(requisition.getForeignSource(), new DeletedRequisition(requisition));
        m_executor.execute(new QueuePersistRunnable());
    }

    @Override
    public ForeignSource getDefaultForeignSource() throws ForeignSourceRepositoryException {
        return m_repository.getDefaultForeignSource();
    }

    @Override
    public void putDefaultForeignSource(final ForeignSource foreignSource) throws ForeignSourceRepositoryException {
        m_repository.putDefaultForeignSource(foreignSource);
    }

    @Override
    public void resetDefaultForeignSource() throws ForeignSourceRepositoryException {
        m_repository.resetDefaultForeignSource();
    }

    @Override
    public Requisition importResourceRequisition(final Resource resource) throws ForeignSourceRepositoryException {
        return m_repository.importResourceRequisition(resource);
    }

    @Override
    public OnmsNodeRequisition getNodeRequisition(final String foreignSource, final String foreignId) throws ForeignSourceRepositoryException {
        return m_repository.getNodeRequisition(foreignSource, foreignId);
    }

    @Override
    public void validate(final ForeignSource foreignSource) throws ForeignSourceRepositoryException {
        m_repository.validate(foreignSource);
    }

    @Override
    public void validate(final Requisition requisition) throws ForeignSourceRepositoryException {
        m_repository.validate(requisition);
    }

    @Override
    public void clear() throws ForeignSourceRepositoryException {
        m_pendingForeignSources.clear();
        m_pendingRequisitions.clear();
        for (final Requisition req : getRequisitions()) {
            if (req != null) delete(req);
        }
        for (final ForeignSource fs : getForeignSources()) {
            if (fs != null) delete(fs);
        }
        m_executor.execute(new QueuePersistRunnable());
    }

    private final class QueuePersistRunnable implements Runnable {
        @Override
        public void run() {
            try {
                LOG.debug("persisting repository changes");
                final Set<Entry<String,ForeignSource>> foreignSources = m_pendingForeignSources.entrySet();
                final Set<Entry<String,Requisition>>   requisitions   = m_pendingRequisitions.entrySet();

                LOG.debug("* {} pending foreign sources", m_pendingForeignSources.size());
                LOG.debug("* {} pending requisitions",    m_pendingRequisitions.size());

                for (final Entry<String,ForeignSource> entry : foreignSources) {
                    final String foreignSourceName = entry.getKey();
                    final ForeignSource foreignSource = entry.getValue();

                    if (foreignSource instanceof DeletedForeignSource) {
                        final DeletedForeignSource deletedForeignSource = (DeletedForeignSource)foreignSource;
                        m_repository.delete(deletedForeignSource.getOriginal());
                    } else {
                        m_repository.save(foreignSource);
                    }
                    m_pendingForeignSources.remove(foreignSourceName, foreignSource);
                }

                for (final Entry<String,Requisition> entry : requisitions) {
                    final String foreignSourceName = entry.getKey();
                    final Requisition requisition = entry.getValue();

                    if (requisition instanceof DeletedRequisition) {
                        final DeletedRequisition deletedRequisition = (DeletedRequisition)requisition;
                        m_repository.delete(deletedRequisition.getOriginal());
                    } else {
                        m_repository.save(requisition);
                    }
                    m_pendingRequisitions.remove(foreignSourceName, requisition);
                }

                LOG.debug("finished persisting repository changes");
            } finally {
                //
            }
        }
    }

    private static final class DeletedForeignSource extends ForeignSource {
        private static final long serialVersionUID = -1484921681168837826L;
        private final ForeignSource m_foreignSource;

        public DeletedForeignSource(final ForeignSource foreignSource) {
            m_foreignSource = foreignSource;
            setName(foreignSource.getName());
        }

        public ForeignSource getOriginal() {
            return m_foreignSource;
        }
    }

    private static final class DeletedRequisition extends Requisition {
        private static final long serialVersionUID = -19738304185310191L;
        private final Requisition m_requisition;

        public DeletedRequisition(final Requisition requisition) {
            m_requisition = requisition;
            setForeignSource(requisition.getForeignSource());
        }

        public Requisition getOriginal() {
            return m_requisition;
        }
    }

}
