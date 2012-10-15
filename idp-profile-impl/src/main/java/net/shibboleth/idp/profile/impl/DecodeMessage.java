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

package net.shibboleth.idp.profile.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.ext.spring.webflow.Event;
import net.shibboleth.ext.spring.webflow.Events;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.EventIds;
import net.shibboleth.idp.profile.HttpServletRequestMessageDecoderFactory;
import net.shibboleth.idp.profile.ProfileException;
import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.decoder.MessageDecoder;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A profile stage that decodes an incoming request into a given {@link MessageContext}. */
@Events({@Event(id = EventIds.PROCEED_EVENT_ID),
        @Event(id = DecodeMessage.UNABLE_TO_DECODE, description = "An error occured trying to decode the message")})
public class DecodeMessage extends AbstractProfileAction {

    /** ID of the event returned if incoming message could not be decoded. */
    public static final String UNABLE_TO_DECODE = "UnableToDecode";

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(DecodeMessage.class);

    /** Factory used to produce the {@link MessageDecoder} instance used to decode the incoming message. */
    private final HttpServletRequestMessageDecoderFactory decoderFactory;

    /**
     * Constructor.
     * 
     * @param factory factory used to create a {@link MessageDecoder} for an incoming request
     */
    public DecodeMessage(@Nonnull final HttpServletRequestMessageDecoderFactory factory) {
        decoderFactory = Constraint.isNotNull(factory, "Message decoder factory can not be null");
    }

    /** {@inheritDoc} */
    protected org.springframework.webflow.execution.Event
            doExecute(@Nonnull final HttpServletRequest httpRequest, @Nullable final HttpServletResponse httpResponse,
                    @Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {
        try {
            final MessageDecoder decoder = decoderFactory.newDecoder(httpRequest);
            log.debug("Action {}: Using message decoder of type {} for this request", getId(), decoder.getClass()
                    .getName());

            log.debug("Action {}: Decoding incoming request", getId());
            decoder.decode();
            final MessageContext msgContext = decoder.getMessageContext();
            decoder.destroy();
            log.debug("Action {}: Incoming request decoded into a message context of type {}", getId(), msgContext
                    .getClass().getName());

            profileRequestContext.setInboundMessageContext(msgContext);
            return ActionSupport.buildProceedEvent(this);
        } catch (MessageDecodingException e) {
            log.debug("Action {}: Unable to decode incoming request", getId(), e);
            return ActionSupport.buildEvent(this, UNABLE_TO_DECODE);
        }
    }
}