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

package net.shibboleth.idp.session;

import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.utilities.java.support.annotation.Duration;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Positive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.component.IdentifiableComponent;


/**
 * An identity provider session belonging to a particular subject and client device.
 */
@ThreadSafe
public interface IdPSession extends IdentifiableComponent {

    /** Name of {@link org.slf4j.MDC} attribute that holds the current session ID: <code>idp.session.id</code>. */
    public static final String MDC_ATTRIBUTE = "idp.session.id";

    /**
     * Get the canonical principal name for the session.
     * 
     * @return the principal name
     */
    @Nonnull @NotEmpty public String getPrincipalName();

    /**
     * Get the time, in milliseconds since the epoch, when this session was created.
     * 
     * @return time this session was created, never less than 0
     */
    @Positive public long getCreationInstant();
    
    /**
     * Get the last activity instant, in milliseconds since the epoch, for the session.
     * 
     * @return last activity instant, in milliseconds since the epoch, for the session, never less than 0
     */
    @Positive public long getLastActivityInstant();

    /**
     * Set the last activity instant, in milliseconds since the epoch, for the session.
     * 
     * @param instant last activity instant, in milliseconds since the epoch, for the session, must be greater than 0
     * 
     * @throws SessionException if an error occurs updating the session
     */
    public void setLastActivityInstant(@Duration @Positive final long instant) throws SessionException;

    /**
     * Set the last activity instant, in milliseconds since the epoch, for the session to the current time.
     * 
     * @throws SessionException if an error occurs updating the session
     */
    public void setLastActivityInstantToNow() throws SessionException;
    
    /**
     * Test the session's validity based on the supplied client address and the applicable
     * lifetime and timeout limitations.
     * 
     * @param address client address for validation
     * 
     * @return true iff the session is valid for the specified client
     * @throws SessionException if an error occurs validating the session
     */
    public boolean validate(@Nonnull @NotEmpty final String address) throws SessionException;
    
    /**
     * Get the unmodifiable set of {@link AuthenticationResult}s associated with this session.
     * 
     * @return unmodifiable set of results
     */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public Set<AuthenticationResult> getAuthenticationResults();

    /**
     * Get an associated {@link AuthenticationResult} given its flow ID.
     * 
     * @param flowId the ID of the {@link AuthenticationResult}
     * 
     * @return the authentication result, or null
     */
    @Nullable public AuthenticationResult getAuthenticationResult(@Nonnull @NotEmpty final String flowId);

    /**
     * Add a new {@link AuthenticationResult} to this {@link BaseIdPSession}, replacing any
     * existing result of the same flow ID.
     * 
     * @param result the result to add
     * 
     * @throws SessionException if an error occurs updating the session
     */
    public void addAuthenticationResult(@Nonnull final AuthenticationResult result) throws SessionException;
    
    /**
     * Disassociate an {@link AuthenticationResult} from this IdP session.
     * 
     * @param result the result to disassociate
     * 
     * @return true iff the given result had been associated with this IdP session and now is not
     * @throws SessionException if an error occurs accessing the session
     */
    public boolean removeAuthenticationResult(@Nonnull final AuthenticationResult result) throws SessionException;
    
    /**
     * Gets the unmodifiable collection of service sessions associated with this session.
     * 
     * @return unmodifiable collection of service sessions associated with this session
     */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public Set<ServiceSession> getServiceSessions();

    /**
     * Get the ServiceSession for a given service.
     * 
     * @param serviceId ID of the service
     * 
     * @return the session service or null if no session exists for that service, may be null
     */
    @Nullable public ServiceSession getServiceSession(@Nonnull @NotEmpty final String serviceId);
    
    /**
     * Add a new service session to this IdP session, replacing any existing session for the same
     * service.
     * 
     * @param serviceSession the service session
     * @throws SessionException if an error occurs accessing the session
     */
    public void addServiceSession(@Nonnull final ServiceSession serviceSession) throws SessionException;
    
    /**
     * Disassociate the given service session from this IdP session.
     * 
     * @param session the service session
     * 
     * @return true iff the given session had been associated with this IdP session and now is not
     * @throws SessionException if an error occurs accessing the session
     */
    public boolean removeServiceSession(@Nonnull final ServiceSession session) throws SessionException;
    
}