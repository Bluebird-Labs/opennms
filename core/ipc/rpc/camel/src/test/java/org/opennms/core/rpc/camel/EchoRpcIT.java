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
package org.opennms.core.rpc.camel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Test;
import org.opennms.core.rpc.api.RemoteExecutionException;
import org.opennms.core.rpc.api.RequestRejectedException;
import org.opennms.core.rpc.api.RequestTimedOutException;
import org.opennms.core.rpc.echo.EchoClient;
import org.opennms.core.rpc.echo.EchoRequest;
import org.opennms.core.rpc.echo.EchoResponse;
import org.opennms.core.rpc.echo.EchoRpcModule;
import org.opennms.core.rpc.echo.MyEchoException;
import org.opennms.core.test.Level;
import org.opennms.core.test.MockLogAppender;
import org.opennms.distributed.core.api.MinionIdentity;
import org.opennms.netmgt.model.OnmsDistPoller;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class EchoRpcIT {

    public static final String REMOTE_LOCATION_NAME = "remote";

    @Autowired
    private OnmsDistPoller identity;

    @Autowired
    private EchoClient echoClient;

    public abstract CamelContext getContext();

    public abstract CamelContext getClientContext();

    public abstract CamelRpcServerRouteManager getRouteManager(CamelContext context);

    @Test(timeout=60000)
    public void canExecuteRpcViaCurrentLocation() throws InterruptedException, ExecutionException {
        EchoRequest request = new EchoRequest("HELLO!");
        EchoResponse expectedResponse = new EchoResponse("HELLO!");
        EchoResponse actualResponse = echoClient.execute(request).get();
        assertEquals(expectedResponse, actualResponse);
    }

    @Test(timeout=60000)
    public void canExecuteRpcViaAnotherLocation() throws Exception {
        assertNotEquals(REMOTE_LOCATION_NAME, identity.getLocation());
        EchoRpcModule echoRpcModule = new EchoRpcModule();

        CamelContext context = getContext();
        context.start();

        CamelRpcServerRouteManager routeManager = getRouteManager(context);
        routeManager.bind(echoRpcModule);

        EchoRequest request = new EchoRequest("HELLO!!!");
        request.setLocation(REMOTE_LOCATION_NAME);
        EchoResponse expectedResponse = new EchoResponse("HELLO!!!");
        EchoResponse actualResponse = echoClient.execute(request).get();
        assertEquals(expectedResponse, actualResponse);

        routeManager.unbind(echoRpcModule);
        context.stop();
    }

    @Test(timeout=60000)
    public void canExecuteRpcViaAnotherLocationWithSystemId() throws Exception {
        assertNotEquals(REMOTE_LOCATION_NAME, identity.getLocation());
        EchoRpcModule echoRpcModule = new EchoRpcModule();

        CamelContext context = getContext();
        context.start();

        MinionIdentity minionIdentity = new MockMinionIdentity(REMOTE_LOCATION_NAME);
        CamelRpcServerRouteManager routeManager = getRouteManager(context);
        routeManager.bind(echoRpcModule);
        EchoRequest request = new EchoRequest("HELLO!!!");
        // Specify the system id
        assertNotNull(minionIdentity.getId());
        request.setSystemId(minionIdentity.getId());
        request.setLocation(REMOTE_LOCATION_NAME);
        EchoResponse expectedResponse = new EchoResponse("HELLO!!!");
        EchoResponse actualResponse = echoClient.execute(request).get();
        assertEquals(expectedResponse, actualResponse);

        routeManager.unbind(echoRpcModule);
        context.stop();
    }

    /**
     * Issues a RPC to a location at which a listener is registered,
     * but specifies a system id that is not equal to the listener's.
     * Since no matching system can process the request, the request
     * should time out.
     */
    @Test(timeout=60000)
    public void failsWithTimeoutWhenSystemIdDoesNotExist() throws Exception {
        assertNotEquals(REMOTE_LOCATION_NAME, identity.getLocation());
        EchoRpcModule echoRpcModule = new EchoRpcModule();

        CamelContext context = getContext();
        context.start();

        MinionIdentity minionIdentity = new MockMinionIdentity(REMOTE_LOCATION_NAME);
        CamelRpcServerRouteManager routeManager = getRouteManager(context);
        routeManager.bind(echoRpcModule);
        EchoRequest request = new EchoRequest("HELLO!!!");
        // Use a different system id, other than the one that's actually listening
        request.setSystemId(minionIdentity.getId() + "!");
        request.setLocation(REMOTE_LOCATION_NAME);

        try {
            echoClient.execute(request).get();
            fail("Did not get ExecutionException");
        } catch (ExecutionException e) {
            assertTrue("Cause is not of type RequestTimedOutException: " + ExceptionUtils.getStackTrace(e), e.getCause() instanceof RequestTimedOutException);
        }

        routeManager.unbind(echoRpcModule);
        context.stop();
    }

    /**
     * Verifies that the future fails with the original exception if
     * an error occurs when executing locally.
     */
    @Test(timeout=60000)
    public void futureFailsWithOriginalExceptionWhenExecutingLocally() throws InterruptedException, ExecutionException {
        EchoRequest request = new EchoRequest("Oops!");
        request.shouldThrow(true);
        try {
            echoClient.execute(request).get();
            fail();
        } catch (ExecutionException e) {
            assertEquals("Oops!", e.getCause().getMessage());
            assertEquals(MyEchoException.class, e.getCause().getClass());
        }
    }

    /**
     * Verifies that the future fails with a {@code RemoteExecutionException} when
     * if an error occurs when executing remotely.
     */
    @Test(timeout=60000)
    public void futureFailsWithRemoteExecutionExceptionWhenExecutingRemotely() throws Exception {
        assertNotEquals(REMOTE_LOCATION_NAME, identity.getLocation());
        EchoRpcModule echoRpcModule = new EchoRpcModule();

        CamelContext context = getContext();
        context.start();

        CamelRpcServerRouteManager routeManager = getRouteManager(context);
        routeManager.bind(echoRpcModule);

        EchoRequest request = new EchoRequest("Oops!");
        request.shouldThrow(true);
        request.setLocation(REMOTE_LOCATION_NAME);
        try {
            echoClient.execute(request).get();
            fail();
        } catch (ExecutionException e) {
            assertTrue(e.getCause().getMessage(), e.getCause().getMessage().contains("Oops!"));
            assertEquals(RemoteExecutionException.class, e.getCause().getClass());
        }

        routeManager.unbind(echoRpcModule);
        context.stop();
    }

    /**
     * Verifies that the future fails with a {@code RequestRejectedException} when
     * when the client context is stopped.
     */
    @Test(timeout=60000)
    public void futureFailsWithRequestRejectedExceptionWhenClientContextIsStopped() throws Exception {
        assertNotEquals(REMOTE_LOCATION_NAME, identity.getLocation());

        // Stop the client context, this will happen when OpenNMS is shutting down
        getClientContext().stop();

        // Now issue an RPC
        EchoRequest request = new EchoRequest("Helló");
        request.setLocation(REMOTE_LOCATION_NAME);
        try {
            echoClient.execute(request).get();
            fail();
        } catch (ExecutionException e) {
            assertEquals(RequestRejectedException.class, e.getCause().getClass());
        }
    }

    @Test(timeout=60000)
    public void checkDefinedTimeout() throws Exception {
        System.getProperties().setProperty(CamelRpcClientPreProcessor.CAMEL_JMS_REQUEST_TIMEOUT_PROPERTY, "12345");

        CamelContext context = getContext();

        CamelRpcRequest<EchoRequest,EchoResponse> wrapper = new CamelRpcRequest<>(new EchoRpcModule(), new EchoRequest(), new HashMap<>());

        CamelRpcClientPreProcessor camelRpcClientPreProcessor = new CamelRpcClientPreProcessor();
        DefaultExchange defaultExchange = new DefaultExchange(context);
        defaultExchange.getIn().setBody(wrapper);
        camelRpcClientPreProcessor.process(defaultExchange);

        context.stop();

        assertEquals(12345L, defaultExchange.getIn().getHeader(CamelRpcConstants.CAMEL_JMS_REQUEST_TIMEOUT_HEADER));
    }

    @Test(timeout=60000)
    public void checkUndefinedTimeout() throws Exception {
        CamelContext context = getContext();

        CamelRpcRequest<EchoRequest,EchoResponse> wrapper = new CamelRpcRequest<>(new EchoRpcModule(), new EchoRequest(), new HashMap<>());

        CamelRpcClientPreProcessor camelRpcClientPreProcessor = new CamelRpcClientPreProcessor();
        DefaultExchange defaultExchange = new DefaultExchange(context);
        defaultExchange.getIn().setBody(wrapper);
        camelRpcClientPreProcessor.process(defaultExchange);

        context.stop();

        assertEquals(CamelRpcClientPreProcessor.CAMEL_JMS_REQUEST_TIMEOUT_DEFAULT, defaultExchange.getIn().getHeader(CamelRpcConstants.CAMEL_JMS_REQUEST_TIMEOUT_HEADER));
    }

    @Test(timeout=60000)
    public void checkZeroTimeout() throws Exception {
        System.getProperties().setProperty(CamelRpcClientPreProcessor.CAMEL_JMS_REQUEST_TIMEOUT_PROPERTY, "0");

        CamelContext context = getContext();

        EchoRequest echoRequest = new EchoRequest();
        CamelRpcRequest<EchoRequest,EchoResponse> wrapper = new CamelRpcRequest<>(new EchoRpcModule(), echoRequest, new HashMap<>());

        CamelRpcClientPreProcessor camelRpcClientPreProcessor = new CamelRpcClientPreProcessor();
        DefaultExchange defaultExchange = new DefaultExchange(context);
        defaultExchange.getIn().setBody(wrapper);
        camelRpcClientPreProcessor.process(defaultExchange);

        context.stop();

        assertEquals(CamelRpcClientPreProcessor.CAMEL_JMS_REQUEST_TIMEOUT_DEFAULT, defaultExchange.getIn().getHeader(CamelRpcConstants.CAMEL_JMS_REQUEST_TIMEOUT_HEADER));
    }

    @Test(timeout=CamelRpcClientPreProcessor.CAMEL_JMS_REQUEST_TIMEOUT_DEFAULT * 4)
    public void throwsRequestTimedOutExceptionOnTimeout() throws Exception {
        assertNotEquals(REMOTE_LOCATION_NAME, identity.getLocation());
        EchoRpcModule echoRpcModule = new EchoRpcModule();

        CamelContext context = getContext();
        context.getShutdownStrategy().setTimeout(5);
        context.getShutdownStrategy().setTimeUnit(TimeUnit.SECONDS);

        CamelRpcServerRouteManager routeManager = getRouteManager(context);
        routeManager.bind(echoRpcModule);

        EchoRequest request = new EchoRequest("HELLO!!!");
        request.setLocation(REMOTE_LOCATION_NAME);
        request.setDelay(CamelRpcClientPreProcessor.CAMEL_JMS_REQUEST_TIMEOUT_DEFAULT * 2);

        try {
            echoClient.execute(request).get();
            fail("Did not get ExecutionException");
        } catch (ExecutionException e) {
            assertTrue("Cause is not of type RequestTimedOutException: " + ExceptionUtils.getStackTrace(e), e.getCause() instanceof RequestTimedOutException);
            // Verify that the exchange error was logged
            MockLogAppender.assertLogMatched(Level.ERROR, "Message History");
            MockLogAppender.assertLogMatched(Level.ERROR, "direct://executeRpc");
            // Verify that the message body was suppressed
            MockLogAppender.assertNoLogMatched(Level.ERROR, "HELLO!!!");
        }

        routeManager.unbind(echoRpcModule);
        context.stop();
    }
}
