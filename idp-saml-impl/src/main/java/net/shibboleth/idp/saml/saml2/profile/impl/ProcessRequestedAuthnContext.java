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

import java.security.Principal;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.saml.authn.principal.AuthnContextClassRefPrincipal;
import net.shibboleth.idp.saml.authn.principal.AuthnContextDeclRefPrincipal;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.InboundMessageContextLookup;
import org.opensaml.messaging.context.navigate.MessageLookup;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml.saml2.core.AuthnContextDeclRef;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Lists;

/**
 * An authentication action that processes the {@link RequestedAuthnContext} in a SAML 2 {@link AuthnRequest},
 * and populates a {@link RequestedPrincipalContext} with the corresponding information.
 * 
 * <p>Each requested context class or declaration reference is translated into a custom {@link Principal}
 * for use by the authentication subsystem to drive flow selection.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_MSG_CTX}
 * 
 * @post ProfileRequestContext.
 */
public class ProcessRequestedAuthnContext extends AbstractAuthenticationAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ProcessRequestedAuthnContext.class);
    
    /** Lookup strategy function for obtaining {@link AuthnRequest}. */
    @Nonnull private Function<ProfileRequestContext, AuthnRequest> authnRequestLookupStrategy;

    /** The request message to read from. */
    @Nullable private AuthnRequest authnRequest;
    
    /** Constructor. */
    public ProcessRequestedAuthnContext() {
        authnRequestLookupStrategy =
                Functions.compose(new MessageLookup<>(AuthnRequest.class), new InboundMessageContextLookup());
    }

    /**
     * Set the strategy used to locate the {@link AuthnRequest} to read from.
     * 
     * @param strategy lookup strategy
     */
    public synchronized void setAuthnRequestLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, AuthnRequest> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        authnRequestLookupStrategy = Constraint.isNotNull(strategy, "AuthnRequest lookup strategy cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);

        authnRequest = authnRequestLookupStrategy.apply(profileRequestContext);
        if (authnRequest == null) {
            log.debug("{} AuthnRequest message was not returned by lookup strategy", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MSG_CTX);
            return false;
        }
        
        return super.doPreExecute(profileRequestContext, authenticationContext);
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        final RequestedAuthnContext requestedCtx = authnRequest.getRequestedAuthnContext();
        if (requestedCtx == null) {
            log.debug("{} AuthnRequest did not contain a RequestedAuthnContext, nothing to do", getLogPrefix());
            return;
        }

        final List<Principal> principals = Lists.newArrayList();
        
        if (!requestedCtx.getAuthnContextClassRefs().isEmpty()) {
            for (final AuthnContextClassRef ref : requestedCtx.getAuthnContextClassRefs()) {
                if (ref.getAuthnContextClassRef() != null) {
                    principals.add(new AuthnContextClassRefPrincipal(ref.getAuthnContextClassRef()));
                }
            }
        } else if (!requestedCtx.getAuthnContextDeclRefs().isEmpty()) {
            for (final AuthnContextDeclRef ref : requestedCtx.getAuthnContextDeclRefs()) {
                if (ref.getAuthnContextDeclRef() != null) {
                    principals.add(new AuthnContextDeclRefPrincipal(ref.getAuthnContextDeclRef()));
                }
            }
        }
        
        if (principals.isEmpty()) {
            log.debug("{} RequestedAuthnContext did not contain any requested contexts, nothing to do", getLogPrefix());
            return;
        }

        final RequestedPrincipalContext rpCtx = new RequestedPrincipalContext();
        if (requestedCtx.getComparison() != null) {
            rpCtx.setOperator(requestedCtx.getComparison().toString());
        } else {
            rpCtx.setOperator(AuthnContextComparisonTypeEnumeration.EXACT.toString());
        }
        rpCtx.setRequestedPrincipals(principals);
        
        authenticationContext.addSubcontext(rpCtx, true);
        log.debug("{} RequestedPrincipalContext created with operator {} and {} custom principal(s)",
                getLogPrefix(), rpCtx.getOperator(), principals.size());
    }

}