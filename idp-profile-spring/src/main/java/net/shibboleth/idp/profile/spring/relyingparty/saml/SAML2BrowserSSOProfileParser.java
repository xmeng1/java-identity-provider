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

package net.shibboleth.idp.profile.spring.relyingparty.saml;

import java.util.List;

import javax.annotation.Nonnull;
import javax.xml.namespace.QName;

import net.shibboleth.idp.saml.authn.principal.AuthnContextClassRefPrincipal;
import net.shibboleth.idp.saml.profile.config.saml2.BrowserSSOProfileConfiguration;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.AttributeSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Parser to generate {@link BrowserSSOProfileConfiguration} from a <code>saml:SAML2SSOProfile</code>.
 */
public class SAML2BrowserSSOProfileParser extends BaseSAML2ProfileConfigurationParser {

    /** Element name. */
    public static final QName ELEMENT_NAME = new QName(RelyingPartySAMLNamespaceHandler.NAMESPACE, "SAML2SSOProfile");

    /** logger. */
    private Logger log = LoggerFactory.getLogger(SAML2BrowserSSOProfileParser.class);

    /** {@inheritDoc} */
    @Override protected Class<? extends BrowserSSOProfileConfiguration> getBeanClass(Element element) {
        return BrowserSSOProfileConfiguration.class;
    }

    /** {@inheritDoc} */
    @Override protected String getArtifactResolutionServiceURLRef() {
        return "shibboleth.SAML2.BrowserSSO.ServiceURL";
    }

    /** {@inheritDoc} */
    @Override protected String getArtifactResolutionServiceIndexRef() {
        return "shibboleth.SAML2.BrowserSSO.ServiceIndex";
    }

    /** {@inheritDoc} */
    @Override protected String getSignResponsesDefault() {
        return "never";
    }

    /** {@inheritDoc} */
    @Override protected String getSignAssertionsDefault() {
        return "always";
    }

    /**
     * We need to extract the defaultAuthenticationMethod and nameIDFormatPrecedence from the parent element, which will
     * be a &lt;RelyingParty&gt;. If there is no parent we set nothing.
     * 
     * @param element The &lt;rp:ProfileConfiguration&gt; element
     * @param builder The builder in which to set the
     */
    private void setPropertiesFromRelyingParty(@Nonnull final Element element,
            @Nonnull final BeanDefinitionBuilder builder) {

        final Node parentNode = element.getParentNode();
        if (parentNode == null) {
            log.warn("no parent to ProfileConfiguration, no defaultAuthenticationMethod set");
            return;
        }
        if (!(parentNode instanceof Element)) {
            log.warn("parent of ProfileConfiguration was unrecognizable, no defaultAuthenticationMethod set");
            return;
        }
        final Element parent = (Element) parentNode;

        if (parent.hasAttributeNS(null, "defaultAuthenticationMethod")) {
            final String method = StringSupport.trimOrNull(parent.getAttributeNS(null, "defaultAuthenticationMethod"));

            if (null == method) {
                return;
            }

            final BeanDefinitionBuilder methodBuilder =
                    BeanDefinitionBuilder.genericBeanDefinition(AuthnContextClassRefPrincipal.class);
            methodBuilder.addConstructorArgValue(method);
            List<BeanDefinition> methodsList = new ManagedList<>(1);

            methodsList.add(methodBuilder.getBeanDefinition());

            builder.addPropertyValue("defaultAuthenticationMethods", methodBuilder.getBeanDefinition());
        }

        if (parent.hasAttributeNS(null, "nameIDFormatPrecedence")) {
            final List<String> nameIDs =
                    AttributeSupport.getAttributeValueAsList(parent.getAttributeNodeNS(null, "nameIDFormatPrecedence"));
            final List<String> managedNameIds = new ManagedList<>(nameIDs.size());
            managedNameIds.addAll(nameIDs);
            builder.addPropertyValue("nameIDFormatPrecedence", managedNameIds);
        }
    }

    /** {@inheritDoc} */
    @Override protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(element, parserContext, builder);

        if (element.hasAttributeNS(null, "localityAddress")) {
            log.warn("Deprecated attribute 'localityAdress' is being ignored");
        }

        if (element.hasAttributeNS(null, "localityDNSName")) {
            log.warn("Deprecated attribute 'localityDNSName' is being ignored");
        }

        if (element.hasAttributeNS(null, "includeAttributeStatement")) {
            builder.addPropertyValue("includeAttributeStatement",
                    element.getAttributeNS(null, "includeAttributeStatement"));
        }

        if (element.hasAttributeNS(null, "maximumSPSessionLifetime")) {
            builder.addPropertyValue("maximumSPSessionLifetime",
                    element.getAttributeNS(null, "maximumSPSessionLifetime"));
        }

        if (element.hasAttributeNS(null, "skipEndpointValidationWhenSigned")) {
            builder.addPropertyValue("skipEndpointValidationWhenSigned",
                    element.getAttributeNS(null, "skipEndpointValidationWhenSigned"));
        }

        setPropertiesFromRelyingParty(element, builder);

        if (element.hasAttributeNS(null, "securityPolicyRef")) {
            //TODO
            log.warn("I do not (yet) know how to deal with 'securityPolicyRef=\"{}\"'",
                    element.getAttributeNS(null, "securityPolicyRef"));
        }

    }

}