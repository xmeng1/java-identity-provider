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

import java.util.Collection;
import java.util.Set;

import net.shibboleth.idp.saml.saml2.profile.config.ArtifactResolutionProfileConfiguration;

import org.testng.Assert;
import org.testng.annotations.Test;

public class SAML2ArtifactTest extends BaseSAMLProfileTest {

    @Test public void defaults() {

        ArtifactResolutionProfileConfiguration profile =
                getBean(ArtifactResolutionProfileConfiguration.class, true, "beans.xml", "saml/saml2artifact.xml");

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
        Assert.assertEquals(profile.getInboundSubflowId(), "SecurityPolicy.SAML2ArtifactQuery");
        Assert.assertNull(profile.getOutboundSubflowId());
    }

    @Test public void values() {
        ArtifactResolutionProfileConfiguration profile =
                getBean(ArtifactResolutionProfileConfiguration.class, false, "beans.xml", "saml/saml2artifactValues.xml");

        assertFalsePredicate(profile.getEncryptAssertionsPredicate());
        assertTruePredicate(profile.getEncryptNameIDsPredicate());

        Assert.assertEquals(profile.getProxyCount(), 99);
        final Collection<String> proxyAudiences = profile.getProxyAudiences();
        Assert.assertEquals(proxyAudiences.size(), 2);
        Assert.assertTrue(proxyAudiences.contains("ProxyAudience1"));
        Assert.assertTrue(proxyAudiences.contains("ProxyAudience2"));

        assertFalsePredicate(profile.getSignRequestsPredicate());
        assertFalsePredicate(profile.getSignAssertionsPredicate());
        assertConditionalPredicate(profile.getSignResponsesPredicate());
        
        Assert.assertEquals(profile.getInboundSubflowId(), "artifact2ibfid");
        Assert.assertEquals(profile.getOutboundSubflowId(), "artifact2obfid");


        Assert.assertEquals(profile.getAssertionLifetime(), 10 * 60 * 1000);

        final Set<String> audiences = profile.getAdditionalAudiencesForAssertion();
        Assert.assertEquals(audiences.size(), 1);
        Assert.assertEquals(audiences.iterator().next(), "NibbleAHappyWarthog");

        Assert.assertFalse(profile.includeConditionsNotBefore());
    }

}
