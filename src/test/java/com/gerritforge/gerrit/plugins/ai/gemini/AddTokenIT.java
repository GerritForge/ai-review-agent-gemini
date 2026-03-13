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

import static com.gerritforge.gerrit.plugins.ai.gemini.AiReviewRestApiModule.API_TOKEN_ENDPOINT;
import static com.google.common.truth.Truth.assertThat;

import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.entities.Account;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.googlesource.gerrit.plugins.secureconfig.Codec;
import java.io.IOException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.junit.Before;
import org.junit.Test;

@TestPlugin(
    name = "ai-review-agent-gemini",
    sysModule = "com.gerritforge.gerrit.plugins.ai.gemini.AiReviewRestApiModule",
    httpModule = "com.gerritforge.gerrit.plugins.ai.gemini.HttpModule")
public class AddTokenIT extends LightweightPluginDaemonTest {

  @Inject private VersionedGeminiToken.Accessor geminiTokenAccessor;
  String pluginName;
  private Codec codec;

  @Before
  public void setUp() {
    codec = plugin.getSysInjector().getInstance(Codec.class);
    pluginName = plugin.getSysInjector().getInstance(Key.get(String.class, PluginName.class));
  }

  @Test
  public void shouldAddTokenForSelf() throws Exception {
    String token = "my-secret-gemini-token";
    AddToken.Input input = new AddToken.Input();
    input.token = token;

    userRestSession.put(getAddTokenUri("self"), input).assertCreated();

    Account.Id accountId = user.id();

    assertTokenCorrectlySet(accountId, token);
  }

  @Test
  public void shouldUpdateTokenForSelf() throws Exception {
    String token = "my-initial-secret-gemini-token";
    AddToken.Input input = new AddToken.Input();
    input.token = token;

    userRestSession.put(getAddTokenUri("self"), input).assertCreated();

    Account.Id accountId = user.id();

    assertTokenCorrectlySet(accountId, token);

    String updatedToken = "my-updated-secret-gemini-token";
    input.token = updatedToken;

    userRestSession.put(getAddTokenUri("self"), input).assertCreated();

    assertTokenCorrectlySet(accountId, updatedToken);
  }

  private void assertTokenCorrectlySet(Account.Id accountId, String token)
      throws IOException, ConfigInvalidException {
    assertThat(geminiTokenAccessor.getToken(accountId)).hasValue(codec.encode(token));
  }

  @Test
  public void adminShouldAddTokenForOtherUser() throws Exception {
    String token = "my-secret-gemini-token";
    AddToken.Input input = new AddToken.Input();
    input.token = token;

    adminRestSession.put(getAddTokenUri(user.id().toString()), input).assertCreated();

    Account.Id accountId = user.id();

    assertTokenCorrectlySet(accountId, token);
  }

  @Test
  public void shouldReturnBadRequestOnMissingToken() throws Exception {
    AddToken.Input input = new AddToken.Input();
    input.token = null;

    adminRestSession.put(getAddTokenUri("self"), input).assertBadRequest();
  }

  @Test
  public void shouldReturnForbiddenWhenModifyingAnotherUser() throws Exception {
    AddToken.Input input = new AddToken.Input();
    input.token = "token";

    userRestSession.put(getAddTokenUri(admin.id().toString()), input).assertForbidden();
  }

  private String getAddTokenUri(String account) {
    return String.join("/", "/accounts", account, pluginName) + "~" + API_TOKEN_ENDPOINT;
  }
}
