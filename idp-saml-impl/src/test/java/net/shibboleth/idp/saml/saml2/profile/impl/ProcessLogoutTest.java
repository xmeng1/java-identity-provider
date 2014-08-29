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

package net.shibboleth.idp.saml.saml2.profile.impl;

import java.util.Map;

import javax.servlet.http.Cookie;

import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.profile.ActionTestingSupport;
import net.shibboleth.idp.profile.RequestContextBuilder;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.saml.saml2.profile.SAML2ActionTestingSupport;
import net.shibboleth.idp.saml.session.SAML1SPSession;
import net.shibboleth.idp.saml.session.SAML2SPSession;
import net.shibboleth.idp.saml.session.impl.SAML1SPSessionSerializer;
import net.shibboleth.idp.saml.session.impl.SAML2SPSessionSerializer;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.SPSession;
import net.shibboleth.idp.session.SPSessionSerializerRegistry;
import net.shibboleth.idp.session.SessionException;
import net.shibboleth.idp.session.context.LogoutContext;
import net.shibboleth.idp.session.context.SessionContext;
import net.shibboleth.idp.session.criterion.HttpServletRequestCriterion;
import net.shibboleth.idp.session.impl.SessionManagerBaseTestCase;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.net.HttpServletRequestResponseContext;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.profile.SAML2ObjectSupport;
import org.opensaml.storage.StorageSerializer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.Maps;

/** {@link ProcessLogout} unit test. */
public class ProcessLogoutTest extends SessionManagerBaseTestCase {
    
    private RequestContext src;
    
    private ProfileRequestContext prc;
    
    private ProcessLogout action;
    
    @BeforeMethod public void setUpAction() throws ComponentInitializationException {
        src = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);
        
        action = new ProcessLogout();
        action.setSessionResolver(sessionManager);
        action.setSessionManager(sessionManager);
        action.initialize();
    }
    
    /** {@inheritDoc} */
    @Override
    protected void adjustProperties() throws ComponentInitializationException {
        sessionManager.setTrackSPSessions(true);
        sessionManager.setSecondaryServiceIndex(true);
        sessionManager.setSessionSlop(900 * 60 * 1000);
        final SPSessionSerializerRegistry registry = new SPSessionSerializerRegistry();
        final Map<Class<? extends SPSession>,StorageSerializer<? extends SPSession>> mappings = Maps.newHashMap();
        mappings.put(SAML1SPSession.class, new SAML1SPSessionSerializer(900 * 60 * 1000));
        mappings.put(SAML2SPSession.class, new SAML2SPSessionSerializer(900 * 60 * 1000));
        registry.setMappings(mappings);
        registry.initialize();
        sessionManager.setSPSessionSerializerRegistry(registry);
    }
    
    @Test public void testNoMessage() {
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, EventIds.INVALID_PROFILE_CTX);
    }
    
    @Test public void testNoSession() {
        final NameID nameId = SAML2ActionTestingSupport.buildNameID("jdoe");
        prc.getInboundMessageContext().setMessage(SAML2ActionTestingSupport.buildLogoutRequest(nameId));
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertNull(prc.getSubcontext(SessionContext.class));
        Assert.assertNull(prc.getSubcontext(SubjectContext.class));
        Assert.assertNull(prc.getSubcontext(LogoutContext.class));
    }
    
    @Test public void testSessionNoSPSessions() throws SessionException {
        final Cookie cookie = createSession("joe");

        final NameID nameId = SAML2ActionTestingSupport.buildNameID("joe");
        prc.getInboundMessageContext().setMessage(SAML2ActionTestingSupport.buildLogoutRequest(nameId));
        
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        ((MockHttpServletRequest) HttpServletRequestResponseContext.getRequest()).setCookies(cookie);
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertNull(prc.getSubcontext(SessionContext.class));
        Assert.assertNull(prc.getSubcontext(SubjectContext.class));
        Assert.assertNull(prc.getSubcontext(LogoutContext.class));
    }

    @Test public void testSessionOneSPSession() throws SessionException, ResolverException {
        final Cookie cookie = createSession("joe");

        final NameID nameId = SAML2ActionTestingSupport.buildNameID("joe");
        prc.getInboundMessageContext().setMessage(SAML2ActionTestingSupport.buildLogoutRequest(nameId));

        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        ((MockHttpServletRequest) HttpServletRequestResponseContext.getRequest()).setCookies(cookie);
        
        final long creation = System.currentTimeMillis();
        final long expiration = creation + 3600 * 60 * 1000;
        
        final IdPSession session = sessionManager.resolveSingle(new CriteriaSet(new HttpServletRequestCriterion()));
        Assert.assertNotNull(session);
        session.addSPSession(new SAML2SPSession(ActionTestingSupport.INBOUND_MSG_ISSUER, "authn/Password", creation, expiration,
                nameId, "index"));
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        final SubjectContext subjectCtx = prc.getSubcontext(SubjectContext.class);
        Assert.assertNotNull(subjectCtx);
        Assert.assertEquals(subjectCtx.getPrincipalName(), "joe");
        
        final SessionContext sessionCtx = prc.getSubcontext(SessionContext.class);
        Assert.assertNotNull(sessionCtx);
        Assert.assertEquals(session.getId(), sessionCtx.getIdPSession().getId());
        
        final LogoutContext logoutCtx = prc.getSubcontext(LogoutContext.class, true);
        
        SAML2SPSession sp = (SAML2SPSession) logoutCtx.getSessions(ActionTestingSupport.INBOUND_MSG_ISSUER).iterator().next();
        Assert.assertNotNull(sp);
        Assert.assertEquals(sp.getAuthenticationFlowId(), "authn/Password");
        Assert.assertEquals(sp.getCreationInstant(), creation);
        Assert.assertEquals(sp.getExpirationInstant(), expiration);
        Assert.assertTrue(SAML2ObjectSupport.areNameIDsEquivalent(nameId, sp.getNameID()));
        Assert.assertEquals(sp.getSessionIndex(), "index");
    }
    
}