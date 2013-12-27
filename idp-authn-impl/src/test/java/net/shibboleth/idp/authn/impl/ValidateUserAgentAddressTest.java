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

package net.shibboleth.idp.authn.impl;


import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.authn.impl.principal.ExactPrincipalEvalPredicateFactory;
import net.shibboleth.idp.authn.principal.TestPrincipal;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.utilities.java.support.net.IPRange;

import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.ActionTestingSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link ValidateUserAgentAddress} unit test. */
public class ValidateUserAgentAddressTest extends PopulateAuthenticationContextTest {
    
    private ValidateUserAgentAddress action; 
    
    @BeforeMethod public void setUp() throws Exception {
        super.setUp();
        
        action = new ValidateUserAgentAddress();
        action.setMappings(Collections.<String,Collection<IPRange>>singletonMap(
                "foo", Arrays.asList(IPRange.parseCIDRBlock("192.168.1.0/24"))));
        action.setSupportedPrincipals(Arrays.asList(new TestPrincipal("UserAgentAuthentication")));
        action.setHttpServletRequest(new MockHttpServletRequest());
        action.initialize();
    }

    @Test public void testMissingFlow() throws ProfileException {
        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, EventIds.INVALID_PROFILE_CTX);
    }
    
    @Test public void testMissingAddress() throws ProfileException {
        prc.getSubcontext(AuthenticationContext.class, false).setAttemptedFlow(authenticationFlows.get(0));
        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.NO_CREDENTIALS);
    }

    @Test public void testMissingAddress2() throws Exception {
        ((MockHttpServletRequest) action.getHttpServletRequest()).setRemoteAddr(null);

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class, false);
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        doExtract(prc);
        
        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.NO_CREDENTIALS);
    }

    @Test public void testUnauthorized() throws Exception {
        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class, false);
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        doExtract(prc);
        
        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.INVALID_CREDENTIALS);
    }

    @Test public void testIncompatible() throws Exception {
        ((MockHttpServletRequest) action.getHttpServletRequest()).setRemoteAddr("192.168.1.1");

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class, false);
        ac.setAttemptedFlow(authenticationFlows.get(0));
        ac.getPrincipalEvalPredicateFactoryRegistry().register(
                TestPrincipal.class, "exact", new ExactPrincipalEvalPredicateFactory());
        
        RequestedPrincipalContext rpc = new RequestedPrincipalContext("exact",
                Arrays.<Principal>asList(new TestPrincipal("PasswordAuthentication")));
        ac.addSubcontext(rpc, true);
        
        doExtract(prc);
        
        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.REQUEST_UNSUPPORTED);
    }

    @Test public void testCompatible() throws Exception {
        ((MockHttpServletRequest) action.getHttpServletRequest()).setRemoteAddr("192.168.1.1");

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class, false);
        ac.setAttemptedFlow(authenticationFlows.get(0));
        ac.getPrincipalEvalPredicateFactoryRegistry().register(
                TestPrincipal.class, "exact", new ExactPrincipalEvalPredicateFactory());
        
        RequestedPrincipalContext rpc = new RequestedPrincipalContext("exact",
                Arrays.<Principal>asList(new TestPrincipal("UserAgentAuthentication")));
        ac.addSubcontext(rpc, true);
        
        doExtract(prc);
        
        action.execute(prc);
        ActionTestingSupport.assertProceedEvent(prc);
        Assert.assertNotNull(ac.getAuthenticationResult());
        Assert.assertEquals(ac.getAuthenticationResult().getSubject().getPrincipals(
                UsernamePrincipal.class).iterator().next().getName(), "foo");
    }
    
    private void doExtract(ProfileRequestContext prc) throws Exception {
        ExtractUserAgentAddress extract = new ExtractUserAgentAddress();
        extract.setHttpServletRequest(action.getHttpServletRequest());
        extract.initialize();
        extract.execute(prc);
    }
    
    
}