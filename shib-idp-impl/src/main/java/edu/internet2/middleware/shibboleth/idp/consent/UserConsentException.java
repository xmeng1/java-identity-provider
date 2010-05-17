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

package edu.internet2.middleware.shibboleth.idp.consent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class UserConsentException extends Throwable {    
    /**
     * 
     */
    private static final long serialVersionUID = 3905215080482737756L;
    private final Logger logger = LoggerFactory.getLogger(UserConsentException.class);

    /**
     * Constructor
     *
     * @param message
     * @param cause
     */
    public UserConsentException(String message, Throwable cause) {    
    	super(message, cause);
    	//TODO
    	//logger.error(message, cause);
    }

    /**
     * Constructor
     *
     * @param message
     */
    public UserConsentException(String message) {
        super(message);
        logger.error(message);
    }
    
    /**
     * Constructor
     *
     * @param message
     */
    public UserConsentException(String message, Object... arguments) {
        super(message);
        logger.error(message, arguments);
    }
}
