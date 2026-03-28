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
import com.google.gson.JsonParseException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
      "https://generativelanguage.googleapis.com/v1/models/%s:generateContent";
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
    HttpPost post = new HttpPost(String.format(GEMINI_API_URL, model));
    post.setHeader("Content-Type", "application/json");
    post.setHeader("x-goog-api-key", apiKey);
    post.setEntity(
        new StringEntity(
            GSON.toJson(GeminiApi.GenerateContentRequest.fromPrompt(prompt)),
            StandardCharsets.UTF_8));

    try (CloseableHttpResponse response = httpClient.execute(post)) {
      int statusCode = response.getStatusLine().getStatusCode();
      String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

      if (statusCode < 200 || statusCode >= 300) {
        throw new IOException(
            String.format(
                "Gemini API returned HTTP %d: %s", statusCode, extractErrorMessage(body)));
      }

      return extractResponseText(body);
    } catch (IOException e) {
      logger.atWarning().withCause(e).log("Failed to call Gemini API (model=%s)", model);
      throw new RuntimeException("Failed to call Gemini API", e);
    }
  }

  private String extractResponseText(String body) throws IOException {
    GeminiApi.GenerateContentResponse response =
        GSON.fromJson(body, GeminiApi.GenerateContentResponse.class);

    GeminiApi.GenerateContentResponse.Candidate candidate =
        ensureExists(response.candidates(), "Gemini API returned no candidates").get(0);
    if (candidate.content() == null) {
      String finishReason = candidate.finishReason() != null ? candidate.finishReason() : "unknown";
      throw new IOException(
          String.format("Gemini API candidate has no content, finishReason=%s", finishReason));
    }

    StringBuilder text = new StringBuilder();
    for (GeminiApi.Content.Part part :
        ensureExists(candidate.content().parts(), "Gemini API candidate content has no parts")) {
      if (part.text() != null) {
        text.append(part.text());
      }
    }

    if (text.isEmpty()) {
      throw new IOException("Gemini API response contains no text parts");
    }

    return text.toString();
  }

  private static <T> List<T> ensureExists(List<T> existsAndNotEmpty, String error)
      throws IOException {
    if (existsAndNotEmpty == null || existsAndNotEmpty.isEmpty()) {
      throw new IOException(error);
    }
    return existsAndNotEmpty;
  }

  private static String extractErrorMessage(String body) {
    try {
      GeminiApi.ErrorResponse errorResponse = GSON.fromJson(body, GeminiApi.ErrorResponse.class);
      if (errorResponse != null && errorResponse.error() != null) {
        GeminiApi.ErrorResponse.Error error = errorResponse.error();
        return String.format("[%s] %s", error.status(), error.message());
      }
    } catch (JsonParseException e) {
      logger.atWarning().withCause(e).log("Failed to parse error response");
    }

    // fall back to plain string content if parsing the error fails
    return body.length() > MAX_ERROR_BODY_LENGTH
        ? body.substring(0, MAX_ERROR_BODY_LENGTH) + "..."
        : body;
  }
}
