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

import static com.gerritforge.gerrit.plugins.ai.gemini.TokenUtils.API_TOKEN_ENDPOINT;
import static com.google.common.truth.Truth.assertThat;

import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.RestResponse;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gson.Gson;
import com.google.inject.Key;
import org.junit.Before;
import org.junit.Test;

@TestPlugin(
    name = "ai-review-agent-gemini",
    sysModule = "com.gerritforge.gerrit.plugins.ai.gemini.AiReviewRestApiModule",
    httpModule = "com.gerritforge.gerrit.plugins.ai.gemini.HttpModule")
public class GetTokenIT extends LightweightPluginDaemonTest {
  private final Gson gson = new Gson();
  private String pluginName;

  @Before
  public void setUp() {
    pluginName = plugin.getSysInjector().getInstance(Key.get(String.class, PluginName.class));
  }

  @Test
  public void shouldGetTokenForSelf() throws Exception {
    String token = "my-secret-gemini-token";
    AddToken.Input input = new AddToken.Input();
    input.token = token;

    userRestSession.put(getTokenUri("self"), input).assertCreated();

    RestResponse response = userRestSession.get(getTokenUri("self"));
    response.assertOK();

    GetToken.Output out = gson.fromJson(response.getReader(), GetToken.Output.class);
    assertThat(out.token).isEqualTo(token);
  }

  @Test
  public void shouldReturnNotFoundWhenTokenNotSet() throws Exception {
    userRestSession.get(getTokenUri("self")).assertNotFound();
  }

  @Test
  public void shouldReturnForbiddenWhenGettingAnotherUserToken() throws Exception {
    String token = "admin-token";
    AddToken.Input input = new AddToken.Input();
    input.token = token;

    adminRestSession.put(getTokenUri("self"), input).assertCreated();

    userRestSession.get(getTokenUri(admin.id().toString())).assertForbidden();
  }

  private String getTokenUri(String account) {
    return String.join("/", "/accounts", account, pluginName) + "~" + API_TOKEN_ENDPOINT;
  }
}
