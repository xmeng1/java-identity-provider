/*
 * Licensed to the University Corporation for Advanced Internet Development,
 * Inc. (UCAID) under one or more contributor license agreements.  See the
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.cas.flow;

import net.shibboleth.idp.cas.config.LoginConfiguration;
import net.shibboleth.idp.cas.protocol.ProtocolError;
import net.shibboleth.idp.cas.protocol.TicketValidationRequest;
import net.shibboleth.idp.cas.ticket.ProxyGrantingTicket;
import net.shibboleth.idp.cas.ticket.ProxyTicket;
import net.shibboleth.idp.cas.ticket.ServiceTicket;
import net.shibboleth.idp.cas.ticket.TicketService;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import org.springframework.webflow.execution.RequestContext;
import org.testng.annotations.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Unit test for {@link ValidateTicketAction} class.
 *
 * @author Marvin S. Addison
 */
public class ValidateTicketActionTest extends AbstractFlowActionTest {

    private static final String TEST_SERVICE = "https://example.com/widget";

    @Test
    public void testInvalidTicketFormat() throws Exception {
        final RequestContext context = new TestContextBuilder(LoginConfiguration.PROFILE_ID)
                .addProtocolContext(new TicketValidationRequest(TEST_SERVICE, "AB-1234-012346abcdef"), null)
                .build();
        assertEquals(newAction(ticketService).execute(context).getId(), ProtocolError.InvalidTicketFormat.id());
    }

    @Test
    public void testServiceMismatch() throws Exception {
        final ServiceTicket ticket = createServiceTicket(TEST_SERVICE, false);
        final RequestContext context = new TestContextBuilder(LoginConfiguration.PROFILE_ID)
                .addProtocolContext(new TicketValidationRequest("mismatch", ticket.getId()), null)
                .build();
        assertEquals(newAction(ticketService).execute(context).getId(), ProtocolError.ServiceMismatch.id());
    }

    @Test
    public void testTicketExpired() throws Exception {
        final ServiceTicket ticket = createServiceTicket(TEST_SERVICE, false);
        final RequestContext context = new TestContextBuilder(LoginConfiguration.PROFILE_ID)
                .addProtocolContext(new TicketValidationRequest(TEST_SERVICE, ticket.getId()), null)
                .build();
        // Remove the ticket prior to validation to simulate expiration
        ticketService.removeServiceTicket(ticket.getId());
        assertEquals(newAction(ticketService).execute(context).getId(), ProtocolError.TicketExpired.id());
    }

    @Test
    public void testTicketRetrievalError() throws Exception {
        final TicketService throwingTicketService = mock(TicketService.class);
        when(throwingTicketService.removeServiceTicket(any(String.class))).thenThrow(new RuntimeException("Broken"));
        final RequestContext context = new TestContextBuilder(LoginConfiguration.PROFILE_ID)
                .addProtocolContext(new TicketValidationRequest(TEST_SERVICE, "ST-12345"), null)
                .build();
        assertEquals(
                newAction(throwingTicketService).execute(context).getId(),
                ProtocolError.TicketRetrievalError.id());
    }

    @Test
    public void testServiceTicketValidateSuccess() throws Exception {
        final ServiceTicket ticket = createServiceTicket(TEST_SERVICE, false);
        final RequestContext context = new TestContextBuilder(LoginConfiguration.PROFILE_ID)
                .addProtocolContext(new TicketValidationRequest(TEST_SERVICE, ticket.getId()), null)
                .build();
        final ValidateTicketAction action = newAction(ticketService);
        assertEquals(action.execute(context).getId(), Events.ServiceTicketValidated.id());
        assertNotNull(action.getCASResponse(getProfileContext(context)));
    }


    @Test
    public void testProxyTicketValidateSuccess() throws Exception {
        final ServiceTicket st = createServiceTicket(TEST_SERVICE, false);
        final ProxyGrantingTicket pgt = createProxyGrantingTicket(st);
        final ProxyTicket pt = createProxyTicket(pgt, "proxyA");
        final RequestContext context = new TestContextBuilder(LoginConfiguration.PROFILE_ID)
                .addProtocolContext(new TicketValidationRequest("proxyA", pt.getId()), null)
                .build();
        final ValidateTicketAction action = newAction(ticketService);
        assertEquals(action.execute(context).getId(), Events.ProxyTicketValidated.id());
        assertNotNull(action.getCASResponse(getProfileContext(context)));
    }

    private static ValidateTicketAction newAction(final TicketService service) {
        final ValidateTicketAction action = new ValidateTicketAction(service);
        try {
            action.initialize();
        } catch (ComponentInitializationException e) {
            throw new RuntimeException("Initialization error", e);
        }
        return action;
    }
}