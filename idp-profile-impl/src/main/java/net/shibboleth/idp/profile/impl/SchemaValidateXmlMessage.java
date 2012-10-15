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

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import net.shibboleth.ext.spring.webflow.Event;
import net.shibboleth.ext.spring.webflow.Events;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.EventIds;
import net.shibboleth.idp.profile.ProfileException;
import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.messaging.context.MessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/** An action that schema validates inbound XML messages. */
@Events({@Event(id = EventIds.PROCEED_EVENT_ID),
        @Event(id = EventIds.INVALID_MSG_CTX, description = "No inbound message context or message"),
        @Event(id = SchemaValidateXmlMessage.SCHEMA_INVALID, description = "Inbound message was schema invalid")})
public class SchemaValidateXmlMessage extends AbstractProfileAction<XMLObject, Object> {

    /** ID of the event returned if the incoming request is schema invalid. */
    public static final String SCHEMA_INVALID = "SchemaInvalid";

    /** Class logger. */
    private Logger log = LoggerFactory.getLogger(SchemaValidateXmlMessage.class);

    /** Schema used to validate incoming messages. */
    private final Schema validationSchema;

    /**
     * Constructor.
     * 
     * @param schema schema used to validate incoming messages
     */
    public SchemaValidateXmlMessage(@Nonnull final Schema schema) {
        validationSchema = Constraint.isNotNull(schema, "Schema can not be null");
    }

    /**
     * Gets the schema used to validate incoming messages.
     * 
     * @return schema used to validate incoming messages, not null after action is initialized
     */
    @Nonnull public Schema getValidationSchema() {
        return validationSchema;
    }

    /** {@inheritDoc} */
    protected org.springframework.webflow.execution.Event doExecute(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, ProfileRequestContext<XMLObject, Object> profileRequestContext)
            throws ProfileException {

        log.debug("Action {}: Attempting to schema validate incoming message", getId());

        final MessageContext<XMLObject> msgCtx = profileRequestContext.getInboundMessageContext();
        if (msgCtx == null) {
            log.debug("Action {}: Inbound message context is null, unable to proceed", getId());
            return ActionSupport.buildEvent(this, EventIds.INVALID_MSG_CTX);
        }

        final XMLObject request = msgCtx.getMessage();
        if (request == null) {
            log.debug("Action {}: Inbound message context did not contain a message, unable to proceed", getId());
            return ActionSupport.buildEvent(this, EventIds.INVALID_MSG_CTX);
        }

        final Element requestElem = request.getDOM();
        if (requestElem == null) {
            log.debug("Action {}: Inbound message doesn't contain a DOM, unable to proceed", getId());
            return ActionSupport.buildEvent(this, EventIds.INVALID_MSG_CTX);
        }

        try {
            final Validator schemaValidator = validationSchema.newValidator();
            schemaValidator.validate(new DOMSource(requestElem));
        } catch (SAXException e) {
            log.debug("Action {}: Incoming request {} is not schema valid",
                    new Object[] {getId(), request.getElementQName(), e});
            return ActionSupport.buildEvent(this, SCHEMA_INVALID);
        } catch (IOException e) {
            log.debug("Action {}: Unable to read incoming message");
            return ActionSupport.buildEvent(this, SCHEMA_INVALID);
        }

        log.debug("Action {}: Incoming message is valid", getId());
        return ActionSupport.buildProceedEvent(this);
    }
}