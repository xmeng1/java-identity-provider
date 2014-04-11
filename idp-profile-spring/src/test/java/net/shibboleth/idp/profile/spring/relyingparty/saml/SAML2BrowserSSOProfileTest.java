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

import net.shibboleth.idp.saml.authn.principal.AuthnContextClassRefPrincipal;
import net.shibboleth.idp.saml.profile.config.SAMLArtifactConfiguration;
import net.shibboleth.idp.saml.profile.config.saml2.BrowserSSOProfileConfiguration;

import org.testng.Assert;
import org.testng.annotations.Test;

public class SAML2BrowserSSOProfileTest extends BaseSAMLProfileTest {

    @Test public void defaults() {

        BrowserSSOProfileConfiguration profile = getBean(BrowserSSOProfileConfiguration.class, true, "saml/saml2SSO.xml");

        Assert.assertTrue(profile.includeAttributeStatement());
        Assert.assertFalse(profile.skipEndpointValidationWhenSigned());
        Assert.assertEquals(profile.getMaximumSPSessionLifetime(), 0);

        // defaults for AbstractSAML2ProfileConfiguration

        assertConditionalPredicate(profile.getEncryptAssertionsPredicate());
        assertFalsePredicate(profile.getEncryptNameIDsPredicate());

        Assert.assertEquals(profile.getProxyCount(), 0);
        Assert.assertTrue(profile.getProxyAudiences().isEmpty());

        // defaults for AbstractSAMLProfileConfiguration
        assertConditionalPredicate(profile.getSignRequestsPredicate());
        assertTruePredicate(profile.getSignAssertionsPredicate());
        assertFalsePredicate(profile.getSignResponsesPredicate());
        Assert.assertEquals(profile.getAssertionLifetime(), 5 * 60 * 1000);
        Assert.assertTrue(profile.getAdditionalAudiencesForAssertion().isEmpty());
        Assert.assertTrue(profile.includeConditionsNotBefore());
        Assert.assertNull(profile.getArtifactConfiguration());
    }

    @Test public void values() {
        BrowserSSOProfileConfiguration profile =
                getBean(BrowserSSOProfileConfiguration.class, true, "beans.xml", "saml/saml2SSOValues.xml");

        Assert.assertFalse(profile.includeAttributeStatement());
        Assert.assertTrue(profile.skipEndpointValidationWhenSigned());
        Assert.assertEquals(profile.getMaximumSPSessionLifetime(), 1);

        assertConditionalPredicate(profile.getEncryptAssertionsPredicate());
        assertFalsePredicate(profile.getEncryptNameIDsPredicate());
        Assert.assertTrue(profile.isEncryptionOptional());

        Assert.assertEquals(profile.getProxyCount(), 0);
        Assert.assertTrue(profile.getProxyAudiences().isEmpty());

        // defaults for AbstractSAMLProfileConfiguration
        assertConditionalPredicate(profile.getSignRequestsPredicate());
        assertTruePredicate(profile.getSignAssertionsPredicate());
        assertFalsePredicate(profile.getSignResponsesPredicate());
        Assert.assertEquals(profile.getAssertionLifetime(), 5 * 60 * 1000);
        Assert.assertTrue(profile.getAdditionalAudiencesForAssertion().isEmpty());
        Assert.assertTrue(profile.includeConditionsNotBefore());

        final SAMLArtifactConfiguration artifact = profile.getArtifactConfiguration();
        Assert.assertEquals(artifact.getArtifactResolutionServiceURL(), "https://idp.example.org/SSO");
        Assert.assertEquals(artifact.getArtifactType().intValue(), 76543);
        Assert.assertEquals(artifact.getArtifactResolutionServiceIndex().intValue(), 1111);

        Assert.assertEquals(profile.getDefaultAuthenticationMethods().size(), 1);
        final AuthnContextClassRefPrincipal authnMethod =
                (AuthnContextClassRefPrincipal) profile.getDefaultAuthenticationMethods().get(0);
        Assert.assertEquals(authnMethod.getAuthnContextClassRef().getAuthnContextClassRef(),
                "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport");

        final List<String> nameIDPrefs = profile.getNameIDFormatPrecedence();

        Assert.assertEquals(nameIDPrefs.size(), 2);
        Assert.assertTrue(nameIDPrefs.contains("one"));
        Assert.assertTrue(nameIDPrefs.contains("two"));

    }
}