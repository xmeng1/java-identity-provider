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

package net.shibboleth.idp.attribute.filter.spring;

import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.security.IdentifierGenerationStrategy;
import net.shibboleth.utilities.java.support.security.RandomIdentifierGenerationStrategy;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import com.google.common.base.Strings;

// TODO incomplete v2 port
/**
 * Base class for Spring bean definition parsers within the filter engine configuration. This base class is responsible
 * for generating an ID for the Spring bean that is unique within all the policy components loaded.
 */
public abstract class BaseFilterParser extends AbstractSingleBeanDefinitionParser {

    /** Generator of unique IDs. */
    // TODO correct random identifier ?
    private static IdentifierGenerationStrategy idGen = new RandomIdentifierGenerationStrategy();

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(BaseFilterParser.class);

    /**
     * Generates an ID for a filter engine component. If the given localId is null a random one will be generated.
     * 
     * @param configElement component configuration element
     * @param componentNamespace namespace for the component
     * @param localId local id or null
     * 
     * @return unique ID for the component
     */
    protected String getQualifiedId(Element configElement, String componentNamespace, String localId) {
        Element afpgElement = configElement.getOwnerDocument().getDocumentElement();
        String policyGroupId = StringSupport.trimOrNull(afpgElement.getAttributeNS(null, "id"));

        StringBuilder qualifiedId = new StringBuilder();
        qualifiedId.append("/");
        qualifiedId.append(AttributeFilterPolicyGroupParser.ELEMENT_NAME.getLocalPart());
        qualifiedId.append(":");
        qualifiedId.append(policyGroupId);
        if (!Strings.isNullOrEmpty(componentNamespace)) {
            qualifiedId.append("/");
            qualifiedId.append(componentNamespace);
            qualifiedId.append(":");

            if (Strings.isNullOrEmpty(localId)) {
                qualifiedId.append(idGen.generateIdentifier());
            } else {
                qualifiedId.append(localId);
            }
        }

        return qualifiedId.toString();
    }

    /**
     * Gets the reference text from an element.
     * 
     * @param element the element to look at.
     * @return the text.
     * 
     * <br/>
     *         TODO The V2 implementation used the text context but the schema suggest using the attribute "ref". This
     *         does both (for now)
     * 
     * 
     */
    @Nullable protected String getReferenceText(Element element) {
        String reference = StringSupport.trimOrNull(element.getAttributeNS(null, "ref"));

        if (null == reference) {
            reference = StringSupport.trimOrNull(element.getTextContent());
        }

        return reference;
    }

    /**
     * Gets the absolute reference given a possibly relative reference.
     * 
     * @param configElement component configuration element
     * @param componentNamespace namespace for the component
     * @param reference Reference to convert into an absolute
     * 
     * @return absolute form of the reference
     */
    protected String getAbsoluteReference(Element configElement, String componentNamespace, String reference) {
        if (reference.startsWith("/")) {
            return reference;
        } else {
            return getQualifiedId(configElement, componentNamespace, reference);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * Calculate the qualified id once, and set both the id property as well as a qualified id metadata attribute used
     * by the resolveId() method.
     */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(element, parserContext, builder);

        final String suppliedId = StringSupport.trimOrNull(element.getAttributeNS(null, "id"));
        final String generatedId = getQualifiedId(element, element.getLocalName(), suppliedId);

        if (suppliedId == null) {
            log.warn("Element '{}' did not contain an 'id' attribute.  Generated id '{}' will be used",
                    element.getLocalName(), generatedId);

        } else {
            log.debug("Element '{}' id attribute {} is mapped to '{}'", element.getLocalName(), suppliedId, 
                    generatedId);
        }

        builder.getBeanDefinition().setAttribute("qualifiedId", generatedId);
    }

    /** {@inheritDoc} */
    protected String
            resolveId(Element configElement, AbstractBeanDefinition beanDefinition, ParserContext parserContext) {
        return beanDefinition.getAttribute("qualifiedId").toString();
    }

    /**
     * Is this inside a &lt;PolicyRequirementRule&gt; or an permit or deny rule?.
     * @param element the element under question
     * @return true if it is inside a policy requirement rule, false otherwise.
     */
    protected boolean isPolicyRule(final Element element) {

        Element elem = element;
        do {
            if (ElementSupport.isElementNamed(element, AttributeFilterPolicyParser.POLICY_REQUIREMENT_RULE)) {
                return true;
            } else if (ElementSupport.isElementNamed(element, AttributeRuleParser.DENY_VALUE_RULE) ||
                       ElementSupport.isElementNamed(element, AttributeRuleParser.PERMIT_VALUE_RULE)) {
                return false;
            }
            elem = ElementSupport.getElementAncestor(elem);
        } while (elem != null);
        log.warn("Element '{}' : could not find schema defined parent");
        return false;
    }
}