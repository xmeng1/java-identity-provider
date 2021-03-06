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

package net.shibboleth.idp.attribute.resolver.dc.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.security.auth.Subject;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.resolver.AbstractDataConnector;
import net.shibboleth.idp.attribute.resolver.PluginDependencySupport;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.ad.impl.DelegatedWorkContext;
import net.shibboleth.idp.attribute.resolver.ad.impl.ScriptedIdPAttributeImpl;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolverWorkContext;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.scripting.AbstractScriptEvaluator;
import net.shibboleth.utilities.java.support.scripting.EvaluableScript;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.context.navigate.ParentContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

/**
 * A Data Connector which populates a series of attributes from a provided {@link ProfileRequestContext}.
 */
@SuppressWarnings("deprecation")
public class ScriptedDataConnector extends AbstractDataConnector {

    /** The id of the object where the results go. */
    @Nonnull public static final String RESULTS_STRING = "connectorResults";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ScriptedDataConnector.class);

    /** Script to be evaluated. */
    @NonnullAfterInit private EvaluableScript script;

    /** Evaluator. */
    @NonnullAfterInit private DataConnectorScriptEvaluator scriptEvaluator;
    
    /** Strategy used to locate the {@link ProfileRequestContext} to use. */
    @Nonnull private Function<AttributeResolutionContext,ProfileRequestContext> prcLookupStrategy;

    /** Strategy used to locate the {@link SubjectContext} to use. */
    @Nonnull private Function<ProfileRequestContext,SubjectContext> scLookupStrategy;

    /** The custom object we inject into all scripts. */
    @Nullable private Object customObject;

    /** Constructor. */
    public ScriptedDataConnector() {
        // Defaults to ProfileRequestContext -> RelyingPartyContext -> AttributeContext.
        prcLookupStrategy = new ParentContextLookup<>();
        scLookupStrategy = new ChildContextLookup<>(SubjectContext.class);
    }

    /**
     * Return the custom (externally provided) object.
     * 
     * @return the custom object
     */
    @Nullable public Object getCustomObject() {
        return customObject;
    }

    /**
     * Set the custom (externally provided) object.
     * 
     * @param object the custom object
     */
    @Nullable public void setCustomObject(final Object object) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        customObject = object;
    }

    /**
     * Gets the script to be evaluated.
     * 
     * @return the script to be evaluated
     */
    @NonnullAfterInit public EvaluableScript getScript() {
        return script;
    }

    /**
     * Sets the script to be evaluated.
     * 
     * @param definitionScript the script to be evaluated
     */
    public void setScript(@Nonnull final EvaluableScript definitionScript) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        script = Constraint.isNotNull(definitionScript, "Attribute definition script cannot be null");
    }

    /**
     * Set the strategy used to locate the {@link ProfileRequestContext} associated with a given
     * {@link AttributeResolutionContext}.
     * 
     * @param strategy strategy used to locate the {@link ProfileRequestContext} associated with a given
     *            {@link AttributeResolutionContext}
     */
    public void setProfileRequestContextLookupStrategy(
            @Nonnull final Function<AttributeResolutionContext, ProfileRequestContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        prcLookupStrategy = Constraint.isNotNull(strategy, "ProfileRequestContext lookup strategy cannot be null");
    }

    /**
     * Set the strategy used to locate the {@link SubjectContext} associated with a given
     * {@link AttributeResolutionContext}.
     * 
     * @param strategy strategy used to locate the {@link SubjectContext} associated with a given
     *            {@link AttributeResolutionContext}
     */
    public void
            setSubjectContextLookupStrategy(@Nonnull final Function<ProfileRequestContext, SubjectContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        scLookupStrategy = Constraint.isNotNull(strategy, "SubjectContext lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (null == script) {
            throw new ComponentInitializationException(getLogPrefix() + ": No script supplied");
        }
        
        scriptEvaluator = new DataConnectorScriptEvaluator(script);
        scriptEvaluator.setCustomObject(customObject);
        scriptEvaluator.setLogPrefix(getLogPrefix());
    }

    /** {@inheritDoc} */
    @Override @Nullable protected Map<String, IdPAttribute> doDataConnectorResolve(
            @Nonnull final AttributeResolutionContext resolutionContext,
            @Nonnull final AttributeResolverWorkContext workContext) throws ResolutionException {
        Constraint.isNotNull(resolutionContext, "AttributeResolutionContext cannot be null");
        Constraint.isNotNull(workContext, "AttributeResolverWorkContext cannot be null");

        return scriptEvaluator.execute(resolutionContext, workContext);
    }

    /**
     * Evaluator bound to the DataConnector semantic.
     */
    private class DataConnectorScriptEvaluator extends AbstractScriptEvaluator {

        /**
         * Constructor.
         * 
         * @param theScript the script we will evaluate.
         */
        public DataConnectorScriptEvaluator(@Nonnull final EvaluableScript theScript) {
            super(theScript);
        }

        /**
         * Execution hook.
         * 
         * @param resolutionContext resolution context
         * @param workContext work context
         * 
         * @return script result
         * @throws ResolutionException if the script fails
         */
        @Nullable protected Map<String,IdPAttribute> execute(
                @Nonnull final AttributeResolutionContext resolutionContext,
                @Nonnull final AttributeResolverWorkContext workContext) throws ResolutionException {
            try {
                return (Map<String,IdPAttribute>) evaluate(resolutionContext, workContext);
            } catch (final RuntimeException e) {
                throw new ResolutionException(getLogPrefix() + "Script did not run successfully", e);
            }
        }

        /** {@inheritDoc} */
        @Override
        protected void prepareContext(@Nonnull final ScriptContext scriptContext, @Nullable final Object... input) {

            log.debug("{} Adding to-be-populated attribute set '{}' to script context", getLogPrefix(), RESULTS_STRING);
            scriptContext.setAttribute(RESULTS_STRING, new HashSet<>(), ScriptContext.ENGINE_SCOPE);

            log.debug("{} Adding current attribute resolution contexts to script context", getLogPrefix());
            scriptContext.setAttribute("resolutionContext", input[0], ScriptContext.ENGINE_SCOPE);
            scriptContext.setAttribute("workContext",
                    new DelegatedWorkContext((AttributeResolverWorkContext) input[1], getLogPrefix()),
                    ScriptContext.ENGINE_SCOPE);
            
            final ProfileRequestContext prc = prcLookupStrategy.apply((AttributeResolutionContext) input[0]);
            if (null == prc) {
                log.error("{} ProfileRequestContext could not be located", getLogPrefix());
            }
            scriptContext.setAttribute("profileContext", prc, ScriptContext.ENGINE_SCOPE);
            
            final SubjectContext sc = scLookupStrategy.apply(prc);
            if (null == sc) {
                log.warn("{} Could not locate SubjectContext", getLogPrefix());
            } else {
                final List<Subject> subjects = sc.getSubjects();
                if (null == subjects) {
                    log.warn("{} Could not locate Subjects", getLogPrefix());
                } else {
                    scriptContext.setAttribute("subjects", subjects.toArray(new Subject[subjects.size()]),
                            ScriptContext.ENGINE_SCOPE);
                }
            }

            final Map<String, List<IdPAttributeValue<?>>> dependencyAttributes =
                    PluginDependencySupport.getAllAttributeValues(
                            (AttributeResolverWorkContext) input[1], getDependencies());

            for (final Entry<String,List<IdPAttributeValue<?>>> dependencyAttribute : dependencyAttributes.entrySet()) {
                log.trace("{} Adding dependent attribute '{}' with the following values to the script context: {}",
                        new Object[] {getLogPrefix(), dependencyAttribute.getKey(), dependencyAttribute.getValue(),});
                final IdPAttribute pseudoAttribute = new IdPAttribute(dependencyAttribute.getKey());
                pseudoAttribute.setValues(dependencyAttribute.getValue());

                scriptContext.setAttribute(dependencyAttribute.getKey(), new ScriptedIdPAttributeImpl(pseudoAttribute,
                        getLogPrefix()), ScriptContext.ENGINE_SCOPE);
            }
        }

        /** {@inheritDoc} */
        @Override
        @Nullable protected Object finalizeContext(@Nonnull final ScriptContext scriptContext,
                @Nullable final Object scriptResult) throws ScriptException {
            
            // The real result in our case is a variable in the context.
            final Object res = scriptContext.getAttribute(RESULTS_STRING);

            if (null == res) {
                log.error("{} Could not locate output variable '{}' from script", getLogPrefix(), RESULTS_STRING);
                throw new ScriptException("Could not locate output from script");
            }
            if (!(res instanceof Collection)) {
                log.error("{} Output '{}' was of type '{}', expected '{}'", getLogPrefix(), res.getClass().getName(),
                        Collection.class.getName());
                throw new ScriptException("Output was of the wrong type");
            }

            final Collection outputCollection = (Collection) res;
            final Map<String, IdPAttribute> outputMap = new HashMap<>(outputCollection.size());
            for (final Object o : outputCollection) {
                if (o instanceof IdPAttribute) {
                    final IdPAttribute attribute = (IdPAttribute) o;
                    if (null == attribute.getId()) {
                        log.warn("{} Anonymous Attribute encountered, ignored", getLogPrefix());
                    } else {
                        checkValues(attribute);
                        outputMap.put(attribute.getId(), attribute);
                    }
                } else {
                    log.warn("{} Output collection contained an object of type '{}', ignored", getLogPrefix(),
                            o.getClass().getName());
                }
            }

            return outputMap;
        }
        
        /**
         * Ensure that all the values in the attribute are of the correct type.
         * 
         * @param attribute the attribute to look at
         */
        private void checkValues(final IdPAttribute attribute) {

            if (null == attribute.getValues()) {
                log.info("{} Attribute '{}' has no values provided.", getLogPrefix(), attribute.getId());
                attribute.setValues(Collections.<IdPAttributeValue<?>> emptyList());
                return;
            }
            log.debug("{} Attribute '{}' has {} value(s).", getLogPrefix(), attribute.getId(),
                    attribute.getValues().size());
            final List<IdPAttributeValue<?>> inputValues = attribute.getValues();
            final List<IdPAttributeValue<?>> outputValues = new ArrayList<>(inputValues.size());

            for (final Object o : inputValues) {
                if (o instanceof IdPAttributeValue<?>) {
                    outputValues.add((IdPAttributeValue<?>) o);
                } else {
                    log.error("{} Attribute '{} has attribute value of type {}.  This will be ignored", getLogPrefix(),
                            attribute.getId(), o.getClass().getName());
                }
            }
            attribute.setValues(outputValues);
        }
    }
    
}