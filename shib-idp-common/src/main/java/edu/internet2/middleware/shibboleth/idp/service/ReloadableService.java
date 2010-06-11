/*
 * Copyright 2010 University Corporation for Advanced Internet Development, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.idp.service;

import org.joda.time.DateTime;

/** A service that supports the reloading of its configuration. */
public interface ReloadableService extends Service {

    /**
     * Reloads the configuration of the service. If a problem occurs with the reload process the service must continue
     * to operate upon its previously, known-good, if it had one.
     * 
     * @throws ServiceException thrown if there is a problem reloading the service
     */
    public void reload() throws ServiceException;

    /**
     * Gets the time when the service was last successfully reloaded.
     * 
     * @return time when the service was last successfully reloaded
     */
    public DateTime getLastSuccessfulReloadInstant();

    /**
     * Gets the time when the service last attempted to reload. If the reload was successful this time should match the
     * time given by {@link #getLastSuccessfulReloadInstant()}.
     * 
     * @return time when the service last attempted to reload
     */
    public DateTime getLastReloadAttemptInstant();

    /**
     * Gets the reason the last reload failed.
     * 
     * @return reason the last reload failed or null if the last reload was successful
     */
    public Throwable getReloadFailureCause();
}