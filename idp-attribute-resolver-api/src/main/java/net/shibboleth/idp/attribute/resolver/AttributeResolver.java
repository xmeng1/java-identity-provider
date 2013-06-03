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

package net.shibboleth.idp.attribute.resolver;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.collection.LazyList;
import net.shibboleth.utilities.java.support.collection.LazySet;
import net.shibboleth.utilities.java.support.component.AbstractDestructableIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponent;
import net.shibboleth.utilities.java.support.component.ValidatableComponent;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

//TODO(lajoie) perf metrics
//TODO(lajoie) need to deal with thread safety issue 
//             where attribute definitions/data connectors might change in the midst of a resolution

/** A component that resolves the attributes for a particular subject. */
@ThreadSafe
public class AttributeResolver extends AbstractDestructableIdentifiableInitializableComponent implements
        ValidatableComponent, UnmodifiableComponent {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AttributeResolver.class);

    /** Attribute definitions defined for this resolver. */
    private final Map<String, BaseAttributeDefinition> attributeDefinitions;

    /** Data connectors defined for this resolver. */
    private final Map<String, BaseDataConnector> dataConnectors;

    /** cache for the log prefix - to save multiple recalculations. */
    private String logPrefix;

    /**
     * Constructor.
     * 
     * @param resolverId ID of this resolver
     * @param definitions attribute definitions loaded in to this resolver
     * @param connectors data connectors loaded in to this resolver
     */
    public AttributeResolver(@Nonnull @NotEmpty String resolverId,
            @Nullable @NullableElements Collection<BaseAttributeDefinition> definitions,
            @Nullable @NullableElements Collection<BaseDataConnector> connectors) {
        setId(resolverId);

        logPrefix = new StringBuilder("Attribute Resolver '").append(getId()).append("':").toString();

        HashMap<String, BaseAttributeDefinition> checkedDefinitions = new HashMap<String, BaseAttributeDefinition>();
        if (definitions != null) {
            for (BaseAttributeDefinition definition : definitions) {
                if (definition != null) {
                    checkedDefinitions.put(definition.getId(), definition);
                }
            }
        }
        attributeDefinitions = ImmutableMap.copyOf(checkedDefinitions);

        HashMap<String, BaseDataConnector> checkedConnectors = new HashMap<String, BaseDataConnector>();
        if (connectors != null) {
            for (BaseDataConnector connector : connectors) {
                if (connector != null) {
                    checkedConnectors.put(connector.getId(), connector);
                }
            }
        }
        dataConnectors = ImmutableMap.copyOf(checkedConnectors);
    }

    /**
     * Gets the collection of attribute definitions for this resolver.
     * 
     * @return attribute definitions loaded in to this resolver
     */
    @Nonnull @NonnullElements @Unmodifiable public Map<String, BaseAttributeDefinition> getAttributeDefinitions() {
        return attributeDefinitions;
    }

    /**
     * Gets the unmodifiable collection of data connectors for this resolver.
     * 
     * @return data connectors loaded in to this resolver
     */
    @Nonnull @NonnullElements @Unmodifiable public Map<String, BaseDataConnector> getDataConnectors() {
        return dataConnectors;
    }

    /**
     * This method checks if each registered data connector and attribute definition is valid (via
     * {@link BaseResolverPlugin#validate()} and checks to see if there are any loops in the dependency for all
     * registered plugins.
     * 
     * {@inheritDoc}
     */
    public void validate() throws ComponentValidationException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        final LazyList<String> invalidDataConnectors = new LazyList<String>();
        for (BaseDataConnector plugin : dataConnectors.values()) {
            log.debug("{} checking if data connector {} is valid", logPrefix, plugin.getId());
            if (!validateDataConnector(plugin, invalidDataConnectors)) {
                invalidDataConnectors.add(plugin.getId());
            }
        }

        final LazyList<String> invalidAttributeDefinitions = new LazyList<String>();
        for (BaseAttributeDefinition plugin : attributeDefinitions.values()) {
            log.debug("{} checking if attribute definition {} is valid", logPrefix, plugin.getId());
            try {
                plugin.validate();
                log.debug("{} attribute definition {} is valid", logPrefix, plugin.getId());
            } catch (ComponentValidationException e) {
                log.warn("{} attribute definition {} is not valid", new Object[] {logPrefix, plugin.getId(), e,});
                invalidAttributeDefinitions.add(plugin.getId());
            }
        }

        if (!invalidDataConnectors.isEmpty() || !invalidAttributeDefinitions.isEmpty()) {
            throw new ComponentValidationException(logPrefix + " the following attribute definitions were invalid ["
                    + StringSupport.listToStringValue(invalidAttributeDefinitions, ", ")
                    + "] and the following data connectors were invalid ["
                    + StringSupport.listToStringValue(invalidDataConnectors, ", ") + "]");
        }
    }

    /**
     * Resolves the attribute for the give request. Note, if attributes are requested,
     * {@link AttributeResolutionContext#getRequestedAttributes()}, the resolver will <strong>not</strong> fail if they
     * can not be resolved. This information serves only as a hint to the resolver to, potentially, optimize the
     * resolution of attributes.
     * 
     * @param resolutionContext the attribute resolution context that identifies the request subject and accumulates the
     *            resolved attributes
     * 
     * @throws ResolutionException thrown if there is a problem resolving the attributes for the subject
     */
    public void resolveAttributes(@Nonnull final AttributeResolutionContext resolutionContext)
            throws ResolutionException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        Constraint.isNotNull(resolutionContext, "Attribute resolution context can not be null");

        log.debug("{} initiating attribute resolution", logPrefix);

        if (attributeDefinitions.size() == 0) {
            log.debug("{} no attribute definition available, no attributes were resolved", logPrefix);
            return;
        }

        final Collection<String> attributeIds = getToBeResolvedAttributes(resolutionContext);
        log.debug("{} attempting to resolve the following attribute definitions {}", logPrefix, attributeIds);

        for (String attributeId : attributeIds) {
            resolveAttributeDefinition(attributeId, resolutionContext);
        }

        log.debug("{} finalizing resolved attributes", logPrefix);
        finalizeResolvedAttributes(resolutionContext);

        log.debug("{} final resolved attribute collection: {}", logPrefix, resolutionContext.getResolvedAttributes()
                .keySet());

        return;
    }

    /** {@inheritDoc} */
    protected void doDestroy() {
        for (BaseResolverPlugin plugin : attributeDefinitions.values()) {
            plugin.destroy();
        }

        for (BaseResolverPlugin plugin : dataConnectors.values()) {
            plugin.destroy();
        }
    }

    /**
     * Gets the list of attributes, identified by IDs, that should be resolved. If the
     * {@link AttributeResolutionContext#getRequestedAttributes()} is not empty then those attributes are the ones to be
     * resolved, otherwise all registered attribute definitions are to be resolved.
     * 
     * @param resolutionContext current resolution context
     * 
     * @return list of attributes, identified by IDs, that should be resolved
     */
    @Nonnull @NonnullElements protected Collection<String> getToBeResolvedAttributes(
            @Nonnull final AttributeResolutionContext resolutionContext) {
        Constraint.isNotNull(resolutionContext, "Attribute resolution context can not be null");

        final Collection<String> attributeIds = new LazyList<String>();
        for (Attribute requestedAttribute : resolutionContext.getRequestedAttributes()) {
            attributeIds.add(requestedAttribute.getId());
        }

        // if no attributes requested, then resolve everything
        if (attributeIds.isEmpty()) {
            attributeIds.addAll(attributeDefinitions.keySet());
        }

        return attributeIds;
    }

    /**
     * Resolve the {@link BaseAttributeDefinition} which has the specified ID.
     * 
     * The results of the resolution are stored in the given {@link AttributeResolutionContext}.
     * 
     * @param attributeId id of the attribute definition to resolve
     * @param resolutionContext resolution context that we are working in
     * 
     * @throws ResolutionException if unable to resolve the requested attribute definition
     */
    protected void resolveAttributeDefinition(@Nonnull final String attributeId,
            @Nonnull final AttributeResolutionContext resolutionContext) throws ResolutionException {
        Constraint.isNotNull(attributeId, "Attribute ID can not be null");
        Constraint.isNotNull(resolutionContext, "Attribute resolution context can not be null");

        log.debug("{} beginning to resolve attribute definition {}", logPrefix, attributeId);

        if (resolutionContext.getResolvedAttributeDefinitions().containsKey(attributeId)) {
            log.debug("{} attribute definition {} was already resolved, nothing to do", logPrefix, attributeId);
            return;
        }

        final BaseAttributeDefinition definition = attributeDefinitions.get(attributeId);
        if (definition == null) {
            log.debug("{} no attribute definition was registered with ID {}, nothing to do", logPrefix, attributeId);
            return;
        }

        resolveDependencies(definition, resolutionContext);

        log.debug("{} resolving attribute definition {}", logPrefix, attributeId);
        final Attribute resolvedAttribute = definition.resolve(resolutionContext);

        if (null == resolvedAttribute) {
            log.debug("{} attribute definition {} produced no attribute", logPrefix, attributeId);
            // TODO why would we record this?
        } else {
            log.debug("{} attribute definition {} produced an attribute with {} values", new Object[] {logPrefix,
                    attributeId, resolvedAttribute.getValues().size(),});
        }

        resolutionContext.recordAttributeDefinitionResolution(definition, resolvedAttribute);
    }

    /**
     * Resolve the {@link DataConnector} which has the specified ID.
     * 
     * The results of the resolution are stored in the given {@link AttributeResolutionContext}.
     * 
     * @param connectorId id of the data connector to resolve
     * @param resolutionContext resolution context that we are working in
     * 
     * @throws ResolutionException if unable to resolve the requested connector
     */
    protected void resolveDataConnector(@Nonnull final String connectorId,
            @Nonnull final AttributeResolutionContext resolutionContext) throws ResolutionException {
        Constraint.isNotNull(connectorId, "Data connector ID can not be null");
        Constraint.isNotNull(resolutionContext, "Attribute resolution context can not be null");

        log.debug("{} beginning to resolve data connector {}", logPrefix, connectorId);
        if (resolutionContext.getResolvedDataConnectors().containsKey(connectorId)) {
            log.debug("{} data connector {} was already resolved, nothing to do", logPrefix, connectorId);
            return;
        }

        final BaseDataConnector connector = dataConnectors.get(connectorId);
        if (connector == null) {
            log.debug("{} no data connector was registered with ID {}, nothing to do", logPrefix, connectorId);
            return;
        }

        resolveDependencies(connector, resolutionContext);
        Map<String, Attribute> resolvedAttributes;
        try {
            log.debug("{} resolving data connector {}", logPrefix, connectorId);
            resolvedAttributes = connector.resolve(resolutionContext);
        } catch (ResolutionException e) {
            final String failoverDataConnectorId = connector.getFailoverDataConnectorId();
            if (null != failoverDataConnectorId) {
                log.debug("{} data connector {} failed to resolve, invoking failover data"
                        + " connector {}.  Reason for the failure was: {}", new Object[] {logPrefix, connectorId,
                        failoverDataConnectorId, e,});
                resolveDataConnector(failoverDataConnectorId, resolutionContext);
                return;
            } else {
                // Pass it on. Do not look at propagateException because this is handled in the
                // connector code logic.
                throw e;
            }
        }

        if (null != resolvedAttributes) {
            log.debug("{} data connector {} resolved the following attributes {}", new Object[] {logPrefix,
                    connectorId, resolvedAttributes.keySet(),});
        } else {
            log.debug("{} data connector {} produced no attributes", logPrefix, connectorId);
        }
        resolutionContext.recordDataConnectorResolution(connector, resolvedAttributes);
    }

    /**
     * Resolves all the dependencies for a given plugin.
     * 
     * @param plugin plugin whose dependencies should be resolved
     * @param resolutionContext current resolution context
     * 
     * @throws ResolutionException thrown if there is a problem resolving a dependency
     */
    protected void resolveDependencies(@Nonnull final BaseResolverPlugin<?> plugin,
            @Nonnull final AttributeResolutionContext resolutionContext) throws ResolutionException {
        Constraint.isNotNull(plugin, "Plugin dependency can not be null");
        Constraint.isNotNull(resolutionContext, "Attribute resolution context can not be null");

        if (plugin.getDependencies().isEmpty()) {
            return;
        }

        log.debug("{} resolving dependencies for {}", logPrefix, plugin.getId());

        String pluginId;
        for (ResolverPluginDependency dependency : plugin.getDependencies()) {
            pluginId = dependency.getDependencyPluginId();
            if (attributeDefinitions.containsKey(pluginId)) {
                resolveAttributeDefinition(pluginId, resolutionContext);
            } else if (dataConnectors.containsKey(pluginId)) {
                resolveDataConnector(pluginId, resolutionContext);
            } else {
                // This will not happen for as long as we test this in initialization
                throw new ResolutionException("Plugin " + plugin.getId() + " contains a depedency on plugin "
                        + pluginId + " which does not exist.");
            }
        }

        log.debug("{} finished resolving dependencies for {}", logPrefix, plugin.getId());
    }

    /**
     * Finalizes the set of resolved attributes and places them in the {@link AttributeResolutionContext}. The result of
     * each {@link BaseAttributeDefinition} resolution is inspected. If the result is not null, a dependency-only
     * attribute, or an attribute that contains no values then it becomes part of the final set of resolved attributes.
     * 
     * @param resolutionContext current resolution context
     */
    protected void finalizeResolvedAttributes(@Nonnull final AttributeResolutionContext resolutionContext) {
        Constraint.isNotNull(resolutionContext, "Attribute resolution context can not be null");

        final LazySet<Attribute> resolvedAttributes = new LazySet<Attribute>();

        Attribute resolvedAttribute;
        for (ResolvedAttributeDefinition definition : resolutionContext.getResolvedAttributeDefinitions().values()) {
            resolvedAttribute = definition.getResolvedAttribute();

            // remove nulls
            if (null == resolvedAttribute) {
                log.debug("{} removing result of attribute definition {}, it's null", logPrefix, definition.getId());
                continue;
            }

            // remove dependency-only attributes
            if (definition.isDependencyOnly()) {
                log.debug("{} removing result of attribute definition {}, it's marked as depdency only", logPrefix,
                        definition.getId());
                continue;
            }

            // remove value-less attributes
            if (resolvedAttribute.getValues().size() == 0) {
                log.debug("{} removing result of attribute definition {}, it's attribute contains no values",
                        logPrefix, definition.getId());
                continue;
            }

            resolvedAttributes.add(resolvedAttribute);
        }

        resolutionContext.setResolvedAttributes(resolvedAttributes);
    }

    /**
     * Validates the given data connector.
     * 
     * @param connector connector to valid
     * @param invalidDataConnectors data connectors which have already been validated
     * 
     * @return whether the given data connector is valid
     */
    protected boolean validateDataConnector(@Nonnull BaseDataConnector connector,
            @Nonnull LazyList<String> invalidDataConnectors) {
        Constraint.isNotNull(connector, "To-be-validated connector can not be null");
        Constraint.isNotNull(invalidDataConnectors, "List of invalid data connectors can not be null");

        final String failoverId = connector.getFailoverDataConnectorId();
        if (null != failoverId) {
            if (!dataConnectors.containsKey(failoverId)) {
                log.warn("{} failover data connector {} for {} cannot be found", new Object[] {logPrefix, failoverId,
                        connector.getId(),});
                return false;
            }
        }

        boolean returnValue;
        try {
            connector.validate();
            log.debug("{} data connector {} is valid", logPrefix, connector.getId());
            returnValue = true;
        } catch (ComponentValidationException e) {
            if (null != failoverId) {
                if (invalidDataConnectors.contains(failoverId)) {
                    log.warn("{} data connector {} is not valid for the following reason and"
                            + " failover data connector {} has already been found to be inavlid", new Object[] {
                            logPrefix, connector.getId(), failoverId, e,});
                    invalidDataConnectors.add(connector.getId());
                    returnValue = false;
                } else {
                    log.warn("{} data connector {} is not valid for the following reason {},"
                            + " checking if failover data connector {} is valid",
                            new Object[] {logPrefix, connector.getId(), e, failoverId,});
                    returnValue = validateDataConnector(dataConnectors.get(failoverId), invalidDataConnectors);
                    if (!returnValue) {
                        invalidDataConnectors.add(failoverId);
                    }
                }
            } else {

                log.warn("{} data connector {} is not valid and has not failover connector", new Object[] {logPrefix,
                        connector.getId(), e,});
                returnValue = false;
            }
        }
        return returnValue;
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        HashSet<String> dependencyVerifiedPlugins = new HashSet<String>();
        for (BaseDataConnector plugin : dataConnectors.values()) {
            ComponentSupport.initialize(plugin);
        }
        for (BaseAttributeDefinition plugin : attributeDefinitions.values()) {
            ComponentSupport.initialize(plugin);
        }

        for (BaseDataConnector plugin : dataConnectors.values()) {
            log.debug("{} checking if data connector {} is has a circular dependency", logPrefix, plugin.getId());
            checkPlugInDependencies(plugin.getId(), plugin, dependencyVerifiedPlugins);
        }

        for (BaseAttributeDefinition plugin : attributeDefinitions.values()) {
            log.debug("{} checking if attribute definition {} has a circular dependency", logPrefix, plugin.getId());
            checkPlugInDependencies(plugin.getId(), plugin, dependencyVerifiedPlugins);
        }
    }

    /**
     * Checks to ensure that there are no circular dependencies or dependencies on non-existent plugins.
     * 
     * @param circularCheckPluginId the ID of the plugin currently being checked for circular dependencies
     * @param plugin current plugin, in the dependency tree of the plugin being checked, that we're currently looking at
     * @param checkedPlugins IDs of plugins that have already been checked and known to be good
     * 
     * @throws ComponentInitializationException thrown if there is a dependency loop
     */
    protected void checkPlugInDependencies(final String circularCheckPluginId, final BaseResolverPlugin<?> plugin,
            final Set<String> checkedPlugins) throws ComponentInitializationException {
        final String pluginId = plugin.getId();

        BaseResolverPlugin<?> dependencyPlugin;
        for (ResolverPluginDependency dependency : plugin.getDependencies()) {
            if (checkedPlugins.contains(pluginId)) {
                continue;
            }

            if (circularCheckPluginId.equals(dependency.getDependencyPluginId())) {
                throw new ComponentInitializationException(logPrefix + " Plugin " + circularCheckPluginId
                        + " and plugin " + dependency.getDependencyPluginId()
                        + " have a circular dependecy on each other.");
            }

            dependencyPlugin = dataConnectors.get(dependency.getDependencyPluginId());
            if (dependencyPlugin == null) {
                dependencyPlugin = attributeDefinitions.get(dependency.getDependencyPluginId());
            }
            if (dependencyPlugin == null) {
                throw new ComponentInitializationException(logPrefix + " Plugin " + plugin.getId()
                        + " has a dependency on plugin " + dependency.getDependencyPluginId() + " which doesn't exist");
            }

            checkPlugInDependencies(circularCheckPluginId, dependencyPlugin, checkedPlugins);
            checkedPlugins.add(pluginId);
        }
    }
}