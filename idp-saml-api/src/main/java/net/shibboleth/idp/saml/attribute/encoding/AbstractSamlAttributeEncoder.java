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

package net.shibboleth.idp.saml.attribute.encoding;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.AttributeEncoder;
import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponent;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.common.SAMLObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;

/**
 * Base class for encoders that produce SAML attributes.
 * 
 * @param <AttributeType> type of attribute produced
 * @param <EncodedType> the type of data that can be encoded by the encoder
 */
// TODO display name and description
public abstract class AbstractSamlAttributeEncoder<AttributeType extends SAMLObject, EncodedType extends AttributeValue>
        extends AbstractInitializableComponent implements AttributeEncoder<AttributeType>, UnmodifiableComponent {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AbstractSamlAttributeEncoder.class);

    /** The name of the attribute. */
    private String name;

    /** The namespace in which the attribute name is interpreted. */
    private String namespace;

    /**
     * Gets the name of the attribute.
     * 
     * @return name of the attribute, never null after initialization
     */
    @Nullable public final String getName() {
        return name;
    }

    /**
     * Sets the name of the attribute.
     * 
     * @param attributeName name of the attribute
     */
    public final synchronized void setName(@Nullable final String attributeName) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        name = StringSupport.trimOrNull(attributeName);
    }

    /**
     * Gets the namespace in which the attribute name is interpreted.
     * 
     * @return namespace in which the attribute name is interpreted, never null after initialization
     */
    @Nullable public final String getNamespace() {
        return namespace;
    }

    /**
     * Sets the namespace in which the attribute name is interpreted.
     * 
     * @param attributeNamespace namespace in which the attribute name is interpreted
     */
    public final synchronized void setNamespace(@Nullable final String attributeNamespace) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        namespace = StringSupport.trimOrNull(attributeNamespace);
    }

    /**
     * Ensures that the attribute name and namespace are not null.
     * 
     * {@inheritDoc}
     */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (name == null) {
            throw new ComponentInitializationException("Attribute name can not be null or empty");
        }

        if (namespace == null) {
            throw new ComponentInitializationException("Attribute namespace can not be null or empty");
        }
    }

    /** {@inheritDoc} */
    @Nullable public AttributeType encode(@Nonnull final net.shibboleth.idp.attribute.Attribute attribute)
            throws AttributeEncodingException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        Constraint.isNotNull(attribute, "attribute passed in to encode must not be null");
        final String attributeId = attribute.getId();
        log.debug("Beginning to encode attribute {}", attributeId);

        if (attribute.getValues().isEmpty()) {
            log.debug("Unable to encode {} attribute.  It does not contain any values", attributeId);
            return null;
        }

        final List<XMLObject> samlAttributeValues = new ArrayList<XMLObject>();

        EncodedType attributeValue;
        XMLObject samlAttributeValue;
        for (AttributeValue o : attribute.getValues()) {
            if (o == null) {
                // filtered out upstream leave in test for sanity
                log.debug("Skipping null value of attribute {}", attributeId);
                continue;
            }

            if (!canEncodeValue(attribute, o)) {
                log.debug("Skipping value of attribute {}; Type {} can not be encoded by this encoder.", attributeId, o
                        .getClass().getName());
                continue;
            }

            attributeValue = (EncodedType) o;
            samlAttributeValue = encodeValue(attribute, attributeValue);
            if (samlAttributeValue == null) {
                log.debug("Skipping empty value for attribute {}", attributeId);
            } else {
                samlAttributeValues.add(samlAttributeValue);
            }
        }

        if (samlAttributeValues.isEmpty()) {
            log.debug("Attribute {} did not contain any non-empty String values, nothing to encode", attributeId);
            return null;
        }

        log.debug("Completed encoding {} values for attribute {}", samlAttributeValues.size(), attributeId);
        return buildAttribute(attribute, samlAttributeValues);
    }
    
    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (!(obj instanceof AbstractSamlAttributeEncoder)) {
            return false;
        }
        
        AbstractSamlAttributeEncoder other = (AbstractSamlAttributeEncoder) obj;
        
        return Objects.equal(getName(), other.getName()) &&
               Objects.equal(getNamespace(), other.getNamespace()) &&
               Objects.equal(getProtocol(), other.getProtocol());
    }
    
    /** {@inheritDoc} */
    public int hashCode() {
        return Objects.hashCode(getName(), getNamespace(), getProtocol());
    }

    /**
     * Checks if the given value can be handled by the encoder. In many cases this is simply a check to see if the given
     * object is of the right type.
     * 
     * @param attribute the attribute being encoded, never null
     * @param value the value to check, never null
     * 
     * @return true if the encoder can encoder this value, false if not
     */
    protected abstract boolean canEncodeValue(@Nonnull final net.shibboleth.idp.attribute.Attribute attribute,
            @Nonnull final AttributeValue value);

    /**
     * Encodes an attribute value in to a SAML attribute value element.
     * 
     * @param attribute the attribute being encoded, never null
     * @param value the value to encoder, never null
     * 
     * @return the attribute value or null if the resulting attribute value would be empty
     * 
     * @throws AttributeEncodingException thrown if there is a problem encoding the attribute value
     */
    @Nullable protected abstract XMLObject encodeValue(@Nonnull final net.shibboleth.idp.attribute.Attribute attribute,
            @Nonnull final EncodedType value) throws AttributeEncodingException;

    /**
     * Builds a SAML attribute element from the given attribute values.
     * 
     * @param attribute the attribute being encoded, never null
     * @param attributeValues the encoded values for the attribute, never null or containing null elements
     * 
     * @return the SAML attribute element
     * 
     * @throws AttributeEncodingException thrown if there is a problem constructing the SAML attribute
     */
    @Nonnull protected abstract AttributeType buildAttribute(
            @Nonnull final net.shibboleth.idp.attribute.Attribute attribute,
            @Nonnull @NonnullElements final List<XMLObject> attributeValues) throws AttributeEncodingException;
}