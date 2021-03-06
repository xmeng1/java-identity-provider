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

package net.shibboleth.idp.attribute.filter.spring.basic.impl;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.xml.namespace.QName;

import net.shibboleth.ext.spring.util.SpringSupport;
import net.shibboleth.idp.attribute.filter.matcher.logic.impl.NotMatcher;
import net.shibboleth.idp.attribute.filter.policyrule.logic.impl.NotPolicyRule;
import net.shibboleth.idp.attribute.filter.spring.BaseFilterParser;
import net.shibboleth.idp.attribute.filter.spring.impl.AbstractWarningFilterParser;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Bean definition parser for {@link NotMatcher} or {@link NotPolicyRule} objects.
 */
public class NotMatcherParser extends AbstractWarningFilterParser {

    /** Schema type. */
    public static final QName SCHEMA_TYPE = new QName(AttributeFilterBasicNamespaceHandler.NAMESPACE, "NOT");

    /** Schema type. */
    public static final QName SCHEMA_TYPE_AFP = new QName(BaseFilterParser.NAMESPACE, "NOT");

    /** {@inheritDoc} */
    @Override protected QName getAFPName() {
        return SCHEMA_TYPE_AFP;
    }

    /** {@inheritDoc} */
    @Override @Nonnull protected Class<?> getBeanClass(@Nonnull final Element element) {
        if (isPolicyRule(element)) {
            return NotPolicyRule.class;
        }
        return NotMatcher.class;
    }

    /** {@inheritDoc} */
    @Override protected void doParse(@Nonnull final Element configElement, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {
        super.doParse(configElement, parserContext, builder);

        final String myId = builder.getBeanDefinition().getAttribute("qualifiedId").toString();

        builder.addPropertyValue("id", myId);

        final List<Element> ruleElementsBasic =
                ElementSupport.getChildElementsByTagNameNS(configElement,
                        AttributeFilterBasicNamespaceHandler.NAMESPACE, "Rule");
        final List<Element> ruleReferenceBasic =
                ElementSupport.getChildElementsByTagNameNS(configElement,
                        AttributeFilterBasicNamespaceHandler.NAMESPACE, "RuleReference");

        final List<Element> ruleElementsAfp =
                ElementSupport.getChildElementsByTagNameNS(configElement, BaseFilterParser.NAMESPACE, "Rule");

        final List<Element> ruleElements = new ArrayList<>(ruleElementsBasic.size() + ruleElementsAfp.size());
        ruleElements.addAll(ruleElementsAfp);
        ruleElements.addAll(ruleElementsBasic);

        if (ruleElements != null && !ruleElements.isEmpty()) {

            builder.addPropertyValue("negation", SpringSupport.parseCustomElements(ruleElements, parserContext).get(0));

        } else if (ruleReferenceBasic != null && !ruleReferenceBasic.isEmpty()) {
            throw new BeanCreationException(parserContext.getReaderContext().getResource().getDescription(), myId,
                    "RuleReference is not supported");
        }
    }
}