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

package net.shibboleth.idp.attribute.filter.impl.saml.attributemapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.ScopedStringAttributeValue;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.core.xml.XMLObject;

/**
 * Mapping to extract a {@link net.shibboleth.idp.attribute.ScopedStringAttributeValue} from an AttributeValue.
 */
public class ScopedStringAttributeValueMapper extends BaseAttributeValueMapper {

    /** The scope delimiter. */
    private String delimiter = "@";

    /**
     * Returns the delimiter.
     * 
     * @return the delimiter
     */
    @Nonnull public String getDelimiter() {
        return delimiter;
    }

    /**
     * Sets the delimiter.
     * 
     * @param delim what to set.
     */
    public void setDelimiter(@Nonnull @NotEmpty String delim) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        String trimmed = StringSupport.trimOrNull(delim);
        Constraint.isNotNull(trimmed, "ScopedStringAttributeDecoder: delimiter can not be null");
        delimiter = delim;
    }

    /** {@inheritDoc} */
    @Nullable protected AttributeValue decodeValue(final XMLObject object) {
        final String stringValue = getStringValue(object);

        if (null == stringValue) {
            return null;
        }

        final int offset = stringValue.indexOf(getDelimiter());

        if (offset < 0) {
            return null;
        }

        return new ScopedStringAttributeValue(stringValue.substring(0, offset), stringValue.substring(offset
                + getDelimiter().length()));
    }

    /** {@inheritDoc} */
    @Nonnull protected String getAttributeTypeName() {
        return "Scoped";
    }
}
