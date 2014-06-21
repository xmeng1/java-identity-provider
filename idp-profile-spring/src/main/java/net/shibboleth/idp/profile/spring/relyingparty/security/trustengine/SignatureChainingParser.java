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

package net.shibboleth.idp.profile.spring.relyingparty.security.trustengine;

import java.util.List;

import javax.xml.namespace.QName;

import net.shibboleth.ext.spring.util.SpringSupport;
import net.shibboleth.idp.profile.spring.relyingparty.security.SecurityNamespaceHandler;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Parser for trust engines of type SignatureChaining.
 */
public class SignatureChainingParser extends AbstractTrustEngineParser {
    
    /** Schema type. */
    public static final QName SCHEMA_TYPE = new QName(SecurityNamespaceHandler.NAMESPACE, "SignatureChaining");

    /** TrustEngineRef element name. */
    public static final QName TRUST_ENGINE_REF= new QName(SecurityNamespaceHandler.NAMESPACE, "TrustEngineRef");
   
    /** {@inheritDoc} */
    @Override protected Class<?> getBeanClass(Element element) {
        return ChainingSignatureTrustEngineFactory.class;
    }

    /** {@inheritDoc} */
    @Override protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(element, parserContext, builder);

        final List<Element> childEngines =
                ElementSupport.getChildElements(element, SecurityNamespaceHandler.TRUST_ENGINE_ELEMENT_NAME);
        final List<Element> childEngineRefs =
                ElementSupport.getChildElements(element, TRUST_ENGINE_REF);
        
        final List<BeanMetadataElement> allChildren = new ManagedList<>(childEngines.size()+ childEngineRefs.size());
        
        allChildren.addAll(SpringSupport.parseCustomElements(childEngines, parserContext));
        
        for (Element ref:childEngineRefs) {
            allChildren.add(new RuntimeBeanReference(StringSupport.trim(ref.getTextContent())));
        }
        builder.addConstructorArgValue(allChildren);
    }
}
