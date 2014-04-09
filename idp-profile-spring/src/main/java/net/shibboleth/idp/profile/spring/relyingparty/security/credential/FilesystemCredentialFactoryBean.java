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

import java.io.File;
import java.security.KeyException;
import java.security.PrivateKey;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.collection.LazyList;

import org.opensaml.security.crypto.KeySupport;
import org.opensaml.security.x509.X509Support;
import org.springframework.beans.FatalBeanException;

/**
 * A factory bean to understand X509Filesystem credentials.
 */
public class FilesystemCredentialFactoryBean extends AbstractX509CredentialFactoryBean {

    /** The specification of where the entity File is to be found. */
    private File entityFile;

    /** Where the certificates are to be found. */
    private List<File> certificateFiles;

    /** Where the private key is to be found. */
    private File privateKeyFile;

    /** Where the crls are to be found. */
    private List<File> crlFiles;

    /**
     * Set the file with the entity certificate.
     * 
     * @param file The file to set.
     */
    public void setEntityFile(File file) {
        entityFile = file;
    }

    /**
     * Sets the files which contain the certificates.
     * 
     * @param files The value to set.
     */
    public void setCertificateFiles(List<File> files) {
        certificateFiles = files;
    }

    /**
     * Set the file with the entity certificate.
     * 
     * @param file The file to set.
     */
    public void setPrivateKeyFile(File file) {
        privateKeyFile = file;
    }

    /**
     * Sets the files which contain the crls.
     * 
     * @param files The value to set.
     */
    public void setCrlFiles(List<File> files) {
        crlFiles = files;
    }

    /** {@inheritDoc}. */
    @Override @Nullable protected X509Certificate getEntityCertificate() {

        if (null == entityFile) {
            return null;
        }
        try {
            final Collection<X509Certificate> certs = X509Support.decodeCertificates(entityFile);
            if (certs.size() > 1) {
                throw new FatalBeanException(
                        "Config element indicated an entityCertificate, but multiple certs where decoded");
            }
            return certs.iterator().next();
        } catch (CertificateException e) {
            throw new FatalBeanException("Could not decode provided Entity Certificate file "
                    + entityFile.getAbsolutePath(), e);
        }
    }

    /** {@inheritDoc} */
    @Override @Nonnull protected List<X509Certificate> getCertificates() {
        List<X509Certificate> certificates = new LazyList<>();
        for (File f : certificateFiles) {
            try {
                certificates.addAll(X509Support.decodeCertificates(f));
            } catch (CertificateException e) {
                throw new FatalBeanException("Could not decode provided CertificateFile: " + f.getAbsolutePath(), e);
            }
        }
        return certificates;
    }

    /** {@inheritDoc} */
    @Override @Nullable protected PrivateKey getPrivateKey() {
        if (null == privateKeyFile) {
            return null;
        }
        try {
            return KeySupport.decodePrivateKey(privateKeyFile, getPrivateKeyPassword().toCharArray());
        } catch (KeyException e) {
            throw new FatalBeanException("Could not decode provided KeyFile " + privateKeyFile.getAbsolutePath(), e);
        }
    }

    /** {@inheritDoc} */
    @Override @Nullable protected List<X509CRL> getCrls() {
        if (null == crlFiles) {
            return null;
        }
        List<X509CRL> crls = new LazyList<>();
        for (File crlFile:crlFiles) {
            try {
                crls.addAll(X509Support.decodeCRLs(crlFile));
            } catch (CRLException e) {
                throw new FatalBeanException("Could not decode provided CRL file " + crlFile.getAbsolutePath(), e);
            }
        }
        return crls;
    }
}
