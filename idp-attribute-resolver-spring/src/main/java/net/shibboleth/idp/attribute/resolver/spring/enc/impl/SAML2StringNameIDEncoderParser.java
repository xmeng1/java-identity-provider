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

package net.shibboleth.idp.attribute.resolver.spring.enc.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.resolver.spring.impl.AttributeResolverNamespaceHandler;
import net.shibboleth.idp.profile.logic.ScriptedPredicate;
import net.shibboleth.idp.profile.spring.relyingparty.metadata.ScriptTypeBeanParser;
import net.shibboleth.idp.saml.attribute.encoding.impl.SAML2StringNameIDEncoder;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport.ObjectType;
import net.shibboleth.utilities.java.support.xml.DOMTypeSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Spring bean definition parser for {@link SAML2StringNameIDEncoder}.
 * 
 * @deprecated
 */
public class SAML2StringNameIDEncoderParser extends AbstractSingleBeanDefinitionParser {

    /** Schema type- enc: (legacy). */
    @Nonnull public static final QName TYPE_NAME_ENC = new QName(AttributeEncoderNamespaceHandler.NAMESPACE,
            "SAML2StringNameID");

    /** Schema type- resolver:. */
    @Nonnull public static final QName TYPE_NAME_RESOLVER = new QName(AttributeResolverNamespaceHandler.NAMESPACE,
            "SAML2StringNameID");

    /** Local name of name format attribute. */
    @Nonnull @NotEmpty public static final String FORMAT_ATTRIBUTE_NAME = "nameFormat";

    /** Local name of name qualifier attribute. */
    @Nonnull @NotEmpty public static final String NAMEQUALIFIER_ATTRIBUTE_NAME = "nameQualifier";

    /** {@inheritDoc} */
    @Override protected Class<SAML2StringNameIDEncoder> getBeanClass(@Nullable final Element element) {
        return SAML2StringNameIDEncoder.class;
    }

    /** {@inheritDoc} */
    @Override protected void doParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);

        DeprecationSupport.warnOnce(ObjectType.XSITYPE, DOMTypeSupport.getXSIType(config).toString(),
                parserContext.getReaderContext().getResource().getDescription(),
                "via NameID Generation Service configuration");
        
        if (config.hasAttributeNS(null, "activationConditionRef")) {
            builder.addPropertyReference("activationCondition",
                    StringSupport.trimOrNull(config.getAttributeNS(null, "activationConditionRef")));
        } else {
            final Element child = ElementSupport.getFirstChildElement(config);
            if (child != null && ElementSupport.isElementNamed(child,
                    AttributeResolverNamespaceHandler.NAMESPACE, "ActivationConditionScript")) {
                builder.addPropertyValue("activationCondition",
                        ScriptTypeBeanParser.parseScriptType(ScriptedPredicate.class, child).getBeanDefinition());
            }
        }
        
        if (config.hasAttributeNS(null, FORMAT_ATTRIBUTE_NAME)) {
            final String namespace = StringSupport.trimOrNull(config.getAttributeNS(null, FORMAT_ATTRIBUTE_NAME));
            builder.addPropertyValue("nameFormat", namespace);
        }
        builder.setInitMethodName(null);

        builder.addPropertyValue("nameQualifier",
                StringSupport.trimOrNull(config.getAttributeNS(null, NAMEQUALIFIER_ATTRIBUTE_NAME)));
    }

    /** {@inheritDoc} */
    @Override public boolean shouldGenerateId() {
        return true;
    }

}