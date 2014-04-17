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

package net.shibboleth.idp.attribute.resolver.ad.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.script.ScriptException;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.XMLObjectAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeDefinition;
import net.shibboleth.idp.attribute.resolver.DataConnector;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.ResolverPluginDependency;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.dc.impl.SAMLAttributeDataConnector;
import net.shibboleth.idp.attribute.resolver.impl.AttributeResolverImpl;
import net.shibboleth.idp.saml.impl.TestSources;
import net.shibboleth.utilities.java.support.collection.LazySet;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.scripting.EvaluableScript;

import org.apache.commons.codec.digest.DigestUtils;
import org.opensaml.core.xml.XMLObjectBaseTestCase;
import org.opensaml.saml.ext.saml2mdattr.EntityAttributes;
import org.opensaml.saml.saml2.core.Attribute;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.base.Function;

/** test for {@link net.shibboleth.idp.attribute.resolver.ad.impl.ScriptedIdPAttribute}. */
public class ScriptedAttributeTest extends XMLObjectBaseTestCase {

    /** The name. */
    private static final String TEST_ATTRIBUTE_NAME = "Scripted";

    /** The language */
    private static final String SCRIPT_LANGUAGE = "JavaScript";

    /** Simple result. */
    private static final String SIMPLE_VALUE = "simple";

    private String fileNameToPath(String fileName) {
        return "/data/net/shibboleth/idp/attribute/resolver/impl/ad/" + fileName;
    }

    private String getScript(String fileName) throws IOException {
        return StringSupport.inputStreamToString(getClass().getResourceAsStream(fileNameToPath(fileName)), null);
    }
    
    private boolean isV8() {
        final String ver = System.getProperty("java.version");
        return ver.startsWith("1.8");
    }

    /**
     * Test resolution of an simple script (statically generated data).
     * 
     * @throws ResolutionException
     * @throws ComponentInitializationException only if the test will fail
     * @throws ScriptException
     * @throws IOException
     */
    @Test public void simple() throws ResolutionException, ComponentInitializationException, ScriptException,
            IOException {

        if (isV8()) {
            return;
        }
        
        final IdPAttribute test = new IdPAttribute(TEST_ATTRIBUTE_NAME);

        test.setValues(Collections.singleton(new StringAttributeValue(SIMPLE_VALUE)));

        final ScriptedAttributeDefinition attr = new ScriptedAttributeDefinition();
        Assert.assertNull(attr.getScript());
        attr.setId(TEST_ATTRIBUTE_NAME);
        attr.setScript(new EvaluableScript(SCRIPT_LANGUAGE, getScript("simple.script")));
        attr.initialize();
        Assert.assertNotNull(attr.getScript());

        final IdPAttribute val = attr.resolve(generateContext());
        final Set<IdPAttributeValue<?>> results = val.getValues();

        Assert.assertTrue(test.equals(val), "Scripted result is the same as bases");
        Assert.assertEquals(results.size(), 1, "Scripted result value count");
        Assert.assertEquals(results.iterator().next().getValue(), SIMPLE_VALUE, "Scripted result contains known value");
    }

    /**
     * Test resolution of an simple script (statically generated data).
     * 
     * @throws ResolutionException
     * @throws ComponentInitializationException only if the test will fail
     * @throws ScriptException
     * @throws IOException
     */
    @Test public void simple2() throws ResolutionException, ComponentInitializationException, ScriptException,
            IOException {

        if (isV8()) {
            return;
        }
        final IdPAttribute test = new IdPAttribute(TEST_ATTRIBUTE_NAME);

        test.setValues(Collections.singleton(new StringAttributeValue(SIMPLE_VALUE)));

        final ScriptedAttributeDefinition attr = new ScriptedAttributeDefinition();
        Assert.assertNull(attr.getScript());
        attr.setId(TEST_ATTRIBUTE_NAME);
        attr.setScript(new EvaluableScript(SCRIPT_LANGUAGE, getScript("simple2.script")));
        attr.initialize();
        Assert.assertNotNull(attr.getScript());

        final IdPAttribute val = attr.resolve(generateContext());
        final Set<IdPAttributeValue<?>> results = val.getValues();

        Assert.assertTrue(test.equals(val), "Scripted result is the same as bases");
        Assert.assertEquals(results.size(), 1, "Scripted result value count");
        Assert.assertEquals(results.iterator().next().getValue(), SIMPLE_VALUE, "Scripted result contains known value");
    }

    @Test public void simpleWithPredef() throws ResolutionException, ComponentInitializationException,
            ScriptException, IOException {

        if (isV8()) {
            return;
        }

        final IdPAttribute test = new IdPAttribute(TEST_ATTRIBUTE_NAME);
        final IdPAttributeValue<?> attributeValue = new StringAttributeValue(SIMPLE_VALUE);

        test.setValues(Collections.singleton(attributeValue));

        final ScriptedAttributeDefinition attr = new ScriptedAttributeDefinition();
        Assert.assertNull(attr.getScript());
        attr.setId(TEST_ATTRIBUTE_NAME);
        attr.setScript(new EvaluableScript(SCRIPT_LANGUAGE, getScript("simpleWithPredef.script")));
        attr.initialize();
        Assert.assertNotNull(attr.getScript());

        final IdPAttribute val = attr.resolve(generateContext());
        final Set<IdPAttributeValue<?>> results = val.getValues();

        Assert.assertTrue(test.equals(val), "Scripted result is the same as bases");
        Assert.assertEquals(results.size(), 1, "Scripted result value count");
        Assert.assertEquals(results.iterator().next(), attributeValue, "Scripted result contains known value");
    }

    private ScriptedAttributeDefinition buildTest(String failingScript) throws ScriptException, IOException,
            ComponentInitializationException {

        final ScriptedAttributeDefinition attr = new ScriptedAttributeDefinition();
        attr.setId(TEST_ATTRIBUTE_NAME);
        try {
            attr.initialize();
            Assert.fail("No script defined");
        } catch (ComponentInitializationException ex) {
            // OK
        }

        attr.setScript(new EvaluableScript(SCRIPT_LANGUAGE, getScript(failingScript)));
        attr.initialize();

        return attr;
    }

    private void failureTest(String failingScript, String failingMessage) throws ScriptException, IOException,
            ComponentInitializationException {
        try {
            buildTest(failingScript).resolve(generateContext());
            Assert.fail("Script: '" + failingScript + "' should have thrown an exception: " + failingMessage);
        } catch (ResolutionException ex) {
            // OK
        }
    }

    @Test public void fails() throws ResolutionException, ComponentInitializationException, ScriptException,
            IOException {

        if (isV8()) {
            return;
        }
        failureTest("fail1.script", "Unknown method");
        failureTest("fail2.script", "Bad output type");
        Assert.assertNull(buildTest("fail3.script").resolve(generateContext()),
                "returns nothing");
        failureTest("fail4.script", "getValues, then getNativeAttributes");
        failureTest("fail5.script", "getNativeAttributes, then getValues");

        failureTest("fail6.script", "bad type added");
        failureTest("fail7.script", "null added ");
    }

    @Test public void addAfterGetValues() throws ResolutionException, ScriptException, IOException,
            ComponentInitializationException {

        final IdPAttribute result =
                buildTest("addAfterGetValues.script").resolve(generateContext());
        final Set<IdPAttributeValue<?>> values = result.getValues();
        Assert.assertEquals(values.size(), 1);
        Assert.assertTrue(values.contains(new StringAttributeValue("newValue")));
    }

    /**
     * Test resolution of an script which looks at the provided attributes.
     * 
     * @throws ResolutionException if the resolve fails
     * @throws ComponentInitializationException only if things go wrong
     * @throws ScriptException
     * @throws IOException
     */
    @Test public void attributes() throws ResolutionException, ComponentInitializationException,
            ScriptException, IOException {

        if (isV8()) {
            return;
        }
        // Set the dependency on the data connector
        final Set<ResolverPluginDependency> ds = new LazySet<ResolverPluginDependency>();
        ds.add(TestSources.makeResolverPluginDependency(TestSources.STATIC_ATTRIBUTE_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR));
        final ScriptedAttributeDefinition scripted = new ScriptedAttributeDefinition();
        scripted.setId(TEST_ATTRIBUTE_NAME);
        scripted.setScript(new EvaluableScript(SCRIPT_LANGUAGE, getScript("attributes.script")));
        scripted.setDependencies(ds);
        scripted.initialize();

        // And resolve
        final Set<AttributeDefinition> attrDefinitions = new LazySet<AttributeDefinition>();
        attrDefinitions.add(scripted);
        attrDefinitions.add(TestSources.populatedStaticAttribute());

        final Set<DataConnector> dataDefinitions = new LazySet<DataConnector>();
        dataDefinitions.add(TestSources.populatedStaticConnector());

        final AttributeResolverImpl resolver = new AttributeResolverImpl("foo", attrDefinitions, dataDefinitions, null);
        resolver.initialize();

        final AttributeResolutionContext context = generateContext();
        resolver.resolveAttributes(context);
        final IdPAttribute attribute = context.getResolvedIdPAttributes().get(TEST_ATTRIBUTE_NAME);
        final Set<IdPAttributeValue<?>> values = attribute.getValues();

        Assert.assertEquals(values.size(), 2);
        Assert.assertTrue(values.contains(TestSources.COMMON_ATTRIBUTE_VALUE_RESULT));
        Assert.assertTrue(values.contains(TestSources.ATTRIBUTE_ATTRIBUTE_VALUE_RESULT));

    }

    @Test public void nonString() throws ResolutionException, ComponentInitializationException,
            ScriptException, IOException {

        if (isV8()) {
            return;
        }
        // Set the dependency on the data connector
        final Set<ResolverPluginDependency> ds = new LazySet<>();
        ds.add(TestSources.makeResolverPluginDependency(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR, null));
        
        final ScriptedAttributeDefinition scripted = new ScriptedAttributeDefinition();
        scripted.setId(TEST_ATTRIBUTE_NAME);
        scripted.setScript(new EvaluableScript(SCRIPT_LANGUAGE, getScript("attributes2.script")));
        scripted.setDependencies(ds);
        scripted.initialize();

        // And resolve
        final Set<AttributeDefinition> attrDefinitions = new HashSet<>(3);
        attrDefinitions.add(scripted);
        AttributeDefinition nonString = TestSources.nonStringAttributeDefiniton(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR); 
        attrDefinitions.add(nonString);
        attrDefinitions.add(TestSources.populatedStaticAttribute());

        final AttributeResolverImpl resolver = new AttributeResolverImpl("foo", attrDefinitions, null, null);
        resolver.initialize();

        final AttributeResolutionContext context = generateContext();
        resolver.resolveAttributes(context);
        final IdPAttribute attribute = context.getResolvedIdPAttributes().get(TEST_ATTRIBUTE_NAME);
        final Set<IdPAttributeValue<?>> values = attribute.getValues();

        Assert.assertEquals(values.size(), 2);
        for (IdPAttributeValue value: values) {
            if (!(value instanceof XMLObjectAttributeValue)) {
                Assert.fail("Wrong type: " + value.getClass().getName());
            }
        }
    }

    /**
     * Test resolution of an script which looks at the provided request context.
     * 
     * @throws ResolutionException if the resolve fails
     * @throws ComponentInitializationException only if the test has gone wrong
     * @throws ScriptException
     * @throws IOException
     */
    @Test public void context() throws ResolutionException, ComponentInitializationException, ScriptException,
            IOException {

        if (isV8()) {
            return;
        }
        // Set the dependency on the data connector
        final Set<ResolverPluginDependency> ds = new LazySet<>();
        ds.add(TestSources.makeResolverPluginDependency(TestSources.STATIC_CONNECTOR_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME_CONNECTOR));

        final ScriptedAttributeDefinition scripted = new ScriptedAttributeDefinition();
        scripted.setId(TEST_ATTRIBUTE_NAME);
        scripted.setScript(new EvaluableScript(SCRIPT_LANGUAGE, getScript("context.script")));
        scripted.setDependencies(ds);
        scripted.initialize();

        // And resolve
        final Set<AttributeDefinition> attrDefinitions = new LazySet<>();
        attrDefinitions.add(scripted);
        attrDefinitions.add(TestSources.populatedStaticAttribute());

        final Set<DataConnector> dataDefinitions = new LazySet<>();
        dataDefinitions.add(TestSources.populatedStaticConnector());

        final AttributeResolverImpl resolver = new AttributeResolverImpl("foo", attrDefinitions, dataDefinitions, null);
        resolver.initialize();

        TestContextContainer parent = new TestContextContainer();
        final AttributeResolutionContext context = generateContext();
        parent.addSubcontext(context);

        try {
            resolver.resolveAttributes(context);
        } catch (ResolutionException e) {
            Assert.fail("resolution failed", e);
        }

        // The script just put the resolution context in as the attribute value. Yea it makes
        // no sense but it is easy to test.
        final IdPAttribute attribute = context.getResolvedIdPAttributes().get(TEST_ATTRIBUTE_NAME);
        final Collection<IdPAttributeValue<?>> values = attribute.getValues();

        Assert.assertEquals(values.size(), 1, "looking for context");
        Assert.assertEquals(values.iterator().next().getValue(), "TestContainerContextid");
    }

    protected IdPAttribute runExample(String exampleScript, String exampleData, String attributeName)
            throws ScriptException, IOException, ComponentInitializationException {
        SAMLAttributeDataConnector connector = new SAMLAttributeDataConnector();
        connector.setAttributesStrategy(new Locator(exampleData));
        connector.setId("Connector");

        final Set<ResolverPluginDependency> ds = Collections.singleton(TestSources.makeResolverPluginDependency("Connector", null));

        final ScriptedAttributeDefinition scripted = new ScriptedAttributeDefinition();
        scripted.setId(attributeName);
        scripted.setScript(new EvaluableScript(SCRIPT_LANGUAGE, getScript(exampleScript)));
        scripted.setDependencies(ds);

        final Set<DataConnector> dataDefinitions = Collections.singleton((DataConnector) connector);
        final Set<AttributeDefinition> attrDefinitions = Collections.singleton((AttributeDefinition) scripted);

        final AttributeResolverImpl resolver = new AttributeResolverImpl("foo", attrDefinitions, dataDefinitions, null);
        connector.initialize();
        scripted.initialize();
        resolver.initialize();

        final AttributeResolutionContext context =
                TestSources.createResolutionContext("principal", "issuer", "recipient");

        try {
            resolver.resolveAttributes(context);
        } catch (ResolutionException e) {
            Assert.fail("resolution failed", e);
        }

        return context.getResolvedIdPAttributes().get(attributeName);

    }

    @Test public void examples() throws ScriptException, IOException, ComponentInitializationException {

        if (isV8()) {
            return;
        }
        IdPAttribute attribute = runExample("example1.script", "example1.attribute.xml", "swissEduPersonUniqueID");

        Assert.assertEquals(attribute.getValues().iterator().next().getValue(),
                DigestUtils.md5Hex("12345678some#salt#value#12345679") + "@switch.ch");

        attribute = runExample("example2.script", "example2.attribute.xml", "eduPersonAffiliation");
        HashSet<IdPAttributeValue> set = new HashSet(attribute.getValues());
        Assert.assertEquals(set.size(), 3);
        Assert.assertTrue(set.contains(new StringAttributeValue("affiliate")));
        Assert.assertTrue(set.contains(new StringAttributeValue("student")));
        Assert.assertTrue(set.contains(new StringAttributeValue("staff")));

        attribute = runExample("example3.script", "example3.attribute.xml", "eduPersonAffiliation");
        set = new HashSet(attribute.getValues());
        Assert.assertEquals(set.size(), 2);
        Assert.assertTrue(set.contains(new StringAttributeValue("member")));
        Assert.assertTrue(set.contains(new StringAttributeValue("staff")));

        attribute = runExample("example3.script", "example3.attribute.2.xml", "eduPersonAffiliation");
        set = new HashSet(attribute.getValues());
        Assert.assertEquals(set.size(), 3);
        Assert.assertTrue(set.contains(new StringAttributeValue("member")));
        Assert.assertTrue(set.contains(new StringAttributeValue("staff")));
        Assert.assertTrue(set.contains(new StringAttributeValue("walkin")));

        attribute = runExample("example4.script", "example4.attribute.xml", "eduPersonEntitlement");
        set = new HashSet(attribute.getValues());
        Assert.assertEquals(set.size(), 1);
        Assert.assertTrue(set.contains(new StringAttributeValue("urn:mace:dir:entitlement:common-lib-terms")));

        attribute = runExample("example4.script", "example4.attribute.2.xml", "eduPersonEntitlement");
        set = new HashSet(attribute.getValues());
        Assert.assertEquals(set.size(), 2);
        Assert.assertTrue(set.contains(new StringAttributeValue("urn:mace:dir:entitlement:common-lib-terms")));
        Assert.assertTrue(set.contains(new StringAttributeValue("LittleGreenMen")));

        attribute = runExample("example4.script", "example4.attribute.3.xml", "eduPersonEntitlement");
        Assert.assertNull(attribute);

    }

    @Test public void v2Context() throws IOException, ComponentInitializationException, ResolutionException,
            ScriptException {

        if (isV8()) {
            return;
        }
        final ScriptedAttributeDefinition scripted = new ScriptedAttributeDefinition();
        scripted.setId("scripted");
        scripted.setScript(new EvaluableScript(SCRIPT_LANGUAGE, getScript("requestContext.script")));
        scripted.initialize();

        IdPAttribute result = scripted.resolve(generateContext());
        HashSet<IdPAttributeValue> set = new HashSet(result.getValues());
        Assert.assertEquals(set.size(), 3);
        Assert.assertTrue(set.contains(new StringAttributeValue(TestSources.PRINCIPAL_ID)));
        Assert.assertTrue(set.contains(new StringAttributeValue(TestSources.IDP_ENTITY_ID)));
        Assert.assertTrue(set.contains(new StringAttributeValue(TestSources.SP_ENTITY_ID)));

    }

    @Test public void unimplementedV2Context() throws IOException, ComponentInitializationException,
            ResolutionException, ScriptException {

        if (isV8()) {
            return;
        }
        final ScriptedAttributeDefinition scripted = new ScriptedAttributeDefinition();
        scripted.setId("scripted");
        scripted.setScript(new EvaluableScript(SCRIPT_LANGUAGE, getScript("requestContextUnimplemented.script")));
        scripted.initialize();

        IdPAttribute result = scripted.resolve(generateContext());
        Assert.assertEquals(result.getValues().iterator().next(), new StringAttributeValue("AllDone"));

    }

    private static AttributeResolutionContext generateContext() {
        return TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                TestSources.SP_ENTITY_ID);
    }

    final class Locator implements Function<AttributeResolutionContext, List<Attribute>> {

        final EntityAttributes obj;

        public Locator(String file) {
            obj = (EntityAttributes) unmarshallElement(fileNameToPath(file));
        }

        /** {@inheritDoc} */
        @Override
        @Nullable public List<Attribute> apply(@Nullable AttributeResolutionContext input) {
            return obj.getAttributes();
        }

    }
}