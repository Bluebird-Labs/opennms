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
package org.opennms.web.rest.api.support;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.opennms.web.rest.api.ResourceLocation;

public class JsonResourceLocationListDeserializationProvider extends JsonDeserializer<List<ResourceLocation>> {

    @SuppressWarnings("unchecked")
    @Override
    public List<ResourceLocation> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        return (List<ResourceLocation>) jp.readValueAs(List.class).stream()
                .map(s -> ResourceLocation.parse((String)s))
                .collect(Collectors.toList());
    }
}
