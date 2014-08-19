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

package net.shibboleth.idp.profile.audit.impl;

import java.util.Collection;
import java.util.Collections;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;

import com.google.common.base.Function;
import com.google.common.base.Functions;

import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.utilities.java.support.logic.Constraint;

/** {@link Function} that returns the attribute IDs from an {@link AttributeContext}. */
public class AttributesAuditExtractor implements Function<ProfileRequestContext,Collection<String>> {

    /** Lookup strategy for AttributeContext to read from. */
    @Nonnull private final Function<ProfileRequestContext,AttributeContext> attributeContextLookupStrategy;

    /** Constructor. */
    public AttributesAuditExtractor() {
        // Defaults to ProfileRequestContext -> RelyingPartyContext -> AttributeContext.
        attributeContextLookupStrategy = Functions.compose(new ChildContextLookup<>(AttributeContext.class),
                new ChildContextLookup<ProfileRequestContext,RelyingPartyContext>(RelyingPartyContext.class));
    }
    
    /**
     * Constructor.
     *
     * @param strategy lookup strategy for {@link AttributeContext}
     */
    public AttributesAuditExtractor(@Nonnull final Function<ProfileRequestContext,AttributeContext> strategy) {
        attributeContextLookupStrategy = Constraint.isNotNull(strategy,
                "AttributeContext lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    @Nullable public Collection<String> apply(@Nullable final ProfileRequestContext input) {
        final AttributeContext attributeCtx = attributeContextLookupStrategy.apply(input);
        if (attributeCtx != null) {
            return attributeCtx.getIdPAttributes().keySet();
        } else {
            return Collections.emptyList();
        }
    }

}