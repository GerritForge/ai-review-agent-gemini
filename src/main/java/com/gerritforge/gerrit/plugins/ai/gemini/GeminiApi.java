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

import java.util.List;

/**
 * Gson-compatible records modelling the Gemini generateContent request and response.
 *
 * @see <a href="https://ai.google.dev/api/generate-content">Gemini API - Generate Content</a>
 */
final class GeminiApi {
  private GeminiApi() {}

  record Content(List<Part> parts) {
    record Part(String text) {}
  }

  record GenerateContentRequest(List<Content> contents) {
    static GenerateContentRequest fromPrompt(String prompt) {
      return new GenerateContentRequest(List.of(new Content(List.of(new Content.Part(prompt)))));
    }
  }

  record GenerateContentResponse(List<Candidate> candidates) {
    record Candidate(Content content, String finishReason) {}
  }

  /**
   * Standard Google API error envelope.
   *
   * @see <a href="https://cloud.google.com/apis/design/errors#error_model">Google API Error
   *     Model</a>
   */
  record ErrorResponse(Error error) {
    record Error(int code, String message, String status) {}
  }
}
