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

import java.security.PrivateKey;
import java.security.PublicKey;

import javax.annotation.Nullable;
import javax.crypto.SecretKey;

import org.opensaml.security.credential.BasicCredential;
import org.opensaml.security.crypto.KeySupport;
import org.springframework.beans.factory.BeanCreationException;

/**
 * A factory bean to collect information to do with a {@link BasicCredential}.
 */
public abstract class AbstractBasicCredentialFactoryBean extends AbstractCredentialFactoryBean<BasicCredential> {
    
    /** The secretKey Password (if any). */
    @Nullable private char[] secretKeyPassword;

    /** {@inheritDoc} */
    @Override protected BasicCredential createInstance() throws Exception {

        PrivateKey privateKey = getPrivateKey();
        PublicKey publicKey = getPublicKey();
        SecretKey secretKey = getSecretKey();
        final BasicCredential credential;
        
        if (null == publicKey) {
            throw new BeanCreationException("No Public Key specified");            
        }
        if (null == privateKey) {
            credential = new BasicCredential(publicKey);
        } else {
            if (!KeySupport.matchKeyPair(publicKey, privateKey)) {
                throw new BeanCreationException("Public and private keys do not match");
            }
            credential = new BasicCredential(publicKey, privateKey);
        }
        if(null != secretKey) {
            credential.setSecretKey(secretKey);
        }
        return credential;
    }

    /** {@inheritDoc} */
    @Override public Class<?> getObjectType() {
        return BasicCredential.class;
    }
    
    /**
     * Get the password for the Secret key.
     * 
     * @return Returns the secretKeyPassword.
     */
    @Nullable public char[] getSecretKeyPassword() {
        return secretKeyPassword;
    }

    /**
     * Set the password for the Secret key.
     * 
     * @param password The password to set.
     */
    public void setSecretKeyPassword(@Nullable char[] password) {
        secretKeyPassword = password;
    }

    /**
     * return the configured Public Key. 
     * 
     * @return the key, or none if not configured.
     */
    @Nullable protected abstract PublicKey getPublicKey();

    /**
     * Get the configured Private key.
     * 
     * @return the key or null if non configured
     */
    @Nullable protected abstract PrivateKey getPrivateKey();

    /**
     * return the configured Secret Key. 
     * 
     * @return the key, or none if not configured.
     */
    @Nullable protected abstract SecretKey getSecretKey();

}