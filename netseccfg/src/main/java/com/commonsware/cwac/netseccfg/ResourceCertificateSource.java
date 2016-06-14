/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.commonsware.cwac.netseccfg;

import android.content.Context;
import android.util.ArraySet;
import com.commonsware.cwac.netseccfg.conscrypt.TrustedCertificateIndex;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link CertificateSource} based on certificates contained in an application resource file.
 * @hide
 */
public class ResourceCertificateSource implements CertificateSource {
  private final Object mLock = new Object();
  private final int  mResourceId;

  private Set<X509Certificate> mCertificates;
  private Context mContext;
  private TrustedCertificateIndex mIndex;

  public ResourceCertificateSource(int resourceId, Context context) {
    mResourceId = resourceId;
    mContext = context.getApplicationContext();
  }

  private void ensureInitialized() {
    synchronized (mLock) {
      if (mCertificates != null) {
        return;
      }
      Set<X509Certificate> certificates = new HashSet<>();
      Collection<? extends Certificate> certs;
      InputStream in = null;
      try {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        in = mContext.getResources().openRawResource(mResourceId);
        certs = factory.generateCertificates(in);
      } catch (CertificateException e) {
        throw new RuntimeException("Failed to load trust anchors from id " + mResourceId,
          e);
      } finally {
        try {
          in.close();
        } catch (RuntimeException rethrown) {
          throw rethrown;
        } catch (Exception ignored) {
        }
      }
      TrustedCertificateIndex indexLocal = new TrustedCertificateIndex();
      for (Certificate cert : certs) {
        certificates.add((X509Certificate) cert);
        indexLocal.index((X509Certificate) cert);
      }
      mCertificates = certificates;
      mIndex = indexLocal;
      mContext = null;
    }
  }

  @Override
  public Set<X509Certificate> getCertificates() {
    ensureInitialized();
    return mCertificates;
  }

  @Override
  public X509Certificate findBySubjectAndPublicKey(X509Certificate cert) {
    ensureInitialized();
    java.security.cert.TrustAnchor anchor = mIndex.findBySubjectAndPublicKey(cert);
    if (anchor == null) {
      return null;
    }
    return anchor.getTrustedCert();
  }

  @Override
  public X509Certificate findByIssuerAndSignature(X509Certificate cert) {
    ensureInitialized();
    java.security.cert.TrustAnchor anchor = mIndex.findByIssuerAndSignature(cert);
    if (anchor == null) {
      return null;
    }
    return anchor.getTrustedCert();
  }

  @Override
  public Set<X509Certificate> findAllByIssuerAndSignature(X509Certificate cert) {
    ensureInitialized();
    Set<java.security.cert.TrustAnchor> anchors = mIndex.findAllByIssuerAndSignature(cert);
    if (anchors.isEmpty()) {
      return Collections.<X509Certificate>emptySet();
    }
    Set<X509Certificate> certs = new HashSet<>(anchors.size());
    for (java.security.cert.TrustAnchor anchor : anchors) {
      certs.add(anchor.getTrustedCert());
    }
    return certs;
  }
}
