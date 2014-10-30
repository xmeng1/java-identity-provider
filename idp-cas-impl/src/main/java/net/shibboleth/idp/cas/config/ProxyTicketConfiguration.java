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

package net.shibboleth.idp.cas.config;

import javax.annotation.Nonnull;

/**
 * CAS proxy ticket configuration modeled as an IdP profile.
 *
 * @author Marvin S. Addison
 */
public class ProxyTicketConfiguration extends AbstractTicketConfiguration {

    /** Proxy ticket profile URI. */
    public static final String PROFILE_ID = PROTOCOL_URI + "/pt";

    /** Default ticket prefix. */
    public static final String DEFAULT_TICKET_PREFIX = "PT";

    /** Default ticket length (random part). */
    public static final int DEFAULT_TICKET_LENGTH = 25;


    /** Creates a new instance. */
    public ProxyTicketConfiguration() {
        super(PROFILE_ID);
    }

    @Override
    @Nonnull
    protected String getDefaultTicketPrefix() {
        return DEFAULT_TICKET_PREFIX;
    }

    @Override
    @Nonnull
    protected int getDefaultTicketLength() {
        return DEFAULT_TICKET_LENGTH;
    }
}
