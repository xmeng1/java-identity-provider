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

package net.shibboleth.idp.attribute.resolver.dc.rdbms.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import net.shibboleth.idp.attribute.EmptyAttributeValue;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.dc.impl.ExecutableSearchBuilder;
import net.shibboleth.idp.attribute.resolver.dc.impl.TestCache;
import net.shibboleth.idp.saml.impl.TestSources;
import net.shibboleth.idp.testing.DatabaseTestingSupport;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;
import net.shibboleth.utilities.java.support.velocity.VelocityEngine;

import org.hsqldb.jdbc.JDBCDataSource;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * Tests for {@link RDBMSDataConnector}
 */
public class RDBMSDataConnectorTest {

    /** The connector name. */
    private static final String TEST_CONNECTOR_NAME = "rdbmsAttributeConnector";

    private static final String INIT_FILE = "/net/shibboleth/idp/attribute/resolver/impl/dc/rdbms/RdbmsStore.sql";

    private static final String DATA_FILE = "/net/shibboleth/idp/attribute/resolver/impl/dc/rdbms/RdbmsData.sql";

    private static final String USER_QUERY = "SELECT userid, name, homephone, mail FROM people WHERE userid='%s'";

    private static final String GROUP_QUERY = "SELECT name FROM groups WHERE userid='%s'";

    private DataSource datasource;

    /**
     * Creates an HSQLDB database instance.
     * 
     * @throws ClassNotFoundException if the database driver cannot be found
     * @throws SQLException if the database cannot be initialized
     */
    @BeforeTest public void setupDatabaseServer() throws ClassNotFoundException, SQLException {

        datasource = DatabaseTestingSupport.GetMockDataSource(INIT_FILE, "RDBMSDataConnectorStore");
        DatabaseTestingSupport.InitializeDataSourceFromFile(DATA_FILE, datasource);
    }

    /**
     * Creates a RDBMS data connector using the supplied builder and strategy. Sets defaults values if the parameters
     * are null.
     * 
     * @param builder to build executable statements
     * @param strategy to map results
     * @return rdbms data connector
     */
    protected RDBMSDataConnector createUserRdbmsDataConnector(final ExecutableSearchBuilder builder,
            final ResultMappingStrategy strategy) {
        final RDBMSDataConnector connector = new RDBMSDataConnector();
        connector.setId(TEST_CONNECTOR_NAME);
        connector.setDataSource(datasource);
        connector.setExecutableSearchBuilder(builder == null ? newFormatExecutableStatementBuilder(USER_QUERY) : builder);
        connector.setMappingStrategy(strategy == null ? new StringResultMappingStrategy() : strategy);
        return connector;
    }

    /**
     * Creates a RDBMS data connector for group lookup using the supplied builder and strategy. Sets defaults values if
     * the parameters are null.
     * 
     * @param builder to build executable statements
     * @param strategy to map results
     * @return rdbms data connector
     */
    protected RDBMSDataConnector createGroupRdbmsDataConnector(final ExecutableSearchBuilder builder,
            final ResultMappingStrategy strategy) {
        final RDBMSDataConnector connector = new RDBMSDataConnector();
        connector.setId(TEST_CONNECTOR_NAME + "ForGroups");
        connector.setDataSource(datasource);
        connector.setExecutableSearchBuilder(builder == null ? newFormatExecutableStatementBuilder(GROUP_QUERY) : builder);
        connector.setMappingStrategy(strategy == null ? new StringResultMappingStrategy() : strategy);
        return connector;
    }

    @Test public void initializeAndGetters() throws ComponentInitializationException, ResolutionException {

        final RDBMSDataConnector connector = new RDBMSDataConnector();
        connector.setId(TEST_CONNECTOR_NAME);

        try {
            connector.initialize();
            Assert.fail("No datasource");
        } catch (final ComponentInitializationException e) {
            // OK
        }

        connector.setDataSource(new JDBCDataSource());
        try {
            connector.initialize();
            Assert.fail("No statement builder");
        } catch (final ComponentInitializationException e) {
            // OK
        }

        final ExecutableSearchBuilder statementBuilder = newFormatExecutableStatementBuilder(USER_QUERY);
        connector.setExecutableSearchBuilder(statementBuilder);
        try {
            connector.initialize();
            Assert.fail("Invalid datasource");
        } catch (final ComponentInitializationException e) {
            // OK
        }

        connector.setDataSource(datasource);

        final StringResultMappingStrategy mappingStrategy = new StringResultMappingStrategy();
        connector.setMappingStrategy(mappingStrategy);

        try {
            connector.resolve(null);
            Assert.fail("Need to initialize first");
        } catch (final UninitializedComponentException e) {
            // OK
        }

        connector.initialize();
        try {
            connector.setDataSource(null);
            Assert.fail("Setter after initialize");
        } catch (final UnmodifiableComponentException e) {
            // OK
        }
        Assert.assertEquals(connector.getDataSource(), datasource);
        Assert.assertEquals(connector.getExecutableSearchBuilder(), statementBuilder);
        Assert.assertEquals(connector.getMappingStrategy(), mappingStrategy);
    }

    @Test public void failFastInitialize() throws ComponentInitializationException {
        final RDBMSDataConnector connector = new RDBMSDataConnector();
        connector.setId(TEST_CONNECTOR_NAME);
        final ExecutableSearchBuilder statementBuilder = newFormatExecutableStatementBuilder(USER_QUERY);
        connector.setExecutableSearchBuilder(statementBuilder);
        connector.setDataSource(new JDBCDataSource());

        try {
            connector.initialize();
            Assert.fail("No failfast");
        } catch (final ComponentInitializationException e) {
            // OK
        }

        final DataSourceValidator validator = new DataSourceValidator();
        validator.setDataSource(datasource);
        validator.setThrowValidateError(false);
        validator.initialize();
        connector.setValidator(validator);
        connector.initialize();
    }

    @Test public void resolveTemplateWithDepends() throws ComponentInitializationException, ResolutionException {
        final TemplatedExecutableStatementBuilder builder = new TemplatedExecutableStatementBuilder();
        builder.setTemplateText("SELECT userid FROM people WHERE userid='${resolutionContext.principal}' AND affiliation='${affiliation[0]}'");
        builder.setVelocityEngine(VelocityEngine.newVelocityEngine());
        builder.initialize();
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        final Map<String, List<IdPAttributeValue<?>>> dependsAttributes = new HashMap<>();
        final List<IdPAttributeValue<?>> attributeValues = new ArrayList<>();
        attributeValues.add(new StringAttributeValue("student"));
        dependsAttributes.put("affiliation", attributeValues);
        final String query = builder.getSQLQuery(context, dependsAttributes);
        Assert.assertEquals(query, "SELECT userid FROM people WHERE userid='PETER_THE_PRINCIPAL' AND affiliation='student'");
    }

    @Test public void resolveTemplateWithMultiValueDepends() throws ComponentInitializationException, ResolutionException {
        final TemplatedExecutableStatementBuilder builder = new TemplatedExecutableStatementBuilder();
        builder.setTemplateText("SELECT userid FROM people WHERE userid='${resolutionContext.principal}' AND eduPersonEntitlement='${entitlement[0]}' AND eduPersonEntitlement='${entitlement[1]}'");
        builder.setVelocityEngine(VelocityEngine.newVelocityEngine());
        builder.initialize();
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        final Map<String, List<IdPAttributeValue<?>>> dependsAttributes = new HashMap<>();
        final List<IdPAttributeValue<?>> attributeValues = new ArrayList<>();
        attributeValues.add(new StringAttributeValue("entitlement1"));
        attributeValues.add(new StringAttributeValue("entitlement2"));
        dependsAttributes.put("entitlement", attributeValues);
        final String query = builder.getSQLQuery(context, dependsAttributes);
        Assert.assertEquals(query, "SELECT userid FROM people WHERE userid='PETER_THE_PRINCIPAL' AND eduPersonEntitlement='entitlement1' AND eduPersonEntitlement='entitlement2'");
    }

    @Test public void escapeTemplate() throws ComponentInitializationException, ResolutionException {
        final TemplatedExecutableStatementBuilder builder = new TemplatedExecutableStatementBuilder();
        builder.setTemplateText("SELECT userid FROM people WHERE userid='${resolutionContext.principal}'");
        builder.setVelocityEngine(VelocityEngine.newVelocityEngine());
        builder.initialize();
        final AttributeResolutionContext context =
                TestSources.createResolutionContext("McHale's Navy", TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        final String query = builder.getSQLQuery(context, null);
        Assert.assertEquals(query, "SELECT userid FROM people WHERE userid='McHale''s Navy'");
    }

    @Test public void resolve() throws ComponentInitializationException, ResolutionException {
        final RDBMSDataConnector connector = createUserRdbmsDataConnector(null, null);
        connector.initialize();

        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);

        final Map<String, IdPAttribute> attrs = connector.resolve(context);
        // check total attributes: userid, name, homephone, mail
        Assert.assertTrue(attrs.size() == 4);
        // check userid
        Assert.assertTrue(attrs.get("USERID").getValues().size() == 1);
        Assert.assertEquals(new StringAttributeValue(TestSources.PRINCIPAL_ID), attrs.get("USERID").getValues()
                .iterator().next());
        // check name
        Assert.assertTrue(attrs.get("NAME").getValues().size() == 1);
        Assert.assertEquals(new StringAttributeValue("Peter Principal"), attrs.get("NAME").getValues().iterator()
                .next());
        // check homephone
        Assert.assertTrue(attrs.get("HOMEPHONE").getValues().size() == 1);
        Assert.assertEquals(new StringAttributeValue("555-111-2222"), attrs.get("HOMEPHONE").getValues().iterator()
                .next());
        // check mail
        Assert.assertTrue(attrs.get("MAIL").getValues().size() == 1);
        Assert.assertEquals(new StringAttributeValue("peter.principal@shibboleth.net"), attrs.get("MAIL").getValues()
                .iterator().next());
    }

    @Test(expectedExceptions = ResolutionException.class) public void resolveNoStatement()
            throws ComponentInitializationException, ResolutionException {
        final RDBMSDataConnector connector = createUserRdbmsDataConnector(new ExecutableSearchBuilder<ExecutableStatement>() {

            @Override
            @Nonnull public ExecutableStatement build(@Nonnull final AttributeResolutionContext resolutionContext,
                    @Nonnull final Map<String, List<IdPAttributeValue<?>>> dependencyAttributes) throws ResolutionException {
                return null;
            }
        }, null);
        connector.initialize();

        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        connector.resolve(context);
    }

    @Test(expectedExceptions = ResolutionException.class) public void resolveNoResultIsError()
            throws ComponentInitializationException, ResolutionException {
        final StringResultMappingStrategy mappingStrategy = new StringResultMappingStrategy();
        mappingStrategy.setNoResultAnError(true);
        final RDBMSDataConnector connector = createUserRdbmsDataConnector(null, mappingStrategy);
        connector.initialize();

        AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        try {
            Assert.assertNotNull(connector.resolve(context));
        } catch (final ResolutionException e) {
            Assert.fail("Resolution exception occurred", e);
        }

        context =
                TestSources.createResolutionContext("NOT_A_PRINCIPAL", TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        connector.resolve(context);
    }

    @Test(enabled = false, expectedExceptions = ResolutionException.class) public void resolveMultipleResultsIsError()
            throws ComponentInitializationException, ResolutionException {
        final StringResultMappingStrategy mappingStrategy = new StringResultMappingStrategy();
        mappingStrategy.setMultipleResultsAnError(true);
        final RDBMSDataConnector connector = createGroupRdbmsDataConnector(null, mappingStrategy);
        connector.initialize();

        AttributeResolutionContext context =
                TestSources.createResolutionContext("NOT_A_PRINCIPAL", TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        try {
            Assert.assertNull(connector.resolve(context));
        } catch (final ResolutionException e) {
            Assert.fail("Resolution exception occurred", e);
        }

        context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        connector.resolve(context);
    }

    @Test public void resolveWithCache() throws ComponentInitializationException, ResolutionException {
        final RDBMSDataConnector connector = createUserRdbmsDataConnector(null, null);
        final TestCache cache = new TestCache();
        connector.setResultsCache(cache);
        connector.initialize();

        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        Assert.assertTrue(cache.size() == 0);
        final Map<String, IdPAttribute> optional = connector.resolve(context);
        Assert.assertTrue(cache.size() == 1);
        Assert.assertEquals(cache.iterator().next(), optional);
    }

    @Test public void resolveMultiple() throws ComponentInitializationException, ResolutionException {
        final RDBMSDataConnector connector = createGroupRdbmsDataConnector(null, null);
        connector.initialize();

        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);

        final Map<String, IdPAttribute> attrs = connector.resolve(context);
        // check total attributes: name
        Assert.assertTrue(attrs.size() == 1);
        // check name
        Assert.assertTrue(attrs.get("NAME").getValues().size() == 2);
        Assert.assertTrue(attrs.get("NAME").getValues().contains(new StringAttributeValue("group1")));
        Assert.assertTrue(attrs.get("NAME").getValues().contains(new StringAttributeValue("group2")));
    }
    
    /** See IDP-573. */
    @Test public void resolveEmptyAttribute() throws ComponentInitializationException, ResolutionException {
        final RDBMSDataConnector connector = createUserRdbmsDataConnector(null, null);
        connector.initialize();

        final AttributeResolutionContext context =
                TestSources.createResolutionContext("PHILIP_THE_PRINCIPAL", TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);

        final Map<String, IdPAttribute> attrs = connector.resolve(context);
        // check total attributes: userid, name, homephone, mail
        Assert.assertTrue(attrs.size() == 4);
        // check userid
        Assert.assertTrue(attrs.get("USERID").getValues().size() == 4);
        Assert.assertEquals(attrs.get("USERID").getValues().iterator().next(), new StringAttributeValue(
                "PHILIP_THE_PRINCIPAL"));
        // check name
        Assert.assertTrue(attrs.get("NAME").getValues().size() == 4);
        Assert.assertEquals(attrs.get("NAME").getValues().iterator().next(), new StringAttributeValue(
                "Philip Principal"));
        // check homephone
        Assert.assertTrue(attrs.get("HOMEPHONE").getValues().size() == 4);
        Assert.assertEquals(attrs.get("HOMEPHONE").getValues().iterator().next(), new StringAttributeValue(
                "555-111-4444"));
        // check mail
        Assert.assertTrue(attrs.get("MAIL").getValues().size() == 4);
        Assert.assertTrue(attrs.get("MAIL").getValues().contains(EmptyAttributeValue.NULL));
        Assert.assertTrue(attrs.get("MAIL").getValues().contains(EmptyAttributeValue.ZERO_LENGTH));
        Assert.assertTrue(attrs.get("MAIL").getValues().contains(new StringAttributeValue("  ")));
        Assert.assertTrue(attrs.get("MAIL").getValues().contains(new StringAttributeValue(" phil.principal@shibboleth.net ")));
    }
    
    static protected FormatExecutableStatementBuilder newFormatExecutableStatementBuilder(@Nonnull final String query) {
      final FormatExecutableStatementBuilder builder = new FormatExecutableStatementBuilder();
      builder.setQuery(query);
      return builder;
    }

    static protected FormatExecutableStatementBuilder newFormatExecutableStatementBuilder(@Nonnull final String query, @Nonnull final int timeout) {
        final FormatExecutableStatementBuilder builder = new FormatExecutableStatementBuilder();
        builder.setQuery(query);
        builder.setQueryTimeout(timeout);
        return builder;
    }
}