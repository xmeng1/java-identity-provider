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

package net.shibboleth.idp.attribute.resolver.ad.mapped.impl;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * test for {@link SourceValue}.
 */
public class SourceValueTest {

    Logger log = LoggerFactory.getLogger(SourceValueTest.class);

    @Test public void sourceValue() throws ComponentInitializationException {
        SourceValue value = newSourceValue("value", false, true);

        Assert.assertEquals(value.getValue(), "value");
        Assert.assertTrue(value.isPartialMatch());
        Assert.assertFalse(value.isIgnoreCase());

        log.info("Value = 'value', ignore = true, partial = false", value.toString());

        value = newSourceValue("eulaV", true, false);

        Assert.assertEquals(value.getPattern().pattern(), "eulaV");
        Assert.assertFalse(value.isPartialMatch());
        Assert.assertTrue(value.isIgnoreCase());
        log.info("Value = 'eulaV', ignore = false, partial = true", value.toString());

    }

    public static SourceValue newSourceValue(final String value, final boolean ignoreCase, final boolean partialMatch)
            throws ComponentInitializationException {

        final SourceValue sourceValue = new SourceValue();
        sourceValue.setValue(value);
        sourceValue.setIgnoreCase(ignoreCase);
        sourceValue.setPartialMatch(partialMatch);
        sourceValue.initialize();
        return sourceValue;
    }

}
