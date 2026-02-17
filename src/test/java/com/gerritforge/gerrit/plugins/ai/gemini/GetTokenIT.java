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
import com.google.gson.Gson;
import org.junit.Test;

@TestPlugin(
    name = "ai-review-agent-gemini",
    sysModule = "com.gerritforge.gerrit.plugins.ai.gemini.AiReviewRestApiModule",
    httpModule = "com.gerritforge.gerrit.plugins.ai.gemini.HttpModule")
public class GetTokenIT extends LightweightPluginDaemonTest {
  private final Gson gson = new Gson();

  @Test
  public void shouldGetTokenForSelf() throws Exception {
    String token = "my-secret-gemini-token";
    AddToken.Input input = new AddToken.Input();
    input.token = token;

    userRestSession.put(getEndpointForUser("self"), input).assertCreated();

    RestResponse response = userRestSession.get(getEndpointForUser("self"));
    response.assertOK();

    assertThat(gson.fromJson(response.getReader(), String.class)).isEqualTo(token);
  }

  @Test
  public void shouldReturnNotFoundWhenTokenNotSet() throws Exception {
    userRestSession.get(getEndpointForUser("self")).assertNotFound();
  }

  @Test
  public void shouldReturnForbiddenWhenGettingAnotherUserToken() throws Exception {
    String token = "admin-token";
    AddToken.Input input = new AddToken.Input();
    input.token = token;

    adminRestSession.put(getEndpointForUser("self"), input).assertCreated();

    userRestSession.get(getEndpointForUser(admin.id().toString())).assertForbidden();
  }

  private static String getEndpointForUser(String user) {
    return String.join("/", "/accounts", user, API_TOKEN_ENDPOINT);
  }
}
