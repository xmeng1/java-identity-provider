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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.profile.ActionSupport;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A profile action that selects flows to invoke.
 * 
 * <p>
 * The flows available to be executed are held by the {@link ProfileInterceptorContext}. Available flows are executed in
 * the order that they are configured if their activation condition evaluates to true.
 * </p>
 * 
 * <p>
 * This action returns the flow ID to be executed or null if there are no flows available.
 * to be executed.
 * </p>
 * 
 * @event Selected flow ID to execute
 * @pre <pre>ProfileRequestContext.getSubcontext(ProfileInterceptorContext.class, true) != null</pre>
 */
public class SelectProfileInterceptorFlow extends AbstractProfileInterceptorAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SelectProfileInterceptorFlow.class);

    /** {@inheritDoc} */
    @Override protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {

        // Detect a previous attempted flow, and move it to the incomplete collection.
        // This will prevent re-selecting the same (probably failed) flow again.
        if (interceptorContext.getAttemptedFlow() != null) {
            log.info("{} Flow {} did not complete, moving to incomplete set", getLogPrefix(), interceptorContext
                    .getAttemptedFlow().getId());
            interceptorContext.getIncompleteFlows().put(interceptorContext.getAttemptedFlow().getId(),
                    interceptorContext.getAttemptedFlow());
        }

        return super.doPreExecute(profileRequestContext);
    }

    /** {@inheritDoc} */
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {

        final ProfileInterceptorFlowDescriptor flow = selectUnattemptedFlow(profileRequestContext, interceptorContext);
        if (flow == null) {
            log.debug("{} No flows available to choose from", getLogPrefix());
            return;
        }

        log.debug("{} Selecting flow {}", getLogPrefix(), flow.getId());
        ActionSupport.buildEvent(profileRequestContext, flow.getId());
    }

    /**
     * Select the first potential flow not found in the intermediate flows collection, and that is applicable to the
     * context.
     * 
     * @param profileRequestContext the current IdP profile request context
     * @param interceptorContext the current profile interceptor context
     * @return an eligible flow, or null
     */
    @Nullable private ProfileInterceptorFlowDescriptor selectUnattemptedFlow(
            @Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {
        for (final ProfileInterceptorFlowDescriptor flow : interceptorContext.getAvailableFlows().values()) {
            if (!interceptorContext.getIncompleteFlows().containsKey(flow.getId())) {
                log.debug("{} Checking flow {} for applicability...", getLogPrefix(), flow.getId());
                interceptorContext.setAttemptedFlow(flow);
                if (flow.apply(profileRequestContext)) {
                    return flow;
                }
                log.debug("{} Flow {} was not applicable to this request", getLogPrefix(), flow.getId());

                // Note that we don't exclude this flow from possible future selection, since one flow
                // could in theory do partial work and change the context such that this flow then applies.
            }
        }

        return null;
    }

}