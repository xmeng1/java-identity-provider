/*
 * Copyright [2006] [University Corporation for Advanced Internet Development, Inc.]
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

package edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.AbstractResolutionPlugIn;

/**
 * Base class for {@link DataConnector} plug-ins.
 */
public abstract class BaseDataConnector extends AbstractResolutionPlugIn<Map<String, Attribute>> implements
        DataConnector {

    /** ID of the data connector to use if this one fails. */
    private List<String> failoverDependencyIds;
    
    /** Constructor. */
    public BaseDataConnector(){
        super();
        failoverDependencyIds = new ArrayList<String>();
    }

    /** {@inheritDoc} */
    public List<String> getFailoverDependencyIds() {
        return failoverDependencyIds;
    }

    /**
     * Set the IDs of data connectors to use if this one fails.
     * 
     * @param ids IDs of data connectors to use if this one fails
     */
    public void setFailoverDependencyIds(List<String> ids) {
        failoverDependencyIds = ids;
    }
}