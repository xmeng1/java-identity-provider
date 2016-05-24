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

import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.MultiFactorAuthenticationTransition;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.MultiFactorAuthenticationContext;
import net.shibboleth.idp.profile.ActionTestingSupport;
import net.shibboleth.idp.profile.RequestContextBuilder;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;

/** Base class for further action tests. */
public class BaseMultiFactorAuthenticationContextTest {

    protected RequestContext src;
    protected ProfileRequestContext prc;
    protected AuthenticationContext ac;
    protected MultiFactorAuthenticationContext mfa;
    protected ImmutableMap<String,AuthenticationFlowDescriptor> authenticationFlows;

    protected void initializeMembers() throws Exception {
        src = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);
        ac = (AuthenticationContext) prc.addSubcontext(new AuthenticationContext(), true);

        ImmutableMap.Builder<String,AuthenticationFlowDescriptor> builder = ImmutableMap.builder();
        
        AuthenticationFlowDescriptor flow = new AuthenticationFlowDescriptor();
        flow.setId("authn/MFA");
        flow.setResultSerializer(new DefaultAuthenticationResultSerializer());
        flow.initialize();
        builder.put("authn/MFA", flow);
        ac.setAttemptedFlow(flow);
        
        flow = new AuthenticationFlowDescriptor();
        flow.setId("authn/test1");
        flow.setResultSerializer(new DefaultAuthenticationResultSerializer());
        flow.initialize();
        builder.put("authn/test1", flow);

        flow = new AuthenticationFlowDescriptor();
        flow.setId("authn/test2");
        flow.setResultSerializer(new DefaultAuthenticationResultSerializer());
        flow.initialize();
        builder.put("authn/test2", flow);

        flow = new AuthenticationFlowDescriptor();
        flow.setId("authn/test3");
        flow.setResultSerializer(new DefaultAuthenticationResultSerializer());
        flow.initialize();
        builder.put("authn/test3", flow);

        authenticationFlows = builder.build();
    }

    protected void setUp() throws Exception {        
        initializeMembers();
        
        final PopulateAuthenticationContext action = new PopulateAuthenticationContext();
        action.setAvailableFlows(authenticationFlows.values());
        action.setPotentialFlows(Collections.singletonList(authenticationFlows.get("authn/MFA")));
        action.initialize();

        action.execute(src);
        
        final Map<String,MultiFactorAuthenticationTransition> transitionMap = new HashMap<>();
        MultiFactorAuthenticationTransition transition = new MultiFactorAuthenticationTransition();
        transition.setNextFlow("authn/test1");
        transition.setCompletionCondition(Predicates.<ProfileRequestContext>alwaysFalse());
        transitionMap.put(null, transition);

        transition = new MultiFactorAuthenticationTransition();
        transition.setNextFlow("interim");
        transition.setCompletionCondition(Predicates.<ProfileRequestContext>alwaysFalse());
        transitionMap.put("authn/test1", transition);

        transition = new MultiFactorAuthenticationTransition();
        transition.setNextFlow("authn/test2");
        transition.setCompletionCondition(Predicates.<ProfileRequestContext>alwaysFalse());
        transitionMap.put("interim", transition);

        transition = new MultiFactorAuthenticationTransition();
        transition.setCompletionCondition(Predicates.<ProfileRequestContext>alwaysTrue());
        transitionMap.put("authn/test2", transition);

        final PopulateMultiFactorAuthenticationContext mfaaction = new PopulateMultiFactorAuthenticationContext();
        mfaaction.setTransitionMap(transitionMap);
        mfaaction.initialize();
        ActionTestingSupport.assertProceedEvent(mfaaction.execute(src));
        
        mfa = ac.getSubcontext(MultiFactorAuthenticationContext.class);
        Assert.assertNotNull(mfa);
    }

}