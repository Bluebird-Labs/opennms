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
package org.opennms.features.topology.plugins.topo.graphml;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.opennms.features.graphml.model.GraphMLGraph;
import org.opennms.features.graphml.model.GraphMLNode;
import org.opennms.features.topology.api.browsers.ContentType;
import org.opennms.features.topology.api.browsers.SelectionChangedListener;
import org.opennms.features.topology.api.support.FocusStrategy;
import org.opennms.features.topology.api.support.hops.VertexHopCriteria;
import org.opennms.features.topology.api.topo.AbstractTopologyProvider;
import org.opennms.features.topology.api.topo.DefaultTopologyProviderInfo;
import org.opennms.features.topology.api.topo.Defaults;
import org.opennms.features.topology.api.topo.GraphProvider;
import org.opennms.features.topology.api.topo.TopologyProviderInfo;
import org.opennms.features.topology.api.topo.VertexRef;
import org.opennms.features.topology.plugins.topo.graphml.internal.GraphMLServiceAccessor;
import org.opennms.netmgt.model.OnmsNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class GraphMLTopologyProvider extends AbstractTopologyProvider implements GraphProvider {

    protected static final String DEFAULT_DESCRIPTION = "This Topology Provider visualizes a predefined GraphML graph.";
    private static final Logger LOG = LoggerFactory.getLogger(GraphMLTopologyProvider.class);

    private final GraphMLServiceAccessor m_serviceAccessor;

    private static TopologyProviderInfo createTopologyProviderInfo(GraphMLGraph graph) {
        String name = graph.getProperty(GraphMLProperties.LABEL, graph.getId());
        String description = graph.getProperty(GraphMLProperties.DESCRIPTION, DEFAULT_DESCRIPTION);
        return new DefaultTopologyProviderInfo(name, description, null, true);
    }

    public enum VertexStatusProviderType {
        NO_STATUS_PROVIDER,
        DEFAULT_STATUS_PROVIDER,
        SCRIPT_STATUS_PROVIDER,
        PROPAGATE_STATUS_PROVIDER,
    }

    private final int defaultSzl;
    private final String preferredLayout;
    private final FocusStrategy focusStrategy;
    private final List<String> focusIds;
    private final VertexStatusProviderType vertexStatusProviderType;

    public GraphMLTopologyProvider(final GraphMLGraph graphMLGraph,
                                   final GraphMLServiceAccessor serviceAccessor) {
        super((String)graphMLGraph.getProperty(GraphMLProperties.NAMESPACE));

        m_serviceAccessor = serviceAccessor;

        for (GraphMLNode graphMLNode : graphMLGraph.getNodes()) {
            GraphMLVertex newVertex = new GraphMLVertex(this.getNamespace(), graphMLNode);
            setNodeIdForVertex(newVertex);
            graph.addVertices(newVertex);
        }
        for (org.opennms.features.graphml.model.GraphMLEdge eachEdge : graphMLGraph.getEdges()) {
            GraphMLVertex sourceVertex = (GraphMLVertex) graph.getVertex(getNamespace(), eachEdge.getSource().getId());
            GraphMLVertex targetVertex = (GraphMLVertex) graph.getVertex(getNamespace(), eachEdge.getTarget().getId());
            if (sourceVertex == null || targetVertex == null) {
                // Skip edges where either the source of target vertices are outside of this graph
                continue;
            }
            GraphMLEdge newEdge = new GraphMLEdge(getNamespace(), eachEdge, sourceVertex, targetVertex);
            graph.addEdges(newEdge);
        }
        setTopologyProviderInfo(createTopologyProviderInfo(graphMLGraph));
        defaultSzl = getDefaultSzl(graphMLGraph);
        focusStrategy = getFocusStrategy(graphMLGraph);
        focusIds = getFocusIds(graphMLGraph);
        preferredLayout = graphMLGraph.getProperty(GraphMLProperties.PREFERRED_LAYOUT);

        this.vertexStatusProviderType = getVertexProviderTypeFromGraph(graphMLGraph);

        if (focusStrategy != FocusStrategy.SPECIFIC && !focusIds.isEmpty()) {
            LOG.warn("Focus ids is defined, but strategy is {}. Did you mean to specify {}={}. Ignoring focusIds.", focusStrategy.name(), GraphMLProperties.FOCUS_STRATEGY, FocusStrategy.SPECIFIC.name());
        }
    }

    private void setNodeIdForVertex(GraphMLVertex vertex) {
        if (vertex == null) {
            return;
        }
        if (vertex.getNodeID() == null) {
            String foreignSource = (String) vertex.getProperties().get(GraphMLProperties.FOREIGN_SOURCE);
            String foreignId = (String) vertex.getProperties().get(GraphMLProperties.FOREIGN_ID);
            if (!Strings.isNullOrEmpty(foreignSource) && !Strings.isNullOrEmpty(foreignId)) {
                OnmsNode onmsNode = m_serviceAccessor.getNodeDao().findByForeignId(foreignSource, foreignId);
                if (onmsNode != null) {
                    vertex.setNodeID(onmsNode.getId());
                } else {
                    LOG.warn("No node found for the given foreignSource ({}) and foreignId ({}).", foreignSource, foreignId);
                }
            }
        }
    }

    private static FocusStrategy getFocusStrategy(GraphMLGraph graph) {
        String strategy = graph.getProperty(GraphMLProperties.FOCUS_STRATEGY);
        if (strategy != null) {
            return FocusStrategy.getStrategy(strategy, FocusStrategy.FIRST);
        }
        return FocusStrategy.FIRST;
    }

    private static List<String>  getFocusIds(GraphMLGraph graph) {
        String property = graph.getProperty(GraphMLProperties.FOCUS_IDS);
        if (property != null) {
            String[] split = property.split(",");
            return Lists.newArrayList(split);
        }
        return Lists.newArrayList();
    }

    private static int getDefaultSzl(GraphMLGraph graph) {
        Integer szl = graph.getProperty(GraphMLProperties.SEMANTIC_ZOOM_LEVEL);
        if (szl != null) {
            return szl;
        }
        return Defaults.DEFAULT_SEMANTIC_ZOOM_LEVEL;
    }

    @Override
    public void refresh() {
        // Refresh is handled by the MetaTopologyProvider as one GraphML file may represent
        // multiple layers (GraphMLTopologyProviders)
    }

    @Override
    public Defaults getDefaults() {
        return new Defaults()
                .withSemanticZoomLevel(defaultSzl)
                .withPreferredLayout(preferredLayout)
                .withCriteria(() -> {
                    List<VertexHopCriteria> focusCriteria = focusStrategy.getFocusCriteria(graph, focusIds.toArray(new String[focusIds.size()]));
                    return Lists.newArrayList(focusCriteria);
                });
    }

    @Override
    public SelectionChangedListener.Selection getSelection(List<VertexRef> selectedVertices, ContentType contentType) {
        Set<Integer> nodeIds = selectedVertices.stream()
                .filter(eachVertex -> eachVertex.getNamespace().equals(getNamespace()) && eachVertex instanceof GraphMLVertex)
                .map(eachVertex -> (GraphMLVertex) eachVertex)
                .map(GraphMLVertex::getNodeID)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (contentType == ContentType.Alarm) {
            return new SelectionChangedListener.AlarmNodeIdSelection(nodeIds);
        }
        if (contentType == ContentType.Node) {
            return new SelectionChangedListener.IdSelection<>(nodeIds);
        }
        return SelectionChangedListener.Selection.NONE;
    }

    @Override
    public boolean contributesTo(ContentType type) {
        return Sets.newHashSet(ContentType.Alarm, ContentType.Node).contains(type);
    }

    public VertexStatusProviderType getVertexStatusProviderType() {
        return this.vertexStatusProviderType;
    }

    private static VertexStatusProviderType getVertexProviderTypeFromGraph(final GraphMLGraph graph) {
        final Object vertexStatusProviderType = graph.<Object>getProperty(GraphMLProperties.VERTEX_STATUS_PROVIDER, Boolean.FALSE);
        if (vertexStatusProviderType instanceof Boolean) {
            if (vertexStatusProviderType == Boolean.TRUE) {
                return VertexStatusProviderType.DEFAULT_STATUS_PROVIDER;
            } else {
                return VertexStatusProviderType.NO_STATUS_PROVIDER;
            }

        } else if (vertexStatusProviderType instanceof String) {
            if ("default".equalsIgnoreCase((String)vertexStatusProviderType)) {
                return VertexStatusProviderType.DEFAULT_STATUS_PROVIDER;
            } else if ("script".equalsIgnoreCase((String)vertexStatusProviderType)) {
                return VertexStatusProviderType.SCRIPT_STATUS_PROVIDER;
            } else if ("propagate".equalsIgnoreCase((String)vertexStatusProviderType)) {
                return VertexStatusProviderType.PROPAGATE_STATUS_PROVIDER;
            } else {
                LOG.warn("Unknown GraphML vertex status provider type: {}", vertexStatusProviderType);
                return VertexStatusProviderType.NO_STATUS_PROVIDER;
            }
        } else {
            return VertexStatusProviderType.NO_STATUS_PROVIDER;
        }
    }
}
