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
package org.opennms.netmgt.collectd.tca;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.opennms.core.rpc.api.RequestRejectedException;
import org.opennms.core.rpc.api.RequestTimedOutException;
import org.opennms.features.distributed.kvstore.api.BlobStore;
import org.opennms.netmgt.collectd.SnmpCollectionAgent;
import org.opennms.netmgt.collection.api.AttributeType;
import org.opennms.netmgt.collection.api.CollectionException;
import org.opennms.netmgt.collection.api.CollectionResource;
import org.opennms.netmgt.collection.api.CollectionSet;
import org.opennms.netmgt.collection.api.CollectionUnknown;
import org.opennms.netmgt.collection.support.builder.CollectionSetBuilder;
import org.opennms.netmgt.collection.support.builder.GenericTypeResource;
import org.opennms.netmgt.collection.support.builder.NodeLevelResource;
import org.opennms.netmgt.config.api.ResourceTypesDao;
import org.opennms.netmgt.config.datacollection.ResourceType;
import org.opennms.netmgt.model.ResourcePath;
import org.opennms.netmgt.model.ResourceTypeUtils;
import org.opennms.netmgt.rrd.RrdRepository;
import org.opennms.netmgt.snmp.SnmpObjId;
import org.opennms.netmgt.snmp.proxy.LocationAwareSnmpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class TcaCollectionSet.
 *
 * <p>The resource type is fixed to <code>juniperTcaEntry</code></p>
 * <p>This requires to define a datacollection-group like this:</p>
 * <pre>
 * &lt;datacollection-group name="Juniper TCA"&gt;
 *    &lt;resourceType name="juniperTcaEntry" label="Juniper TCA Entry" resourceLabel="Peer ${index}"&gt;
 *     &lt;persistenceSelectorStrategy class="org.opennms.netmgt.collection.support.PersistAllSelectorStrategy"/&gt;
 *     &lt;storageStrategy class="org.opennms.netmgt.collection.support.IndexStorageStrategy"/&gt;
 *   &lt;/resourceType&gt;
 * &lt;/datacollection-group&gt;
 * </pre>
 * <p>Note: the persistenceSelectorStrategy and storageStrategy won't be used by the collector.</p>
 *
 * @author Alejandro Galue <agalue@opennms.org>
 */
public class TcaCollectionHandler {
	private static final Logger LOG = LoggerFactory.getLogger(TcaCollectionHandler.class);

	/** The Constant RESOURCE_TYPE_NAME. */
	public static final String RESOURCE_TYPE_NAME = "juniperTcaEntry";

	/** The Constant LAST_TIMESTAMP. */
	public static final String LAST_TIMESTAMP = "__tcaLastTimestamp";

	/** The Constant INBOUND_DELAY. */
	public static final String INBOUND_DELAY = "inboundDelay";

	/** The Constant INBOUND_JITTER. */
	public static final String INBOUND_JITTER = "inboundJitter";

	/** The Constant OUTBOUND_DELAY. */
	public static final String OUTBOUND_DELAY = "outboundDelay";

	/** The Constant OUTBOUND_JITTER. */
	public static final String OUTBOUND_JITTER = "outboundJitter";

	/** The Constant TIMESYNC_STATUS. */
	public static final String TIMESYNC_STATUS = "timesyncStatus";


	/** The Collection Agent. */
	private SnmpCollectionAgent m_agent;

	private final RrdRepository m_repository;

    private final ResourceType m_resourceType;

    private final LocationAwareSnmpClient m_locationAwareSnmpClient;

	private final BlobStore m_blobStore;

	/**
	 * Instantiates a new TCA collection set.
	 *
	 * @param agent the agent
	 * @param repository the repository
	 */
	public TcaCollectionHandler(SnmpCollectionAgent agent, RrdRepository repository,
	        ResourceTypesDao resourceTypesDao, LocationAwareSnmpClient locationAwareSnmpClient, BlobStore blobStore) {
		m_agent = Objects.requireNonNull(agent);
		m_repository = Objects.requireNonNull(repository);
		m_resourceType = Objects.requireNonNull(resourceTypesDao).getResourceTypeByName(RESOURCE_TYPE_NAME);
		if (m_resourceType == null) {
		    throw new IllegalArgumentException("No resource of type juniperTcaEntry is defined.");
		}
		m_locationAwareSnmpClient = Objects.requireNonNull(locationAwareSnmpClient);
		m_blobStore = Objects.requireNonNull(blobStore);
	}

	/**
	 * Collect.
	 *
	 * @throws CollectionException the collection exception
	 */
	protected CollectionSet collect() throws CollectionException {
		try {
			CollectionSetBuilder builder = new CollectionSetBuilder(m_agent);
			TcaData tracker = new TcaData(m_agent.getAddress());
            CompletableFuture<TcaData> future = m_locationAwareSnmpClient.walk(m_agent.getAgentConfig(), tracker)
                    .withDescription("TcaCollector for " + m_agent.getHostAddress())
                    .withLocation(m_agent.getLocationName())
                    .execute();
            LOG.debug("collect: successfully instantiated TCA Collector for {}", m_agent.getHostAddress());
            future.get();
            LOG.info("collect: node TCA query for address {} complete.", m_agent.getHostAddress());
			process(tracker, builder);
			return builder.build();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new CollectionUnknown("Collection of node TCA data for interface " + m_agent.getHostAddress() + " interrupted: " + e, e);
		} catch (ExecutionException e) {
            final Throwable cause = e.getCause();
            if (cause != null && cause instanceof RequestTimedOutException) {
                throw new CollectionUnknown(String.format("No response received when remotely collecting TCA data"
                        + " for interface %s at location %s interrupted.",
                        m_agent.getHostAddress(), m_agent.getLocationName()), e);
            } else if (cause != null && cause instanceof RequestRejectedException) {
                throw new CollectionUnknown(String.format("The request to remotely collect TCA data"
                        + " for interface %s at location %s was rejected.",
                        m_agent.getHostAddress(), m_agent.getLocationName()), e);
            }
            throw new CollectionException(String.format("Unexpected exception when collecting TCA data for interface %s at location %s.",
                    m_agent.getHostAddress(), m_agent.getLocationName()), e);
        }
	}

	/**
	 * Process.
	 * 
	 * <p>A sample TCA Data looks like the following:</p>
	 * <ul>
	 * <li>OID=.1.3.6.1.4.1.27091.3.1.6.1.1.172.19.37.60.1, Type=OctetString, Value=172.19.37.60 </li>
	 * <li>OID=.1.3.6.1.4.1.27091.3.1.6.1.2.172.19.37.60.1, Type=OctetString, Value=
	 * |25|1327451762,11,0,11,0,1|1327451763,11,0,11,0,1|1327451764,11,0,11,0,1|1327451765,11,0,11,0,1|1327451766,11,0,11,0,1|
	 * 1327451767,11,0,11,0,1|1327451768,11,0,11,0,1|1327451769,11,0,11,0,1|1327451770,11,0,11,0,1|1327451771,11,0,11,0,1|
	 * 1327451772,11,0,11,0,1|1327451773,11,0,11,0,1|1327451774,11,0,11,0,1|1327451775,11,0,11,0,1|1327451776,11,0,11,0,1|
	 * 1327451777,11,0,11,0,1|1327451778,11,0,11,0,1|1327451779,11,0,11,0,1|1327451780,11,0,11,0,1|1327451781,11,0,11,0,1|
	 * 1327451782,11,0,11,0,1|1327451783,11,0,11,0,1|1327451784,11,0,11,0,1|1327451785,11,0,11,0,1|1327451786,11,0,11,0,1|</li>
	 * </ul>
	 * 
	 * <ul>
	 * <li>timestamp (epoch)</li>
	 * <li>delay local-remote ~ current inbound-delay</li>
	 * <li>jitter local-remote ~ current inbound-jitter</li>
	 * <li>delay remote-local ~ current outbound-delay</li>
	 * <li>jitter remote-local ~ current outbound-jitter-</li>
	 * <li>timesync status (1 = good, time is synced, 0 = bad, out-of sync)</li>
	 * </ul>
	 *
	 * @param tracker the tracker
	 * @throws Exception the exception
	 */
	private void process(TcaData tracker, CollectionSetBuilder builder) {
		LOG.debug("process: processing raw TCA data for {} peers.", tracker.size());

		final NodeLevelResource nodeResource = new NodeLevelResource(m_agent.getNodeId());

		long timestamp = 0;
		for (TcaDataEntry entry : tracker.getEntries()) {
			GenericTypeResource resource = new GenericTypeResource(nodeResource, m_resourceType, entry.getPeerAddress());
			CollectionResource collectionResource = CollectionSetBuilder.toCollectionResource(resource, m_agent);

			long lastTimestamp = getLastTimestamp(collectionResource);
			String[] rawData = entry.getRawData().split("\\|");
			int samples = Integer.parseInt(rawData[1]);
			SnmpObjId entryObjId = SnmpObjId.get(".1.3.6.1.4.1.27091.3.1.6.1.2", entry.getInstance().toString());
			String identifierPrefix = String.format("TCA_%s_", entryObjId);

			for (int i=0; i<samples; i++) {
				LOG.debug("process: processing row {}: {}", i, rawData[2 + i]);
				String[] rawEntry = rawData[2 + i].split(",");
				timestamp = Long.parseLong(rawEntry[0]);
				if (timestamp > lastTimestamp) {
					resource = new GenericTypeResource(nodeResource, m_resourceType, entry.getPeerAddress());
					resource.setTimestamp(new Date(timestamp * 1000));
					builder.withIdentifiedNumericAttribute(resource, RESOURCE_TYPE_NAME, INBOUND_DELAY, Double.parseDouble(rawEntry[1]), AttributeType.GAUGE, identifierPrefix + INBOUND_DELAY);
					builder.withIdentifiedNumericAttribute(resource, RESOURCE_TYPE_NAME, INBOUND_JITTER, Double.parseDouble(rawEntry[2]), AttributeType.GAUGE, identifierPrefix + INBOUND_JITTER);
					builder.withIdentifiedNumericAttribute(resource, RESOURCE_TYPE_NAME, OUTBOUND_DELAY, Double.parseDouble(rawEntry[3]), AttributeType.GAUGE, identifierPrefix + OUTBOUND_DELAY);
					builder.withIdentifiedNumericAttribute(resource, RESOURCE_TYPE_NAME, OUTBOUND_JITTER, Double.parseDouble(rawEntry[4]), AttributeType.GAUGE, identifierPrefix + OUTBOUND_JITTER);
					builder.withIdentifiedNumericAttribute(resource, RESOURCE_TYPE_NAME, TIMESYNC_STATUS, Double.parseDouble(rawEntry[5]), AttributeType.GAUGE, identifierPrefix + TIMESYNC_STATUS);
				} else {
					LOG.debug("process: skipping row {} {} because it was already processed.", i, rawData[2+i]);
				}
			}
			setLastTimestamp(collectionResource, timestamp);
		}
	}

	long getLastTimestamp(CollectionResource resource) {
		long timestamp = 0;
		ResourcePath path = ResourceTypeUtils.getResourcePathWithRepository(m_repository, resource.getPath());
		try {
			LOG.debug("Retrieving timestamp from path {}", path);
			timestamp = this.m_blobStore.get(toKvStoreKey(path), this.getClass().getName())
					.filter(a -> a.length>0)
					.map(String::new)
					.map(Long::parseLong)
					.orElse(0L);
		} catch (Exception e) {
			LOG.error("Failed to retrieve timestamp from path {}", path, e);
		}
		return timestamp;
	}

	void setLastTimestamp(CollectionResource resource, long timestamp) {
		ResourcePath path = ResourceTypeUtils.getResourcePathWithRepository(m_repository, resource.getPath());
		LOG.debug("Setting timestamp to {} at path {}", timestamp, path);
		this.m_blobStore.put(toKvStoreKey(path), Long.toString(timestamp).getBytes(StandardCharsets.UTF_8), this.getClass().getName());
	}

	private String toKvStoreKey(ResourcePath path) {
		return LAST_TIMESTAMP + String.join("/", path.elements());
	}
}
