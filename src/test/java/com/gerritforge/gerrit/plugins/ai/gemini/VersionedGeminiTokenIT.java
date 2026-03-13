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
import com.google.inject.Inject;
import java.util.Optional;
import org.junit.Test;

@TestPlugin(
    name = "ai-review-agent-gemini",
    sysModule = "com.gerritforge.gerrit.plugins.ai.gemini.AiReviewRestApiModule",
    httpModule = "com.gerritforge.gerrit.plugins.ai.gemini.HttpModule")
public class VersionedGeminiTokenIT extends LightweightPluginDaemonTest {
  @Inject private VersionedGeminiToken.Accessor geminiTokenAccessor;

  @Test
  public void getTokenReturnsEmptyIfUnset() throws Exception {
    assertThat(geminiTokenAccessor.getToken(user.id())).isEqualTo(Optional.empty());
  }

  @Test
  public void setTokenPersistsGeminiConfig() throws Exception {
    String encryptedToken = "encrypted-token";

    geminiTokenAccessor.setToken(user.id(), encryptedToken);

    assertThat(geminiTokenAccessor.getToken(user.id())).hasValue(encryptedToken);
  }

  @Test
  public void setTokenOverwritesExistingValue() throws Exception {
    geminiTokenAccessor.setToken(user.id(), "old-token");
    geminiTokenAccessor.setToken(user.id(), "new-token");

    assertThat(geminiTokenAccessor.getToken(user.id())).hasValue("new-token");
  }
}
