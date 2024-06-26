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
package org.opennms.netmgt.poller.monitors.support;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.opennms.core.spring.BeanUtils;
import org.opennms.core.utils.ParameterMap;
import org.opennms.netmgt.dao.api.NodeDao;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.model.OnmsAssetRecord;
import org.opennms.netmgt.poller.MonitoredService;
import org.opennms.netmgt.poller.support.AbstractServiceMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
/**
 * {@Inheritdoc}
 * Uses {@link org.opennms.netmgt.poller.ServiceMonitor#getRuntimeAttributes(MonitoredService svc, Map<String, Object> parameters)}
 * to add parameters with substitutions of placeholders in existing parameters.
 * The new parameter keys will be prepended with <code>'subbed-'</code>.
 *
 * @author <A HREF="dschlenk@convergeone.com">David Schlenk</A>
 */
public abstract class ParameterSubstitutingMonitor extends AbstractServiceMonitor {

    public static final Logger LOG = LoggerFactory.getLogger(ParameterSubstitutingMonitor.class);

    private static final Pattern LOOKS_LIKE_SUBSTITUTION = Pattern.compile(".*[{][a-zA-Z0-9]*[}].*");
    private static Map<String, Pattern> subPatterns = new HashMap<>();
    private static Pattern substitutionPattern;
    private static final Supplier<NodeDao> nodeDao = Suppliers.memoize(() -> BeanUtils.getBean("daoContext", "nodeDao", NodeDao.class));
    static {
        StringBuilder patternBuilder = new StringBuilder();
        patternBuilder.append(".*[{](ipAddr(?:ess)?|nodeId|nodeLabel|foreignId|foreignSource");
        try {
            BeanInfo info = Introspector.getBeanInfo(OnmsAssetRecord.class);
            PropertyDescriptor[] pds = info.getPropertyDescriptors();
            for (PropertyDescriptor pd : pds) {
                patternBuilder.append("|").append(pd.getName());
                subPatterns.put(pd.getName(), Pattern.compile("(.*)[{]" + pd.getName() + "[}](.*)"));
            }
        } catch (IntrospectionException ie) {
            LOG.warn("Failed to introspect OnmsAssetRecord when initializing due to {}", ie.getLocalizedMessage());
        }
        patternBuilder.append(")[}].*");
        substitutionPattern = Pattern.compile(patternBuilder.toString());

        Pattern p = Pattern.compile("(.*)[{]ipAddr(?:ess)?[}](.*)");
        subPatterns.put("ipAddr", p);
        subPatterns.put("ipAddress", p);
        subPatterns.put("nodeId", Pattern.compile("(.*)[{]nodeId[}](.*)"));
        subPatterns.put("nodeLabel", Pattern.compile("(.*)[{]nodeLabel[}](.*)"));
        subPatterns.put("foreignId", Pattern.compile("(.*)[{]foreignId[}](.*)"));
        subPatterns.put("foreignSource", Pattern.compile("(.*)[{]foreignSource[}](.*)"));
    }

    /**
     * {@inheritDoc}
     *
     * Add new parameters derived from existing parameters that contain placeholders.
     * The new key will be prepended with 'subbed-'.
     */
    @Override
    public Map<String, Object> getRuntimeAttributes(MonitoredService svc, Map<String, Object> parameters) {
        return getSubstitutedParameters(svc, parameters);
    }

    public static Map<String, Object> getSubstitutedParameters(final MonitoredService svc, Map<String, Object> parameters) {
        Map<String, Object> subbedParams = new HashMap<>();
        parameters.forEach((k, v) -> {
            Matcher m = substitutionPattern.matcher(v.toString());
            if (m.matches()) {
                subbedParams.put("subbed-" + k, parseString(v.toString(), m, svc));
            } else if (LOOKS_LIKE_SUBSTITUTION.matcher(v.toString()).matches()){
                LOG.error("unknown substitution found: {}. Possible substitutions: {}", v.toString(), substitutionPattern.toString());
            }
        });
        return subbedParams;
    }

    protected static String parseString(final String unformattedString, final Matcher m, final MonitoredService svc) {
        Pattern p = subPatterns.get(m.group(1));
        if (p != null) {
        } else {
            LOG.error("invalid substitution pattern found: {}", m.group(1));
            return null;
        }
        Matcher n = p.matcher(unformattedString);
        String formattedString;
        if (n.matches()) {
            StringBuilder sb = new StringBuilder();
            sb.append(n.group(1));
            switch (m.group(1)) {
                case "ipAddr":
                case "ipAddress":
                    sb.append(svc.getIpAddr());
                    break;
                case "nodeId":
                    sb.append(svc.getNodeId());
                    break;
                case "nodeLabel":
                    sb.append(svc.getNodeLabel());
                    break;
                case "foreignId":
                    sb.append(nodeDao.get().get(svc.getNodeId()).getForeignId());
                    break;
                case "foreignSource":
                    sb.append(nodeDao.get().get(svc.getNodeId()).getForeignSource());
                    break;
                default:
                    LOG.debug("attempting to add node asset property {}", m.group(1));
                    OnmsNode node = nodeDao.get().get(svc.getNodeId());
                    if (node != null) {
                        BeanWrapper wrapper = new BeanWrapperImpl(node.getAssetRecord());
                        Object obj = wrapper.getPropertyValue(m.group(1));
                        if (obj != null) {
                            sb.append(obj.toString());
                        }
                    }
            }
            sb.append(n.group(2));
            formattedString = sb.toString();
        } else {
            // We should never end up here since we should have a match, see getSubstitutedParameters(): if(m.matches){...}
            // If we get here something is wrong with the regexp => throw an exception
            throw new IllegalStateException(
                    String.format("Engineering mistake: we should never end up here, the regular expression seems to have a problem: %s", substitutionPattern.toString()));
        }
        Matcher o = substitutionPattern.matcher(formattedString);
        if (o.matches()) {
            return parseString(formattedString, o, svc);
        }
        return formattedString;
    }

    protected static String resolveKeyedString(final Map<String, Object> parameterMap, final String key, final String defaultValue) {
        String ret = ParameterMap.getKeyedString(parameterMap, key, defaultValue);
        String subKey = "subbed-" + key;
        if (parameterMap.containsKey(subKey)) {
            ret = ParameterMap.getKeyedString(parameterMap, subKey, defaultValue);
        }
        return ret;
    }
}
