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

package net.shibboleth.idp.saml.impl.profile.saml1;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import net.shibboleth.idp.profile.AbstractProfileAction;

import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.OutboundMessageContextLookup;

import net.shibboleth.idp.profile.context.SpringRequestContext;
import net.shibboleth.idp.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.idp.relyingparty.RelyingPartyContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.context.navigate.MessageLookup;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.saml1.core.Response;
import org.opensaml.saml.saml1.core.Status;
import org.opensaml.saml.saml1.core.StatusCode;
import org.opensaml.saml.saml1.core.StatusMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.NoSuchMessageException;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

/**
 * Action that sets {@link Status} content in a {@link Response} obtained from
 * a lookup strategy, typically from the outbound message context.
 * 
 * <p>If the message already contains status information, this action will overwrite it.</p>
 * 
 * <p>Options allows for the creation of a {@link StatusMessage} either explicitly,
 * or through the mapping of the previous event ID. The action is Spring-aware in order
 * to obtain the previous Event to map, to do message lookup, and to obtain the active
 * {@link java.util.Locale}.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_MSG_CTX}
 * @event {@link IdPEventIds#INVALID_RELYING_PARTY_CTX}
 * @event {@link IdPEventIds#INVALID_RELYING_PARTY_CONFIG}
 */
public class AddStatusToResponse extends AbstractProfileAction<Object, Response> {

    /** Class logger. */
    @Nonnull private Logger log = LoggerFactory.getLogger(AddStatusToResponse.class);

    /**
     * Strategy used to locate the {@link RelyingPartyContext} associated with a given {@link ProfileRequestContext}.
     */
    @Nonnull private Function<ProfileRequestContext, RelyingPartyContext> relyingPartyContextLookupStrategy;

    /** Strategy used to locate the {@link Response} to operate on. */
    @Nonnull private Function<ProfileRequestContext<Object,Response>, Response> responseLookupStrategy;
    
    /** One or more status codes to insert. */
    @Nonnull @NonnullElements private List<QName> statusCodes;
    
    /** A default status message to include. */
    @Nullable private String statusMessage;
    
    /** Whether to create a status message based on the last Web Flow Event. */
    private boolean statusMessageFromEvent;
    
    /** Whether to include detailed status information. */
    private boolean detailedStatus;
    
    /** Spring WebFlow request context. */
    @Nullable private RequestContext springRequestContext;
    
    /** Response to modify. */
    @Nullable private Response response;
    
    /** Constructor. */
    public AddStatusToResponse() {
        relyingPartyContextLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class, false);
        responseLookupStrategy =
                Functions.compose(new MessageLookup<Response>(), new OutboundMessageContextLookup<Response>());
        statusCodes = Collections.emptyList();
        statusMessageFromEvent = true;
        detailedStatus = false;
    }

    /**
     * Set the strategy used to locate the {@link RelyingPartyContext} associated with a given
     * {@link ProfileRequestContext}.
     * 
     * @param strategy strategy used to locate the {@link RelyingPartyContext} associated with a given
     *            {@link ProfileRequestContext}
     */
    public synchronized void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, RelyingPartyContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        relyingPartyContextLookupStrategy =
                Constraint.isNotNull(strategy, "RelyingPartyContext lookup strategy cannot be null");
    }

    /**
     * Set the strategy used to locate the {@link Response} to operate on.
     * 
     * @param strategy strategy used to locate the {@link Response} to operate on
     */
    public synchronized void setResponseLookupStrategy(
            @Nonnull final Function<ProfileRequestContext<Object,Response>, Response> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        responseLookupStrategy = Constraint.isNotNull(strategy, "Response lookup strategy cannot be null");
    }
    
    /**
     * Set the list of status code values to insert, ordered such that the top level code is first
     * and every other code will be nested inside the previous one.
     * 
     * @param codes list of status code values to insert
     */
    public synchronized void setStatusCodes(@Nonnull @NonnullElements List<QName> codes) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        Constraint.isNotNull(codes, "Status code list cannot be null");
        statusCodes = Lists.newArrayList(Collections2.filter(codes, Predicates.notNull()));
    }
    
    /**
     * Set a default status message to use.
     * 
     * <p>If set, the {@link StatusMessage} element will be set to this value, unless
     * {@link #statusMessageFromEvent} is true, the event exists and is mappable to a
     * message, and {@link RelyingPartyConfiguration#isDetailedErrors()} is also true. 
     * 
     * @param message default status message
     */
    public synchronized void setStatusMessage(@Nullable final String message) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        statusMessage = StringSupport.trimOrNull(message);
    }
    
    /**
     * Set whether to try and map the last WebFlow Event to a status message.
     * 
     * @param flag  flag to set
     */
    public synchronized void setStatusMessageFromEvent(final boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        statusMessageFromEvent = flag;
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext<Object, Response> profileRequestContext)
            throws ProfileException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);

        response = responseLookupStrategy.apply(profileRequestContext);
        if (response == null) {
            log.debug("{} Response message was not returned by lookup strategy", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MSG_CTX);
            return false;
        }

        final RelyingPartyContext relyingPartyCtx = relyingPartyContextLookupStrategy.apply(profileRequestContext);
        if (relyingPartyCtx != null) {
            final RelyingPartyConfiguration relyingPartyConfig = relyingPartyCtx.getConfiguration();
            if (relyingPartyConfig != null) {
                detailedStatus = relyingPartyConfig.isDetailedErrors();
            }
        }
        
        log.debug("{} Detailed status information is {}", getLogPrefix(), detailedStatus ? "enabled" : "disabled");
        
        // Save off Spring context for later use.
        SpringRequestContext springContext = profileRequestContext.getSubcontext(SpringRequestContext.class, false);
        if (springContext != null) {
            springRequestContext = springContext.getRequestContext();
        }
        
        return super.doPreExecute(profileRequestContext);
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext<Object, Response> profileRequestContext)
            throws ProfileException {

        final SAMLObjectBuilder<Status> statusBuilder = (SAMLObjectBuilder<Status>)
                XMLObjectProviderRegistrySupport.getBuilderFactory().<Status>getBuilderOrThrow(Status.TYPE_NAME);

        final Status status = statusBuilder.buildObject();
        response.setStatus(status);
        buildStatusCode(status);
                
        // StatusMessage processing.
        if (!detailedStatus || !statusMessageFromEvent) {
            if (statusMessage != null) {
                log.debug("{} Setting StatusMessage to defaulted value", getLogPrefix());
                buildStatusMessage(status, statusMessage);
            }
        } else {
            final Event previousEvent = springRequestContext != null ? springRequestContext.getCurrentEvent() : null;
            if (previousEvent != null) {
                try {
                    final String message = getMessage(previousEvent.getId(), null,
                            springRequestContext.getExternalContext().getLocale());
                    log.debug("{} Event {} was mappable, setting StatusMessage to mapped value",
                            getLogPrefix(), previousEvent.getId());
                    buildStatusMessage(status, message);
                } catch (NoSuchMessageException e) {
                    if (statusMessage != null) {
                        log.debug("{} Event {} was not mappable, setting StatusMessage to defaulted value",
                                getLogPrefix(), previousEvent.getId());
                        buildStatusMessage(status, statusMessage);
                    }
                }
            } else if (statusMessage != null) {
                log.debug("{} No Event to map, setting StatusMessage to defaulted value", getLogPrefix());
                buildStatusMessage(status, statusMessage);
            }
        }
    }
    
    /**
     * Build and attach {@link StatusCode} element.
     * 
     * @param status    the element to attach to
     */
    private void buildStatusCode(@Nonnull final Status status) {
        final SAMLObjectBuilder<StatusCode> statusCodeBuilder = (SAMLObjectBuilder<StatusCode>)
                XMLObjectProviderRegistrySupport.getBuilderFactory().<StatusCode>getBuilderOrThrow(
                        StatusCode.TYPE_NAME);

        // Build nested StatusCodes.
        StatusCode statusCode = statusCodeBuilder.buildObject();
        status.setStatusCode(statusCode);
        if (statusCodes.isEmpty()) {
            statusCode.setValue(StatusCode.RESPONDER);
        } else {
            statusCode.setValue(statusCodes.get(0));
            final Iterator<QName> i = statusCodes.iterator();
            i.next();
            while (i.hasNext()) {
                final StatusCode subcode = statusCodeBuilder.buildObject();
                subcode.setValue(i.next());
                statusCode.setStatusCode(subcode);
                statusCode = subcode;
            }
        }
    }
    
    /**
     * Build and attach {@link StatusMessage} element.
     * 
     * @param status    the element to attach to
     * @param message   the message to set
     */
    private void buildStatusMessage(@Nonnull final Status status, @Nonnull @NotEmpty final String message) {
        final SAMLObjectBuilder<StatusMessage> statusMessageBuilder = (SAMLObjectBuilder<StatusMessage>)
                XMLObjectProviderRegistrySupport.getBuilderFactory().<StatusMessage>getBuilderOrThrow(
                        StatusMessage.DEFAULT_ELEMENT_NAME);
        final StatusMessage sm = statusMessageBuilder.buildObject();
        sm.setMessage(message);
        status.setStatusMessage(sm);
    }
    
}