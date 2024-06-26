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
package org.opennms.web.rest.v2;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;
import javax.ws.rs.core.UriInfo;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opennms.netmgt.dao.api.ApplicationDao;
import org.opennms.netmgt.dao.mock.MockApplicationDao;
import org.opennms.netmgt.events.api.EventConstants;
import org.opennms.netmgt.events.api.EventProxy;
import org.opennms.netmgt.events.api.EventProxyException;
import org.opennms.netmgt.model.OnmsApplication;
import org.opennms.netmgt.xml.event.Event;

public class ApplicationRestServiceTest {
    @Test
    public void shouldSendEvents() throws EventProxyException {

        EventProxy proxy = mock(EventProxy.class);
        ApplicationDao dao = new MockApplicationDao();
        ApplicationRestService service = new ApplicationRestService(dao, proxy);
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getRequestUriBuilder()).thenReturn(new UriBuilderMock());

        // Create
        OnmsApplication application = new OnmsApplication();
        application.setName("myApp");
        ArgumentCaptor<Event> argument = ArgumentCaptor.forClass(Event.class);
        service.doCreate(mock(SecurityContext.class), uriInfo, application);
        verify(proxy).send(argument.capture());
        assertEquals(EventConstants.APPLICATION_CREATED_EVENT_UEI, argument.getValue().getUei());

        // Delete
        service.doDelete(mock(SecurityContext.class), uriInfo, application);
        verify(proxy, times(2)).send(argument.capture());
        assertEquals(EventConstants.APPLICATION_DELETED_EVENT_UEI, argument.getValue().getUei());
    }

    // pretty ugly but it saves us spinning up another integration test.
    static class UriBuilderMock extends UriBuilder {

        @Override
        public UriBuilder clone() {
            return null;
        }

        @Override
        public UriBuilder uri(URI uri) {
            return null;
        }

        @Override
        public UriBuilder uri(String uriTemplate) {
            return null;
        }

        @Override
        public UriBuilder scheme(String scheme) {
            return null;
        }

        @Override
        public UriBuilder schemeSpecificPart(String ssp) {
            return null;
        }

        @Override
        public UriBuilder userInfo(String ui) {
            return null;
        }

        @Override
        public UriBuilder host(String host) {
            return null;
        }

        @Override
        public UriBuilder port(int port) {
            return null;
        }

        @Override
        public UriBuilder replacePath(String path) {
            return null;
        }

        @Override
        public UriBuilder path(String path) {
            return this;
        }

        @Override
        public UriBuilder path(Class resource) {
            return null;
        }

        @Override
        public UriBuilder path(Class resource, String method) {
            return null;
        }

        @Override
        public UriBuilder path(Method method) {
            return null;
        }

        @Override
        public UriBuilder segment(String... segments) {
            return null;
        }

        @Override
        public UriBuilder replaceMatrix(String matrix) {
            return null;
        }

        @Override
        public UriBuilder matrixParam(String name, Object... values) {
            return null;
        }

        @Override
        public UriBuilder replaceMatrixParam(String name, Object... values) {
            return null;
        }

        @Override
        public UriBuilder replaceQuery(String query) {
            return null;
        }

        @Override
        public UriBuilder queryParam(String name, Object... values) {
            return null;
        }

        @Override
        public UriBuilder replaceQueryParam(String name, Object... values) {
            return null;
        }

        @Override
        public UriBuilder fragment(String fragment) {
            return null;
        }

        @Override
        public UriBuilder resolveTemplate(String name, Object value) {
            return null;
        }

        @Override
        public UriBuilder resolveTemplate(String name, Object value, boolean encodeSlashInPath) {
            return null;
        }

        @Override
        public UriBuilder resolveTemplateFromEncoded(String name, Object value) {
            return null;
        }

        @Override
        public UriBuilder resolveTemplates(Map<String, Object> templateValues) {
            return null;
        }

        @Override
        public UriBuilder resolveTemplates(Map<String, Object> templateValues, boolean encodeSlashInPath) throws IllegalArgumentException {
            return null;
        }

        @Override
        public UriBuilder resolveTemplatesFromEncoded(Map<String, Object> templateValues) {
            return null;
        }

        @Override
        public URI buildFromMap(Map<String, ?> values) {
            return null;
        }

        @Override
        public URI buildFromMap(Map<String, ?> values, boolean encodeSlashInPath) throws IllegalArgumentException, UriBuilderException {
            return null;
        }

        @Override
        public URI buildFromEncodedMap(Map<String, ?> values) throws IllegalArgumentException, UriBuilderException {
            return null;
        }

        @Override
        public URI build(Object... values) throws IllegalArgumentException, UriBuilderException {
            try {
                return new URI("somewhere");
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public URI build(Object[] values, boolean encodeSlashInPath) throws IllegalArgumentException, UriBuilderException {
            return null;
        }

        @Override
        public URI buildFromEncoded(Object... values) throws IllegalArgumentException, UriBuilderException {
            return null;
        }

        @Override
        public String toTemplate() {
            return null;
        }
    }
}
