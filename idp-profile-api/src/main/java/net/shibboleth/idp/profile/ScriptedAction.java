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

package net.shibboleth.idp.profile;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.script.ScriptContext;
import javax.script.ScriptException;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.scripting.AbstractScriptEvaluator;
import net.shibboleth.utilities.java.support.scripting.EvaluableScript;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

/**
 * An action which calls out to a supplied script.
 * 
 * <p>
 * The return value must be an event ID to signal. As this is a generic wrapper, the action may return any event
 * depending on the context of the activity, and may manipulate the profile context tree as required.
 * </p>
 * 
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 */
public class ScriptedAction extends AbstractProfileAction {

    /** The default language is Javascript. */
    @Nonnull @NotEmpty public static final String DEFAULT_ENGINE = "JavaScript";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ScriptedAction.class);

    /** Evaluator. */
    @Nonnull private final ActionScriptEvaluator scriptEvaluator;

    /**
     * Constructor.
     * 
     * @param theScript the script we will evaluate
     * @param extraInfo debugging information
     * 
     * @deprecated
     */
    public ScriptedAction(@Nonnull final EvaluableScript theScript, @Nullable final String extraInfo) {
        scriptEvaluator = new ActionScriptEvaluator(theScript);
    }

    /**
     * Constructor.
     * 
     * @param theScript the script we will evaluate
     */
    public ScriptedAction(@Nonnull final EvaluableScript theScript) {
        scriptEvaluator = new ActionScriptEvaluator(theScript);
    }

    /**
     * Return the custom (externally provided) object.
     * 
     * @return the custom object
     */
    @Nullable public Object getCustomObject() {
        return scriptEvaluator.getCustomObject();
    }

    /**
     * Set the custom (externally provided) object.
     * 
     * @param object the custom object
     */
    @Nullable public void setCustomObject(@Nullable final Object object) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        scriptEvaluator.setCustomObject(object);
    }

    /**
     * Set whether to hide exceptions in script execution (default is false).
     * 
     * @param flag flag to set
     * 
     * @since 3.4.0
     */
    public void setHideExceptions(final boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        scriptEvaluator.setHideExceptions(flag);
    }
    
    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        scriptEvaluator.setLogPrefix(getLogPrefix());
    }

    /** {@inheritDoc} */
    @Override public void doExecute(@Nullable final ProfileRequestContext profileContext) {

        final String result = scriptEvaluator.execute(profileContext);
        if (result == null) {
            log.debug("{} signaled proceed event", getLogPrefix());
            ActionSupport.buildProceedEvent(profileContext);
        } else {
            log.debug("{} signaled event: {}", getLogPrefix(), result);
            ActionSupport.buildEvent(profileContext, (String) result);
        }
    }

    /**
     * Factory to create {@link ScriptedAction} from a {@link Resource}.
     * 
     * @param resource the resource to look at
     * @param engineName the language
     * @return the predicate
     * @throws ScriptException if the compile fails
     * @throws IOException if the file doesn't exist.
     */
    static ScriptedAction resourceScript(@Nonnull @NotEmpty final String engineName, @Nonnull final Resource resource)
            throws ScriptException, IOException {
        try (final InputStream is = resource.getInputStream()) {
            final EvaluableScript script = new EvaluableScript(engineName, is);
            return new ScriptedAction(script);
        }
    }

    /**
     * Factory to create {@link ScriptedAction} from a {@link Resource}.
     * 
     * @param resource the resource to look at
     * @return the predicate
     * @throws ScriptException if the compile fails
     * @throws IOException if the file doesn't exist.
     */
    static ScriptedAction resourceScript(@Nonnull final Resource resource) throws ScriptException, IOException {
        return resourceScript(DEFAULT_ENGINE, resource);
    }

    /**
     * Factory to create {@link ScriptedAction} from inline data.
     * 
     * @param scriptSource the script, as a string
     * @param engineName the language
     * @return the predicate
     * @throws ScriptException if the compile fails
     */
    static ScriptedAction inlineScript(@Nonnull @NotEmpty final String engineName,
            @Nonnull @NotEmpty final String scriptSource) throws ScriptException {
        final EvaluableScript script = new EvaluableScript(engineName, scriptSource);
        return new ScriptedAction(script);
    }

    /**
     * Factory to create {@link ScriptedAction} from inline data.
     * 
     * @param scriptSource the script, as a string
     * @return the predicate
     * @throws ScriptException if the compile fails
     */
    static ScriptedAction inlineScript(@Nonnull @NotEmpty final String scriptSource) throws ScriptException {
        final EvaluableScript script = new EvaluableScript(DEFAULT_ENGINE, scriptSource);
        return new ScriptedAction(script);
    }

    /**
     * Evaluator bound to the Action semantic.
     */
    private class ActionScriptEvaluator extends AbstractScriptEvaluator {

        /**
         * Constructor.
         * 
         * @param theScript the script we will evaluate.
         */
        public ActionScriptEvaluator(@Nonnull final EvaluableScript theScript) {
            super(theScript);
            setOutputType(String.class);
            setReturnOnError(EventIds.INVALID_PROFILE_CTX);
        }
        
        /** {@inheritDoc} */
        @Override
        @Nullable public Object getCustomObject() {
            return super.getCustomObject();
        }
        
        /**
         * Execution hook for the script.
         * 
         * @param profileContext profile request context
         * 
         * @return the resulting event
         */
        @Nullable public String execute(@Nullable final ProfileRequestContext profileContext) {
            return (String) evaluate(profileContext);
        }

        /** {@inheritDoc} */
        @Override
        protected void prepareContext(@Nonnull final ScriptContext scriptContext, @Nullable final Object... input) {
            scriptContext.setAttribute("profileContext", input[0], ScriptContext.ENGINE_SCOPE);
        }
    }
    
}