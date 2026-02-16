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

import static com.gerritforge.gerrit.plugins.ai.gemini.AddToken.API_TOKEN_ENDPOINT;
import static com.gerritforge.gerrit.plugins.ai.gemini.AddToken.getTokenPrefix;
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
import com.googlesource.gerrit.plugins.secureconfig.Codec;
import java.io.IOException;
import java.util.Optional;
import org.eclipse.jgit.lib.Config;
import org.junit.Before;
import org.junit.Test;

@TestPlugin(
    name = "ai-review-agent-gemini",
    sysModule = "com.gerritforge.gerrit.plugins.ai.gemini.AiReviewRestApiModule",
    httpModule = "com.gerritforge.gerrit.plugins.ai.gemini.HttpModule")
public class AddTokenIT extends LightweightPluginDaemonTest {

  @Inject private ExternalIds externalIds;
  private Codec codec;

  private static final String TEST_PASSWORD = "test-password";

  @ConfigSuite.Default
  public static Config defaultConfig() {
    Config cfg = new Config();
    cfg.setString("plugin", "ai-review-agent-gemini", "password", TEST_PASSWORD);
    return cfg;
  }

  @Before
  public void setUp() {
    codec = plugin.getSysInjector().getInstance(Codec.class);
  }

  @Test
  public void shouldAddTokenForSelf() throws Exception {
    String token = "my-secret-gemini-token";
    AddToken.Input input = new AddToken.Input();
    input.token = token;

    userRestSession.put("/accounts/self/" + API_TOKEN_ENDPOINT, input).assertCreated();

    Account.Id accountId = user.id();

    assertTokenCorrectlySet(accountId, token);
  }

  private void assertTokenCorrectlySet(Account.Id accountId, String token) throws IOException {
    ImmutableSet<ExternalId> extIds = externalIds.byAccount(accountId);
    Optional<ExternalId> optExtId =
        extIds.stream()
            .filter(e -> SCHEME_EXTERNAL.equals(e.key().scheme()))
            .filter(
                e ->
                    e.key().id() != null
                        && e.key().id().startsWith(getTokenPrefix(accountId) + ":"))
            .findFirst();

    assertThat(optExtId.isPresent()).isTrue();
    String storedId = optExtId.get().key().id();
    String encodedPart = storedId.substring(getTokenPrefix(accountId).length() + 1);
    assertThat(codec.decode(encodedPart)).isEqualTo(token);
  }

  @Test
  public void adminShouldAddTokenForOtherUser() throws Exception {
    String token = "my-secret-gemini-token";
    AddToken.Input input = new AddToken.Input();
    input.token = token;

    adminRestSession
        .put(String.join("/", "/accounts", user.id().toString(), API_TOKEN_ENDPOINT), input)
        .assertCreated();

    Account.Id accountId = user.id();

    assertTokenCorrectlySet(accountId, token);
  }

  @Test
  public void shouldReturnBadRequestOnMissingToken() throws Exception {
    AddToken.Input input = new AddToken.Input();
    input.token = null;

    adminRestSession.put("/accounts/self/" + API_TOKEN_ENDPOINT, input).assertBadRequest();
  }

  @Test
  public void shouldReturnForbiddenWhenModifyingAnotherUser() throws Exception {
    AddToken.Input input = new AddToken.Input();
    input.token = "token";

    userRestSession
        .put(String.join("/", "/accounts", admin.id().toString(), API_TOKEN_ENDPOINT), input)
        .assertForbidden();
  }
}
