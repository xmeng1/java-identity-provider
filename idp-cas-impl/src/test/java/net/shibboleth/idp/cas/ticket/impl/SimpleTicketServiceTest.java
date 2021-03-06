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

package net.shibboleth.idp.cas.ticket.impl;

import net.shibboleth.idp.cas.ticket.ProxyGrantingTicket;
import net.shibboleth.idp.cas.ticket.ProxyTicket;
import net.shibboleth.idp.cas.ticket.ServiceTicket;
import net.shibboleth.idp.cas.ticket.TicketState;
import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.opensaml.storage.impl.MemoryStorageService;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit test for {@link SimpleTicketService} class.
 *
 * @author Marvin S. Addison
 */
public class SimpleTicketServiceTest {

    private static final String TEST_SESSION_ID = "jHXRo42W0ATPEN+X5Zk1cw==";

    private static final String TEST_SERVICE = "https://example.com/widget";

    private SimpleTicketService ticketService;

    @BeforeTest
    public void setUp() throws Exception {
        final MemoryStorageService ss = new MemoryStorageService();
        ss.setId("shibboleth.StorageService");
        ss.initialize();
        ticketService = new SimpleTicketService(ss);
    }


    @Test
    public void testCreateRemoveServiceTicket() throws Exception {
        final ServiceTicket st = createServiceTicket();
        assertNotNull(st);
        assertNotNull(st.getTicketState().getSessionId());
        assertNotNull(st.getTicketState().getPrincipalName());
        final ServiceTicket st2 = ticketService.removeServiceTicket(st.getId());
        assertEquals(st, st2);
        assertEquals(st.getExpirationInstant(), st2.getExpirationInstant());
        assertEquals(st.getService(), st2.getService());
        assertEquals(st.getTicketState(), st2.getTicketState());
        assertNull(ticketService.removeServiceTicket(st.getId()));
    }

    @Test
    public void testCreateFetchRemoveProxyGrantingTicket() throws Exception {
        final ProxyGrantingTicket pgt = createProxyGrantingTicket();
        assertNotNull(pgt);
        assertNotNull(pgt.getTicketState().getSessionId());
        assertNotNull(pgt.getTicketState().getPrincipalName());
        final ProxyGrantingTicket pgt2 = ticketService.fetchProxyGrantingTicket(pgt.getId());
        assertEquals(pgt, pgt2);
        assertEquals(pgt.getExpirationInstant(), pgt2.getExpirationInstant());
        assertEquals(pgt.getService(), pgt2.getService());
        assertEquals(pgt.getTicketState(), pgt2.getTicketState());
        assertEquals(ticketService.removeProxyGrantingTicket(pgt.getId()), pgt);
        assertNull(ticketService.removeProxyGrantingTicket(pgt.getId()));
    }

    @Test
    public void testCreateRemoveProxyTicket() throws Exception {
        final ProxyTicket pt = ticketService.createProxyTicket(
                new TicketIdentifierGenerationStrategy("PT", 25).generateIdentifier(),
                expiry(),
                createProxyGrantingTicket(),
                TEST_SERVICE);
        assertNotNull(pt);
        assertNotNull(pt.getTicketState().getSessionId());
        assertNotNull(pt.getTicketState().getPrincipalName());
        final ProxyTicket pt2 = ticketService.removeProxyTicket(pt.getId());
        assertEquals(pt, pt2);
        assertEquals(pt.getExpirationInstant(), pt2.getExpirationInstant());
        assertEquals(pt.getService(), pt2.getService());
        assertEquals(pt.getTicketState(), pt2.getTicketState());
        assertNull(ticketService.removeProxyTicket(pt.getId()));
    }

    private ServiceTicket createServiceTicket() {
        return ticketService.createServiceTicket(
                new TicketIdentifierGenerationStrategy("ST", 25).generateIdentifier(),
                expiry(),
                TEST_SERVICE,
                new TicketState(TEST_SESSION_ID, "bob", Instant.now(), "Password"),
                false);
    }

    private ProxyGrantingTicket createProxyGrantingTicket() {
        return ticketService.createProxyGrantingTicket(
                new TicketIdentifierGenerationStrategy("PGT", 50).generateIdentifier(),
                expiry(),
                createServiceTicket());
    }

    private static Instant expiry() {
        return DateTime.now().plusSeconds(10).toInstant();
    }
}
