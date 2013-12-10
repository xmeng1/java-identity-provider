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

package net.shibboleth.idp.profile.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.AttributeContext;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.filter.AttributeFilter;
import net.shibboleth.idp.attribute.filter.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.AttributeFilterImpl;
import net.shibboleth.idp.attribute.filter.AttributeFilterPolicy;
import net.shibboleth.idp.attribute.filter.AttributeRule;
import net.shibboleth.idp.attribute.filter.MockMatcher;
import net.shibboleth.idp.attribute.filter.PolicyRequirementRule;
import net.shibboleth.idp.profile.EventIds;
import net.shibboleth.idp.profile.RequestContextBuilder;
import net.shibboleth.idp.relyingparty.RelyingPartyContext;
import net.shibboleth.idp.service.AbstractReloadableService;
import net.shibboleth.idp.service.ServiceableComponent;

import org.opensaml.profile.action.ActionTestingSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

/** {@link FilterAttributes} unit test. */
public class FilterAttributesTest {

    /** Test that the action errors out properly if there is no relying party context. */
    @Test public void testNoRelyingPartyContext() throws Exception {
        final ProfileRequestContext profileCtx = new ProfileRequestContext();

        final AttributeFilterImpl engine = new AttributeFilterImpl("test", null);
        engine.initialize();

        final FilterAttributes action = new FilterAttributes(new FilterService(engine));
        action.setId("test");
        action.initialize();

        action.execute(profileCtx);
        ActionTestingSupport.assertEvent(profileCtx, EventIds.INVALID_RELYING_PARTY_CTX);
    }

    /** Test that the action errors out properly if there is no attribute context. */
    @Test public void testNoAttributeContext() throws Exception {
        final ProfileRequestContext profileCtx = new RequestContextBuilder().buildProfileRequestContext();

        final AttributeFilterImpl engine = new AttributeFilterImpl("test", null);
        engine.initialize();

        final FilterAttributes action = new FilterAttributes(new FilterService(engine));
        action.setId("test");
        action.initialize();

        action.execute(profileCtx);
        ActionTestingSupport.assertEvent(profileCtx, EventIds.INVALID_ATTRIBUTE_CTX);
    }

    /** Test that the action proceeds properly if there are no attributes to filter . */
    @Test public void testNoAttributes() throws Exception {
        final ProfileRequestContext profileCtx = new RequestContextBuilder().buildProfileRequestContext();

        final AttributeContext attribCtx = new AttributeContext();
        profileCtx.getSubcontext(RelyingPartyContext.class).addSubcontext(attribCtx);

        final AttributeFilterImpl engine = new AttributeFilterImpl("test", null);
        engine.initialize();
        
        final FilterAttributes action = new FilterAttributes(new FilterService(engine));
        action.setId("test");
        action.initialize();

        action.execute(profileCtx);
        ActionTestingSupport.assertProceedEvent(profileCtx);
    }

    /** Test that the action filters attributes and proceeds properly while auto-creating a filter context. */
    @Test public void testFilterAttributesAutoCreateFilterContext() throws Exception {
        final IdPAttribute attribute1 = new IdPAttribute("attribute1");
        attribute1.setValues(Lists.newArrayList(new StringAttributeValue("one"), new StringAttributeValue("two")));

        final IdPAttribute attribute2 = new IdPAttribute("attribute2");
        attribute2.setValues(Lists.newArrayList(new StringAttributeValue("a"), new StringAttributeValue("b")));

        final List<IdPAttribute> attributes = Arrays.asList(attribute1, attribute2);

        final MockMatcher attribute1Matcher = new MockMatcher();
        attribute1Matcher.setMatchingAttribute("attribute1");
        attribute1Matcher.setMatchingValues(null);

        final AttributeRule attribute1Policy = new AttributeRule();
        attribute1Policy.setId("attribute1Policy");
        attribute1Policy.setAttributeId("attribute1");
        attribute1Policy.setMatcher(attribute1Matcher);
        attribute1Policy.setIsDenyRule(false);

        final AttributeFilterPolicy policy =
                new AttributeFilterPolicy("attribute1Policy", PolicyRequirementRule.MATCHES_ALL,
                        Lists.newArrayList(attribute1Policy));

        final AttributeFilterImpl engine = new AttributeFilterImpl("engine", Lists.newArrayList(policy));
        engine.initialize();

        final ProfileRequestContext profileCtx = new RequestContextBuilder().buildProfileRequestContext();

        final AttributeContext attributeCtx = new AttributeContext();
        attributeCtx.setIdPAttributes(attributes);
        profileCtx.getSubcontext(RelyingPartyContext.class).addSubcontext(attributeCtx);

        final FilterAttributes action = new FilterAttributes(new FilterService(engine));
        action.setId("test");
        action.initialize();

        action.execute(profileCtx);
        ActionTestingSupport.assertProceedEvent(profileCtx);

        // The attribute filter context should be removed by the filter attributes action.
        Assert.assertNull(profileCtx.getSubcontext(RelyingPartyContext.class).getSubcontext(
                AttributeFilterContext.class));

        final AttributeContext resultAttributeCtx =
                profileCtx.getSubcontext(RelyingPartyContext.class).getSubcontext(AttributeContext.class);
        Assert.assertNotNull(resultAttributeCtx);

        final Map<String, IdPAttribute> resultAttributes = resultAttributeCtx.getIdPAttributes();
        Assert.assertEquals(resultAttributes.size(), 1);

        final Set<IdPAttributeValue<?>> resultAttributeValue = resultAttributes.get("attribute1").getValues();
        Assert.assertEquals(resultAttributeValue.size(), 2);
        Assert.assertTrue(resultAttributeValue.contains(new StringAttributeValue("one")));
        Assert.assertTrue(resultAttributeValue.contains(new StringAttributeValue("two")));
    }

    /** Test that the action filters attributes and proceeds properly with an existing filter context. */
    @Test public void testFilterAttributesExistingFilterContext() throws Exception {
        final IdPAttribute attribute1 = new IdPAttribute("attribute1");
        attribute1.setValues(Lists.newArrayList(new StringAttributeValue("one"), new StringAttributeValue("two")));

        final IdPAttribute attribute2 = new IdPAttribute("attribute2");
        attribute2.setValues(Lists.newArrayList(new StringAttributeValue("a"), new StringAttributeValue("b")));

        final List<IdPAttribute> attributes = Arrays.asList(attribute1, attribute2);

        final MockMatcher attribute1Matcher = new MockMatcher();
        attribute1Matcher.setMatchingAttribute("attribute1");
        attribute1Matcher.setMatchingValues(null);

        final AttributeRule attribute1Policy = new AttributeRule();
        attribute1Policy.setId("attribute1Policy");
        attribute1Policy.setAttributeId("attribute1");
        attribute1Policy.setMatcher(attribute1Matcher);
        attribute1Policy.setIsDenyRule(false);

        final AttributeFilterPolicy policy =
                new AttributeFilterPolicy("attribute1Policy", PolicyRequirementRule.MATCHES_ALL,
                        Lists.newArrayList(attribute1Policy));

        final AttributeFilterImpl engine = new AttributeFilterImpl("engine", Lists.newArrayList(policy));
        engine.initialize();

        final ProfileRequestContext profileCtx = new RequestContextBuilder().buildProfileRequestContext();

        final AttributeContext attributeCtx = new AttributeContext();
        attributeCtx.setIdPAttributes(attributes);
        profileCtx.getSubcontext(RelyingPartyContext.class).addSubcontext(attributeCtx);

        final AttributeFilterContext attributeFilterCtx = new AttributeFilterContext();
        profileCtx.getSubcontext(RelyingPartyContext.class).addSubcontext(attributeFilterCtx);

        final FilterAttributes action = new FilterAttributes(new FilterService(engine));
        action.setId("test");
        action.initialize();

        action.execute(profileCtx);
        ActionTestingSupport.assertProceedEvent(profileCtx);

        // The attribute filter context should be removed by the filter attributes action.
        Assert.assertNull(profileCtx.getSubcontext(RelyingPartyContext.class).getSubcontext(
                AttributeFilterContext.class));

        final AttributeContext resultAttributeCtx =
                profileCtx.getSubcontext(RelyingPartyContext.class).getSubcontext(AttributeContext.class);
        Assert.assertNotNull(resultAttributeCtx);

        final Map<String, IdPAttribute> resultAttributes = resultAttributeCtx.getIdPAttributes();
        Assert.assertEquals(resultAttributes.size(), 1);

        final Set<IdPAttributeValue<?>> resultAttributeValue = resultAttributes.get("attribute1").getValues();
        Assert.assertEquals(resultAttributeValue.size(), 2);
        Assert.assertTrue(resultAttributeValue.contains(new StringAttributeValue("one")));
        Assert.assertTrue(resultAttributeValue.contains(new StringAttributeValue("two")));
    }

    /** Test that action returns the proper event if the attributes are not able to be filtered. */
    @Test public void testUnableToFilterAttributes() throws Exception {
        final IdPAttribute attribute1 = new MockUncloneableAttribute("attribute1");
        attribute1.setValues(Lists.newArrayList(new StringAttributeValue("one"), new StringAttributeValue("two")));

        final List<IdPAttribute> attributes = Arrays.asList(attribute1);

        final MockMatcher attribute1Matcher = new MockMatcher();
        attribute1Matcher.setMatchingAttribute("attribute1");
        attribute1Matcher.setMatchingValues(null);

        final AttributeRule attribute1Policy = new AttributeRule();
        attribute1Policy.setId("attribute1Policy");
        attribute1Policy.setAttributeId("attribute1");
        attribute1Policy.setMatcher(attribute1Matcher);
        attribute1Policy.setIsDenyRule(false);

        final AttributeFilterPolicy policy =
                new AttributeFilterPolicy("attribute1Policy", PolicyRequirementRule.MATCHES_ALL,
                        Lists.newArrayList(attribute1Policy));

        final AttributeFilterImpl engine = new AttributeFilterImpl("engine", Lists.newArrayList(policy));
        engine.initialize();

        final ProfileRequestContext profileCtx = new RequestContextBuilder().buildProfileRequestContext();

        final AttributeContext attributeCtx = new AttributeContext();
        attributeCtx.setIdPAttributes(attributes);
        profileCtx.getSubcontext(RelyingPartyContext.class).addSubcontext(attributeCtx);

        final AttributeFilterContext attributeFilterCtx = new AttributeFilterContext();
        profileCtx.getSubcontext(RelyingPartyContext.class).addSubcontext(attributeFilterCtx);

        final FilterAttributes action = new FilterAttributes(new FilterService(engine));
        action.setId("test");
        action.initialize();

        action.execute(profileCtx);
        ActionTestingSupport.assertEvent(profileCtx, EventIds.UNABLE_FILTER_ATTRIBS);
    }
    
    /** Test that action returns the proper event if the attribute configuration is broken */
    @Test public void testUnableToFindFilter() throws Exception {
        final ProfileRequestContext profileCtx = new RequestContextBuilder().buildProfileRequestContext();

        final IdPAttribute attribute1 = new MockUncloneableAttribute("attribute1");
        attribute1.setValues(Lists.newArrayList(new StringAttributeValue("one"), new StringAttributeValue("two")));

        final List<IdPAttribute> attributes = Arrays.asList(attribute1);


        final AttributeContext attributeCtx = new AttributeContext();
        attributeCtx.setIdPAttributes(attributes);
        profileCtx.getSubcontext(RelyingPartyContext.class).addSubcontext(attributeCtx);

        final AttributeFilterContext attributeFilterCtx = new AttributeFilterContext();
        profileCtx.getSubcontext(RelyingPartyContext.class).addSubcontext(attributeFilterCtx);

        final FilterAttributes action = new FilterAttributes(new FilterService(null));
        action.setId("test");
        action.initialize();

        action.execute(profileCtx);
        ActionTestingSupport.assertEvent(profileCtx, EventIds.UNABLE_FILTER_ATTRIBS);
    }


    /** {@link IdPAttribute} which always throws a {@link CloneNotSupportedException}. */
    private class MockUncloneableAttribute extends IdPAttribute {

        /**
         * Constructor.
         * 
         * @param attributeId
         */
        public MockUncloneableAttribute(String attributeId) {
            super(attributeId);
        }

        /** Always throws exception. */
        public IdPAttribute clone() throws CloneNotSupportedException {
            throw new CloneNotSupportedException();
        }
    }
    

    private static class FilterService extends AbstractReloadableService<AttributeFilter> {

        private ServiceableComponent<AttributeFilter> component;

        protected FilterService(ServiceableComponent<AttributeFilter> what) {
            component = what;
        }

        /** {@inheritDoc} */
        @Nullable public ServiceableComponent<AttributeFilter> getServiceableComponent() {
            if (null == component) {
                return null;
            }
            component.pinComponent();
            return component;
        }

        /** {@inheritDoc} */
        protected boolean shouldReload() {
            return false;
        }
    }

}
