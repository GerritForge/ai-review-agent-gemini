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
import static com.google.gerrit.server.account.externalids.ExternalId.SCHEME_EXTERNAL;

import com.google.common.collect.ImmutableSet;
import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.entities.Account;
import com.google.gerrit.server.account.externalids.ExternalId;
import com.google.gerrit.server.account.externalids.ExternalIds;
import com.google.gerrit.testing.ConfigSuite;
import com.google.inject.Inject;
import org.eclipse.jgit.lib.Config;
import org.junit.Before;
import org.junit.Test;

@TestPlugin(
    name = "ai-review-agent-gemini",
    sysModule = "com.gerritforge.gerrit.plugins.ai.gemini.AiReviewRestApiModule",
    httpModule = "com.gerritforge.gerrit.plugins.ai.gemini.HttpModule")
public class AddTokenIT extends LightweightPluginDaemonTest {

  @Inject private ExternalIds externalIds;
  private PBECodec codec;

  private static final String TEST_PASSWORD = "test-password";
  private static final String TEST_SALT = "thesalt1234";

  @ConfigSuite.Default
  public static Config defaultConfig() {
    Config cfg = new Config();
    cfg.setString("plugin", "ai-review-agent-gemini", "password", TEST_PASSWORD);
    cfg.setString("plugin", "ai-review-agent-gemini", "salt", TEST_SALT);
    return cfg;
  }

  @Before
  public void setUp() {
    codec = plugin.getSysInjector().getInstance(PBECodec.class);
  }

  @Test
  public void shouldAddTokenForSelf() throws Exception {
    String token = "my-secret-gemini-token";
    AddToken.Input input = new AddToken.Input();
    input.token = token;

    adminRestSession.put("/accounts/self/geminiToken", input).assertOK();

    Account.Id accountId = admin.id();
    String expectedExtId = AddToken.getFormattedUserToken(accountId, token, codec);

    ImmutableSet<ExternalId> extIds = externalIds.byAccount(accountId);
    boolean found =
        extIds.stream()
            .anyMatch(
                e ->
                    SCHEME_EXTERNAL.equals(e.key().scheme()) && expectedExtId.equals(e.key().id()));

    assertThat(found).isTrue();
  }

  @Test
  public void shouldReturnBadRequestOnMissingToken() throws Exception {
    AddToken.Input input = new AddToken.Input();
    input.token = null;

    adminRestSession.put("/accounts/self/geminiToken", input).assertBadRequest();
  }

  @Test
  public void shouldReturnForbiddenWhenModifyingAnotherUser() throws Exception {
    AddToken.Input input = new AddToken.Input();
    input.token = "token";

    adminRestSession.put("/accounts/" + user.id().get() + "/geminiToken", input).assertForbidden();
  }
}
