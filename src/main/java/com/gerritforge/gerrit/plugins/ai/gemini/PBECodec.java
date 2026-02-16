// Copyright (C) 2026 GerritForge, Inc.
//
// Licensed under the BSL 1.1 (the "License");
// you may not use this file except in compliance with the License.
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.gerritforge.gerrit.plugins.ai.gemini;

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

@Singleton
public class PBECodec {
  private static final String GEMINI_PASSWORD = "password";
  private static final String GEMINI_SALT = "salt";
  private static final FluentLogger log = FluentLogger.forEnclosingClass();
  private final String password;
  private final byte[] salt;

  @Inject
  protected PBECodec(PluginConfigFactory cfgFactory, @PluginName String pluginName) {
    this.password = cfgFactory.getFromGerritConfig(pluginName).getString(GEMINI_PASSWORD);
    String saltBase64 = cfgFactory.getFromGerritConfig(pluginName).getString(GEMINI_SALT);
    if (saltBase64 == null || saltBase64.isEmpty()) {
      throw new IllegalStateException("Missing 'salt' in configuration");
    }
    this.salt = Base64.getDecoder().decode(saltBase64);
  }

  private static final int iterationCount = 2048;

  String encode(String s) {
    try {
      Key sKey = generateKey();
      Cipher encoder = getCipher();
      encoder.init(Cipher.ENCRYPT_MODE, sKey, getCipherParameterSpec());
      return Base64.getEncoder()
          .encodeToString(encoder.doFinal(s.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      log.atSevere().withCause(e).log("encode() failed");
      throw new IllegalArgumentException("encode() failed", e);
    }
  }

  String decode(String s) {
    try {
      Cipher encoder = getCipher();
      Key sKey = generateKey();
      encoder.init(Cipher.DECRYPT_MODE, sKey, getCipherParameterSpec());
      return new String(encoder.doFinal(Base64.getDecoder().decode(s)), StandardCharsets.UTF_8);
    } catch (Exception e) {
      log.atSevere().withCause(e).log("decode() failed");
      throw new IllegalArgumentException("encode() failed", e);
    }
  }

  private PBEParameterSpec getCipherParameterSpec() {
    return new PBEParameterSpec(salt, iterationCount);
  }

  private Cipher getCipher() throws NoSuchAlgorithmException, NoSuchPaddingException {
    @SuppressWarnings("InsecureCipherMode")
    Cipher encoder = Cipher.getInstance("PBEWithMD5AndDES", Security.getProvider("SunJCE"));
    return encoder;
  }

  private Key generateKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
    PBEKeySpec pbeSpec = new PBEKeySpec(password.toCharArray());
    SecretKeyFactory keyFact =
        SecretKeyFactory.getInstance("PBEWithMD5AndDES", Security.getProvider("SunJCE"));
    return keyFact.generateSecret(pbeSpec);
  }
}
