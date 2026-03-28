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

import com.gerritforge.gerrit.plugins.ai.provider.api.AiReviewProvider;
import com.google.common.flogger.FluentLogger;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

@Singleton
public class AiGeminiReviewProvider implements AiReviewProvider {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final String GEMINI_DISPLAY_NAME = "Gemini";
  private static final String GEMINI_API_URL =
      "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";
  private static final int MAX_ERROR_BODY_LENGTH = 500;

  private static final Gson GSON = new Gson();

  private final CloseableHttpClient httpClient;

  @Inject
  AiGeminiReviewProvider(CloseableHttpClient httpClient) {
    this.httpClient = httpClient;
  }

  @Override
  public String getDisplayName() {
    return GEMINI_DISPLAY_NAME;
  }

  @Override
  public Set<String> getModels() {
    return Set.of("gemini-2.5-flash", "gemini-2.5-flash-lite", "gemini-2.5-pro");
  }

  @Override
  public String review(String apiKey, String model, String prompt) {
    JsonObject requestBody = buildRequestBody(prompt);

    HttpPost post = new HttpPost(String.format(GEMINI_API_URL, model));
    post.setHeader("Content-Type", "application/json");
    post.setHeader("x-goog-api-key", apiKey);
    post.setEntity(new StringEntity(GSON.toJson(requestBody), StandardCharsets.UTF_8));

    try (CloseableHttpResponse response = httpClient.execute(post)) {
      int statusCode = response.getStatusLine().getStatusCode();
      String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

      if (statusCode < 200 || statusCode >= 300) {
        String snippet =
            body.length() > MAX_ERROR_BODY_LENGTH
                ? body.substring(0, MAX_ERROR_BODY_LENGTH) + "..."
                : body;
        throw new IOException(
            String.format("Gemini API returned HTTP %d: %s", statusCode, snippet));
      }

      return extractResponseText(body);
    } catch (IOException e) {
      logger.atWarning().withCause(e).log("Failed to call Gemini API (model=%s)", model);
      throw new RuntimeException("Failed to call Gemini API", e);
    }
  }

  private static JsonObject buildRequestBody(String prompt) {
    JsonObject part = new JsonObject();
    part.addProperty("text", prompt);

    JsonObject content = new JsonObject();
    content.add("parts", jsonArrayOf(part));

    JsonObject requestBody = new JsonObject();
    requestBody.add("contents", jsonArrayOf(content));
    return requestBody;
  }

  private static JsonArray jsonArrayOf(JsonElement element) {
    JsonArray array = new JsonArray();
    array.add(element);
    return array;
  }

  /**
   * Extracts and concatenates text from all parts of the first candidate, matching the behavior of
   * the Gemini SDK's.
   *
   * @see <a href="https://ai.google.dev/api/generate-content#v1beta.GenerateContentResponse">Gemini
   *     API - GenerateContentResponse</a>
   */
  private String extractResponseText(String body) throws IOException {
    JsonObject json = GSON.fromJson(body, JsonObject.class);
    JsonArray candidates = json.getAsJsonArray("candidates");
    if (candidates == null || candidates.isEmpty()) {
      throw new IOException("Gemini API returned no candidates");
    }

    JsonObject candidate = candidates.get(0).getAsJsonObject();
    JsonObject content = candidate.getAsJsonObject("content");
    if (content == null) {
      String finishReason =
          candidate.has("finishReason") ? candidate.get("finishReason").getAsString() : "unknown";
      throw new IOException(
          String.format("Gemini API candidate has no content, finishReason=%s", finishReason));
    }

    JsonArray parts = content.getAsJsonArray("parts");
    if (parts == null || parts.isEmpty()) {
      throw new IOException("Gemini API candidate content has no parts");
    }

    StringBuilder text = new StringBuilder();
    for (JsonElement part : parts) {
      JsonElement textElement = part.getAsJsonObject().get("text");
      if (textElement != null) {
        text.append(textElement.getAsString());
      }
    }

    if (text.isEmpty()) {
      throw new IOException("Gemini API response contains no text parts");
    }

    return text.toString();
  }
}
