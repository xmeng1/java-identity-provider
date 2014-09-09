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

package net.shibboleth.idp.profile.interceptor;

import net.shibboleth.idp.profile.interceptor.ProfileInterceptorContext;
import net.shibboleth.idp.profile.interceptor.SelectProfileInterceptorFlow;

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.base.Predicates;

/** {@link SelectProfileInterceptorFlow} unit test. */
public class SelectProfileInterceptorFlowTest extends PopulateProfileInterceptorContextTest {

    private SelectProfileInterceptorFlow action;

    private ProfileInterceptorContext interceptorCtx;

    @BeforeMethod public void setUp() throws Exception {
        super.setUp();

        action = new SelectProfileInterceptorFlow();
        action.initialize();

        interceptorCtx = prc.getSubcontext(ProfileInterceptorContext.class, false);
    }

    @Test public void testSelect() {

        final Event event = action.execute(src);

        Assert.assertEquals(interceptorCtx.getAttemptedFlow(), interceptorCtx.getAvailableFlows().get(event.getId()));
        Assert.assertEquals(interceptorCtx.getAttemptedFlow().getId(), "test1");
    }

    @Test public void testIncompleteFlows() {
        interceptorCtx.getIncompleteFlows().put("test1", interceptorCtx.getAvailableFlows().get("test1"));

        final Event event = action.execute(src);

        Assert.assertEquals(interceptorCtx.getAttemptedFlow(), interceptorCtx.getAvailableFlows().get(event.getId()));
        Assert.assertEquals(interceptorCtx.getAttemptedFlow().getId(), "test2");
    }

    @Test public void testPredicate() {
        interceptorCtx.getAvailableFlows().get("test1")
                .setActivationCondition(Predicates.<ProfileRequestContext> alwaysFalse());

        final Event event = action.execute(src);

        Assert.assertEquals(interceptorCtx.getAttemptedFlow(), interceptorCtx.getAvailableFlows().get(event.getId()));
        Assert.assertEquals(interceptorCtx.getAttemptedFlow().getId(), "test2");
    }

}