<%--

    Licensed to The OpenNMS Group, Inc (TOG) under one or more
    contributor license agreements.  See the LICENSE.md file
    distributed with this work for additional information
    regarding copyright ownership.

    TOG licenses this file to You under the GNU Affero General
    Public License Version 3 (the "License") or (at your option)
    any later version.  You may not use this file except in
    compliance with the License.  You may obtain a copy of the
    License at:

         https://www.gnu.org/licenses/agpl-3.0.txt

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied.  See the License for the specific
    language governing permissions and limitations under the
    License.

--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" import="
java.lang.*,
java.util.*,
org.opennms.web.utils.assets.*,
org.slf4j.*
" %>
<%@ page import="org.owasp.encoder.Encode" %>
<%@ page import="com.google.common.collect.Sets" %>
<%!
private static final Logger LOG = LoggerFactory.getLogger(AssetLocator.class);
private static final Set<String> WHITELIST_ASSET_MEDIA = Sets.newHashSet("screen");
private static final Set<String> WHITELIST_ASSET_ASYNC = Sets.newHashSet("true");
%><%
final AssetLocator locator = AssetLocatorImpl.getInstance();
//locator.reload();
final long lastModified = locator.lastModified();

if (locator == null) {
    LOG.warn("load-assets.jsp is missing the locator");
} else {
    final String media = request.getParameter("asset-media");
    final String mediaString = WHITELIST_ASSET_MEDIA.contains(media) ? " media=\"" + media + "\"" : "";
    final String type = request.getParameter("asset-type");
    final boolean defer = Boolean.valueOf(request.getParameter("asset-defer"));
    final String async = WHITELIST_ASSET_ASYNC.contains(request.getParameter("asset-async")) ? request.getParameter("asset-async") : null;

    Boolean minified = null;
    final String minifiedString = request.getParameter("minified");
    if (minifiedString != null && !"".equals(minifiedString.trim())) {
        minified = Boolean.valueOf(minifiedString);
    }

    final String[] assets = request.getParameterValues("asset");
    //if (LOG.isDebugEnabled()) LOG.debug("load-assets.jsp: assets={}, type={}, media={}", Arrays.toString(assets), type, media);

    for (final String assetParam : assets) {
        LOG.debug("load-assets.jsp: asset={}, type={}, media={}", assetParam, type, media);
        final Optional<Collection<AssetResource>> resources = minified == null? locator.getResources(assetParam) : locator.getResources(assetParam, minified);
        if (!resources.isPresent()) {
            LOG.warn("load-assets.jsp: resources not found for asset {}", assetParam);
        } else {
            for (final AssetResource resource : resources.get()) {
                final StringBuilder sb = new StringBuilder();
                if (type != null && !type.equals(resource.getType())) {
                    LOG.debug("load-assets.jsp: skipping type {} for asset {}, page requested {}", resource.getType(), assetParam, type);
                    continue;
                }
                if ("js".equals(resource.getType())) {
                    LOG.debug("load-assets.jsp: found javascript resource");
                    sb.append("<script ");
                    if (defer) {
                        sb.append("defer ");
                    }
                    if (async != null) {
                        sb.append("async=\"").append(async).append("\"");
                    }
                    sb.append("src=\"assets/")
                        .append(resource.getPath())
                        .append("?v=").append(lastModified)
                        .append("\"></script>");
                } else if ("css".equals(resource.getType())) {
                    LOG.debug("load-assets.jsp: found stylesheet resource");
                    sb.append("<link rel=\"stylesheet\" href=\"assets/")
                        .append(resource.getPath())
                        .append("?v=").append(lastModified)
                        .append("\"");
                    if (mediaString != null && !mediaString.trim().equals("")) {
                        sb.append(mediaString);
                    }
                    sb.append(">");
                } else {
                    LOG.warn("load-assets.jsp: unknown/unhandled asset resource type: {}", resource.getType());
                }
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                LOG.debug("load-assets.jsp: Writing HTML: {}", sb.toString());
                out.write(sb.toString());
            }
        }
    }
}
%>
