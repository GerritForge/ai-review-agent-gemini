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

import static com.google.common.truth.Truth.assertThat;

import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.TestPlugin;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

@TestPlugin(
    name = "ai-review-agent-gemini",
    sysModule = "com.gerritforge.gerrit.plugins.ai.gemini.AiReviewRestApiModule",
    httpModule = "com.gerritforge.gerrit.plugins.ai.gemini.HttpModule")
public class VersionedGeminiTokenIT extends LightweightPluginDaemonTest {
  private VersionedGeminiToken.Factory geminiTokenFactory;

  @Before
  public void setUp() {
    geminiTokenFactory = plugin.getSysInjector().getInstance(VersionedGeminiToken.Factory.class);
  }

  @Test
  public void getTokenReturnsEmptyIfUnset() throws Exception {
    assertThat(geminiTokenFactory.create(user.id()).load().getToken()).isEqualTo(Optional.empty());
  }

  @Test
  public void setTokenPersistsGeminiConfig() throws Exception {
    String encryptedToken = "encrypted-token";

    VersionedGeminiToken geminiToken = geminiTokenFactory.create(user.id()).load();
    assertThat(geminiToken.setToken(encryptedToken)).isTrue();
    geminiToken.save();

    assertThat(geminiTokenFactory.create(user.id()).load().getToken()).hasValue(encryptedToken);
  }

  @Test
  public void setTokenOverwritesExistingValue() throws Exception {
    VersionedGeminiToken geminiToken = geminiTokenFactory.create(user.id()).load();
    assertThat(geminiToken.setToken("old-token")).isTrue();
    geminiToken.save();

    geminiToken = geminiTokenFactory.create(user.id()).load();
    assertThat(geminiToken.setToken("new-token")).isTrue();
    geminiToken.save();

    assertThat(geminiTokenFactory.create(user.id()).load().getToken()).hasValue("new-token");
  }

  @Test
  public void setTokenReturnsFalseIfValueIsUnchanged() throws Exception {
    VersionedGeminiToken geminiToken = geminiTokenFactory.create(user.id()).load();
    assertThat(geminiToken.setToken("same-token")).isTrue();
    geminiToken.save();

    geminiToken = geminiTokenFactory.create(user.id()).load();
    assertThat(geminiToken.setToken("same-token")).isFalse();
  }
}
