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

package net.shibboleth.idp.profile.spring.relyingparty.security.credential;

import java.io.IOException;

import net.shibboleth.ext.spring.config.DurationToLongConverter;
import net.shibboleth.ext.spring.config.StringToIPRangeConverter;

import org.opensaml.security.credential.Credential;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.env.MockPropertySource;
import org.testng.annotations.BeforeSuite;

import com.google.common.collect.Sets;

/**
 * Base mechanics for Credential parser tests
 */
public class AbstractCredentialParserTest {

    private static final String PATH = "/net/shibboleth/idp/profile/spring/relyingparty/security/credential/";
    
    protected static final String SP_ID = "https://sp.example.org/sp/shibboleth"; 
    protected static final String IDP_ID = "https://idp.example.org/idp/shibboleth";
    
    static private String workspaceDirName;
    
    @BeforeSuite public void setupDirs() throws IOException {
        final ClassPathResource resource = new ClassPathResource(PATH);
        workspaceDirName = resource.getFile().getAbsolutePath();
    }

    /**
     * Set up a property placeholder called DIR which points to the test directory
     * this makes the test location insensitive but able to look at the local
     * filesystem.
     * @param context the context
     * @throws IOException 
     */
    protected void setDirectoryPlaceholder(GenericApplicationContext context) throws IOException {
        PropertySourcesPlaceholderConfigurer placeholderConfig = new PropertySourcesPlaceholderConfigurer();
        
        MutablePropertySources propertySources = context.getEnvironment().getPropertySources();
        MockPropertySource mockEnvVars = new MockPropertySource();
        mockEnvVars.setProperty("DIR", workspaceDirName);
        
        propertySources.replace(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, mockEnvVars);
        placeholderConfig.setPropertySources(propertySources);
        
        context.addBeanFactoryPostProcessor(placeholderConfig);
        
    }
    
    protected <T extends Credential> T getBean(Class<T> claz,  boolean validating, String... files) throws IOException{
        final Resource[] resources = new Resource[files.length];
       
        for (int i = 0; i < files.length; i++) {
            resources[i] = new ClassPathResource(PATH + files[i]);
        }
        
        final GenericApplicationContext context = new GenericApplicationContext();

        setDirectoryPlaceholder(context);
        
        ConversionServiceFactoryBean service = new ConversionServiceFactoryBean();
        context.setDisplayName("ApplicationContext: " + claz);
        service.setConverters(Sets.newHashSet(new DurationToLongConverter(), new StringToIPRangeConverter()));
        service.afterPropertiesSet();

        context.getBeanFactory().setConversionService(service.getObject());

        final XmlBeanDefinitionReader configReader = new XmlBeanDefinitionReader(context);


        configReader.setValidating(validating);
        
        configReader.loadBeanDefinitions(resources);
        context.refresh();
        
        return context.getBean(claz);
    }

}