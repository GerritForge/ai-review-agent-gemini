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
import com.google.gerrit.acceptance.RestResponse;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.testing.ConfigSuite;
import com.google.gson.Gson;
import org.eclipse.jgit.lib.Config;
import org.junit.Test;

@TestPlugin(
    name = "ai-review-agent-gemini",
    sysModule = "com.gerritforge.gerrit.plugins.ai.gemini.AiReviewRestApiModule",
    httpModule = "com.gerritforge.gerrit.plugins.ai.gemini.HttpModule")
public class GetTokenIT extends LightweightPluginDaemonTest {

  private static final String TEST_PASSWORD = "test-password";
  private final Gson gson = new Gson();

  @ConfigSuite.Default
  public static Config defaultConfig() {
    Config cfg = new Config();
    cfg.setString("secureConfig", null, "password", TEST_PASSWORD);
    return cfg;
  }

  @Test
  public void shouldGetTokenForSelf() throws Exception {
    String token = "my-secret-gemini-token";
    AddToken.Input input = new AddToken.Input();
    input.token = token;

    userRestSession.put("/accounts/self/geminiToken", input).assertCreated();

    RestResponse response = userRestSession.get("/accounts/self/geminiToken");
    response.assertOK();

    GetToken.Output output = gson.fromJson(response.getReader(), GetToken.Output.class);
    assertThat(output.token).isEqualTo(token);
  }

  @Test
  public void shouldReturnNotFoundWhenTokenNotSet() throws Exception {
    userRestSession.get("/accounts/self/geminiToken").assertNotFound();
  }

  @Test
  public void shouldReturnForbiddenWhenGettingAnotherUserToken() throws Exception {
    String token = "admin-token";
    AddToken.Input input = new AddToken.Input();
    input.token = token;

    adminRestSession.put("/accounts/self/geminiToken", input).assertCreated();

    userRestSession.get("/accounts/" + admin.id().get() + "/geminiToken").assertForbidden();
  }
}
