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
package org.opennms.core.ipc.sink.kafka.client;

import static org.opennms.core.ipc.common.kafka.KafkaSinkConstants.DEFAULT_MAX_BUFFER_SIZE;
import static org.opennms.core.ipc.common.kafka.KafkaSinkConstants.KAFKA_COMMON_CONFIG_PID;
import static org.opennms.core.ipc.common.kafka.KafkaSinkConstants.MAX_BUFFER_SIZE_PROPERTY;
import static org.opennms.core.ipc.sink.api.Message.SINK_METRIC_PRODUCER_DOMAIN;

import java.io.IOException;
import java.math.RoundingMode;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.opennms.core.camel.JmsQueueNameFactory;
import org.opennms.core.ipc.common.kafka.KafkaConfigProvider;
import org.opennms.core.ipc.common.kafka.KafkaSinkConstants;
import org.opennms.core.ipc.common.kafka.OsgiKafkaConfigProvider;
import org.opennms.core.ipc.common.kafka.Utils;
import org.opennms.core.ipc.sink.api.Message;
import org.opennms.core.ipc.sink.api.MessageConsumerManager;
import org.opennms.core.ipc.sink.api.SinkModule;
import org.opennms.core.ipc.sink.common.AbstractMessageDispatcherFactory;
import org.opennms.core.ipc.sink.model.SinkMessage;
import org.opennms.core.logging.Logging;
import org.opennms.core.logging.Logging.MDCCloseable;
import org.opennms.core.tracing.api.TracerConstants;
import org.opennms.core.tracing.api.TracerRegistry;
import org.opennms.core.tracing.util.TracingInfoCarrier;
import org.opennms.distributed.core.api.Identity;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.google.common.math.IntMath;
import com.google.protobuf.ByteString;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.util.GlobalTracer;

public class KafkaRemoteMessageDispatcherFactory extends AbstractMessageDispatcherFactory<String> {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaRemoteMessageDispatcherFactory.class);

    private final static int INVALID_PARTITION = -1;
    private final Properties kafkaConfig = new Properties();

    private ConfigurationAdmin configAdmin;

    private BundleContext bundleContext;

    private KafkaProducer<String, byte[]> producer;

    private TracerRegistry tracerRegistry;

    private MetricRegistry metrics;

    private Identity identity;

    private int maxBufferSize;

    @Override
    public <S extends Message, T extends Message> String getModuleMetadata(final SinkModule<S, T> module) {
        final JmsQueueNameFactory topicNameFactory = new JmsQueueNameFactory(KafkaSinkConstants.KAFKA_TOPIC_PREFIX, module.getId());
        return topicNameFactory.getName();
    }

    @Override
    public <S extends Message, T extends Message> void dispatch(SinkModule<S, T> module, String topic, T message) {
        try (MDCCloseable mdc = Logging.withPrefixCloseable(MessageConsumerManager.LOG_PREFIX)) {
            LOG.trace("dispatch({}): sending message {}", topic, message);
            byte[] sinkMessageContent = module.marshal(message);
            String messageId = UUID.randomUUID().toString();
            final String messageKey = module.getRoutingKey(message).orElse(messageId);
            // Send this message to Kafka, If partition changed in between sending chunks of a larger message,
            // try to send message again.
            boolean partitionChanged = false;
            do {
                partitionChanged = sendMessage(topic, messageId, messageKey, sinkMessageContent);
            } while (partitionChanged);
        }
    }

    /**
     * This method will divide message into chunks and send each chunk to kafka.
     * This will return false by default. If this is large buffer (total chunks > 1) and if different chunks have
     * been sent to different partitions, method will return true indicating partition change in between.
     * @param topic    The kafka topic message needs to be sent
     * @param messageId  The messageId message associated with
     * @param messageKey  The key used to route the message
     * @param sinkMessageContent  The sink message
     * @return partitionChanged  return true if partition changed in between else return false by default.
     */
    private boolean sendMessage(String topic, String messageId, String messageKey, byte[] sinkMessageContent) {
        int partitionNum = INVALID_PARTITION;
        boolean partitionChanged = false;
        int totalChunks = IntMath.divide(sinkMessageContent.length, maxBufferSize, RoundingMode.UP);
        for (int chunk = 0; chunk < totalChunks; chunk++) {
            byte[] messageInBytes = wrapMessageToProto(messageId, chunk, totalChunks, sinkMessageContent);
            final ProducerRecord<String, byte[]> record = new ProducerRecord<>(topic, messageKey, messageInBytes);
            // Add tags to tracer active span.
            Span activeSpan = getTracer().activeSpan();
            if (activeSpan != null && (chunk + 1 == totalChunks)) {
                activeSpan.setTag(TracerConstants.TAG_TOPIC, topic);
                activeSpan.setTag(TracerConstants.TAG_MESSAGE_SIZE, sinkMessageContent.length);
                activeSpan.setTag(TracerConstants.TAG_THREAD, Thread.currentThread().getName());
            }
            // Keep sending record till it delivers successfully.
            int partition = sendMessageChunkToKafka(topic, record);
            if (totalChunks > 1 && chunk == 0) {
                partitionNum = partition;
            } else if (totalChunks > 1 && partitionNum != partition) {
                partitionChanged = true;
                break;
            }
        }
        return partitionChanged;
    }

    /**
     *  This method will send one chunk of message to kafka and returns the partition number the message has been sent to.
     * @param topic   The kafka topic message needs to be sent
     * @param record message
     * @return  partition number
     */
    private int sendMessageChunkToKafka(String topic, ProducerRecord<String, byte[]> record) {

        while (true) {
            try {
                // From KafkaProducer's JavaDoc: The producer is thread safe and should generally be shared among all threads for best performance.
                final Future<RecordMetadata> future = producer.send(record);
                // The call to dispatch() is synchronous, so we block until the message was sent
                RecordMetadata recordMetadata = future.get();
                return recordMetadata.partition();
            } catch (InterruptedException e) {
                LOG.warn("Interrupted while sending message to topic {}.", topic, e);
                Thread.currentThread().interrupt();
                break;
            } catch (ExecutionException e) {
                // Timeout typically happens when Kafka is Offline or it didn't initialize yet.
                // For this case keep sending the message until it delivers, will cause sink messages to buffer.
                if (e.getCause() != null && e.getCause() instanceof TimeoutException) {
                    LOG.warn("Timeout occured while sending message to topic {}, it will be attempted again.", topic);
                } else {
                    LOG.error("Exception occured while sending message to topic {} ", e);
                    break;
                }
            }
        }
      return INVALID_PARTITION;
    }

    private byte[] wrapMessageToProto(String messageId, int chunk, int totalChunks, byte[] sinkMessageContent) {
        // Calculate remaining bufferSize for each chunk.
        int bufferSize = getRemainingBufferSize(sinkMessageContent.length, chunk);
        ByteString byteString = ByteString.copyFrom(sinkMessageContent, chunk * maxBufferSize, bufferSize);
        SinkMessage.Builder sinkMessageBuilder = SinkMessage.newBuilder()
                .setMessageId(messageId)
                .setCurrentChunkNumber(chunk)
                .setTotalChunks(totalChunks)
                .setContent(byteString);
        // Add tracing info
        final Tracer tracer = getTracer();
        if (tracer.activeSpan() != null && (chunk + 1 == totalChunks)) {
            TracingInfoCarrier tracingInfoCarrier = new TracingInfoCarrier();
            tracer.inject(tracer.activeSpan().context(), Format.Builtin.TEXT_MAP, tracingInfoCarrier);
            tracer.activeSpan().setTag(TracerConstants.TAG_LOCATION, identity.getLocation());
            tracer.activeSpan().setTag(TracerConstants.TAG_THREAD, Thread.currentThread().getName());
            tracingInfoCarrier.getTracingInfoMap().forEach(sinkMessageBuilder::putTracingInfo);
        }
        return sinkMessageBuilder.build().toByteArray();
    }

    public void init() throws IOException {
        try (MDCCloseable mdc = Logging.withPrefixCloseable(MessageConsumerManager.LOG_PREFIX)) {
            // Defaults
            kafkaConfig.clear();
            kafkaConfig.put("key.serializer", StringSerializer.class.getCanonicalName());
            kafkaConfig.put("value.serializer", ByteArraySerializer.class.getCanonicalName());
            // Retrieve all of the properties from org.opennms.core.ipc.sink.kafka.cfg, fallback to common pid.
            KafkaConfigProvider configProvider = new OsgiKafkaConfigProvider(KafkaSinkConstants.KAFKA_CONFIG_PID, configAdmin, KAFKA_COMMON_CONFIG_PID);
            kafkaConfig.putAll(configProvider.getProperties());
            LOG.info("KafkaRemoteMessageDispatcherFactory: initializing the Kafka producer with: {}", kafkaConfig);
            producer = Utils.runWithGivenClassLoader(() -> new KafkaProducer<>(kafkaConfig), KafkaProducer.class.getClassLoader());
            maxBufferSize = getMaxBufferSize();
            if (tracerRegistry != null && identity != null) {
                tracerRegistry.init(identity.getLocation() + "@" + identity.getId());
            }
            onInit();
        }
    }

    public void destroy() {
        onDestroy();
        if (producer != null) {
            producer.close();
            producer = null;
        }
    }

    @Override
    public String getMetricDomain() {
        return SINK_METRIC_PRODUCER_DOMAIN;
    }

    @Override
    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    // Calculate remaining buffer size for each chunk.
    private int getRemainingBufferSize(int messageSize, int chunk) {
        int bufferSize = messageSize;
        if (messageSize > maxBufferSize) {
            int remaining = messageSize - chunk * maxBufferSize;
            bufferSize = (remaining > maxBufferSize) ? maxBufferSize : remaining;
        }
        return bufferSize;
    }

    public Integer getMaxBufferSize() {
        int maxBufferSize = DEFAULT_MAX_BUFFER_SIZE;
        String bufferSize = kafkaConfig.getProperty(MAX_BUFFER_SIZE_PROPERTY);
        if (bufferSize != null) {
            try {
                maxBufferSize = Integer.parseInt(bufferSize);
            } catch (NumberFormatException ex){
                LOG.warn("Configured max buffer size is not a number");
            }
        }
        return Math.min(DEFAULT_MAX_BUFFER_SIZE, maxBufferSize);
    }

    public void setTracerRegistry(TracerRegistry tracerRegistry) {
        this.tracerRegistry = tracerRegistry;
    }

    public TracerRegistry getTracerRegistry() {
        return tracerRegistry;
    }

    @Override
    public Tracer getTracer() {
        if (getTracerRegistry() != null) {
            return getTracerRegistry().getTracer();
        }
        return  GlobalTracer.get();
    }

    public Identity getIdentity() {
        return identity;
    }

    public void setIdentity(Identity identity) {
        this.identity = identity;
    }

    @Override
    public MetricRegistry getMetrics() {
        if(metrics == null) {
            metrics = new MetricRegistry();
        }
        return metrics;
    }

    public void setMetrics(MetricRegistry metrics) {
        this.metrics = metrics;
    }
}
