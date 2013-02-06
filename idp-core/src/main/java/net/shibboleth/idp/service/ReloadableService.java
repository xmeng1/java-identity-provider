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

package net.shibboleth.idp.service;

import javax.annotation.Nullable;

import org.joda.time.DateTime;

/** A service that supports reloading its configuration. */
public interface ReloadableService extends Service {
    
    /** Indicates the service is in the process of reloading its configuration files. */
    public static final String STATE_RELOADING = "reloading";

    /**
     * Gets the time when the service was last successfully reloaded.
     * 
     * @return time when the service was last successfully reloaded
     */
    @Nullable public DateTime getLastSuccessfulReloadInstant();

    /**
     * Gets the time when the service last attempted to reload. If the reload was successful this time should match the
     * time given by {@link #getLastSuccessfulReloadInstant()}.
     * 
     * @return time when the service last attempted to reload
     */
    @Nullable public DateTime getLastReloadAttemptInstant();

    /**
     * Gets the reason the last reload failed.
     * 
     * @return reason the last reload failed or null if the last reload was successful
     */
    @Nullable public Throwable getReloadFailureCause();

    /**
     * Reloads the configuration of the service. Whether internal state is maintained between reloads is implementation
     * dependent.
     * 
     * @throws ServiceException thrown if there is a problem reloading the service
     */
    public void reload() throws ServiceException;
}