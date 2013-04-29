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

package net.shibboleth.idp.attribute.resolver.spring.ad;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.resolver.impl.ad.TemplateAttributeDefinition;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Spring bean definition parser for scripted attribute configuration elements.
 */
public class TemplateAttributeDefinitionBeanDefinitionParser extends BaseAttributeDefinitionBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(AttributeDefinitionNamespaceHandler.NAMESPACE, "Template");

    /** SourceValue element name. */
    public static final QName TEMPLATE_ELEMENT_NAME = new QName(AttributeDefinitionNamespaceHandler.NAMESPACE,
            "Template");

    /** SourceValue element name. */
    public static final QName SOURCE_ATTRIBUTE_ELEMENT_NAME = new QName(AttributeDefinitionNamespaceHandler.NAMESPACE,
            "SourceAttribute");

    /** Class logger. */
    private final Logger log = LoggerFactory
            .getLogger(TemplateAttributeDefinitionBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected Class getBeanClass(Element arg0) {
        return TemplateAttributeDefinition.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element config, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);

        final List<Element> templateElements = ElementSupport.getChildElements(config, TEMPLATE_ELEMENT_NAME);

        if (null != templateElements && templateElements.size() >= 1) {
            final String templateText = StringSupport.trimOrNull(templateElements.get(0).getTextContent());
            log.debug("{} template is '{}'", getLogPrefix(), templateText);

            builder.addPropertyValue("templateText", templateText);
        }

        final List<Element> sourceAttributeElements =
                ElementSupport.getChildElements(config, SOURCE_ATTRIBUTE_ELEMENT_NAME);
        if (null != sourceAttributeElements) {
            final List<String> sourceAttributes = new ArrayList<String>(sourceAttributeElements.size());
            for (Element element : sourceAttributeElements) {
                sourceAttributes.add(StringSupport.trimOrNull(element.getTextContent()));
            }
            log.debug("{} source attributes are '{}'.", getLogPrefix(), sourceAttributes);
            builder.addPropertyValue("sourceAttributes", sourceAttributes);
        }

        String velocityEngineRef = StringSupport.trimOrNull(config.getAttributeNS(null, "velocityEngine"));
        if (null == velocityEngineRef) {
            velocityEngineRef = "shibboleth.VelocityEngine";
        }
        log.debug("{} velocity engine reference '{}'.", getLogPrefix(), velocityEngineRef);
        builder.addPropertyReference("velocityEngine", velocityEngineRef);
    }
}